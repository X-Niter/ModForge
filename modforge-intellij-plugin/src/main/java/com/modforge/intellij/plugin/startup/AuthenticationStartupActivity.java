package com.modforge.intellij.plugin.startup;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Activity that runs at IDE startup to verify authentication with ModForge server.
 */
public class AuthenticationStartupActivity implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(AuthenticationStartupActivity.class);
    private final AuthenticationManager authManager = new AuthenticationManager();
    
    @Override
    public void runActivity(@NotNull Project project) {
        // Check if we have saved credentials and verify them
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (settings.isAuthenticated() && !settings.getAccessToken().isEmpty()) {
            LOG.info("Verifying saved authentication on startup");
            
            if (authManager.verifyAuthentication()) {
                LOG.info("Successfully verified authentication on startup");
                showNotification(
                        project,
                        "ModForge Authentication",
                        "Successfully connected to ModForge server.",
                        NotificationType.INFORMATION
                );
            } else {
                LOG.warn("Failed to verify authentication on startup");
                showNotification(
                        project,
                        "ModForge Authentication Failed",
                        "Your ModForge authentication has expired. Please log in again.",
                        NotificationType.WARNING
                );
                
                // Clear authentication state
                settings.setAuthenticated(false);
                settings.setAccessToken("");
                settings.setUserId("");
            }
        }
    }
    
    private void showNotification(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }
}