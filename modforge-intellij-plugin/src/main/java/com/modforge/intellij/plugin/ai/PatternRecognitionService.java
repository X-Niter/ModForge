package com.modforge.intellij.plugin.ai;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for pattern recognition to reduce API usage.
 * This service caches and learns from previous requests to minimize OpenAI API calls.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    private static final Gson GSON = new Gson();
    private static final Type PATTERN_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();
    private static final double SIMILARITY_THRESHOLD = 0.85; // 85% similarity is considered a match
    
    private final Project project;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final Map<String, List<Pattern>> patternsByType = new ConcurrentHashMap<>();
    private final ReadWriteLock patternLock = new ReentrantReadWriteLock();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger apiCalls = new AtomicInteger(0);
    
    /**
     * Create a pattern recognition service.
     *
     * @param project The project
     */
    public PatternRecognitionService(@NotNull Project project) {
        this.project = project;
        
        // Initialize from settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        enabled.set(settings.isEnablePatternRecognition());
        
        // Load patterns
        loadPatterns();
    }
    
    /**
     * Check if pattern recognition is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled.get();
    }
    
    /**
     * Enable or disable pattern recognition.
     *
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setEnablePatternRecognition(enabled);
    }
    
    /**
     * Get metrics on pattern recognition.
     *
     * @return Metrics as a map
     */
    @NotNull
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("enabled", enabled.get());
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("patternMatches", patternMatches.get());
        metrics.put("apiCalls", apiCalls.get());
        
        // Calculate cost savings (approximation)
        double estimatedTokensSaved = patternMatches.get() * 1500; // Assuming average of 1500 tokens per request
        double estimatedCostSaved = estimatedTokensSaved * 0.00002; // $0.02 per 1000 tokens
        
        metrics.put("estimatedTokensSaved", estimatedTokensSaved);
        metrics.put("estimatedCostSaved", estimatedCostSaved);
        
        // Add pattern counts by type
        Map<String, Integer> patternCountsByType = new HashMap<>();
        patternLock.readLock().lock();
        try {
            patternsByType.forEach((type, patterns) -> patternCountsByType.put(type, patterns.size()));
        } finally {
            patternLock.readLock().unlock();
        }
        metrics.put("patternCountsByType", patternCountsByType);
        
        return metrics;
    }
    
    /**
     * Try to match a request with existing patterns.
     *
     * @param type        The type of pattern (e.g., "code", "docs", "fix")
     * @param context     The context for the request (e.g., file contents, error message)
     * @param instruction The instruction or prompt for the request
     * @return The response from a matching pattern, or null if no match is found
     */
    @Nullable
    public String tryMatchPattern(@NotNull String type, @NotNull String context, @NotNull String instruction) {
        if (!enabled.get()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        List<Pattern> patterns = getPatternsByType(type);
        if (patterns.isEmpty()) {
            // No patterns of this type yet
            apiCalls.incrementAndGet();
            return null;
        }
        
        // Find the most similar pattern
        Pattern bestMatch = null;
        double bestSimilarity = 0;
        
        for (Pattern pattern : patterns) {
            double contextSimilarity = calculateSimilarity(context, pattern.context);
            double instructionSimilarity = calculateSimilarity(instruction, pattern.instruction);
            
            // Weight the similarities (context is more important)
            double overallSimilarity = (contextSimilarity * 0.7) + (instructionSimilarity * 0.3);
            
            if (overallSimilarity > bestSimilarity) {
                bestSimilarity = overallSimilarity;
                bestMatch = pattern;
            }
        }
        
        if (bestMatch != null && bestSimilarity >= SIMILARITY_THRESHOLD) {
            LOG.info("Found pattern match with similarity " + bestSimilarity);
            patternMatches.incrementAndGet();
            return bestMatch.response;
        }
        
        // No match found, will fall back to API
        apiCalls.incrementAndGet();
        return null;
    }
    
    /**
     * Store a pattern from a successful API request.
     *
     * @param type        The type of pattern (e.g., "code", "docs", "fix")
     * @param context     The context for the request (e.g., file contents, error message)
     * @param instruction The instruction or prompt for the request
     * @param response    The response from the API
     */
    public void storePattern(@NotNull String type, @NotNull String context, @NotNull String instruction, @NotNull String response) {
        if (!enabled.get()) {
            return;
        }
        
        patternLock.writeLock().lock();
        try {
            List<Pattern> patterns = patternsByType.computeIfAbsent(type, k -> new ArrayList<>());
            
            // Check if very similar pattern already exists
            for (Pattern pattern : patterns) {
                double contextSimilarity = calculateSimilarity(context, pattern.context);
                double instructionSimilarity = calculateSimilarity(instruction, pattern.instruction);
                
                // If almost identical, update response and return
                if (contextSimilarity > 0.95 && instructionSimilarity > 0.95) {
                    pattern.response = response;
                    LOG.info("Updated existing pattern");
                    return;
                }
            }
            
            // Add new pattern
            Pattern pattern = new Pattern(type, context, instruction, response);
            patterns.add(pattern);
            LOG.info("Added new pattern of type " + type);
            
            // Upload patterns to server
            uploadPatterns();
        } finally {
            patternLock.writeLock().unlock();
        }
    }
    
    /**
     * Load patterns from the server.
     */
    private void loadPatterns() {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            LOG.warn("Cannot load patterns: not authenticated");
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = authManager.getToken();
        
        String patternsUrl = serverUrl.endsWith("/") ? serverUrl + "patterns" : serverUrl + "/patterns";
        
        TokenAuthConnectionUtil.executeGet(patternsUrl, token)
                .thenAccept(response -> {
                    if (response == null || response.isEmpty()) {
                        LOG.info("No patterns found on server");
                        return;
                    }
                    
                    try {
                        List<Map<String, Object>> patternsList = GSON.fromJson(response, PATTERN_LIST_TYPE);
                        int count = 0;
                        
                        patternLock.writeLock().lock();
                        try {
                            patternsByType.clear();
                            
                            for (Map<String, Object> patternMap : patternsList) {
                                String type = (String) patternMap.get("type");
                                String context = (String) patternMap.get("context");
                                String instruction = (String) patternMap.get("instruction");
                                String response = (String) patternMap.get("response");
                                
                                if (type != null && context != null && instruction != null && response != null) {
                                    List<Pattern> patterns = patternsByType.computeIfAbsent(type, k -> new ArrayList<>());
                                    patterns.add(new Pattern(type, context, instruction, response));
                                    count++;
                                }
                            }
                        } finally {
                            patternLock.writeLock().unlock();
                        }
                        
                        LOG.info("Loaded " + count + " patterns from server");
                    } catch (Exception e) {
                        LOG.error("Error parsing patterns from server", e);
                    }
                })
                .exceptionally(e -> {
                    LOG.error("Error loading patterns from server", e);
                    return null;
                });
    }
    
    /**
     * Upload patterns to the server.
     */
    private void uploadPatterns() {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            LOG.warn("Cannot upload patterns: not authenticated");
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = authManager.getToken();
        
        String patternsUrl = serverUrl.endsWith("/") ? serverUrl + "patterns" : serverUrl + "/patterns";
        
        List<Map<String, Object>> patternsList = new ArrayList<>();
        
        patternLock.readLock().lock();
        try {
            patternsByType.forEach((type, patterns) -> {
                for (Pattern pattern : patterns) {
                    Map<String, Object> patternMap = new HashMap<>();
                    patternMap.put("type", pattern.type);
                    patternMap.put("context", pattern.context);
                    patternMap.put("instruction", pattern.instruction);
                    patternMap.put("response", pattern.response);
                    patternsList.add(patternMap);
                }
            });
        } finally {
            patternLock.readLock().unlock();
        }
        
        LOG.info("Uploading " + patternsList.size() + " patterns to server");
        
        TokenAuthConnectionUtil.executePost(patternsUrl, patternsList, token)
                .thenAccept(response -> LOG.info("Patterns uploaded successfully"))
                .exceptionally(e -> {
                    LOG.error("Error uploading patterns to server", e);
                    return null;
                });
    }
    
    /**
     * Get patterns by type.
     *
     * @param type The type of pattern
     * @return The list of patterns for the given type
     */
    @NotNull
    private List<Pattern> getPatternsByType(@NotNull String type) {
        patternLock.readLock().lock();
        try {
            return patternsByType.getOrDefault(type, Collections.emptyList());
        } finally {
            patternLock.readLock().unlock();
        }
    }
    
    /**
     * Calculate the similarity between two strings.
     * This is a basic implementation using Jaccard similarity on tokens.
     *
     * @param str1 The first string
     * @param str2 The second string
     * @return A similarity score between 0 and 1
     */
    private static double calculateSimilarity(@NotNull String str1, @NotNull String str2) {
        if (str1.isEmpty() || str2.isEmpty()) {
            return 0;
        }
        
        // Exact match
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        // Tokenize
        Set<String> tokens1 = new HashSet<>(Arrays.asList(tokenize(str1)));
        Set<String> tokens2 = new HashSet<>(Arrays.asList(tokenize(str2)));
        
        // Calculate Jaccard similarity
        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);
        
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Tokenize a string.
     *
     * @param str The string to tokenize
     * @return An array of tokens
     */
    private static String[] tokenize(@NotNull String str) {
        // Remove excess whitespace and punctuation
        String cleaned = str.replaceAll("[\\p{Punct}]", " ").replaceAll("\\s+", " ").trim().toLowerCase();
        
        // Split by whitespace
        String[] words = cleaned.split("\\s+");
        
        // Filter out stop words and short words
        List<String> tokens = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 2 && !isStopWord(word)) {
                tokens.add(word);
            }
        }
        
        return tokens.toArray(new String[0]);
    }
    
    /**
     * Check if a word is a stop word.
     *
     * @param word The word to check
     * @return True if it's a stop word, false otherwise
     */
    private static boolean isStopWord(@NotNull String word) {
        // Common English stop words
        return Arrays.asList("the", "and", "or", "of", "to", "in", "is", "that", "it", "with", "for", "as", "be", "this", "was", "are").contains(word);
    }
    
    /**
     * A pattern for matching contexts and instructions.
     */
    private static class Pattern {
        final String type;
        final String context;
        final String instruction;
        String response;
        
        Pattern(String type, String context, String instruction, String response) {
            this.type = type;
            this.context = context;
            this.instruction = instruction;
            this.response = response;
        }
    }
}