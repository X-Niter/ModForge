package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for authentication with ModForge services.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.APP)
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);

    private String username;
    private String token;
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    /**
     * Gets the singleton instance of the authentication manager.
     *
     * @return The authentication manager.
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }

    /**
     * Checks if the user is authenticated.
     *
     * @return True if authenticated, false otherwise.
     */
    public boolean isAuthenticated() {
        return authenticated.get() && username != null && token != null;
    }

    /**
     * Gets the username of the authenticated user.
     *
     * @return The username, or null if not authenticated.
     */
    public String getUsername() {
        return isAuthenticated() ? username : null;
    }

    /**
     * Gets the authentication token.
     *
     * @return The token, or null if not authenticated.
     */
    public String getToken() {
        return isAuthenticated() ? token : null;
    }

    /**
     * Authenticates with the ModForge service.
     *
     * @param username The username.
     * @param password The password.
     * @return A CompletableFuture that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> login(String username, String password) {
        LOG.info("Attempting to log in as: " + username);

        // For now, always succeed (real implementation would call API)
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate API call
                Thread.sleep(500);
                
                this.username = username;
                this.token = "mock-token-for-" + username;
                authenticated.set(true);
                
                LOG.info("Successfully authenticated as: " + username);
                return true;
            } catch (Exception e) {
                LOG.error("Authentication failed", e);
                authenticated.set(false);
                return false;
            }
        });
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        LOG.info("Logging out current user");
        
        username = null;
        token = null;
        authenticated.set(false);
    }
}