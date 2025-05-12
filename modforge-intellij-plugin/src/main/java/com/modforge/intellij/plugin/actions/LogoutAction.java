package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging out from ModForge.
 */
public class LogoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LogoutAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Enable action only if the user is authenticated
        e.getPresentation().setEnabled(authManager.isAuthenticated());
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Make sure the user is authenticated
        if (!authManager.isAuthenticated()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showInfoDialog(
                        project,
                        "Logout",
                        "You are not currently logged in."
                );
            } else {
                CompatibilityUtil.showInfoDialog(
                        project,
                        "You are not currently logged in.",
                        "Logout"
                );
            }
            return;
        }
        
        // Ask for confirmation
        String username = authManager.getUsername();
        int result;
        
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        if (notificationService != null) {
            result = notificationService.showYesNoDialog(
                    project,
                    "Are you sure you want to log out from ModForge? This will stop all services that require authentication.",
                    "Confirm Logout",
                    "Logout",
                    "Cancel",
                    null
            );
        } else {
            result = Messages.showYesNoDialog(
                    project,
                    "Are you sure you want to log out from ModForge? This will stop all services that require authentication.",
                    "Confirm Logout",
                    "Logout",
                    "Cancel",
                    null
            );
        }
        
        if (result == Messages.YES) {
            // Stop continuous development if it's running
            stopContinuousDevelopment(project);
            
            // Logout - handle returned CompletableFuture
            authManager.logout().thenAccept(success -> {
                if (notificationService != null) {
                    if (success) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            notificationService.showInfoDialog(
                                    project,
                                    "Logout Successful",
                                    "You have been logged out from ModForge." +
                                            (username.isEmpty() ? "" : " (Goodbye, " + username + "!)")
                            );
                        });
                    } else {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            notificationService.showErrorDialog(
                                    project,
                                    "Logout Failed",
                                    "Failed to log out from ModForge."
                            );
                        });
                    }
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        CompatibilityUtil.showInfoDialog(
                                project,
                                "You have been logged out from ModForge." +
                                        (username.isEmpty() ? "" : " (Goodbye, " + username + "!)"),
                                "Logout Successful"
                        );
                    });
                }
            });
        }
    }
    
    /**
     * Stop continuous development if it's running.
     *
     * @param project The project
     */
    private void stopContinuousDevelopment(Project project) {
        if (project == null) {
            return;
        }
        
        ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
        if (continuousService != null && continuousService.isRunning()) {
            continuousService.stop();
            
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showInfoDialog(
                        project,
                        "Continuous Development",
                        "Continuous development has been stopped due to logout."
                );
            } else {
                CompatibilityUtil.showInfoDialog(
                        project,
                        "Continuous development has been stopped due to logout.",
                        "Continuous Development"
                );
            }
        }
    }
}