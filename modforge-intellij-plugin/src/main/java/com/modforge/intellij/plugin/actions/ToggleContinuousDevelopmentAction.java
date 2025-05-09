package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle continuous development.
 */
public class ToggleContinuousDevelopmentAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        boolean newState = !settings.isContinuousDevelopmentEnabled();
        settings.setContinuousDevelopmentEnabled(newState);
        
        if (newState) {
            service.start();
        } else {
            service.stop();
        }
        
        e.getPresentation().setText(getActionText(newState));
        e.getPresentation().setDescription(getActionDescription(newState));
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        boolean enabled = ModForgeSettings.getInstance().isContinuousDevelopmentEnabled();
        
        e.getPresentation().setText(getActionText(enabled));
        e.getPresentation().setDescription(getActionDescription(enabled));
        e.getPresentation().setEnabledAndVisible(true);
    }
    
    /**
     * Gets the action text based on the current state.
     * @param enabled Whether continuous development is enabled
     * @return The action text
     */
    private String getActionText(boolean enabled) {
        return enabled ? "Disable Continuous Development" : "Enable Continuous Development";
    }
    
    /**
     * Gets the action description based on the current state.
     * @param enabled Whether continuous development is enabled
     * @return The action description
     */
    private String getActionDescription(boolean enabled) {
        return enabled
                ? "Disable automatic error fixing and feature addition"
                : "Enable automatic error fixing and feature addition";
    }
}