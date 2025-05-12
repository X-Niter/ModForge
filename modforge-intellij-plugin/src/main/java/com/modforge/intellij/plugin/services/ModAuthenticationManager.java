package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Authentication manager for ModForge.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.PROJECT)
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    private final Project project;
    private final ModForgeSettings settings;
    private final ModForgeNotificationService notificationService;
    
    private String username;
    private String token;
    private boolean isAuthenticated = false;

    /**
     * Creates a new instance of the authentication manager.
     *
     * @param project The project.
     */
    public ModAuthenticationManager(Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance(project);
        
        // Try to initialize from settings
        this.token = settings.getAccessToken();
        this.username = settings.getGitHubUsername();
        this.isAuthenticated = this.token != null && !this.token.isEmpty();
        
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
     * Gets the username of the authenticated user.
     *
     * @return The username, or null if not authenticated.
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * Gets the access token.
     *
     * @return The access token, or null if not authenticated.
     */
    @Nullable
    public String getAccessToken() {
        return token;
    }

    /**
     * Checks if the user is authenticated.
     *
     * @return Whether the user is authenticated.
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    /**
     * Logs in with a username and password.
     *
     * @param username The username.
     * @param password The password.
     * @param callback Callback for success.
     */
    public void login(@NotNull String username, @NotNull String password, @NotNull Consumer<Boolean> callback) {
        LOG.info("Logging in with username: " + username);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the ModForge API to authenticate
                // For now, simulate a successful login with a token
                Thread.sleep(1000);
                
                // Generate a mock token (in the real implementation, this would come from the server)
                String mockToken = "mf_" + System.currentTimeMillis();
                
                LOG.info("Successfully logged in as: " + username);
                
                return mockToken;
            } catch (Exception e) {
                LOG.error("Failed to log in", e);
                return null;
            }
        }).thenAccept(newToken -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (newToken != null) {
                    this.username = username;
                    this.token = newToken;
                    this.isAuthenticated = true;
                    
                    // Save to settings
                    settings.setAccessToken(newToken);
                    settings.setGitHubUsername(username);
                    
                    notificationService.showInfo("Login Successful", "Successfully logged in as " + username);
                    callback.accept(true);
                } else {
                    notificationService.showError("Login Failed", "Failed to log in as " + username);
                    callback.accept(false);
                }
            });
        });
    }

    /**
     * Logs in with a token.
     *
     * @param token The token.
     * @param callback Callback for success.
     */
    public void loginWithToken(@NotNull String token, @NotNull Consumer<Boolean> callback) {
        LOG.info("Logging in with token");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the ModForge API to validate the token
                // For now, simulate a successful token validation and fetch the username
                Thread.sleep(800);
                
                // Get username from token (in the real implementation, this would come from the server)
                String fetchedUsername = "user_" + System.currentTimeMillis();
                
                LOG.info("Successfully validated token for: " + fetchedUsername);
                
                return fetchedUsername;
            } catch (Exception e) {
                LOG.error("Failed to validate token", e);
                return null;
            }
        }).thenAccept(fetchedUsername -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (fetchedUsername != null) {
                    this.username = fetchedUsername;
                    this.token = token;
                    this.isAuthenticated = true;
                    
                    // Save to settings
                    settings.setAccessToken(token);
                    settings.setGitHubUsername(fetchedUsername);
                    
                    notificationService.showInfo("Login Successful", "Successfully logged in as " + fetchedUsername);
                    callback.accept(true);
                } else {
                    notificationService.showError("Login Failed", "Failed to validate token");
                    callback.accept(false);
                }
            });
        });
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        LOG.info("Logging out user: " + username);
        
        CompletableFuture.runAsync(() -> {
            try {
                // In a real implementation, this would call the ModForge API to invalidate the token
                // For now, just simulate a logout
                Thread.sleep(500);
                
                LOG.info("Successfully logged out user: " + username);
            } catch (Exception e) {
                LOG.error("Error during logout", e);
            }
        }).thenRun(() -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                this.username = null;
                this.token = null;
                this.isAuthenticated = false;
                
                // Clear settings
                settings.setAccessToken("");
                settings.setGitHubUsername("");
                
                notificationService.showInfo("Logout Successful", "Successfully logged out");
            });
        });
    }

    /**
     * Tests the current authentication.
     *
     * @param callback Callback for success.
     */
    public void testAuthentication(@NotNull Consumer<Boolean> callback) {
        if (!isAuthenticated) {
            LOG.info("Not authenticated, cannot test authentication");
            callback.accept(false);
            return;
        }
        
        LOG.info("Testing authentication for user: " + username);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the ModForge API to test the token
                // For now, simulate a successful test with a GitHub API call
                URL url = new URL("https://api.github.com/user");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "token " + token);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                
                LOG.info("Authentication test result: " + responseCode);
                
                return responseCode == 200;
            } catch (Exception e) {
                LOG.error("Failed to test authentication", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (success) {
                    notificationService.showInfo("Authentication Successful", "Successfully authenticated with ModForge");
                } else {
                    notificationService.showError("Authentication Failed", "Failed to authenticate with ModForge");
                    
                    // Clear invalid credentials
                    this.isAuthenticated = false;
                }
                callback.accept(success);
            });
        });
    }
}