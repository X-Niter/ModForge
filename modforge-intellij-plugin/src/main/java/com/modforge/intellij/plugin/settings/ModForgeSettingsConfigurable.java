package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeSettingsComponent mySettingsComponent;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
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
        
        return !mySettingsComponent.getServerUrl().equals(settings.getServerUrl()) ||
               mySettingsComponent.getContinuousDevelopment() != settings.isContinuousDevelopment() ||
               mySettingsComponent.getPatternRecognition() != settings.isPatternRecognition();
    }
    
    @Override
    public void apply() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Get current values
        String oldServerUrl = settings.getServerUrl();
        boolean oldContinuousDevelopment = settings.isContinuousDevelopment();
        
        // Apply new values
        settings.setServerUrl(mySettingsComponent.getServerUrl());
        settings.setContinuousDevelopment(mySettingsComponent.getContinuousDevelopment());
        settings.setPatternRecognition(mySettingsComponent.getPatternRecognition());
        
        // Test connection if server URL changed
        if (!oldServerUrl.equals(mySettingsComponent.getServerUrl())) {
            boolean connected = ConnectionTestUtil.testConnection(mySettingsComponent.getServerUrl());
            
            if (!connected) {
                Messages.showWarningDialog(
                        "Could not connect to server. Please check the server URL.",
                        "Connection Warning"
                );
            }
            
            // Try to verify token
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (authManager.isAuthenticated()) {
                if (!authManager.verifyAuthentication()) {
                    Messages.showWarningDialog(
                            "Could not verify authentication with new server URL. Please log in again.",
                            "Authentication Warning"
                    );
                    
                    // Logout
                    authManager.logout();
                }
            }
        }
        
        // Update continuous development service if changed
        if (oldContinuousDevelopment != mySettingsComponent.getContinuousDevelopment()) {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            
            for (Project project : projects) {
                ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
                
                if (service != null) {
                    if (mySettingsComponent.getContinuousDevelopment()) {
                        service.start();
                    } else {
                        service.stop();
                    }
                }
            }
        }
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        mySettingsComponent.setServerUrl(settings.getServerUrl());
        mySettingsComponent.setContinuousDevelopment(settings.isContinuousDevelopment());
        mySettingsComponent.setPatternRecognition(settings.isPatternRecognition());
    }
    
    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}