package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that provides pattern recognition for code generation.
 * This service helps reduce API calls by recognizing patterns in code
 * and using stored patterns to generate responses without calling the API.
 */
@Service
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    private static final int MAX_PATTERNS = 10000;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.7;
    
    private final ExecutorService executor;
    private final Map<String, Pattern> codePatterns = new ConcurrentHashMap<>();
    private final Map<String, Pattern> errorPatterns = new ConcurrentHashMap<>();
    private final Map<String, Pattern> documentationPatterns = new ConcurrentHashMap<>();
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger patternMisses = new AtomicInteger(0);
    
    /**
     * Creates a new PatternRecognitionService.
     */
    public PatternRecognitionService() {
        this.executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("PatternRecognitionService", 2);
        
        LOG.info("Pattern recognition service created");
    }
    
    /**
     * Gets the pattern recognition service instance.
     * @return The pattern recognition service
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Represents a pattern for code generation.
     */
    public static class Pattern {
        private final String key;
        private final String input;
        private final String output;
        private final Map<String, Object> metadata;
        private final long timestamp;
        private int usageCount;
        private int successCount;
        
        /**
         * Creates a new Pattern.
         * @param key The pattern key
         * @param input The input text
         * @param output The output text
         * @param metadata The metadata
         */
        public Pattern(String key, String input, String output, Map<String, Object> metadata) {
            this.key = key;
            this.input = input;
            this.output = output;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
            this.usageCount = 0;
            this.successCount = 0;
        }
        
        /**
         * Gets the pattern key.
         * @return The pattern key
         */
        public String getKey() {
            return key;
        }
        
        /**
         * Gets the input text.
         * @return The input text
         */
        public String getInput() {
            return input;
        }
        
        /**
         * Gets the output text.
         * @return The output text
         */
        public String getOutput() {
            return output;
        }
        
        /**
         * Gets the metadata.
         * @return The metadata
         */
        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }
        
        /**
         * Gets the timestamp.
         * @return The timestamp
         */
        public long getTimestamp() {
            return timestamp;
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
         * Increments the usage count.
         */
        public void incrementUsageCount() {
            usageCount++;
        }
        
        /**
         * Increments the success count.
         */
        public void incrementSuccessCount() {
            successCount++;
        }
        
        /**
         * Gets the success rate.
         * @return The success rate
         */
        public double getSuccessRate() {
            return usageCount > 0 ? (double) successCount / usageCount : 0;
        }
    }
    
    /**
     * Finds a matching pattern for code generation.
     * @param input The input text
     * @param options The options
     * @return The matching pattern, or null if no match found
     */
    @Nullable
    public Pattern findCodePattern(@NotNull String input, @Nullable Map<String, Object> options) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return null;
        }
        
        // Get language from options
        String language = options != null ? (String) options.getOrDefault("language", "java") : "java";
        
        // Find best match
        Pattern bestMatch = findBestMatch(input, codePatterns, options);
        
        if (bestMatch != null) {
            // Increment usage count
            bestMatch.incrementUsageCount();
            
            // Increment pattern matches
            patternMatches.incrementAndGet();
            
            // Record pattern match in AI service
            AIServiceManager.getInstance().recordPatternMatch();
            
            return bestMatch;
        }
        
        // Increment pattern misses
        patternMisses.incrementAndGet();
        
        return null;
    }
    
    /**
     * Finds a matching pattern for error resolution.
     * @param errorMessage The error message
     * @param options The options
     * @return The matching pattern, or null if no match found
     */
    @Nullable
    public Pattern findErrorPattern(@NotNull String errorMessage, @Nullable Map<String, Object> options) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return null;
        }
        
        // Find best match
        Pattern bestMatch = findBestMatch(errorMessage, errorPatterns, options);
        
        if (bestMatch != null) {
            // Increment usage count
            bestMatch.incrementUsageCount();
            
            // Increment pattern matches
            patternMatches.incrementAndGet();
            
            // Record pattern match in AI service
            AIServiceManager.getInstance().recordPatternMatch();
            
            return bestMatch;
        }
        
        // Increment pattern misses
        patternMisses.incrementAndGet();
        
        return null;
    }
    
    /**
     * Finds a matching pattern for documentation generation.
     * @param code The code
     * @param options The options
     * @return The matching pattern, or null if no match found
     */
    @Nullable
    public Pattern findDocumentationPattern(@NotNull String code, @Nullable Map<String, Object> options) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return null;
        }
        
        // Find best match
        Pattern bestMatch = findBestMatch(code, documentationPatterns, options);
        
        if (bestMatch != null) {
            // Increment usage count
            bestMatch.incrementUsageCount();
            
            // Increment pattern matches
            patternMatches.incrementAndGet();
            
            // Record pattern match in AI service
            AIServiceManager.getInstance().recordPatternMatch();
            
            return bestMatch;
        }
        
        // Increment pattern misses
        patternMisses.incrementAndGet();
        
        return null;
    }
    
    /**
     * Finds the best matching pattern.
     * @param input The input text
     * @param patterns The patterns
     * @param options The options
     * @return The best matching pattern, or null if no match found
     */
    @Nullable
    private Pattern findBestMatch(@NotNull String input, @NotNull Map<String, Pattern> patterns, 
                                @Nullable Map<String, Object> options) {
        if (patterns.isEmpty()) {
            return null;
        }
        
        // Get similarity threshold
        double similarityThreshold = options != null ? 
                (double) options.getOrDefault("similarityThreshold", MIN_SIMILARITY_THRESHOLD) : 
                MIN_SIMILARITY_THRESHOLD;
        
        // Get patterns sorted by similarity
        List<Map.Entry<Double, Pattern>> matches = new ArrayList<>();
        
        for (Pattern pattern : patterns.values()) {
            double similarity = calculateSimilarity(input, pattern.getInput());
            
            if (similarity >= similarityThreshold) {
                matches.add(Map.entry(similarity, pattern));
            }
        }
        
        // Sort by similarity (highest first)
        matches.sort(Map.Entry.<Double, Pattern>comparingByKey().reversed());
        
        // Return best match if any
        return matches.isEmpty() ? null : matches.get(0).getValue();
    }
    
    /**
     * Calculates the similarity between two strings.
     * @param str1 The first string
     * @param str2 The second string
     * @return The similarity (0-1)
     */
    private double calculateSimilarity(@NotNull String str1, @NotNull String str2) {
        // Simple similarity calculation based on the Levenshtein distance
        int distance = levenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        return maxLength > 0 ? 1.0 - (double) distance / maxLength : 1.0;
    }
    
    /**
     * Calculates the Levenshtein distance between two strings.
     * @param str1 The first string
     * @param str2 The second string
     * @return The Levenshtein distance
     */
    private int levenshteinDistance(@NotNull String str1, @NotNull String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
    
    /**
     * Stores a code pattern.
     * @param input The input text
     * @param output The output text
     * @param options The options
     */
    public void storeCodePattern(@NotNull String input, @NotNull String output, @Nullable Map<String, Object> options) {
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                // Get key
                String key = generateKey(input);
                
                // Create pattern
                Pattern pattern = new Pattern(key, input, output, options);
                
                // Store pattern
                codePatterns.put(key, pattern);
                
                // Clean up if needed
                cleanupPatterns(codePatterns);
                
                LOG.info("Stored code pattern: " + key);
            } catch (Exception ex) {
                LOG.error("Error storing code pattern", ex);
            }
        });
    }
    
    /**
     * Stores an error pattern.
     * @param errorMessage The error message
     * @param fix The fix
     * @param options The options
     */
    public void storeErrorPattern(@NotNull String errorMessage, @NotNull String fix, @Nullable Map<String, Object> options) {
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                // Get key
                String key = generateKey(errorMessage);
                
                // Create pattern
                Pattern pattern = new Pattern(key, errorMessage, fix, options);
                
                // Store pattern
                errorPatterns.put(key, pattern);
                
                // Clean up if needed
                cleanupPatterns(errorPatterns);
                
                LOG.info("Stored error pattern: " + key);
            } catch (Exception ex) {
                LOG.error("Error storing error pattern", ex);
            }
        });
    }
    
    /**
     * Stores a documentation pattern.
     * @param code The code
     * @param documentation The documentation
     * @param options The options
     */
    public void storeDocumentationPattern(@NotNull String code, @NotNull String documentation, @Nullable Map<String, Object> options) {
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                // Get key
                String key = generateKey(code);
                
                // Create pattern
                Pattern pattern = new Pattern(key, code, documentation, options);
                
                // Store pattern
                documentationPatterns.put(key, pattern);
                
                // Clean up if needed
                cleanupPatterns(documentationPatterns);
                
                LOG.info("Stored documentation pattern: " + key);
            } catch (Exception ex) {
                LOG.error("Error storing documentation pattern", ex);
            }
        });
    }
    
    /**
     * Records a pattern match result.
     * @param pattern The pattern
     * @param success Whether the match was successful
     */
    public void recordPatternResult(@NotNull Pattern pattern, boolean success) {
        if (success) {
            pattern.incrementSuccessCount();
        }
    }
    
    /**
     * Generates a key for a pattern.
     * @param input The input text
     * @return The key
     */
    private String generateKey(@NotNull String input) {
        // Simple key generation based on content hash
        return String.valueOf(Math.abs(input.hashCode()));
    }
    
    /**
     * Cleans up patterns if needed.
     * @param patterns The patterns
     */
    private void cleanupPatterns(@NotNull Map<String, Pattern> patterns) {
        if (patterns.size() <= MAX_PATTERNS) {
            return;
        }
        
        // Sort patterns by success rate and timestamp
        List<Pattern> sortedPatterns = new ArrayList<>(patterns.values());
        
        // Sort by success rate (descending) and then by timestamp (descending)
        sortedPatterns.sort(Comparator.<Pattern>comparingDouble(Pattern::getSuccessRate)
                .reversed()
                .thenComparingLong(Pattern::getTimestamp)
                .reversed());
        
        // Remove excess patterns
        for (int i = MAX_PATTERNS; i < sortedPatterns.size(); i++) {
            patterns.remove(sortedPatterns.get(i).getKey());
        }
    }
    
    /**
     * Gets the pattern match count.
     * @return The pattern match count
     */
    public int getPatternMatchCount() {
        return patternMatches.get();
    }
    
    /**
     * Gets the pattern miss count.
     * @return The pattern miss count
     */
    public int getPatternMissCount() {
        return patternMisses.get();
    }
    
    /**
     * Gets the code pattern count.
     * @return The code pattern count
     */
    public int getCodePatternCount() {
        return codePatterns.size();
    }
    
    /**
     * Gets the error pattern count.
     * @return The error pattern count
     */
    public int getErrorPatternCount() {
        return errorPatterns.size();
    }
    
    /**
     * Gets the documentation pattern count.
     * @return The documentation pattern count
     */
    public int getDocumentationPatternCount() {
        return documentationPatterns.size();
    }
    
    /**
     * Gets the total pattern count.
     * @return The total pattern count
     */
    public int getTotalPatternCount() {
        return codePatterns.size() + errorPatterns.size() + documentationPatterns.size();
    }
    
    /**
     * Clears all patterns.
     */
    public void clearPatterns() {
        codePatterns.clear();
        errorPatterns.clear();
        documentationPatterns.clear();
    }
    
    /**
     * Resets metrics.
     */
    public void resetMetrics() {
        patternMatches.set(0);
        patternMisses.set(0);
    }
}