package com.modforge.intellij.plugin.memory;

/**
 * Class representing a snapshot of memory usage
 */
public class MemorySnapshot {
    private final long timestamp;
    private final long initHeap;
    private final long usedHeap;
    private final long committedHeap;
    private final long maxHeap;
    private final long usedNonHeap;
    private final long maxNonHeap;
    
    /**
     * Create a new memory snapshot
     * 
     * @param timestamp The timestamp of the snapshot
     * @param initHeap The initial heap size in bytes
     * @param usedHeap The used heap size in bytes
     * @param committedHeap The committed heap size in bytes
     * @param maxHeap The maximum heap size in bytes
     * @param usedNonHeap The used non-heap memory in bytes
     * @param maxNonHeap The maximum non-heap memory in bytes
     */
    public MemorySnapshot(long timestamp, long initHeap, long usedHeap, long committedHeap, 
                         long maxHeap, long usedNonHeap, long maxNonHeap) {
        this.timestamp = timestamp;
        this.initHeap = initHeap;
        this.usedHeap = usedHeap;
        this.committedHeap = committedHeap;
        this.maxHeap = maxHeap;
        this.usedNonHeap = usedNonHeap;
        this.maxNonHeap = maxNonHeap;
    }
    
    /**
     * Get the timestamp of the snapshot
     * 
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the initial heap size
     * 
     * @return The initial heap size in bytes
     */
    public long getInitHeap() {
        return initHeap;
    }
    
    /**
     * Get the used heap size
     * 
     * @return The used heap size in bytes
     */
    public long getUsedHeap() {
        return usedHeap;
    }
    
    /**
     * Get the committed heap size
     * 
     * @return The committed heap size in bytes
     */
    public long getCommittedHeap() {
        return committedHeap;
    }
    
    /**
     * Get the maximum heap size
     * 
     * @return The maximum heap size in bytes
     */
    public long getMaxHeap() {
        return maxHeap;
    }
    
    /**
     * Get the used non-heap memory
     * 
     * @return The used non-heap memory in bytes
     */
    public long getUsedNonHeap() {
        return usedNonHeap;
    }
    
    /**
     * Get the maximum non-heap memory
     * 
     * @return The maximum non-heap memory in bytes
     */
    public long getMaxNonHeap() {
        return maxNonHeap;
    }
    
    /**
     * Get the usage percentage
     * 
     * @return The usage percentage
     */
    public double getUsagePercentage() {
        if (maxHeap <= 0) {
            return 0.0;
        }
        
        return (usedHeap * 100.0) / maxHeap;
    }
    
    /**
     * Get the used heap size in megabytes
     * 
     * @return The used heap size in MB
     */
    public double getUsedHeapMB() {
        return usedHeap / (1024.0 * 1024.0);
    }
    
    /**
     * Get the maximum heap size in megabytes
     * 
     * @return The maximum heap size in MB
     */
    public double getMaxHeapMB() {
        return maxHeap / (1024.0 * 1024.0);
    }
    
    /**
     * Get the used non-heap memory in megabytes
     * 
     * @return The used non-heap memory in MB
     */
    public double getUsedNonHeapMB() {
        return usedNonHeap / (1024.0 * 1024.0);
    }
    
    /**
     * Get the maximum non-heap memory in megabytes
     * 
     * @return The maximum non-heap memory in MB
     */
    public double getMaxNonHeapMB() {
        return maxNonHeap / (1024.0 * 1024.0);
    }
    
    @Override
    public String toString() {
        return String.format("MemorySnapshot[heap: %.1f/%.1f MB (%.1f%%), non-heap: %.1f/%.1f MB]",
                          getUsedHeapMB(), getMaxHeapMB(), getUsagePercentage(),
                          getUsedNonHeapMB(), getMaxNonHeapMB());
    }
}