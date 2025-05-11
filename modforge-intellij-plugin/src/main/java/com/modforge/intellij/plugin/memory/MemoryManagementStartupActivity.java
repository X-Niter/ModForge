package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
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
        LOG.info("Initializing memory management for project: " + project.getName());
        
        // Initialize memory manager
        MemoryManager memoryManager = MemoryManager.getInstance();
        if (memoryManager != null) {
            memoryManager.initialize();
            LOG.info("Memory manager initialized");
        } else {
            LOG.warn("Memory manager not available");
        }
        
        // Initialize memory optimizer
        MemoryOptimizer memoryOptimizer = project.getService(MemoryOptimizer.class);
        if (memoryOptimizer != null) {
            LOG.info("Memory optimizer initialized");
        } else {
            LOG.warn("Memory optimizer not available");
        }
        
        // Start memory-aware continuous service if enabled
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        if (settings.isMemoryAwareContinuousServiceEnabled()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                MemoryAwareContinuousService service = project.getService(MemoryAwareContinuousService.class);
                if (service != null) {
                    service.start();
                    LOG.info("Memory-aware continuous service started");
                } else {
                    LOG.warn("Memory-aware continuous service not available");
                }
            });
        }
        
        // Log initial memory stats
        MemoryUtils.logMemoryStats();
    }
}