package com.modforge.intellij.plugin.memory.monitoring;

import java.time.LocalDateTime;

/**
 * Represents a snapshot of memory usage at a specific point in time.
 */
public class MemorySnapshot {
    private final LocalDateTime timestamp;
    private final long usedMemory;
    private final long freeMemory;
    private final long totalMemory;
    private final long maxMemory;
    private final String pressureLevel;
    
    /**
     * Constructor for creating a memory snapshot
     *
     * @param timestamp     The time when the snapshot was taken
     * @param usedMemory    The amount of used memory in bytes
     * @param freeMemory    The amount of free memory in bytes
     * @param totalMemory   The total memory allocated in bytes
     * @param maxMemory     The maximum amount of memory that can be allocated in bytes
     * @param pressureLevel The memory pressure level at the time of the snapshot
     */
    public MemorySnapshot(
            LocalDateTime timestamp,
            long usedMemory,
            long freeMemory,
            long totalMemory,
            long maxMemory,
            String pressureLevel
    ) {
        this.timestamp = timestamp;
        this.usedMemory = usedMemory;
        this.freeMemory = freeMemory;
        this.totalMemory = totalMemory;
        this.maxMemory = maxMemory;
        this.pressureLevel = pressureLevel;
    }
    
    /**
     * Create a memory snapshot with the current system memory state
     *
     * @param pressureLevel The current memory pressure level
     * @return A new memory snapshot
     */
    public static MemorySnapshot createCurrentSnapshot(String pressureLevel) {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemorySnapshot(
                LocalDateTime.now(),
                usedMemory,
                freeMemory,
                totalMemory,
                maxMemory,
                pressureLevel
        );
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public long getUsedMemory() {
        return usedMemory;
    }
    
    public long getFreeMemory() {
        return freeMemory;
    }
    
    public long getTotalMemory() {
        return totalMemory;
    }
    
    public long getMaxMemory() {
        return maxMemory;
    }
    
    public String getPressureLevel() {
        return pressureLevel;
    }
    
    /**
     * Get the percentage of memory used (used / total)
     *
     * @return The percentage of memory used
     */
    public double getMemoryUsagePercentage() {
        return (double) usedMemory / totalMemory * 100.0;
    }
    
    /**
     * Get the percentage of used memory 
     *
     * @return The percentage of used memory
     */
    public double getUsedMemoryPercent() {
        return (double) usedMemory / totalMemory * 100.0;
    }
    
    /**
     * Get the used memory in megabytes
     *
     * @return Used memory in MB
     */
    public double getUsedMemoryMB() {
        return usedMemory / (1024.0 * 1024.0);
    }
    
    /**
     * Get the available (free) memory in megabytes
     *
     * @return Available memory in MB
     */
    public double getAvailableMemoryMB() {
        return freeMemory / (1024.0 * 1024.0);
    }
    
    /**
     * Get the total memory in megabytes
     *
     * @return Total memory in MB
     */
    public double getTotalMemoryMB() {
        return totalMemory / (1024.0 * 1024.0);
    }
    
    /**
     * Get the percentage of memory allocation (total / max)
     *
     * @return The percentage of memory allocation
     */
    public double getMemoryAllocationPercentage() {
        return (double) totalMemory / maxMemory * 100.0;
    }
    
    /**
     * Convert the snapshot to a CSV line
     *
     * @return A CSV line representing the snapshot
     */
    public String toCsvLine() {
        return String.format("%s,%d,%d,%d,%d,%s,%.2f,%.2f",
                timestamp,
                usedMemory,
                freeMemory,
                totalMemory,
                maxMemory,
                pressureLevel,
                getMemoryUsagePercentage(),
                getMemoryAllocationPercentage()
        );
    }
    
    @Override
    public String toString() {
        return "MemorySnapshot{" +
                "timestamp=" + timestamp +
                ", usedMemory=" + usedMemory +
                ", freeMemory=" + freeMemory +
                ", totalMemory=" + totalMemory +
                ", maxMemory=" + maxMemory +
                ", pressureLevel='" + pressureLevel + '\'' +
                ", usagePercentage=" + String.format("%.2f%%", getMemoryUsagePercentage()) +
                ", allocationPercentage=" + String.format("%.2f%%", getMemoryAllocationPercentage()) +
                '}';
    }
}