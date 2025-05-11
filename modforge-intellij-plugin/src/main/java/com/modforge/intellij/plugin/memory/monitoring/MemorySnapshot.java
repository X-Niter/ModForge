package com.modforge.intellij.plugin.memory.monitoring;

import com.modforge.intellij.plugin.memory.MemoryUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a snapshot of memory usage at a specific point in time
 * Used for memory trend analysis and health monitoring
 */
public class MemorySnapshot {
    private final long timestamp;
    private final long totalMemory;
    private final long freeMemory;
    private final long maxMemory;
    private final long usedMemory;
    private final double usagePercentage;
    private final long heapUsed;
    private final long nonHeapUsed;
    private final List<PoolData> poolDataList = new ArrayList<>();
    
    /**
     * Constructor
     * 
     * @param timestamp The timestamp of the snapshot
     * @param totalMemory The total memory
     * @param freeMemory The free memory
     * @param maxMemory The max memory
     * @param usedMemory The used memory
     * @param usagePercentage The usage percentage
     * @param heapUsed The heap used
     * @param nonHeapUsed The non-heap used
     */
    public MemorySnapshot(
            long timestamp,
            long totalMemory,
            long freeMemory,
            long maxMemory,
            long usedMemory,
            double usagePercentage,
            long heapUsed,
            long nonHeapUsed
    ) {
        this.timestamp = timestamp;
        this.totalMemory = totalMemory;
        this.freeMemory = freeMemory;
        this.maxMemory = maxMemory;
        this.usedMemory = usedMemory;
        this.usagePercentage = usagePercentage;
        this.heapUsed = heapUsed;
        this.nonHeapUsed = nonHeapUsed;
    }
    
    /**
     * Add pool usage data to the snapshot
     * 
     * @param name The pool name
     * @param type The pool type
     * @param used The used memory
     * @param committed The committed memory
     * @param max The max memory
     */
    public void addPoolUsage(String name, String type, long used, long committed, long max) {
        poolDataList.add(new PoolData(name, type, used, committed, max));
    }
    
    /**
     * Get the timestamp
     * 
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the timestamp as a date
     * 
     * @return The timestamp as a date
     */
    public Date getTimestampAsDate() {
        return new Date(timestamp);
    }
    
    /**
     * Get the total memory
     * 
     * @return The total memory
     */
    public long getTotalMemory() {
        return totalMemory;
    }
    
    /**
     * Get the formatted total memory
     * 
     * @return The formatted total memory
     */
    public String getFormattedTotalMemory() {
        return MemoryUtils.formatMemorySize(totalMemory);
    }
    
    /**
     * Get the free memory
     * 
     * @return The free memory
     */
    public long getFreeMemory() {
        return freeMemory;
    }
    
    /**
     * Get the formatted free memory
     * 
     * @return The formatted free memory
     */
    public String getFormattedFreeMemory() {
        return MemoryUtils.formatMemorySize(freeMemory);
    }
    
    /**
     * Get the max memory
     * 
     * @return The max memory
     */
    public long getMaxMemory() {
        return maxMemory;
    }
    
    /**
     * Get the formatted max memory
     * 
     * @return The formatted max memory
     */
    public String getFormattedMaxMemory() {
        return MemoryUtils.formatMemorySize(maxMemory);
    }
    
    /**
     * Get the used memory
     * 
     * @return The used memory
     */
    public long getUsedMemory() {
        return usedMemory;
    }
    
    /**
     * Get the formatted used memory
     * 
     * @return The formatted used memory
     */
    public String getFormattedUsedMemory() {
        return MemoryUtils.formatMemorySize(usedMemory);
    }
    
    /**
     * Get the usage percentage
     * 
     * @return The usage percentage
     */
    public double getUsagePercentage() {
        return usagePercentage;
    }
    
    /**
     * Get the formatted usage percentage
     * 
     * @return The formatted usage percentage
     */
    public String getFormattedUsagePercentage() {
        return String.format("%.2f%%", usagePercentage);
    }
    
    /**
     * Get the heap used
     * 
     * @return The heap used
     */
    public long getHeapUsed() {
        return heapUsed;
    }
    
    /**
     * Get the formatted heap used
     * 
     * @return The formatted heap used
     */
    public String getFormattedHeapUsed() {
        return MemoryUtils.formatMemorySize(heapUsed);
    }
    
    /**
     * Get the non-heap used
     * 
     * @return The non-heap used
     */
    public long getNonHeapUsed() {
        return nonHeapUsed;
    }
    
    /**
     * Get the formatted non-heap used
     * 
     * @return The formatted non-heap used
     */
    public String getFormattedNonHeapUsed() {
        return MemoryUtils.formatMemorySize(nonHeapUsed);
    }
    
    /**
     * Get the pool data list
     * 
     * @return The pool data list
     */
    public List<PoolData> getPoolData() {
        return new ArrayList<>(poolDataList);
    }
    
    /**
     * Get a map of pool data by name
     * 
     * @return The pool data map
     */
    public Map<String, PoolData> getPoolDataMap() {
        Map<String, PoolData> map = new HashMap<>();
        for (PoolData poolData : poolDataList) {
            map.put(poolData.getName(), poolData);
        }
        return map;
    }
    
    /**
     * Get a string representation of the snapshot
     * 
     * @return The string representation
     */
    @Override
    public String toString() {
        return "MemorySnapshot{" +
                "timestamp=" + new Date(timestamp) +
                ", total=" + getFormattedTotalMemory() +
                ", used=" + getFormattedUsedMemory() +
                ", usage=" + getFormattedUsagePercentage() +
                ", pools=" + poolDataList.size() +
                '}';
    }
    
    /**
     * Represents data for a single memory pool
     */
    public static class PoolData {
        private final String name;
        private final String type;
        private final long used;
        private final long committed;
        private final long max;
        
        /**
         * Constructor
         * 
         * @param name The pool name
         * @param type The pool type
         * @param used The used memory
         * @param committed The committed memory
         * @param max The max memory
         */
        public PoolData(String name, String type, long used, long committed, long max) {
            this.name = name;
            this.type = type;
            this.used = used;
            this.committed = committed;
            this.max = max;
        }
        
        /**
         * Get the pool name
         * 
         * @return The pool name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get the pool type
         * 
         * @return The pool type
         */
        public String getType() {
            return type;
        }
        
        /**
         * Get the used memory
         * 
         * @return The used memory
         */
        public long getUsed() {
            return used;
        }
        
        /**
         * Get the formatted used memory
         * 
         * @return The formatted used memory
         */
        public String getFormattedUsed() {
            return MemoryUtils.formatMemorySize(used);
        }
        
        /**
         * Get the committed memory
         * 
         * @return The committed memory
         */
        public long getCommitted() {
            return committed;
        }
        
        /**
         * Get the formatted committed memory
         * 
         * @return The formatted committed memory
         */
        public String getFormattedCommitted() {
            return MemoryUtils.formatMemorySize(committed);
        }
        
        /**
         * Get the max memory
         * 
         * @return The max memory
         */
        public long getMax() {
            return max;
        }
        
        /**
         * Get the formatted max memory
         * 
         * @return The formatted max memory
         */
        public String getFormattedMax() {
            return max < 0 ? "undefined" : MemoryUtils.formatMemorySize(max);
        }
        
        /**
         * Get the usage percentage
         * 
         * @return The usage percentage
         */
        public double getUsagePercentage() {
            if (max <= 0) {
                return 0;
            }
            return (double) used / max * 100;
        }
        
        /**
         * Get the formatted usage percentage
         * 
         * @return The formatted usage percentage
         */
        public String getFormattedUsagePercentage() {
            if (max <= 0) {
                return "N/A";
            }
            return String.format("%.2f%%", getUsagePercentage());
        }
        
        /**
         * Get a string representation of the pool data
         * 
         * @return The string representation
         */
        @Override
        public String toString() {
            return "PoolData{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", used=" + getFormattedUsed() +
                    ", committed=" + getFormattedCommitted() +
                    ", max=" + getFormattedMax() +
                    ", usage=" + getFormattedUsagePercentage() +
                    '}';
        }
    }
}