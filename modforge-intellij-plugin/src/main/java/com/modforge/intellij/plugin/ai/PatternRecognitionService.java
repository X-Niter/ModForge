package com.modforge.intellij.plugin.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for pattern recognition to reduce API costs.
 * This service stores and recognizes patterns in prompts and responses to avoid
 * making redundant API calls.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private static final String PATTERNS_DIRECTORY = "patterns";
    private static final int DEFAULT_TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.7;
    
    private final Project project;
    
    // Pattern storage
    private final Map<String, List<RecognizedPattern>> patterns = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger patternMatchCount = new AtomicInteger(0);
    private final AtomicInteger patternAddCount = new AtomicInteger(0);
    private final AtomicInteger apiSavingsCount = new AtomicInteger(0);
    
    // GSON for serialization
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Creates a new PatternRecognitionService.
     * @param project The project
     */
    public PatternRecognitionService(@NotNull Project project) {
        this.project = project;
        
        // Load existing patterns
        loadPatterns();
        
        // Schedule periodic pattern saving
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                this::savePatterns, 5, 5, TimeUnit.MINUTES);
        
        LOG.info("PatternRecognitionService initialized with " + getTotalPatternCount() + " patterns");
    }
    
    /**
     * Gets the PatternRecognitionService instance.
     * @param project The project
     * @return The PatternRecognitionService instance
     */
    public static PatternRecognitionService getInstance(@NotNull Project project) {
        return project.getService(PatternRecognitionService.class);
    }
    
    /**
     * Finds a matching pattern for a prompt.
     * @param patternType The pattern type
     * @param prompt The prompt
     * @param context Additional context
     * @return The matching pattern, or null if no match found
     */
    @Nullable
    public RecognizedPattern findMatchingPattern(@NotNull String patternType, @NotNull String prompt, 
                                               @Nullable Map<String, Object> context) {
        // Skip if pattern recognition is disabled
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return null;
        }
        
        List<RecognizedPattern> typePatterns = patterns.getOrDefault(patternType, Collections.emptyList());
        if (typePatterns.isEmpty()) {
            return null;
        }
        
        // Find similar patterns
        List<PatternMatch> matches = new ArrayList<>();
        
        for (RecognizedPattern pattern : typePatterns) {
            double similarity = calculateSimilarity(prompt, pattern.getPrompt());
            
            // Check context similarity if available
            if (context != null && pattern.getContext() != null) {
                double contextSimilarity = calculateContextSimilarity(context, pattern.getContext());
                similarity = (similarity + contextSimilarity) / 2; // Average with context similarity
            }
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                matches.add(new PatternMatch(pattern, similarity));
            }
        }
        
        // Sort by similarity (highest first)
        matches.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        
        // Return the best match
        if (!matches.isEmpty()) {
            PatternMatch bestMatch = matches.get(0);
            LOG.info("Found matching pattern with similarity: " + bestMatch.similarity);
            
            // Track statistics
            patternMatchCount.incrementAndGet();
            apiSavingsCount.incrementAndGet();
            
            // Update usage count and timestamp
            bestMatch.pattern.incrementUsageCount();
            bestMatch.pattern.setLastUsed(System.currentTimeMillis());
            
            return bestMatch.pattern;
        }
        
        return null;
    }
    
    /**
     * Adds a new pattern.
     * @param patternType The pattern type
     * @param prompt The prompt
     * @param response The response
     * @param context Additional context
     * @return The added pattern
     */
    @NotNull
    public RecognizedPattern addPattern(@NotNull String patternType, @NotNull String prompt, 
                                      @NotNull String response, @Nullable Map<String, Object> context) {
        // Skip if pattern recognition is disabled
        if (!ModForgeSettings.getInstance().isUsePatternRecognition()) {
            return new RecognizedPattern(patternType, prompt, response, context);
        }
        
        // Create pattern
        RecognizedPattern pattern = new RecognizedPattern(patternType, prompt, response, context);
        
        // Add to storage
        patterns.computeIfAbsent(patternType, k -> new ArrayList<>()).add(pattern);
        
        // Track statistics
        patternAddCount.incrementAndGet();
        
        // Save patterns (if many new patterns added)
        if (patternAddCount.get() % 10 == 0) {
            AppExecutorUtil.getAppExecutorService().execute(this::savePatterns);
        }
        
        return pattern;
    }
    
    /**
     * Gets the total number of patterns.
     * @return The total number of patterns
     */
    public int getTotalPatternCount() {
        return patterns.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Gets the pattern match count.
     * @return The pattern match count
     */
    public int getPatternMatchCount() {
        return patternMatchCount.get();
    }
    
    /**
     * Gets the pattern add count.
     * @return The pattern add count
     */
    public int getPatternAddCount() {
        return patternAddCount.get();
    }
    
    /**
     * Gets the API savings count.
     * @return The API savings count
     */
    public int getApiSavingsCount() {
        return apiSavingsCount.get();
    }
    
    /**
     * Calculates the similarity between two prompts.
     * @param prompt1 The first prompt
     * @param prompt2 The second prompt
     * @return The similarity score
     */
    private double calculateSimilarity(@NotNull String prompt1, @NotNull String prompt2) {
        // Normalize prompts
        String normalized1 = normalizeText(prompt1);
        String normalized2 = normalizeText(prompt2);
        
        // Calculate cosine similarity
        return cosineSimilarity(normalized1, normalized2);
    }
    
    /**
     * Calculates the similarity between two contexts.
     * @param context1 The first context
     * @param context2 The second context
     * @return The similarity score
     */
    private double calculateContextSimilarity(@NotNull Map<String, Object> context1, 
                                            @NotNull Map<String, Object> context2) {
        // Count matching keys
        Set<String> keys1 = context1.keySet();
        Set<String> keys2 = context2.keySet();
        
        Set<String> intersection = new HashSet<>(keys1);
        intersection.retainAll(keys2);
        
        if (intersection.isEmpty()) {
            return 0.0;
        }
        
        // Calculate similarity for each matching key
        double totalSimilarity = 0.0;
        int matchCount = 0;
        
        for (String key : intersection) {
            Object value1 = context1.get(key);
            Object value2 = context2.get(key);
            
            if (value1 == null || value2 == null) {
                continue;
            }
            
            if (value1.equals(value2)) {
                totalSimilarity += 1.0;
                matchCount++;
            } else if (value1 instanceof String && value2 instanceof String) {
                totalSimilarity += calculateSimilarity((String) value1, (String) value2);
                matchCount++;
            }
        }
        
        return matchCount > 0 ? totalSimilarity / matchCount : 0.0;
    }
    
    /**
     * Normalizes text by removing extra whitespace and converting to lowercase.
     * @param text The text to normalize
     * @return The normalized text
     */
    @NotNull
    private String normalizeText(@NotNull String text) {
        // Remove extra whitespace and convert to lowercase
        return text.replaceAll("\\s+", " ").toLowerCase().trim();
    }
    
    /**
     * Calculates the cosine similarity between two texts.
     * @param text1 The first text
     * @param text2 The second text
     * @return The cosine similarity
     */
    private double cosineSimilarity(@NotNull String text1, @NotNull String text2) {
        // Get word frequencies
        Map<String, Integer> vector1 = getWordFrequencies(text1);
        Map<String, Integer> vector2 = getWordFrequencies(text2);
        
        // Calculate dot product
        double dotProduct = 0.0;
        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            String word = entry.getKey();
            Integer count1 = entry.getValue();
            Integer count2 = vector2.getOrDefault(word, 0);
            
            dotProduct += count1 * count2;
        }
        
        // Calculate magnitudes
        double magnitude1 = calculateMagnitude(vector1);
        double magnitude2 = calculateMagnitude(vector2);
        
        // Calculate cosine similarity
        return magnitude1 > 0 && magnitude2 > 0 ? dotProduct / (magnitude1 * magnitude2) : 0.0;
    }
    
    /**
     * Gets word frequencies for a text.
     * @param text The text
     * @return The word frequencies
     */
    @NotNull
    private Map<String, Integer> getWordFrequencies(@NotNull String text) {
        Map<String, Integer> frequencies = new HashMap<>();
        
        // Split text into words
        String[] words = text.split("\\s+");
        
        // Count frequencies
        for (String word : words) {
            frequencies.put(word, frequencies.getOrDefault(word, 0) + 1);
        }
        
        return frequencies;
    }
    
    /**
     * Calculates the magnitude of a vector.
     * @param vector The vector
     * @return The magnitude
     */
    private double calculateMagnitude(@NotNull Map<String, Integer> vector) {
        double sum = 0.0;
        
        for (Integer count : vector.values()) {
            sum += count * count;
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * Loads patterns from disk.
     */
    private void loadPatterns() {
        try {
            // Create patterns directory if it doesn't exist
            File patternsDir = new File(PATTERNS_DIRECTORY);
            if (!patternsDir.exists()) {
                boolean created = patternsDir.mkdirs();
                if (!created) {
                    LOG.warn("Failed to create patterns directory: " + patternsDir.getAbsolutePath());
                }
                return;
            }
            
            // Load each pattern file
            File[] patternFiles = patternsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (patternFiles == null) {
                LOG.warn("Failed to list pattern files in directory: " + patternsDir.getAbsolutePath());
                return;
            }
            
            for (File patternFile : patternFiles) {
                try {
                    // Read file
                    String content = Files.readString(patternFile.toPath(), StandardCharsets.UTF_8);
                    
                    // Parse patterns
                    TypeToken<List<RecognizedPattern>> typeToken = new TypeToken<>() {};
                    List<RecognizedPattern> filePatterns = gson.fromJson(content, typeToken.getType());
                    
                    if (filePatterns != null && !filePatterns.isEmpty()) {
                        // Get pattern type from filename
                        String patternType = patternFile.getName().replace(".json", "");
                        
                        // Add patterns
                        patterns.computeIfAbsent(patternType, k -> new ArrayList<>()).addAll(filePatterns);
                    }
                } catch (Exception e) {
                    LOG.error("Error loading patterns from file: " + patternFile.getAbsolutePath(), e);
                }
            }
            
            LOG.info("Loaded " + getTotalPatternCount() + " patterns from disk");
        } catch (Exception e) {
            LOG.error("Error loading patterns", e);
        }
    }
    
    /**
     * Saves patterns to disk.
     */
    private void savePatterns() {
        try {
            // Create patterns directory if it doesn't exist
            Path patternsDir = Paths.get(PATTERNS_DIRECTORY);
            if (!Files.exists(patternsDir)) {
                Files.createDirectories(patternsDir);
            }
            
            // Save each pattern type
            for (Map.Entry<String, List<RecognizedPattern>> entry : patterns.entrySet()) {
                String patternType = entry.getKey();
                List<RecognizedPattern> patternList = entry.getValue();
                
                if (patternList.isEmpty()) {
                    continue;
                }
                
                // Create file
                Path patternFile = patternsDir.resolve(patternType + ".json");
                
                // Write patterns
                String json = gson.toJson(patternList);
                Files.writeString(patternFile, json, StandardCharsets.UTF_8);
            }
            
            LOG.info("Saved " + getTotalPatternCount() + " patterns to disk");
        } catch (Exception e) {
            LOG.error("Error saving patterns", e);
        }
    }
    
    /**
     * Pattern match result.
     */
    private static class PatternMatch {
        private final RecognizedPattern pattern;
        private final double similarity;
        
        /**
         * Creates a new PatternMatch.
         * @param pattern The pattern
         * @param similarity The similarity score
         */
        public PatternMatch(@NotNull RecognizedPattern pattern, double similarity) {
            this.pattern = pattern;
            this.similarity = similarity;
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void dispose() {
        savePatterns();
    }
    
    /**
     * Recognized pattern.
     */
    public static class RecognizedPattern {
        private final String type;
        private final String prompt;
        private final String response;
        private final Map<String, Object> context;
        private int usageCount;
        private long createdAt;
        private long lastUsed;
        
        /**
         * Creates a new RecognizedPattern.
         * @param type The pattern type
         * @param prompt The prompt
         * @param response The response
         * @param context Additional context
         */
        public RecognizedPattern(@NotNull String type, @NotNull String prompt, @NotNull String response, 
                                @Nullable Map<String, Object> context) {
            this.type = type;
            this.prompt = prompt;
            this.response = response;
            this.context = context != null ? new HashMap<>(context) : null;
            this.usageCount = 1;
            this.createdAt = System.currentTimeMillis();
            this.lastUsed = this.createdAt;
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
         * Gets the prompt.
         * @return The prompt
         */
        @NotNull
        public String getPrompt() {
            return prompt;
        }
        
        /**
         * Gets the response.
         * @return The response
         */
        @NotNull
        public String getResponse() {
            return response;
        }
        
        /**
         * Gets the context.
         * @return The context
         */
        @Nullable
        public Map<String, Object> getContext() {
            return context;
        }
        
        /**
         * Gets the usage count.
         * @return The usage count
         */
        public int getUsageCount() {
            return usageCount;
        }
        
        /**
         * Increments the usage count.
         */
        public void incrementUsageCount() {
            usageCount++;
        }
        
        /**
         * Gets the creation timestamp.
         * @return The creation timestamp
         */
        public long getCreatedAt() {
            return createdAt;
        }
        
        /**
         * Gets the last used timestamp.
         * @return The last used timestamp
         */
        public long getLastUsed() {
            return lastUsed;
        }
        
        /**
         * Sets the last used timestamp.
         * @param lastUsed The last used timestamp
         */
        public void setLastUsed(long lastUsed) {
            this.lastUsed = lastUsed;
        }
    }
}