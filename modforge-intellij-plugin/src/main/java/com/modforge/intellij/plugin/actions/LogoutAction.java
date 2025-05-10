package com.modforge.intellij.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging out from the ModForge server.
 */
public class LogoutAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Check if authenticated
        if (!ModForgeSettings.getInstance().isAuthenticated()) {
            showNotification(project, "Not logged in", "You are not currently logged in to ModForge.", NotificationType.INFORMATION);
            return;
        }
        
        // Perform logout
        boolean success = AuthenticationManager.getInstance().logout();
        
        if (success) {
            showNotification(project, "Logout successful", "You have been logged out from ModForge.", NotificationType.INFORMATION);
        } else {
            showNotification(project, "Logout failed", "Failed to log out from ModForge. Please try again.", NotificationType.ERROR);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable action if authenticated
        boolean isAuthenticated = ModForgeSettings.getInstance().isAuthenticated();
        e.getPresentation().setEnabled(isAuthenticated);
        e.getPresentation().setVisible(true);
    }
    
    /**
     * Shows a notification.
     * @param project The project
     * @param title The notification title
     * @param content The notification content
     * @param type The notification type
     */
    private void showNotification(Project project, String title, String content, NotificationType type) {
        Notification notification = new Notification(
                "ModForge.Authentication",
                title,
                content,
                type
        );
        
        Notifications.Bus.notify(notification, project);
    }
}