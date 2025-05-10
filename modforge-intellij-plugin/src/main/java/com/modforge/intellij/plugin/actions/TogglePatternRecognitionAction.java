package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        LOG.info("Toggling pattern recognition");
        
        // Get pattern recognition service
        PatternRecognitionService patternRecognitionService = 
                PatternRecognitionService.getInstance();
        
        if (patternRecognitionService == null) {
            LOG.error("Could not get PatternRecognitionService");
            return;
        }
        
        // Toggle pattern recognition
        boolean isEnabled = patternRecognitionService.toggle();
        
        // Update action presentation
        Presentation presentation = e.getPresentation();
        presentation.setText((isEnabled ? "Disable" : "Enable") + " Pattern Recognition");
        
        // Show notification
        String message = "Pattern recognition " + (isEnabled ? "enabled" : "disabled");
        
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(
                        "Pattern Recognition",
                        message,
                        isEnabled ? NotificationType.INFORMATION : NotificationType.WARNING)
                .notify(project);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Get pattern recognition service
        PatternRecognitionService patternRecognitionService = 
                PatternRecognitionService.getInstance();
        
        boolean isEnabled = false;
        if (patternRecognitionService != null) {
            isEnabled = patternRecognitionService.isEnabled();
        }
        
        e.getPresentation().setText((isEnabled ? "Disable" : "Enable") + " Pattern Recognition");
    }
}