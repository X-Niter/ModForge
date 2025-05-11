package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Utility class for memory management operations
 */
public final class MemoryUtils {
    private static final Logger LOG = Logger.getInstance(MemoryUtils.class);
    
    private MemoryUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Run an intensive task with memory optimization
     * 
     * @param project The project
     * @param taskName The name of the task (for logging)
     * @param task The task to run
     * @param <T> The return type of the task
     * @return A CompletableFuture that will complete with the result of the task
     */
    public static <T> CompletableFuture<T> runIntensiveTask(@NotNull Project project, 
                                                          @NotNull String taskName, 
                                                          @NotNull Supplier<T> task) {
        MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
        if (optimizer != null) {
            return optimizer.runWithOptimization(taskName, task);
        } else {
            LOG.warn("Memory optimizer not available, running task without optimization: " + taskName);
            return CompletableFuture.supplyAsync(task);
        }
    }
    
    /**
     * Optimize memory usage
     * 
     * @param project The project
     * @param level The optimization level
     */
    public static void optimizeMemory(@NotNull Project project, 
                                    @NotNull MemoryOptimizer.OptimizationLevel level) {
        MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
        if (optimizer != null) {
            optimizer.performOptimization(level);
        } else {
            LOG.warn("Memory optimizer not available, skipping optimization");
        }
    }
    
    /**
     * Get the current memory usage percentage
     * 
     * @return The memory usage percentage (0-100)
     */
    public static double getMemoryUsagePercentage() {
        MemoryManager manager = ApplicationManager.getApplication().getService(MemoryManager.class);
        if (manager != null) {
            return manager.getCurrentSnapshot().getUsagePercentage();
        } else {
            // Fallback to direct calculation
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            
            return (usedMemory * 100.0) / maxMemory;
        }
    }
    
    /**
     * Check if memory usage is critical
     * 
     * @return True if memory usage is critical
     */
    public static boolean isMemoryCritical() {
        return getMemoryUsagePercentage() >= 85.0;
    }
    
    /**
     * Log current memory stats
     */
    public static void logMemoryStats() {
        MemoryManager manager = ApplicationManager.getApplication().getService(MemoryManager.class);
        if (manager != null) {
            MemorySnapshot snapshot = manager.getCurrentSnapshot();
            LOG.info("Memory stats: " + snapshot.getUsedHeapMB() + "MB / " + 
                    snapshot.getMaxHeapMB() + "MB (" + snapshot.getUsagePercentage() + "%)");
        } else {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            double percentage = (usedMemory * 100.0) / maxMemory;
            
            LOG.info("Memory stats (direct): " + usedMemory + "MB / " + 
                    maxMemory + "MB (" + percentage + "%)");
        }
    }
}