package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling continuous development mode.
 */
public class ToggleContinuousDevelopmentAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Toggling continuous development for project: " + project.getName());
        
        // Get continuous development service
        ContinuousDevelopmentService continuousDevelopmentService = 
                project.getService(ContinuousDevelopmentService.class);
        
        if (continuousDevelopmentService == null) {
            LOG.error("Could not get ContinuousDevelopmentService for project: " + project.getName());
            return;
        }
        
        // Toggle continuous development
        boolean isRunning = continuousDevelopmentService.toggle();
        
        // Update action presentation
        Presentation presentation = e.getPresentation();
        presentation.setText((isRunning ? "Disable" : "Enable") + " Continuous Development");
        
        // Show notification
        String message = "Continuous development " + (isRunning ? "enabled" : "disabled") + 
                " for project: " + project.getName();
        
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Continuous Development",
                        message,
                        isRunning ? NotificationType.INFORMATION : NotificationType.WARNING)
                .notify(project);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // Only enable action if we have a project and continuous development is enabled in settings
        boolean enabled = false;
        boolean isRunning = false;
        
        if (project != null) {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            enabled = settings.isEnableContinuousDevelopment();
            
            ContinuousDevelopmentService continuousDevelopmentService = 
                    project.getService(ContinuousDevelopmentService.class);
            
            if (continuousDevelopmentService != null) {
                isRunning = continuousDevelopmentService.isRunning();
            }
        }
        
        e.getPresentation().setEnabledAndVisible(project != null);
        e.getPresentation().setText((isRunning ? "Disable" : "Enable") + " Continuous Development");
    }
}