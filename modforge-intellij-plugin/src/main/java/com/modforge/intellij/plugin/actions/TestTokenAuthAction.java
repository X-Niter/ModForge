package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
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
        
        // Use the AuthTestUtil to test token-based authentication
        String testResults = AuthTestUtil.testTokenAuthentication();
        
        // Show dialog with results
        Messages.showInfoMessage(project, testResults, "Token Authentication Test Results");
        
        LOG.info("Completed testing token authentication");
        
        showNotification(project, "Token Authentication Test Completed", 
                "Token-based authentication test completed. Check the dialog for results.", 
                NotificationType.INFORMATION);
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