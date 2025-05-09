package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for pattern recognition to reduce API calls.
 * This service stores and matches patterns for code generation, error fixing, and documentation.
 */
@Service
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    private static final int MAX_PATTERNS = 1000;
    private static final double SIMILARITY_THRESHOLD = 0.8;
    
    // Pattern storage
    private final Map<String, List<CodeGenerationPattern>> codeGenerationPatterns = new ConcurrentHashMap<>();
    private final Map<String, List<ErrorFixPattern>> errorFixPatterns = new ConcurrentHashMap<>();
    private final Map<String, List<DocumentationPattern>> documentationPatterns = new ConcurrentHashMap<>();
    private final Map<String, List<FeatureAdditionPattern>> featureAdditionPatterns = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger apiCalls = new AtomicInteger(0);
    private final AtomicInteger estimatedTokensSaved = new AtomicInteger(0);
    
    /**
     * Gets the instance of the service.
     * @return The pattern recognition service
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Finds a code generation pattern match.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return The generated code or {@code null} if no match found
     */
    @Nullable
    public String findCodeGenerationMatch(@NotNull String prompt, @NotNull String language, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        List<CodeGenerationPattern> patterns = codeGenerationPatterns.getOrDefault(language.toLowerCase(), new ArrayList<>());
        
        for (CodeGenerationPattern pattern : patterns) {
            double similarity = calculateSimilarity(prompt, pattern.getPrompt());
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                patternMatches.incrementAndGet();
                estimatedTokensSaved.addAndGet(calculateEstimatedTokens(prompt));
                
                return pattern.getCode();
            }
        }
        
        apiCalls.incrementAndGet();
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
        
        String key = language.toLowerCase();
        List<CodeGenerationPattern> patterns = codeGenerationPatterns.computeIfAbsent(key, k -> new ArrayList<>());
        
        // Check if a similar pattern already exists
        for (CodeGenerationPattern pattern : patterns) {
            double similarity = calculateSimilarity(prompt, pattern.getPrompt());
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                // Update existing pattern
                pattern.setCode(code);
                pattern.incrementUseCount();
                return;
            }
        }
        
        // Add new pattern
        if (patterns.size() >= MAX_PATTERNS) {
            // Remove least used pattern
            patterns.sort((a, b) -> Integer.compare(a.getUseCount(), b.getUseCount()));
            patterns.remove(0);
        }
        
        patterns.add(new CodeGenerationPattern(prompt, language, code));
    }
    
    /**
     * Finds an error fix pattern match.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return The fixed code or {@code null} if no match found
     */
    @Nullable
    public String findErrorFixMatch(@NotNull String code, @NotNull String errorMessage, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        // Extract error type
        String errorType = extractErrorType(errorMessage);
        List<ErrorFixPattern> patterns = errorFixPatterns.getOrDefault(errorType, new ArrayList<>());
        
        for (ErrorFixPattern pattern : patterns) {
            double codeSimilarity = calculateSimilarity(code, pattern.getOriginalCode());
            double errorSimilarity = calculateSimilarity(errorMessage, pattern.getErrorMessage());
            
            // Both code and error need to be similar
            if (codeSimilarity >= SIMILARITY_THRESHOLD * 0.8 && errorSimilarity >= SIMILARITY_THRESHOLD) {
                patternMatches.incrementAndGet();
                estimatedTokensSaved.addAndGet(calculateEstimatedTokens(code + errorMessage));
                
                return pattern.getFixedCode();
            }
        }
        
        apiCalls.incrementAndGet();
        return null;
    }
    
    /**
     * Stores an error fix pattern.
     * @param originalCode The original code
     * @param errorMessage The error message
     * @param fixedCode The fixed code
     * @param options Additional options
     */
    public void storeErrorFixPattern(@NotNull String originalCode, @NotNull String errorMessage, @NotNull String fixedCode, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        String errorType = extractErrorType(errorMessage);
        List<ErrorFixPattern> patterns = errorFixPatterns.computeIfAbsent(errorType, k -> new ArrayList<>());
        
        // Check if a similar pattern already exists
        for (ErrorFixPattern pattern : patterns) {
            double codeSimilarity = calculateSimilarity(originalCode, pattern.getOriginalCode());
            double errorSimilarity = calculateSimilarity(errorMessage, pattern.getErrorMessage());
            
            if (codeSimilarity >= SIMILARITY_THRESHOLD * 0.8 && errorSimilarity >= SIMILARITY_THRESHOLD) {
                // Update existing pattern
                pattern.setFixedCode(fixedCode);
                pattern.incrementUseCount();
                return;
            }
        }
        
        // Add new pattern
        if (patterns.size() >= MAX_PATTERNS) {
            // Remove least used pattern
            patterns.sort((a, b) -> Integer.compare(a.getUseCount(), b.getUseCount()));
            patterns.remove(0);
        }
        
        patterns.add(new ErrorFixPattern(originalCode, errorMessage, fixedCode));
    }
    
    /**
     * Finds a documentation pattern match.
     * @param code The code to document
     * @param options Additional options
     * @return The documented code or {@code null} if no match found
     */
    @Nullable
    public String findDocumentationMatch(@NotNull String code, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        // Extract code type
        String codeType = extractCodeType(code);
        List<DocumentationPattern> patterns = documentationPatterns.getOrDefault(codeType, new ArrayList<>());
        
        for (DocumentationPattern pattern : patterns) {
            double similarity = calculateSimilarity(code, pattern.getOriginalCode());
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                patternMatches.incrementAndGet();
                estimatedTokensSaved.addAndGet(calculateEstimatedTokens(code));
                
                return pattern.getDocumentedCode();
            }
        }
        
        apiCalls.incrementAndGet();
        return null;
    }
    
    /**
     * Stores a documentation pattern.
     * @param originalCode The original code
     * @param documentedCode The documented code
     * @param options Additional options
     */
    public void storeDocumentationPattern(@NotNull String originalCode, @NotNull String documentedCode, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        String codeType = extractCodeType(originalCode);
        List<DocumentationPattern> patterns = documentationPatterns.computeIfAbsent(codeType, k -> new ArrayList<>());
        
        // Check if a similar pattern already exists
        for (DocumentationPattern pattern : patterns) {
            double similarity = calculateSimilarity(originalCode, pattern.getOriginalCode());
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                // Update existing pattern
                pattern.setDocumentedCode(documentedCode);
                pattern.incrementUseCount();
                return;
            }
        }
        
        // Add new pattern
        if (patterns.size() >= MAX_PATTERNS) {
            // Remove least used pattern
            patterns.sort((a, b) -> Integer.compare(a.getUseCount(), b.getUseCount()));
            patterns.remove(0);
        }
        
        patterns.add(new DocumentationPattern(originalCode, documentedCode));
    }
    
    /**
     * Finds a feature addition pattern match.
     * @param code The code to add features to
     * @param featureDescription The feature description
     * @param options Additional options
     * @return The enhanced code or {@code null} if no match found
     */
    @Nullable
    public String findFeatureAdditionMatch(@NotNull String code, @NotNull String featureDescription, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        // Extract feature type
        String featureType = extractFeatureType(featureDescription);
        List<FeatureAdditionPattern> patterns = featureAdditionPatterns.getOrDefault(featureType, new ArrayList<>());
        
        for (FeatureAdditionPattern pattern : patterns) {
            double codeSimilarity = calculateSimilarity(code, pattern.getOriginalCode());
            double featureSimilarity = calculateSimilarity(featureDescription, pattern.getFeatureDescription());
            
            if (codeSimilarity >= SIMILARITY_THRESHOLD * 0.7 && featureSimilarity >= SIMILARITY_THRESHOLD) {
                patternMatches.incrementAndGet();
                estimatedTokensSaved.addAndGet(calculateEstimatedTokens(code + featureDescription));
                
                return pattern.getEnhancedCode();
            }
        }
        
        apiCalls.incrementAndGet();
        return null;
    }
    
    /**
     * Stores a feature addition pattern.
     * @param originalCode The original code
     * @param featureDescription The feature description
     * @param enhancedCode The enhanced code
     * @param options Additional options
     */
    public void storeFeatureAdditionPattern(@NotNull String originalCode, @NotNull String featureDescription, @NotNull String enhancedCode, @Nullable Map<String, Object> options) {
        if (!isEnabled()) {
            return;
        }
        
        String featureType = extractFeatureType(featureDescription);
        List<FeatureAdditionPattern> patterns = featureAdditionPatterns.computeIfAbsent(featureType, k -> new ArrayList<>());
        
        // Check if a similar pattern already exists
        for (FeatureAdditionPattern pattern : patterns) {
            double codeSimilarity = calculateSimilarity(originalCode, pattern.getOriginalCode());
            double featureSimilarity = calculateSimilarity(featureDescription, pattern.getFeatureDescription());
            
            if (codeSimilarity >= SIMILARITY_THRESHOLD * 0.7 && featureSimilarity >= SIMILARITY_THRESHOLD) {
                // Update existing pattern
                pattern.setEnhancedCode(enhancedCode);
                pattern.incrementUseCount();
                return;
            }
        }
        
        // Add new pattern
        if (patterns.size() >= MAX_PATTERNS) {
            // Remove least used pattern
            patterns.sort((a, b) -> Integer.compare(a.getUseCount(), b.getUseCount()));
            patterns.remove(0);
        }
        
        patterns.add(new FeatureAdditionPattern(originalCode, featureDescription, enhancedCode));
    }
    
    /**
     * Gets statistics for the pattern recognition service.
     * @return The statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("enabled", isEnabled());
        statistics.put("totalRequests", totalRequests.get());
        statistics.put("patternMatches", patternMatches.get());
        statistics.put("apiCalls", apiCalls.get());
        statistics.put("estimatedTokensSaved", estimatedTokensSaved.get());
        statistics.put("estimatedCostSaved", estimatedTokensSaved.get() * 0.00001); // Rough cost estimate
        
        // Pattern counts
        Map<String, Integer> patternCounts = new HashMap<>();
        
        int codeGenPatternCount = 0;
        for (List<CodeGenerationPattern> patterns : codeGenerationPatterns.values()) {
            codeGenPatternCount += patterns.size();
        }
        patternCounts.put("codeGeneration", codeGenPatternCount);
        
        int errorFixPatternCount = 0;
        for (List<ErrorFixPattern> patterns : errorFixPatterns.values()) {
            errorFixPatternCount += patterns.size();
        }
        patternCounts.put("errorFix", errorFixPatternCount);
        
        int docPatternCount = 0;
        for (List<DocumentationPattern> patterns : documentationPatterns.values()) {
            docPatternCount += patterns.size();
        }
        patternCounts.put("documentation", docPatternCount);
        
        int featurePatternCount = 0;
        for (List<FeatureAdditionPattern> patterns : featureAdditionPatterns.values()) {
            featurePatternCount += patterns.size();
        }
        patternCounts.put("featureAddition", featurePatternCount);
        
        statistics.put("patternCounts", patternCounts);
        
        return statistics;
    }
    
    /**
     * Resets all statistics.
     */
    public void resetStatistics() {
        totalRequests.set(0);
        patternMatches.set(0);
        apiCalls.set(0);
        estimatedTokensSaved.set(0);
    }
    
    /**
     * Clears all patterns.
     */
    public void clearPatterns() {
        codeGenerationPatterns.clear();
        errorFixPatterns.clear();
        documentationPatterns.clear();
        featureAdditionPatterns.clear();
    }
    
    /**
     * Checks if the pattern recognition service is enabled.
     * @return {@code true} if the service is enabled, {@code false} otherwise
     */
    private boolean isEnabled() {
        return ModForgeSettings.getInstance().isPatternRecognitionEnabled();
    }
    
    /**
     * Calculates the similarity between two strings.
     * @param str1 The first string
     * @param str2 The second string
     * @return The similarity score between 0.0 and 1.0
     */
    private double calculateSimilarity(@NotNull String str1, @NotNull String str2) {
        // Simple Jaccard similarity for now
        // In a real implementation, this would use more sophisticated techniques like word embeddings
        
        // Normalize
        str1 = str1.toLowerCase().replaceAll("[^a-z0-9]", " ");
        str2 = str2.toLowerCase().replaceAll("[^a-z0-9]", " ");
        
        // Split into words
        String[] words1 = str1.split("\\s+");
        String[] words2 = str2.split("\\s+");
        
        // Count common and total words
        int common = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    common++;
                    break;
                }
            }
        }
        
        int total = words1.length + words2.length - common;
        
        return total > 0 ? (double) common / total : 0.0;
    }
    
    /**
     * Extracts the error type from an error message.
     * @param errorMessage The error message
     * @return The error type
     */
    @NotNull
    private String extractErrorType(@NotNull String errorMessage) {
        // Simple heuristic for now
        // In a real implementation, this would use more sophisticated techniques
        
        errorMessage = errorMessage.toLowerCase();
        
        if (errorMessage.contains("syntax error") || errorMessage.contains("expected") || errorMessage.contains("token")) {
            return "syntax";
        } else if (errorMessage.contains("cannot find symbol") || errorMessage.contains("cannot resolve") || errorMessage.contains("undefined")) {
            return "undefined";
        } else if (errorMessage.contains("incompatible types") || errorMessage.contains("type mismatch")) {
            return "type";
        } else if (errorMessage.contains("null pointer") || errorMessage.contains("nullpointerexception")) {
            return "null";
        } else if (errorMessage.contains("already defined") || errorMessage.contains("duplicate")) {
            return "duplicate";
        } else {
            return "other";
        }
    }
    
    /**
     * Extracts the code type from code.
     * @param code The code
     * @return The code type
     */
    @NotNull
    private String extractCodeType(@NotNull String code) {
        // Simple heuristic for now
        // In a real implementation, this would use more sophisticated techniques
        
        if (code.contains("class ")) {
            return "class";
        } else if (code.contains("interface ")) {
            return "interface";
        } else if (code.contains("enum ")) {
            return "enum";
        } else if (code.contains("public static void main")) {
            return "main";
        } else if (code.contains("void ") || code.contains("return ")) {
            return "method";
        } else {
            return "other";
        }
    }
    
    /**
     * Extracts the feature type from a feature description.
     * @param featureDescription The feature description
     * @return The feature type
     */
    @NotNull
    private String extractFeatureType(@NotNull String featureDescription) {
        // Simple heuristic for now
        // In a real implementation, this would use more sophisticated techniques
        
        featureDescription = featureDescription.toLowerCase();
        
        if (featureDescription.contains("gui") || featureDescription.contains("screen") || featureDescription.contains("render")) {
            return "gui";
        } else if (featureDescription.contains("item") || featureDescription.contains("block")) {
            return "item";
        } else if (featureDescription.contains("entity") || featureDescription.contains("mob")) {
            return "entity";
        } else if (featureDescription.contains("world") || featureDescription.contains("dimension")) {
            return "world";
        } else if (featureDescription.contains("command")) {
            return "command";
        } else {
            return "other";
        }
    }
    
    /**
     * Calculates the estimated number of tokens for a string.
     * @param str The string
     * @return The estimated number of tokens
     */
    private int calculateEstimatedTokens(@NotNull String str) {
        // Rough estimate: 4 characters per token
        return str.length() / 4;
    }
    
    // Pattern classes
    
    /**
     * A code generation pattern.
     */
    private static class CodeGenerationPattern {
        private final String prompt;
        private final String language;
        private String code;
        private int useCount;
        
        /**
         * Creates a new CodeGenerationPattern.
         * @param prompt The prompt
         * @param language The programming language
         * @param code The generated code
         */
        public CodeGenerationPattern(@NotNull String prompt, @NotNull String language, @NotNull String code) {
            this.prompt = prompt;
            this.language = language;
            this.code = code;
            this.useCount = 1;
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
         * Gets the programming language.
         * @return The programming language
         */
        @NotNull
        public String getLanguage() {
            return language;
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
         * Sets the generated code.
         * @param code The generated code
         */
        public void setCode(@NotNull String code) {
            this.code = code;
        }
        
        /**
         * Gets the use count.
         * @return The use count
         */
        public int getUseCount() {
            return useCount;
        }
        
        /**
         * Increments the use count.
         */
        public void incrementUseCount() {
            useCount++;
        }
    }
    
    /**
     * An error fix pattern.
     */
    private static class ErrorFixPattern {
        private final String originalCode;
        private final String errorMessage;
        private String fixedCode;
        private int useCount;
        
        /**
         * Creates a new ErrorFixPattern.
         * @param originalCode The original code
         * @param errorMessage The error message
         * @param fixedCode The fixed code
         */
        public ErrorFixPattern(@NotNull String originalCode, @NotNull String errorMessage, @NotNull String fixedCode) {
            this.originalCode = originalCode;
            this.errorMessage = errorMessage;
            this.fixedCode = fixedCode;
            this.useCount = 1;
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
         * Gets the error message.
         * @return The error message
         */
        @NotNull
        public String getErrorMessage() {
            return errorMessage;
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
         * Sets the fixed code.
         * @param fixedCode The fixed code
         */
        public void setFixedCode(@NotNull String fixedCode) {
            this.fixedCode = fixedCode;
        }
        
        /**
         * Gets the use count.
         * @return The use count
         */
        public int getUseCount() {
            return useCount;
        }
        
        /**
         * Increments the use count.
         */
        public void incrementUseCount() {
            useCount++;
        }
    }
    
    /**
     * A documentation pattern.
     */
    private static class DocumentationPattern {
        private final String originalCode;
        private String documentedCode;
        private int useCount;
        
        /**
         * Creates a new DocumentationPattern.
         * @param originalCode The original code
         * @param documentedCode The documented code
         */
        public DocumentationPattern(@NotNull String originalCode, @NotNull String documentedCode) {
            this.originalCode = originalCode;
            this.documentedCode = documentedCode;
            this.useCount = 1;
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
         * Gets the documented code.
         * @return The documented code
         */
        @NotNull
        public String getDocumentedCode() {
            return documentedCode;
        }
        
        /**
         * Sets the documented code.
         * @param documentedCode The documented code
         */
        public void setDocumentedCode(@NotNull String documentedCode) {
            this.documentedCode = documentedCode;
        }
        
        /**
         * Gets the use count.
         * @return The use count
         */
        public int getUseCount() {
            return useCount;
        }
        
        /**
         * Increments the use count.
         */
        public void incrementUseCount() {
            useCount++;
        }
    }
    
    /**
     * A feature addition pattern.
     */
    private static class FeatureAdditionPattern {
        private final String originalCode;
        private final String featureDescription;
        private String enhancedCode;
        private int useCount;
        
        /**
         * Creates a new FeatureAdditionPattern.
         * @param originalCode The original code
         * @param featureDescription The feature description
         * @param enhancedCode The enhanced code
         */
        public FeatureAdditionPattern(@NotNull String originalCode, @NotNull String featureDescription, @NotNull String enhancedCode) {
            this.originalCode = originalCode;
            this.featureDescription = featureDescription;
            this.enhancedCode = enhancedCode;
            this.useCount = 1;
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
         * Gets the feature description.
         * @return The feature description
         */
        @NotNull
        public String getFeatureDescription() {
            return featureDescription;
        }
        
        /**
         * Gets the enhanced code.
         * @return The enhanced code
         */
        @NotNull
        public String getEnhancedCode() {
            return enhancedCode;
        }
        
        /**
         * Sets the enhanced code.
         * @param enhancedCode The enhanced code
         */
        public void setEnhancedCode(@NotNull String enhancedCode) {
            this.enhancedCode = enhancedCode;
        }
        
        /**
         * Gets the use count.
         * @return The use count
         */
        public int getUseCount() {
            return useCount;
        }
        
        /**
         * Increments the use count.
         */
        public void incrementUseCount() {
            useCount++;
        }
    }
}