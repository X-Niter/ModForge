package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ModForge settings.
 * This class is used to display and modify settings in the IDE settings dialog.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeSettingsComponent settingsComponent;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new ModForgeSettingsComponent();
        return settingsComponent.getPanel();
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Check if settings are modified
        return !settingsComponent.getApiKey().equals(settings.getApiKey())
                || !settingsComponent.getAiModel().equals(settings.getAiModel())
                || settingsComponent.isContinuousDevelopmentEnabled() != settings.isContinuousDevelopmentEnabled()
                || settingsComponent.getContinuousDevelopmentIntervalMinutes() != settings.getContinuousDevelopmentIntervalMinutes()
                || settingsComponent.isPatternRecognitionEnabled() != settings.isPatternRecognitionEnabled()
                || !settingsComponent.getSyncServerUrl().equals(settings.getSyncServerUrl())
                || !settingsComponent.getSyncToken().equals(settings.getSyncToken())
                || settingsComponent.isSyncEnabled() != settings.isSyncEnabled()
                || settingsComponent.isAutoUploadEnabled() != settings.isAutoUploadEnabled()
                || settingsComponent.isAutoDownloadEnabled() != settings.isAutoDownloadEnabled()
                || settingsComponent.isDarkTheme() != settings.isDarkTheme();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Update settings
        settings.setApiKey(settingsComponent.getApiKey());
        settings.setAiModel(settingsComponent.getAiModel());
        settings.setContinuousDevelopmentEnabled(settingsComponent.isContinuousDevelopmentEnabled());
        settings.setContinuousDevelopmentIntervalMinutes(settingsComponent.getContinuousDevelopmentIntervalMinutes());
        settings.setPatternRecognitionEnabled(settingsComponent.isPatternRecognitionEnabled());
        settings.setSyncServerUrl(settingsComponent.getSyncServerUrl());
        settings.setSyncToken(settingsComponent.getSyncToken());
        settings.setSyncEnabled(settingsComponent.isSyncEnabled());
        settings.setAutoUploadEnabled(settingsComponent.isAutoUploadEnabled());
        settings.setAutoDownloadEnabled(settingsComponent.isAutoDownloadEnabled());
        settings.setDarkTheme(settingsComponent.isDarkTheme());
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Reset component
        settingsComponent.setApiKey(settings.getApiKey());
        settingsComponent.setAiModel(settings.getAiModel());
        settingsComponent.setContinuousDevelopmentEnabled(settings.isContinuousDevelopmentEnabled());
        settingsComponent.setContinuousDevelopmentIntervalMinutes(settings.getContinuousDevelopmentIntervalMinutes());
        settingsComponent.setPatternRecognitionEnabled(settings.isPatternRecognitionEnabled());
        settingsComponent.setSyncServerUrl(settings.getSyncServerUrl());
        settingsComponent.setSyncToken(settings.getSyncToken());
        settingsComponent.setSyncEnabled(settings.isSyncEnabled());
        settingsComponent.setAutoUploadEnabled(settings.isAutoUploadEnabled());
        settingsComponent.setAutoDownloadEnabled(settings.isAutoDownloadEnabled());
        settingsComponent.setDarkTheme(settings.isDarkTheme());
    }
    
    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}