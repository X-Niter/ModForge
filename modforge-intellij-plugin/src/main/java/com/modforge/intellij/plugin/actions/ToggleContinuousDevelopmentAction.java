package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle continuous development.
 */
public class ToggleContinuousDevelopmentAction extends ToggleAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.isContinuousDevelopment();
    }
    
    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setContinuousDevelopment(state);
        
        // Get continuous development service
        ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
        
        if (state) {
            LOG.info("Enabling continuous development");
            service.start();
        } else {
            LOG.info("Disabling continuous development");
            service.stop();
        }
        
        // Update presentation
        String text = state ? "Disable Continuous Development" : "Enable Continuous Development";
        e.getPresentation().setText(text);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        
        // Update text based on current state
        boolean selected = isSelected(e);
        String text = selected ? "Disable Continuous Development" : "Enable Continuous Development";
        e.getPresentation().setText(text);
    }
}