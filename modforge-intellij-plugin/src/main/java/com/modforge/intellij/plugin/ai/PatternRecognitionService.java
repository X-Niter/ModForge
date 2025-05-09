package com.modforge.intellij.plugin.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Service for pattern recognition.
 * This service is responsible for recognizing and applying patterns to reduce API usage.
 */
@Service(Service.Level.APP)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private static final int MAX_PATTERNS = 1000;
    private static final int SAVE_INTERVAL_MINUTES = 30;
    
    private final List<Pattern> codePatterns = Collections.synchronizedList(new ArrayList<>());
    private final List<Pattern> errorPatterns = Collections.synchronizedList(new ArrayList<>());
    private final List<Pattern> documentationPatterns = Collections.synchronizedList(new ArrayList<>());
    
    private final Map<String, Integer> patternUsageCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> patternSuccessCounts = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler;
    
    // Metrics
    private int totalApiCalls = 0;
    private int totalPatternMatches = 0;
    private int totalTokensSaved = 0;
    private double totalCostSaved = 0.0;
    
    // Cost estimation (average cost per 1K tokens)
    private static final double COST_PER_1K_TOKENS = 0.015;
    
    /**
     * Creates a new PatternRecognitionService.
     */
    public PatternRecognitionService() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ModForge Pattern Recognition Service");
            thread.setDaemon(true);
            return thread;
        });
        
        // Load patterns
        loadPatterns();
        
        // Schedule auto-save
        scheduler.scheduleAtFixedRate(
                this::savePatterns,
                SAVE_INTERVAL_MINUTES,
                SAVE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
        
        LOG.info("Pattern recognition service initialized with " + 
                 codePatterns.size() + " code patterns, " + 
                 errorPatterns.size() + " error patterns, " + 
                 documentationPatterns.size() + " documentation patterns");
    }
    
    /**
     * Gets the pattern recognition service instance.
     * @return The pattern recognition service
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Represents a pattern.
     */
    public static class Pattern {
        private String id;
        private String type;
        private String input;
        private String output;
        private Map<String, Object> options;
        private long creationTime;
        private int usageCount;
        private int successCount;
        private Map<String, Double> inputEmbedding;
        
        /**
         * Creates a new Pattern.
         * @param id The ID
         * @param type The type
         * @param input The input
         * @param output The output
         * @param options The options
         */
        public Pattern(String id, String type, String input, String output, Map<String, Object> options) {
            this.id = id;
            this.type = type;
            this.input = input;
            this.output = output;
            this.options = options != null ? new HashMap<>(options) : new HashMap<>();
            this.creationTime = System.currentTimeMillis();
            this.usageCount = 0;
            this.successCount = 0;
            this.inputEmbedding = generateEmbedding(input);
        }
        
        /**
         * Gets the ID.
         * @return The ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Gets the type.
         * @return The type
         */
        public String getType() {
            return type;
        }
        
        /**
         * Gets the input.
         * @return The input
         */
        public String getInput() {
            return input;
        }
        
        /**
         * Gets the output.
         * @return The output
         */
        public String getOutput() {
            return output;
        }
        
        /**
         * Gets the options.
         * @return The options
         */
        public Map<String, Object> getOptions() {
            return options;
        }
        
        /**
         * Gets the creation time.
         * @return The creation time
         */
        public long getCreationTime() {
            return creationTime;
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
            successCount++;
        }
        
        /**
         * Gets the success rate.
         * @return The success rate
         */
        public double getSuccessRate() {
            return usageCount > 0 ? (double) successCount / usageCount : 0.0;
        }
        
        /**
         * Gets the input embedding.
         * @return The input embedding
         */
        public Map<String, Double> getInputEmbedding() {
            return inputEmbedding;
        }
    }
    
    /**
     * Stores a code pattern.
     * @param prompt The prompt
     * @param code The generated code
     * @param options The options
     */
    public void storeCodePattern(@NotNull String prompt, @NotNull String code, @Nullable Map<String, Object> options) {
        if (!isPatternRecognitionEnabled()) {
            return;
        }
        
        String id = UUID.randomUUID().toString();
        Pattern pattern = new Pattern(id, "code", prompt, code, options);
        
        synchronized (codePatterns) {
            codePatterns.add(pattern);
            
            // Trim if needed
            if (codePatterns.size() > MAX_PATTERNS) {
                // Sort by success rate and usage count
                codePatterns.sort((p1, p2) -> {
                    int successRateCompare = Double.compare(p2.getSuccessRate(), p1.getSuccessRate());
                    if (successRateCompare != 0) {
                        return successRateCompare;
                    }
                    return Integer.compare(p2.getUsageCount(), p1.getUsageCount());
                });
                
                // Remove least useful patterns
                while (codePatterns.size() > MAX_PATTERNS) {
                    codePatterns.remove(codePatterns.size() - 1);
                }
            }
        }
        
        LOG.info("Stored code pattern: " + id);
        
        // Save patterns
        AppExecutorUtil.getAppExecutorService().execute(this::savePatterns);
    }
    
    /**
     * Stores an error pattern.
     * @param error The error
     * @param fixedCode The fixed code
     * @param options The options
     */
    public void storeErrorPattern(@NotNull String error, @NotNull String fixedCode, @Nullable Map<String, Object> options) {
        if (!isPatternRecognitionEnabled()) {
            return;
        }
        
        String id = UUID.randomUUID().toString();
        Pattern pattern = new Pattern(id, "error", error, fixedCode, options);
        
        synchronized (errorPatterns) {
            errorPatterns.add(pattern);
            
            // Trim if needed
            if (errorPatterns.size() > MAX_PATTERNS) {
                // Sort by success rate and usage count
                errorPatterns.sort((p1, p2) -> {
                    int successRateCompare = Double.compare(p2.getSuccessRate(), p1.getSuccessRate());
                    if (successRateCompare != 0) {
                        return successRateCompare;
                    }
                    return Integer.compare(p2.getUsageCount(), p1.getUsageCount());
                });
                
                // Remove least useful patterns
                while (errorPatterns.size() > MAX_PATTERNS) {
                    errorPatterns.remove(errorPatterns.size() - 1);
                }
            }
        }
        
        LOG.info("Stored error pattern: " + id);
        
        // Save patterns
        AppExecutorUtil.getAppExecutorService().execute(this::savePatterns);
    }
    
    /**
     * Stores a documentation pattern.
     * @param code The code
     * @param documentedCode The documented code
     * @param options The options
     */
    public void storeDocumentationPattern(@NotNull String code, @NotNull String documentedCode, @Nullable Map<String, Object> options) {
        if (!isPatternRecognitionEnabled()) {
            return;
        }
        
        String id = UUID.randomUUID().toString();
        Pattern pattern = new Pattern(id, "documentation", code, documentedCode, options);
        
        synchronized (documentationPatterns) {
            documentationPatterns.add(pattern);
            
            // Trim if needed
            if (documentationPatterns.size() > MAX_PATTERNS) {
                // Sort by success rate and usage count
                documentationPatterns.sort((p1, p2) -> {
                    int successRateCompare = Double.compare(p2.getSuccessRate(), p1.getSuccessRate());
                    if (successRateCompare != 0) {
                        return successRateCompare;
                    }
                    return Integer.compare(p2.getUsageCount(), p1.getUsageCount());
                });
                
                // Remove least useful patterns
                while (documentationPatterns.size() > MAX_PATTERNS) {
                    documentationPatterns.remove(documentationPatterns.size() - 1);
                }
            }
        }
        
        LOG.info("Stored documentation pattern: " + id);
        
        // Save patterns
        AppExecutorUtil.getAppExecutorService().execute(this::savePatterns);
    }
    
    /**
     * Finds a code pattern.
     * @param prompt The prompt
     * @param options The options
     * @return The pattern or null if not found
     */
    @Nullable
    public Pattern findCodePattern(@NotNull String prompt, @Nullable Map<String, Object> options) {
        if (!isPatternRecognitionEnabled()) {
            return null;
        }
        
        Map<String, Double> promptEmbedding = generateEmbedding(prompt);
        
        List<PatternMatch> matches = new ArrayList<>();
        
        synchronized (codePatterns) {
            for (Pattern pattern : codePatterns) {
                double similarity = calculateSimilarity(promptEmbedding, pattern.getInputEmbedding());
                
                if (similarity >= 0.85) { // Threshold for match
                    matches.add(new PatternMatch(pattern, similarity));
                }
            }
        }
        
        if (matches.isEmpty()) {
            return null;
        }
        
        // Sort by similarity
        matches.sort((m1, m2) -> Double.compare(m2.getSimilarity(), m1.getSimilarity()));
        
        // Get best match
        Pattern bestMatch = matches.get(0).getPattern();
        
        // Increment usage count
        bestMatch.incrementUsageCount();
        patternUsageCounts.compute(bestMatch.getId(), (id, count) -> count == null ? 1 : count + 1);
        
        // Update metrics
        totalPatternMatches++;
        
        // Estimate tokens saved (rough estimate based on input length)
        int tokensSaved = estimateTokens(prompt) + estimateTokens(bestMatch.getOutput());
        totalTokensSaved += tokensSaved;
        
        // Estimate cost saved
        double costSaved = (tokensSaved / 1000.0) * COST_PER_1K_TOKENS;
        totalCostSaved += costSaved;
        
        LOG.info("Found code pattern match with similarity: " + matches.get(0).getSimilarity());
        
        return bestMatch;
    }
    
    /**
     * Finds an error pattern.
     * @param error The error
     * @param options The options
     * @return The pattern or null if not found
     */
    @Nullable
    public Pattern findErrorPattern(@NotNull String error, @Nullable Map<String, Object> options) {
        if (!isPatternRecognitionEnabled()) {
            return null;
        }
        
        Map<String, Double> errorEmbedding = generateEmbedding(error);
        
        List<PatternMatch> matches = new ArrayList<>();
        
        synchronized (errorPatterns) {
            for (Pattern pattern : errorPatterns) {
                double similarity = calculateSimilarity(errorEmbedding, pattern.getInputEmbedding());
                
                if (similarity >= 0.9) { // Higher threshold for errors
                    matches.add(new PatternMatch(pattern, similarity));
                }
            }
        }
        
        if (matches.isEmpty()) {
            return null;
        }
        
        // Sort by similarity
        matches.sort((m1, m2) -> Double.compare(m2.getSimilarity(), m1.getSimilarity()));
        
        // Get best match
        Pattern bestMatch = matches.get(0).getPattern();
        
        // Increment usage count
        bestMatch.incrementUsageCount();
        patternUsageCounts.compute(bestMatch.getId(), (id, count) -> count == null ? 1 : count + 1);
        
        // Update metrics
        totalPatternMatches++;
        
        // Estimate tokens saved (rough estimate based on input length)
        int tokensSaved = estimateTokens(error) + estimateTokens(bestMatch.getOutput());
        totalTokensSaved += tokensSaved;
        
        // Estimate cost saved
        double costSaved = (tokensSaved / 1000.0) * COST_PER_1K_TOKENS;
        totalCostSaved += costSaved;
        
        LOG.info("Found error pattern match with similarity: " + matches.get(0).getSimilarity());
        
        return bestMatch;
    }
    
    /**
     * Finds a documentation pattern.
     * @param code The code
     * @param options The options
     * @return The pattern or null if not found
     */
    @Nullable
    public Pattern findDocumentationPattern(@NotNull String code, @Nullable Map<String, Object> options) {
        if (!isPatternRecognitionEnabled()) {
            return null;
        }
        
        Map<String, Double> codeEmbedding = generateEmbedding(code);
        
        List<PatternMatch> matches = new ArrayList<>();
        
        synchronized (documentationPatterns) {
            for (Pattern pattern : documentationPatterns) {
                double similarity = calculateSimilarity(codeEmbedding, pattern.getInputEmbedding());
                
                if (similarity >= 0.85) { // Threshold for match
                    matches.add(new PatternMatch(pattern, similarity));
                }
            }
        }
        
        if (matches.isEmpty()) {
            return null;
        }
        
        // Sort by similarity
        matches.sort((m1, m2) -> Double.compare(m2.getSimilarity(), m1.getSimilarity()));
        
        // Get best match
        Pattern bestMatch = matches.get(0).getPattern();
        
        // Increment usage count
        bestMatch.incrementUsageCount();
        patternUsageCounts.compute(bestMatch.getId(), (id, count) -> count == null ? 1 : count + 1);
        
        // Update metrics
        totalPatternMatches++;
        
        // Estimate tokens saved (rough estimate based on input length)
        int tokensSaved = estimateTokens(code) + estimateTokens(bestMatch.getOutput());
        totalTokensSaved += tokensSaved;
        
        // Estimate cost saved
        double costSaved = (tokensSaved / 1000.0) * COST_PER_1K_TOKENS;
        totalCostSaved += costSaved;
        
        LOG.info("Found documentation pattern match with similarity: " + matches.get(0).getSimilarity());
        
        return bestMatch;
    }
    
    /**
     * Records the result of a pattern.
     * @param pattern The pattern
     * @param success Whether the pattern was successful
     */
    public void recordPatternResult(@NotNull Pattern pattern, boolean success) {
        if (success) {
            pattern.incrementSuccessCount();
            patternSuccessCounts.compute(pattern.getId(), (id, count) -> count == null ? 1 : count + 1);
        }
    }
    
    /**
     * Records an API call.
     */
    public void recordApiCall() {
        totalApiCalls++;
    }
    
    /**
     * Generates an embedding for text.
     * @param text The text
     * @return The embedding
     */
    @NotNull
    private Map<String, Double> generateEmbedding(@NotNull String text) {
        // Tokenize and clean
        String cleanedText = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        // Split into tokens (words)
        String[] tokens = cleanedText.split("\\s+");
        
        // Create term frequency map
        Map<String, Double> embedding = new HashMap<>();
        
        for (String token : tokens) {
            embedding.compute(token, (key, count) -> count == null ? 1.0 : count + 1.0);
        }
        
        return embedding;
    }
    
    /**
     * Calculates the similarity between two embeddings.
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The similarity
     */
    private double calculateSimilarity(@NotNull Map<String, Double> embedding1, @NotNull Map<String, Double> embedding2) {
        if (embedding1.isEmpty() || embedding2.isEmpty()) {
            return 0.0;
        }
        
        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        return cosineSimilarity.cosineSimilarity(embedding1, embedding2);
    }
    
    /**
     * Gets metrics about pattern recognition.
     * @return The metrics
     */
    @NotNull
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("apiCalls", totalApiCalls);
        metrics.put("patternMatches", totalPatternMatches);
        metrics.put("tokensSaved", totalTokensSaved);
        metrics.put("costSaved", totalCostSaved);
        
        // Pattern counts
        Map<String, Integer> patternCounts = new HashMap<>();
        patternCounts.put("code", codePatterns.size());
        patternCounts.put("error", errorPatterns.size());
        patternCounts.put("documentation", documentationPatterns.size());
        
        metrics.put("patternCounts", patternCounts);
        
        // Success rates
        Map<String, Double> successRates = new HashMap<>();
        
        synchronized (codePatterns) {
            double codeSuccessRate = codePatterns.stream()
                    .filter(p -> p.getUsageCount() > 0)
                    .mapToDouble(Pattern::getSuccessRate)
                    .average()
                    .orElse(0.0);
            successRates.put("code", codeSuccessRate);
        }
        
        synchronized (errorPatterns) {
            double errorSuccessRate = errorPatterns.stream()
                    .filter(p -> p.getUsageCount() > 0)
                    .mapToDouble(Pattern::getSuccessRate)
                    .average()
                    .orElse(0.0);
            successRates.put("error", errorSuccessRate);
        }
        
        synchronized (documentationPatterns) {
            double docSuccessRate = documentationPatterns.stream()
                    .filter(p -> p.getUsageCount() > 0)
                    .mapToDouble(Pattern::getSuccessRate)
                    .average()
                    .orElse(0.0);
            successRates.put("documentation", docSuccessRate);
        }
        
        metrics.put("successRates", successRates);
        
        return metrics;
    }
    
    /**
     * Loads patterns from disk.
     */
    private void loadPatterns() {
        try {
            // Create patterns directory if it doesn't exist
            Path patternsDir = getPatternDirectory();
            if (!Files.exists(patternsDir)) {
                Files.createDirectories(patternsDir);
            }
            
            // Load code patterns
            loadPatternsOfType(patternsDir, "code", codePatterns);
            
            // Load error patterns
            loadPatternsOfType(patternsDir, "error", errorPatterns);
            
            // Load documentation patterns
            loadPatternsOfType(patternsDir, "documentation", documentationPatterns);
            
            LOG.info("Loaded patterns: " + 
                     codePatterns.size() + " code, " + 
                     errorPatterns.size() + " error, " + 
                     documentationPatterns.size() + " documentation");
            
        } catch (Exception e) {
            LOG.error("Error loading patterns", e);
        }
    }
    
    /**
     * Loads patterns of a specific type.
     * @param patternsDir The patterns directory
     * @param type The type
     * @param patternList The pattern list to populate
     */
    private void loadPatternsOfType(Path patternsDir, String type, List<Pattern> patternList) {
        try {
            Path typeDir = patternsDir.resolve(type);
            
            if (!Files.exists(typeDir)) {
                Files.createDirectories(typeDir);
                return;
            }
            
            // Get pattern files
            List<Path> patternFiles = Files.list(typeDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());
            
            Gson gson = new GsonBuilder().create();
            
            for (Path patternFile : patternFiles) {
                try (FileReader reader = new FileReader(patternFile.toFile())) {
                    Pattern pattern = gson.fromJson(reader, Pattern.class);
                    patternList.add(pattern);
                    
                    // Update metrics
                    patternUsageCounts.put(pattern.getId(), pattern.getUsageCount());
                    patternSuccessCounts.put(pattern.getId(), pattern.getSuccessCount());
                } catch (Exception e) {
                    LOG.error("Error loading pattern file: " + patternFile, e);
                }
            }
        } catch (Exception e) {
            LOG.error("Error loading patterns of type: " + type, e);
        }
    }
    
    /**
     * Saves patterns to disk.
     */
    private void savePatterns() {
        try {
            // Create patterns directory if it doesn't exist
            Path patternsDir = getPatternDirectory();
            if (!Files.exists(patternsDir)) {
                Files.createDirectories(patternsDir);
            }
            
            // Save code patterns
            savePatternsOfType(patternsDir, "code", codePatterns);
            
            // Save error patterns
            savePatternsOfType(patternsDir, "error", errorPatterns);
            
            // Save documentation patterns
            savePatternsOfType(patternsDir, "documentation", documentationPatterns);
            
            LOG.info("Saved patterns: " + 
                     codePatterns.size() + " code, " + 
                     errorPatterns.size() + " error, " + 
                     documentationPatterns.size() + " documentation");
            
        } catch (Exception e) {
            LOG.error("Error saving patterns", e);
        }
    }
    
    /**
     * Saves patterns of a specific type.
     * @param patternsDir The patterns directory
     * @param type The type
     * @param patternList The pattern list
     */
    private void savePatternsOfType(Path patternsDir, String type, List<Pattern> patternList) {
        try {
            Path typeDir = patternsDir.resolve(type);
            
            if (!Files.exists(typeDir)) {
                Files.createDirectories(typeDir);
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            synchronized (patternList) {
                for (Pattern pattern : patternList) {
                    Path patternFile = typeDir.resolve(pattern.getId() + ".json");
                    
                    try (FileWriter writer = new FileWriter(patternFile.toFile())) {
                        gson.toJson(pattern, writer);
                    } catch (Exception e) {
                        LOG.error("Error saving pattern file: " + patternFile, e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error saving patterns of type: " + type, e);
        }
    }
    
    /**
     * Gets the pattern directory.
     * @return The pattern directory
     */
    @NotNull
    private Path getPatternDirectory() {
        Path configDir = Paths.get(System.getProperty("user.home"), ".modforge");
        return configDir.resolve("patterns");
    }
    
    /**
     * Estimates the number of tokens in text.
     * @param text The text
     * @return The estimated number of tokens
     */
    private int estimateTokens(@NotNull String text) {
        // Very rough estimate: 1 token per 4 characters
        return Math.max(1, text.length() / 4);
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return True if enabled, false otherwise
     */
    private boolean isPatternRecognitionEnabled() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.isPatternRecognitionEnabled();
    }
    
    /**
     * Represents a pattern match.
     */
    private static class PatternMatch {
        private final Pattern pattern;
        private final double similarity;
        
        /**
         * Creates a new PatternMatch.
         * @param pattern The pattern
         * @param similarity The similarity
         */
        public PatternMatch(Pattern pattern, double similarity) {
            this.pattern = pattern;
            this.similarity = similarity;
        }
        
        /**
         * Gets the pattern.
         * @return The pattern
         */
        public Pattern getPattern() {
            return pattern;
        }
        
        /**
         * Gets the similarity.
         * @return The similarity
         */
        public double getSimilarity() {
            return similarity;
        }
    }
}