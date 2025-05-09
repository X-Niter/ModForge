package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle continuous development.
 */
public class ToggleContinuousDevelopmentAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean enabled = settings.isContinuousDevelopmentEnabled();
        
        // Toggle setting
        settings.setContinuousDevelopmentEnabled(!enabled);
        
        // Get service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        if (service == null) {
            Messages.showErrorDialog(
                    project,
                    "Continuous development service not available.",
                    "Toggle Continuous Development"
            );
            return;
        }
        
        // Start or stop service
        if (!enabled) {
            service.start();
            Messages.showInfoDialog(
                    project,
                    "Continuous development has been enabled.\n\n" +
                            "The system will now automatically monitor for errors and fix them.",
                    "Continuous Development Enabled"
            );
        } else {
            service.stop();
            Messages.showInfoDialog(
                    project,
                    "Continuous development has been disabled.",
                    "Continuous Development Disabled"
            );
        }
        
        // Update presentation
        update(e);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Get service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        if (service == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Update text based on current state
        Presentation presentation = e.getPresentation();
        boolean running = service.isRunning();
        
        presentation.setText(running ? "Disable Continuous Development" : "Enable Continuous Development");
        presentation.setDescription(running ?
                "Disable automatic error fixing" :
                "Enable automatic error fixing");
        
        presentation.setEnabledAndVisible(true);
    }
}