package com.modforge.intellij.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.ui.dialog.LoginDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging in to the ModForge server.
 */
public class LoginAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // Show login dialog
        LoginDialog dialog = new LoginDialog(project);
        if (dialog.showAndGet()) {
            // User clicked OK, and authentication is already handled in the dialog
            showNotification(project, "Authentication Successful", 
                    "You have been authenticated with the ModForge server.", 
                    NotificationType.INFORMATION);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when the user is not authenticated
        ModForgeSettings settings = ModForgeSettings.getInstance();
        e.getPresentation().setEnabledAndVisible(!settings.isAuthenticated());
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