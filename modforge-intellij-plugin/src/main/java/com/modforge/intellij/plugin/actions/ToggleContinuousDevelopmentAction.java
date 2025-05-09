package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling continuous development mode.
 */
public class ToggleContinuousDevelopmentAction extends AnAction implements Toggleable {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Toggle continuous development action performed");
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Toggle continuous development
        boolean newValue = !settings.isContinuousDevelopmentEnabled();
        settings.setContinuousDevelopmentEnabled(newValue);
        
        // Get continuous development service
        ContinuousDevelopmentService continuousDevService = ContinuousDevelopmentService.getInstance(project);
        
        if (newValue) {
            // Start continuous development
            continuousDevService.start();
            
            Messages.showInfoMessage(
                    project,
                    "Continuous development is now enabled. Your code will be automatically compiled, errors will be fixed, and improvements will be suggested.",
                    "Continuous Development Enabled"
            );
        } else {
            // Stop continuous development
            continuousDevService.stop();
            
            Messages.showInfoMessage(
                    project,
                    "Continuous development is now disabled.",
                    "Continuous Development Disabled"
            );
        }
        
        // Update presentation
        Toggleable.setSelected(e.getPresentation(), newValue);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        
        // Enable only if project is not null
        e.getPresentation().setEnabled(project != null);
        
        // Update selected state
        ModForgeSettings settings = ModForgeSettings.getInstance();
        Toggleable.setSelected(e.getPresentation(), settings.isContinuousDevelopmentEnabled());
    }
}