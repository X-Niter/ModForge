package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to logout from ModForge server.
 */
public class LogoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LogoutAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        LOG.info("Logging out from ModForge server");
        
        // Log out using authentication manager
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        authManager.logout();
        
        // Notify user
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Logout",
                        "Successfully logged out from ModForge server",
                        NotificationType.INFORMATION)
                .notify(project);
        
        LOG.info("Logout successful");
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Only enable if we have a project and are authenticated
        e.getPresentation().setEnabledAndVisible(project != null && authManager.isAuthenticated());
    }
}