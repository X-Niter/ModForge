package com.modforge.intellij.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.HttpConfigurable;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for authentication with the ModForge service.
 */
public class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private String authToken = null;
    private String username = null;

    /**
     * Get the authentication manager instance.
     *
     * @return The authentication manager instance
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }

    /**
     * Check if the user is authenticated.
     *
     * @return True if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        // Check token validity if authenticated
        if (authenticated.get() && authToken != null) {
            // Check token expiration
            if (isTokenExpired(authToken)) {
                logout();
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Get the authentication token.
     *
     * @return The authentication token, or null if not authenticated
     */
    @Nullable
    public String getAuthToken() {
        if (isAuthenticated()) {
            return authToken;
        }
        return null;
    }

    /**
     * Get the username.
     *
     * @return The username, or null if not authenticated
     */
    @Nullable
    public String getUsername() {
        if (isAuthenticated()) {
            return username;
        }
        return null;
    }

    /**
     * Login to the ModForge service.
     *
     * @param username The username
     * @param password The password
     * @return A future with the result of the login attempt
     */
    @NotNull
    public CompletableFuture<Boolean> login(@NotNull String username, @NotNull String password) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // Get server URL from settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set in settings");
                future.complete(false);
                return future;
            }

            // Test connection to server
            ConnectionTestUtil.testConnection(serverUrl)
                .thenAcceptAsync(connectionOk -> {
                    if (!connectionOk) {
                        LOG.error("Connection test failed");
                        future.complete(false);
                        return;
                    }

                    // Perform login
                    TokenAuthConnectionUtil.getAuthToken(serverUrl, username, password)
                        .thenAcceptAsync(loginResponse -> {
                            if (loginResponse == null || loginResponse.token == null) {
                                LOG.error("Login response or token is null");
                                future.complete(false);
                                return;
                            }

                            // Store token
                            this.authToken = loginResponse.token;
                            this.username = username;
                            this.authenticated.set(true);
                            
                            // Save settings if "Remember me" is checked
                            settings.setLastUsername(username);
                            
                            LOG.info("Login successful for user: " + username);
                            future.complete(true);
                        })
                        .exceptionally(ex -> {
                            LOG.error("Error during login", ex);
                            future.complete(false);
                            return null;
                        });
                })
                .exceptionally(ex -> {
                    LOG.error("Error testing connection", ex);
                    future.complete(false);
                    return null;
                });
        } catch (Exception e) {
            LOG.error("Exception during login", e);
            future.complete(false);
        }

        return future;
    }

    /**
     * Login using a token.
     *
     * @param token The token
     * @return A future with the result of the login attempt
     */
    @NotNull
    public CompletableFuture<Boolean> loginWithToken(@NotNull String token) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // Get server URL from settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set in settings");
                future.complete(false);
                return future;
            }

            // Test connection to server
            ConnectionTestUtil.testConnection(serverUrl)
                .thenAcceptAsync(connectionOk -> {
                    if (!connectionOk) {
                        LOG.error("Connection test failed");
                        future.complete(false);
                        return;
                    }

                    // Verify token
                    TokenAuthConnectionUtil.verifyToken(serverUrl, token)
                        .thenAcceptAsync(verified -> {
                            if (!verified) {
                                LOG.error("Token verification failed");
                                future.complete(false);
                                return;
                            }

                            // Get username from token (assuming JWT)
                            String usernameFromToken = extractUsernameFromToken(token);
                            if (usernameFromToken == null) {
                                LOG.error("Failed to extract username from token");
                                future.complete(false);
                                return;
                            }

                            // Store token
                            this.authToken = token;
                            this.username = usernameFromToken;
                            this.authenticated.set(true);
                            
                            LOG.info("Login with token successful for user: " + usernameFromToken);
                            future.complete(true);
                        })
                        .exceptionally(ex -> {
                            LOG.error("Error during token verification", ex);
                            future.complete(false);
                            return null;
                        });
                })
                .exceptionally(ex -> {
                    LOG.error("Error testing connection", ex);
                    future.complete(false);
                    return null;
                });
        } catch (Exception e) {
            LOG.error("Exception during login with token", e);
            future.complete(false);
        }

        return future;
    }

    /**
     * Logout from the ModForge service.
     */
    public void logout() {
        this.authToken = null;
        this.username = null;
        this.authenticated.set(false);
        LOG.info("Logout successful");
    }

    /**
     * Check if a token is expired.
     *
     * @param token The token to check
     * @return True if expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        try {
            // Basic check for token expiration (assuming JWT)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return true; // Invalid token format
            }

            // Decode payload
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            
            // Check for "exp" claim (simplified approach)
            if (payload.contains("\"exp\":")) {
                long exp = Long.parseLong(payload.split("\"exp\":")[1].split("[,}]")[0].trim());
                return System.currentTimeMillis() / 1000 > exp;
            }
            
            return false; // No expiration found
        } catch (Exception e) {
            LOG.error("Error checking token expiration", e);
            return true; // Assume expired on error
        }
    }

    /**
     * Extract username from token.
     *
     * @param token The token
     * @return The username, or null if not found
     */
    @Nullable
    private String extractUsernameFromToken(String token) {
        try {
            // Extract username from JWT token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null; // Invalid token format
            }

            // Decode payload
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            
            // Extract username
            if (payload.contains("\"username\":")) {
                return payload.split("\"username\":")[1].split("\"")[1];
            } else if (payload.contains("\"sub\":")) {
                return payload.split("\"sub\":")[1].split("\"")[1];
            }
            
            return null;
        } catch (Exception e) {
            LOG.error("Error extracting username from token", e);
            return null;
        }
    }
}