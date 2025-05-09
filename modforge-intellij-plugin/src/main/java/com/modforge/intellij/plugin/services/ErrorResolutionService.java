package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.recommendation.PremiumFeatureRecommender;
import com.modforge.intellij.plugin.utils.ApiRequestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for resolving errors in Minecraft mods using machine learning.
 * Leverages pattern learning to minimize API usage and speed up error resolution.
 */
@Service
public final class ErrorResolutionService {
    private static final Logger LOG = Logger.getInstance(ErrorResolutionService.class);
    
    private final Project project;
    private final ModForgeSettingsService settingsService;
    private final AIServiceManager aiServiceManager;
    private final RecommendationService recommendationService;
    private final UserSubscriptionService subscriptionService;
    
    // Error tracking
    private final Map<String, ErrorData> errorCache = new ConcurrentHashMap<>();
    private final List<ErrorResolutionPattern> patternDatabase = new CopyOnWriteArrayList<>();
    private final AtomicInteger totalErrorsResolved = new AtomicInteger(0);
    private final AtomicInteger patternMatchSuccesses = new AtomicInteger(0);
    private final AtomicInteger apiFallbacks = new AtomicInteger(0);
    private final Map<String, Integer> errorTypeStats = new ConcurrentHashMap<>();
    
    /**
     * Creates a new error resolution service.
     * @param project The project
     */
    public ErrorResolutionService(@NotNull Project project) {
        this.project = project;
        this.settingsService = ModForgeSettingsService.getInstance();
        this.aiServiceManager = AIServiceManager.getInstance();
        this.recommendationService = RecommendationService.getInstance(project);
        this.subscriptionService = UserSubscriptionService.getInstance();
        
        // Load patterns from disk (in a real implementation)
        loadPatternDatabase();
        
        LOG.info("Initialized ErrorResolutionService for project: " + project.getName());
    }
    
    /**
     * Gets the instance of this service.
     * @param project The project
     * @return The service instance
     */
    public static ErrorResolutionService getInstance(@NotNull Project project) {
        return project.getService(ErrorResolutionService.class);
    }
    
    /**
     * Resolves an error using machine learning.
     * @param errorMessage The error message
     * @param errorFile The file containing the error
     * @param errorLine The line number of the error
     * @param context The surrounding code context
     * @return The resolution, or null if none could be found
     */
    @Nullable
    public String resolveError(
            @NotNull String errorMessage,
            @NotNull String errorFile,
            int errorLine,
            @NotNull String context
    ) {
        // Record this error type for statistics
        String errorType = classifyErrorType(errorMessage);
        errorTypeStats.merge(errorType, 1, Integer::sum);
        
        // Create error data
        ErrorData errorData = new ErrorData(
                errorMessage,
                errorFile,
                errorLine,
                context,
                errorType
        );
        
        // Check if we've seen this exact error before
        String errorKey = errorData.generateKey();
        if (errorCache.containsKey(errorKey)) {
            LOG.info("Found cached resolution for error: " + errorKey);
            return errorCache.get(errorKey).getResolution();
        }
        
        // Try to find a pattern match
        ErrorResolutionPattern pattern = findMatchingPattern(errorData);
        
        if (pattern != null && !subscriptionService.isPremium()) {
            // For free users, show a recommendation for premium error resolution
            recommendationService.recordFeatureUsage(
                    "advanced_error_resolution",
                    PremiumFeatureRecommender.RecommendationContext.ERROR_RESOLUTION
            );
            
            // 50% chance to show a recommendation when resolving errors
            if (Math.random() < 0.5) {
                recommendationService.considerShowingRecommendation(
                        PremiumFeatureRecommender.RecommendationContext.ERROR_RESOLUTION,
                        null
                );
            }
        }
        
        String resolution;
        
        if (pattern != null) {
            // Pattern match found
            LOG.info("Found pattern match for error: " + errorKey);
            resolution = applyPattern(pattern, errorData);
            patternMatchSuccesses.incrementAndGet();
        } else {
            // Fall back to API
            LOG.info("No pattern match found for error: " + errorKey + ", falling back to API");
            resolution = resolveErrorViaApi(errorData);
            apiFallbacks.incrementAndGet();
        }
        
        if (resolution != null) {
            // Cache the resolution
            errorData.setResolution(resolution);
            errorCache.put(errorKey, errorData);
            
            // Update statistics
            totalErrorsResolved.incrementAndGet();
        }
        
        return resolution;
    }
    
    /**
     * Resolves an error in a file using machine learning.
     * @param file The file containing the error
     * @param errorMessage The error message
     * @param lineNumber The line number of the error
     * @return The resolution, or null if none could be found
     */
    @Nullable
    public String resolveErrorInFile(@NotNull PsiFile file, @NotNull String errorMessage, int lineNumber) {
        // Get the context from the file
        String context = extractContext(file, lineNumber);
        
        return resolveError(
                errorMessage,
                file.getName(),
                lineNumber,
                context
        );
    }
    
    /**
     * Extracts context around an error from a file.
     * @param file The file
     * @param lineNumber The line number
     * @return The context
     */
    private String extractContext(@NotNull PsiFile file, int lineNumber) {
        // In a real implementation, we would extract the code around the error
        String fileText = file.getText();
        
        // Simple implementation - extract 5 lines before and after
        String[] lines = fileText.split("\n");
        int startLine = Math.max(0, lineNumber - 5);
        int endLine = Math.min(lines.length - 1, lineNumber + 5);
        
        StringBuilder context = new StringBuilder();
        for (int i = startLine; i <= endLine; i++) {
            context.append(lines[i]).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Finds a pattern that matches the error.
     * @param errorData The error data
     * @return The matching pattern, or null if none found
     */
    @Nullable
    private ErrorResolutionPattern findMatchingPattern(@NotNull ErrorData errorData) {
        // If user is not premium, limit pattern matching to basic patterns
        boolean isPremium = subscriptionService.isPremium();
        
        // Advanced pattern matching for premium users
        double bestSimilarity = isPremium ? 0.6 : 0.8; // Higher threshold for free users
        ErrorResolutionPattern bestMatch = null;
        
        for (ErrorResolutionPattern pattern : patternDatabase) {
            // Skip advanced patterns for non-premium users
            if (pattern.isAdvanced() && !isPremium) {
                continue;
            }
            
            double similarity = calculatePatternSimilarity(pattern, errorData);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = pattern;
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Calculates the similarity between a pattern and an error.
     * @param pattern The pattern
     * @param errorData The error data
     * @return The similarity score (0-1)
     */
    private double calculatePatternSimilarity(@NotNull ErrorResolutionPattern pattern, @NotNull ErrorData errorData) {
        // Simple implementation - just check if the error types match and the error messages are similar
        if (!pattern.getErrorType().equals(errorData.getErrorType())) {
            return 0.0;
        }
        
        // Compare error messages (simplified implementation)
        String normalizedPatternMessage = normalizeErrorMessage(pattern.getErrorMessage());
        String normalizedErrorMessage = normalizeErrorMessage(errorData.getErrorMessage());
        
        return calculateStringSimilarity(normalizedPatternMessage, normalizedErrorMessage);
    }
    
    /**
     * Normalizes an error message for comparison.
     * @param errorMessage The error message
     * @return The normalized error message
     */
    private String normalizeErrorMessage(@NotNull String errorMessage) {
        // Remove specific details like line numbers, file paths, and variable names
        return errorMessage.replaceAll("\\d+", "NUM")
                .replaceAll("'[^']*'", "VAR")
                .replaceAll("\"[^\"]*\"", "STR")
                .replaceAll("\\w+\\.java", "FILE")
                .toLowerCase();
    }
    
    /**
     * Calculates the similarity between two strings.
     * @param s1 The first string
     * @param s2 The second string
     * @return The similarity score (0-1)
     */
    private double calculateStringSimilarity(@NotNull String s1, @NotNull String s2) {
        // Simple Jaccard similarity for demonstration
        List<String> tokens1 = tokenize(s1);
        List<String> tokens2 = tokenize(s2);
        
        int intersection = 0;
        for (String token : tokens1) {
            if (tokens2.contains(token)) {
                intersection++;
            }
        }
        
        int union = tokens1.size() + tokens2.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }
    
    /**
     * Tokenizes a string.
     * @param s The string
     * @return The tokens
     */
    private List<String> tokenize(@NotNull String s) {
        List<String> tokens = new ArrayList<>();
        for (String token : s.split("\\s+|\\p{Punct}")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
    
    /**
     * Applies a pattern to resolve an error.
     * @param pattern The pattern
     * @param errorData The error data
     * @return The resolution
     */
    private String applyPattern(@NotNull ErrorResolutionPattern pattern, @NotNull ErrorData errorData) {
        // In a real implementation, we would apply the pattern to the error
        // For demonstration, just return the resolution
        return pattern.getResolution();
    }
    
    /**
     * Resolves an error via the API.
     * @param errorData The error data
     * @return The resolution, or null if none could be found
     */
    @Nullable
    private String resolveErrorViaApi(@NotNull ErrorData errorData) {
        // In a real implementation, we would call the API
        // For demonstration, just generate a resolution
        try {
            // Convert error data to JSON
            Map<String, Object> payload = new HashMap<>();
            payload.put("errorMessage", errorData.getErrorMessage());
            payload.put("errorFile", errorData.getErrorFile());
            payload.put("errorLine", errorData.getErrorLine());
            payload.put("context", errorData.getContext());
            payload.put("errorType", errorData.getErrorType());
            
            Gson gson = new Gson();
            String json = gson.toJson(payload);
            
            // Make API request
            String apiUrl = settingsService.getApiUrl() + "/api/errors/resolve";
            String apiKey = settingsService.getApiKey();
            
            if (apiKey == null || apiKey.isEmpty()) {
                LOG.warn("Cannot resolve error via API without API key");
                return null;
            }
            
            String response = ApiRequestUtil.post(apiUrl, json, apiKey);
            if (response == null) {
                LOG.warn("Error resolution API returned null response");
                return null;
            }
            
            // Parse response
            Map<String, Object> result = gson.fromJson(response, Map.class);
            String resolution = (String) result.get("resolution");
            
            // If resolution is successful, learn the pattern
            if (resolution != null && !resolution.isEmpty()) {
                learnErrorResolutionPattern(errorData, resolution);
            }
            
            return resolution;
        } catch (Exception e) {
            LOG.error("Error resolving error via API", e);
            return null;
        }
    }
    
    /**
     * Learns a new error resolution pattern.
     * @param errorData The error data
     * @param resolution The resolution
     */
    private void learnErrorResolutionPattern(@NotNull ErrorData errorData, @NotNull String resolution) {
        // Create a new pattern
        ErrorResolutionPattern pattern = new ErrorResolutionPattern(
                errorData.getErrorType(),
                errorData.getErrorMessage(),
                resolution,
                false, // Not advanced
                1 // Initial success count
        );
        
        // Add to database
        patternDatabase.add(pattern);
        
        // In a real implementation, we would save the pattern database to disk
        
        LOG.info("Learned new error resolution pattern for error type: " + errorData.getErrorType());
    }
    
    /**
     * Classifies an error message into a type.
     * @param errorMessage The error message
     * @return The error type
     */
    private String classifyErrorType(@NotNull String errorMessage) {
        // Simple classification based on keywords
        if (errorMessage.contains("cannot find symbol") || errorMessage.contains("cannot be resolved")) {
            return "SYMBOL_NOT_FOUND";
        } else if (errorMessage.contains("incompatible types")) {
            return "TYPE_MISMATCH";
        } else if (errorMessage.contains("is already defined")) {
            return "DUPLICATE_DEFINITION";
        } else if (errorMessage.contains("method") && errorMessage.contains("not found")) {
            return "METHOD_NOT_FOUND";
        } else if (errorMessage.contains("is not abstract and does not override abstract method")) {
            return "ABSTRACT_METHOD_NOT_IMPLEMENTED";
        } else if (errorMessage.contains("no suitable constructor found")) {
            return "CONSTRUCTOR_NOT_FOUND";
        } else if (errorMessage.contains("unreported exception")) {
            return "UNCAUGHT_EXCEPTION";
        } else if (errorMessage.contains("cannot be applied to")) {
            return "METHOD_ARGUMENT_MISMATCH";
        } else if (errorMessage.contains("missing return statement")) {
            return "MISSING_RETURN";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * Loads the pattern database from disk.
     */
    private void loadPatternDatabase() {
        // In a real implementation, we would load the pattern database from disk or API
        // For demonstration, just add some sample patterns
        
        // Add sample patterns for common Minecraft mod errors
        patternDatabase.add(new ErrorResolutionPattern(
                "SYMBOL_NOT_FOUND",
                "cannot find symbol class IEventBus",
                "You need to import 'net.minecraftforge.eventbus.api.IEventBus'. Add this import at the top of your file.",
                false,
                10
        ));
        
        patternDatabase.add(new ErrorResolutionPattern(
                "SYMBOL_NOT_FOUND",
                "cannot find symbol class FMLClientSetupEvent",
                "You need to import 'net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent'. Add this import at the top of your file.",
                false,
                8
        ));
        
        patternDatabase.add(new ErrorResolutionPattern(
                "METHOD_NOT_FOUND",
                "cannot find method registerRenderLayer",
                "For Minecraft 1.16+, use 'ItemBlockRenderTypes.setRenderLayer()' instead of 'RenderTypeLookup.setRenderLayer()'.",
                true,
                5
        ));
        
        patternDatabase.add(new ErrorResolutionPattern(
                "ABSTRACT_METHOD_NOT_IMPLEMENTED",
                "is not abstract and does not override abstract method onInitializeClient",
                "You need to implement the 'onInitializeClient()' method for Fabric mods. Add this method to your class.",
                false,
                12
        ));
        
        LOG.info("Loaded " + patternDatabase.size() + " error resolution patterns");
    }
    
    /**
     * Gets statistics about error resolution.
     * @return The statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalErrorsResolved", totalErrorsResolved.get());
        statistics.put("patternMatchSuccesses", patternMatchSuccesses.get());
        statistics.put("apiFallbacks", apiFallbacks.get());
        statistics.put("errorTypeStats", new HashMap<>(errorTypeStats));
        statistics.put("patternDatabaseSize", patternDatabase.size());
        
        // Calculate success rate
        int total = patternMatchSuccesses.get() + apiFallbacks.get();
        double successRate = total == 0 ? 0 : (double) patternMatchSuccesses.get() / total;
        statistics.put("patternMatchSuccessRate", successRate);
        
        return statistics;
    }
    
    /**
     * Class representing error data.
     */
    private static class ErrorData {
        private final String errorMessage;
        private final String errorFile;
        private final int errorLine;
        private final String context;
        private final String errorType;
        private String resolution;
        
        /**
         * Creates new error data.
         * @param errorMessage The error message
         * @param errorFile The file containing the error
         * @param errorLine The line number of the error
         * @param context The surrounding code context
         * @param errorType The error type
         */
        public ErrorData(
                @NotNull String errorMessage,
                @NotNull String errorFile,
                int errorLine,
                @NotNull String context,
                @NotNull String errorType
        ) {
            this.errorMessage = errorMessage;
            this.errorFile = errorFile;
            this.errorLine = errorLine;
            this.context = context;
            this.errorType = errorType;
        }
        
        /**
         * Gets the error message.
         * @return The error message
         */
        @NotNull
        public String getErrorMessage() {
            return errorMessage;
        }
        
        /**
         * Gets the file containing the error.
         * @return The file
         */
        @NotNull
        public String getErrorFile() {
            return errorFile;
        }
        
        /**
         * Gets the line number of the error.
         * @return The line number
         */
        public int getErrorLine() {
            return errorLine;
        }
        
        /**
         * Gets the surrounding code context.
         * @return The context
         */
        @NotNull
        public String getContext() {
            return context;
        }
        
        /**
         * Gets the error type.
         * @return The error type
         */
        @NotNull
        public String getErrorType() {
            return errorType;
        }
        
        /**
         * Gets the resolution.
         * @return The resolution
         */
        @Nullable
        public String getResolution() {
            return resolution;
        }
        
        /**
         * Sets the resolution.
         * @param resolution The resolution
         */
        public void setResolution(@Nullable String resolution) {
            this.resolution = resolution;
        }
        
        /**
         * Generates a key for the error.
         * @return The key
         */
        @NotNull
        public String generateKey() {
            return errorType + ":" + errorMessage.hashCode();
        }
    }
    
    /**
     * Class representing an error resolution pattern.
     */
    private static class ErrorResolutionPattern {
        private final String errorType;
        private final String errorMessage;
        private final String resolution;
        private final boolean advanced;
        private int successCount;
        
        /**
         * Creates a new error resolution pattern.
         * @param errorType The error type
         * @param errorMessage The error message
         * @param resolution The resolution
         * @param advanced Whether this is an advanced pattern
         * @param successCount The initial success count
         */
        public ErrorResolutionPattern(
                @NotNull String errorType,
                @NotNull String errorMessage,
                @NotNull String resolution,
                boolean advanced,
                int successCount
        ) {
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.resolution = resolution;
            this.advanced = advanced;
            this.successCount = successCount;
        }
        
        /**
         * Gets the error type.
         * @return The error type
         */
        @NotNull
        public String getErrorType() {
            return errorType;
        }
        
        /**
         * Gets the error message.
         * @return The error message
         */
        @NotNull
        public String getErrorMessage() {
            return errorMessage;
        }
        
        /**
         * Gets the resolution.
         * @return The resolution
         */
        @NotNull
        public String getResolution() {
            return resolution;
        }
        
        /**
         * Checks if this is an advanced pattern.
         * @return Whether this is an advanced pattern
         */
        public boolean isAdvanced() {
            return advanced;
        }
        
        /**
         * Gets the success count.
         * @return The success count
         */
        public int getSuccessCount() {
            return successCount;
        }
        
        /**
         * Increments the success count.
         */
        public void incrementSuccessCount() {
            this.successCount++;
        }
    }
}