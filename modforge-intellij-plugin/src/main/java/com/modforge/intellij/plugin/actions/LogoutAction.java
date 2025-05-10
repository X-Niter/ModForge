package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging out from ModForge.
 */
public class LogoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LogoutAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Enable action only if the user is authenticated
        e.getPresentation().setEnabled(authManager.isAuthenticated());
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Make sure the user is authenticated
        if (!authManager.isAuthenticated()) {
            Messages.showInfoMessage(
                    project,
                    "You are not currently logged in.",
                    "Logout"
            );
            return;
        }
        
        // Ask for confirmation
        String username = authManager.getUsername();
        int result = Messages.showYesNoDialog(
                project,
                "Are you sure you want to log out from ModForge? This will stop all services that require authentication.",
                "Confirm Logout",
                "Logout",
                "Cancel",
                null
        );
        
        if (result == Messages.YES) {
            // Stop continuous development if it's running
            stopContinuousDevelopment(project);
            
            // Logout
            authManager.logout();
            
            Messages.showInfoMessage(
                    project,
                    "You have been logged out from ModForge." +
                            (username.isEmpty() ? "" : " (Goodbye, " + username + "!)"),
                    "Logout Successful"
            );
        }
    }
    
    /**
     * Stop continuous development if it's running.
     *
     * @param project The project
     */
    private void stopContinuousDevelopment(Project project) {
        if (project == null) {
            return;
        }
        
        ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
        if (continuousService != null && continuousService.isRunning()) {
            continuousService.stop();
            
            Messages.showInfoMessage(
                    project,
                    "Continuous development has been stopped due to logout.",
                    "Continuous Development"
            );
        }
    }
}