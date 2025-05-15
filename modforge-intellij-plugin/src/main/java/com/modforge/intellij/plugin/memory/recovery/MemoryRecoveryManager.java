package com.modforge.intellij.plugin.memory.recovery;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager for memory recovery operations.
 * Provides automated recovery actions when memory pressure reaches critical
 * levels.
 */
public class MemoryRecoveryManager {
    private static final Logger LOG = Logger.getInstance(MemoryRecoveryManager.class);

    // Singleton instance
    private static MemoryRecoveryManager instance;

    // Thresholds for auto-recovery
    private static final int DEFAULT_RECOVERY_THRESHOLD_PERCENT = 80;
    private static final int EMERGENCY_RECOVERY_THRESHOLD_PERCENT = 90;

    // Recovery count to track number of recovery actions
    private final AtomicInteger recoveryCount = new AtomicInteger(0);
    private final AtomicBoolean recovering = new AtomicBoolean(false);

    // Recovery actions to perform
    private final List<MemoryRecoveryAction> recoveryActions = new ArrayList<>();

    // Recovery listeners
    private final List<RecoveryListener> recoveryListeners = new ArrayList<>();

    // Scheduled executor for recovery monitoring
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> monitoringTask;

    /**
     * Enum for recovery action priorities
     */
    public enum RecoveryPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Private constructor for singleton pattern
     */
    private MemoryRecoveryManager() {
        executor = AppExecutorUtil.createBoundedScheduledExecutorService(
                "MemoryRecoveryManager", 1);

        // Register standard recovery actions
        registerStandardRecoveryActions();

        // Start monitoring for recovery
        startMonitoring();
    }

    /**
     * Get the singleton instance
     *
     * @return The singleton instance
     */
    public static synchronized MemoryRecoveryManager getInstance() {
        if (instance == null) {
            instance = new MemoryRecoveryManager();
        }
        return instance;
    }

    /**
     * Initialize the memory recovery manager
     * Sets up the recovery systems and prepares for operation
     */
    public void initialize() {
        LOG.info("Initializing memory recovery manager");

        try {
            // Check if monitoring is active and start if needed
            if (monitoringTask == null || monitoringTask.isDone() || monitoringTask.isCancelled()) {
                startMonitoring();
            }

            // Ensure standard recovery actions are registered
            if (recoveryActions.isEmpty()) {
                registerStandardRecoveryActions();
            }

            LOG.info("Memory recovery manager initialized successfully");
        } catch (Exception e) {
            LOG.error("Error initializing memory recovery manager", e);
        }
    }

    private void registerStandardRecoveryActions() {
        // Clear caches (Low priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Clear IDE Caches",
                RecoveryPriority.LOW,
                this::clearIDECaches));

        // Run garbage collection (Low priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Run Garbage Collection",
                RecoveryPriority.LOW,
                this::runGarbageCollection));

        // Optimize memory usage (Medium priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Optimize Memory Usage",
                RecoveryPriority.MEDIUM,
                this::optimizeMemoryUsage));

        // Close unused editors (Medium priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Close Unused Editors",
                RecoveryPriority.MEDIUM,
                this::closeUnusedEditors));

        // Reduce feature set (High priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Reduce Feature Set",
                RecoveryPriority.HIGH,
                this::reduceFeatureSet));

        // Stop background tasks (High priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Stop Background Tasks",
                RecoveryPriority.HIGH,
                this::stopBackgroundTasks));

        // Emergency recovery (Critical priority)
        recoveryActions.add(new MemoryRecoveryAction(
                "Emergency Recovery",
                RecoveryPriority.CRITICAL,
                this::performEmergencyRecovery));
    }

    /**
     * Start monitoring memory for recovery
     */
    private void startMonitoring() {
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
        }

        // Monitor memory every 10 seconds for recovery purposes
        monitoringTask = executor.scheduleWithFixedDelay(
                this::checkMemoryForRecovery,
                10,
                10,
                TimeUnit.SECONDS);

        LOG.info("Memory recovery monitoring started");
    }

    /**
     * Check memory status and trigger recovery if needed
     */
    private void checkMemoryForRecovery() {
        try {
            // Get memory pressure level
            MemoryUtils.MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();

            // Check if automatic recovery is enabled
            MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
            if (!settings.isAutomaticRecoveryEnabled()) {
                return;
            }

            double memoryUsagePercent = MemoryUtils.getMemoryUsagePercentage();

            // Trigger recovery based on thresholds
            if (pressureLevel == MemoryUtils.MemoryPressureLevel.EMERGENCY &&
                    memoryUsagePercent >= EMERGENCY_RECOVERY_THRESHOLD_PERCENT) {
                performRecovery(RecoveryPriority.CRITICAL);
            } else if (pressureLevel == MemoryUtils.MemoryPressureLevel.CRITICAL &&
                    memoryUsagePercent >= DEFAULT_RECOVERY_THRESHOLD_PERCENT) {
                performRecovery(RecoveryPriority.HIGH);
            } else if (pressureLevel == MemoryUtils.MemoryPressureLevel.WARNING) {
                // For warning level, only perform recovery if count is low
                if (recoveryCount.get() < 3) {
                    performRecovery(RecoveryPriority.MEDIUM);
                }
            }
        } catch (Exception e) {
            LOG.error("Error checking memory for recovery", e);
        }
    }

    /**
     * Add a recovery listener
     *
     * @param listener The listener to add
     */
    public void addRecoveryListener(@NotNull RecoveryListener listener) {
        synchronized (recoveryListeners) {
            recoveryListeners.add(listener);
            LOG.debug("Added recovery listener: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Remove a recovery listener
     *
     * @param listener The listener to remove
     */
    public void removeRecoveryListener(@NotNull RecoveryListener listener) {
        synchronized (recoveryListeners) {
            recoveryListeners.remove(listener);
            LOG.debug("Removed recovery listener: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all recovery listeners that recovery has started
     *
     * @param priority The priority of the recovery
     */
    private void notifyRecoveryStarted(@NotNull RecoveryPriority priority) {
        List<RecoveryListener> listeners;
        synchronized (recoveryListeners) {
            listeners = new ArrayList<>(recoveryListeners);
        }

        for (RecoveryListener listener : listeners) {
            try {
                listener.onRecoveryStarted(priority);
            } catch (Exception e) {
                LOG.warn("Error notifying recovery listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Notify all recovery listeners that recovery has completed
     *
     * @param priority The priority of the recovery
     */
    private void notifyRecoveryCompleted(@NotNull RecoveryPriority priority) {
        List<RecoveryListener> listeners;
        synchronized (recoveryListeners) {
            listeners = new ArrayList<>(recoveryListeners);
        }

        for (RecoveryListener listener : listeners) {
            try {
                listener.onRecoveryCompleted(priority);
            } catch (Exception e) {
                LOG.warn("Error notifying recovery listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Notify all recovery listeners that recovery has failed
     *
     * @param priority The priority of the recovery
     * @param error    The error that occurred
     */
    private void notifyRecoveryFailed(@NotNull RecoveryPriority priority, @NotNull Exception error) {
        List<RecoveryListener> listeners;
        synchronized (recoveryListeners) {
            listeners = new ArrayList<>(recoveryListeners);
        }

        for (RecoveryListener listener : listeners) {
            try {
                listener.onRecoveryFailed(priority, error);
            } catch (Exception e) {
                LOG.warn("Error notifying recovery listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Perform memory recovery up to the specified priority level
     *
     * @param maxPriority The maximum priority level to execute
     * @return True if recovery was performed
     */
    public boolean performRecovery(@NotNull RecoveryPriority maxPriority) {
        if (recovering.get()) {
            LOG.info("Skipping recovery because recovery is already in progress");
            return false;
        }

        try {
            recovering.set(true);
            int count = recoveryCount.incrementAndGet();

            LOG.info("Performing memory recovery (priority: " + maxPriority +
                    ", count: " + count + ")");

            // Notify listeners that recovery has started
            notifyRecoveryStarted(maxPriority);

            MemoryUtils.logMemoryStats();

            // Sort actions by priority
            List<MemoryRecoveryAction> sortedActions = new ArrayList<>(recoveryActions);
            sortedActions.sort((a1, a2) -> a1.getPriority().compareTo(a2.getPriority()));

            boolean anyPerformed = false;

            try {
                // Execute actions up to the maximum priority
                for (MemoryRecoveryAction action : sortedActions) {
                    if (action.getPriority().compareTo(maxPriority) <= 0) {
                        try {
                            LOG.info("Executing recovery action: " + action.getName() +
                                    " (priority: " + action.getPriority() + ")");

                            boolean success = action.execute();
                            if (success) {
                                LOG.info("Recovery action completed successfully: " + action.getName());
                                anyPerformed = true;
                            } else {
                                LOG.warn("Recovery action did not complete successfully: " + action.getName());
                            }
                        } catch (Exception e) {
                            LOG.error("Error executing recovery action: " + action.getName(), e);
                        }
                    }
                }

                MemoryUtils.logMemoryStats();

                // Notify listeners that recovery has completed
                notifyRecoveryCompleted(maxPriority);

                return anyPerformed;
            } catch (Exception e) {
                // Notify listeners that recovery has failed
                notifyRecoveryFailed(maxPriority, e);
                LOG.error("Error performing memory recovery", e);
                return false;
            }
        } finally {
            recovering.set(false);
        }
    }

    /**
     * Clear IDE caches
     *
     * @return True if the action was successful
     */
    private boolean clearIDECaches() {
        try {
            // Alternative cache invalidation
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(() -> {
                com.intellij.util.indexing.FileBasedIndex.getInstance().invalidateCaches();
            });

            return true;
        } catch (Exception e) {
            LOG.error("Error clearing IDE caches", e);
            return false;
        }
    }

    /**
     * Run garbage collection
     *
     * @return True if the action was successful
     */
    private boolean runGarbageCollection() {
        try {
            MemoryUtils.requestGarbageCollection();
            return true;
        } catch (Exception e) {
            LOG.error("Error running garbage collection", e);
            return false;
        }
    }

    /**
     * Optimize memory usage
     *
     * @return True if the action was successful
     */
    private boolean optimizeMemoryUsage() {
        try {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed())
                    continue;

                MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
                if (optimizer != null) {
                    optimizer.optimize(MemoryOptimizer.OptimizationLevel.AGGRESSIVE);
                }
            }

            return true;
        } catch (Exception e) {
            LOG.error("Error optimizing memory usage", e);
            return false;
        }
    }

    /**
     * Close unused editors
     *
     * @return True if the action was successful
     */
    private boolean closeUnusedEditors() {
        try {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed())
                    continue;

                com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(() -> {
                    com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl editorManager = (com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl) com.intellij.openapi.fileEditor.FileEditorManager
                            .getInstance(project);

                    editorManager.closeAllFiles();
                });
            }

            return true;
        } catch (Exception e) {
            LOG.error("Error closing unused editors", e);
            return false;
        }
    }

    /**
     * Reduce feature set by disabling non-essential features
     *
     * @return True if the action was successful
     */
    private boolean reduceFeatureSet() {
        try {
            // Get memory-aware continuous service for all projects and set to reduced
            // features mode
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed())
                    continue;

                com.modforge.intellij.plugin.services.MemoryAwareContinuousService maService = com.modforge.intellij.plugin.utils.ServiceUtil
                        .getServiceFromProject(
                                project,
                                com.modforge.intellij.plugin.services.MemoryAwareContinuousService.class);

                if (maService != null) {
                    // Use the delegation method in MemoryAwareContinuousService
                    maService.setReducedFeaturesMode(true);
                }
            }

            return true;
        } catch (Exception e) {
            LOG.error("Error reducing feature set", e);
            return false;
        }
    }

    /**
     * Stop background tasks
     *
     * @return True if the action was successful
     */
    public boolean stopBackgroundTasks() {
        try {
            // Stop continuous development service
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isDisposed())
                    continue;

                com.modforge.intellij.plugin.services.ContinuousDevelopmentService cdService = com.modforge.intellij.plugin.utils.ServiceUtil
                        .getServiceFromProject(
                                project,
                                com.modforge.intellij.plugin.services.ContinuousDevelopmentService.class);

                if (cdService != null && cdService.isRunning()) {
                    cdService.stop();
                }

                // Stop other background tasks
                com.modforge.intellij.plugin.services.MemoryAwareContinuousService maService = com.modforge.intellij.plugin.utils.ServiceUtil
                        .getServiceFromProject(
                                project,
                                com.modforge.intellij.plugin.services.MemoryAwareContinuousService.class);

                if (maService != null) {
                    maService.pauseBackgroundTasks();
                }
            }

            return true;
        } catch (Exception e) {
            LOG.error("Error stopping background tasks", e);
            return false;
        }
    }

    /**
     * Perform emergency recovery
     *
     * @return True if the action was successful
     */
    private boolean performEmergencyRecovery() {
        try {
            // Perform all other recovery actions
            clearIDECaches();
            runGarbageCollection();
            optimizeMemoryUsage();
            closeUnusedEditors();
            reduceFeatureSet();
            stopBackgroundTasks();

            // Custom approach to close modal dialogs
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(() -> {
                java.awt.Window[] windows = java.awt.Window.getWindows();
                for (java.awt.Window window : windows) {
                    if (window instanceof javax.swing.JDialog) {
                        ((javax.swing.JDialog) window).dispose();
                    }
                }
            });

            // Force full garbage collection
            System.gc();

            // Allow a brief pause for GC to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // Second GC pass
            System.gc();

            return true;
        } catch (Exception e) {
            LOG.error("Error performing emergency recovery", e);
            return false;
        }
    }

    /**
     * Reset the recovery manager
     */
    public void reset() {
        LOG.info("Resetting memory recovery manager");

        // Stop monitoring
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }

        // Reset state
        recoveryCount.set(0);
        recovering.set(false);

        // Store current recovery listeners to re-register
        List<RecoveryListener> currentListeners;
        synchronized (recoveryListeners) {
            currentListeners = new ArrayList<>(recoveryListeners);
            recoveryListeners.clear();
        }

        // Reset recovery actions
        synchronized (recoveryActions) {
            recoveryActions.clear();
            registerStandardRecoveryActions();
        }

        // Re-register recovery listeners
        for (RecoveryListener listener : currentListeners) {
            addRecoveryListener(listener);
        }

        // Restart monitoring
        startMonitoring();

        LOG.info("Memory recovery manager reset completed");
    }

    /**
     * Check if recovery is in progress
     *
     * @return True if recovery is in progress
     */
    public boolean isRecovering() {
        return recovering.get();
    }

    /**
     * Get the number of recovery actions performed
     *
     * @return The number of recovery actions performed
     */
    public int getRecoveryCount() {
        return recoveryCount.get();
    }

    /**
     * Add a custom recovery action
     *
     * @param name     The name of the action
     * @param priority The priority of the action
     * @param action   The action to perform
     */
    public void addRecoveryAction(@NotNull String name, @NotNull RecoveryPriority priority,
            @NotNull RecoveryActionExecutor action) {
        recoveryActions.add(new MemoryRecoveryAction(name, priority, action));
        LOG.info("Added custom recovery action: " + name + " (priority: " + priority + ")");
    }

    /**
     * Reset the recovery count
     */
    public void resetRecoveryCount() {
        recoveryCount.set(0);
        LOG.info("Reset recovery count to 0");
    }

    /**
     * Interface for recovery action execution
     */
    public interface RecoveryActionExecutor {
        boolean execute();
    }

    /**
     * Interface for listening to recovery events
     */
    public interface RecoveryListener {
        /**
         * Called when a recovery process is started
         *
         * @param priority The priority of the recovery
         */
        default void onRecoveryStarted(RecoveryPriority priority) {
        }

        /**
         * Called when a recovery process is completed
         *
         * @param priority The priority of the recovery
         */
        default void onRecoveryCompleted(RecoveryPriority priority) {
        }

        /**
         * Called when a recovery process fails
         *
         * @param priority The priority of the recovery
         * @param error    The error that occurred
         */
        default void onRecoveryFailed(RecoveryPriority priority, Exception error) {
        }
    }

    /**
     * Class representing a memory recovery action
     */
    private static class MemoryRecoveryAction {
        private final String name;
        private final RecoveryPriority priority;
        private final RecoveryActionExecutor action;

        /**
         * Constructor
         *
         * @param name     The name of the action
         * @param priority The priority of the action
         * @param action   The action to perform
         */
        public MemoryRecoveryAction(@NotNull String name, @NotNull RecoveryPriority priority,
                @NotNull RecoveryActionExecutor action) {
            this.name = name;
            this.priority = priority;
            this.action = action;
        }

        /**
         * Get the name of the action
         *
         * @return The name of the action
         */
        public String getName() {
            return name;
        }

        /**
         * Get the priority of the action
         *
         * @return The priority of the action
         */
        public RecoveryPriority getPriority() {
            return priority;
        }

        /**
         * Execute the action
         *
         * @return True if the action was successful
         */
        public boolean execute() {
            return action.execute();
        }
    }
}