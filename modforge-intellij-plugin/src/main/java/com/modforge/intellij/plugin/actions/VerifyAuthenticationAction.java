package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action to verify the authentication status with the ModForge server.
 * Useful for debugging and ensuring the token-based authentication is working.
 */
public class VerifyAuthenticationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(VerifyAuthenticationAction.class);
    private final AuthenticationManager authManager = new AuthenticationManager();
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            showNotification(project, "Not Authenticated", 
                    "You are not currently authenticated with ModForge. Please log in first.", 
                    NotificationType.WARNING);
            return;
        }
        
        // Attempt to verify authentication
        boolean isVerified = authManager.verifyAuthentication();
        
        if (isVerified) {
            showNotification(project, "Authentication Verified", 
                    "Successfully verified authentication with ModForge server. User ID: " + settings.getUserId(), 
                    NotificationType.INFORMATION);
        } else {
            showNotification(project, "Authentication Failed", 
                    "Failed to verify authentication with ModForge server. Please try logging in again.", 
                    NotificationType.ERROR);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable action if we have a project
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
    
    private void showNotification(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }
}