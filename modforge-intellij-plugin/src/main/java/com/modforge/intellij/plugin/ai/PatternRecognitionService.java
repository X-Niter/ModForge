package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.components.Service;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for pattern recognition in AI tasks.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private final Project project;
    
    // Metrics
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private final AtomicInteger patternStorageCount = new AtomicInteger(0);
    
    /**
     * Create PatternRecognitionService.
     * @param project Project
     */
    public PatternRecognitionService(Project project) {
        this.project = project;
    }
    
    /**
     * Store a pattern for future use.
     * @param patternType Type of pattern (code, error, feature, etc.)
     * @param inputData Input data for pattern
     * @param outputData Output data for pattern
     * @return Whether pattern was stored successfully
     */
    public boolean storePattern(String patternType, JSONObject inputData, JSONObject outputData) {
        try {
            // Check if pattern recognition is enabled
            ModForgeSettings settings = ModForgeSettings.getInstance();
            if (!settings.isPatternRecognition()) {
                LOG.info("Pattern recognition is disabled");
                return false;
            }
            
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return false;
            }
            
            // Create pattern data
            JSONObject patternData = new JSONObject();
            patternData.put("patternType", patternType);
            patternData.put("inputData", inputData);
            patternData.put("outputData", outputData);
            
            // Call API
            String serverUrl = settings.getServerUrl();
            String token = settings.getAccessToken();
            
            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                LOG.warn("Server URL or token is empty");
                return false;
            }
            
            // Call store pattern API
            String storePatternUrl = serverUrl + "/api/store-pattern";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(storePatternUrl, "POST", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + storePatternUrl);
                return false;
            }
            
            // Set content type
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Write pattern data
            ConnectionTestUtil.writeJsonToConnection(connection, patternData);
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from store pattern API");
                    return false;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    JSONObject responseJson = (JSONObject) responseObj;
                    
                    // Get success status
                    Object successObj = responseJson.get("success");
                    if (successObj instanceof Boolean && (Boolean) successObj) {
                        // Increment pattern storage count
                        patternStorageCount.incrementAndGet();
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    LOG.warn("Response is not a JSON object");
                    return false;
                }
            } else {
                LOG.warn("Store pattern failed with response code " + responseCode);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error storing pattern", e);
            return false;
        }
    }
    
    /**
     * Find a pattern for given input data.
     * @param patternType Type of pattern (code, error, feature, etc.)
     * @param inputData Input data for pattern
     * @return Output data for pattern or null if no pattern found
     */
    public JSONObject findPattern(String patternType, JSONObject inputData) {
        try {
            // Check if pattern recognition is enabled
            ModForgeSettings settings = ModForgeSettings.getInstance();
            if (!settings.isPatternRecognition()) {
                LOG.info("Pattern recognition is disabled");
                return null;
            }
            
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            // Create search data
            JSONObject searchData = new JSONObject();
            searchData.put("patternType", patternType);
            searchData.put("inputData", inputData);
            
            // Call API
            String serverUrl = settings.getServerUrl();
            String token = settings.getAccessToken();
            
            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                LOG.warn("Server URL or token is empty");
                return null;
            }
            
            // Call find pattern API
            String findPatternUrl = serverUrl + "/api/find-pattern";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(findPatternUrl, "POST", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + findPatternUrl);
                return null;
            }
            
            // Set content type
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Write search data
            ConnectionTestUtil.writeJsonToConnection(connection, searchData);
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from find pattern API");
                    return null;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    JSONObject responseJson = (JSONObject) responseObj;
                    
                    // Get pattern found status
                    Object patternFoundObj = responseJson.get("patternFound");
                    if (patternFoundObj instanceof Boolean && (Boolean) patternFoundObj) {
                        // Increment pattern matches count
                        patternMatches.incrementAndGet();
                        
                        // Get output data
                        Object outputDataObj = responseJson.get("outputData");
                        if (outputDataObj instanceof JSONObject) {
                            return (JSONObject) outputDataObj;
                        } else {
                            LOG.warn("Output data not found in response");
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    LOG.warn("Response is not a JSON object");
                    return null;
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No pattern found
                return null;
            } else {
                LOG.warn("Find pattern failed with response code " + responseCode);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error finding pattern", e);
            return null;
        }
    }
    
    /**
     * Get number of pattern matches.
     * @return Number of pattern matches
     */
    public int getPatternMatches() {
        return patternMatches.get();
    }
    
    /**
     * Get number of patterns stored.
     * @return Number of patterns stored
     */
    public int getPatternStorageCount() {
        return patternStorageCount.get();
    }
    
    /**
     * Reset pattern match count.
     */
    public void resetMetrics() {
        patternMatches.set(0);
        patternStorageCount.set(0);
    }
}