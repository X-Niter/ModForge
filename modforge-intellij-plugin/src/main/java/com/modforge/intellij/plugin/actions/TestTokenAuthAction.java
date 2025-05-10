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
 * Action to test token authentication with ModForge server.
 */
public class TestTokenAuthAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TestTokenAuthAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        LOG.info("Testing token authentication with ModForge server");
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getAccessToken();
        
        if (token.isEmpty()) {
            notifyError(project, "No access token found. Please log in first.");
            return;
        }
        
        // Test authentication
        boolean isAuthenticated = AuthTestUtil.verifyAuthentication(settings.getServerUrl(), token);
        
        if (isAuthenticated) {
            notifySuccess(project, "Token authentication successful!");
        } else {
            notifyError(project, "Token authentication failed. Please log in again.");
        }
    }
    
    private void notifySuccess(Project project, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Token Authentication Test",
                        message,
                        NotificationType.INFORMATION)
                .notify(project);
    }
    
    private void notifyError(Project project, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Token Authentication Test",
                        message,
                        NotificationType.ERROR)
                .notify(project);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if we have a project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}