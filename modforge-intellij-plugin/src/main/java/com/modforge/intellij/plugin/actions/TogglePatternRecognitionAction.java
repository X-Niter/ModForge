package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction implements Toggleable {
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
                        "You must be logged in to use pattern recognition.",
                        "Authentication Required"
                );
                return;
            }
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            // Toggle pattern recognition
            boolean enabled = !settings.isEnablePatternRecognition();
            settings.setEnablePatternRecognition(enabled);
            
            // Show message
            if (enabled) {
                Messages.showInfoMessage(
                        project,
                        "Pattern recognition has been enabled.\n" +
                                "This helps reduce API usage by learning from previous requests.",
                        "Pattern Recognition"
                );
            } else {
                Messages.showInfoMessage(
                        project,
                        "Pattern recognition has been disabled.\n" +
                                "All requests will be sent to the API, which may incur higher costs.",
                        "Pattern Recognition"
                );
            }
            
            // Update selected state
            Toggleable.setSelected(e.getPresentation(), enabled);
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
        Project project = e.getProject();
        
        // Only enable if authenticated and project is available
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        boolean enabled = authManager.isAuthenticated() && project != null;
        
        e.getPresentation().setEnabled(enabled);
        
        // Update selected state
        ModForgeSettings settings = ModForgeSettings.getInstance();
        Toggleable.setSelected(e.getPresentation(), settings.isEnablePatternRecognition());
    }
}