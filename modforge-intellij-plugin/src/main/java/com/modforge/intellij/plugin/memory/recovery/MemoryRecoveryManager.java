package com.modforge.intellij.plugin.memory.recovery;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Memory recovery manager that provides emergency recovery actions when memory issues are detected
 * Implements an escalating response to memory pressure with increasingly aggressive actions
 */
public class MemoryRecoveryManager implements MemoryManager.MemoryPressureListener {
    private static final Logger LOG = Logger.getInstance(MemoryRecoveryManager.class);
    
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean recovering = new AtomicBoolean(false);
    private final Set<RecoveryListener> listeners = new HashSet<>();
    private final ScheduledExecutorService executor = AppExecutorUtil.createBoundedScheduledExecutorService(
            "ModForgeMemoryRecoveryManager", 1);
    private ScheduledFuture<?> monitoringTask;
    private int consecutiveCriticalCount = 0;
    private int consecutiveEmergencyCount = 0;
    private long lastRecoveryTime = 0;
    
    /**
     * Recovery levels representing increasingly aggressive memory recovery actions
     */
    public enum RecoveryLevel {
        LEVEL1,  // Basic recovery - GC request and minimal optimization
        LEVEL2,  // Moderate recovery - GC request, normal optimization, pause background tasks
        LEVEL3,  // Aggressive recovery - GC request, aggressive optimization, stop non-essential services
        LEVEL4   // Critical recovery - GC request, aggressive optimization, restart services/components
    }
    
    /**
     * Get the singleton instance of MemoryRecoveryManager
     */
    public static MemoryRecoveryManager getInstance() {
        return ApplicationManager.getApplication().getService(MemoryRecoveryManager.class);
    }
    
    /**
     * Initialize the memory recovery manager
     */
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            LOG.info("Initializing MemoryRecoveryManager");
            
            // Register as memory pressure listener
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                memoryManager.addMemoryPressureListener(this);
            }
            
            // Start monitoring
            startMonitoring();
        }
    }
    
    /**
     * Start the monitoring task
     */
    private void startMonitoring() {
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
        }
        
        monitoringTask = executor.scheduleWithFixedDelay(
                this::checkMemoryHealth,
                1,
                30,
                TimeUnit.SECONDS
        );
        
        LOG.info("Memory recovery monitoring started");
    }
    
    /**
     * Completely reset the memory recovery manager
     * This cancels all monitoring tasks, resets all counters, and reinitializes
     */
    public void reset() {
        LOG.info("Resetting memory recovery manager");
        
        // Cancel monitoring task
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
        
        // Reset state
        recovering.set(false);
        consecutiveCriticalCount = 0;
        consecutiveEmergencyCount = 0;
        lastRecoveryTime = 0;
        
        // Reset initialized flag to force reinitialization
        initialized.set(false);
        
        // Reinitialize
        initialize();
        
        LOG.info("Memory recovery manager reset completed");
    }
    
    /**
     * Check memory health and trigger recovery if needed
     */
    private void checkMemoryHealth() {
        try {
            MemoryUtils.MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();
            
            if (pressureLevel == MemoryUtils.MemoryPressureLevel.CRITICAL) {
                consecutiveCriticalCount++;
                LOG.warn("Detected critical memory pressure (" + consecutiveCriticalCount + " consecutive times)");
                
                if (consecutiveCriticalCount >= 3) {
                    // After 3 consecutive critical events, initiate level 2 recovery
                    LOG.warn("Initiating level 2 memory recovery due to sustained critical memory pressure");
                    initiateRecovery(RecoveryLevel.LEVEL2);
                    consecutiveCriticalCount = 0;
                }
            } else if (pressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY) {
                consecutiveEmergencyCount++;
                LOG.error("Detected emergency memory pressure (" + consecutiveEmergencyCount + " consecutive times)");
                
                if (consecutiveEmergencyCount >= 2) {
                    // After 2 consecutive emergency events, initiate level 3 recovery
                    LOG.error("Initiating level 3 memory recovery due to sustained emergency memory pressure");
                    initiateRecovery(RecoveryLevel.LEVEL3);
                    consecutiveEmergencyCount = 0;
                } else {
                    // First emergency event, initiate level 2 recovery
                    LOG.warn("Initiating level 2 memory recovery due to emergency memory pressure");
                    initiateRecovery(RecoveryLevel.LEVEL2);
                }
            } else {
                // Reset counters if pressure is normal or just warning
                if (consecutiveCriticalCount > 0 || consecutiveEmergencyCount > 0) {
                    LOG.info("Memory pressure reduced to " + pressureLevel + ", resetting recovery counters");
                    consecutiveCriticalCount = 0;
                    consecutiveEmergencyCount = 0;
                }
            }
        } catch (Exception e) {
            LOG.error("Error checking memory health", e);
        }
    }
    
    @Override
    public void onMemoryPressureChanged(MemoryUtils.MemoryPressureLevel pressureLevel) {
        LOG.info("Memory pressure changed to " + pressureLevel);
        
        if (pressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY) {
            // Immediate level 1 recovery on first emergency event
            initiateRecovery(RecoveryLevel.LEVEL1);
        }
    }
    
    /**
     * Initiate memory recovery at the specified level
     * 
     * @param level The recovery level
     */
    public void initiateRecovery(RecoveryLevel level) {
        // Check if we're already recovering or if recovery was performed recently
        if (recovering.get()) {
            LOG.info("Memory recovery already in progress, ignoring new request");
            return;
        }
        
        // Rate limit recovery actions (no more than once per minute for lower levels)
        long now = System.currentTimeMillis();
        if (level.ordinal() < RecoveryLevel.LEVEL3.ordinal() && 
                now - lastRecoveryTime < 60 * 1000) {
            
            LOG.info("Memory recovery was performed recently, skipping");
            return;
        }
        
        // For LEVEL3 and LEVEL4, use a longer rate limit (5 minutes)
        if (level.ordinal() >= RecoveryLevel.LEVEL3.ordinal() && 
                now - lastRecoveryTime < 5 * 60 * 1000) {
            
            LOG.info("Aggressive memory recovery was performed recently, skipping");
            return;
        }
        
        if (recovering.compareAndSet(false, true)) {
            LOG.info("Initiating memory recovery at level " + level);
            lastRecoveryTime = now;
            
            try {
                notifyRecoveryStarted(level);
                
                // Execute recovery in a background thread
                executor.execute(() -> {
                    try {
                        performRecoveryActions(level);
                        notifyRecoveryCompleted(level);
                    } catch (Exception e) {
                        LOG.error("Error during memory recovery", e);
                        notifyRecoveryFailed(level, e);
                    } finally {
                        recovering.set(false);
                    }
                });
            } catch (Exception e) {
                LOG.error("Error initiating memory recovery", e);
                recovering.set(false);
            }
        }
    }
    
    /**
     * Perform memory recovery actions based on the specified level
     * 
     * @param level The recovery level
     */
    private void performRecoveryActions(RecoveryLevel level) {
        LOG.info("Performing memory recovery actions for level " + level);
        
        // Log memory stats before recovery
        MemoryUtils.logMemoryStats();
        
        // Level 1 actions
        performLevel1RecoveryActions();
        
        // Higher level actions if needed
        if (level.ordinal() >= RecoveryLevel.LEVEL2.ordinal()) {
            performLevel2RecoveryActions();
        }
        
        if (level.ordinal() >= RecoveryLevel.LEVEL3.ordinal()) {
            performLevel3RecoveryActions();
        }
        
        if (level.ordinal() >= RecoveryLevel.LEVEL4.ordinal()) {
            performLevel4RecoveryActions();
        }
        
        // Log memory stats after recovery
        MemoryUtils.logMemoryStats();
    }
    
    /**
     * Perform level 1 recovery actions (minimal)
     */
    private void performLevel1RecoveryActions() {
        LOG.info("Performing level 1 recovery actions");
        
        // Request garbage collection
        MemoryUtils.requestGarbageCollection();
        
        // Perform minimal optimization on all open projects
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            if (project.isDisposed()) continue;
            
            MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
            if (optimizer != null) {
                optimizer.optimize(MemoryOptimizer.OptimizationLevel.MINIMAL);
            }
        }
    }
    
    /**
     * Perform level 2 recovery actions (moderate)
     */
    private void performLevel2RecoveryActions() {
        LOG.info("Performing level 2 recovery actions");
        
        // Perform normal optimization on all open projects
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            if (project.isDisposed()) continue;
            
            MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
            if (optimizer != null) {
                optimizer.optimize(MemoryOptimizer.OptimizationLevel.NORMAL);
            }
        }
        
        // Request garbage collection again
        MemoryUtils.requestGarbageCollection();
    }
    
    /**
     * Perform level 3 recovery actions (aggressive)
     */
    private void performLevel3RecoveryActions() {
        LOG.info("Performing level 3 recovery actions");
        
        // Perform aggressive optimization on all open projects
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            if (project.isDisposed()) continue;
            
            MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
            if (optimizer != null) {
                optimizer.optimize(MemoryOptimizer.OptimizationLevel.AGGRESSIVE);
            }
        }
        
        // Pause non-essential background tasks
        pauseNonEssentialServices();
        
        // Request garbage collection again
        MemoryUtils.requestGarbageCollection();
    }
    
    /**
     * Perform level 4 recovery actions (critical)
     */
    private void performLevel4RecoveryActions() {
        LOG.info("Performing level 4 recovery actions");
        
        // Restart critical services
        restartCriticalServices();
        
        // Request garbage collection again
        MemoryUtils.requestGarbageCollection();
    }
    
    /**
     * Pause non-essential background services to free up memory
     */
    private void pauseNonEssentialServices() {
        LOG.info("Pausing non-essential services");
        
        // Pause continuous services in all projects
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            if (project.isDisposed()) continue;
            
            try {
                // Get the memory-aware continuous service
                com.modforge.intellij.plugin.services.MemoryAwareContinuousService maService = 
                        project.getService(com.modforge.intellij.plugin.services.MemoryAwareContinuousService.class);
                
                if (maService != null && maService.isRunning()) {
                    LOG.info("Pausing memory-aware continuous service in project " + project.getName());
                    maService.stop();
                }
                
                // Get the continuous development service
                com.modforge.intellij.plugin.services.ContinuousDevelopmentService cdService =
                        project.getService(com.modforge.intellij.plugin.services.ContinuousDevelopmentService.class);
                
                if (cdService != null && cdService.isEnabled()) {
                    LOG.info("Pausing continuous development service in project " + project.getName());
                    cdService.disable();
                }
            } catch (Exception e) {
                LOG.warn("Error pausing services in project " + project.getName(), e);
            }
        }
    }
    
    /**
     * Restart critical services to recover memory
     * This performs a more drastic recovery by restarting essential services
     */
    private void restartCriticalServices() {
        LOG.info("Restarting critical services");
        
        try {
            // Restart memory monitoring service
            if (monitoringTask != null) {
                monitoringTask.cancel(false);
            }
            startMonitoring();
            LOG.info("Memory monitoring service restarted");
            
            // Restart memory manager services
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                LOG.info("Reinitializing memory manager");
                memoryManager.reinitialize();
            }
            
            // Restart memory-aware services in projects
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project == null || project.isDisposed()) continue;
                
                try {
                    // Restart memory optimizer
                    MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                    if (optimizer != null) {
                        LOG.info("Resetting memory optimizer for project: " + project.getName());
                        optimizer.reset();
                    }
                    
                    // Restart continuous services that were paused, if appropriate
                    MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
                    if (settings != null && settings.isMemoryAwareContinuousServiceEnabled()) {
                        com.modforge.intellij.plugin.services.MemoryAwareContinuousService maService = 
                                project.getService(com.modforge.intellij.plugin.services.MemoryAwareContinuousService.class);
                        
                        if (maService != null && !maService.isRunning()) {
                            LOG.info("Restarting memory-aware continuous service for project: " + project.getName());
                            maService.start();
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Error restarting services for project: " + project.getName(), e);
                }
            }
            
            // Request garbage collection again after all services have been restarted
            MemoryUtils.requestGarbageCollection();
            
        } catch (Exception e) {
            LOG.error("Error restarting critical services", e);
        }
    }
    
    /**
     * Add a recovery listener
     * 
     * @param listener The listener to add
     */
    public void addRecoveryListener(RecoveryListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a recovery listener
     * 
     * @param listener The listener to remove
     */
    public void removeRecoveryListener(RecoveryListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Notify listeners that recovery has started
     * 
     * @param level The recovery level
     */
    private void notifyRecoveryStarted(RecoveryLevel level) {
        synchronized (listeners) {
            for (RecoveryListener listener : listeners) {
                try {
                    listener.onRecoveryStarted(level);
                } catch (Exception e) {
                    LOG.warn("Error notifying recovery listener", e);
                }
            }
        }
    }
    
    /**
     * Notify listeners that recovery has completed
     * 
     * @param level The recovery level
     */
    private void notifyRecoveryCompleted(RecoveryLevel level) {
        synchronized (listeners) {
            for (RecoveryListener listener : listeners) {
                try {
                    listener.onRecoveryCompleted(level);
                } catch (Exception e) {
                    LOG.warn("Error notifying recovery listener", e);
                }
            }
        }
    }
    
    /**
     * Notify listeners that recovery has failed
     * 
     * @param level The recovery level
     * @param error The error that occurred
     */
    private void notifyRecoveryFailed(RecoveryLevel level, Exception error) {
        synchronized (listeners) {
            for (RecoveryListener listener : listeners) {
                try {
                    listener.onRecoveryFailed(level, error);
                } catch (Exception e) {
                    LOG.warn("Error notifying recovery listener", e);
                }
            }
        }
    }
    
    /**
     * Interface for recovery listeners
     */
    public interface RecoveryListener {
        /**
         * Called when recovery has started
         * 
         * @param level The recovery level
         */
        default void onRecoveryStarted(RecoveryLevel level) {}
        
        /**
         * Called when recovery has completed
         * 
         * @param level The recovery level
         */
        default void onRecoveryCompleted(RecoveryLevel level) {}
        
        /**
         * Called when recovery has failed
         * 
         * @param level The recovery level
         * @param error The error that occurred
         */
        default void onRecoveryFailed(RecoveryLevel level, Exception error) {}
    }
}