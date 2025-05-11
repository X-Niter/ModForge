package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.ai.pattern.PatternData;
import com.modforge.intellij.plugin.ai.pattern.PatternLearningSystem;
import com.modforge.intellij.plugin.notifications.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Pattern recognition service for optimizing API calls.
 * Uses learned patterns to avoid redundant API calls and reduce costs.
 */
@Service
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    // Estimated average tokens per API call
    private static final int AVG_TOKENS_PER_REQUEST = 1000;
    
    // Estimated cost per 1000 tokens in cents (GPT-4 pricing)
    private static final float COST_PER_1000_TOKENS_CENTS = 3.0f;
    
    private final Project project;
    private final PatternLearningSystem patternLearningSystem;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicLong lastNotificationTime = new AtomicLong(0);
    
    // Prevent notification spam by limiting frequency
    private static final long MIN_NOTIFICATION_INTERVAL_MS = TimeUnit.HOURS.toMillis(2);
    
    public PatternRecognitionService(Project project) {
        this.project = project;
        this.patternLearningSystem = PatternLearningSystem.getInstance(project);
        
        // Schedule periodic reports about pattern learning effectiveness
        schedulePeriodicReports();
        
        LOG.info("Pattern recognition service initialized for project: " + project.getName());
    }
    
    /**
     * Try to match the prompt against learned patterns to avoid API calls
     * 
     * @param prompt The AI prompt
     * @param context Optional context for the prompt
     * @return The matched response if found, null otherwise
     */
    @Nullable
    public String tryMatchPattern(String prompt, @Nullable String context) {
        if (!enabled.get() || !patternLearningSystem.isEnabled()) {
            return null;
        }
        
        try {
            Pair<PatternData, Float> match = patternLearningSystem.findMatch(prompt, context);
            if (match != null) {
                PatternData pattern = match.getFirst();
                float confidence = match.getSecond();
                
                LOG.info("Pattern match found for prompt with confidence: " + confidence);
                return pattern.getResponse();
            }
        } catch (Exception e) {
            LOG.warn("Error in pattern matching", e);
        }
        
        return null;
    }
    
    /**
     * Use the pattern recognition system to optimize API calls
     * 
     * @param prompt The AI prompt
     * @param context Optional context for the prompt
     * @param apiCallSupplier Supplier that makes the actual API call if no pattern match is found
     * @return CompletableFuture with the response, either from a pattern match or the API call
     */
    @NotNull
    public CompletableFuture<String> withPatternRecognition(
            String prompt, 
            @Nullable String context,
            Supplier<CompletableFuture<String>> apiCallSupplier) {
        
        // Try to match against existing patterns first
        String patternMatch = tryMatchPattern(prompt, context);
        if (patternMatch != null) {
            // If we found a match, return it immediately
            return CompletableFuture.completedFuture(patternMatch);
        }
        
        // If no match was found, make the API call
        return apiCallSupplier.get()
            .thenApply(response -> {
                // Record the successful API call for future pattern matching
                recordSuccessfulCall(prompt, response, context);
                return response;
            })
            .exceptionally(e -> {
                // Record the failed API call
                recordFailedCall(prompt, context);
                throw new RuntimeException("API call failed", e);
            });
    }
    
    /**
     * Record a successful API call for pattern learning
     * 
     * @param prompt The prompt used
     * @param response The response received
     * @param context Optional context for the prompt
     */
    public void recordSuccessfulCall(String prompt, String response, @Nullable String context) {
        if (!enabled.get() || !patternLearningSystem.isEnabled()) {
            return;
        }
        
        try {
            // Estimate tokens used based on prompt and response length
            int estimatedTokens = estimateTokens(prompt, response);
            
            // Estimate cost in cents
            int estimatedCostCents = Math.round(estimatedTokens * (COST_PER_1000_TOKENS_CENTS / 1000));
            
            // Record the successful pattern
            patternLearningSystem.recordSuccess(prompt, response, context, estimatedTokens, estimatedCostCents);
            
            LOG.debug("Recorded successful API call for pattern learning");
        } catch (Exception e) {
            LOG.warn("Error recording successful API call", e);
        }
    }
    
    /**
     * Record a failed API call for pattern learning
     * 
     * @param prompt The prompt used
     * @param context Optional context for the prompt
     */
    public void recordFailedCall(String prompt, @Nullable String context) {
        if (!enabled.get() || !patternLearningSystem.isEnabled()) {
            return;
        }
        
        try {
            patternLearningSystem.recordFailure(prompt, context);
            LOG.debug("Recorded failed API call for pattern learning");
        } catch (Exception e) {
            LOG.warn("Error recording failed API call", e);
        }
    }
    
    /**
     * Estimate the number of tokens used for a prompt and response
     * 
     * @param prompt The prompt
     * @param response The response
     * @return Estimated token count
     */
    private int estimateTokens(String prompt, String response) {
        // Simple estimation based on number of words (1 token ≈ 0.75 words)
        int promptWords = countWords(prompt);
        int responseWords = countWords(response);
        
        return (int) ((promptWords + responseWords) / 0.75);
    }
    
    /**
     * Count the number of words in a string
     * 
     * @param text The text to count words in
     * @return The number of words
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        return text.split("\\s+").length;
    }
    
    /**
     * Schedule periodic reports about pattern learning effectiveness
     */
    private void schedulePeriodicReports() {
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            this::reportPatternLearningEffectiveness,
            24, 24, TimeUnit.HOURS
        );
    }
    
    /**
     * Report statistics about pattern learning effectiveness
     */
    private void reportPatternLearningEffectiveness() {
        if (!enabled.get() || !patternLearningSystem.isEnabled()) {
            return;
        }
        
        Map<String, Object> stats = patternLearningSystem.getStatistics();
        
        int totalRequests = (int) stats.get("totalRequests");
        int patternMatches = (int) stats.get("patternMatches");
        float hitRate = (float) stats.get("hitRate");
        int tokensSaved = (int) stats.get("estimatedTokensSaved");
        int costSavedCents = (int) stats.get("estimatedCostSavedCents");
        
        if (totalRequests > 100 && hitRate > 0.1) {
            // Only notify if it's been a while since the last notification
            long now = System.currentTimeMillis();
            long lastTime = lastNotificationTime.get();
            
            if (now - lastTime > MIN_NOTIFICATION_INTERVAL_MS) {
                lastNotificationTime.set(now);
                
                // Format the message
                String message = String.format(
                    "Pattern learning has saved approximately $%.2f by avoiding %d API calls (%.1f%% hit rate).",
                    costSavedCents / 100.0, patternMatches, hitRate * 100
                );
                
                // Show a notification
                ModForgeNotificationService notificationService = project.getService(ModForgeNotificationService.class);
                if (notificationService != null) {
                    notificationService.showInfo("Pattern Learning Savings", message);
                }
                
                LOG.info("Pattern learning effectiveness: " + message);
            }
        }
    }
    
    /**
     * Enable or disable pattern recognition
     * 
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        LOG.info("Pattern recognition " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if pattern recognition is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled.get();
    }
    
    /**
     * Get statistics about pattern recognition usage
     * 
     * @return Statistics about pattern recognition
     */
    public Map<String, Object> getStatistics() {
        return patternLearningSystem.getStatistics();
    }
    
    /**
     * Reset pattern recognition statistics
     */
    public void resetStatistics() {
        patternLearningSystem.resetStatistics();
    }
}