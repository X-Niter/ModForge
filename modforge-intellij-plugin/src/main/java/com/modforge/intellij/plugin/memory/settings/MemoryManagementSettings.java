package com.modforge.intellij.plugin.memory.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for memory management
 */
@Service
@State(
    name = "com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings",
    storages = @Storage("ModForgeMemorySettings.xml")
)
public final class MemoryManagementSettings implements PersistentStateComponent<MemoryManagementSettings.State> {
    private State myState = new State();
    
    /**
     * Get the settings instance
     */
    public static MemoryManagementSettings getInstance() {
        return ApplicationManager.getApplication().getService(MemoryManagementSettings.class);
    }
    
    @Override
    public @Nullable State getState() {
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }
    
    /**
     * State class for memory management settings
     * Contains all persistent settings
     */
    public static class State {
        // General settings
        public boolean enableAutomaticOptimization = true;
        public boolean showMemoryWidget = true;
        
        // Thresholds
        public int warningThresholdPercent = 75;
        public int criticalThresholdPercent = 85;
        public int emergencyThresholdPercent = 95;
        
        // Automatic optimization
        public int optimizationIntervalMinutes = 30;
        public boolean optimizeOnLowMemory = true;
        public boolean optimizeBeforeLongRunningTasks = true;
        
        // Continuous development settings
        public boolean enableMemoryAwareContinuousService = true;
        public int continuousServiceDefaultIntervalMinutes = 5;
        public int continuousServiceReducedIntervalMinutes = 15;
        public int continuousServiceMinimumIntervalMinutes = 30;
    }
    
    /**
     * Check if automatic optimization is enabled
     * 
     * @return True if automatic optimization is enabled
     */
    public boolean isAutomaticOptimizationEnabled() {
        return myState.enableAutomaticOptimization;
    }
    
    /**
     * Set whether automatic optimization is enabled
     * 
     * @param enabled Whether automatic optimization is enabled
     */
    public void setAutomaticOptimizationEnabled(boolean enabled) {
        myState.enableAutomaticOptimization = enabled;
    }
    
    /**
     * Check if the memory widget should be shown
     * 
     * @return True if the memory widget should be shown
     */
    public boolean isShowMemoryWidget() {
        return myState.showMemoryWidget;
    }
    
    /**
     * Set whether the memory widget should be shown
     * 
     * @param show Whether the memory widget should be shown
     */
    public void setShowMemoryWidget(boolean show) {
        myState.showMemoryWidget = show;
    }
    
    /**
     * Get the warning threshold percentage
     * 
     * @return The warning threshold percentage
     */
    public int getWarningThresholdPercent() {
        return myState.warningThresholdPercent;
    }
    
    /**
     * Set the warning threshold percentage
     * 
     * @param percent The warning threshold percentage
     */
    public void setWarningThresholdPercent(int percent) {
        myState.warningThresholdPercent = percent;
    }
    
    /**
     * Get the critical threshold percentage
     * 
     * @return The critical threshold percentage
     */
    public int getCriticalThresholdPercent() {
        return myState.criticalThresholdPercent;
    }
    
    /**
     * Set the critical threshold percentage
     * 
     * @param percent The critical threshold percentage
     */
    public void setCriticalThresholdPercent(int percent) {
        myState.criticalThresholdPercent = percent;
    }
    
    /**
     * Get the emergency threshold percentage
     * 
     * @return The emergency threshold percentage
     */
    public int getEmergencyThresholdPercent() {
        return myState.emergencyThresholdPercent;
    }
    
    /**
     * Set the emergency threshold percentage
     * 
     * @param percent The emergency threshold percentage
     */
    public void setEmergencyThresholdPercent(int percent) {
        myState.emergencyThresholdPercent = percent;
    }
    
    /**
     * Get the optimization interval in minutes
     * 
     * @return The optimization interval in minutes
     */
    public int getOptimizationIntervalMinutes() {
        return myState.optimizationIntervalMinutes;
    }
    
    /**
     * Set the optimization interval in minutes
     * 
     * @param minutes The optimization interval in minutes
     */
    public void setOptimizationIntervalMinutes(int minutes) {
        myState.optimizationIntervalMinutes = minutes;
    }
    
    /**
     * Check if optimization should be performed on low memory
     * 
     * @return True if optimization should be performed on low memory
     */
    public boolean isOptimizeOnLowMemory() {
        return myState.optimizeOnLowMemory;
    }
    
    /**
     * Set whether optimization should be performed on low memory
     * 
     * @param optimize Whether optimization should be performed on low memory
     */
    public void setOptimizeOnLowMemory(boolean optimize) {
        myState.optimizeOnLowMemory = optimize;
    }
    
    /**
     * Check if optimization should be performed before long-running tasks
     * 
     * @return True if optimization should be performed before long-running tasks
     */
    public boolean isOptimizeBeforeLongRunningTasks() {
        return myState.optimizeBeforeLongRunningTasks;
    }
    
    /**
     * Set whether optimization should be performed before long-running tasks
     * 
     * @param optimize Whether optimization should be performed before long-running tasks
     */
    public void setOptimizeBeforeLongRunningTasks(boolean optimize) {
        myState.optimizeBeforeLongRunningTasks = optimize;
    }
    
    /**
     * Check if memory-aware continuous service is enabled
     * 
     * @return True if memory-aware continuous service is enabled
     */
    public boolean isMemoryAwareContinuousServiceEnabled() {
        return myState.enableMemoryAwareContinuousService;
    }
    
    /**
     * Set whether memory-aware continuous service is enabled
     * 
     * @param enabled Whether memory-aware continuous service is enabled
     */
    public void setMemoryAwareContinuousServiceEnabled(boolean enabled) {
        myState.enableMemoryAwareContinuousService = enabled;
    }
    
    /**
     * Get the continuous service default interval in minutes
     * 
     * @return The continuous service default interval in minutes
     */
    public int getContinuousServiceDefaultIntervalMinutes() {
        return myState.continuousServiceDefaultIntervalMinutes;
    }
    
    /**
     * Set the continuous service default interval in minutes
     * 
     * @param minutes The continuous service default interval in minutes
     */
    public void setContinuousServiceDefaultIntervalMinutes(int minutes) {
        myState.continuousServiceDefaultIntervalMinutes = minutes;
    }
    
    /**
     * Get the continuous service reduced interval in minutes
     * 
     * @return The continuous service reduced interval in minutes
     */
    public int getContinuousServiceReducedIntervalMinutes() {
        return myState.continuousServiceReducedIntervalMinutes;
    }
    
    /**
     * Set the continuous service reduced interval in minutes
     * 
     * @param minutes The continuous service reduced interval in minutes
     */
    public void setContinuousServiceReducedIntervalMinutes(int minutes) {
        myState.continuousServiceReducedIntervalMinutes = minutes;
    }
    
    /**
     * Get the continuous service minimum interval in minutes
     * 
     * @return The continuous service minimum interval in minutes
     */
    public int getContinuousServiceMinimumIntervalMinutes() {
        return myState.continuousServiceMinimumIntervalMinutes;
    }
    
    /**
     * Set the continuous service minimum interval in minutes
     * 
     * @param minutes The continuous service minimum interval in minutes
     */
    public void setContinuousServiceMinimumIntervalMinutes(int minutes) {
        myState.continuousServiceMinimumIntervalMinutes = minutes;
    }
}