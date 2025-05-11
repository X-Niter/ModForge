package com.modforge.intellij.plugin.memory;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.memory.MemoryOptimizer.OptimizationLevel;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Utility methods for memory management
 */
public class MemoryUtils {
    private static final Logger LOG = Logger.getInstance(MemoryUtils.class);
    private static final DecimalFormat MEMORY_FORMAT = new DecimalFormat("#,###.##");
    private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
    private static final List<MemoryPoolMXBean> MEMORY_POOL_BEANS = ManagementFactory.getMemoryPoolMXBeans();
    
    /**
     * Memory pressure levels based on memory usage
     */
    public enum MemoryPressureLevel {
        NORMAL,
        WARNING,
        CRITICAL,
        EMERGENCY
    }
    
    /**
     * Get the total memory in bytes
     * 
     * @return The total memory in bytes
     */
    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }
    
    /**
     * Get the maximum memory in bytes
     * 
     * @return The maximum memory in bytes
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }
    
    /**
     * Get the free memory in bytes
     * 
     * @return The free memory in bytes
     */
    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }
    
    /**
     * Get the used memory in bytes
     * 
     * @return The used memory in bytes
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getFreeMemory();
    }
    
    /**
     * Get the memory usage percentage
     * 
     * @return The memory usage percentage
     */
    public static double getMemoryUsagePercentage() {
        return ((double) getUsedMemory() / getMaxMemory()) * 100.0;
    }
    
    /**
     * Get the available memory in bytes
     * 
     * @return The available memory in bytes
     */
    public static long getAvailableMemory() {
        return getMaxMemory() - getUsedMemory();
    }
    
    /**
     * Format memory size for display
     * 
     * @param bytes The memory size in bytes
     * @return The formatted memory size
     */
    public static String formatMemorySize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return MEMORY_FORMAT.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return MEMORY_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return MEMORY_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
    
    /**
     * Get the current memory pressure level
     * 
     * @return The memory pressure level
     */
    public static MemoryPressureLevel getMemoryPressureLevel() {
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        double usagePercentage = getMemoryUsagePercentage();
        
        if (usagePercentage >= settings.getEmergencyThresholdPercent()) {
            return MemoryPressureLevel.EMERGENCY;
        } else if (usagePercentage >= settings.getCriticalThresholdPercent()) {
            return MemoryPressureLevel.CRITICAL;
        } else if (usagePercentage >= settings.getWarningThresholdPercent()) {
            return MemoryPressureLevel.WARNING;
        } else {
            return MemoryPressureLevel.NORMAL;
        }
    }
    
    /**
     * Log detailed memory statistics
     */
    public static void logMemoryStats() {
        MemoryUsage heapUsage = MEMORY_MX_BEAN.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = MEMORY_MX_BEAN.getNonHeapMemoryUsage();
        
        LOG.info("=== Memory Statistics ===");
        LOG.info("Total Memory: " + formatMemorySize(getTotalMemory()));
        LOG.info("Used Memory: " + formatMemorySize(getUsedMemory()) + 
                " (" + String.format("%.2f", getMemoryUsagePercentage()) + "%)");
        LOG.info("Free Memory: " + formatMemorySize(getFreeMemory()));
        LOG.info("Max Memory: " + formatMemorySize(getMaxMemory()));
        LOG.info("Available Memory: " + formatMemorySize(getAvailableMemory()));
        LOG.info("Heap Usage: " + formatMemorySize(heapUsage.getUsed()) + 
                " / " + formatMemorySize(heapUsage.getCommitted()));
        LOG.info("Non-Heap Usage: " + formatMemorySize(nonHeapUsage.getUsed()) + 
                " / " + formatMemorySize(nonHeapUsage.getCommitted()));
        LOG.info("Memory Pressure Level: " + getMemoryPressureLevel());
        
        LOG.info("Memory Pool Details:");
        for (MemoryPoolMXBean pool : MEMORY_POOL_BEANS) {
            MemoryUsage usage = pool.getUsage();
            LOG.info("  " + pool.getName() + " (Type: " + pool.getType() + "):");
            LOG.info("    Used: " + formatMemorySize(usage.getUsed()));
            LOG.info("    Committed: " + formatMemorySize(usage.getCommitted()));
            LOG.info("    Max: " + (usage.getMax() < 0 ? "undefined" : formatMemorySize(usage.getMax())));
        }
        
        LOG.info("=========================");
    }
    
    /**
     * Request a garbage collection
     * Note: This is just a request, the JVM may choose to ignore it
     */
    public static void requestGarbageCollection() {
        long beforeMemory = getUsedMemory();
        LOG.info("Requesting garbage collection. Before: " + formatMemorySize(beforeMemory));
        
        System.gc();
        
        long afterMemory = getUsedMemory();
        long freed = beforeMemory - afterMemory;
        
        LOG.info("Garbage collection completed. After: " + formatMemorySize(afterMemory) + 
                ", Freed: " + formatMemorySize(freed) + 
                " (" + String.format("%.2f", (freed * 100.0 / beforeMemory)) + "%)");
    }
    
    /**
     * Optimize memory using the MemoryOptimizer
     * 
     * @param project The project
     * @param level The optimization level
     */
    public static void optimizeMemory(Project project, OptimizationLevel level) {
        if (project == null) {
            LOG.warn("Cannot optimize memory: project is null");
            return;
        }
        
        MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
        if (optimizer == null) {
            LOG.warn("Cannot optimize memory: MemoryOptimizer service not available");
            return;
        }
        
        double beforeUsage = getMemoryUsagePercentage();
        LOG.info("Starting memory optimization at level " + level + 
                ". Before: " + String.format("%.2f", beforeUsage) + "%");
        
        optimizer.optimize(level);
        
        double afterUsage = getMemoryUsagePercentage();
        LOG.info("Memory optimization completed. After: " + String.format("%.2f", afterUsage) + 
                "%, Improvement: " + String.format("%.2f", beforeUsage - afterUsage) + "%");
    }
    
    /**
     * Check if memory optimization is needed based on the current memory pressure level
     * 
     * @return True if memory optimization is needed
     */
    public static boolean isMemoryOptimizationNeeded() {
        MemoryPressureLevel level = getMemoryPressureLevel();
        return level == MemoryPressureLevel.WARNING || 
               level == MemoryPressureLevel.CRITICAL || 
               level == MemoryPressureLevel.EMERGENCY;
    }
    
    /**
     * Get the appropriate optimization level based on the memory pressure level
     * 
     * @return The appropriate optimization level
     */
    public static OptimizationLevel getOptimizationLevelForCurrentPressure() {
        MemoryPressureLevel pressureLevel = getMemoryPressureLevel();
        
        switch (pressureLevel) {
            case EMERGENCY:
                return OptimizationLevel.AGGRESSIVE;
            case CRITICAL:
                return OptimizationLevel.NORMAL;
            case WARNING:
                return OptimizationLevel.CONSERVATIVE;
            default:
                return OptimizationLevel.MINIMAL;
        }
    }
    
    /**
     * Generate a memory status report
     * 
     * @return The memory status report
     */
    public static String generateMemoryStatusReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("Memory Status Report\n");
        report.append("-------------------\n");
        report.append("Total Memory: ").append(formatMemorySize(getTotalMemory())).append("\n");
        report.append("Used Memory: ").append(formatMemorySize(getUsedMemory()))
              .append(" (").append(String.format("%.2f", getMemoryUsagePercentage())).append("%)\n");
        report.append("Free Memory: ").append(formatMemorySize(getFreeMemory())).append("\n");
        report.append("Max Memory: ").append(formatMemorySize(getMaxMemory())).append("\n");
        report.append("Available Memory: ").append(formatMemorySize(getAvailableMemory())).append("\n");
        report.append("Memory Pressure Level: ").append(getMemoryPressureLevel()).append("\n");
        
        return report.toString();
    }
}