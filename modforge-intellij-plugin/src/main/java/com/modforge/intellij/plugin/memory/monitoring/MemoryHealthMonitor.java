package com.modforge.intellij.plugin.memory.monitoring;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Memory health monitor that tracks memory trends and predicts potential issues
 * This monitor can trigger preemptive actions before memory pressure becomes critical
 */
public class MemoryHealthMonitor implements Disposable {
    private static final Logger LOG = Logger.getInstance(MemoryHealthMonitor.class);
    
    private static final int MAX_HISTORY_SIZE = 60; // Keep an hour of data at 1-minute intervals
    private static final int PREDICTION_THRESHOLD_MINUTES = 10; // Predict issues 10 minutes in advance
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<MemorySnapshot> memoryHistory = new ArrayList<>();
    private final ScheduledExecutorService executor = AppExecutorUtil.createBoundedScheduledExecutorService(
            "ModForgeMemoryHealthMonitor", 1);
    private ScheduledFuture<?> monitoringTask;
    private final List<MemoryHealthListener> listeners = new ArrayList<>();
    
    /**
     * Memory health status
     */
    public enum MemoryHealthStatus {
        HEALTHY,
        DEGRADED,
        PROBLEMATIC,
        CRITICAL
    }
    
    /**
     * Get the singleton instance of MemoryHealthMonitor
     */
    public static MemoryHealthMonitor getInstance() {
        return ApplicationManager.getApplication().getService(MemoryHealthMonitor.class);
    }
    
    /**
     * Start the memory health monitor
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting memory health monitor");
            startMonitoring();
        }
    }
    
    /**
     * Stop the memory health monitor
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping memory health monitor");
            
            if (monitoringTask != null) {
                monitoringTask.cancel(false);
                monitoringTask = null;
            }
        }
    }
    
    /**
     * Start the monitoring task
     */
    private void startMonitoring() {
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
        }
        
        monitoringTask = executor.scheduleWithFixedDelay(
                this::monitorMemoryHealth,
                0,
                1,
                TimeUnit.MINUTES
        );
        
        LOG.info("Memory health monitoring started");
    }
    
    /**
     * Monitor memory health
     */
    private void monitorMemoryHealth() {
        try {
            // Take a snapshot of current memory state
            MemorySnapshot snapshot = takeMemorySnapshot();
            
            // Add to history
            synchronized (memoryHistory) {
                memoryHistory.add(snapshot);
                
                // Trim history if needed
                if (memoryHistory.size() > MAX_HISTORY_SIZE) {
                    memoryHistory.remove(0);
                }
            }
            
            // Analyze memory trends and predict issues
            if (memoryHistory.size() >= 5) { // Need at least 5 data points for trend analysis
                analyzeMemoryTrends();
            }
            
            // Check current health status
            MemoryHealthStatus status = calculateHealthStatus(snapshot);
            notifyHealthStatusChanged(status, snapshot);
            
        } catch (Exception e) {
            LOG.error("Error monitoring memory health", e);
        }
    }
    
    /**
     * Take a snapshot of current memory state
     * 
     * @return The memory snapshot
     */
    private MemorySnapshot takeMemorySnapshot() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        
        long totalMemory = MemoryUtils.getTotalMemory();
        long freeMemory = MemoryUtils.getFreeMemory();
        long maxMemory = MemoryUtils.getMaxMemory();
        long usedMemory = MemoryUtils.getUsedMemory();
        double usagePercentage = MemoryUtils.getMemoryUsagePercentage();
        
        MemorySnapshot snapshot = new MemorySnapshot(
                System.currentTimeMillis(), 
                totalMemory, 
                freeMemory, 
                maxMemory, 
                usedMemory, 
                usagePercentage,
                memoryMXBean.getHeapMemoryUsage().getUsed(),
                memoryMXBean.getNonHeapMemoryUsage().getUsed()
        );
        
        // Add memory pool data
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            snapshot.addPoolUsage(
                    pool.getName(),
                    pool.getType().toString(),
                    pool.getUsage().getUsed(),
                    pool.getUsage().getCommitted(),
                    pool.getUsage().getMax()
            );
        }
        
        return snapshot;
    }
    
    /**
     * Analyze memory trends and predict potential issues
     */
    private void analyzeMemoryTrends() {
        List<MemorySnapshot> history;
        synchronized (memoryHistory) {
            history = new ArrayList<>(memoryHistory);
        }
        
        int size = history.size();
        if (size < 5) {
            return; // Need at least 5 data points
        }
        
        // Calculate the slope of memory usage over time (simple linear regression)
        double sumX = 0;
        double sumY = 0;
        double sumXX = 0;
        double sumXY = 0;
        
        for (int i = 0; i < size; i++) {
            MemorySnapshot snapshot = history.get(i);
            double x = i; // use index as time (uniform intervals)
            double y = snapshot.getUsagePercentage();
            
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }
        
        double meanX = sumX / size;
        double meanY = sumY / size;
        
        // Calculate slope (m) of trend line
        double numerator = sumXY - size * meanX * meanY;
        double denominator = sumXX - size * meanX * meanX;
        
        if (Math.abs(denominator) < 1e-10) {
            // Avoid division by zero
            return;
        }
        
        double slope = numerator / denominator;
        double intercept = meanY - slope * meanX;
        
        // Predict future memory usage
        double currentUsage = history.get(size - 1).getUsagePercentage();
        double predictedUsageIn10Minutes = intercept + slope * (size + PREDICTION_THRESHOLD_MINUTES);
        
        LOG.debug("Memory trend analysis: current=" + String.format("%.2f", currentUsage) + 
                "%, predicted in " + PREDICTION_THRESHOLD_MINUTES + " minutes=" + 
                String.format("%.2f", predictedUsageIn10Minutes) + "%, slope=" + 
                String.format("%.4f", slope) + "% per minute");
        
        // Check if we're predicting a problem
        if (slope > 0.5 && predictedUsageIn10Minutes > 80) {
            // Memory is growing quickly and will reach a high level soon
            LOG.warn("Memory trend analysis predicts high memory usage (" + 
                    String.format("%.1f", predictedUsageIn10Minutes) + 
                    "%) in the next " + PREDICTION_THRESHOLD_MINUTES + " minutes");
            
            // Notify listeners
            notifyPredictedMemoryPressure(predictedUsageIn10Minutes, PREDICTION_THRESHOLD_MINUTES);
            
            // Take preemptive action if prediction is severe
            if (predictedUsageIn10Minutes > 90) {
                LOG.warn("Taking preemptive action due to predicted critical memory usage");
                triggerPreemptiveRecovery();
            }
        }
    }
    
    /**
     * Calculate the current health status
     * 
     * @param snapshot The current memory snapshot
     * @return The health status
     */
    private MemoryHealthStatus calculateHealthStatus(MemorySnapshot snapshot) {
        double usagePercentage = snapshot.getUsagePercentage();
        
        if (usagePercentage >= 90) {
            return MemoryHealthStatus.CRITICAL;
        } else if (usagePercentage >= 80) {
            return MemoryHealthStatus.PROBLEMATIC;
        } else if (usagePercentage >= 70) {
            return MemoryHealthStatus.DEGRADED;
        } else {
            return MemoryHealthStatus.HEALTHY;
        }
    }
    
    /**
     * Trigger preemptive recovery to prevent potential memory issues
     */
    private void triggerPreemptiveRecovery() {
        MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
        if (recoveryManager != null) {
            recoveryManager.initiateRecovery(MemoryRecoveryManager.RecoveryLevel.LEVEL1);
        }
    }
    
    /**
     * Add a memory health listener
     * 
     * @param listener The listener to add
     */
    public void addMemoryHealthListener(MemoryHealthListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a memory health listener
     * 
     * @param listener The listener to remove
     */
    public void removeMemoryHealthListener(MemoryHealthListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Get the current memory health status
     * 
     * @return The current memory health status
     */
    public MemoryHealthStatus getCurrentStatus() {
        MemorySnapshot snapshot = takeMemorySnapshot();
        return calculateHealthStatus(snapshot);
    }
    
    /**
     * Get memory snapshots for a specified time range
     * 
     * @param minutes The number of minutes of history to return
     * @return The memory snapshots
     */
    public List<MemorySnapshot> getMemoryHistory(int minutes) {
        synchronized (memoryHistory) {
            if (memoryHistory.isEmpty()) {
                return new ArrayList<>();
            }
            
            int count = Math.min(minutes, memoryHistory.size());
            return new ArrayList<>(memoryHistory.subList(memoryHistory.size() - count, memoryHistory.size()));
        }
    }
    
    /**
     * Notify listeners that a memory pressure is predicted
     * 
     * @param predictedUsagePercentage The predicted usage percentage
     * @param minutesAway How many minutes until the predicted pressure
     */
    private void notifyPredictedMemoryPressure(double predictedUsagePercentage, int minutesAway) {
        List<MemoryHealthListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (MemoryHealthListener listener : listenersCopy) {
            try {
                listener.onPredictedMemoryPressure(predictedUsagePercentage, minutesAway);
            } catch (Exception e) {
                LOG.warn("Error notifying memory health listener", e);
            }
        }
    }
    
    /**
     * Notify listeners that the health status has changed
     * 
     * @param status The new health status
     * @param snapshot The current memory snapshot
     */
    private void notifyHealthStatusChanged(MemoryHealthStatus status, MemorySnapshot snapshot) {
        List<MemoryHealthListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (MemoryHealthListener listener : listenersCopy) {
            try {
                listener.onMemoryHealthStatusChanged(status, snapshot);
            } catch (Exception e) {
                LOG.warn("Error notifying memory health listener", e);
            }
        }
    }
    
    @Override
    public void dispose() {
        stop();
        
        memoryHistory.clear();
        listeners.clear();
        
        if (executor != null) {
            executor.shutdownNow();
        }
    }
    
    /**
     * Interface for memory health listeners
     */
    public interface MemoryHealthListener {
        /**
         * Called when a memory pressure is predicted
         * 
         * @param predictedUsagePercentage The predicted usage percentage
         * @param minutesAway How many minutes until the predicted pressure
         */
        default void onPredictedMemoryPressure(double predictedUsagePercentage, int minutesAway) {}
        
        /**
         * Called when the memory health status changes
         * 
         * @param status The new health status
         * @param snapshot The current memory snapshot
         */
        default void onMemoryHealthStatusChanged(MemoryHealthStatus status, MemorySnapshot snapshot) {}
    }
}