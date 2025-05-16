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
     * Gets the current memory snapshot
     * 
     * @return The current memory snapshot
     */
    public com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot getCurrentMemorySnapshot() {
        return com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot.createCurrentSnapshot("NORMAL");
    }

    // Compatibility aliases
    public void addListener(MemoryPressureListener listener) {
        addMemoryPressureListener(listener);
    }

    public void removeListener(MemoryPressureListener listener) {
        removeMemoryPressureListener(listener);
    }

    public com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot getCurrentSnapshot() {
        return getCurrentMemorySnapshot();
    }

    /**
     * Reinitialize the memory manager
     * This can be called to restart memory monitoring and optimization after issues
     */
    public void reinitialize() {
        LOG.info("Reinitializing MemoryManager");

        // Force restart of monitoring regardless of initialization state
        startMemoryMonitoring();

        // Force restart of optimization regardless of initialization state
        scheduleAutomaticOptimization();

        // Reset initialization flag if it was false
        if (!initialized.get()) {
            initialized.set(true);
        }

        // Request garbage collection
        MemoryUtils.requestGarbageCollection();

        LOG.info("MemoryManager reinitialized");
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
                TimeUnit.SECONDS);

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
                TimeUnit.MILLISECONDS);

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
                if (project.isDisposed())
                    continue;

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
                if (project.isDisposed())
                    continue;

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
     * Performs a complete reset of the memory management system
     * This resets and reinitializes all memory-related components
     */
    public void resetMemorySystem() {
        LOG.info("Performing complete reset of memory management system");

        try {
            // Cancel current scheduled tasks
            if (monitoringTask != null) {
                monitoringTask.cancel(false);
                monitoringTask = null;
            }

            if (optimizationTask != null) {
                optimizationTask.cancel(false);
                optimizationTask = null;
            }

            // Reset state
            optimizing.set(false);

            // Request forceful garbage collection
            MemoryUtils.requestGarbageCollection();

            // Reset recovery manager
            try {
                com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager recoveryManager = com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager
                        .getInstance();
                if (recoveryManager != null) {
                    recoveryManager.reset();
                    LOG.info("Memory recovery manager reset successful");
                }
            } catch (Exception e) {
                LOG.warn("Error resetting memory recovery manager", e);
            }

            // Reset snapshot manager
            try {
                com.modforge.intellij.plugin.memory.monitoring.MemorySnapshotManager snapshotManager = com.modforge.intellij.plugin.memory.monitoring.MemorySnapshotManager
                        .getInstance();
                if (snapshotManager != null) {
                    snapshotManager.reset();
                    LOG.info("Memory snapshot manager reset successful");
                }
            } catch (Exception e) {
                LOG.warn("Error resetting memory snapshot manager", e);
            }

            // Reset and optimize all projects
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed())
                    continue;

                try {
                    // Reset memory optimizer
                    MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                    if (optimizer != null) {
                        optimizer.reset();
                        LOG.info("Memory optimizer reset for project: " + project.getName());
                    }

                    // Reset memory-aware continuous service
                    com.modforge.intellij.plugin.services.MemoryAwareContinuousService maService = com.modforge.intellij.plugin.utils.ServiceUtil
                            .getServiceFromProject(
                                    project,
                                    com.modforge.intellij.plugin.services.MemoryAwareContinuousService.class);
                    if (maService != null) {
                        maService.reset(true); // Auto-restart the service
                        LOG.info("Memory-aware continuous service reset for project: " + project.getName());
                    }

                    // Reset visualization panel if available
                    com.modforge.intellij.plugin.memory.visualization.MemoryVisualizationPanel visualPanel = com.modforge.intellij.plugin.utils.ServiceUtil
                            .getServiceFromProject(
                                    project,
                                    com.modforge.intellij.plugin.memory.visualization.MemoryVisualizationPanel.class);
                    if (visualPanel != null) {
                        visualPanel.resetVisualization();
                        LOG.info("Memory visualization panel reset for project: " + project.getName());
                    }
                } catch (Exception e) {
                    LOG.warn("Error resetting memory components for project: " + project.getName(), e);
                }
            }

            // Clear listeners and re-register them
            try {
                listeners.forEach(this::removeMemoryPressureListener);
                List<MemoryPressureListener> listenersBackup = new ArrayList<>(listeners);
                listeners.clear();
                listenersBackup.forEach(this::addMemoryPressureListener);
                LOG.info("Memory pressure listeners reset successful");
            } catch (Exception e) {
                LOG.warn("Error resetting memory pressure listeners", e);
            }

            // Reinitialize the memory manager
            initialized.set(false);
            reinitialize();

            // Request another garbage collection after reset
            MemoryUtils.requestGarbageCollection();

            LOG.info("Memory management system reset completed successfully");
        } catch (Exception e) {
            LOG.error("Error during memory management system reset", e);

            // Force reinitialization even if there was an error
            try {
                initialized.set(false);
                reinitialize();
            } catch (Exception reinitEx) {
                LOG.error("Failed to reinitialize memory manager after reset error", reinitEx);
            }
        }
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
                if (project.isDisposed())
                    continue;

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
     * Get the current memory pressure level
     * 
     * @return The current memory pressure level
     */
    @NotNull
    public MemoryUtils.MemoryPressureLevel getPressureLevel() {
        return MemoryUtils.getMemoryPressureLevel();
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
     * @param level   The optimization level, or null to use recommended level
     */
    public void optimizeProject(@NotNull Project project, @Nullable MemoryOptimizer.OptimizationLevel level) {
        if (optimizing.get()) {
            LOG.info("Skipping project optimization because optimization is already in progress");
            return;
        }

        MemoryOptimizer.OptimizationLevel actualLevel = level != null ? level : getRecommendedOptimizationLevel();

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