package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Settings configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeSettingsComponent mySettingsComponent;
    
    /**
     * Create a settings configurable.
     */
    public ModForgeSettingsConfigurable() {
    }
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new ModForgeSettingsComponent();
        return mySettingsComponent.getPanel();
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return !mySettingsComponent.getServerUrl().equals(settings.getServerUrl())
                || mySettingsComponent.isUseDarkMode() != settings.isUseDarkMode()
                || mySettingsComponent.isEnableContinuousDevelopment() != settings.isEnableContinuousDevelopment()
                || mySettingsComponent.isEnablePatternRecognition() != settings.isEnablePatternRecognition()
                || mySettingsComponent.isEnableGitHubIntegration() != settings.isEnableGitHubIntegration()
                || !mySettingsComponent.getGithubToken().equals(settings.getGithubToken())
                || mySettingsComponent.getMaxApiRequestsPerDay() != settings.getMaxApiRequestsPerDay();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Validate serverUrl
        if (mySettingsComponent.getServerUrl().isEmpty()) {
            throw new ConfigurationException("Server URL cannot be empty");
        }
        
        // Validate GitHub token if GitHub integration is enabled
        if (mySettingsComponent.isEnableGitHubIntegration() && mySettingsComponent.getGithubToken().isEmpty()) {
            throw new ConfigurationException("GitHub token cannot be empty when GitHub integration is enabled");
        }
        
        // Apply changes
        settings.setServerUrl(mySettingsComponent.getServerUrl());
        settings.setUseDarkMode(mySettingsComponent.isUseDarkMode());
        settings.setEnableContinuousDevelopment(mySettingsComponent.isEnableContinuousDevelopment());
        settings.setEnablePatternRecognition(mySettingsComponent.isEnablePatternRecognition());
        settings.setEnableGitHubIntegration(mySettingsComponent.isEnableGitHubIntegration());
        settings.setGithubToken(mySettingsComponent.getGithubToken());
        settings.setMaxApiRequestsPerDay(mySettingsComponent.getMaxApiRequestsPerDay());
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        mySettingsComponent.setServerUrl(settings.getServerUrl());
        mySettingsComponent.setUseDarkMode(settings.isUseDarkMode());
        mySettingsComponent.setEnableContinuousDevelopment(settings.isEnableContinuousDevelopment());
        mySettingsComponent.setEnablePatternRecognition(settings.isEnablePatternRecognition());
        mySettingsComponent.setEnableGitHubIntegration(settings.isEnableGitHubIntegration());
        mySettingsComponent.setGithubToken(settings.getGithubToken());
        mySettingsComponent.setMaxApiRequestsPerDay(settings.getMaxApiRequestsPerDay());
    }
    
    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}