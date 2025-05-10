package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Service that manages autonomous code generation.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private int successfulPatternMatches = 0;
    private int totalApiFallbacks = 0;
    private int totalRequests = 0;
    
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
    }
    
    /**
     * Process mods from server response.
     * @param response Response from server
     */
    public void processMods(JSONObject response) {
        if (response == null) {
            LOG.error("Response is null");
            return;
        }
        
        if (!response.containsKey("mods")) {
            LOG.error("Response doesn't contain mods");
            return;
        }
        
        try {
            JSONArray mods = (JSONArray) response.get("mods");
            
            LOG.info("Processing " + mods.size() + " mods");
            
            for (Object modObj : mods) {
                JSONObject mod = (JSONObject) modObj;
                processMod(mod);
            }
        } catch (Exception e) {
            LOG.error("Error processing mods", e);
        }
    }
    
    /**
     * Process a single mod.
     * @param mod Mod data
     */
    private void processMod(JSONObject mod) {
        long modId = (Long) mod.get("id");
        LOG.info("Processing mod #" + modId);
        
        // Check if development is needed
        if (needsDevelopment(mod)) {
            developMod(modId);
        }
    }
    
    /**
     * Check if a mod needs development.
     * @param mod Mod data
     * @return Whether the mod needs development
     */
    private boolean needsDevelopment(JSONObject mod) {
        // Check for compilation errors
        if (mod.containsKey("hasErrors") && (Boolean) mod.get("hasErrors")) {
            LOG.info("Mod has compilation errors, needs development");
            return true;
        }
        
        // Check if continuous development is requested
        if (mod.containsKey("continuousDevelopment") && (Boolean) mod.get("continuousDevelopment")) {
            LOG.info("Continuous development is requested for mod");
            return true;
        }
        
        return false;
    }
    
    /**
     * Request mod development from server.
     * @param modId Mod ID
     */
    private void developMod(long modId) {
        LOG.info("Requesting development for mod #" + modId);
        
        totalRequests++;
        
        // Check authentication
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            LOG.warn("Not authenticated, skipping development");
            return;
        }
        
        // Check connection
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = settings.getAccessToken();
        
        // Check if pattern recognition is enabled
        boolean usePatterns = settings.isPatternRecognition();
        
        // Get pattern recognition service if needed
        PatternRecognitionService patternService = null;
        if (usePatterns) {
            patternService = project.getService(PatternRecognitionService.class);
        }
        
        try {
            // Create development request
            JSONObject requestData = new JSONObject();
            requestData.put("modId", modId);
            requestData.put("usePatterns", usePatterns);
            
            // Send request to server
            JSONObject response = TokenAuthConnectionUtil.post(serverUrl, "/api/mods/" + modId + "/develop", token, requestData);
            
            if (response == null) {
                LOG.error("Failed to send development request to server");
                return;
            }
            
            // Process response
            processDevelopmentResponse(response, patternService);
        } catch (Exception e) {
            LOG.error("Error requesting development", e);
        }
    }
    
    /**
     * Process development response from server.
     * @param response Response from server
     * @param patternService Pattern recognition service
     */
    private void processDevelopmentResponse(JSONObject response, PatternRecognitionService patternService) {
        if (response == null) {
            return;
        }
        
        // Check for success
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            LOG.info("Development request was successful");
            
            // Check if pattern was used
            if (response.containsKey("patternUsed") && (Boolean) response.get("patternUsed")) {
                successfulPatternMatches++;
                
                if (patternService != null) {
                    patternService.recordPatternSuccess((String) response.get("patternId"));
                }
            } else {
                totalApiFallbacks++;
                
                // Record pattern failure if pattern recognition is enabled
                if (patternService != null && response.containsKey("patternId")) {
                    patternService.recordPatternFailure((String) response.get("patternId"));
                }
            }
        } else {
            LOG.error("Development request failed: " + response.get("message"));
        }
    }
    
    /**
     * Get the number of successful pattern matches.
     * @return Number of successful pattern matches
     */
    public int getSuccessfulPatternMatches() {
        return successfulPatternMatches;
    }
    
    /**
     * Get the number of API fallbacks.
     * @return Number of API fallbacks
     */
    public int getTotalApiFallbacks() {
        return totalApiFallbacks;
    }
    
    /**
     * Get the total number of requests.
     * @return Total number of requests
     */
    public int getTotalRequests() {
        return totalRequests;
    }
    
    /**
     * Get the pattern success rate.
     * @return Pattern success rate
     */
    public double getPatternSuccessRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        
        return (double) successfulPatternMatches / totalRequests;
    }
    
    /**
     * Reset the metrics.
     */
    public void resetMetrics() {
        successfulPatternMatches = 0;
        totalApiFallbacks = 0;
        totalRequests = 0;
    }
}