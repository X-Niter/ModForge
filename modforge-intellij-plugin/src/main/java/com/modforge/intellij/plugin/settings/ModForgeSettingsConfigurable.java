package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeSettingsComponent component;
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public JComponent getPreferredFocusedComponent() {
        return component.getPreferredFocusedComponent();
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        component = new ModForgeSettingsComponent();
        return component.getPanel();
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !component.getServerUrl().equals(settings.getServerUrl())
                || !component.getUsername().equals(settings.getUsername())
                || !component.getPassword().equals(settings.getPassword())
                || component.isRememberCredentials() != settings.isRememberCredentials()
                || component.isEnableContinuousDevelopment() != settings.isEnableContinuousDevelopment()
                || component.getContinuousDevelopmentFrequency() != settings.getContinuousDevelopmentFrequency()
                || component.isEnableAIGeneration() != settings.isEnableAIGeneration()
                || component.isUsePatternLearning() != settings.isUsePatternLearning()
                || !component.getGithubToken().equals(settings.getGithubToken())
                || !component.getGithubUsername().equals(settings.getGithubUsername());
    }
    
    @Override
    public void apply() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setServerUrl(component.getServerUrl());
        settings.setUsername(component.getUsername());
        settings.setPassword(component.getPassword());
        settings.setRememberCredentials(component.isRememberCredentials());
        settings.setEnableContinuousDevelopment(component.isEnableContinuousDevelopment());
        settings.setContinuousDevelopmentFrequency(component.getContinuousDevelopmentFrequency());
        settings.setEnableAIGeneration(component.isEnableAIGeneration());
        settings.setUsePatternLearning(component.isUsePatternLearning());
        settings.setGithubToken(component.getGithubToken());
        settings.setGithubUsername(component.getGithubUsername());
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        component.setServerUrl(settings.getServerUrl());
        component.setUsername(settings.getUsername());
        component.setPassword(settings.getPassword());
        component.setRememberCredentials(settings.isRememberCredentials());
        component.setEnableContinuousDevelopment(settings.isEnableContinuousDevelopment());
        component.setContinuousDevelopmentFrequency(settings.getContinuousDevelopmentFrequency());
        component.setEnableAIGeneration(settings.isEnableAIGeneration());
        component.setUsePatternLearning(settings.isUsePatternLearning());
        component.setGithubToken(settings.getGithubToken());
        component.setGithubUsername(settings.getGithubUsername());
    }
    
    @Override
    public void disposeUIResources() {
        component = null;
    }
}