package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages AI service requests and tracks usage metrics.
 * Integrates with pattern learning system to optimize API usage.
 */
@Service(Service.Level.APP)
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    
    // Estimated average tokens per request
    private static final int AVG_TOKENS_PER_REQUEST = 1000;
    
    // Cost per 1K tokens
    private static final double COST_PER_1K_TOKENS = 0.002; // $0.002 per 1K tokens for GPT-4
    
    // Metrics
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger apiCalls = new AtomicInteger(0);
    private final AtomicInteger estimatedTokensSaved = new AtomicInteger(0);
    private final AtomicInteger estimatedCostSavedInCents = new AtomicInteger(0);
    
    // Cache for project-specific services
    private final Map<Project, ProjectAIServices> projectServicesMap = new ConcurrentHashMap<>();
    
    /**
     * Project-specific AI services.
     */
    private static class ProjectAIServices {
        final PatternCachingService cachingService;
        final PatternRecognitionService recognitionService;
        final EmbeddingService embeddingService;
        
        ProjectAIServices(Project project) {
            this.cachingService = PatternCachingService.getInstance(project);
            this.recognitionService = PatternRecognitionService.getInstance(project);
            this.embeddingService = EmbeddingService.getInstance(project);
        }
    }
    
    /**
     * Gets the AIServiceManager instance.
     * @return The AIServiceManager instance
     */
    public static AIServiceManager getInstance() {
        return ServiceManager.getService(AIServiceManager.class);
    }
    
    /**
     * Gets project-specific services.
     * @param project The project
     * @return The project services
     */
    @NotNull
    private ProjectAIServices getProjectServices(@NotNull Project project) {
        return projectServicesMap.computeIfAbsent(project, ProjectAIServices::new);
    }
    
    /**
     * Records a request to the AI service.
     * @return The request ID
     */
    public int recordRequest() {
        return totalRequests.incrementAndGet();
    }
    
    /**
     * Records a pattern match success.
     * @param tokensEstimate The estimated number of tokens saved
     * @param project The current project, may be null for application-level instances
     * @return The pattern match ID
     */
    public int recordPatternMatchSuccess(int tokensEstimate, @Nullable Project project) {
        estimatedTokensSaved.addAndGet(tokensEstimate);
        
        // Calculate cost saved in cents
        int costSavedInCents = (int) (tokensEstimate * COST_PER_1K_TOKENS * 100 / 1000);
        estimatedCostSavedInCents.addAndGet(costSavedInCents);
        
        // Show cost-saving notification if significant enough (more than 1 cent)
        if (project != null && costSavedInCents > 1) {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            if (settings.isEnableNotifications()) {
                // Only show cost-saving notifications for significant savings
                double costSaved = costSavedInCents / 100.0;
                ModForgeNotificationService.getInstance(project).showInfoNotification(
                        project,
                        "ModForge Pattern Learning",
                        String.format("Pattern learning saved ~$%.2f in API costs by reusing similar patterns.", costSaved)
                );
            }
        }
        
        return patternMatches.incrementAndGet();
    }
    
    /**
     * Records a pattern match success without a project reference.
     * @param tokensEstimate The estimated number of tokens saved
     * @return The pattern match ID
     */
    public int recordPatternMatchSuccess(int tokensEstimate) {
        return recordPatternMatchSuccess(tokensEstimate, null);
    }
    
    /**
     * Records an API fallback.
     * @param project The current project, may be null for application-level instances
     * @return The API call ID
     */
    public int recordApiFallback(@Nullable Project project) {
        int callId = apiCalls.incrementAndGet();
        
        // Show a notification about API usage with current pattern matching percentage
        if (project != null) {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            if (settings.isEnableNotifications() && settings.isEnableContinuousDevelopment()) {
                int total = totalRequests.get();
                int matches = patternMatches.get();
                int apis = apiCalls.get();
                
                // Only show if we have enough data to make meaningful percentage
                if (total > 10) {
                    int matchPercentage = matches * 100 / total;
                    ModForgeNotificationService.getInstance(project).showInfoNotification(
                            project,
                            "ModForge API Usage",
                            String.format("Using OpenAI API for this request. Current pattern match rate: %d%%", matchPercentage)
                    );
                }
            }
        }
        
        return callId;
    }
    
    /**
     * Records an API fallback without a project reference.
     * @return The API call ID
     */
    public int recordApiFallback() {
        return recordApiFallback(null);
    }
    
    /**
     * Gets usage metrics.
     * @return The usage metrics
     */
    @NotNull
    public Map<String, Integer> getUsageMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("patternMatches", patternMatches.get());
        metrics.put("apiCalls", apiCalls.get());
        metrics.put("estimatedTokensSaved", estimatedTokensSaved.get());
        metrics.put("estimatedCostSavedInCents", estimatedCostSavedInCents.get());
        
        return metrics;
    }
    
    /**
     * Executes an AI request with pattern matching optimization.
     * @param project The project
     * @param input The input
     * @param category The pattern category
     * @param fallbackFunction The fallback function to call if no pattern match
     * @return The result
     */
    @Nullable
    public String executeWithPatternMatching(
            @NotNull Project project,
            @NotNull String input,
            @NotNull PatternRecognitionService.PatternCategory category,
            @NotNull java.util.function.Function<String, String> fallbackFunction
    ) {
        // Record the request
        recordRequest();
        
        // Get project services
        ProjectAIServices services = getProjectServices(project);
        
        // Try to find a matching pattern
        String patternResult = services.recognitionService.findMatchingPattern(input, category);
        
        if (patternResult != null) {
            // Pattern match found
            recordPatternMatchSuccess(AVG_TOKENS_PER_REQUEST, project);
            LOG.info("Used pattern matching for category: " + category);
            return patternResult;
        }
        
        // No pattern match, fallback to API
        recordApiFallback(project);
        LOG.info("Falling back to API for category: " + category);
        
        // Execute the fallback function
        String apiResult = fallbackFunction.apply(input);
        
        // If we got a result, register it as a pattern
        if (apiResult != null && !apiResult.isEmpty()) {
            services.recognitionService.registerPattern(input, apiResult, category);
        }
        
        return apiResult;
    }
    
    /**
     * Smart method for generating code with pattern matching optimization.
     * @param project The project
     * @param prompt The prompt
     * @param language The programming language
     * @return The generated code
     */
    @Nullable
    public String smartGenerateCode(
            @NotNull Project project,
            @NotNull String prompt,
            @NotNull String language
    ) {
        return executeWithPatternMatching(
                project,
                prompt + "\nLanguage: " + language,
                PatternRecognitionService.PatternCategory.CODE_GENERATION,
                input -> {
                    // Call OpenAI API - This is simplified for the demo
                    // In a real implementation, this would call the actual OpenAI API
                    LOG.info("Called OpenAI API for code generation");
                    return "// Generated code for: " + prompt + "\n" +
                           "public class Example {\n" +
                           "    public static void main(String[] args) {\n" +
                           "        System.out.println(\"Hello, world!\");\n" +
                           "    }\n" +
                           "}";
                }
        );
    }
    
    /**
     * Smart method for fixing code errors with pattern matching optimization.
     * @param project The project
     * @param code The code with errors
     * @param errorMessage The error message
     * @return The fixed code
     */
    @Nullable
    public String smartFixCode(
            @NotNull Project project,
            @NotNull String code,
            @NotNull String errorMessage
    ) {
        return executeWithPatternMatching(
                project,
                "Code: " + code + "\nError: " + errorMessage,
                PatternRecognitionService.PatternCategory.ERROR_RESOLUTION,
                input -> {
                    // Call OpenAI API - This is simplified for the demo
                    // In a real implementation, this would call the actual OpenAI API
                    LOG.info("Called OpenAI API for error resolution");
                    return code.replace("System.out.println(\"Hello, world!\");",
                                       "System.out.println(\"Hello, fixed world!\");");
                }
        );
    }
    
    /**
     * Smart method for suggesting features with pattern matching optimization.
     * @param project The project
     * @param codeContext The code context
     * @return The suggested features
     */
    @Nullable
    public String smartSuggestFeatures(
            @NotNull Project project,
            @NotNull String codeContext
    ) {
        return executeWithPatternMatching(
                project,
                codeContext,
                PatternRecognitionService.PatternCategory.FEATURE_SUGGESTION,
                input -> {
                    // Call OpenAI API - This is simplified for the demo
                    // In a real implementation, this would call the actual OpenAI API
                    LOG.info("Called OpenAI API for feature suggestion");
                    return "1. Add logging functionality\n" +
                           "2. Implement exception handling\n" +
                           "3. Add unit tests";
                }
        );
    }
    
    /**
     * Registers a project for cleanup when it's closed.
     * @param project The project
     */
    public void registerProjectForCleanup(@NotNull Project project) {
        // Remove the project from the services map when it's closed
        // Register for project closing using ProjectManagerListener
        ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
            @Override
            public void projectClosing(@NotNull Project closingProject) {
                if (closingProject.equals(project)) {
                    ProjectAIServices services = projectServicesMap.remove(project);
                    
                    // Show notification if significant cost savings
                    if (services != null && ModForgeSettings.getInstance().isEnableNotifications()) {
                        int tokensSaved = services.recognitionService.getEstimatedTokensSaved();
                        if (tokensSaved > 1000) {  // Only show if we saved more than 1000 tokens
                            double costSaved = tokensSaved * COST_PER_1K_TOKENS / 1000.0;
                            
                            // Show cost savings notification - but only if it's significant
                            if (costSaved >= 0.01) {  // More than 1 cent
                                ModForgeNotificationService.getInstance(project).showInfoNotification(
                                    project,
                                    "ModForge AI Cost Savings",
                                    String.format("This session saved approximately $%.2f in API costs through pattern matching.", costSaved)
                                );
                            }
                        }
                    }
                    
                    LOG.info("Cleaned up AI services for closed project: " + project.getName());
                }
            }
        });
    }
}