package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging out from ModForge.
 */
public class LogoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LogoutAction.class);
    
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
                Messages.showInfoMessage(
                        project,
                        "You are not logged in.",
                        "Not Logged In"
                );
                return;
            }
            
            // Confirm logout
            int option = Messages.showYesNoDialog(
                    project,
                    "Are you sure you want to log out?",
                    "Confirm Logout",
                    Messages.getQuestionIcon()
            );
            
            if (option != Messages.YES) {
                return;
            }
            
            // Logout
            String username = authManager.getUsername();
            authManager.logout();
            
            Messages.showInfoMessage(
                    project,
                    "Successfully logged out" + (username != null ? " from " + username : ""),
                    "Logout Successful"
            );
        } catch (Exception ex) {
            LOG.error("Error in logout action", ex);
            
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
        
        e.getPresentation().setEnabled(authManager.isAuthenticated());
    }
}