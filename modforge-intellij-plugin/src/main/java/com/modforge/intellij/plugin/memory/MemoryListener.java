package com.modforge.intellij.plugin.memory;

/**
 * Interface for listening to memory events
 */
public interface MemoryListener {
    /**
     * Called when memory pressure returns to normal
     * 
     * @param snapshot The memory snapshot
     */
    default void onNormalMemory(MemorySnapshot snapshot) {
        // Default empty implementation
    }
    
    /**
     * Called when memory pressure reaches warning level
     * 
     * @param snapshot The memory snapshot
     */
    default void onWarningMemoryPressure(MemorySnapshot snapshot) {
        // Default empty implementation
    }
    
    /**
     * Called when memory pressure reaches critical level
     * 
     * @param snapshot The memory snapshot
     */
    default void onCriticalMemoryPressure(MemorySnapshot snapshot) {
        // Default empty implementation
    }
    
    /**
     * Called when memory pressure reaches emergency level
     * 
     * @param snapshot The memory snapshot
     */
    default void onEmergencyMemoryPressure(MemorySnapshot snapshot) {
        // Default empty implementation
    }
}