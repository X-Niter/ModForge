package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.memory.config.MemoryThresholdConfig;
import com.modforge.intellij.plugin.memory.monitoring.MemoryHealthMonitor;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshotManager;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import com.modforge.intellij.plugin.services.MemoryAwareContinuousService;
import org.jetbrains.annotations.NotNull;

/**
 * Startup activity for memory management
 * This ensures that memory management features are initialized when the project opens
 */
public class MemoryManagementStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(MemoryManagementStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        if (project.isDisposed()) {
            LOG.info("Project is already disposed, skipping memory management initialization");
            return;
        }
        
        LOG.info("Initializing memory management for project: " + project.getName());
        
        try {
            // Initialize memory manager
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                memoryManager.initialize();
                LOG.info("Memory manager initialized");
            } else {
                LOG.warn("Memory manager not available");
            }
            
            // Initialize memory snapshot manager
            try {
                MemorySnapshotManager snapshotManager = MemorySnapshotManager.getInstance();
                if (snapshotManager != null) {
                    snapshotManager.initialize();
                    LOG.info("Memory snapshot manager initialized");
                } else {
                    LOG.warn("Memory snapshot manager not available");
                }
            } catch (Exception ex) {
                LOG.error("Error initializing memory snapshot manager", ex);
            }
            
            // Initialize memory health monitor
            try {
                MemoryHealthMonitor healthMonitor = MemoryHealthMonitor.getInstance();
                if (healthMonitor != null) {
                    healthMonitor.start();
                    LOG.info("Memory health monitor started");
                } else {
                    LOG.warn("Memory health monitor not available");
                }
            } catch (Exception ex) {
                LOG.error("Error initializing memory health monitor", ex);
            }
            
            // Initialize memory threshold config
            try {
                MemoryThresholdConfig thresholdConfig = MemoryThresholdConfig.getInstance();
                if (thresholdConfig != null) {
                    if (thresholdConfig.getState() == null || 
                        thresholdConfig.getState().environmentConfigs.isEmpty()) {
                        LOG.info("Initializing memory threshold configuration with defaults");
                        thresholdConfig.initializeDefaults();
                    }
                    LOG.info("Memory threshold configuration initialized");
                } else {
                    LOG.warn("Memory threshold configuration not available");
                }
            } catch (Exception ex) {
                LOG.error("Error initializing memory threshold configuration", ex);
            }
            
            // Initialize memory optimizer
            try {
                MemoryOptimizer memoryOptimizer = project.getService(MemoryOptimizer.class);
                if (memoryOptimizer != null) {
                    LOG.info("Memory optimizer initialized");
                } else {
                    LOG.warn("Memory optimizer not available");
                }
            } catch (Exception ex) {
                LOG.error("Error initializing memory optimizer", ex);
            }
            
            // Start memory-aware continuous service if enabled
            try {
                MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
                if (settings != null && settings.isMemoryAwareContinuousServiceEnabled()) {
                    final MemoryManagementSettings finalSettings = settings;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (project.isDisposed()) {
                            LOG.info("Project disposed, skipping memory-aware continuous service initialization");
                            return;
                        }
                        
                        try {
                            MemoryAwareContinuousService service = project.getService(MemoryAwareContinuousService.class);
                            if (service != null) {
                                service.start();
                                LOG.info("Memory-aware continuous service started with threshold=" +
                                        finalSettings.getMemoryThresholdPercent() + "%, interval=" +
                                        finalSettings.getCheckIntervalMs() + "ms");
                            } else {
                                LOG.warn("Memory-aware continuous service not available");
                            }
                        } catch (Exception ex) {
                            LOG.error("Error starting memory-aware continuous service", ex);
                        }
                    });
                } else {
                    LOG.info("Memory-aware continuous service is disabled or settings not available");
                }
            } catch (Exception ex) {
                LOG.error("Error checking memory-aware continuous service settings", ex);
            }
            
            // Log initial memory stats
            try {
                MemoryUtils.logMemoryStats();
            } catch (Exception ex) {
                LOG.error("Error logging initial memory stats", ex);
            }
            
            LOG.info("Memory management initialization completed for project: " + project.getName());
            
        } catch (Exception ex) {
            LOG.error("Unexpected error during memory management initialization", ex);
        }
    }
}