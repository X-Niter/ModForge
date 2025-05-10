package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeSettingsComponent settingsComponent;
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new ModForgeSettingsComponent();
        return settingsComponent.getPanel();
    }
    
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !settings.getOpenAiApiKey().equals(settingsComponent.getApiKey()) ||
               !settings.getServerUrl().equals(settingsComponent.getServerUrl()) ||
               !settings.getUsername().equals(settingsComponent.getUsername()) ||
               !settings.getPassword().equals(settingsComponent.getPassword()) ||
               settings.isContinuousDevelopmentEnabled() != settingsComponent.isContinuousDevelopmentEnabled() ||
               settings.isPatternRecognitionEnabled() != settingsComponent.isPatternRecognitionEnabled() ||
               settings.getUpdateFrequencyMinutes() != settingsComponent.getContinuousDevelopmentIntervalMinutes();
    }
    
    @Override
    public void apply() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Save API settings
        settings.setOpenAiApiKey(settingsComponent.getApiKey());
        
        // Save server connection settings
        settings.setServerUrl(settingsComponent.getServerUrl());
        settings.setUsername(settingsComponent.getUsername());
        settings.setPassword(settingsComponent.getPassword());
        
        // Save development settings
        settings.setContinuousDevelopmentEnabled(settingsComponent.isContinuousDevelopmentEnabled());
        settings.setPatternRecognitionEnabled(settingsComponent.isPatternRecognitionEnabled());
        settings.setUpdateFrequencyMinutes(settingsComponent.getContinuousDevelopmentIntervalMinutes());
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Reset API settings
        settingsComponent.setApiKey(settings.getOpenAiApiKey());
        
        // Reset server connection settings
        settingsComponent.setServerUrl(settings.getServerUrl());
        settingsComponent.setUsername(settings.getUsername());
        settingsComponent.setPassword(settings.getPassword());
        
        // Reset development settings
        settingsComponent.setContinuousDevelopmentEnabled(settings.isContinuousDevelopmentEnabled());
        settingsComponent.setPatternRecognitionEnabled(settings.isPatternRecognitionEnabled());
        settingsComponent.setContinuousDevelopmentIntervalMinutes(settings.getUpdateFrequencyMinutes());
    }
    
    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}