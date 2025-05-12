package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Manager for authentication in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    private final Project project;
    private final ModForgeNotificationService notificationService;
    private final ModForgeSettings settings;
    
    private boolean isAuthenticated = false;
    private String username = null;
    private String token = null;

    /**
     * Creates a new instance of the authentication manager.
     *
     * @param project The project.
     */
    public ModAuthenticationManager(Project project) {
        this.project = project;
        this.notificationService = ModForgeNotificationService.getInstance(project);
        this.settings = ModForgeSettings.getInstance();
        LOG.info("ModAuthenticationManager initialized for project: " + project.getName());
    }

    /**
     * Gets the instance of the authentication manager for the specified project.
     *
     * @param project The project.
     * @return The authentication manager.
     */
    public static ModAuthenticationManager getInstance(@NotNull Project project) {
        return project.getService(ModAuthenticationManager.class);
    }

    /**
     * Logs in with the specified credentials.
     *
     * @param username The username.
     * @param password The password.
     * @return A future that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> login(@NotNull String username, @NotNull String password) {
        LOG.info("Logging in with username: " + username);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call an authentication API
                // For now, simulate successful login
                Thread.sleep(1000); // Simulate network delay
                
                // For demonstration, accept any non-empty credentials
                if (username.isEmpty() || password.isEmpty()) {
                    LOG.warn("Login failed: Empty credentials");
                    return false;
                }
                
                this.isAuthenticated = true;
                this.username = username;
                this.token = generateDummyToken(username);
                
                // Save username to settings
                settings.setUsername(username);
                
                LOG.info("Login successful for username: " + username);
                
                return true;
            } catch (Exception e) {
                LOG.error("Login failed", e);
                this.isAuthenticated = false;
                this.username = null;
                this.token = null;
                return false;
            }
        });
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        LOG.info("Logging out user: " + (username != null ? username : "unknown"));
        
        isAuthenticated = false;
        username = null;
        token = null;
        
        LOG.info("Logout successful");
    }

    /**
     * Checks whether the user is authenticated.
     *
     * @return Whether the user is authenticated.
     */
    public boolean isAuthenticated() {
        return isAuthenticated && username != null && token != null;
    }

    /**
     * Gets the username of the authenticated user.
     *
     * @return The username, or null if not authenticated.
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * Gets the authentication token.
     *
     * @return The token, or null if not authenticated.
     */
    @Nullable
    public String getToken() {
        return token;
    }

    /**
     * Verifies the authentication status.
     *
     * @return A future that completes with a boolean indicating whether authentication is valid.
     */
    public CompletableFuture<Boolean> verifyAuthentication() {
        LOG.info("Verifying authentication");
        
        if (!isAuthenticated || username == null || token == null) {
            LOG.warn("Authentication verification failed: Not authenticated");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call an API to verify the token
                // For now, simulate successful verification
                Thread.sleep(800); // Simulate network delay
                
                LOG.info("Authentication verification successful");
                
                return true;
            } catch (Exception e) {
                LOG.error("Authentication verification failed", e);
                return false;
            }
        });
    }

    /**
     * Authenticates with a token.
     *
     * @param token The token.
     * @return A future that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> authenticateWithToken(@NotNull String token) {
        LOG.info("Authenticating with token");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call an API to validate the token
                // For now, simulate successful authentication
                Thread.sleep(800); // Simulate network delay
                
                // For demonstration, extract username from token
                this.username = extractUsernameFromToken(token);
                this.token = token;
                this.isAuthenticated = true;
                
                LOG.info("Token authentication successful for username: " + username);
                
                return true;
            } catch (Exception e) {
                LOG.error("Token authentication failed", e);
                this.isAuthenticated = false;
                this.username = null;
                this.token = null;
                return false;
            }
        });
    }

    /**
     * Tests the authentication endpoints.
     *
     * @param serverUrl The server URL.
     * @return A future that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> testAuthEndpoints(@NotNull String serverUrl) {
        LOG.info("Testing authentication endpoints at: " + serverUrl);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would test the authentication API endpoints
                // For now, simulate successful test
                Thread.sleep(1200); // Simulate network delay
                
                // Make a simple HTTP request to check if the server is reachable
                URL url = new URL(serverUrl + "/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                
                LOG.info("Authentication endpoint test result: " + responseCode);
                
                return responseCode >= 200 && responseCode < 300;
            } catch (Exception e) {
                LOG.error("Authentication endpoint test failed", e);
                return false;
            }
        });
    }

    /**
     * Generates a dummy token for demonstration purposes.
     *
     * @param username The username.
     * @return A dummy token.
     */
    private String generateDummyToken(String username) {
        String payload = "{\"sub\":\"" + username + "\",\"iat\":" + System.currentTimeMillis() / 1000 + "}";
        String base64Payload = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "header." + base64Payload + ".signature";
    }

    /**
     * Extracts the username from a token for demonstration purposes.
     *
     * @param token The token.
     * @return The extracted username.
     */
    private String extractUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return "unknown";
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            
            // Simple JSON parsing for demonstration
            if (payload.contains("\"sub\":")) {
                int start = payload.indexOf("\"sub\":\"") + 7;
                int end = payload.indexOf("\"", start);
                if (start >= 7 && end > start) {
                    return payload.substring(start, end);
                }
            }
            
            return "unknown";
        } catch (Exception e) {
            LOG.error("Failed to extract username from token", e);
            return "unknown";
        }
    }
}