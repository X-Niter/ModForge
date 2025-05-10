package com.modforge.intellij.plugin.startup;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Activity that runs on project startup to verify authentication status.
 * This ensures the token is still valid and notifies users if authentication has expired.
 */
public class AuthenticationStartupActivity implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(AuthenticationStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Running ModForge authentication verification on startup");
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Skip verification if not authenticated
        if (!settings.isAuthenticated()) {
            LOG.info("Skipping authentication verification - not authenticated");
            return;
        }
        
        // Skip verification if server URL or token is empty
        if (settings.getServerUrl().isEmpty() || settings.getAccessToken().isEmpty()) {
            LOG.info("Skipping authentication verification - missing server URL or token");
            settings.setAuthenticated(false); // Reset authenticated state
            return;
        }
        
        // Verify token authentication in background thread to avoid freezing the UI
        Thread verificationThread = new Thread(() -> {
            try {
                boolean isValid = TokenAuthConnectionUtil.testTokenAuthentication();
                
                if (!isValid) {
                    LOG.warn("Authentication token is no longer valid");
                    settings.setAuthenticated(false);
                    
                    // Show notification
                    NotificationGroupManager.getInstance()
                            .getNotificationGroup("ModForge Notifications")
                            .createNotification(
                                    "ModForge Authentication Expired",
                                    "Your ModForge authentication has expired. Please log in again.",
                                    NotificationType.WARNING)
                            .notify(project);
                } else {
                    LOG.info("Authentication token is valid");
                }
            } catch (Exception e) {
                LOG.warn("Error verifying authentication on startup", e);
                // Don't change authentication state on connection error
                // as it might be temporary
            }
        });
        
        // Set thread as daemon to not prevent IDE from exiting
        verificationThread.setDaemon(true);
        verificationThread.setName("ModForge-Auth-Verification");
        verificationThread.start();
    }
}