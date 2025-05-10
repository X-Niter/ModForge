package com.modforge.intellij.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Base64;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Manager for ModForge authentication.
 * Handles login, logout, and token management.
 */
@Service
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    private String authToken;
    private String username;
    private boolean authenticated = false;
    
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }
    
    /**
     * Login to ModForge.
     *
     * @param username The username
     * @param password The password
     * @return Whether the login was successful
     */
    public boolean login(@NotNull String username, @NotNull String password) {
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            
            if (serverUrl.isEmpty()) {
                LOG.warn("Server URL is empty");
                return false;
            }
            
            // Create login URL
            URL url = new URL(serverUrl + "/api/login");
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Create request body
            String jsonInputString = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", 
                    username.replace("\"", "\\\""), 
                    password.replace("\"", "\\\""));
            
            // Send request
            try (var os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // Read token from response
                String response = TokenAuthConnectionUtil.readResponse(connection);
                if (response != null && !response.isEmpty()) {
                    // Try to find token in response (simple approach - might need improvement)
                    if (response.contains("\"token\":")) {
                        String token = response.split("\"token\":")[1].split("\"")[1];
                        this.authToken = token;
                        this.username = username;
                        this.authenticated = true;
                        return true;
                    }
                }
            }
            
            LOG.warn("Login failed with response code: " + responseCode);
            return false;
        } catch (IOException e) {
            LOG.error("Error during login", e);
            return false;
        }
    }
    
    /**
     * Verify authentication with server.
     *
     * @return Whether the authentication is valid
     */
    public boolean verifyAuthentication() {
        if (!authenticated || authToken == null) {
            return false;
        }
        
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            
            if (serverUrl.isEmpty()) {
                LOG.warn("Server URL is empty");
                return false;
            }
            
            // Create verify URL
            URL url = new URL(serverUrl + "/api/auth/verify");
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            
            return responseCode == 200;
        } catch (IOException e) {
            LOG.error("Error during authentication verification", e);
            return false;
        }
    }
    
    /**
     * Get the authorization header value for API requests.
     *
     * @return The authorization header value, or null if not authenticated
     */
    @Nullable
    public String getAuthorizationHeader() {
        if (!authenticated || authToken == null) {
            return null;
        }
        
        return "Bearer " + authToken;
    }
    
    /**
     * Logout from ModForge.
     */
    public void logout() {
        authenticated = false;
        authToken = null;
        username = null;
    }
    
    /**
     * Check if authenticated.
     *
     * @return Whether authenticated
     */
    public boolean isAuthenticated() {
        return authenticated && authToken != null;
    }
    
    /**
     * Get the username.
     *
     * @return The username, or null if not authenticated
     */
    @Nullable
    public String getUsername() {
        return username;
    }
    
    /**
     * Get the auth token.
     *
     * @return The auth token, or null if not authenticated
     */
    @Nullable
    public String getAuthToken() {
        return authToken;
    }
}