package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that provides pattern recognition capabilities.
 * This service is responsible for recognizing and storing patterns to reduce API calls.
 */
@Service(Service.Level.APP)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    private static final int MAX_PATTERNS = 1000;
    private static final double SIMILARITY_THRESHOLD = 0.8;
    
    private final Map<String, List<Pattern>> codePatterns = new ConcurrentHashMap<>();
    private final Map<String, List<Pattern>> errorPatterns = new ConcurrentHashMap<>();
    private final Map<String, List<Pattern>> documentationPatterns = new ConcurrentHashMap<>();
    
    private boolean enabled = true;
    
    /**
     * Represents a recognized pattern.
     */
    public static class Pattern {
        private final String type;
        private final String input;
        private final String output;
        private final Map<String, Object> metadata;
        private int usageCount;
        private int successCount;
        private int failureCount;
        
        /**
         * Creates a new Pattern.
         * @param type The pattern type
         * @param input The input
         * @param output The output
         * @param metadata Additional metadata
         */
        public Pattern(@NotNull String type, @NotNull String input, @NotNull String output, @Nullable Map<String, Object> metadata) {
            this.type = type;
            this.input = input;
            this.output = output;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.usageCount = 0;
            this.successCount = 0;
            this.failureCount = 0;
        }
        
        /**
         * Gets the pattern type.
         * @return The pattern type
         */
        @NotNull
        public String getType() {
            return type;
        }
        
        /**
         * Gets the input.
         * @return The input
         */
        @NotNull
        public String getInput() {
            return input;
        }
        
        /**
         * Gets the output.
         * @return The output
         */
        @NotNull
        public String getOutput() {
            return output;
        }
        
        /**
         * Gets the metadata.
         * @return The metadata
         */
        @NotNull
        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }
        
        /**
         * Gets the usage count.
         * @return The usage count
         */
        public int getUsageCount() {
            return usageCount;
        }
        
        /**
         * Gets the success count.
         * @return The success count
         */
        public int getSuccessCount() {
            return successCount;
        }
        
        /**
         * Gets the failure count.
         * @return The failure count
         */
        public int getFailureCount() {
            return failureCount;
        }
        
        /**
         * Gets the success rate.
         * @return The success rate (0-1)
         */
        public double getSuccessRate() {
            if (usageCount == 0) {
                return 0;
            }
            
            return (double) successCount / usageCount;
        }
        
        /**
         * Records a pattern result.
         * @param success Whether the pattern was successful
         */
        public void recordResult(boolean success) {
            usageCount++;
            
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
        }
    }
    
    /**
     * Creates a new PatternRecognitionService.
     */
    public PatternRecognitionService() {
        LOG.info("Pattern recognition service created");
        
        // Load settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (settings != null) {
            enabled = settings.isPatternRecognitionEnabled();
        }
    }
    
    /**
     * Gets the pattern recognition service.
     * @return The pattern recognition service
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Enables or disables pattern recognition.
     * @param enabled Whether pattern recognition is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (settings != null) {
            settings.setPatternRecognitionEnabled(enabled);
        }
        
        LOG.info("Pattern recognition " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return Whether pattern recognition is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets pattern statistics.
     * @return Pattern statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Count patterns
        int codePatternCount = codePatterns.values().stream()
                .mapToInt(List::size)
                .sum();
        
        int errorPatternCount = errorPatterns.values().stream()
                .mapToInt(List::size)
                .sum();
        
        int documentationPatternCount = documentationPatterns.values().stream()
                .mapToInt(List::size)
                .sum();
        
        int totalPatternCount = codePatternCount + errorPatternCount + documentationPatternCount;
        
        statistics.put("codePatternCount", codePatternCount);
        statistics.put("errorPatternCount", errorPatternCount);
        statistics.put("documentationPatternCount", documentationPatternCount);
        statistics.put("totalPatternCount", totalPatternCount);
        
        // Count usages
        int totalUsageCount = countTotalUsages();
        int totalSuccessCount = countTotalSuccesses();
        int totalFailureCount = countTotalFailures();
        
        statistics.put("totalUsageCount", totalUsageCount);
        statistics.put("totalSuccessCount", totalSuccessCount);
        statistics.put("totalFailureCount", totalFailureCount);
        
        // Calculate success rate
        double successRate = totalUsageCount > 0 ? (double) totalSuccessCount / totalUsageCount : 0;
        
        statistics.put("successRate", successRate);
        
        // Calculate API savings
        int estimatedTokensSaved = totalSuccessCount * 1000;
        double estimatedCostSaved = estimatedTokensSaved / 1000.0 * 0.01;
        
        statistics.put("estimatedTokensSaved", estimatedTokensSaved);
        statistics.put("estimatedCostSaved", estimatedCostSaved);
        
        return statistics;
    }
    
    /**
     * Counts total pattern usages.
     * @return Total pattern usages
     */
    private int countTotalUsages() {
        int count = 0;
        
        for (List<Pattern> patterns : codePatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getUsageCount();
            }
        }
        
        for (List<Pattern> patterns : errorPatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getUsageCount();
            }
        }
        
        for (List<Pattern> patterns : documentationPatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getUsageCount();
            }
        }
        
        return count;
    }
    
    /**
     * Counts total pattern successes.
     * @return Total pattern successes
     */
    private int countTotalSuccesses() {
        int count = 0;
        
        for (List<Pattern> patterns : codePatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getSuccessCount();
            }
        }
        
        for (List<Pattern> patterns : errorPatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getSuccessCount();
            }
        }
        
        for (List<Pattern> patterns : documentationPatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getSuccessCount();
            }
        }
        
        return count;
    }
    
    /**
     * Counts total pattern failures.
     * @return Total pattern failures
     */
    private int countTotalFailures() {
        int count = 0;
        
        for (List<Pattern> patterns : codePatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getFailureCount();
            }
        }
        
        for (List<Pattern> patterns : errorPatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getFailureCount();
            }
        }
        
        for (List<Pattern> patterns : documentationPatterns.values()) {
            for (Pattern pattern : patterns) {
                count += pattern.getFailureCount();
            }
        }
        
        return count;
    }
    
    /**
     * Finds a code pattern.
     * @param input The input
     * @param options Additional options
     * @return The pattern or null if no pattern is found
     */
    @Nullable
    public Pattern findCodePattern(@NotNull String input, @Nullable Map<String, Object> options) {
        if (!enabled) {
            return null;
        }
        
        LOG.info("Finding code pattern for input: " + input);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get language
        String language = (String) requestOptions.getOrDefault("language", "java");
        
        // Normalize input
        String normalizedInput = normalizeInput(input);
        
        // Get patterns for language
        List<Pattern> patterns = codePatterns.getOrDefault(language, Collections.emptyList());
        
        // Find similar patterns
        return findSimilarPattern(normalizedInput, patterns);
    }
    
    /**
     * Finds an error pattern.
     * @param input The input
     * @param options Additional options
     * @return The pattern or null if no pattern is found
     */
    @Nullable
    public Pattern findErrorPattern(@NotNull String input, @Nullable Map<String, Object> options) {
        if (!enabled) {
            return null;
        }
        
        LOG.info("Finding error pattern for input: " + input);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get language
        String language = (String) requestOptions.getOrDefault("language", "java");
        
        // Normalize input
        String normalizedInput = normalizeInput(input);
        
        // Get patterns for language
        List<Pattern> patterns = errorPatterns.getOrDefault(language, Collections.emptyList());
        
        // Find similar patterns
        return findSimilarPattern(normalizedInput, patterns);
    }
    
    /**
     * Finds a documentation pattern.
     * @param input The input
     * @param options Additional options
     * @return The pattern or null if no pattern is found
     */
    @Nullable
    public Pattern findDocumentationPattern(@NotNull String input, @Nullable Map<String, Object> options) {
        if (!enabled) {
            return null;
        }
        
        LOG.info("Finding documentation pattern for input: " + input);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get language
        String language = (String) requestOptions.getOrDefault("language", "java");
        
        // Normalize input
        String normalizedInput = normalizeInput(input);
        
        // Get patterns for language
        List<Pattern> patterns = documentationPatterns.getOrDefault(language, Collections.emptyList());
        
        // Find similar patterns
        return findSimilarPattern(normalizedInput, patterns);
    }
    
    /**
     * Finds a similar pattern.
     * @param input The input
     * @param patterns The patterns to search
     * @return The pattern or null if no pattern is found
     */
    @Nullable
    private Pattern findSimilarPattern(@NotNull String input, @NotNull List<Pattern> patterns) {
        if (patterns.isEmpty()) {
            return null;
        }
        
        // Find most similar pattern
        Pattern mostSimilarPattern = null;
        double highestSimilarity = 0;
        
        for (Pattern pattern : patterns) {
            double similarity = calculateSimilarity(input, pattern.getInput());
            
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                mostSimilarPattern = pattern;
            }
        }
        
        // Check if similarity is above threshold
        if (highestSimilarity >= SIMILARITY_THRESHOLD) {
            LOG.info("Found similar pattern with similarity: " + highestSimilarity);
            return mostSimilarPattern;
        }
        
        return null;
    }
    
    /**
     * Stores a code pattern.
     * @param input The input
     * @param output The output
     * @param options Additional options
     */
    public void storeCodePattern(@NotNull String input, @NotNull String output, @Nullable Map<String, Object> options) {
        if (!enabled) {
            return;
        }
        
        LOG.info("Storing code pattern for input: " + input);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get language
        String language = (String) requestOptions.getOrDefault("language", "java");
        
        // Normalize input
        String normalizedInput = normalizeInput(input);
        
        // Create pattern
        Pattern pattern = new Pattern("code", normalizedInput, output, requestOptions);
        
        // Get patterns for language
        List<Pattern> patterns = codePatterns.computeIfAbsent(language, k -> new ArrayList<>());
        
        // Add pattern
        addPattern(patterns, pattern);
    }
    
    /**
     * Stores an error pattern.
     * @param input The input
     * @param output The output
     * @param options Additional options
     */
    public void storeErrorPattern(@NotNull String input, @NotNull String output, @Nullable Map<String, Object> options) {
        if (!enabled) {
            return;
        }
        
        LOG.info("Storing error pattern for input: " + input);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get language
        String language = (String) requestOptions.getOrDefault("language", "java");
        
        // Normalize input
        String normalizedInput = normalizeInput(input);
        
        // Create pattern
        Pattern pattern = new Pattern("error", normalizedInput, output, requestOptions);
        
        // Get patterns for language
        List<Pattern> patterns = errorPatterns.computeIfAbsent(language, k -> new ArrayList<>());
        
        // Add pattern
        addPattern(patterns, pattern);
    }
    
    /**
     * Stores a documentation pattern.
     * @param input The input
     * @param output The output
     * @param options Additional options
     */
    public void storeDocumentationPattern(@NotNull String input, @NotNull String output, @Nullable Map<String, Object> options) {
        if (!enabled) {
            return;
        }
        
        LOG.info("Storing documentation pattern for input: " + input);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get language
        String language = (String) requestOptions.getOrDefault("language", "java");
        
        // Normalize input
        String normalizedInput = normalizeInput(input);
        
        // Create pattern
        Pattern pattern = new Pattern("documentation", normalizedInput, output, requestOptions);
        
        // Get patterns for language
        List<Pattern> patterns = documentationPatterns.computeIfAbsent(language, k -> new ArrayList<>());
        
        // Add pattern
        addPattern(patterns, pattern);
    }
    
    /**
     * Adds a pattern to a list.
     * @param patterns The patterns
     * @param pattern The pattern to add
     */
    private void addPattern(@NotNull List<Pattern> patterns, @NotNull Pattern pattern) {
        // Find similar pattern
        Pattern existingPattern = findSimilarPattern(pattern.getInput(), patterns);
        
        if (existingPattern != null) {
            // Update existing pattern
            LOG.info("Updating existing pattern");
            
            // Replace with new pattern
            patterns.remove(existingPattern);
            patterns.add(pattern);
        } else {
            // Add new pattern
            LOG.info("Adding new pattern");
            
            // Check if max patterns reached
            if (patterns.size() >= MAX_PATTERNS) {
                // Remove least used pattern
                Pattern leastUsedPattern = patterns.stream()
                        .min(Comparator.comparingInt(Pattern::getUsageCount))
                        .orElse(null);
                
                if (leastUsedPattern != null) {
                    LOG.info("Removing least used pattern with usage count: " + leastUsedPattern.getUsageCount());
                    patterns.remove(leastUsedPattern);
                }
            }
            
            // Add new pattern
            patterns.add(pattern);
        }
    }
    
    /**
     * Records a pattern result.
     * @param pattern The pattern
     * @param success Whether the pattern was successful
     */
    public void recordPatternResult(@NotNull Pattern pattern, boolean success) {
        LOG.info("Recording pattern result: " + success);
        
        // Record result
        pattern.recordResult(success);
    }
    
    /**
     * Normalizes input by removing whitespace and comments.
     * @param input The input
     * @return The normalized input
     */
    @NotNull
    private String normalizeInput(@NotNull String input) {
        return input.replaceAll("\\s+", " ");
    }
    
    /**
     * Calculates the similarity between two strings.
     * @param str1 The first string
     * @param str2 The second string
     * @return The similarity (0-1)
     */
    private double calculateSimilarity(@NotNull String str1, @NotNull String str2) {
        // Simple Jaccard similarity
        Set<String> tokens1 = tokenize(str1);
        Set<String> tokens2 = tokenize(str2);
        
        // Calculate intersection size
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        
        // Calculate union size
        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);
        
        // Calculate Jaccard similarity
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Tokenizes a string.
     * @param str The string
     * @return The tokens
     */
    @NotNull
    private Set<String> tokenize(@NotNull String str) {
        // Split into words
        return Arrays.stream(str.replaceAll("[^a-zA-Z0-9]", " ").split("\\s+"))
                .filter(token -> !token.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
    
    /**
     * Clears all patterns.
     */
    public void clearPatterns() {
        LOG.info("Clearing all patterns");
        
        codePatterns.clear();
        errorPatterns.clear();
        documentationPatterns.clear();
    }
}