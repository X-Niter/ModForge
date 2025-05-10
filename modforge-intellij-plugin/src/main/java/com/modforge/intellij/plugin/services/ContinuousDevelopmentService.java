package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for continuous development.
 * Automatically compiles and fixes code errors.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    
    private static final int INITIAL_DELAY_SECONDS = 10;
    private static final int POLL_INTERVAL_SECONDS = 60;
    
    private final Project project;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private ScheduledFuture<?> scheduledFuture;
    
    /**
     * Construct the continuous development service.
     *
     * @param project The project
     */
    public ContinuousDevelopmentService(@NotNull Project project) {
        this.project = project;
        this.scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("ModForge Continuous Development", 1);
        
        // Check settings and start service if enabled
        if (ModForgeSettings.getInstance().isContinuousDevelopment() &&
                ModAuthenticationManager.getInstance().isAuthenticated()) {
            start();
        }
    }
    
    /**
     * Start the continuous development service.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting continuous development service");
            
            // Schedule continuous development
            scheduledFuture = scheduler.scheduleWithFixedDelay(
                    this::runContinuousDevelopment,
                    INITIAL_DELAY_SECONDS,
                    POLL_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
        }
    }
    
    /**
     * Stop the continuous development service.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping continuous development service");
            
            // Cancel scheduled task
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
        }
    }
    
    /**
     * Check if the service is running.
     *
     * @return Whether the service is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Run continuous development.
     * This method is called periodically by the scheduler.
     * It checks for project changes and compiles the project.
     */
    private void runContinuousDevelopment() {
        try {
            // Check if the service should still be running
            if (!running.get() ||
                    !ModForgeSettings.getInstance().isContinuousDevelopment() ||
                    !ModAuthenticationManager.getInstance().isAuthenticated()) {
                stop();
                return;
            }
            
            // Check for project changes
            boolean hasChanges = checkForChanges();
            
            if (hasChanges) {
                // Compile project
                compileProject();
            }
        } catch (Exception e) {
            LOG.error("Error in continuous development", e);
        }
    }
    
    /**
     * Check for project changes.
     *
     * @return Whether the project has changes
     */
    private boolean checkForChanges() {
        try {
            // Call API to check for changes
            String response = TokenAuthConnectionUtil.executeGet("/api/project/changes");
            
            if (response != null && !response.isEmpty()) {
                // Parse response to check for changes
                return response.contains("\"hasChanges\":true");
            }
            
            return false;
        } catch (Exception e) {
            LOG.error("Error checking for project changes", e);
            return false;
        }
    }
    
    /**
     * Compile the project.
     */
    private void compileProject() {
        try {
            // Call API to compile project
            String response = TokenAuthConnectionUtil.executePost("/api/project/compile", "{}");
            
            if (response != null && !response.isEmpty()) {
                // Parse response for compilation result
                boolean success = response.contains("\"success\":true");
                
                if (success) {
                    LOG.info("Compilation successful");
                } else {
                    LOG.warn("Compilation failed");
                    
                    // Check for errors
                    if (response.contains("\"errors\":")) {
                        // Fix errors
                        fixErrors(response);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error compiling project", e);
        }
    }
    
    /**
     * Fix compilation errors.
     *
     * @param compilationResponse The compilation response containing errors
     */
    private void fixErrors(String compilationResponse) {
        try {
            // Call API to fix errors
            String response = TokenAuthConnectionUtil.executePost("/api/project/fix-errors", compilationResponse);
            
            if (response != null && !response.isEmpty()) {
                // Parse response for fix result
                boolean success = response.contains("\"success\":true");
                
                if (success) {
                    LOG.info("Error fixing successful");
                } else {
                    LOG.warn("Error fixing failed");
                }
            }
        } catch (Exception e) {
            LOG.error("Error fixing compilation errors", e);
        }
    }
}