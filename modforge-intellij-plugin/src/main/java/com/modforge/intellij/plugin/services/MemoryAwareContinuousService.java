package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryListener;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemorySnapshot;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Memory-aware implementation of the continuous service
 * Adapts the continuous code generation and improvement to current memory conditions
 */
@Service
public final class MemoryAwareContinuousService implements MemoryListener {
    private static final Logger LOG = Logger.getInstance(MemoryAwareContinuousService.class);
    
    private final Project project;
    private final ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
    
    private ScheduledFuture<?> continuousTask;
    private final AtomicBoolean taskRunning = new AtomicBoolean(false);
    private final AtomicBoolean memoryPressure = new AtomicBoolean(false);
    
    // Dynamic interval management
    private int currentIntervalMinutes = 5;
    private static final int DEFAULT_INTERVAL_MINUTES = 5;
    private static final int REDUCED_INTERVAL_MINUTES = 15;
    private static final int MINIMUM_INTERVAL_MINUTES = 30;
    
    public MemoryAwareContinuousService(Project project) {
        this.project = project;
        
        // Register as memory listener
        MemoryManager.getInstance().addListener(this);
        
        LOG.info("Memory-aware continuous service initialized");
    }
    
    /**
     * Start the continuous service
     */
    public void start() {
        if (continuousTask != null && !continuousTask.isDone()) {
            LOG.info("Continuous service already started");
            return;
        }
        
        LOG.info("Starting memory-aware continuous service");
        
        // Calculate initial interval based on available memory
        adjustInterval();
        
        // Schedule the continuous task
        continuousTask = executor.scheduleWithFixedDelay(
            this::performContinuousTask,
            currentIntervalMinutes,
            currentIntervalMinutes,
            TimeUnit.MINUTES
        );
    }
    
    /**
     * Stop the continuous service
     */
    public void stop() {
        if (continuousTask != null) {
            LOG.info("Stopping memory-aware continuous service");
            continuousTask.cancel(false);
            continuousTask = null;
        }
    }
    
    /**
     * Check if the continuous service is running
     * 
     * @return True if the service is running
     */
    public boolean isRunning() {
        return continuousTask != null && !continuousTask.isDone();
    }
    
    /**
     * Perform a task with memory awareness
     * 
     * @param taskName The name of the task (for logging)
     * @param task The task to run
     * @param <T> The return type of the task
     * @return A CompletableFuture that will complete with the result of the task
     */
    public <T> CompletableFuture<T> runTask(@NotNull String taskName, @NotNull Supplier<T> task) {
        // Check if we can run the task
        if (memoryPressure.get()) {
            LOG.warn("Task " + taskName + " postponed due to memory pressure");
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Wait for memory pressure to subside
                    while (memoryPressure.get()) {
                        Thread.sleep(1000);
                    }
                    
                    // Run with memory optimization
                    return MemoryUtils.runIntensiveTask(project, taskName, task).get();
                } catch (Exception e) {
                    LOG.error("Error running postponed task: " + taskName, e);
                    throw new RuntimeException(e);
                }
            });
        } else {
            // Run normally with memory optimization
            return MemoryUtils.runIntensiveTask(project, taskName, task);
        }
    }
    
    /**
     * Adjust the task interval based on memory conditions
     */
    private void adjustInterval() {
        double memoryUsage = MemoryUtils.getMemoryUsagePercentage();
        int oldInterval = currentIntervalMinutes;
        
        if (memoryUsage >= 85.0) {
            // Critical memory pressure
            currentIntervalMinutes = MINIMUM_INTERVAL_MINUTES;
        } else if (memoryUsage >= 75.0) {
            // Elevated memory pressure
            currentIntervalMinutes = REDUCED_INTERVAL_MINUTES;
        } else {
            // Normal memory conditions
            currentIntervalMinutes = DEFAULT_INTERVAL_MINUTES;
        }
        
        if (oldInterval != currentIntervalMinutes && continuousTask != null) {
            LOG.info("Adjusting continuous task interval from " + oldInterval + 
                    " to " + currentIntervalMinutes + " minutes due to memory usage: " + 
                    memoryUsage + "%");
            
            // Reschedule the task with the new interval
            continuousTask.cancel(false);
            continuousTask = executor.scheduleWithFixedDelay(
                this::performContinuousTask,
                currentIntervalMinutes,
                currentIntervalMinutes,
                TimeUnit.MINUTES
            );
        }
    }
    
    /**
     * Perform the continuous task
     */
    private void performContinuousTask() {
        if (taskRunning.compareAndSet(false, true)) {
            try {
                // Check memory before starting
                if (MemoryUtils.isMemoryCritical()) {
                    LOG.warn("Skipping continuous task due to critical memory usage");
                    MemoryUtils.logMemoryStats();
                    return;
                }
                
                LOG.info("Running continuous development task");
                MemoryUtils.logMemoryStats();
                
                // Perform pre-task memory optimization
                MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.NORMAL);
                
                // In a real implementation, this would:
                // 1. Check for compile errors
                // 2. Look for TODOs or improvement opportunities
                // 3. Generate code or fix issues
                // 4. Update documentation
                simulateContinuousTask();
                
                // Perform post-task memory optimization
                MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.NORMAL);
                
                LOG.info("Continuous development task completed");
                MemoryUtils.logMemoryStats();
                
            } catch (Exception e) {
                LOG.error("Error in continuous task", e);
            } finally {
                taskRunning.set(false);
            }
        } else {
            LOG.info("Skipping continuous task as previous task is still running");
        }
    }
    
    /**
     * Simulate a continuous development task
     * This is just a placeholder - in a real implementation, this would do actual work
     */
    private void simulateContinuousTask() {
        try {
            // Simulate work being done
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void onWarningMemoryPressure(MemorySnapshot snapshot) {
        // If not already in memory pressure state
        if (memoryPressure.compareAndSet(false, true)) {
            LOG.warn("Memory pressure detected, adjusting continuous service");
            adjustInterval();
        }
    }
    
    @Override
    public void onCriticalMemoryPressure(MemorySnapshot snapshot) {
        // Always update state for critical pressure
        memoryPressure.set(true);
        LOG.warn("Critical memory pressure detected, pausing continuous service");
        
        // Force stop any running task
        if (taskRunning.get()) {
            LOG.warn("Stopping running continuous task due to critical memory pressure");
            // In a real implementation, this would have a way to interrupt the task
        }
        
        adjustInterval();
    }
    
    @Override
    public void onNormalMemory(MemorySnapshot snapshot) {
        // If coming out of memory pressure state
        if (memoryPressure.compareAndSet(true, false)) {
            LOG.info("Memory pressure resolved, resuming normal continuous service operation");
            adjustInterval();
        }
    }
    
    /**
     * Dispose the service
     */
    public void dispose() {
        stop();
        MemoryManager.getInstance().removeListener(this);
        LOG.info("Memory-aware continuous service disposed");
    }
}