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
 * Action for toggling AI pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        try {
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean currentState = settings.isPatternRecognition();
            
            // Toggle state
            settings.setPatternRecognition(!currentState);
            boolean newState = !currentState;
            
            // Log state change
            LOG.info("Pattern recognition " + (newState ? "enabled" : "disabled"));
            
            // Show confirmation
            if (newState) {
                Messages.showInfoMessage(
                        project,
                        "Pattern recognition has been enabled.\n" +
                        "ModForge will learn from previous AI interactions to improve performance.",
                        "Pattern Recognition Enabled"
                );
            } else {
                Messages.showInfoMessage(
                        project,
                        "Pattern recognition has been disabled.\n" +
                        "ModForge will use direct API calls for all operations.",
                        "Pattern Recognition Disabled"
                );
            }
        } catch (Exception ex) {
            LOG.error("Error toggling pattern recognition", ex);
            
            // Show error
            Messages.showErrorDialog(
                    project,
                    "An error occurred while toggling pattern recognition: " + ex.getMessage(),
                    "Toggle Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        e.getPresentation().setEnabled(authManager.isAuthenticated());
        
        // Update text based on current state
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean isEnabled = settings.isPatternRecognition();
        
        e.getPresentation().setText(isEnabled 
                ? "Disable Pattern Recognition" 
                : "Enable Pattern Recognition");
    }
}