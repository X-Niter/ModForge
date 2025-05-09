package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.util.messages.MessageBusConnection;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for continuous development.
 */
@Service
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    
    private final Project project;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final MessageBusConnection messageBusConnection;
    
    private Timer timer;
    
    /**
     * Gets the instance of this service for the specified project.
     * @param project The project
     * @return The service instance
     */
    public static ContinuousDevelopmentService getInstance(@NotNull Project project) {
        return project.getService(ContinuousDevelopmentService.class);
    }
    
    /**
     * Creates a new instance of this service.
     * @param project The project
     */
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
        this.messageBusConnection = project.getMessageBus().connect();
        
        // Register project close listener
        messageBusConnection.subscribe(ProjectCloseListener.TOPIC, new ProjectCloseListener() {
            @Override
            public void projectClosing(@NotNull Project project) {
                stop();
            }
        });
        
        // Auto-start if enabled in settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.isContinuousDevelopmentEnabled()) {
            start();
        }
    }
    
    /**
     * Starts the continuous development service.
     */
    public void start() {
        if (running.getAndSet(true)) {
            // Already running
            return;
        }
        
        LOG.info("Starting continuous development service");
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        int frequencyMinutes = settings.getUpdateFrequencyMinutes();
        
        // Convert to milliseconds
        long frequencyMs = frequencyMinutes * 60 * 1000L;
        
        // Create and schedule timer
        timer = new Timer("ModForge Continuous Development", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkAndFixErrors();
                } catch (Exception e) {
                    LOG.error("Error in continuous development service", e);
                }
            }
        }, frequencyMs, frequencyMs);
    }
    
    /**
     * Stops the continuous development service.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            // Already stopped
            return;
        }
        
        LOG.info("Stopping continuous development service");
        
        // Cancel timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    /**
     * Checks if the service is running.
     * @return True if the service is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Checks for and fixes errors.
     */
    private void checkAndFixErrors() {
        if (!running.get()) {
            return;
        }
        
        LOG.info("Checking for errors");
        
        // Get compilation listener
        ModForgeCompilationListener compilationListener = ModForgeCompilationListener.getInstance(project);
        
        if (compilationListener == null) {
            LOG.warn("Compilation listener not available");
            return;
        }
        
        // Get all issues
        List<ModForgeCompilationListener.CompilationIssue> issues = compilationListener.getAllActiveIssues();
        
        if (issues.isEmpty()) {
            LOG.info("No compilation errors found");
            return;
        }
        
        LOG.info("Found " + issues.size() + " compilation errors");
        
        // Group issues by file
        // TODO: Implement grouping issues by file
        
        // Fix issues
        // TODO: Implement fixing issues
    }
}