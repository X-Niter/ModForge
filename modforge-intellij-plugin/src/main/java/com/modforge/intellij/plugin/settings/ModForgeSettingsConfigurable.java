package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeSettingsComponent settingsComponent;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new ModForgeSettingsComponent();
        return settingsComponent.getPanel();
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !settingsComponent.getServerUrl().equals(settings.getServerUrl())
                || settingsComponent.isUseDarkMode() != settings.isUseDarkMode()
                || settingsComponent.isEnableContinuousDevelopment() != settings.isEnableContinuousDevelopment()
                || settingsComponent.isEnablePatternRecognition() != settings.isEnablePatternRecognition()
                || settingsComponent.isEnableGitHubIntegration() != settings.isEnableGitHubIntegration()
                || !settingsComponent.getGithubToken().equals(settings.getGithubToken())
                || settingsComponent.getMaxApiRequestsPerDay() != settings.getMaxApiRequestsPerDay();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        // Validate server URL
        String serverUrl = settingsComponent.getServerUrl();
        if (serverUrl.isEmpty()) {
            throw new ConfigurationException("Server URL is required");
        }
        
        // Validate GitHub token if integration is enabled
        if (settingsComponent.isEnableGitHubIntegration()) {
            String githubToken = settingsComponent.getGithubToken();
            if (githubToken.isEmpty()) {
                throw new ConfigurationException("GitHub token is required when GitHub integration is enabled");
            }
        }
        
        // Validate max API requests
        int maxApiRequests = settingsComponent.getMaxApiRequestsPerDay();
        if (maxApiRequests <= 0) {
            throw new ConfigurationException("Max API requests per day must be greater than 0");
        }
        
        // Save settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setServerUrl(serverUrl);
        settings.setUseDarkMode(settingsComponent.isUseDarkMode());
        settings.setEnableContinuousDevelopment(settingsComponent.isEnableContinuousDevelopment());
        settings.setEnablePatternRecognition(settingsComponent.isEnablePatternRecognition());
        settings.setEnableGitHubIntegration(settingsComponent.isEnableGitHubIntegration());
        settings.setGithubToken(settingsComponent.getGithubToken());
        settings.setMaxApiRequestsPerDay(settingsComponent.getMaxApiRequestsPerDay());
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settingsComponent.setServerUrl(settings.getServerUrl());
        settingsComponent.setUseDarkMode(settings.isUseDarkMode());
        settingsComponent.setEnableContinuousDevelopment(settings.isEnableContinuousDevelopment());
        settingsComponent.setEnablePatternRecognition(settings.isEnablePatternRecognition());
        settingsComponent.setEnableGitHubIntegration(settings.isEnableGitHubIntegration());
        settingsComponent.setGithubToken(settings.getGithubToken());
        settingsComponent.setMaxApiRequestsPerDay(settings.getMaxApiRequestsPerDay());
    }
    
    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
    
    /**
     * Test the connection to the server.
     *
     * @param serverUrl The server URL to test
     * @return A future with the result of the test (true if successful)
     */
    public CompletableFuture<Boolean> testConnection(String serverUrl) {
        return ConnectionTestUtil.testConnection(serverUrl);
    }
}