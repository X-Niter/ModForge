package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to verify authentication with ModForge server.
 */
public class VerifyAuthenticationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(VerifyAuthenticationAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        LOG.info("Verifying authentication with ModForge server");
        
        // Get authentication manager
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Verify authentication
        boolean isValid = authManager.verifyAuthentication();
        
        if (isValid) {
            LOG.info("Authentication verification successful");
            
            // Get user info
            String username = authManager.getUsername();
            
            // Notify user
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("ModForge Notifications")
                    .createNotification(
                            "Authentication Verified",
                            "Successfully verified authentication as " + username,
                            NotificationType.INFORMATION)
                    .notify(project);
        } else {
            LOG.info("Authentication verification failed");
            
            // Notify user
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("ModForge Notifications")
                    .createNotification(
                            "Authentication Failed",
                            "Failed to verify authentication. Please log in again.",
                            NotificationType.ERROR)
                    .notify(project);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if we have a project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}