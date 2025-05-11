package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central manager for memory-related operations
 * Handles scheduled memory optimizations and monitoring
 */
public class MemoryManager {
    private static final Logger LOG = Logger.getInstance(MemoryManager.class);
    
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean optimizing = new AtomicBoolean(false);
    private final List<MemoryPressureListener> listeners = new ArrayList<>();
    private final ScheduledExecutorService executor = AppExecutorUtil.createBoundedScheduledExecutorService(
            "ModForgeMemoryManager", 1);
    private ScheduledFuture<?> monitoringTask;
    private ScheduledFuture<?> optimizationTask;
    
    /**
     * Get the singleton instance of MemoryManager
     */
    public static MemoryManager getInstance() {
        return ApplicationManager.getApplication().getService(MemoryManager.class);
    }
    
    /**
     * Initialize the memory manager
     * Sets up scheduled tasks for monitoring and optimization
     */
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            LOG.info("Initializing MemoryManager");
            startMemoryMonitoring();
            scheduleAutomaticOptimization();
        }
    }
    
    /**
     * Check if the memory manager is currently optimizing memory
     * 
     * @return True if memory is being optimized
     */
    public boolean isOptimizing() {
        return optimizing.get();
    }
    
    /**
     * Start the memory monitoring task
     * This periodically checks memory pressure and notifies listeners
     */
    private void startMemoryMonitoring() {
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
        }
        
        // Monitor memory every 5 seconds
        monitoringTask = executor.scheduleWithFixedDelay(
                this::checkMemoryPressure,
                0,
                5,
                TimeUnit.SECONDS
        );
        
        LOG.info("Memory monitoring started");
    }
    
    /**
     * Schedule automatic memory optimization based on settings
     */
    private void scheduleAutomaticOptimization() {
        if (optimizationTask != null && !optimizationTask.isDone()) {
            optimizationTask.cancel(false);
        }
        
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        if (!settings.isAutomaticOptimizationEnabled()) {
            LOG.info("Automatic memory optimization is disabled");
            return;
        }
        
        // Convert minutes to milliseconds
        long optimizationInterval = settings.getOptimizationIntervalMinutes() * 60 * 1000L;
        
        optimizationTask = executor.scheduleWithFixedDelay(
                this::performScheduledOptimization,
                optimizationInterval,
                optimizationInterval,
                TimeUnit.MILLISECONDS
        );
        
        LOG.info("Automatic memory optimization scheduled (interval: " +
                settings.getOptimizationIntervalMinutes() + " minutes)");
    }
    
    /**
     * Check memory pressure and notify listeners if necessary
     */
    private void checkMemoryPressure() {
        try {
            MemoryUtils.MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();
            
            // Notify listeners of the current pressure level
            for (MemoryPressureListener listener : listeners) {
                try {
                    listener.onMemoryPressureChanged(pressureLevel);
                } catch (Exception e) {
                    LOG.warn("Error notifying memory pressure listener", e);
                }
            }
            
            // Perform optimization if memory pressure is high
            MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
            if (settings.isOptimizeOnLowMemory() && 
                    (pressureLevel == MemoryUtils.MemoryPressureLevel.CRITICAL || 
                     pressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY)) {
                LOG.info("High memory pressure detected (" + pressureLevel + 
                        "), performing emergency optimization");
                performEmergencyOptimization();
            }
        } catch (Exception e) {
            LOG.error("Error checking memory pressure", e);
        }
    }
    
    /**
     * Perform scheduled memory optimization
     */
    private void performScheduledOptimization() {
        if (optimizing.get()) {
            LOG.info("Skipping scheduled optimization because optimization is already in progress");
            return;
        }
        
        LOG.info("Performing scheduled memory optimization");
        
        try {
            optimizing.set(true);
            MemoryUtils.logMemoryStats();
            
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed()) continue;
                
                MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                if (optimizer != null) {
                    optimizer.optimize(MemoryOptimizer.OptimizationLevel.NORMAL);
                }
            }
            
            // Request garbage collection after optimization
            MemoryUtils.requestGarbageCollection();
            
            MemoryUtils.logMemoryStats();
        } catch (Exception e) {
            LOG.error("Error during scheduled memory optimization", e);
        } finally {
            optimizing.set(false);
        }
    }
    
    /**
     * Perform emergency memory optimization when memory pressure is high
     */
    private void performEmergencyOptimization() {
        if (optimizing.get()) {
            LOG.info("Skipping emergency optimization because optimization is already in progress");
            return;
        }
        
        LOG.warn("Performing emergency memory optimization due to high memory pressure");
        
        try {
            optimizing.set(true);
            
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed()) continue;
                
                MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                if (optimizer != null) {
                    // Use aggressive optimization for emergency situations
                    optimizer.optimize(MemoryOptimizer.OptimizationLevel.AGGRESSIVE);
                }
            }
            
            // Request garbage collection after optimization
            MemoryUtils.requestGarbageCollection();
        } catch (Exception e) {
            LOG.error("Error during emergency memory optimization", e);
        } finally {
            optimizing.set(false);
        }
    }
    
    /**
     * Add a memory pressure listener
     * 
     * @param listener The listener to add
     */
    public void addMemoryPressureListener(@NotNull MemoryPressureListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a memory pressure listener
     * 
     * @param listener The listener to remove
     */
    public void removeMemoryPressureListener(@NotNull MemoryPressureListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Update automatic optimization settings based on current settings
     */
    public void updateSettings() {
        scheduleAutomaticOptimization();
    }
    
    /**
     * Perform optimization on all open projects
     * 
     * @param level The optimization level
     */
    public void optimizeAllProjects(@NotNull MemoryOptimizer.OptimizationLevel level) {
        if (optimizing.get()) {
            LOG.info("Skipping optimization because optimization is already in progress");
            return;
        }
        
        LOG.info("Optimizing all projects at level " + level);
        
        try {
            optimizing.set(true);
            
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed()) continue;
                
                MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                if (optimizer != null) {
                    optimizer.optimize(level);
                }
            }
            
            // Request garbage collection after optimization
            MemoryUtils.requestGarbageCollection();
        } catch (Exception e) {
            LOG.error("Error during all-project optimization", e);
        } finally {
            optimizing.set(false);
        }
    }
    
    /**
     * Get the recommended optimization level based on current memory pressure
     * 
     * @return The recommended optimization level
     */
    @NotNull
    public MemoryOptimizer.OptimizationLevel getRecommendedOptimizationLevel() {
        MemoryUtils.MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();
        
        switch (pressureLevel) {
            case EMERGENCY:
                return MemoryOptimizer.OptimizationLevel.AGGRESSIVE;
            case CRITICAL:
                return MemoryOptimizer.OptimizationLevel.NORMAL;
            case WARNING:
                return MemoryOptimizer.OptimizationLevel.CONSERVATIVE;
            default:
                return MemoryOptimizer.OptimizationLevel.MINIMAL;
        }
    }
    
    /**
     * Perform an immediate optimization on a specific project
     * 
     * @param project The project to optimize
     * @param level The optimization level, or null to use recommended level
     */
    public void optimizeProject(@NotNull Project project, @Nullable MemoryOptimizer.OptimizationLevel level) {
        if (optimizing.get()) {
            LOG.info("Skipping project optimization because optimization is already in progress");
            return;
        }
        
        MemoryOptimizer.OptimizationLevel actualLevel = level != null ? 
                level : getRecommendedOptimizationLevel();
        
        LOG.info("Optimizing project " + project.getName() + " at level " + actualLevel);
        
        try {
            optimizing.set(true);
            
            MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
            if (optimizer != null) {
                optimizer.optimize(actualLevel);
            }
            
            // Request garbage collection after optimization
            MemoryUtils.requestGarbageCollection();
        } catch (Exception e) {
            LOG.error("Error during project optimization", e);
        } finally {
            optimizing.set(false);
        }
    }
    
    /**
     * Interface for memory pressure listeners
     */
    public interface MemoryPressureListener {
        /**
         * Called when memory pressure changes
         * 
         * @param pressureLevel The new memory pressure level
         */
        void onMemoryPressureChanged(MemoryUtils.MemoryPressureLevel pressureLevel);
    }
}