package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle continuous development mode.
 * This action enables or disables the continuous development service.
 */
public final class ToggleContinuousDevelopmentAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("Toggle continuous development action performed");
        
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Get continuous development service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        // Toggle continuous development
        if (service.isRunning()) {
            service.stop();
        } else {
            service.start();
        }
        
        // Update presentation
        updatePresentation(e.getPresentation(), service.isRunning());
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Show action when project is open
        e.getPresentation().setEnabledAndVisible(true);
        
        // Get continuous development service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        // Update presentation
        updatePresentation(e.getPresentation(), service.isRunning());
    }
    
    /**
     * Updates the action presentation.
     * @param presentation The presentation to update
     * @param isRunning Whether continuous development is running
     */
    private void updatePresentation(@NotNull Presentation presentation, boolean isRunning) {
        if (isRunning) {
            presentation.setText("Disable Continuous Development");
            presentation.setDescription("Disable continuous development mode");
        } else {
            presentation.setText("Enable Continuous Development");
            presentation.setDescription("Enable continuous development mode");
        }
    }
}