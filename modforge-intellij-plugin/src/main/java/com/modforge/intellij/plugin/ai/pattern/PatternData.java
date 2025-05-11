package com.modforge.intellij.plugin.ai.pattern;

import com.intellij.util.xmlb.annotations.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data class for storing pattern information
 * Includes the prompt, response, and statistics about pattern usage
 */
public class PatternData {
    private String prompt;
    private String context;
    private String response;
    private int successCount;
    private int failureCount;
    private long lastUsed;
    private int estimatedTokens;
    private int estimatedCostCents;
    
    @Transient
    private final AtomicInteger successCounter = new AtomicInteger(0);
    
    @Transient
    private final AtomicInteger failureCounter = new AtomicInteger(0);
    
    @Transient
    private final List<String> alternativeResponses = new ArrayList<>();
    
    /**
     * Default constructor for serialization
     */
    public PatternData() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new pattern data instance
     * 
     * @param prompt The prompt for this pattern
     * @param context Optional context for the pattern
     */
    public PatternData(String prompt, String context) {
        this.prompt = prompt;
        this.context = context;
        this.lastUsed = System.currentTimeMillis();
    }
    
    /**
     * Record a successful match for this pattern
     * 
     * @param response The successful response
     * @param estimatedTokens Estimated tokens used
     * @param estimatedCostCents Estimated cost in cents
     */
    public void recordSuccess(String response, int estimatedTokens, int estimatedCostCents) {
        if (this.response == null) {
            this.response = response;
        } else if (!this.response.equals(response) && !alternativeResponses.contains(response)) {
            // Store alternative responses for potential future use
            alternativeResponses.add(response);
        }
        
        // Update statistics
        this.successCount = successCounter.incrementAndGet();
        this.lastUsed = System.currentTimeMillis();
        
        // Update token estimates by averaging
        if (this.estimatedTokens == 0) {
            this.estimatedTokens = estimatedTokens;
        } else {
            this.estimatedTokens = (this.estimatedTokens + estimatedTokens) / 2;
        }
        
        // Update cost estimates by averaging
        if (this.estimatedCostCents == 0) {
            this.estimatedCostCents = estimatedCostCents;
        } else {
            this.estimatedCostCents = (this.estimatedCostCents + estimatedCostCents) / 2;
        }
    }
    
    /**
     * Record a failure for this pattern
     */
    public void recordFailure() {
        this.failureCount = failureCounter.incrementAndGet();
        this.lastUsed = System.currentTimeMillis();
    }
    
    /**
     * Check if this pattern is reliable enough to use
     * 
     * @return True if the pattern is reliable
     */
    public boolean isReliable() {
        // Must have a non-null response
        if (response == null) {
            return false;
        }
        
        // Require a minimum number of successful matches
        if (successCount < 3) {
            return false;
        }
        
        // Calculate success rate
        float totalAttempts = successCount + failureCount;
        float successRate = totalAttempts > 0 ? successCount / totalAttempts : 0;
        
        // Require at least 80% success rate
        return successRate >= 0.8f;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
        this.successCounter.set(successCount);
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
        this.failureCounter.set(failureCount);
    }
    
    public long getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public int getEstimatedTokens() {
        return estimatedTokens;
    }
    
    public void setEstimatedTokens(int estimatedTokens) {
        this.estimatedTokens = estimatedTokens;
    }
    
    public int getEstimatedCostCents() {
        return estimatedCostCents;
    }
    
    public void setEstimatedCostCents(int estimatedCostCents) {
        this.estimatedCostCents = estimatedCostCents;
    }
    
    @Transient
    public List<String> getAlternativeResponses() {
        return new ArrayList<>(alternativeResponses);
    }
}