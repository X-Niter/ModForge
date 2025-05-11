package com.modforge.intellij.plugin.memory.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.MemoryAwareContinuousService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle the memory-aware continuous service
 */
public class ToggleMemoryAwareContinuousServiceAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        MemoryAwareContinuousService service = project.getService(MemoryAwareContinuousService.class);
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
        Presentation presentation = e.getPresentation();
        
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        
        presentation.setEnabled(true);
        
        MemoryAwareContinuousService service = project.getService(MemoryAwareContinuousService.class);
        updatePresentation(presentation, service.isRunning());
    }
    
    /**
     * Update the action presentation based on the service state
     * 
     * @param presentation The action presentation
     * @param isRunning Whether the service is running
     */
    private void updatePresentation(@NotNull Presentation presentation, boolean isRunning) {
        if (isRunning) {
            presentation.setText("Stop Memory-Aware Continuous Service");
            presentation.setDescription("Stop the memory-aware continuous service");
        } else {
            presentation.setText("Start Memory-Aware Continuous Service");
            presentation.setDescription("Start the memory-aware continuous service");
        }
    }
}