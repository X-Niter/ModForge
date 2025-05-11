package com.modforge.intellij.plugin.memory.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for memory thresholds
 * Allows for environment-specific memory threshold settings
 */
@Service(Service.Level.APP)
@State(
    name = "com.modforge.intellij.plugin.memory.config.MemoryThresholdConfig",
    storages = @Storage("ModForgeMemoryThresholds.xml")
)
public final class MemoryThresholdConfig implements PersistentStateComponent<MemoryThresholdConfig.State> {
    private static final Logger LOG = Logger.getInstance(MemoryThresholdConfig.class);
    
    private State myState = new State();
    
    /**
     * State class for memory threshold configuration
     */
    public static class State {
        /**
         * Environment-specific threshold configurations
         */
        public Map<String, EnvironmentConfig> environmentConfigs = new HashMap<>();
        
        /**
         * Default environment configuration
         */
        public EnvironmentConfig defaultConfig = new EnvironmentConfig();
        
        /**
         * Whether to auto-detect environment
         */
        public boolean autoDetectEnvironment = true;
        
        /**
         * Manually selected environment (used if auto-detect is disabled)
         */
        public String selectedEnvironment = "default";
    }
    
    /**
     * Environment-specific configuration
     */
    public static class EnvironmentConfig {
        /**
         * Warning threshold percentage (0-100)
         */
        public int warningThresholdPercent = 70;
        
        /**
         * Critical threshold percentage (0-100)
         */
        public int criticalThresholdPercent = 85;
        
        /**
         * Emergency threshold percentage (0-100)
         */
        public int emergencyThresholdPercent = 95;
        
        /**
         * Available memory threshold in MB - triggers warning if available memory falls below this
         */
        public int availableMemoryWarningMb = 512;
        
        /**
         * Available memory threshold in MB - triggers critical if available memory falls below this
         */
        public int availableMemoryCriticalMb = 256;
        
        /**
         * Available memory threshold in MB - triggers emergency if available memory falls below this
         */
        public int availableMemoryEmergencyMb = 128;
        
        /**
         * Memory growth rate threshold (percentage per minute) - triggers preemptive action if exceeded
         */
        public double memoryGrowthRateThresholdPctPerMin = 1.0;
    }
    
    /**
     * Get the singleton instance
     */
    public static MemoryThresholdConfig getInstance() {
        return ApplicationManager.getApplication().getService(MemoryThresholdConfig.class);
    }
    
    @Override
    public @Nullable State getState() {
        // Make a deep copy to avoid issues with mutable state objects
        // This is important for the PersistentStateComponent infrastructure
        State stateCopy = new State();
        
        // Copy all environment configs
        for (Map.Entry<String, EnvironmentConfig> entry : myState.environmentConfigs.entrySet()) {
            EnvironmentConfig configCopy = new EnvironmentConfig();
            EnvironmentConfig original = entry.getValue();
            
            // Copy all properties
            configCopy.warningThresholdPercent = original.warningThresholdPercent;
            configCopy.criticalThresholdPercent = original.criticalThresholdPercent;
            configCopy.emergencyThresholdPercent = original.emergencyThresholdPercent;
            configCopy.availableMemoryWarningMb = original.availableMemoryWarningMb;
            configCopy.availableMemoryCriticalMb = original.availableMemoryCriticalMb;
            configCopy.availableMemoryEmergencyMb = original.availableMemoryEmergencyMb;
            configCopy.memoryGrowthRateThresholdPctPerMin = original.memoryGrowthRateThresholdPctPerMin;
            
            stateCopy.environmentConfigs.put(entry.getKey(), configCopy);
        }
        
        // Copy default config
        if (myState.defaultConfig != null) {
            EnvironmentConfig defaultCopy = new EnvironmentConfig();
            EnvironmentConfig original = myState.defaultConfig;
            
            defaultCopy.warningThresholdPercent = original.warningThresholdPercent;
            defaultCopy.criticalThresholdPercent = original.criticalThresholdPercent;
            defaultCopy.emergencyThresholdPercent = original.emergencyThresholdPercent;
            defaultCopy.availableMemoryWarningMb = original.availableMemoryWarningMb;
            defaultCopy.availableMemoryCriticalMb = original.availableMemoryCriticalMb;
            defaultCopy.availableMemoryEmergencyMb = original.availableMemoryEmergencyMb;
            defaultCopy.memoryGrowthRateThresholdPctPerMin = original.memoryGrowthRateThresholdPctPerMin;
            
            stateCopy.defaultConfig = defaultCopy;
        }
        
        // Copy other properties
        stateCopy.autoDetectEnvironment = myState.autoDetectEnvironment;
        stateCopy.selectedEnvironment = myState.selectedEnvironment;
        
        return stateCopy;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        try {
            // Don't modify the passed state directly; make a deep copy
            myState = new State();
            
            // Copy all environment configs
            if (state.environmentConfigs != null) {
                for (Map.Entry<String, EnvironmentConfig> entry : state.environmentConfigs.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        EnvironmentConfig configCopy = new EnvironmentConfig();
                        EnvironmentConfig original = entry.getValue();
                        
                        // Copy all properties with validation
                        configCopy.warningThresholdPercent = Math.max(0, Math.min(100, original.warningThresholdPercent));
                        configCopy.criticalThresholdPercent = Math.max(0, Math.min(100, original.criticalThresholdPercent));
                        configCopy.emergencyThresholdPercent = Math.max(0, Math.min(100, original.emergencyThresholdPercent));
                        configCopy.availableMemoryWarningMb = Math.max(1, original.availableMemoryWarningMb);
                        configCopy.availableMemoryCriticalMb = Math.max(1, original.availableMemoryCriticalMb);
                        configCopy.availableMemoryEmergencyMb = Math.max(1, original.availableMemoryEmergencyMb);
                        configCopy.memoryGrowthRateThresholdPctPerMin = Math.max(0.1, original.memoryGrowthRateThresholdPctPerMin);
                        
                        myState.environmentConfigs.put(entry.getKey(), configCopy);
                    }
                }
            }
            
            // Copy default config
            if (state.defaultConfig != null) {
                EnvironmentConfig defaultCopy = new EnvironmentConfig();
                EnvironmentConfig original = state.defaultConfig;
                
                defaultCopy.warningThresholdPercent = Math.max(0, Math.min(100, original.warningThresholdPercent));
                defaultCopy.criticalThresholdPercent = Math.max(0, Math.min(100, original.criticalThresholdPercent));
                defaultCopy.emergencyThresholdPercent = Math.max(0, Math.min(100, original.emergencyThresholdPercent));
                defaultCopy.availableMemoryWarningMb = Math.max(1, original.availableMemoryWarningMb);
                defaultCopy.availableMemoryCriticalMb = Math.max(1, original.availableMemoryCriticalMb);
                defaultCopy.availableMemoryEmergencyMb = Math.max(1, original.availableMemoryEmergencyMb);
                defaultCopy.memoryGrowthRateThresholdPctPerMin = Math.max(0.1, original.memoryGrowthRateThresholdPctPerMin);
                
                myState.defaultConfig = defaultCopy;
            } else {
                myState.defaultConfig = new EnvironmentConfig();
            }
            
            // Copy other properties
            myState.autoDetectEnvironment = state.autoDetectEnvironment;
            myState.selectedEnvironment = state.selectedEnvironment;
            
            // Ensure default environment config always exists
            if (!myState.environmentConfigs.containsKey("default")) {
                myState.environmentConfigs.put("default", new EnvironmentConfig());
            }
            
            // Validate threshold relationships
            for (EnvironmentConfig config : myState.environmentConfigs.values()) {
                validateThresholdRelationships(config);
            }
            validateThresholdRelationships(myState.defaultConfig);
            
            LOG.info("Successfully loaded memory threshold configuration with " + 
                    myState.environmentConfigs.size() + " environment(s)");
                    
        } catch (Exception ex) {
            LOG.error("Error loading memory threshold configuration, using defaults", ex);
            myState = new State();
            myState.environmentConfigs.put("default", new EnvironmentConfig());
        }
    }
    
    /**
     * Validate and fix the relationships between thresholds
     * 
     * @param config The environment configuration to validate
     */
    private void validateThresholdRelationships(EnvironmentConfig config) {
        if (config == null) {
            return;
        }
        
        // Ensure warning < critical < emergency for percentage thresholds
        if (config.warningThresholdPercent >= config.criticalThresholdPercent) {
            config.warningThresholdPercent = config.criticalThresholdPercent - 5;
        }
        
        if (config.criticalThresholdPercent >= config.emergencyThresholdPercent) {
            config.criticalThresholdPercent = config.emergencyThresholdPercent - 5;
        }
        
        // Ensure warning > critical > emergency for available memory thresholds
        if (config.availableMemoryWarningMb <= config.availableMemoryCriticalMb) {
            config.availableMemoryWarningMb = config.availableMemoryCriticalMb + 64;
        }
        
        if (config.availableMemoryCriticalMb <= config.availableMemoryEmergencyMb) {
            config.availableMemoryCriticalMb = config.availableMemoryEmergencyMb + 32;
        }
    }
    
    /**
     * Initialize with default environment configurations
     */
    public void initializeDefaults() {
        // Clear existing configs
        myState.environmentConfigs.clear();
        
        // Add default config
        EnvironmentConfig defaultConfig = new EnvironmentConfig();
        myState.environmentConfigs.put("default", defaultConfig);
        
        // Add development environment config (more conservative)
        EnvironmentConfig devConfig = new EnvironmentConfig();
        devConfig.warningThresholdPercent = 65;
        devConfig.criticalThresholdPercent = 80;
        devConfig.emergencyThresholdPercent = 90;
        devConfig.availableMemoryWarningMb = 768;
        devConfig.availableMemoryCriticalMb = 384;
        devConfig.availableMemoryEmergencyMb = 192;
        myState.environmentConfigs.put("development", devConfig);
        
        // Add production environment config (more aggressive)
        EnvironmentConfig prodConfig = new EnvironmentConfig();
        prodConfig.warningThresholdPercent = 75;
        prodConfig.criticalThresholdPercent = 88;
        prodConfig.emergencyThresholdPercent = 97;
        prodConfig.availableMemoryWarningMb = 384;
        prodConfig.availableMemoryCriticalMb = 192;
        prodConfig.availableMemoryEmergencyMb = 96;
        myState.environmentConfigs.put("production", prodConfig);
        
        // Set defaults
        myState.defaultConfig = defaultConfig;
        myState.autoDetectEnvironment = true;
        myState.selectedEnvironment = "default";
        
        LOG.info("Memory threshold configuration initialized with defaults");
    }
    
    /**
     * Get the current environment configuration
     * 
     * @return The environment configuration
     */
    public EnvironmentConfig getCurrentConfig() {
        String environmentName = getCurrentEnvironmentName();
        return myState.environmentConfigs.getOrDefault(environmentName, myState.defaultConfig);
    }
    
    /**
     * Get the current environment name
     * 
     * @return The environment name
     */
    public String getCurrentEnvironmentName() {
        if (myState.autoDetectEnvironment) {
            return detectEnvironment();
        } else {
            return myState.selectedEnvironment;
        }
    }
    
    /**
     * Set the selected environment
     * 
     * @param environmentName The environment name
     */
    public void setSelectedEnvironment(String environmentName) {
        myState.selectedEnvironment = environmentName;
        myState.autoDetectEnvironment = false;
    }
    
    /**
     * Set whether to auto-detect environment
     * 
     * @param autoDetect Whether to auto-detect
     */
    public void setAutoDetectEnvironment(boolean autoDetect) {
        myState.autoDetectEnvironment = autoDetect;
    }
    
    /**
     * Get the warning threshold percentage
     * 
     * @return The warning threshold percentage
     */
    public int getWarningThresholdPercent() {
        return getCurrentConfig().warningThresholdPercent;
    }
    
    /**
     * Get the critical threshold percentage
     * 
     * @return The critical threshold percentage
     */
    public int getCriticalThresholdPercent() {
        return getCurrentConfig().criticalThresholdPercent;
    }
    
    /**
     * Get the emergency threshold percentage
     * 
     * @return The emergency threshold percentage
     */
    public int getEmergencyThresholdPercent() {
        return getCurrentConfig().emergencyThresholdPercent;
    }
    
    /**
     * Get the available memory warning threshold in MB
     * 
     * @return The available memory warning threshold in MB
     */
    public int getAvailableMemoryWarningMb() {
        return getCurrentConfig().availableMemoryWarningMb;
    }
    
    /**
     * Get the available memory critical threshold in MB
     * 
     * @return The available memory critical threshold in MB
     */
    public int getAvailableMemoryCriticalMb() {
        return getCurrentConfig().availableMemoryCriticalMb;
    }
    
    /**
     * Get the available memory emergency threshold in MB
     * 
     * @return The available memory emergency threshold in MB
     */
    public int getAvailableMemoryEmergencyMb() {
        return getCurrentConfig().availableMemoryEmergencyMb;
    }
    
    /**
     * Get the memory growth rate threshold
     * 
     * @return The memory growth rate threshold
     */
    public double getMemoryGrowthRateThreshold() {
        return getCurrentConfig().memoryGrowthRateThresholdPctPerMin;
    }
    
    /**
     * Detect the current environment
     * 
     * @return The detected environment name
     */
    private String detectEnvironment() {
        // Attempt to detect the environment from system properties or environment variables
        String detectedEnv = System.getProperty("modforge.environment");
        if (detectedEnv == null || detectedEnv.isEmpty()) {
            detectedEnv = System.getenv("MODFORGE_ENVIRONMENT");
        }
        
        if (detectedEnv != null && !detectedEnv.isEmpty() && myState.environmentConfigs.containsKey(detectedEnv)) {
            return detectedEnv;
        }
        
        // Default to the selected environment if auto-detection fails
        return myState.selectedEnvironment;
    }
    
    /**
     * Save an environment configuration
     * 
     * @param name The environment name
     * @param config The environment configuration
     */
    public void saveEnvironmentConfig(String name, EnvironmentConfig config) {
        myState.environmentConfigs.put(name, config);
        
        // If this is the selected environment, update the default config
        if (name.equals(myState.selectedEnvironment)) {
            myState.defaultConfig = config;
        }
    }
    
    /**
     * Remove an environment configuration
     * 
     * @param name The environment name
     */
    public void removeEnvironmentConfig(String name) {
        if (name.equals("default")) {
            // Cannot remove default
            return;
        }
        
        myState.environmentConfigs.remove(name);
        
        // If this was the selected environment, revert to default
        if (name.equals(myState.selectedEnvironment)) {
            myState.selectedEnvironment = "default";
        }
    }
    
    /**
     * Get all available environment names
     * 
     * @return The environment names
     */
    public String[] getAvailableEnvironments() {
        return myState.environmentConfigs.keySet().toArray(new String[0]);
    }
    
    /**
     * Get a specific environment configuration
     * 
     * @param name The environment name
     * @return The environment configuration
     */
    public EnvironmentConfig getEnvironmentConfig(String name) {
        return myState.environmentConfigs.getOrDefault(name, myState.defaultConfig);
    }
}