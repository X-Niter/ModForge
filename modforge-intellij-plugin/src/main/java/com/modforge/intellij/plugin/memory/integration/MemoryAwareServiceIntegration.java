package com.modforge.intellij.plugin.memory.integration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.config.MemoryThresholdConfig;
import com.modforge.intellij.plugin.memory.monitoring.MemoryHealthMonitor;
import com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import com.modforge.intellij.plugin.services.MemoryAwareContinuousService;
import com.modforge.intellij.plugin.services.MemoryRecoveryService;
import org.jetbrains.annotations.NotNull;

/**
 * Integration for memory-aware services
 * Sets up memory monitoring, recovery, and continuous services with proper
 * memory awareness
 * Initializes all memory-related components and connects them together
 */
public class MemoryAwareServiceIntegration implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(MemoryAwareServiceIntegration.class);

    @Override
    public void runActivity(@NotNull Project project) {
        if (project.isDisposed()) {
            LOG.info("Project is already disposed, skipping memory-aware services integration");
            return;
        }

        LOG.info("Initializing memory-aware services integration for project " + project.getName());

        try {
            // Initialize memory manager
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                memoryManager.initialize();
                LOG.info("Memory manager initialized");
            } else {
                LOG.warn("Memory manager is not available");
            }

            // Initialize memory health monitor
            MemoryHealthMonitor healthMonitor = MemoryHealthMonitor.getInstance();
            if (healthMonitor != null) {
                healthMonitor.start();
                LOG.info("Memory health monitor started");
            } else {
                LOG.warn("Memory health monitor is not available");
            }

            // Initialize memory recovery manager
            MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
            if (recoveryManager != null) {
                recoveryManager.initialize();
                LOG.info("Memory recovery manager initialized");
            } else {
                LOG.warn("Memory recovery manager is not available");
            }

            // Initialize memory recovery service
            MemoryRecoveryService recoveryService = null;
            try {
                recoveryService = project.getService(MemoryRecoveryService.class);
            } catch (Exception ex) {
                LOG.warn("Failed to get memory recovery service", ex);
            }

            if (recoveryService != null) {
                recoveryService.start();
                LOG.info("Memory recovery service started");
            } else {
                LOG.warn("Memory recovery service is not available");
            }

            // Initialize memory-aware continuous service based on settings
            MemoryManagementSettings settings = null;
            try {
                settings = MemoryManagementSettings.getInstance();
            } catch (Exception ex) {
                LOG.warn("Failed to get memory management settings", ex);
            }

            if (settings != null && settings.isMemoryAwareContinuousServiceEnabled()) {
                final MemoryManagementSettings finalSettings = settings;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        LOG.info("Project is disposed, skipping memory-aware continuous service initialization");
                        return;
                    }

                    MemoryAwareContinuousService continuousService = null;
                    try {
                        continuousService = project.getService(MemoryAwareContinuousService.class);
                    } catch (Exception ex) {
                        LOG.warn("Failed to get memory-aware continuous service", ex);
                    }

                    if (continuousService != null) {
                        continuousService.start();
                        LOG.info("Memory-aware continuous service started with settings: threshold=" +
                                finalSettings.getCriticalThresholdPercent() + "% (critical), interval=" +
                                finalSettings.getContinuousServiceDefaultIntervalMinutes() + "min");
                    } else {
                        LOG.warn("Memory-aware continuous service is not available");
                    }
                });
            } else {
                LOG.info("Memory-aware continuous service is disabled in settings or settings are not available");
            }

            // Setup integration between health monitor and recovery manager
            if (healthMonitor != null && recoveryManager != null) {
                setupHealthMonitorRecoveryIntegration(healthMonitor, recoveryManager);
            } else {
                LOG.warn(
                        "Cannot set up health monitor and recovery manager integration - one or both components are not available");
            }

            // Log initial memory stats
            try {
                MemoryUtils.logMemoryStats();
            } catch (Exception ex) {
                LOG.warn("Failed to log initial memory stats", ex);
            }

        } catch (Exception e) {
            LOG.error("Error initializing memory-aware services integration", e);
        }
    }

    /**
     * Setup integration between health monitor and recovery manager
     * 
     * @param healthMonitor   The health monitor
     * @param recoveryManager The recovery manager
     */
    private void setupHealthMonitorRecoveryIntegration(
            @NotNull MemoryHealthMonitor healthMonitor,
            @NotNull MemoryRecoveryManager recoveryManager) {

        try {
            // Add listener for predicted memory pressure
            healthMonitor.addMemoryHealthListener(new MemoryHealthMonitor.MemoryHealthListener() {
                @Override
                public void onPredictedMemoryPressure(double predictedUsagePercentage, int minutesAway) {
                    try {
                        LOG.info("Memory health monitor predicts " + String.format("%.1f", predictedUsagePercentage) +
                                "% usage in " + minutesAway + " minutes");

                        // Get threshold config if available
                        int severeThreshold = 90;
                        int moderateThreshold = 80;
                        int severeMinutesThreshold = 5;
                        int moderateMinutesThreshold = 3;

                        MemoryThresholdConfig thresholdConfig = null;
                        try {
                            thresholdConfig = MemoryThresholdConfig.getInstance();
                        } catch (Exception ex) {
                            LOG.debug("Could not get memory threshold config", ex);
                        }

                        if (thresholdConfig != null) {
                            severeThreshold = thresholdConfig.getEmergencyThresholdPercent();
                            moderateThreshold = thresholdConfig.getCriticalThresholdPercent();
                        }

                        // If predicting severe pressure, take preemptive action
                        if (predictedUsagePercentage > severeThreshold && minutesAway <= severeMinutesThreshold) {
                            LOG.warn("Taking preemptive recovery action due to predicted severe memory pressure: " +
                                    String.format("%.1f", predictedUsagePercentage) + "% in " + minutesAway
                                    + " minutes");
                            // Use performRecovery instead of initiateRecovery, with correct enum type
                            recoveryManager.performRecovery(MemoryRecoveryManager.RecoveryPriority.HIGH);
                        }
                        // If predicting moderate pressure, take lighter preemptive action
                        else if (predictedUsagePercentage > moderateThreshold
                                && minutesAway <= moderateMinutesThreshold) {
                            LOG.info("Taking light preemptive recovery action due to predicted memory pressure: " +
                                    String.format("%.1f", predictedUsagePercentage) + "% in " + minutesAway
                                    + " minutes");
                            // Use performRecovery instead of initiateRecovery, with correct enum type
                            recoveryManager.performRecovery(MemoryRecoveryManager.RecoveryPriority.MEDIUM);
                        }
                    } catch (Exception ex) {
                        LOG.error("Error handling predicted memory pressure", ex);
                    }
                }

                @Override
                public void onMemoryHealthStatusChanged(MemoryHealthMonitor.MemoryHealthStatus status,
                        com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot snapshot) {
                    try {
                        if (status == null) {
                            LOG.warn("Received null memory health status");
                            return;
                        }

                        LOG.info("Memory health status changed to " + status +
                                (snapshot != null
                                        ? " (Used: " + String.format("%.1f", snapshot.getUsedMemoryPercent()) + "%, " +
                                                "Available: " + String.format("%.1f", snapshot.getAvailableMemoryMB())
                                                + " MB)"
                                        : ""));

                        // Take action based on health status
                        switch (status) {
                            case CRITICAL:
                                LOG.warn("Critical memory health status detected, initiating recovery");
                                // Use performRecovery instead of initiateRecovery, with correct enum type
                                recoveryManager.performRecovery(MemoryRecoveryManager.RecoveryPriority.HIGH);
                                break;
                            case PROBLEMATIC:
                                LOG.info("Problematic memory health status detected, initiating light recovery");
                                // Use performRecovery instead of initiateRecovery, with correct enum type
                                recoveryManager.performRecovery(MemoryRecoveryManager.RecoveryPriority.MEDIUM);
                                break;
                            case HEALTHY:
                                LOG.debug("Memory health status is healthy, no action needed");
                                break;
                            default:
                                LOG.info("Memory health status: " + status + ", no specific action defined");
                                break;
                        }
                    } catch (Exception ex) {
                        LOG.error("Error handling memory health status change", ex);
                    }
                }
            });

            LOG.info("Successfully set up integration between memory health monitor and recovery manager");
        } catch (Exception ex) {
            LOG.error("Failed to set up integration between memory health monitor and recovery manager", ex);
        }
    }
}