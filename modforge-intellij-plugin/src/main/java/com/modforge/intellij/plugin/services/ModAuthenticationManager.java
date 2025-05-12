package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Service for managing user authentication.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    // Authentication states
    private final AtomicBoolean isAuthenticated = new AtomicBoolean(false);
    private final AtomicBoolean isAuthenticating = new AtomicBoolean(false);
    
    // Settings access
    private final ModForgeSettings settings;
    private final ModForgeNotificationService notificationService;
    
    // GitHub API endpoints
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String USER_ENDPOINT = GITHUB_API_BASE + "/user";
    private static final int HTTP_OK = 200;
    private static final int HTTP_UNAUTHORIZED = 401;
    
    /**
     * Constructor.
     */
    public ModAuthenticationManager() {
        this.settings = ModForgeSettings.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance();
    }

    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }

    /**
     * Logs in with username and password.
     *
     * @param username The username.
     * @param password The password.
     * @return A CompletableFuture with a boolean indicating if login was successful.
     */
    public CompletableFuture<Boolean> login(@NotNull String username, @NotNull String password) {
        if (isAuthenticating.get()) {
            LOG.warn("Authentication already in progress");
            return CompletableFuture.completedFuture(false);
        }
        
        isAuthenticating.set(true);
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                LOG.info("Authenticating user: " + username);
                
                // Store credentials
                settings.setGitHubUsername(username);
                settings.setAccessToken(password);
                
                // Verify authentication
                boolean authResult = verifyAuthentication();
                isAuthenticated.set(authResult);
                
                LOG.info("Authentication " + (authResult ? "successful" : "failed") + " for user: " + username);
                
                return authResult;
            } catch (Exception e) {
                LOG.error("Authentication error", e);
                return false;
            } finally {
                isAuthenticating.set(false);
            }
        });
    }
    
    /**
     * Logs in with username and password synchronously.
     *
     * @param username The username.
     * @param password The password.
     * @return Whether login was successful.
     */
    public boolean loginSync(@NotNull String username, @NotNull String password) {
        try {
            return login(username, password).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | java.util.concurrent.ExecutionException | TimeoutException e) {
            LOG.error("Login timed out", e);
            return false;
        }
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        if (isAuthenticating.get()) {
            LOG.warn("Authentication in progress, cannot logout");
            return;
        }
        
        ThreadUtils.runAsyncVirtual(() -> {
            LOG.info("Logging out user: " + getUsername());
            
            // Clear credentials
            settings.setAccessToken(null);
            
            isAuthenticated.set(false);
            
            LOG.info("Logout complete");
        });
    }

    /**
     * Checks if the user is authenticated.
     *
     * @return Whether the user is authenticated.
     */
    public boolean isAuthenticated() {
        // If we think we're authenticated, verify it
        if (isAuthenticated.get() && !isAuthenticating.get()) {
            String token = settings.getAccessToken();
            if (token == null || token.isEmpty()) {
                isAuthenticated.set(false);
                return false;
            }
            
            // Periodically re-verify authentication
            // This is done in a background thread to avoid blocking the UI
            ThreadUtils.runAsyncVirtual(() -> {
                try {
                    boolean verified = verifyAuthentication();
                    if (!verified) {
                        LOG.warn("Authentication token is no longer valid");
                        isAuthenticated.set(false);
                    }
                } catch (Exception e) {
                    LOG.error("Error verifying authentication", e);
                }
            });
        }
        
        return isAuthenticated.get();
    }

    /**
     * Gets the current username.
     *
     * @return The username or null if not set.
     */
    @Nullable
    public String getUsername() {
        return settings.getGitHubUsername();
    }

    /**
     * Gets the current access token.
     *
     * @return The access token.
     */
    @Nullable
    public String getAccessToken() {
        return settings.getAccessToken();
    }

    /**
     * Verifies authentication with the GitHub API.
     *
     * @return Whether authentication is valid.
     */
    @RequiresBackgroundThread
    private boolean verifyAuthentication() {
        String token = settings.getAccessToken();
        if (token == null || token.isEmpty()) {
            LOG.warn("No access token available");
            return false;
        }
        
        String username = settings.getGitHubUsername();
        if (username == null || username.isEmpty()) {
            LOG.warn("No username available");
            return false;
        }
        
        try {
            URL url = new URL(USER_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + token);
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HTTP_OK) {
                return true;
            } else if (responseCode == HTTP_UNAUTHORIZED) {
                LOG.warn("Invalid access token");
                return false;
            } else {
                LOG.warn("Unexpected response code: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            LOG.error("Error verifying authentication", e);
            return false;
        }
    }

    /**
     * Generates authentication headers for use with the GitHub API.
     *
     * @return The authentication headers or null if not authenticated.
     */
    @Nullable
    public String getAuthenticationHeader() {
        if (!isAuthenticated()) {
            return null;
        }
        
        String token = settings.getAccessToken();
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        return "token " + token;
    }

    /**
     * Gets basic authentication header for use with the GitHub API.
     *
     * @return The basic authentication header or null if not authenticated.
     */
    @Nullable
    public String getBasicAuthenticationHeader() {
        if (!isAuthenticated()) {
            return null;
        }
        
        String username = settings.getGitHubUsername();
        String token = settings.getAccessToken();
        
        if (username == null || username.isEmpty() || token == null || token.isEmpty()) {
            return null;
        }
        
        String credentials = username + ":" + token;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        return "Basic " + encoded;
    }

    /**
     * Shows the login dialog and performs login.
     *
     * @param project  The project.
     * @param onSuccess Called when login is successful.
     */
    @RequiresEdt
    public void showLoginDialog(@NotNull Project project, @Nullable Consumer<Boolean> onSuccess) {
        if (isAuthenticated.get()) {
            if (onSuccess != null) {
                onSuccess.accept(true);
            }
            return;
        }
        
        if (isAuthenticating.get()) {
            notificationService.showWarningNotification(project, "Authentication", "Authentication already in progress");
            return;
        }
        
        // Show login dialog and handle login
        // This would be implemented with a dialog UI in the real plugin
        // For this implementation, we'll just assume the user enters credentials
        
        String username = settings.getGitHubUsername();
        String token = settings.getAccessToken();
        
        if (username != null && !username.isEmpty() && token != null && !token.isEmpty()) {
            // If we have credentials, try to authenticate with them
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Logging in...", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    
                    try {
                        boolean result = login(username, token).get(30, TimeUnit.SECONDS);
                        
                        if (result) {
                            notificationService.showInfoNotification(project, "Authentication", "Successfully logged in as " + username);
                        } else {
                            notificationService.showErrorNotification(project, "Authentication", "Failed to log in. Please check your credentials.");
                        }
                        
                        if (onSuccess != null) {
                            onSuccess.accept(result);
                        }
                    } catch (TimeoutException e) {
                        notificationService.showErrorNotification(project, "Authentication", "Login timed out. Please try again.");
                        LOG.error("Login timed out", e);
                    } catch (Exception e) {
                        notificationService.showErrorNotification(project, "Authentication", "An error occurred during login: " + e.getMessage());
                        LOG.error("Login error", e);
                    }
                }
            });
        } else {
            // If we don't have credentials, show a message asking user to enter them in settings
            notificationService.showWarningNotification(project, "Authentication", "Please enter your GitHub credentials in the settings.");
            // Note: settings.openSettings(project) would be implemented in ModForgeSettings
        }
    }

    /**
     * Tests the authentication with the provided credentials.
     *
     * @param username The username.
     * @param token    The access token.
     * @return Whether authentication is valid.
     */
    public boolean testAuthentication(@NotNull String username, @NotNull String token) {
        try {
            URL url = new URL(USER_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + token);
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
            
            int responseCode = connection.getResponseCode();
            
            return responseCode == HTTP_OK;
        } catch (IOException e) {
            LOG.error("Error testing authentication", e);
            return false;
        }
    }
}