package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Action to test all authentication endpoints with token-based authentication.
 * This is primarily for development and debugging purposes.
 */
public class TestAuthEndpointsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TestAuthEndpointsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance(project);
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            showNotification(project, "Not Authenticated", 
                    "You are not authenticated with ModForge. Please log in first.", 
                    NotificationType.WARNING);
            return;
        }
        
        // Build a message with results from all endpoints
        StringBuilder results = new StringBuilder("Authentication Test Results:\n\n");
        
        // Test each endpoint
        for (AuthTestUtil.Endpoint endpoint : AuthTestUtil.Endpoint.values()) {
            AuthTestUtil.TestResult result = AuthTestUtil.testEndpoint(endpoint);
            
            results.append(endpoint.getPath())
                  .append(" - ")
                  .append(result.isSuccess() ? "Success" : "Failed")
                  .append(" (").append(result.getResponseCode()).append(")")
                  .append("\n");
            
            // Limit response size for display
            String response = result.getResponse();
            if (response.length() > 100) {
                response = response.substring(0, 97) + "...";
            }
            
            results.append("Response: ").append(response).append("\n\n");
        }
        
        // Show dialog with all results
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        if (notificationService != null) {
            notificationService.showInfoMessage(
                    project,
                    "Authentication Test Results",
                    results.toString()
            );
        } else {
            CompatibilityUtil.showInfoDialog(project, results.toString(), "Authentication Test Results");
        }
        
        LOG.info("Completed testing authentication endpoints");
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