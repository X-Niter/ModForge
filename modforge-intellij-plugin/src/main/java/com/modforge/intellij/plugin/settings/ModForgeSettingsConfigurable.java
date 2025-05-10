package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(ModForgeSettingsConfigurable.class);
    
    private ModForgeSettingsComponent settingsComponent;
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new ModForgeSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            // Check if any settings have been modified
            return !settingsComponent.getServerUrl().equals(settings.getServerUrl())
                    || settingsComponent.isContinuousDevelopment() != settings.isContinuousDevelopment()
                    || settingsComponent.isPatternRecognition() != settings.isPatternRecognition()
                    || settingsComponent.getPollingInterval() != settings.getPollingInterval();
        } catch (Exception e) {
            LOG.error("Error checking if settings are modified", e);
            return false;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            // Apply settings
            settings.setServerUrl(settingsComponent.getServerUrl());
            settings.setContinuousDevelopment(settingsComponent.isContinuousDevelopment());
            settings.setPatternRecognition(settingsComponent.isPatternRecognition());
            settings.setPollingInterval(settingsComponent.getPollingInterval());
            
            LOG.info("Applied ModForge settings");
        } catch (Exception e) {
            LOG.error("Error applying settings", e);
            throw new ConfigurationException("Failed to apply settings: " + e.getMessage(), "Settings Error");
        }
    }

    @Override
    public void reset() {
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            // Reset component with current settings
            settingsComponent.setServerUrl(settings.getServerUrl());
            settingsComponent.setContinuousDevelopment(settings.isContinuousDevelopment());
            settingsComponent.setPatternRecognition(settings.isPatternRecognition());
            settingsComponent.setPollingInterval(settings.getPollingInterval());
            
            LOG.info("Reset ModForge settings");
        } catch (Exception e) {
            LOG.error("Error resetting settings", e);
        }
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}