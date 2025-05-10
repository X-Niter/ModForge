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
 * Action for verifying authentication status with the ModForge server.
 */
public class VerifyAuthenticationAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Check if credentials are saved
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.getUsername().isEmpty() || settings.getPassword().isEmpty()) {
            showNotification(project, "Authentication Missing", 
                    "No credentials found. Please set your username and password in the ModForge settings.", 
                    NotificationType.WARNING);
            return;
        }
        
        // Start verification
        showNotification(project, "Verifying Authentication", 
                "Verifying authentication with ModForge server...", 
                NotificationType.INFORMATION);
        
        // Run in a background thread
        new Thread(() -> {
            boolean isValid = AuthenticationManager.getInstance().verifyAuthentication();
            
            if (isValid) {
                showNotification(project, "Authentication Valid", 
                        "Your authentication with ModForge server is valid.", 
                        NotificationType.INFORMATION);
            } else {
                showNotification(project, "Authentication Invalid", 
                        "Your authentication with ModForge server is no longer valid. Please log in again.", 
                        NotificationType.WARNING);
            }
        }).start();
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when a project is available and user is authenticated
        ModForgeSettings settings = ModForgeSettings.getInstance();
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && settings.isAuthenticated());
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