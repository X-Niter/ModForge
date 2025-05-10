package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            LOG.warn("Project is null");
            return;
        }
        
        try {
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to toggle pattern recognition.",
                        "Authentication Required"
                );
                return;
            }
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean patternRecognition = settings.isPatternRecognition();
            
            // Toggle pattern recognition
            boolean newValue = !patternRecognition;
            settings.setPatternRecognition(newValue);
            
            // Show message
            Messages.showInfoMessage(
                    project,
                    "Pattern recognition " + (newValue ? "enabled" : "disabled"),
                    "Pattern Recognition"
            );
        } catch (Exception ex) {
            LOG.error("Error in toggle pattern recognition action", ex);
            
            Messages.showErrorDialog(
                    project,
                    "An error occurred: " + ex.getMessage(),
                    "Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        e.getPresentation().setEnabled(authManager.isAuthenticated() && e.getProject() != null);
        
        // Update text
        Project project = e.getProject();
        if (project != null) {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean patternRecognition = settings.isPatternRecognition();
            
            e.getPresentation().setText(patternRecognition ? "Disable Pattern Recognition" : "Enable Pattern Recognition");
        }
    }
}