package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling continuous development.
 */
public class ToggleContinuousDevelopmentAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
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
                        "You must be logged in to toggle continuous development.",
                        "Authentication Required"
                );
                return;
            }
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean continuousDevelopment = settings.isContinuousDevelopment();
            
            // Get service
            ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
            if (service == null) {
                LOG.error("ContinuousDevelopmentService is null");
                return;
            }
            
            // Toggle continuous development
            boolean newValue = !continuousDevelopment;
            settings.setContinuousDevelopment(newValue);
            
            // Start or stop service
            if (newValue) {
                service.start();
                Messages.showInfoMessage(
                        project,
                        "Continuous development enabled",
                        "Continuous Development"
                );
            } else {
                service.stop();
                Messages.showInfoMessage(
                        project,
                        "Continuous development disabled",
                        "Continuous Development"
                );
            }
        } catch (Exception ex) {
            LOG.error("Error in toggle continuous development action", ex);
            
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
            boolean continuousDevelopment = settings.isContinuousDevelopment();
            
            e.getPresentation().setText(continuousDevelopment ? "Disable Continuous Development" : "Enable Continuous Development");
        }
    }
}