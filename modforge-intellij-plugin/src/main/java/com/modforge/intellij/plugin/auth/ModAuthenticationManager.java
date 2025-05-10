package com.modforge.intellij.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for managing ModForge authentication.
 */
@Service
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    private JSONObject userData = null;
    private boolean authenticated = false;
    
    /**
     * Get instance of ModAuthenticationManager.
     * @return ModAuthenticationManager instance
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }
    
    /**
     * Login to ModForge with username and password.
     * @param username Username
     * @param password Password
     * @return Whether login was successful
     * @throws IOException If an I/O error occurs
     * @throws ParseException If JSON parsing fails
     */
    public boolean login(String username, String password) throws IOException, ParseException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Username or password is empty");
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl == null || serverUrl.isEmpty()) {
            LOG.warn("Server URL is not set");
            return false;
        }
        
        // Create login data
        JSONObject loginData = new JSONObject();
        loginData.put("username", username);
        loginData.put("password", password);
        
        try {
            // Call login API
            String loginUrl = serverUrl + "/api/login";
            HttpURLConnection connection = ConnectionTestUtil.createConnection(loginUrl, "POST");
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + loginUrl);
                return false;
            }
            
            // Set content type
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Write login data
            ConnectionTestUtil.writeJsonToConnection(connection, loginData);
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from login API");
                    return false;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    userData = (JSONObject) responseObj;
                    
                    // Get token from response
                    Object tokenObj = userData.get("token");
                    if (tokenObj instanceof String) {
                        String token = (String) tokenObj;
                        
                        // Set token in settings
                        settings.setAccessToken(token);
                        
                        // Try to get user data to verify token
                        boolean verified = verifyAuthentication();
                        
                        if (verified) {
                            authenticated = true;
                            return true;
                        } else {
                            LOG.warn("Failed to verify token");
                            return false;
                        }
                    } else {
                        LOG.warn("Token not found in response");
                        return false;
                    }
                } else {
                    LOG.warn("Response is not a JSON object");
                    return false;
                }
            } else {
                LOG.warn("Login failed with response code " + responseCode);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error during login", e);
            throw e;
        }
    }
    
    /**
     * Login with token.
     * @param token Access token
     * @return Whether login was successful
     * @throws IOException If an I/O error occurs
     */
    public boolean loginWithToken(String token) throws IOException {
        if (token == null || token.isEmpty()) {
            LOG.warn("Token is empty");
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Set token in settings
        settings.setAccessToken(token);
        
        try {
            // Try to get user data to verify token
            boolean verified = verifyAuthentication();
            
            if (verified) {
                authenticated = true;
                return true;
            } else {
                LOG.warn("Failed to verify token");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error during token login", e);
            return false;
        }
    }
    
    /**
     * Logout from ModForge.
     */
    public void logout() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Clear token
        settings.setAccessToken("");
        
        // Clear user data
        userData = null;
        authenticated = false;
    }
    
    /**
     * Get whether user is authenticated.
     * @return Whether user is authenticated
     */
    public boolean isAuthenticated() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getAccessToken();
        
        return authenticated && token != null && !token.isEmpty();
    }
    
    /**
     * Verify authentication by getting user data.
     * @return Whether authentication is valid
     */
    public boolean verifyAuthentication() {
        if (!isAuthenticated()) {
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = settings.getAccessToken();
        
        if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            // Call user API
            String userUrl = serverUrl + "/api/auth/me";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(userUrl, "GET", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + userUrl);
                return false;
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response
                String response = ConnectionTestUtil.readResponseFromConnection(connection);
                
                if (response == null || response.isEmpty()) {
                    LOG.warn("Empty response from user API");
                    return false;
                }
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                Object responseObj = parser.parse(response);
                
                if (responseObj instanceof JSONObject) {
                    userData = (JSONObject) responseObj;
                    return true;
                } else {
                    LOG.warn("Response is not a JSON object");
                    return false;
                }
            } else {
                LOG.warn("Verification failed with response code " + responseCode);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error during verification", e);
            return false;
        }
    }
    
    /**
     * Get username of authenticated user.
     * @return Username
     */
    public String getUsername() {
        if (userData == null) {
            return null;
        }
        
        Object usernameObj = userData.get("username");
        if (usernameObj instanceof String) {
            return (String) usernameObj;
        }
        
        return null;
    }
    
    /**
     * Get user data of authenticated user.
     * @return User data
     */
    public JSONObject getUserData() {
        return userData;
    }
    
    /**
     * Parse JWT token to get user data.
     * @param token JWT token
     * @return User data
     */
    private JSONObject parseToken(String token) {
        try {
            // Split token into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                LOG.warn("Invalid token format");
                return null;
            }
            
            // Decode payload
            String payload = parts[1];
            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);
            
            // Parse JSON
            JSONParser parser = new JSONParser();
            Object payloadObj = parser.parse(decodedPayload);
            
            if (payloadObj instanceof JSONObject) {
                return (JSONObject) payloadObj;
            } else {
                LOG.warn("Decoded payload is not a JSON object");
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error parsing token", e);
            return null;
        }
    }
}