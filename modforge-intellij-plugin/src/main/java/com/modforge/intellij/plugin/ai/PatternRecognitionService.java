package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that manages pattern recognition and learning.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private final Project project;
    private final ScheduledExecutorService executor;
    private final Map<String, Integer> patternSuccessCount = new HashMap<>();
    private final Map<String, Integer> patternFailureCount = new HashMap<>();
    private int totalSuccesses = 0;
    private int totalFailures = 0;
    
    public PatternRecognitionService(Project project) {
        this.project = project;
        this.executor = AppExecutorUtil.getAppScheduledExecutorService();
        
        // Start periodic sync
        startPeriodicSync();
    }
    
    /**
     * Start periodic synchronization of pattern metrics with server.
     */
    private void startPeriodicSync() {
        executor.scheduleWithFixedDelay(
                this::syncPatternMetrics,
                60000, // Initial delay of 1 minute
                30 * 60 * 1000, // Every 30 minutes
                TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Synchronize pattern metrics with server.
     */
    private void syncPatternMetrics() {
        LOG.info("Synchronizing pattern metrics with server");
        
        try {
            // Check authentication
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated, skipping pattern metrics sync");
                return;
            }
            
            // Check if there's anything to sync
            if (totalSuccesses == 0 && totalFailures == 0) {
                LOG.info("No pattern metrics to sync");
                return;
            }
            
            // Prepare metrics data
            JSONObject metricsData = new JSONObject();
            metricsData.put("successes", totalSuccesses);
            metricsData.put("failures", totalFailures);
            
            JSONObject successPatterns = new JSONObject();
            for (Map.Entry<String, Integer> entry : patternSuccessCount.entrySet()) {
                if (entry.getKey() != null) {
                    successPatterns.put(entry.getKey(), entry.getValue());
                }
            }
            metricsData.put("successPatterns", successPatterns);
            
            JSONObject failurePatterns = new JSONObject();
            for (Map.Entry<String, Integer> entry : patternFailureCount.entrySet()) {
                if (entry.getKey() != null) {
                    failurePatterns.put(entry.getKey(), entry.getValue());
                }
            }
            metricsData.put("failurePatterns", failurePatterns);
            
            // Get server URL and token
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            String token = settings.getAccessToken();
            
            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                LOG.error("Invalid server URL or token for syncing metrics");
                return;
            }
            
            // Send metrics to server
            JSONObject response = TokenAuthConnectionUtil.post(serverUrl, "/api/patterns/metrics", token, metricsData);
            
            if (response != null) {
                boolean success = false;
                if (response.containsKey("success")) {
                    Object successObj = response.get("success");
                    if (successObj instanceof Boolean) {
                        success = (Boolean) successObj;
                    }
                }
                
                if (success) {
                    LOG.info("Pattern metrics synchronized successfully");
                    
                    // Reset local metrics
                    resetMetrics();
                } else {
                    LOG.error("Failed to synchronize pattern metrics: " + 
                              (response.containsKey("message") ? response.get("message") : "Unknown error"));
                }
            } else {
                LOG.error("No response when synchronizing pattern metrics");
            }
        } catch (Exception e) {
            LOG.error("Error synchronizing pattern metrics", e);
        }
    }
    
    /**
     * Record a successful pattern match.
     * @param patternId Pattern ID
     */
    public void recordPatternSuccess(String patternId) {
        if (patternId == null || patternId.isEmpty()) {
            return;
        }
        
        LOG.info("Recording success for pattern " + patternId);
        
        patternSuccessCount.put(patternId, patternSuccessCount.getOrDefault(patternId, 0) + 1);
        totalSuccesses++;
    }
    
    /**
     * Record a failed pattern match.
     * @param patternId Pattern ID
     */
    public void recordPatternFailure(String patternId) {
        if (patternId == null || patternId.isEmpty()) {
            return;
        }
        
        LOG.info("Recording failure for pattern " + patternId);
        
        patternFailureCount.put(patternId, patternFailureCount.getOrDefault(patternId, 0) + 1);
        totalFailures++;
    }
    
    /**
     * Get pattern success rate.
     * @return Success rate (0-1)
     */
    public double getSuccessRate() {
        int total = totalSuccesses + totalFailures;
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) totalSuccesses / total;
    }
    
    /**
     * Get total number of pattern matches (both success and failure).
     * @return Total number of pattern matches
     */
    public int getTotalMatches() {
        return totalSuccesses + totalFailures;
    }
    
    /**
     * Get total number of successful pattern matches.
     * @return Total number of successful pattern matches
     */
    public int getTotalSuccesses() {
        return totalSuccesses;
    }
    
    /**
     * Get total number of failed pattern matches.
     * @return Total number of failed pattern matches
     */
    public int getTotalFailures() {
        return totalFailures;
    }
    
    /**
     * Get number of unique patterns recognized.
     * @return Number of unique patterns
     */
    public int getUniquePatternCount() {
        // Combine keys from both maps
        Map<String, Boolean> uniquePatterns = new HashMap<>();
        
        for (String patternId : patternSuccessCount.keySet()) {
            uniquePatterns.put(patternId, true);
        }
        
        for (String patternId : patternFailureCount.keySet()) {
            uniquePatterns.put(patternId, true);
        }
        
        return uniquePatterns.size();
    }
    
    /**
     * Reset pattern metrics.
     */
    public void resetMetrics() {
        patternSuccessCount.clear();
        patternFailureCount.clear();
        totalSuccesses = 0;
        totalFailures = 0;
    }
    
    /**
     * Force sync pattern metrics with server.
     */
    public void forceSyncMetrics() {
        syncPatternMetrics();
    }
}