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
                || !mySettingsComponent.getGitHubUsername().equals(settings.getGitHubUsername())
                || !mySettingsComponent.getGitHubRepository().equals(settings.getGitHubRepository())
                || mySettingsComponent.isAutoMonitorRepository() != settings.isAutoMonitorRepository()
                || mySettingsComponent.isAutoRespondToIssues() != settings.isAutoRespondToIssues()
                || mySettingsComponent.getMaxApiRequestsPerDay() != settings.getMaxApiRequestsPerDay()
                || !mySettingsComponent.getOpenAiApiKey().equals(settings.getOpenAiApiKey())
                || !mySettingsComponent.getOpenAiModel().equals(settings.getOpenAiModel())
                || mySettingsComponent.getMaxTokens() != settings.getMaxTokens()
                || mySettingsComponent.getTemperature() != settings.getTemperature();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Validate serverUrl
        if (mySettingsComponent.getServerUrl().isEmpty()) {
            throw new ConfigurationException("Server URL cannot be empty");
        }
        
        // Validate GitHub settings if GitHub integration is enabled
        if (mySettingsComponent.isEnableGitHubIntegration()) {
            if (mySettingsComponent.getGithubToken().isEmpty()) {
                throw new ConfigurationException("GitHub token cannot be empty when GitHub integration is enabled");
            }
            
            if (mySettingsComponent.getGitHubUsername().isEmpty()) {
                throw new ConfigurationException("GitHub username cannot be empty when GitHub integration is enabled");
            }
        }
        
        // Validate OpenAI settings
        if (mySettingsComponent.getOpenAiApiKey().isEmpty()) {
            throw new ConfigurationException("OpenAI API key cannot be empty");
        }
        
        if (mySettingsComponent.getOpenAiModel().isEmpty()) {
            throw new ConfigurationException("OpenAI model cannot be empty");
        }
        
        // Apply changes
        settings.setServerUrl(mySettingsComponent.getServerUrl());
        settings.setUseDarkMode(mySettingsComponent.isUseDarkMode());
        settings.setEnableContinuousDevelopment(mySettingsComponent.isEnableContinuousDevelopment());
        settings.setEnablePatternRecognition(mySettingsComponent.isEnablePatternRecognition());
        settings.setEnableGitHubIntegration(mySettingsComponent.isEnableGitHubIntegration());
        settings.setGithubToken(mySettingsComponent.getGithubToken());
        settings.setGitHubUsername(mySettingsComponent.getGitHubUsername());
        settings.setGitHubRepository(mySettingsComponent.getGitHubRepository());
        settings.setAutoMonitorRepository(mySettingsComponent.isAutoMonitorRepository());
        settings.setAutoRespondToIssues(mySettingsComponent.isAutoRespondToIssues());
        settings.setMaxApiRequestsPerDay(mySettingsComponent.getMaxApiRequestsPerDay());
        settings.setOpenAiApiKey(mySettingsComponent.getOpenAiApiKey());
        settings.setOpenAiModel(mySettingsComponent.getOpenAiModel());
        settings.setMaxTokens(mySettingsComponent.getMaxTokens());
        settings.setTemperature(mySettingsComponent.getTemperature());
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
        mySettingsComponent.setGitHubUsername(settings.getGitHubUsername());
        mySettingsComponent.setGitHubRepository(settings.getGitHubRepository());
        mySettingsComponent.setAutoMonitorRepository(settings.isAutoMonitorRepository());
        mySettingsComponent.setAutoRespondToIssues(settings.isAutoRespondToIssues());
        mySettingsComponent.setMaxApiRequestsPerDay(settings.getMaxApiRequestsPerDay());
        mySettingsComponent.setOpenAiApiKey(settings.getOpenAiApiKey());
        mySettingsComponent.setOpenAiModel(settings.getOpenAiModel());
        mySettingsComponent.setMaxTokens(settings.getMaxTokens());
        mySettingsComponent.setTemperature(settings.getTemperature());
    }
    
    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}