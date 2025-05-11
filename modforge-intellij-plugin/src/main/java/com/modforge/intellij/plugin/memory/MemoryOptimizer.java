package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Service for optimizing memory usage during long-running operations
 * Helps prevent out-of-memory errors during continuous development
 */
@Service
public final class MemoryOptimizer implements MemoryListener {
    private static final Logger LOG = Logger.getInstance(MemoryOptimizer.class);
    
    private final Project project;
    private final AtomicBoolean optimizationInProgress = new AtomicBoolean(false);
    private final Executor executor = AppExecutorUtil.getAppExecutorService();
    
    // State flags
    private final AtomicBoolean warningState = new AtomicBoolean(false);
    private final AtomicBoolean criticalState = new AtomicBoolean(false);
    private final AtomicBoolean emergencyState = new AtomicBoolean(false);
    
    public MemoryOptimizer(Project project) {
        this.project = project;
        
        // Register as memory listener
        MemoryManager.getInstance().addListener(this);
        
        LOG.info("Memory optimizer initialized for project " + project.getName());
    }
    
    /**
     * Run a task with memory optimization
     * 
     * @param taskName The name of the task (for logging)
     * @param task The task to run
     * @param <T> The return type of the task
     * @return A CompletableFuture that will complete with the result of the task
     */
    public <T> CompletableFuture<T> runWithOptimization(@NotNull String taskName, @NotNull Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            LOG.info("Starting optimized task: " + taskName);
            
            // Check memory before starting
            MemorySnapshot snapshot = MemoryManager.getInstance().getCurrentSnapshot();
            double usagePercentage = snapshot.getUsagePercentage();
            
            // Perform pre-emptive optimization if memory usage is already high
            if (usagePercentage > 70.0) {
                LOG.info("Pre-emptive memory optimization before task " + taskName + 
                        " (current usage: " + usagePercentage + "%)");
                performOptimization(OptimizationLevel.NORMAL);
            }
            
            // Run the task
            T result = task.get();
            
            // Perform cleanup afterwards
            ApplicationManager.getApplication().invokeLater(() -> {
                performOptimization(OptimizationLevel.LIGHT);
            });
            
            LOG.info("Completed optimized task: " + taskName);
            return result;
        }, executor);
    }
    
    /**
     * Perform memory optimization
     * 
     * @param level The optimization level
     */
    public void performOptimization(@NotNull OptimizationLevel level) {
        // Avoid multiple concurrent optimizations
        if (!optimizationInProgress.compareAndSet(false, true)) {
            LOG.info("Optimization already in progress, skipping");
            return;
        }
        
        try {
            LOG.info("Performing memory optimization at level: " + level);
            
            switch (level) {
                case LIGHT:
                    // Light optimization - just clear small caches
                    cleanSmallCaches();
                    break;
                    
                case NORMAL:
                    // Normal optimization - clear all caches and run GC
                    cleanSmallCaches();
                    cleanLargeCaches();
                    System.gc();
                    break;
                    
                case AGGRESSIVE:
                    // Aggressive optimization - clear everything and run GC multiple times
                    cleanSmallCaches();
                    cleanLargeCaches();
                    cleanTemporaryFiles();
                    System.gc();
                    System.gc();
                    break;
                    
                case EMERGENCY:
                    // Emergency optimization - last resort before OOM
                    cleanSmallCaches();
                    cleanLargeCaches();
                    cleanTemporaryFiles();
                    shutdownNonEssentialServices();
                    System.gc();
                    System.gc();
                    System.gc();
                    break;
            }
            
            // Log memory after optimization
            MemorySnapshot snapshot = MemoryManager.getInstance().getCurrentSnapshot();
            LOG.info("Memory after optimization: " + snapshot.getUsedHeapMB() + "MB / " + 
                    snapshot.getMaxHeapMB() + "MB (" + snapshot.getUsagePercentage() + "%)");
            
        } catch (Exception e) {
            LOG.error("Error during memory optimization", e);
        } finally {
            optimizationInProgress.set(false);
        }
    }
    
    /**
     * Clean small caches (fast and low impact)
     */
    private void cleanSmallCaches() {
        // In a real implementation, this would clear:
        // - Small in-memory caches
        // - Recently used files lists
        // - Undo history beyond a certain point
        LOG.info("Cleaning small caches");
    }
    
    /**
     * Clean large caches (slower but more effective)
     */
    private void cleanLargeCaches() {
        // In a real implementation, this would clear:
        // - Image caches
        // - Code analysis results
        // - Search index caches
        LOG.info("Cleaning large caches");
    }
    
    /**
     * Clean temporary files
     */
    private void cleanTemporaryFiles() {
        // In a real implementation, this would:
        // - Delete temporary compilation outputs
        // - Clear log files
        // - Remove temporary downloads
        LOG.info("Cleaning temporary files");
    }
    
    /**
     * Shut down non-essential services
     */
    private void shutdownNonEssentialServices() {
        // In a real implementation, this would:
        // - Disable background indexing
        // - Disable code analysis
        // - Pause synchronization tasks
        // - Disable auto-save
        LOG.info("Shutting down non-essential services");
    }
    
    @Override
    public void onWarningMemoryPressure(MemorySnapshot snapshot) {
        // If this is a new warning (not already in warning state)
        if (warningState.compareAndSet(false, true)) {
            LOG.warn("Memory warning detected, performing normal optimization");
            performOptimization(OptimizationLevel.NORMAL);
        }
    }
    
    @Override
    public void onCriticalMemoryPressure(MemorySnapshot snapshot) {
        // Always respond to critical pressure
        criticalState.set(true);
        warningState.set(true);
        
        LOG.warn("Critical memory pressure detected, performing aggressive optimization");
        performOptimization(OptimizationLevel.AGGRESSIVE);
    }
    
    @Override
    public void onEmergencyMemoryPressure(MemorySnapshot snapshot) {
        // Always respond to emergency pressure
        emergencyState.set(true);
        criticalState.set(true);
        warningState.set(true);
        
        LOG.error("Emergency memory pressure detected, performing emergency optimization");
        performOptimization(OptimizationLevel.EMERGENCY);
    }
    
    @Override
    public void onNormalMemory(MemorySnapshot snapshot) {
        // Reset state flags
        boolean wasInHighPressure = warningState.getAndSet(false) || 
                                  criticalState.getAndSet(false) || 
                                  emergencyState.getAndSet(false);
        
        if (wasInHighPressure) {
            LOG.info("Memory pressure returned to normal");
        }
    }
    
    /**
     * Dispose the memory optimizer
     */
    public void dispose() {
        MemoryManager.getInstance().removeListener(this);
        LOG.info("Memory optimizer disposed for project " + project.getName());
    }
    
    /**
     * Levels of memory optimization
     */
    public enum OptimizationLevel {
        LIGHT,     // Light optimization (fast, minimal impact)
        NORMAL,    // Normal optimization (balanced)
        AGGRESSIVE, // Aggressive optimization (thorough)
        EMERGENCY  // Emergency optimization (last resort)
    }
}