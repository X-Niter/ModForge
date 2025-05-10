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
        
        // Perform logout using non-singleton AuthenticationManager
        AuthenticationManager authManager = new AuthenticationManager();
        boolean success = authManager.logout();
        
        if (success) {
            // Clear credentials if not set to remember
            ModForgeSettings settings = ModForgeSettings.getInstance();
            if (!settings.isRememberCredentials()) {
                settings.setUsername("");
                settings.setPassword("");
            }
            
            showNotification(project, "Logout Successful", 
                    "You have been logged out from the ModForge server.", 
                    NotificationType.INFORMATION);
        } else {
            showNotification(project, "Logout Failed", 
                    "Failed to log out from the ModForge server.", 
                    NotificationType.ERROR);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when the user is authenticated
        ModForgeSettings settings = ModForgeSettings.getInstance();
        e.getPresentation().setEnabledAndVisible(settings.isAuthenticated());
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