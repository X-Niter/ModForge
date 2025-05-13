package com.modforge.intellij.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for ModForge authentication.
 * This service manages authentication with the ModForge server.
 */
@Service
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    private final ModForgeSettings settings;
    private boolean authenticated = false;
    
    /**
     * Creates a new ModForge authentication manager.
     */
    public ModAuthenticationManager() {
        this.settings = ModForgeSettings.getInstance();
    }
    
    /**
     * Gets the ModForge authentication manager.
     *
     * @return The ModForge authentication manager
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }
    
    /**
     * Checks if the user is authenticated.
     *
     * @return True if the user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return settings.isAuthenticated() || authenticated;
    }
    
    /**
     * Gets the ModForge server URL.
     *
     * @return The server URL
     */
    @NotNull
    public String getServerUrl() {
        return settings.getServerUrl();
    }
    
    /**
     * Gets the authentication token.
     *
     * @return The authentication token
     */
    @NotNull
    public String getToken() {
        return settings.getToken();
    }
    
    /**
     * Authenticates with the ModForge server.
     *
     * @param username The username
     * @param password The password
     * @return True if authentication was successful, false otherwise
     */
    @RequiresBackgroundThread
    public boolean authenticate(@NotNull String username, @NotNull String password) {
        try {
            // Get the server URL from settings
            String serverUrl = settings.getServerUrl();
            if (serverUrl.isEmpty()) {
                LOG.warn("Server URL is not configured");
                return false;
            }
            
            // Make sure the URL ends with a slash
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Build the authentication URL
            String authUrl = serverUrl + "api/token";
            
            // Create request body
            String requestBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
            
            // Execute the request
            String response = executePostRequest(authUrl, requestBody);
            if (response == null) {
                LOG.warn("Authentication failed: No response from server");
                return false;
            }
            
            // Parse the response
            String token = parseTokenFromResponse(response);
            if (token == null) {
                LOG.warn("Authentication failed: Invalid response format");
                return false;
            }
            
            // Save the token to settings
            settings.setToken(token);
            settings.setUsername(username);
            
            // Set authenticated flag
            authenticated = true;
            
            return true;
        } catch (Exception e) {
            LOG.warn("Authentication failed", e);
            return false;
        }
    }
    
    /**
     * Authenticates with the ModForge server using a token.
     *
     * @param token The authentication token
     * @return True if authentication was successful, false otherwise
     */
    @RequiresBackgroundThread
    public boolean authenticateWithToken(@NotNull String token) {
        try {
            // Get the server URL from settings
            String serverUrl = settings.getServerUrl();
            if (serverUrl.isEmpty()) {
                LOG.warn("Server URL is not configured");
                return false;
            }
            
            // Test if the token is valid
            boolean valid = TokenAuthConnectionUtil.testTokenAuthentication(serverUrl, token);
            if (!valid) {
                LOG.warn("Token authentication failed: Invalid token");
                return false;
            }
            
            // Save the token to settings
            settings.setToken(token);
            
            // Set authenticated flag
            authenticated = true;
            
            return true;
        } catch (Exception e) {
            LOG.warn("Token authentication failed", e);
            return false;
        }
    }
    
    /**
     * Authenticates with the ModForge server asynchronously.
     *
     * @param username The username
     * @param password The password
     * @return A completable future that completes with true if authentication was successful, false otherwise
     */
    public CompletableFuture<Boolean> authenticateAsync(@NotNull String username, @NotNull String password) {
        return CompletableFuture.supplyAsync(() -> authenticate(username, password));
    }
    
    /**
     * Authenticates with the ModForge server using a token asynchronously.
     *
     * @param token The authentication token
     * @return A completable future that completes with true if authentication was successful, false otherwise
     */
    public CompletableFuture<Boolean> authenticateWithTokenAsync(@NotNull String token) {
        return CompletableFuture.supplyAsync(() -> authenticateWithToken(token));
    }
    
    /**
     * Logs out.
     */
    public void logout() {
        settings.setToken("");
        authenticated = false;
    }
    
    /**
     * Executes a POST request with the given URL and body.
     *
     * @param url The URL
     * @param body The request body
     * @return The response, or null if an error occurs
     */
    @Nullable
    private String executePostRequest(@NotNull String url, @NotNull String body) {
        try {
            com.intellij.util.io.RequestBuilder request = com.intellij.util.io.HttpRequests.post(url, "application/json")
                    .accept("application/json")
                    .productNameAsUserAgent();
            
            return request.connect(connection -> {
                // Write request body
                connection.write(body);
                
                // Read response
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    LOG.warn("HTTP request failed with response code: " + responseCode);
                    return null;
                }
                
                // Read response body
                return new String(connection.getInputStream().readAllBytes());
            });
        } catch (IOException e) {
            LOG.warn("Failed to execute POST request", e);
            return null;
        }
    }
    
    /**
     * Parses a token from a JSON response.
     *
     * @param response The JSON response
     * @return The token, or null if not found
     */
    @Nullable
    private String parseTokenFromResponse(@NotNull String response) {
        try {
            // Simple parsing, could use a JSON library for more robust parsing
            int tokenStartIndex = response.indexOf("\"token\":\"");
            if (tokenStartIndex == -1) {
                return null;
            }
            
            tokenStartIndex += 9; // Length of "\"token\":\""
            int tokenEndIndex = response.indexOf("\"", tokenStartIndex);
            if (tokenEndIndex == -1) {
                return null;
            }
            
            return response.substring(tokenStartIndex, tokenEndIndex);
        } catch (Exception e) {
            LOG.warn("Failed to parse token from response", e);
            return null;
        }
    }
}