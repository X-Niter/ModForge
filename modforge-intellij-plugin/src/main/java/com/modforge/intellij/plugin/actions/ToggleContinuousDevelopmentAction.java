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
 * Action for toggling continuous development mode.
 */
public class ToggleContinuousDevelopmentAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            LOG.warn("No project available for continuous development toggle");
            return;
        }
        
        try {
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean currentState = settings.isContinuousDevelopment();
            
            // Toggle state
            settings.setContinuousDevelopment(!currentState);
            boolean newState = !currentState;
            
            // Log state change
            LOG.info("Continuous development " + (newState ? "enabled" : "disabled"));
            
            // Get service
            ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
            
            if (service != null) {
                if (newState) {
                    // Start service if enabled
                    service.start();
                    
                    // Show confirmation
                    Messages.showInfoMessage(
                            project,
                            "Continuous development mode has been enabled.\n" +
                            "ModForge will monitor your mods and fix errors automatically.",
                            "Continuous Development Enabled"
                    );
                } else {
                    // Stop service if disabled
                    service.stop();
                    
                    // Show confirmation
                    Messages.showInfoMessage(
                            project,
                            "Continuous development mode has been disabled.",
                            "Continuous Development Disabled"
                    );
                }
            } else {
                LOG.error("ContinuousDevelopmentService is null");
                
                // Show error
                Messages.showErrorDialog(
                        project,
                        "Could not access ContinuousDevelopmentService.\n" +
                        "Please restart IntelliJ IDEA and try again.",
                        "Service Error"
                );
            }
        } catch (Exception ex) {
            LOG.error("Error toggling continuous development", ex);
            
            // Show error
            Messages.showErrorDialog(
                    project,
                    "An error occurred while toggling continuous development: " + ex.getMessage(),
                    "Toggle Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Enable if authenticated and project is available
        e.getPresentation().setEnabled(authManager.isAuthenticated() && e.getProject() != null);
        
        // Update text based on current state
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean isEnabled = settings.isContinuousDevelopment();
        
        e.getPresentation().setText(isEnabled 
                ? "Disable Continuous Development" 
                : "Enable Continuous Development");
    }
}