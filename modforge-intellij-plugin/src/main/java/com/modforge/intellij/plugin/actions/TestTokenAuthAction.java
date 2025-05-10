package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Action to test token-based authentication with direct HTTP requests.
 * This provides a more comprehensive test than the verification action.
 */
public class TestTokenAuthAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TestTokenAuthAction.class);
    
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
        
        // Test basic token auth
        boolean tokenWorks = TokenAuthConnectionUtil.testTokenAuthentication();
        
        if (!tokenWorks) {
            showNotification(project, "Token Authentication Failed", 
                    "Your token is not valid or the server is not responding. Please log in again.", 
                    NotificationType.ERROR);
            return;
        }
        
        // Test specific endpoints and build a report
        StringBuilder results = new StringBuilder("Token Authentication Test Results:\n\n");
        
        // Test user endpoint
        String userResponse = TokenAuthConnectionUtil.makeAuthenticatedGetRequest("/api/user");
        results.append("GET /api/user:\n")
               .append(userResponse != null ? "Success" : "Failed")
               .append("\n");
        
        if (userResponse != null) {
            results.append("Response: ").append(formatResponse(userResponse)).append("\n\n");
        } else {
            results.append("No response received\n\n");
        }
        
        // Test auth/me endpoint
        String authMeResponse = TokenAuthConnectionUtil.makeAuthenticatedGetRequest("/api/auth/me");
        results.append("GET /api/auth/me:\n")
               .append(authMeResponse != null ? "Success" : "Failed")
               .append("\n");
        
        if (authMeResponse != null) {
            results.append("Response: ").append(formatResponse(authMeResponse)).append("\n\n");
        } else {
            results.append("No response received\n\n");
        }
        
        // Show dialog with results
        Messages.showInfoMessage(project, results.toString(), "Token Authentication Test Results");
        
        LOG.info("Completed testing token authentication");
        
        showNotification(project, "Token Authentication Test Completed", 
                "Token-based authentication test completed. Check the dialog for results.", 
                NotificationType.INFORMATION);
    }
    
    /**
     * Format response for display - trim if too long.
     */
    private String formatResponse(String response) {
        if (response.length() > 200) {
            return response.substring(0, 197) + "...";
        }
        return response;
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