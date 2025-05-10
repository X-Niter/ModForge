package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for continuous development of Minecraft mods.
 * This service periodically compiles and tests mods, and fixes errors automatically.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    private static final Gson GSON = new Gson();
    
    private final Project project;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledTask;
    
    /**
     * Create a new continuous development service.
     *
     * @param project The project
     */
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
    }
    
    /**
     * Start continuous development.
     * This will periodically compile and test the mod, and fix errors automatically.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting continuous development");
            
            // Schedule the task
            scheduledTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                    this::runContinuousDevelopment,
                    1,
                    5,
                    TimeUnit.MINUTES
            );
        }
    }
    
    /**
     * Stop continuous development.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping continuous development");
            
            // Cancel the scheduled task
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                scheduledTask = null;
            }
        }
    }
    
    /**
     * Check if continuous development is running.
     *
     * @return True if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Run the continuous development task.
     * This will compile and test the mod, and fix errors automatically.
     */
    private void runContinuousDevelopment() {
        try {
            LOG.info("Running continuous development task");
            
            // Check if continuous development is enabled in settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            if (!settings.isEnableContinuousDevelopment()) {
                LOG.info("Continuous development is disabled in settings, stopping");
                stop();
                return;
            }
            
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.error("Not authenticated, stopping continuous development");
                stop();
                return;
            }
            
            // Get the server URL
            String serverUrl = settings.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set, stopping continuous development");
                stop();
                return;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null, stopping continuous development");
                stop();
                return;
            }
            
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("projectName", project.getName());
            
            // Send the request to start continuous development on the server
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/continuous/start",
                    "POST",
                    token,
                    requestBody.toString()
            ).thenAccept(response -> {
                if (response != null) {
                    LOG.info("Continuous development task started on server");
                } else {
                    LOG.error("Failed to start continuous development task on server");
                }
            }).exceptionally(ex -> {
                LOG.error("Exception starting continuous development task on server", ex);
                return null;
            });
        } catch (Exception e) {
            LOG.error("Error in continuous development task", e);
        }
    }
    
    /**
     * Stop continuous development on the server.
     */
    private void stopServerContinuousDevelopment() {
        try {
            LOG.info("Stopping continuous development on server");
            
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.error("Not authenticated");
                return;
            }
            
            // Get the server URL
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set");
                return;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null");
                return;
            }
            
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("projectName", project.getName());
            
            // Send the request to stop continuous development on the server
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/continuous/stop",
                    "POST",
                    token,
                    requestBody.toString()
            ).thenAccept(response -> {
                if (response != null) {
                    LOG.info("Continuous development stopped on server");
                } else {
                    LOG.error("Failed to stop continuous development on server");
                }
            }).exceptionally(ex -> {
                LOG.error("Exception stopping continuous development on server", ex);
                return null;
            });
        } catch (Exception e) {
            LOG.error("Error stopping continuous development on server", e);
        }
    }
    
    /**
     * Get the status of continuous development from the server.
     */
    private void getServerContinuousDevelopmentStatus() {
        try {
            LOG.info("Getting continuous development status from server");
            
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.error("Not authenticated");
                return;
            }
            
            // Get the server URL
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set");
                return;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null");
                return;
            }
            
            // Create request URL
            String url = "api/continuous/status?projectName=" + project.getName();
            
            // Send the request to get continuous development status from the server
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    url,
                    "GET",
                    token,
                    null
            ).thenAccept(response -> {
                if (response != null) {
                    LOG.info("Got continuous development status from server: " + response);
                    
                    // Parse the response
                    try {
                        JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
                        boolean running = jsonResponse.get("running").getAsBoolean();
                        
                        // Update our running state
                        this.running.set(running);
                    } catch (Exception e) {
                        LOG.error("Error parsing continuous development status response", e);
                    }
                } else {
                    LOG.error("Failed to get continuous development status from server");
                }
            }).exceptionally(ex -> {
                LOG.error("Exception getting continuous development status from server", ex);
                return null;
            });
        } catch (Exception e) {
            LOG.error("Error getting continuous development status from server", e);
        }
    }
}