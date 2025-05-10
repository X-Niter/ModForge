package com.modforge.intellij.plugin.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.components.Service;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for autonomous code generation.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final ExecutorService executorService;
    
    // Metrics
    private final AtomicInteger successfulPatternMatches = new AtomicInteger(0);
    private final AtomicInteger apiFallbacks = new AtomicInteger(0);
    
    /**
     * Create AutonomousCodeGenerationService.
     * @param project Project
     */
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
        this.executorService = AppExecutorUtil.createBoundedApplicationPoolExecutor("ModForge Autonomous Code Generation", 4);
    }
    
    /**
     * Generate code asynchronously.
     * @param description Code description
     * @param className Optional class name
     * @param fileName Optional file name
     * @return CompletableFuture with generated code
     */
    public CompletableFuture<String> generateCodeAsync(String description, String className, String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateCode(description, className, fileName);
            } catch (Exception e) {
                LOG.error("Error generating code", e);
                throw new RuntimeException("Error generating code: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    /**
     * Generate code.
     * @param description Code description
     * @param className Optional class name
     * @param fileName Optional file name
     * @return Generated code
     */
    public String generateCode(String description, String className, String fileName) {
        try {
            // Check if inputs are valid
            if (description == null || description.isEmpty()) {
                LOG.warn("Code description is empty");
                return null;
            }
            
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            // Create input data
            JSONObject inputData = new JSONObject();
            inputData.put("description", description);
            
            if (className != null && !className.isEmpty()) {
                inputData.put("className", className);
            }
            
            if (fileName != null && !fileName.isEmpty()) {
                inputData.put("fileName", fileName);
            }
            
            // Check for pattern recognition
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean usePatterns = settings.isPatternRecognition();
            inputData.put("usePatterns", usePatterns);
            
            // Call API
            String serverUrl = settings.getServerUrl();
            String token = settings.getAccessToken();
            
            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                LOG.warn("Server URL or token is empty");
                return null;
            }
            
            // Call code generation API
            String generateUrl = serverUrl + "/api/generate-code";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(generateUrl, "POST", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + generateUrl);
                return null;
            }
            
            // Set content type
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Write input data
            ConnectionTestUtil.writeJsonToConnection(connection, inputData);
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from code generation API");
                    return null;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    JSONObject responseJson = (JSONObject) responseObj;
                    
                    // Check if pattern was used
                    Object patternUsedObj = responseJson.get("patternUsed");
                    if (patternUsedObj instanceof Boolean && (Boolean) patternUsedObj) {
                        successfulPatternMatches.incrementAndGet();
                    } else {
                        apiFallbacks.incrementAndGet();
                    }
                    
                    // Get generated code
                    Object codeObj = responseJson.get("code");
                    if (codeObj instanceof String) {
                        return (String) codeObj;
                    } else {
                        LOG.warn("Code not found in response");
                        return null;
                    }
                } else {
                    LOG.warn("Response is not a JSON object");
                    return null;
                }
            } else {
                LOG.warn("Code generation failed with response code " + responseCode);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error generating code", e);
            return null;
        }
    }
    
    /**
     * Fix code with errors.
     * @param code Code with errors
     * @param fileName File name
     * @return Fixed code
     */
    public String fixCode(String code, String fileName) {
        try {
            // Check if inputs are valid
            if (code == null || code.isEmpty()) {
                LOG.warn("Code is empty");
                return null;
            }
            
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            // Create input data
            JSONObject inputData = new JSONObject();
            inputData.put("code", code);
            
            if (fileName != null && !fileName.isEmpty()) {
                inputData.put("fileName", fileName);
            }
            
            // Check for pattern recognition
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean usePatterns = settings.isPatternRecognition();
            inputData.put("usePatterns", usePatterns);
            
            // Call API
            String serverUrl = settings.getServerUrl();
            String token = settings.getAccessToken();
            
            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                LOG.warn("Server URL or token is empty");
                return null;
            }
            
            // Call fix code API
            String fixUrl = serverUrl + "/api/fix-code";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(fixUrl, "POST", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + fixUrl);
                return null;
            }
            
            // Set content type
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Write input data
            ConnectionTestUtil.writeJsonToConnection(connection, inputData);
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from fix code API");
                    return null;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    JSONObject responseJson = (JSONObject) responseObj;
                    
                    // Check if pattern was used
                    Object patternUsedObj = responseJson.get("patternUsed");
                    if (patternUsedObj instanceof Boolean && (Boolean) patternUsedObj) {
                        successfulPatternMatches.incrementAndGet();
                    } else {
                        apiFallbacks.incrementAndGet();
                    }
                    
                    // Get fixed code
                    Object fixedCodeObj = responseJson.get("code");
                    if (fixedCodeObj instanceof String) {
                        return (String) fixedCodeObj;
                    } else {
                        LOG.warn("Fixed code not found in response");
                        return null;
                    }
                } else {
                    LOG.warn("Response is not a JSON object");
                    return null;
                }
            } else {
                LOG.warn("Fix code failed with response code " + responseCode);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error fixing code", e);
            return null;
        }
    }
    
    /**
     * Add features to mod.
     * @param featureDescription Feature description
     * @return Whether features were added successfully
     */
    public boolean addFeatures(String featureDescription) {
        try {
            // Check if inputs are valid
            if (featureDescription == null || featureDescription.isEmpty()) {
                LOG.warn("Feature description is empty");
                return false;
            }
            
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return false;
            }
            
            // Create input data
            JSONObject inputData = new JSONObject();
            inputData.put("featureDescription", featureDescription);
            
            // Check for pattern recognition
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean usePatterns = settings.isPatternRecognition();
            inputData.put("usePatterns", usePatterns);
            
            // Call API
            String serverUrl = settings.getServerUrl();
            String token = settings.getAccessToken();
            
            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                LOG.warn("Server URL or token is empty");
                return false;
            }
            
            // Call add features API
            String addFeaturesUrl = serverUrl + "/api/add-features";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(addFeaturesUrl, "POST", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + addFeaturesUrl);
                return false;
            }
            
            // Set content type
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Write input data
            ConnectionTestUtil.writeJsonToConnection(connection, inputData);
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from add features API");
                    return false;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    JSONObject responseJson = (JSONObject) responseObj;
                    
                    // Check if pattern was used
                    Object patternUsedObj = responseJson.get("patternUsed");
                    if (patternUsedObj instanceof Boolean && (Boolean) patternUsedObj) {
                        successfulPatternMatches.incrementAndGet();
                    } else {
                        apiFallbacks.incrementAndGet();
                    }
                    
                    // Get success status
                    Object successObj = responseJson.get("success");
                    return successObj instanceof Boolean && (Boolean) successObj;
                } else {
                    LOG.warn("Response is not a JSON object");
                    return false;
                }
            } else {
                LOG.warn("Add features failed with response code " + responseCode);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error adding features", e);
            return false;
        }
    }
    
    /**
     * Get number of successful pattern matches.
     * @return Number of successful pattern matches
     */
    public int getSuccessfulPatternMatches() {
        return successfulPatternMatches.get();
    }
    
    /**
     * Get number of API fallbacks.
     * @return Number of API fallbacks
     */
    public int getApiFallbacks() {
        return apiFallbacks.get();
    }
    
    /**
     * Dispose service.
     */
    public void dispose() {
        // Shutdown executor service
        executorService.shutdownNow();
    }
}