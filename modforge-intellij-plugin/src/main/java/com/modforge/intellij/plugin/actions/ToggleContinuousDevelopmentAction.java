package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle action to enable/disable continuous development.
 * This action toggles the continuous development service when invoked.
 */
public final class ToggleContinuousDevelopmentAction extends ToggleAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return false;
        }
        
        // Get continuous development service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        // Return current state
        return service.isRunning();
    }
    
    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Get continuous development service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        // Update state
        if (state) {
            LOG.info("Starting continuous development service");
            service.start();
        } else {
            LOG.info("Stopping continuous development service");
            service.stop();
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // Enable action if project is open
        e.getPresentation().setEnabledAndVisible(project != null);
        
        super.update(e);
    }
}