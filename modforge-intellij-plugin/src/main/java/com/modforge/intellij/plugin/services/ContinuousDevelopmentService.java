package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.json.simple.JSONObject;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service that manages continuous mod development.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    
    private final Project project;
    private ScheduledFuture<?> scheduledTask;
    private final ScheduledExecutorService executor;
    private int taskCounter = 0;
    
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
        this.executor = AppExecutorUtil.getAppScheduledExecutorService();
    }
    
    /**
     * Start continuous development.
     */
    public void start() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            LOG.info("Continuous development service is already running");
            return;
        }
        
        LOG.info("Starting continuous development service");
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        int interval = settings.getPollingInterval();
        
        if (interval < 1000) {
            LOG.warn("Polling interval is too small, setting to 1 minute");
            interval = 60 * 1000; // 1 minute
        }
        
        scheduledTask = executor.scheduleWithFixedDelay(
                this::runTask,
                10000, // Initial delay of 10 seconds
                interval,
                TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stop continuous development.
     */
    public void stop() {
        if (scheduledTask != null) {
            LOG.info("Stopping continuous development service");
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }
    
    /**
     * Restart continuous development with current settings.
     */
    public void restart() {
        stop();
        start();
    }
    
    /**
     * Check if continuous development is running.
     * @return Whether continuous development is running
     */
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isDone();
    }
    
    /**
     * Run the continuous development task.
     */
    private void runTask() {
        LOG.info("Running continuous development task #" + (++taskCounter));
        
        // Check authentication
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            LOG.warn("Not authenticated, skipping continuous development task");
            return;
        }
        
        // Check connection
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = settings.getAccessToken();
        
        try {
            // Get mods from server
            JSONObject response = TokenAuthConnectionUtil.get(serverUrl, "/api/mods", token);
            
            if (response == null) {
                LOG.error("Failed to get mods from server");
                return;
            }
            
            // Process mods
            processModsResponse(response);
        } catch (Exception e) {
            LOG.error("Error in continuous development task", e);
        }
    }
    
    /**
     * Process mods response from server.
     * @param response Response from server
     */
    private void processModsResponse(JSONObject response) {
        if (response == null) {
            LOG.error("Cannot process null response");
            return;
        }
        
        try {
            // Check if we have pattern recognition enabled
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean patternRecognition = settings.isPatternRecognition();
            
            // Record pattern activity
            if (patternRecognition) {
                // Get pattern recognition service
                AutonomousCodeGenerationService automatedService = project.getService(AutonomousCodeGenerationService.class);
                if (automatedService != null) {
                    automatedService.processMods(response);
                } else {
                    LOG.error("AutonomousCodeGenerationService is null");
                }
            } else {
                LOG.info("Pattern recognition is disabled, skipping patterns");
            }
        } catch (Exception e) {
            LOG.error("Error processing mods response", e);
        }
    }
    
    /**
     * Get the number of tasks that have been run.
     * @return Task counter
     */
    public int getTaskCount() {
        return taskCounter;
    }
}