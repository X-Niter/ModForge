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
        
        try {
            // Confirm logout
            int result = Messages.showYesNoDialog(
                    project,
                    "Are you sure you want to log out from ModForge?",
                    "Confirm Logout",
                    Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                // Log out
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                authManager.logout();
                
                LOG.info("User logged out successfully");
                
                // Show confirmation
                Messages.showInfoMessage(
                        project,
                        "You have been logged out from ModForge.",
                        "Logged Out"
                );
            }
        } catch (Exception ex) {
            LOG.error("Error during logout action", ex);
            
            // Show error
            Messages.showErrorDialog(
                    project,
                    "An error occurred while logging out: " + ex.getMessage(),
                    "Logout Error"
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