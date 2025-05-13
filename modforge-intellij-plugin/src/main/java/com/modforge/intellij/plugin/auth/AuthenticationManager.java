package com.modforge.intellij.plugin.auth;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Manages authentication with the ModForge server.
 */
public class AuthenticationManager {
    private static final Logger LOG = Logger.getInstance(AuthenticationManager.class);
    private static final String AUTH_ENDPOINT = "/api/token";
    private static final String LOGOUT_ENDPOINT = "/api/logout";
    private static final String VERIFY_ENDPOINT = "/api/auth/verify";
    private static final int TIMEOUT = 5000; // 5 seconds
    
    private String username;
    private String password;
    
    /**
     * Constructor.
     */
    public AuthenticationManager() {
        // Load credentials from settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        username = settings.getUsername();
        password = settings.getPassword();
    }
    
    // No longer a singleton service, instantiate directly
    
    /**
     * Set credentials.
     * @param username The username
     * @param password The password
     */
    public void setCredentials(@NotNull String username, @NotNull String password) {
        this.username = username;
        this.password = password;
    }
    
    /**
     * Authenticate with the ModForge server.
     * @return Whether authentication was successful
     */
    public boolean authenticate() {
        if (username.isEmpty() || password.isEmpty()) {
            LOG.warn("Cannot authenticate with empty credentials");
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl.isEmpty()) {
            LOG.warn("Cannot authenticate with empty server URL");
            return false;
        }
        
        HttpURLConnection connection = null;
        
        try {
            // Normalize the URL (add trailing slash if needed)
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Remove "api" if it's already in the URL to avoid duplication
            if (serverUrl.endsWith("/api/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 4);
            }
            
            URL url = new URL(serverUrl + AUTH_ENDPOINT.substring(1));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Create JSON payload
            String jsonPayload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
            
            // Write payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                
                // Extract token (this is a simplistic approach, consider using a JSON parser)
                String responseStr = response.toString();
                
                // Find access token
                int tokenStart = responseStr.indexOf("\"token\":");
                if (tokenStart != -1) {
                    tokenStart = responseStr.indexOf("\"", tokenStart + 8) + 1;
                    int tokenEnd = responseStr.indexOf("\"", tokenStart);
                    if (tokenStart != -1 && tokenEnd != -1) {
                        String token = responseStr.substring(tokenStart, tokenEnd);
                        settings.setAccessToken(token);
                        settings.setAuthenticated(true);
                        
                        // Extract user ID and username if available
                        int userIdStart = responseStr.indexOf("\"userId\":");
                        if (userIdStart != -1) {
                            userIdStart += 9; // Length of "\"userId\":"
                            int userIdEnd = responseStr.indexOf(",", userIdStart);
                            if (userIdEnd == -1) {
                                userIdEnd = responseStr.indexOf("}", userIdStart);
                            }
                            if (userIdEnd != -1) {
                                String userIdStr = responseStr.substring(userIdStart, userIdEnd).trim();
                                try {
                                    int userId = Integer.parseInt(userIdStr);
                                    settings.setUserId(String.valueOf(userId));
                                } catch (NumberFormatException e) {
                                    LOG.warn("Failed to parse user ID: " + userIdStr, e);
                                }
                            }
                        }
                        
                        LOG.info("Successfully authenticated with ModForge server");
                        return true;
                    }
                }
                
                LOG.warn("Received 200 response but could not extract access token");
                return false;
            } else {
                LOG.warn("Authentication failed with response code: " + responseCode);
                settings.setAuthenticated(false);
                settings.setAccessToken("");
                return false;
            }
        } catch (IOException e) {
            LOG.warn("Failed to authenticate with ModForge server: " + e.getMessage());
            settings.setAuthenticated(false);
            settings.setAccessToken("");
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Verify whether the current authentication is valid.
     * @return Whether authentication is valid
     */
    public boolean verifyAuthentication() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            return false;
        }
        
        // Use TokenAuthConnectionUtil to verify authentication
        boolean isValid = TokenAuthConnectionUtil.testTokenAuthentication();
        
        if (!isValid) {
            // Clear authentication state
            settings.setAuthenticated(false);
            settings.setAccessToken("");
            settings.setUserId("");
            LOG.warn("Authentication is no longer valid");
        } else {
            LOG.info("Successfully verified authentication with ModForge server");
        }
        
        return isValid;
    }
    
    /**
     * Logout from the ModForge server.
     * @return Whether logout was successful
     */
    public boolean logout() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            // Already logged out
            settings.setAuthenticated(false);
            settings.setAccessToken("");
            settings.setUserId(0);
            return true;
        }
        
        // Call the logout endpoint
        String response = TokenAuthConnectionUtil.makeAuthenticatedPostRequest(LOGOUT_ENDPOINT, null);
        boolean success = response != null;
        
        // Clear authentication state regardless of response
        settings.setAuthenticated(false);
        settings.setAccessToken("");
        settings.setUserId(0);
        
        if (!success) {
            LOG.warn("Failed to logout from ModForge server");
        } else {
            LOG.info("Successfully logged out from ModForge server");
        }
        
        return success;
    }
    
    /**
     * Get the username.
     * @return The username
     */
    @NotNull
    public String getUsername() {
        return username != null ? username : "";
    }
    
    /**
     * Get the authentication token.
     * @return The authentication token
     */
    @NotNull
    public String getToken() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.getAccessToken();
    }
    
    /**
     * Get the server URL.
     * @return The server URL
     */
    @NotNull
    public String getServerUrl() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.getServerUrl();
    }
    
    /**
     * Get the GitHub token.
     * @return The GitHub token, or null if not available
     */
    @Nullable
    public String getGitHubToken() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.getGitHubToken();
    }
    
    /**
     * Login with the given credentials.
     * This is an alias for setCredentials() + authenticate() for API compatibility.
     * 
     * @param username The username
     * @param password The password
     * @return Whether login was successful
     */
    public boolean login(@NotNull String username, @NotNull String password) {
        setCredentials(username, password);
        return authenticate();
    }
}