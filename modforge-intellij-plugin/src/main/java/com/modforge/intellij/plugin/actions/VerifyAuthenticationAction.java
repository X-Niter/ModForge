package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Action to verify authentication with ModForge server.
 */
public class VerifyAuthenticationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(VerifyAuthenticationAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        LOG.info("Verifying authentication with ModForge server");
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Check if we have token authentication
        if (settings.getAccessToken().isEmpty()) {
            notifyError(project, "No access token found. Please log in first.");
            return;
        }
        
        // Verify authentication
        try {
            boolean isAuthenticated = AuthTestUtil.verifyAuthentication(settings.getServerUrl(), settings.getAccessToken());
            
            if (isAuthenticated) {
                notifySuccess(project, "Successfully authenticated with ModForge server.");
            } else {
                notifyError(project, "Authentication failed. Please log in again.");
                settings.setAuthenticated(false);
            }
        } catch (Exception ex) {
            LOG.error("Error verifying authentication", ex);
            notifyError(project, "Error verifying authentication: " + ex.getMessage());
        }
    }
    
    private void notifySuccess(Project project, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Authentication Verification",
                        message,
                        NotificationType.INFORMATION)
                .notify(project);
    }
    
    private void notifyError(Project project, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Authentication Verification",
                        message,
                        NotificationType.ERROR)
                .notify(project);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Only enable if we have a project and access token
        e.getPresentation().setEnabledAndVisible(project != null && !settings.getAccessToken().isEmpty());
    }
}