package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for pattern recognition to reduce API costs.
 */
@Service(Service.Level.APP)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger apiCalls = new AtomicInteger(0);
    private final AtomicInteger estimatedTokensSaved = new AtomicInteger(0);
    private double estimatedCostSaved = 0.0;
    
    private final Map<String, String> patternCache = new ConcurrentHashMap<>();
    
    /**
     * Gets the instance of this service.
     * @return The service instance
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Creates a new instance of this service.
     */
    public PatternRecognitionService() {
        LOG.info("Pattern recognition service initialized");
    }
    
    /**
     * Gets the response from the pattern cache if it exists, otherwise returns null.
     * @param prompt The prompt to get the response for
     * @param requestType The type of request (e.g., "code", "explanation", "documentation")
     * @return The cached response, or null if not found
     */
    public String getResponseFromPatternCache(String prompt, String requestType) {
        if (!ModForgeSettings.getInstance().isPatternRecognitionEnabled()) {
            return null;
        }
        
        totalRequests.incrementAndGet();
        
        // Create cache key
        String cacheKey = createCacheKey(prompt, requestType);
        
        // Check cache
        String cachedResponse = patternCache.get(cacheKey);
        
        if (cachedResponse != null) {
            // Cache hit
            patternMatches.incrementAndGet();
            estimatedTokensSaved.addAndGet(estimateTokens(prompt, requestType));
            estimatedCostSaved += estimateCost(prompt, requestType);
            
            LOG.info("Pattern match found for request: " + requestType);
            
            return cachedResponse;
        }
        
        // Cache miss
        apiCalls.incrementAndGet();
        
        return null;
    }
    
    /**
     * Stores a response in the pattern cache.
     * @param prompt The prompt for which the response was generated
     * @param requestType The type of request (e.g., "code", "explanation", "documentation")
     * @param response The response to store
     */
    public void storeResponseInPatternCache(String prompt, String requestType, String response) {
        if (!ModForgeSettings.getInstance().isPatternRecognitionEnabled()) {
            return;
        }
        
        // Create cache key
        String cacheKey = createCacheKey(prompt, requestType);
        
        // Store in cache
        patternCache.put(cacheKey, response);
        
        LOG.info("Stored response in pattern cache for request: " + requestType);
    }
    
    /**
     * Creates a cache key for the given prompt and request type.
     * @param prompt The prompt
     * @param requestType The request type
     * @return The cache key
     */
    private String createCacheKey(String prompt, String requestType) {
        // Normalize prompt
        String normalizedPrompt = normalizePrompt(prompt);
        
        // Create cache key
        return requestType + ":" + normalizedPrompt;
    }
    
    /**
     * Normalizes a prompt by removing non-essential information.
     * @param prompt The prompt to normalize
     * @return The normalized prompt
     */
    private String normalizePrompt(String prompt) {
        // TODO: Implement more sophisticated normalization
        // For now, just lowercase and remove extra whitespace
        return prompt.toLowerCase().replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Estimates the number of tokens for a request.
     * @param prompt The prompt
     * @param requestType The request type
     * @return The estimated number of tokens
     */
    private int estimateTokens(String prompt, String requestType) {
        // Rough estimation of tokens
        // 1 token is approximately 4 characters in English
        int promptTokens = prompt.length() / 4;
        
        // Different request types have different response sizes
        if (requestType.equals("code")) {
            return promptTokens + 500; // Code generation typically produces larger responses
        } else if (requestType.equals("explanation")) {
            return promptTokens + 300; // Explanations are medium-sized
        } else if (requestType.equals("documentation")) {
            return promptTokens + 200; // Documentation is typically shorter
        } else {
            return promptTokens + 100; // Default
        }
    }
    
    /**
     * Estimates the cost for a request in dollars.
     * @param prompt The prompt
     * @param requestType The request type
     * @return The estimated cost
     */
    private double estimateCost(String prompt, String requestType) {
        int tokens = estimateTokens(prompt, requestType);
        
        // Cost per 1000 tokens
        double costPer1000Tokens = 0.002;
        
        return tokens * costPer1000Tokens / 1000.0;
    }
    
    /**
     * Gets the statistics for this service.
     * @return The statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalRequests", totalRequests.get());
        statistics.put("patternMatches", patternMatches.get());
        statistics.put("apiCalls", apiCalls.get());
        statistics.put("estimatedTokensSaved", estimatedTokensSaved.get());
        statistics.put("estimatedCostSaved", estimatedCostSaved);
        
        return statistics;
    }
    
    /**
     * Clears the pattern cache.
     */
    public void clearPatternCache() {
        patternCache.clear();
        LOG.info("Pattern cache cleared");
    }
}