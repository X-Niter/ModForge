package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing AI requests and pattern learning.
 * This service coordinates between direct API requests and pattern-matched responses,
 * optimizing for cost and performance.
 */
@Service(Service.Level.PROJECT)
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    
    private final Project project;
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.AIServiceManager", 4);
    
    // Metrics for API usage
    private final AtomicInteger totalApiRequests = new AtomicInteger(0);
    private final AtomicInteger totalPatternMatches = new AtomicInteger(0);
    private final AtomicInteger totalTokensGenerated = new AtomicInteger(0);
    private final AtomicInteger totalTokensSaved = new AtomicInteger(0);
    private final Map<String, AtomicInteger> requestsByType = new HashMap<>();
    
    // Cache for recent requests
    private final Map<String, String> responseCache = new HashMap<>();
    
    /**
     * Creates a new AIServiceManager.
     * @param project The project
     */
    public AIServiceManager(@NotNull Project project) {
        this.project = project;
        
        // Initialize request type counters
        requestsByType.put("Code Generation", new AtomicInteger(0));
        requestsByType.put("Code Improvement", new AtomicInteger(0));
        requestsByType.put("Error Resolution", new AtomicInteger(0));
        requestsByType.put("Documentation", new AtomicInteger(0));
        
        LOG.info("AIServiceManager initialized");
    }
    
    /**
     * Gets the AIServiceManager instance.
     * @param project The project
     * @return The AIServiceManager instance
     */
    public static AIServiceManager getInstance(@NotNull Project project) {
        return project.getService(AIServiceManager.class);
    }
    
    /**
     * Generates code based on a prompt.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return The generated code
     */
    @NotNull
    public CompletableFuture<String> generateCode(@NotNull String prompt, 
                                                 @NotNull String language, 
                                                 @Nullable Map<String, Object> options) {
        // Increment metrics
        totalApiRequests.incrementAndGet();
        requestsByType.get("Code Generation").incrementAndGet();
        
        // Check pattern matching first
        return CompletableFuture.supplyAsync(() -> {
            // In a real implementation, this would check for pattern matches
            return null; // No pattern match found
        }, executor).thenCompose(matchResult -> {
            if (matchResult != null) {
                // Pattern match found
                totalPatternMatches.incrementAndGet();
                
                // Estimate tokens saved (very rough estimate)
                int estimatedTokensSaved = prompt.length() / 4 + 500;
                totalTokensSaved.addAndGet(estimatedTokensSaved);
                
                return CompletableFuture.completedFuture(matchResult);
            } else {
                // No pattern match, use API
                return callOpenAiApi(prompt, language, options);
            }
        });
    }
    
    /**
     * Improves code based on a prompt.
     * @param code The code to improve
     * @param prompt The improvement instructions
     * @param options Additional options
     * @return The improved code
     */
    @NotNull
    public CompletableFuture<String> improveCode(@NotNull String code, 
                                               @NotNull String prompt, 
                                               @Nullable Map<String, Object> options) {
        // Increment metrics
        totalApiRequests.incrementAndGet();
        requestsByType.get("Code Improvement").incrementAndGet();
        
        // Check pattern matching first
        return CompletableFuture.supplyAsync(() -> {
            // In a real implementation, this would check for pattern matches
            return null; // No pattern match found
        }, executor).thenCompose(matchResult -> {
            if (matchResult != null) {
                // Pattern match found
                totalPatternMatches.incrementAndGet();
                
                // Estimate tokens saved (very rough estimate)
                int estimatedTokensSaved = code.length() / 4 + prompt.length() / 4 + 200;
                totalTokensSaved.addAndGet(estimatedTokensSaved);
                
                return CompletableFuture.completedFuture(matchResult);
            } else {
                // No pattern match, use API
                Map<String, Object> fullOptions = new HashMap<>();
                if (options != null) {
                    fullOptions.putAll(options);
                }
                fullOptions.put("code", code);
                
                return callOpenAiApi(prompt, "code_improvement", fullOptions);
            }
        });
    }
    
    /**
     * Fixes code based on error messages.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return The fixed code
     */
    @NotNull
    public CompletableFuture<String> fixCode(@NotNull String code, 
                                           @NotNull String errorMessage, 
                                           @Nullable Map<String, Object> options) {
        // Increment metrics
        totalApiRequests.incrementAndGet();
        requestsByType.get("Error Resolution").incrementAndGet();
        
        // Check pattern matching first
        return CompletableFuture.supplyAsync(() -> {
            // In a real implementation, this would check for pattern matches
            return null; // No pattern match found
        }, executor).thenCompose(matchResult -> {
            if (matchResult != null) {
                // Pattern match found
                totalPatternMatches.incrementAndGet();
                
                // Estimate tokens saved (very rough estimate)
                int estimatedTokensSaved = code.length() / 4 + errorMessage.length() / 4 + 300;
                totalTokensSaved.addAndGet(estimatedTokensSaved);
                
                return CompletableFuture.completedFuture(matchResult);
            } else {
                // No pattern match, use API
                Map<String, Object> fullOptions = new HashMap<>();
                if (options != null) {
                    fullOptions.putAll(options);
                }
                fullOptions.put("code", code);
                fullOptions.put("error", errorMessage);
                
                return callOpenAiApi(errorMessage, "error_resolution", fullOptions);
            }
        });
    }
    
    /**
     * Generates documentation for code.
     * @param code The code to document
     * @param options Additional options
     * @return The documentation
     */
    @NotNull
    public CompletableFuture<String> generateDocumentation(@NotNull String code, 
                                                         @Nullable Map<String, Object> options) {
        // Increment metrics
        totalApiRequests.incrementAndGet();
        requestsByType.get("Documentation").incrementAndGet();
        
        // Check pattern matching first
        return CompletableFuture.supplyAsync(() -> {
            // In a real implementation, this would check for pattern matches
            return null; // No pattern match found
        }, executor).thenCompose(matchResult -> {
            if (matchResult != null) {
                // Pattern match found
                totalPatternMatches.incrementAndGet();
                
                // Estimate tokens saved (very rough estimate)
                int estimatedTokensSaved = code.length() / 4 + 400;
                totalTokensSaved.addAndGet(estimatedTokensSaved);
                
                return CompletableFuture.completedFuture(matchResult);
            } else {
                // No pattern match, use API
                Map<String, Object> fullOptions = new HashMap<>();
                if (options != null) {
                    fullOptions.putAll(options);
                }
                
                return callOpenAiApi("Generate documentation for the following code:\n\n" + code, 
                        "documentation", fullOptions);
            }
        });
    }
    
    /**
     * Calls the OpenAI API.
     * @param prompt The prompt
     * @param type The request type
     * @param options Additional options
     * @return The response
     */
    @NotNull
    private CompletableFuture<String> callOpenAiApi(@NotNull String prompt, 
                                                  @NotNull String type, 
                                                  @Nullable Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Calling OpenAI API for " + type);
                
                // Check for API key
                String apiKey = ModForgeSettings.getInstance().getOpenAiApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalStateException("OpenAI API key is not set. Please configure it in the settings.");
                }
                
                // Check cache
                String cacheKey = prompt + "|" + type + "|" + (options != null ? options.toString() : "");
                if (responseCache.containsKey(cacheKey)) {
                    LOG.info("Using cached response for " + type);
                    return responseCache.get(cacheKey);
                }
                
                // In a real implementation, we would call the OpenAI API here
                // For now, we'll simulate a response
                
                // Simulate network delay
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Generate a mock response
                String response = generateMockResponse(prompt, type, options);
                
                // Update metrics - approximately 4 chars per token
                int estimatedTokens = (prompt.length() + response.length()) / 4;
                totalTokensGenerated.addAndGet(estimatedTokens);
                
                // Cache the response
                responseCache.put(cacheKey, response);
                
                return response;
            } catch (Exception e) {
                LOG.error("Error calling OpenAI API", e);
                throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
            }
        }, executor);
    }
    
    /**
     * Generates a mock response for testing.
     * @param prompt The prompt
     * @param type The request type
     * @param options Additional options
     * @return The mock response
     */
    @NotNull
    private String generateMockResponse(@NotNull String prompt, 
                                      @NotNull String type, 
                                      @Nullable Map<String, Object> options) {
        // NOTE: In a real implementation, this would call the actual OpenAI API
        
        switch (type) {
            case "java":
            case "python":
            case "javascript":
            case "typescript":
                return "/**\n" +
                      " * Generated code based on prompt:\n" +
                      " * " + prompt + "\n" +
                      " */\n" +
                      "public class GeneratedCode {\n" +
                      "    public static void main(String[] args) {\n" +
                      "        System.out.println(\"Hello, ModForge!\");\n" +
                      "    }\n" +
                      "}";
                
            case "code_improvement":
                String code = options != null && options.containsKey("code") ? 
                        (String) options.get("code") : "";
                return "/**\n" +
                      " * Improved code based on prompt:\n" +
                      " * " + prompt + "\n" +
                      " */\n" +
                      "public class ImprovedCode {\n" +
                      "    public static void main(String[] args) {\n" +
                      "        // More efficient implementation\n" +
                      "        System.out.println(\"Hello, Improved ModForge!\");\n" +
                      "    }\n" +
                      "}";
                
            case "error_resolution":
                return "/**\n" +
                      " * Fixed code based on error:\n" +
                      " * " + prompt + "\n" +
                      " */\n" +
                      "public class FixedCode {\n" +
                      "    public static void main(String[] args) {\n" +
                      "        // Fix applied: added missing semicolon\n" +
                      "        System.out.println(\"Hello, Fixed ModForge!\");\n" +
                      "    }\n" +
                      "}";
                
            case "documentation":
                return "/**\n" +
                      " * # Code Documentation\n" +
                      " * \n" +
                      " * This class implements a simple Hello World program.\n" +
                      " * \n" +
                      " * ## Main Method\n" +
                      " * \n" +
                      " * The `main` method is the entry point of the application.\n" +
                      " * It prints a greeting message to the console.\n" +
                      " * \n" +
                      " * @param args Command line arguments (not used)\n" +
                      " */";
                
            default:
                return "/**\n" +
                      " * Generated response based on prompt:\n" +
                      " * " + prompt + "\n" +
                      " */\n" +
                      "// No specific handler for type: " + type;
        }
    }
    
    /**
     * Gets usage metrics.
     * @return The usage metrics
     */
    @NotNull
    public UsageMetrics getUsageMetrics() {
        UsageMetrics metrics = new UsageMetrics();
        
        metrics.totalApiRequests = totalApiRequests.get();
        metrics.totalPatternMatches = totalPatternMatches.get();
        metrics.totalTokensGenerated = totalTokensGenerated.get();
        metrics.totalTokensSaved = totalTokensSaved.get();
        
        // Copy request type counts
        for (Map.Entry<String, AtomicInteger> entry : requestsByType.entrySet()) {
            metrics.requestsByType.put(entry.getKey(), entry.getValue().get());
        }
        
        // Calculate approximate cost savings (assume $0.002 per 1000 tokens)
        metrics.estimatedCostSaved = totalTokensSaved.get() * 0.002 / 1000.0;
        
        return metrics;
    }
    
    /**
     * Clears metrics.
     */
    public void clearMetrics() {
        totalApiRequests.set(0);
        totalPatternMatches.set(0);
        totalTokensGenerated.set(0);
        totalTokensSaved.set(0);
        
        for (AtomicInteger counter : requestsByType.values()) {
            counter.set(0);
        }
    }
    
    /**
     * Usage metrics.
     */
    public static class UsageMetrics {
        private int totalApiRequests;
        private int totalPatternMatches;
        private int totalTokensGenerated;
        private int totalTokensSaved;
        private double estimatedCostSaved;
        private final Map<String, Integer> requestsByType = new HashMap<>();
        
        /**
         * Gets the total API requests.
         * @return The total API requests
         */
        public int getTotalApiRequests() {
            return totalApiRequests;
        }
        
        /**
         * Gets the total pattern matches.
         * @return The total pattern matches
         */
        public int getTotalPatternMatches() {
            return totalPatternMatches;
        }
        
        /**
         * Gets the total tokens generated.
         * @return The total tokens generated
         */
        public int getTotalTokensGenerated() {
            return totalTokensGenerated;
        }
        
        /**
         * Gets the total tokens saved.
         * @return The total tokens saved
         */
        public int getTotalTokensSaved() {
            return totalTokensSaved;
        }
        
        /**
         * Gets the estimated cost saved.
         * @return The estimated cost saved
         */
        public double getEstimatedCostSaved() {
            return estimatedCostSaved;
        }
        
        /**
         * Gets the requests by type.
         * @return The requests by type
         */
        @NotNull
        public Map<String, Integer> getRequestsByType() {
            return requestsByType;
        }
    }
}