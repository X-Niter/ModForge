package com.modforge.intellij.plugin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import com.modforge.intellij.plugin.notifications.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Continuous service that adapts its behavior based on memory pressure
 * This ensures that long-running operations don't cause memory issues
 */
public class MemoryAwareContinuousService implements Disposable, MemoryManager.MemoryPressureListener {
    private static final Logger LOG = Logger.getInstance(MemoryAwareContinuousService.class);
    
    private final Project project;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger consecutiveHighPressureCount = new AtomicInteger(0);
    private final ScheduledExecutorService executor = AppExecutorUtil.createBoundedScheduledExecutorService(
            "ModForgeMemoryAwareContinuousService", 1);
    private ScheduledFuture<?> currentTask;
    private MemoryUtils.MemoryPressureLevel currentPressureLevel = MemoryUtils.MemoryPressureLevel.NORMAL;
    private final ModForgeNotificationService notificationService;
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryAwareContinuousService(Project project) {
        this.project = project;
        this.notificationService = project.getService(ModForgeNotificationService.class);
        
        // Register as a memory pressure listener
        MemoryManager memoryManager = MemoryManager.getInstance();
        if (memoryManager != null) {
            memoryManager.addMemoryPressureListener(this);
        }
    }
    
    /**
     * Start the continuous service
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting memory-aware continuous service");
            schedule();
        }
    }
    
    /**
     * Stop the continuous service
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping memory-aware continuous service");
            if (currentTask != null) {
                currentTask.cancel(false);
                currentTask = null;
            }
        }
    }
    
    /**
     * Reset the continuous service to its initial state
     * This stops the service, resets all counters and state variables, and optionally restarts
     * 
     * @param autoRestart Whether to automatically restart the service after reset
     */
    public void reset(boolean autoRestart) {
        LOG.info("Resetting memory-aware continuous service");
        
        // Stop the service if it's running
        stop();
        
        // Reset internal state
        consecutiveHighPressureCount.set(0);
        currentPressureLevel = MemoryUtils.MemoryPressureLevel.NORMAL;
        
        // Cancel any pending tasks
        if (currentTask != null) {
            currentTask.cancel(false);
            currentTask = null;
        }
        
        // Refresh our memory manager listener registration
        MemoryManager memoryManager = MemoryManager.getInstance();
        if (memoryManager != null) {
            memoryManager.removeMemoryPressureListener(this);
            memoryManager.addMemoryPressureListener(this);
        }
        
        // Get continuous service and reset its state
        try {
            ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
            if (continuousService != null) {
                continuousService.setReducedFeaturesMode(false);
            }
        } catch (Exception e) {
            LOG.warn("Error resetting continuous development service state", e);
        }
        
        LOG.info("Memory-aware continuous service reset completed");
        
        // Restart if requested and not disposed
        if (autoRestart && !project.isDisposed()) {
            LOG.info("Auto-restarting memory-aware continuous service after reset");
            start();
        }
    }
    
    /**
     * Check if the service is running
     * 
     * @return True if the service is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Schedule the next execution of the service
     */
    private void schedule() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        if (currentTask != null && !currentTask.isDone() && !currentTask.isCancelled()) {
            currentTask.cancel(false);
        }
        
        // Determine the interval based on memory pressure
        int intervalMinutes = getIntervalForCurrentPressure();
        
        LOG.info("Scheduling next memory-aware continuous service execution in " + 
                intervalMinutes + " minutes (pressure level: " + currentPressureLevel + ")");
        
        currentTask = executor.schedule(this::execute, intervalMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * Execute the continuous service task
     */
    private void execute() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        LOG.info("Executing memory-aware continuous service");
        
        try {
            // Check if memory optimization is needed before execution
            MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
            if (settings.isOptimizeBeforeLongRunningTasks() && 
                    MemoryUtils.isMemoryOptimizationNeeded()) {
                
                LOG.info("Performing pre-execution memory optimization");
                MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                if (optimizer != null) {
                    optimizer.optimize(MemoryUtils.getOptimizationLevelForCurrentPressure());
                }
            }
            
            // Execute the actual continuous service logic
            performContinuousServiceTask();
            
        } catch (Exception e) {
            LOG.error("Error executing memory-aware continuous service", e);
            
            if (notificationService != null) {
                notificationService.showErrorNotification(
                        "Memory-Aware Continuous Service Error",
                        "An error occurred while executing the memory-aware continuous service: " + e.getMessage()
                );
            }
        } finally {
            // Schedule the next execution
            schedule();
        }
    }
    
    /**
     * Perform the actual continuous service task
     * This is where the main work of the service is done
     */
    private void performContinuousServiceTask() {
        LOG.info("Performing continuous service task");
        
        // Get the main continuous development service
        ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
        if (continuousService == null) {
            LOG.warn("Continuous development service not available");
            return;
        }
        
        if (!continuousService.isEnabled()) {
            LOG.info("Continuous development service is not enabled, skipping development cycle");
            return;
        }
        
        // Execute a development cycle with memory awareness
        boolean useReducedFeatures = currentPressureLevel != MemoryUtils.MemoryPressureLevel.NORMAL;
        
        try {
            LOG.info("Executing memory-aware development cycle (reduced features: " + useReducedFeatures + ")");
            
            // Set memory-aware options in the continuous service
            if (useReducedFeatures) {
                LOG.info("Using reduced features due to memory pressure: " + currentPressureLevel);
                
                // Check severe memory pressure, apply circuit breaker if needed
                if (currentPressureLevel == MemoryUtils.MemoryPressureLevel.CRITICAL || 
                        currentPressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY) {
                    
                    // If we're under severe memory pressure, only perform minimal tasks
                    LOG.warn("Severe memory pressure detected, performing minimal continuous development");
                    
                    // For critical memory situations, just do essential maintenance and defer intensive tasks
                    continuousService.performLightweightCycle();
                    
                    LOG.info("Lightweight memory-aware development cycle complete");
                    return;
                }
                
                // Otherwise use reduced features mode for moderate memory pressure
                continuousService.setReducedFeaturesMode(true);
            } else {
                // Normal memory conditions, use full features
                continuousService.setReducedFeaturesMode(false);
            }
            
            // Execute the development cycle with current memory constraints
            continuousService.executeDevelopmentCycle();
            
            LOG.info("Memory-aware development cycle complete");
            
        } catch (Exception e) {
            LOG.error("Error in memory-aware development cycle", e);
            
            if (notificationService != null) {
                notificationService.showErrorNotification(
                        "Development Cycle Error",
                        "An error occurred during the memory-aware development cycle: " + e.getMessage()
                );
            }
        } finally {
            // Reset any temporary settings
            if (useReducedFeatures) {
                try {
                    continuousService.setReducedFeaturesMode(false);
                } catch (Exception e) {
                    LOG.warn("Error resetting reduced features mode", e);
                }
            }
        }
    }
    
    /**
     * Get the appropriate interval for the current memory pressure level
     * 
     * @return The interval in minutes
     */
    private int getIntervalForCurrentPressure() {
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        
        switch (currentPressureLevel) {
            case EMERGENCY:
                return settings.getContinuousServiceMinimumIntervalMinutes();
            case CRITICAL:
                return settings.getContinuousServiceMinimumIntervalMinutes();
            case WARNING:
                return settings.getContinuousServiceReducedIntervalMinutes();
            default:
                return settings.getContinuousServiceDefaultIntervalMinutes();
        }
    }
    
    @Override
    public void onMemoryPressureChanged(MemoryUtils.MemoryPressureLevel pressureLevel) {
        MemoryUtils.MemoryPressureLevel previousLevel = currentPressureLevel;
        currentPressureLevel = pressureLevel;
        
        // If pressure increased, reschedule with a longer interval
        if (pressureLevel.ordinal() > previousLevel.ordinal()) {
            LOG.info("Memory pressure increased from " + previousLevel + 
                    " to " + pressureLevel + ", adjusting continuous service schedule");
            
            // Keep track of consecutive high pressure events
            if (pressureLevel == MemoryUtils.MemoryPressureLevel.CRITICAL || 
                    pressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY) {
                
                int count = consecutiveHighPressureCount.incrementAndGet();
                
                // If too many consecutive high pressure events, consider pausing
                if (count >= 3) {
                    LOG.warn("Detected " + count + " consecutive high memory pressure events, " +
                            "temporarily pausing continuous service");
                    
                    // Notify the user
                    if (notificationService != null) {
                        notificationService.showWarningNotification(
                                "Memory Pressure Warning",
                                "High memory pressure detected. Memory-aware continuous service " +
                                "is temporarily using extended intervals to prevent memory issues."
                        );
                    }
                }
            }
            
            // Reschedule if already running
            if (isRunning()) {
                schedule();
            }
        } else if (pressureLevel.ordinal() < previousLevel.ordinal()) {
            // Reset the consecutive count if pressure decreased
            consecutiveHighPressureCount.set(0);
            
            LOG.info("Memory pressure decreased from " + previousLevel + 
                    " to " + pressureLevel + ", continuous service will use normal intervals");
            
            // No need to reschedule immediately when pressure decreases,
            // the next execution will use the new interval
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