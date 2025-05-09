package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

/**
 * Service for pattern recognition to reduce API usage.
 * This service stores and matches patterns to avoid redundant API calls.
 */
@Service
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.85;
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.75;
    private static final int MAX_PATTERN_HISTORY = 1000;
    
    // Code generation patterns
    private final Map<String, List<CodeGenerationPattern>> codeGenerationPatterns = new ConcurrentHashMap<>();
    
    // Error fix patterns
    private final Map<String, List<ErrorFixPattern>> errorFixPatterns = new ConcurrentHashMap<>();
    
    // Documentation generation patterns
    private final Map<String, List<DocumentationPattern>> documentationPatterns = new ConcurrentHashMap<>();
    
    // Feature addition patterns
    private final Map<String, List<FeatureAdditionPattern>> featureAdditionPatterns = new ConcurrentHashMap<>();
    
    // Metrics
    private final PatternMetrics metrics = new PatternMetrics();
    
    // Thread pool for background operations
    private final ExecutorService executorService = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge-PatternRecognition",
            4
    );
    
    private boolean enabled = true;
    
    /**
     * Gets the instance of the service.
     * @return The pattern recognition service
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return {@code true} if pattern recognition is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return enabled && ModForgeSettings.getInstance().isUsePatternRecognition();
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param enabled {@code true} to enable pattern recognition, {@code false} to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Gets statistics for the pattern recognition service.
     * @return The statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Pattern counts
        int codePatternCount = codeGenerationPatterns.values().stream().mapToInt(List::size).sum();
        int errorPatternCount = errorFixPatterns.values().stream().mapToInt(List::size).sum();
        int documentationPatternCount = documentationPatterns.values().stream().mapToInt(List::size).sum();
        int featurePatternCount = featureAdditionPatterns.values().stream().mapToInt(List::size).sum();
        int totalPatternCount = codePatternCount + errorPatternCount + documentationPatternCount + featurePatternCount;
        
        statistics.put("codePatternCount", codePatternCount);
        statistics.put("errorPatternCount", errorPatternCount);
        statistics.put("documentationPatternCount", documentationPatternCount);
        statistics.put("featurePatternCount", featurePatternCount);
        statistics.put("totalPatternCount", totalPatternCount);
        
        // Metrics
        synchronized (metrics) {
            statistics.put("totalRequests", metrics.getTotalRequests());
            statistics.put("patternMatches", metrics.getPatternMatches());
            statistics.put("apiCalls", metrics.getApiCalls());
            statistics.put("estimatedTokensSaved", metrics.getEstimatedTokensSaved());
            statistics.put("estimatedCostSaved", metrics.getEstimatedCostSaved());
            
            double successRate = metrics.getTotalRequests() > 0
                    ? (double) metrics.getPatternMatches() / metrics.getTotalRequests()
                    : 0.0;
            statistics.put("successRate", successRate);
        }
        
        return statistics;
    }
    
    /**
     * Clears all patterns.
     */
    public void clearPatterns() {
        codeGenerationPatterns.clear();
        errorFixPatterns.clear();
        documentationPatterns.clear();
        featureAdditionPatterns.clear();
        
        LOG.info("All patterns cleared");
    }
    
    /**
     * Gets a code generation result from patterns or returns {@code null} if no match is found.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return The generated code or {@code null} if no match is found
     */
    @Nullable
    public String findCodeGenerationMatch(@NotNull String prompt, @NotNull String language, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        synchronized (metrics) {
            metrics.incrementTotalRequests();
        }
        
        // Try to find a match in the patterns
        String normalizedPrompt = normalizePrompt(prompt);
        String key = language.toLowerCase();
        
        List<CodeGenerationPattern> patterns = codeGenerationPatterns.getOrDefault(key, Collections.emptyList());
        
        CodeGenerationPattern bestMatch = findBestMatch(patterns, normalizedPrompt, CodeGenerationPattern::getSimilarity);
        
        if (bestMatch != null && bestMatch.getConfidence() >= DEFAULT_CONFIDENCE_THRESHOLD) {
            synchronized (metrics) {
                metrics.incrementPatternMatches();
                metrics.incrementEstimatedTokensSaved(estimateTokensSaved(prompt, bestMatch.getCode()));
            }
            
            LOG.info("Found code generation pattern match with confidence " + bestMatch.getConfidence());
            
            return bestMatch.getCode();
        }
        
        LOG.info("No code generation pattern match found");
        
        synchronized (metrics) {
            metrics.incrementApiCalls();
        }
        
        return null;
    }
    
    /**
     * Stores a code generation pattern.
     * @param prompt The prompt
     * @param language The programming language
     * @param code The generated code
     * @param options Additional options
     */
    public void storeCodeGenerationPattern(@NotNull String prompt, @NotNull String language, @NotNull String code, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                String normalizedPrompt = normalizePrompt(prompt);
                String key = language.toLowerCase();
                
                // Create pattern
                CodeGenerationPattern pattern = new CodeGenerationPattern(normalizedPrompt, code, 1.0);
                
                // Store pattern
                codeGenerationPatterns.computeIfAbsent(key, k -> new ArrayList<>()).add(pattern);
                
                // Trim patterns if necessary
                trimPatterns(key, codeGenerationPatterns);
                
                LOG.info("Stored code generation pattern for language: " + language);
            } catch (Exception e) {
                LOG.error("Error storing code generation pattern", e);
            }
        });
    }
    
    /**
     * Gets an error fix result from patterns or returns {@code null} if no match is found.
     * @param code The code with errors
     * @param errorMessage The error message
     * @param options Additional options
     * @return The fixed code or {@code null} if no match is found
     */
    @Nullable
    public String findErrorFixMatch(@NotNull String code, @NotNull String errorMessage, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        synchronized (metrics) {
            metrics.incrementTotalRequests();
        }
        
        // Try to find a match in the patterns
        String normalizedError = normalizeErrorMessage(errorMessage);
        String key = normalizedError;
        
        List<ErrorFixPattern> patterns = errorFixPatterns.getOrDefault(key, Collections.emptyList());
        
        ErrorFixPattern bestMatch = findBestMatch(patterns, code, (pattern, inputCode) -> {
            return calculateCodeSimilarity(pattern.getOriginalCode(), inputCode);
        });
        
        if (bestMatch != null && bestMatch.getConfidence() >= DEFAULT_CONFIDENCE_THRESHOLD) {
            synchronized (metrics) {
                metrics.incrementPatternMatches();
                metrics.incrementEstimatedTokensSaved(estimateTokensSaved(code, bestMatch.getFixedCode()));
            }
            
            LOG.info("Found error fix pattern match with confidence " + bestMatch.getConfidence());
            
            return bestMatch.getFixedCode();
        }
        
        LOG.info("No error fix pattern match found");
        
        synchronized (metrics) {
            metrics.incrementApiCalls();
        }
        
        return null;
    }
    
    /**
     * Stores an error fix pattern.
     * @param code The code with errors
     * @param errorMessage The error message
     * @param fixedCode The fixed code
     * @param options Additional options
     */
    public void storeErrorFixPattern(@NotNull String code, @NotNull String errorMessage, @NotNull String fixedCode, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                String normalizedError = normalizeErrorMessage(errorMessage);
                String key = normalizedError;
                
                // Create pattern
                ErrorFixPattern pattern = new ErrorFixPattern(code, fixedCode, normalizedError, 1.0);
                
                // Store pattern
                errorFixPatterns.computeIfAbsent(key, k -> new ArrayList<>()).add(pattern);
                
                // Trim patterns if necessary
                trimPatterns(key, errorFixPatterns);
                
                LOG.info("Stored error fix pattern for error: " + normalizedError);
            } catch (Exception e) {
                LOG.error("Error storing error fix pattern", e);
            }
        });
    }
    
    /**
     * Gets a documentation generation result from patterns or returns {@code null} if no match is found.
     * @param code The code to document
     * @param options Additional options
     * @return The documented code or {@code null} if no match is found
     */
    @Nullable
    public String findDocumentationMatch(@NotNull String code, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        synchronized (metrics) {
            metrics.incrementTotalRequests();
        }
        
        // Try to find a match in the patterns
        String codeFingerprint = generateCodeFingerprint(code);
        String key = codeFingerprint;
        
        List<DocumentationPattern> patterns = documentationPatterns.getOrDefault(key, Collections.emptyList());
        
        DocumentationPattern bestMatch = findBestMatch(patterns, code, (pattern, inputCode) -> {
            return calculateCodeSimilarity(pattern.getOriginalCode(), inputCode);
        });
        
        if (bestMatch != null && bestMatch.getConfidence() >= DEFAULT_CONFIDENCE_THRESHOLD) {
            synchronized (metrics) {
                metrics.incrementPatternMatches();
                metrics.incrementEstimatedTokensSaved(estimateTokensSaved(code, bestMatch.getDocumentedCode()));
            }
            
            LOG.info("Found documentation pattern match with confidence " + bestMatch.getConfidence());
            
            return bestMatch.getDocumentedCode();
        }
        
        LOG.info("No documentation pattern match found");
        
        synchronized (metrics) {
            metrics.incrementApiCalls();
        }
        
        return null;
    }
    
    /**
     * Stores a documentation pattern.
     * @param code The code to document
     * @param documentedCode The documented code
     * @param options Additional options
     */
    public void storeDocumentationPattern(@NotNull String code, @NotNull String documentedCode, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                String codeFingerprint = generateCodeFingerprint(code);
                String key = codeFingerprint;
                
                // Create pattern
                DocumentationPattern pattern = new DocumentationPattern(code, documentedCode, 1.0);
                
                // Store pattern
                documentationPatterns.computeIfAbsent(key, k -> new ArrayList<>()).add(pattern);
                
                // Trim patterns if necessary
                trimPatterns(key, documentationPatterns);
                
                LOG.info("Stored documentation pattern for code fingerprint: " + codeFingerprint);
            } catch (Exception e) {
                LOG.error("Error storing documentation pattern", e);
            }
        });
    }
    
    /**
     * Gets a feature addition result from patterns or returns {@code null} if no match is found.
     * @param code The code to add features to
     * @param featureDescription The feature description
     * @param options Additional options
     * @return The code with added features or {@code null} if no match is found
     */
    @Nullable
    public String findFeatureAdditionMatch(@NotNull String code, @NotNull String featureDescription, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        synchronized (metrics) {
            metrics.incrementTotalRequests();
        }
        
        // Try to find a match in the patterns
        String normalizedDescription = normalizeFeatureDescription(featureDescription);
        String key = normalizedDescription;
        
        List<FeatureAdditionPattern> patterns = featureAdditionPatterns.getOrDefault(key, Collections.emptyList());
        
        FeatureAdditionPattern bestMatch = findBestMatch(patterns, code, (pattern, inputCode) -> {
            return calculateCodeSimilarity(pattern.getOriginalCode(), inputCode);
        });
        
        if (bestMatch != null && bestMatch.getConfidence() >= DEFAULT_CONFIDENCE_THRESHOLD) {
            synchronized (metrics) {
                metrics.incrementPatternMatches();
                metrics.incrementEstimatedTokensSaved(estimateTokensSaved(code, bestMatch.getEnhancedCode()));
            }
            
            LOG.info("Found feature addition pattern match with confidence " + bestMatch.getConfidence());
            
            return bestMatch.getEnhancedCode();
        }
        
        LOG.info("No feature addition pattern match found");
        
        synchronized (metrics) {
            metrics.incrementApiCalls();
        }
        
        return null;
    }
    
    /**
     * Stores a feature addition pattern.
     * @param code The code to add features to
     * @param featureDescription The feature description
     * @param enhancedCode The code with added features
     * @param options Additional options
     */
    public void storeFeatureAdditionPattern(@NotNull String code, @NotNull String featureDescription, @NotNull String enhancedCode, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                String normalizedDescription = normalizeFeatureDescription(featureDescription);
                String key = normalizedDescription;
                
                // Create pattern
                FeatureAdditionPattern pattern = new FeatureAdditionPattern(code, enhancedCode, normalizedDescription, 1.0);
                
                // Store pattern
                featureAdditionPatterns.computeIfAbsent(key, k -> new ArrayList<>()).add(pattern);
                
                // Trim patterns if necessary
                trimPatterns(key, featureAdditionPatterns);
                
                LOG.info("Stored feature addition pattern for description: " + normalizedDescription);
            } catch (Exception e) {
                LOG.error("Error storing feature addition pattern", e);
            }
        });
    }
    
    /**
     * Normalizes a prompt by removing whitespace, punctuation, and converting to lowercase.
     * @param prompt The prompt
     * @return The normalized prompt
     */
    @NotNull
    private String normalizePrompt(@NotNull String prompt) {
        // Remove whitespace and convert to lowercase
        return prompt.trim().toLowerCase().replaceAll("\\s+", " ");
    }
    
    /**
     * Normalizes an error message by extracting the error type and message.
     * @param errorMessage The error message
     * @return The normalized error message
     */
    @NotNull
    private String normalizeErrorMessage(@NotNull String errorMessage) {
        // Extract error type and message, remove line numbers and file paths
        String normalized = errorMessage.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("at line \\d+", "")
                .replaceAll("in file .*?:", "")
                .replaceAll("\\[.*?\\]", "");
        
        return normalized.toLowerCase();
    }
    
    /**
     * Normalizes a feature description by removing whitespace and converting to lowercase.
     * @param featureDescription The feature description
     * @return The normalized feature description
     */
    @NotNull
    private String normalizeFeatureDescription(@NotNull String featureDescription) {
        // Remove whitespace and convert to lowercase
        return featureDescription.trim().toLowerCase().replaceAll("\\s+", " ");
    }
    
    /**
     * Generates a fingerprint for code based on its structure.
     * @param code The code
     * @return The code fingerprint
     */
    @NotNull
    private String generateCodeFingerprint(@NotNull String code) {
        // Count occurrences of key structures
        int classCount = countOccurrences(code, "class ");
        int methodCount = countOccurrences(code, "public ") + countOccurrences(code, "private ") + countOccurrences(code, "protected ");
        int fieldCount = countOccurrences(code, "private ") - countOccurrences(code, "private void");
        int importCount = countOccurrences(code, "import ");
        
        // Create fingerprint
        return "C" + classCount + "M" + methodCount + "F" + fieldCount + "I" + importCount;
    }
    
    /**
     * Counts occurrences of a pattern in text.
     * @param text The text
     * @param pattern The pattern
     * @return The number of occurrences
     */
    private int countOccurrences(@NotNull String text, @NotNull String pattern) {
        int count = 0;
        int index = text.indexOf(pattern);
        
        while (index != -1) {
            count++;
            index = text.indexOf(pattern, index + pattern.length());
        }
        
        return count;
    }
    
    /**
     * Calculates the similarity between two code snippets.
     * @param code1 The first code snippet
     * @param code2 The second code snippet
     * @return The similarity (0.0 to 1.0)
     */
    private double calculateCodeSimilarity(@NotNull String code1, @NotNull String code2) {
        // Simple similarity based on shared lines
        Set<String> lines1 = new HashSet<>(Arrays.asList(code1.split("\n")));
        Set<String> lines2 = new HashSet<>(Arrays.asList(code2.split("\n")));
        
        // Calculate Jaccard similarity
        Set<String> union = new HashSet<>(lines1);
        union.addAll(lines2);
        
        Set<String> intersection = new HashSet<>(lines1);
        intersection.retainAll(lines2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Finds the best match from a list of patterns.
     * @param patterns The patterns
     * @param input The input to match
     * @param similarityFunction The function to calculate similarity
     * @param <T> The pattern type
     * @return The best match or {@code null} if no match is found
     */
    @Nullable
    private <T extends Pattern> T findBestMatch(
            @NotNull List<T> patterns,
            @NotNull String input,
            @NotNull BiFunction<T, String, Double> similarityFunction
    ) {
        T bestMatch = null;
        double bestSimilarity = DEFAULT_SIMILARITY_THRESHOLD;
        
        for (T pattern : patterns) {
            double similarity = similarityFunction.apply(pattern, input);
            
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = pattern;
            }
        }
        
        if (bestMatch != null) {
            bestMatch.setConfidence(bestSimilarity);
        }
        
        return bestMatch;
    }
    
    /**
     * Estimates the number of tokens saved by using a pattern.
     * @param input The input
     * @param output The output
     * @return The estimated number of tokens saved
     */
    private int estimateTokensSaved(@NotNull String input, @NotNull String output) {
        // Rough estimate: 1 token ≈ 4 characters
        int inputTokens = input.length() / 4;
        int outputTokens = output.length() / 4;
        
        return inputTokens + outputTokens;
    }
    
    /**
     * Trims patterns to the maximum history size.
     * @param key The key
     * @param patterns The patterns map
     * @param <T> The pattern type
     */
    private <T extends Pattern> void trimPatterns(@NotNull String key, @NotNull Map<String, List<T>> patterns) {
        List<T> list = patterns.get(key);
        
        if (list != null && list.size() > MAX_PATTERN_HISTORY) {
            patterns.put(key, list.subList(list.size() - MAX_PATTERN_HISTORY, list.size()));
        }
    }
    
    /**
     * Base class for all patterns.
     */
    private abstract static class Pattern {
        private double confidence;
        
        /**
         * Gets the confidence of the pattern match.
         * @return The confidence (0.0 to 1.0)
         */
        public double getConfidence() {
            return confidence;
        }
        
        /**
         * Sets the confidence of the pattern match.
         * @param confidence The confidence (0.0 to 1.0)
         */
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
    }
    
    /**
     * Pattern for code generation.
     */
    private static final class CodeGenerationPattern extends Pattern {
        private final String prompt;
        private final String code;
        
        /**
         * Creates a new CodeGenerationPattern.
         * @param prompt The prompt
         * @param code The generated code
         * @param confidence The confidence
         */
        public CodeGenerationPattern(@NotNull String prompt, @NotNull String code, double confidence) {
            this.prompt = prompt;
            this.code = code;
            setConfidence(confidence);
        }
        
        /**
         * Gets the prompt.
         * @return The prompt
         */
        @NotNull
        public String getPrompt() {
            return prompt;
        }
        
        /**
         * Gets the generated code.
         * @return The generated code
         */
        @NotNull
        public String getCode() {
            return code;
        }
        
        /**
         * Calculates the similarity between this pattern's prompt and the input prompt.
         * @param inputPrompt The input prompt
         * @return The similarity (0.0 to 1.0)
         */
        public double getSimilarity(@NotNull String inputPrompt) {
            // Simple similarity based on word overlap
            Set<String> words1 = new HashSet<>(Arrays.asList(prompt.split("\\s+")));
            Set<String> words2 = new HashSet<>(Arrays.asList(inputPrompt.split("\\s+")));
            
            // Calculate Jaccard similarity
            Set<String> union = new HashSet<>(words1);
            union.addAll(words2);
            
            Set<String> intersection = new HashSet<>(words1);
            intersection.retainAll(words2);
            
            return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        }
    }
    
    /**
     * Pattern for error fixes.
     */
    private static final class ErrorFixPattern extends Pattern {
        private final String originalCode;
        private final String fixedCode;
        private final String errorMessage;
        
        /**
         * Creates a new ErrorFixPattern.
         * @param originalCode The original code with errors
         * @param fixedCode The fixed code
         * @param errorMessage The error message
         * @param confidence The confidence
         */
        public ErrorFixPattern(@NotNull String originalCode, @NotNull String fixedCode, @NotNull String errorMessage, double confidence) {
            this.originalCode = originalCode;
            this.fixedCode = fixedCode;
            this.errorMessage = errorMessage;
            setConfidence(confidence);
        }
        
        /**
         * Gets the original code with errors.
         * @return The original code with errors
         */
        @NotNull
        public String getOriginalCode() {
            return originalCode;
        }
        
        /**
         * Gets the fixed code.
         * @return The fixed code
         */
        @NotNull
        public String getFixedCode() {
            return fixedCode;
        }
        
        /**
         * Gets the error message.
         * @return The error message
         */
        @NotNull
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Pattern for documentation generation.
     */
    private static final class DocumentationPattern extends Pattern {
        private final String originalCode;
        private final String documentedCode;
        
        /**
         * Creates a new DocumentationPattern.
         * @param originalCode The original code without documentation
         * @param documentedCode The documented code
         * @param confidence The confidence
         */
        public DocumentationPattern(@NotNull String originalCode, @NotNull String documentedCode, double confidence) {
            this.originalCode = originalCode;
            this.documentedCode = documentedCode;
            setConfidence(confidence);
        }
        
        /**
         * Gets the original code without documentation.
         * @return The original code without documentation
         */
        @NotNull
        public String getOriginalCode() {
            return originalCode;
        }
        
        /**
         * Gets the documented code.
         * @return The documented code
         */
        @NotNull
        public String getDocumentedCode() {
            return documentedCode;
        }
    }
    
    /**
     * Pattern for feature addition.
     */
    private static final class FeatureAdditionPattern extends Pattern {
        private final String originalCode;
        private final String enhancedCode;
        private final String featureDescription;
        
        /**
         * Creates a new FeatureAdditionPattern.
         * @param originalCode The original code
         * @param enhancedCode The enhanced code with new features
         * @param featureDescription The feature description
         * @param confidence The confidence
         */
        public FeatureAdditionPattern(@NotNull String originalCode, @NotNull String enhancedCode, @NotNull String featureDescription, double confidence) {
            this.originalCode = originalCode;
            this.enhancedCode = enhancedCode;
            this.featureDescription = featureDescription;
            setConfidence(confidence);
        }
        
        /**
         * Gets the original code.
         * @return The original code
         */
        @NotNull
        public String getOriginalCode() {
            return originalCode;
        }
        
        /**
         * Gets the enhanced code with new features.
         * @return The enhanced code
         */
        @NotNull
        public String getEnhancedCode() {
            return enhancedCode;
        }
        
        /**
         * Gets the feature description.
         * @return The feature description
         */
        @NotNull
        public String getFeatureDescription() {
            return featureDescription;
        }
    }
    
    /**
     * Metrics for pattern recognition service.
     */
    private static final class PatternMetrics {
        private int totalRequests = 0;
        private int patternMatches = 0;
        private int apiCalls = 0;
        private int estimatedTokensSaved = 0;
        
        /**
         * Gets the total number of requests.
         * @return The total number of requests
         */
        public int getTotalRequests() {
            return totalRequests;
        }
        
        /**
         * Increments the total number of requests.
         */
        public void incrementTotalRequests() {
            totalRequests++;
        }
        
        /**
         * Gets the number of pattern matches.
         * @return The number of pattern matches
         */
        public int getPatternMatches() {
            return patternMatches;
        }
        
        /**
         * Increments the number of pattern matches.
         */
        public void incrementPatternMatches() {
            patternMatches++;
        }
        
        /**
         * Gets the number of API calls.
         * @return The number of API calls
         */
        public int getApiCalls() {
            return apiCalls;
        }
        
        /**
         * Increments the number of API calls.
         */
        public void incrementApiCalls() {
            apiCalls++;
        }
        
        /**
         * Gets the estimated number of tokens saved.
         * @return The estimated number of tokens saved
         */
        public int getEstimatedTokensSaved() {
            return estimatedTokensSaved;
        }
        
        /**
         * Increments the estimated number of tokens saved.
         * @param tokens The number of tokens saved
         */
        public void incrementEstimatedTokensSaved(int tokens) {
            estimatedTokensSaved += tokens;
        }
        
        /**
         * Gets the estimated cost saved (in USD).
         * @return The estimated cost saved
         */
        public double getEstimatedCostSaved() {
            // Rough estimate: $0.002 per 1000 tokens for GPT-3.5-turbo
            return estimatedTokensSaved * 0.002 / 1000.0;
        }
    }
}