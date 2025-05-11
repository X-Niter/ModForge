package com.modforge.intellij.plugin.memory.integration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.monitoring.MemoryHealthMonitor;
import com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import com.modforge.intellij.plugin.services.MemoryAwareContinuousService;
import com.modforge.intellij.plugin.services.MemoryRecoveryService;
import org.jetbrains.annotations.NotNull;

/**
 * Integration for memory-aware services
 * Sets up memory monitoring, recovery, and continuous services with proper memory awareness
 * Initializes all memory-related components and connects them together
 */
public class MemoryAwareServiceIntegration implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(MemoryAwareServiceIntegration.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing memory-aware services integration for project " + project.getName());
        
        try {
            // Initialize memory manager
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                memoryManager.initialize();
                LOG.info("Memory manager initialized");
            }
            
            // Initialize memory health monitor
            MemoryHealthMonitor healthMonitor = MemoryHealthMonitor.getInstance();
            if (healthMonitor != null) {
                healthMonitor.start();
                LOG.info("Memory health monitor started");
            }
            
            // Initialize memory recovery manager
            MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
            if (recoveryManager != null) {
                recoveryManager.initialize();
                LOG.info("Memory recovery manager initialized");
            }
            
            // Initialize memory recovery service
            MemoryRecoveryService recoveryService = project.getService(MemoryRecoveryService.class);
            if (recoveryService != null) {
                recoveryService.start();
                LOG.info("Memory recovery service started");
            }
            
            // Initialize memory-aware continuous service based on settings
            MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
            if (settings.isMemoryAwareContinuousServiceEnabled()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    MemoryAwareContinuousService continuousService = 
                            project.getService(MemoryAwareContinuousService.class);
                    
                    if (continuousService != null) {
                        continuousService.start();
                        LOG.info("Memory-aware continuous service started");
                    }
                });
            }
            
            // Setup integration between health monitor and recovery manager
            if (healthMonitor != null && recoveryManager != null) {
                setupHealthMonitorRecoveryIntegration(healthMonitor, recoveryManager);
            }
            
            // Log initial memory stats
            MemoryUtils.logMemoryStats();
            
        } catch (Exception e) {
            LOG.error("Error initializing memory-aware services integration", e);
        }
    }
    
    /**
     * Setup integration between health monitor and recovery manager
     * 
     * @param healthMonitor The health monitor
     * @param recoveryManager The recovery manager
     */
    private void setupHealthMonitorRecoveryIntegration(
            MemoryHealthMonitor healthMonitor, 
            MemoryRecoveryManager recoveryManager) {
        
        // Add listener for predicted memory pressure
        healthMonitor.addMemoryHealthListener(new MemoryHealthMonitor.MemoryHealthListener() {
            @Override
            public void onPredictedMemoryPressure(double predictedUsagePercentage, int minutesAway) {
                LOG.info("Memory health monitor predicts " + String.format("%.1f", predictedUsagePercentage) + 
                        "% usage in " + minutesAway + " minutes");
                
                // If predicting severe pressure, take preemptive action
                if (predictedUsagePercentage > 90 && minutesAway <= 5) {
                    LOG.warn("Taking preemptive recovery action due to predicted severe memory pressure");
                    recoveryManager.initiateRecovery(MemoryRecoveryManager.RecoveryLevel.LEVEL2);
                }
                // If predicting moderate pressure, take lighter preemptive action
                else if (predictedUsagePercentage > 80 && minutesAway <= 3) {
                    LOG.info("Taking light preemptive recovery action due to predicted memory pressure");
                    recoveryManager.initiateRecovery(MemoryRecoveryManager.RecoveryLevel.LEVEL1);
                }
            }
            
            @Override
            public void onMemoryHealthStatusChanged(MemoryHealthMonitor.MemoryHealthStatus status, 
                    com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot snapshot) {
                
                LOG.info("Memory health status changed to " + status);
                
                // Take action based on health status
                switch (status) {
                    case CRITICAL:
                        LOG.warn("Critical memory health status detected, initiating recovery");
                        recoveryManager.initiateRecovery(MemoryRecoveryManager.RecoveryLevel.LEVEL2);
                        break;
                    case PROBLEMATIC:
                        LOG.info("Problematic memory health status detected, initiating light recovery");
                        recoveryManager.initiateRecovery(MemoryRecoveryManager.RecoveryLevel.LEVEL1);
                        break;
                    default:
                        // No action needed
                        break;
                }
            }
        });
    }
}