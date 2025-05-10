package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
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
public class ToggleContinuousDevelopmentAction extends AnAction implements Toggleable {
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
                        "You must be logged in to use continuous development.",
                        "Authentication Required"
                );
                return;
            }
            
            // Get continuous development service
            ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
            if (continuousService == null) {
                LOG.error("Continuous development service is null");
                return;
            }
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            // Toggle continuous development
            if (continuousService.isRunning()) {
                continuousService.stop();
                settings.setEnableContinuousDevelopment(false);
                
                Messages.showInfoMessage(
                        project,
                        "Continuous development has been stopped.",
                        "Continuous Development"
                );
            } else {
                continuousService.start();
                settings.setEnableContinuousDevelopment(true);
                
                Messages.showInfoMessage(
                        project,
                        "Continuous development has been started.",
                        "Continuous Development"
                );
            }
            
            // Update selected state
            Toggleable.setSelected(e.getPresentation(), continuousService.isRunning());
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
        Project project = e.getProject();
        
        // Only enable if authenticated and project is available
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        boolean enabled = authManager.isAuthenticated() && project != null;
        
        e.getPresentation().setEnabled(enabled);
        
        // Update selected state if project is available
        if (project != null) {
            ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
            if (continuousService != null) {
                Toggleable.setSelected(e.getPresentation(), continuousService.isRunning());
            }
        }
    }
}