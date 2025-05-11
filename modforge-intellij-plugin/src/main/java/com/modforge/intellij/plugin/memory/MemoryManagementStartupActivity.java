package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Startup activity to initialize the memory management system
 */
public class MemoryManagementStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(MemoryManagementStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing memory management system for project: " + project.getName());
        
        // Initialize the Memory Manager
        MemoryManager memoryManager = ApplicationManager.getApplication().getService(MemoryManager.class);
        memoryManager.initialize();
        
        // Get the Memory Optimizer
        MemoryOptimizer memoryOptimizer = project.getService(MemoryOptimizer.class);
        
        // Log initial memory state
        MemorySnapshot snapshot = memoryManager.getCurrentSnapshot();
        LOG.info("Initial memory state: " + snapshot.getUsedHeapMB() + "MB / " + 
                snapshot.getMaxHeapMB() + "MB (" + snapshot.getUsagePercentage() + "%)");
        
        // Perform initial optimization to clear startup artifacts
        memoryOptimizer.performOptimization(MemoryOptimizer.OptimizationLevel.LIGHT);
    }
}