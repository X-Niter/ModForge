package com.modforge.intellij.plugin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager;
import com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryNotifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that monitors memory health and triggers recovery actions when needed
 * This service brings together memory monitoring, recovery, and notification components
 */
public class MemoryRecoveryService implements Disposable, MemoryManager.MemoryPressureListener {
    private static final Logger LOG = Logger.getInstance(MemoryRecoveryService.class);
    
    private final Project project;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = AppExecutorUtil.createBoundedScheduledExecutorService(
            "ModForgeMemoryRecoveryService", 1);
    private ScheduledFuture<?> healthCheckTask;
    private MemoryRecoveryNotifier notifier;
    private MemoryUtils.MemoryPressureLevel lastNotifiedLevel = MemoryUtils.MemoryPressureLevel.NORMAL;
    private long lastNotificationTime = 0;
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryRecoveryService(Project project) {
        this.project = project;
        
        // Create the notifier
        this.notifier = new MemoryRecoveryNotifier(project);
        
        // Register as memory pressure listener
        MemoryManager memoryManager = MemoryManager.getInstance();
        if (memoryManager != null) {
            memoryManager.addMemoryPressureListener(this);
        }
    }
    
    /**
     * Start the service
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting memory recovery service");
            startHealthCheck();
            
            // Initialize the memory recovery manager
            MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
            if (recoveryManager != null) {
                recoveryManager.initialize();
            }
        }
    }
    
    /**
     * Stop the service
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping memory recovery service");
            
            if (healthCheckTask != null) {
                healthCheckTask.cancel(false);
                healthCheckTask = null;
            }
        }
    }
    
    /**
     * Start the health check task
     */
    private void startHealthCheck() {
        if (healthCheckTask != null && !healthCheckTask.isDone()) {
            healthCheckTask.cancel(false);
        }
        
        healthCheckTask = executor.scheduleWithFixedDelay(
                this::checkMemoryHealth,
                0,
                60,
                TimeUnit.SECONDS
        );
        
        LOG.info("Memory health check started");
    }
    
    /**
     * Check memory health
     */
    private void checkMemoryHealth() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        try {
            LOG.debug("Checking memory health");
            
            MemoryUtils.MemoryPressureLevel currentLevel = MemoryUtils.getMemoryPressureLevel();
            
            // If pressure level is warning or higher, log memory stats
            if (currentLevel.ordinal() >= MemoryUtils.MemoryPressureLevel.WARNING.ordinal()) {
                MemoryUtils.logMemoryStats();
            }
            
            // Check if we should notify the user
            checkForNotification(currentLevel);
            
        } catch (Exception e) {
            LOG.error("Error checking memory health", e);
        }
    }
    
    /**
     * Check if a notification should be shown
     * 
     * @param currentLevel The current memory pressure level
     */
    private void checkForNotification(MemoryUtils.MemoryPressureLevel currentLevel) {
        // Don't notify for normal pressure
        if (currentLevel == MemoryUtils.MemoryPressureLevel.NORMAL) {
            return;
        }
        
        // Determine if we should notify based on level changes and time
        boolean shouldNotify = false;
        long now = System.currentTimeMillis();
        
        // Always notify for emergency level, but rate limit to once per 5 minutes
        if (currentLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY) {
            shouldNotify = now - lastNotificationTime > 5 * 60 * 1000;
        }
        // Notify for critical level if it's different from last notified level or it's been more than 10 minutes
        else if (currentLevel == MemoryUtils.MemoryPressureLevel.CRITICAL) {
            shouldNotify = (currentLevel != lastNotifiedLevel) || (now - lastNotificationTime > 10 * 60 * 1000);
        }
        // Notify for warning level only if it's different from last notified level and it's been more than 15 minutes
        else if (currentLevel == MemoryUtils.MemoryPressureLevel.WARNING) {
            shouldNotify = (currentLevel != lastNotifiedLevel) && (now - lastNotificationTime > 15 * 60 * 1000);
        }
        
        if (shouldNotify) {
            LOG.info("Showing memory warning notification for level " + currentLevel);
            notifier.showMemoryWarning(currentLevel);
            lastNotifiedLevel = currentLevel;
            lastNotificationTime = now;
        }
    }
    
    @Override
    public void onMemoryPressureChanged(MemoryUtils.MemoryPressureLevel pressureLevel) {
        LOG.info("Memory pressure changed to " + pressureLevel);
        
        // If pressure changed to critical or emergency, check immediately
        if (pressureLevel == MemoryUtils.MemoryPressureLevel.CRITICAL || 
                pressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY) {
            
            checkMemoryHealth();
        }
    }
    
    @Override
    public void dispose() {
        stop();
        
        MemoryManager memoryManager = MemoryManager.getInstance();
        if (memoryManager != null) {
            memoryManager.removeMemoryPressureListener(this);
        }
        
        executor.shutdownNow();
    }
}