package com.modforge.intellij.plugin.ai.pattern;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Advanced pattern learning system to reduce API calls by identifying common patterns.
 * Learns from successful and failed generation attempts to improve over time.
 * Uses a combination of exact matches, fuzzy matches, and semantic similarity.
 */
@State(
    name = "ModForgePatternLearningSystem",
    storages = @Storage("modforge-pattern-learning.xml")
)
public class PatternLearningSystem implements PersistentStateComponent<PatternLearningSystem> {
    private static final Logger LOG = Logger.getInstance(PatternLearningSystem.class);
    
    // Configuration parameters
    private boolean enabled = true;
    private int maxPatterns = 1000;
    private float minConfidenceThreshold = 0.85f;
    private int minSuccessfulMatchesForPattern = 3;
    
    // Pattern learning data
    private final Map<String, PatternData> patterns = new ConcurrentHashMap<>();
    private final Map<String, PatternData> semanticPatterns = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger apiCalls = new AtomicInteger(0);
    private final AtomicInteger estimatedTokensSaved = new AtomicInteger(0);
    private final AtomicInteger estimatedCostSavedCents = new AtomicInteger(0);
    
    @Transient
    private final Object cleanupLock = new Object();
    
    /**
     * Get the pattern learning system instance for a project
     * 
     * @param project The project
     * @return The pattern learning system
     */
    public static PatternLearningSystem getInstance(Project project) {
        return ApplicationManager.getApplication().getService(PatternLearningSystem.class);
    }
    
    /**
     * Initialize the pattern learning system
     */
    public PatternLearningSystem() {
        // Schedule periodic cleanup to prevent unbounded growth
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            this::cleanupOldPatterns,
            1, 12, TimeUnit.HOURS
        );
        
        LOG.info("Pattern learning system initialized with " + patterns.size() + " patterns");
    }
    
    /**
     * Try to find a match for a prompt in the pattern database
     * 
     * @param prompt The prompt to match
     * @param context Additional context for the match (optional)
     * @return A matching pattern and confidence score, or null if no match found
     */
    @Nullable
    public Pair<PatternData, Float> findMatch(String prompt, @Nullable String context) {
        if (!enabled || prompt == null || prompt.isEmpty()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        // First, try exact match
        PatternData exactMatch = patterns.get(prompt);
        if (exactMatch != null && exactMatch.isReliable()) {
            patternMatches.incrementAndGet();
            estimatedTokensSaved.addAndGet(exactMatch.getEstimatedTokens());
            estimatedCostSavedCents.addAndGet(exactMatch.getEstimatedCostCents());
            return Pair.create(exactMatch, 1.0f);
        }
        
        // Next, try fuzzy matching for high-confidence matches
        Pair<PatternData, Float> bestMatch = findBestFuzzyMatch(prompt, context);
        if (bestMatch != null && bestMatch.getSecond() >= minConfidenceThreshold) {
            PatternData matchedPattern = bestMatch.getFirst();
            patternMatches.incrementAndGet();
            estimatedTokensSaved.addAndGet(matchedPattern.getEstimatedTokens());
            estimatedCostSavedCents.addAndGet(matchedPattern.getEstimatedCostCents());
            return bestMatch;
        }
        
        // No match found
        apiCalls.incrementAndGet();
        return null;
    }
    
    /**
     * Find the best fuzzy match for a prompt
     * 
     * @param prompt The prompt to match
     * @param context Additional context for the match (optional)
     * @return The best matching pattern and confidence score, or null if no good match
     */
    @Nullable
    private Pair<PatternData, Float> findBestFuzzyMatch(String prompt, @Nullable String context) {
        float bestScore = 0;
        PatternData bestPattern = null;
        
        // Simple fuzzy match based on word overlap
        for (PatternData pattern : patterns.values()) {
            if (!pattern.isReliable()) {
                continue;
            }
            
            float score = calculateSimilarity(prompt, pattern.getPrompt());
            
            // Consider context if available
            if (context != null && pattern.getContext() != null) {
                float contextScore = calculateSimilarity(context, pattern.getContext());
                // Weight context less than prompt
                score = score * 0.7f + contextScore * 0.3f;
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestPattern = pattern;
            }
        }
        
        if (bestPattern != null) {
            return Pair.create(bestPattern, bestScore);
        }
        
        return null;
    }
    
    /**
     * Calculate the similarity between two strings
     * 
     * @param str1 The first string
     * @param str2 The second string
     * @return A similarity score between 0 and 1
     */
    private float calculateSimilarity(String str1, String str2) {
        // Tokenize the strings into words
        Set<String> words1 = tokenize(str1);
        Set<String> words2 = tokenize(str2);
        
        // Calculate Jaccard similarity
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        return union.isEmpty() ? 0 : (float) intersection.size() / union.size();
    }
    
    /**
     * Tokenize a string into words
     * 
     * @param str The string to tokenize
     * @return A set of word tokens
     */
    private Set<String> tokenize(String str) {
        return Arrays.stream(str.toLowerCase()
                .replaceAll("[^a-z0-9]", " ")
                .split("\\s+"))
                .filter(s -> s.length() > 2)  // Filter out very short words
                .collect(Collectors.toSet());
    }
    
    /**
     * Record a successful API call for pattern learning
     * 
     * @param prompt The prompt used
     * @param response The successful response
     * @param context Additional context (optional)
     * @param estimatedTokens Estimated tokens used for the API call
     * @param estimatedCostCents Estimated cost of the API call in cents
     */
    public void recordSuccess(String prompt, String response, @Nullable String context, 
                              int estimatedTokens, int estimatedCostCents) {
        if (!enabled || prompt == null || prompt.isEmpty() || response == null || response.isEmpty()) {
            return;
        }
        
        // Get existing pattern or create new one
        PatternData pattern = patterns.computeIfAbsent(prompt, k -> new PatternData(prompt, context));
        
        // Update pattern data
        pattern.recordSuccess(response, estimatedTokens, estimatedCostCents);
        
        LOG.debug("Recorded successful match for pattern: " + prompt.substring(0, Math.min(50, prompt.length())) + "...");
        
        // Periodically clean up if we exceed the maximum number of patterns
        if (patterns.size() > maxPatterns) {
            AppExecutorUtil.getAppExecutorService().execute(this::cleanupOldPatterns);
        }
    }
    
    /**
     * Record a failed API call for pattern learning
     * 
     * @param prompt The prompt that failed
     * @param context Additional context (optional)
     */
    public void recordFailure(String prompt, @Nullable String context) {
        if (!enabled || prompt == null || prompt.isEmpty()) {
            return;
        }
        
        // Get existing pattern or create new one
        PatternData pattern = patterns.computeIfAbsent(prompt, k -> new PatternData(prompt, context));
        
        // Record failure
        pattern.recordFailure();
        
        LOG.debug("Recorded failure for pattern: " + prompt.substring(0, Math.min(50, prompt.length())) + "...");
    }
    
    /**
     * Clean up old or unreliable patterns
     */
    private void cleanupOldPatterns() {
        synchronized (cleanupLock) {
            // Don't clean up if we're under the limit
            if (patterns.size() <= maxPatterns) {
                return;
            }
            
            LOG.info("Cleaning up patterns, current count: " + patterns.size());
            
            // First remove patterns with high failure rates
            List<String> unreliablePatterns = patterns.entrySet().stream()
                    .filter(e -> !e.getValue().isReliable())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            for (String key : unreliablePatterns) {
                patterns.remove(key);
            }
            
            LOG.info("Removed " + unreliablePatterns.size() + " unreliable patterns");
            
            // If we still have too many, remove the least recently used
            if (patterns.size() > maxPatterns) {
                List<Map.Entry<String, PatternData>> sortedPatterns = patterns.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getValue().getLastUsed()))
                        .collect(Collectors.toList());
                
                int toRemove = patterns.size() - maxPatterns;
                for (int i = 0; i < toRemove && i < sortedPatterns.size(); i++) {
                    patterns.remove(sortedPatterns.get(i).getKey());
                }
                
                LOG.info("Removed " + toRemove + " least recently used patterns");
            }
            
            LOG.info("Pattern cleanup complete, new count: " + patterns.size());
        }
    }
    
    /**
     * Get pattern learning statistics
     * 
     * @return Statistics about pattern learning usage
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("enabled", enabled);
        stats.put("totalPatterns", patterns.size());
        stats.put("reliablePatterns", patterns.values().stream().filter(PatternData::isReliable).count());
        stats.put("totalRequests", totalRequests.get());
        stats.put("patternMatches", patternMatches.get());
        stats.put("apiCalls", apiCalls.get());
        stats.put("hitRate", totalRequests.get() > 0 ? 
                (float) patternMatches.get() / totalRequests.get() : 0);
        stats.put("estimatedTokensSaved", estimatedTokensSaved.get());
        stats.put("estimatedCostSavedCents", estimatedCostSavedCents.get());
        
        return stats;
    }
    
    /**
     * Reset pattern learning statistics
     */
    public void resetStatistics() {
        totalRequests.set(0);
        patternMatches.set(0);
        apiCalls.set(0);
        estimatedTokensSaved.set(0);
        estimatedCostSavedCents.set(0);
    }
    
    /**
     * Enable or disable pattern learning
     * 
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOG.info("Pattern learning " + (enabled ? "enabled" : "disabled"));
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setMaxPatterns(int maxPatterns) {
        this.maxPatterns = maxPatterns;
    }
    
    public int getMaxPatterns() {
        return maxPatterns;
    }
    
    public void setMinConfidenceThreshold(float minConfidenceThreshold) {
        this.minConfidenceThreshold = minConfidenceThreshold;
    }
    
    public float getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }
    
    public void setMinSuccessfulMatchesForPattern(int minSuccessfulMatchesForPattern) {
        this.minSuccessfulMatchesForPattern = minSuccessfulMatchesForPattern;
    }
    
    public int getMinSuccessfulMatchesForPattern() {
        return minSuccessfulMatchesForPattern;
    }
    
    @Nullable
    @Override
    public PatternLearningSystem getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull PatternLearningSystem state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}