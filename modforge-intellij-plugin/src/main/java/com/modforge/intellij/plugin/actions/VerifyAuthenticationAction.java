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
 * Action to verify authentication with the ModForge server.
 */
public class VerifyAuthenticationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(VerifyAuthenticationAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            showNotification(project, "Not Authenticated", 
                    "You are not authenticated with ModForge. Please log in first.", 
                    NotificationType.WARNING);
            return;
        }
        
        LOG.info("Verifying authentication status with ModForge server");
        
        // Create a non-singleton AuthenticationManager instance
        AuthenticationManager authManager = new AuthenticationManager();
        boolean isValid = authManager.verifyAuthentication();
        
        if (isValid) {
            showNotification(project, "Authentication Valid", 
                    "Your authentication with ModForge is valid.", 
                    NotificationType.INFORMATION);
        } else {
            showNotification(project, "Authentication Invalid", 
                    "Your authentication with ModForge is no longer valid. Please log in again.", 
                    NotificationType.ERROR);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable action if we have a project
        Project project = e.getProject();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        boolean enabled = project != null && settings.isAuthenticated();
        e.getPresentation().setEnabledAndVisible(enabled);
    }
    
    private void showNotification(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }
}