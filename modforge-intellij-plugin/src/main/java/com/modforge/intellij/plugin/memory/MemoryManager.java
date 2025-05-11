package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Memory management service for ModForge
 * Monitors and optimizes memory usage for long-running operations
 */
@Service
public final class MemoryManager {
    private static final Logger LOG = Logger.getInstance(MemoryManager.class);
    
    // Memory thresholds
    private static final double WARNING_THRESHOLD = 0.75; // 75% of max memory
    private static final double CRITICAL_THRESHOLD = 0.85; // 85% of max memory
    private static final double EMERGENCY_THRESHOLD = 0.95; // 95% of max memory
    
    // Historical data for trend analysis
    private static final int HISTORY_SIZE = 10;
    private final List<MemorySnapshot> memoryHistory = Collections.synchronizedList(new ArrayList<>(HISTORY_SIZE));
    
    // Runtime information
    private final Runtime runtime = Runtime.getRuntime();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    
    // Scheduling
    private final ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
    private ScheduledFuture<?> monitorTask;
    private final AtomicBoolean emergencyMode = new AtomicBoolean(false);
    
    // Listeners
    private final List<MemoryListener> listeners = Collections.synchronizedList(new ArrayList<>());
    
    public static MemoryManager getInstance() {
        return ApplicationManager.getApplication().getService(MemoryManager.class);
    }
    
    /**
     * Initialize memory monitoring
     */
    public void initialize() {
        // Schedule periodic memory checks
        monitorTask = executor.scheduleWithFixedDelay(
            this::checkMemory, 
            5, 
            30, 
            TimeUnit.SECONDS
        );
        
        LOG.info("Memory manager initialized");
    }
    
    /**
     * Register a memory listener
     * 
     * @param listener The listener to register
     */
    public void addListener(@NotNull MemoryListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Unregister a memory listener
     * 
     * @param listener The listener to unregister
     */
    public void removeListener(@NotNull MemoryListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Check memory usage and take appropriate actions
     */
    private void checkMemory() {
        try {
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            
            long used = heapUsage.getUsed();
            long max = heapUsage.getMax();
            double usageRatio = (double) used / max;
            
            // Create a snapshot and add to history
            MemorySnapshot snapshot = new MemorySnapshot(
                System.currentTimeMillis(),
                heapUsage.getInit(),
                used,
                heapUsage.getCommitted(),
                max,
                nonHeapUsage.getUsed(),
                nonHeapUsage.getMax()
            );
            
            addToHistory(snapshot);
            
            // Check for memory pressure
            if (usageRatio >= EMERGENCY_THRESHOLD) {
                handleEmergencyPressure(snapshot);
            } else if (usageRatio >= CRITICAL_THRESHOLD) {
                handleCriticalPressure(snapshot);
            } else if (usageRatio >= WARNING_THRESHOLD) {
                handleWarningPressure(snapshot);
            } else {
                // Memory usage is normal
                if (emergencyMode.compareAndSet(true, false)) {
                    notifyNormalMemory(snapshot);
                }
            }
            
            // Check memory trend
            analyzeMemoryTrend();
            
        } catch (Exception e) {
            LOG.error("Error checking memory", e);
        }
    }
    
    /**
     * Add a memory snapshot to the history
     * 
     * @param snapshot The snapshot to add
     */
    private void addToHistory(MemorySnapshot snapshot) {
        synchronized (memoryHistory) {
            if (memoryHistory.size() >= HISTORY_SIZE) {
                memoryHistory.remove(0);
            }
            memoryHistory.add(snapshot);
        }
    }
    
    /**
     * Handle warning level memory pressure
     * 
     * @param snapshot The current memory snapshot
     */
    private void handleWarningPressure(MemorySnapshot snapshot) {
        LOG.info("Warning memory pressure: " + snapshot.getUsedHeapMB() + "MB / " + 
                 snapshot.getMaxHeapMB() + "MB (" + 
                 snapshot.getUsagePercentage() + "%)");
        
        // Notify listeners
        for (MemoryListener listener : listeners) {
            try {
                listener.onWarningMemoryPressure(snapshot);
            } catch (Exception e) {
                LOG.error("Error notifying memory listener", e);
            }
        }
        
        // Run a lightweight GC
        System.gc();
    }
    
    /**
     * Handle critical level memory pressure
     * 
     * @param snapshot The current memory snapshot
     */
    private void handleCriticalPressure(MemorySnapshot snapshot) {
        LOG.warn("Critical memory pressure: " + snapshot.getUsedHeapMB() + "MB / " + 
                 snapshot.getMaxHeapMB() + "MB (" + 
                 snapshot.getUsagePercentage() + "%)");
        
        // Notify listeners
        for (MemoryListener listener : listeners) {
            try {
                listener.onCriticalMemoryPressure(snapshot);
            } catch (Exception e) {
                LOG.error("Error notifying memory listener", e);
            }
        }
        
        // Perform aggressive cleanup
        clearCachesAndBuffers();
        System.gc();
    }
    
    /**
     * Handle emergency level memory pressure
     * 
     * @param snapshot The current memory snapshot
     */
    private void handleEmergencyPressure(MemorySnapshot snapshot) {
        if (emergencyMode.compareAndSet(false, true)) {
            LOG.error("Emergency memory pressure: " + snapshot.getUsedHeapMB() + "MB / " + 
                     snapshot.getMaxHeapMB() + "MB (" + 
                     snapshot.getUsagePercentage() + "%)");
            
            // Notify listeners
            for (MemoryListener listener : listeners) {
                try {
                    listener.onEmergencyMemoryPressure(snapshot);
                } catch (Exception e) {
                    LOG.error("Error notifying memory listener", e);
                }
            }
        }
        
        // Take emergency actions
        clearCachesAndBuffers();
        cancelNonEssentialTasks();
        System.gc();
    }
    
    /**
     * Notify listeners of normal memory conditions
     * 
     * @param snapshot The current memory snapshot
     */
    private void notifyNormalMemory(MemorySnapshot snapshot) {
        LOG.info("Memory pressure returned to normal: " + snapshot.getUsedHeapMB() + "MB / " + 
                 snapshot.getMaxHeapMB() + "MB (" + 
                 snapshot.getUsagePercentage() + "%)");
        
        // Notify listeners
        for (MemoryListener listener : listeners) {
            try {
                listener.onNormalMemory(snapshot);
            } catch (Exception e) {
                LOG.error("Error notifying memory listener", e);
            }
        }
    }
    
    /**
     * Analyze memory usage trend and predict future issues
     */
    private void analyzeMemoryTrend() {
        if (memoryHistory.size() < 3) {
            return; // Need at least 3 data points for trend analysis
        }
        
        synchronized (memoryHistory) {
            // Calculate slope of memory usage trend
            long timeSpan = memoryHistory.get(memoryHistory.size() - 1).getTimestamp() - 
                           memoryHistory.get(0).getTimestamp();
            
            if (timeSpan == 0) {
                return; // Avoid division by zero
            }
            
            long memoryDelta = memoryHistory.get(memoryHistory.size() - 1).getUsedHeap() - 
                              memoryHistory.get(0).getUsedHeap();
            
            double slope = (double) memoryDelta / timeSpan;
            
            if (slope > 0) {
                // Memory usage is increasing
                MemorySnapshot latest = memoryHistory.get(memoryHistory.size() - 1);
                long remainingMemory = latest.getMaxHeap() - latest.getUsedHeap();
                
                if (remainingMemory <= 0) {
                    return; // Already at max
                }
                
                // Calculate estimated time to reach critical threshold
                double timeToReachCritical = remainingMemory * (1.0 - CRITICAL_THRESHOLD) / slope;
                
                if (timeToReachCritical < 5 * 60 * 1000) { // less than 5 minutes
                    LOG.warn("Memory trend analysis predicts critical pressure in " + 
                             (timeToReachCritical / 1000 / 60) + " minutes");
                    
                    // Take preventative action
                    clearCachesAndBuffers();
                }
            }
        }
    }
    
    /**
     * Clear caches and buffers to free memory
     */
    private void clearCachesAndBuffers() {
        // This is simplified - in a real implementation, it would:
        // 1. Clear document caches
        // 2. Clear analysis caches
        // 3. Clear image caches
        // 4. Ask each component to reduce its memory footprint
        
        LOG.info("Clearing caches and buffers");
    }
    
    /**
     * Cancel non-essential background tasks
     */
    private void cancelNonEssentialTasks() {
        // This is simplified - in a real implementation, it would:
        // 1. Cancel background indexing
        // 2. Cancel background analysis
        // 3. Pause automated features temporarily
        
        LOG.info("Cancelling non-essential tasks");
    }
    
    /**
     * Get the current memory snapshot
     * 
     * @return Current memory snapshot
     */
    public MemorySnapshot getCurrentSnapshot() {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        return new MemorySnapshot(
            System.currentTimeMillis(),
            heapUsage.getInit(),
            heapUsage.getUsed(),
            heapUsage.getCommitted(),
            heapUsage.getMax(),
            nonHeapUsage.getUsed(),
            nonHeapUsage.getMax()
        );
    }
    
    /**
     * Get historical memory snapshots
     * 
     * @return List of memory snapshots
     */
    public List<MemorySnapshot> getMemoryHistory() {
        synchronized (memoryHistory) {
            return new ArrayList<>(memoryHistory);
        }
    }
    
    /**
     * Dispose the memory manager
     */
    public void dispose() {
        if (monitorTask != null) {
            monitorTask.cancel(false);
            monitorTask = null;
        }
        
        listeners.clear();
        memoryHistory.clear();
        
        LOG.info("Memory manager disposed");
    }
}