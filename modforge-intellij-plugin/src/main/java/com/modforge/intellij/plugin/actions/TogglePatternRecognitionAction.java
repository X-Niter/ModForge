package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean newState = !settings.isPatternRecognitionEnabled();
        settings.setPatternRecognitionEnabled(newState);
        
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
        
        boolean enabled = ModForgeSettings.getInstance().isPatternRecognitionEnabled();
        
        e.getPresentation().setText(getActionText(enabled));
        e.getPresentation().setDescription(getActionDescription(enabled));
        e.getPresentation().setEnabledAndVisible(true);
    }
    
    /**
     * Gets the action text based on the current state.
     * @param enabled Whether pattern recognition is enabled
     * @return The action text
     */
    private String getActionText(boolean enabled) {
        return enabled ? "Disable Pattern Recognition" : "Enable Pattern Recognition";
    }
    
    /**
     * Gets the action description based on the current state.
     * @param enabled Whether pattern recognition is enabled
     * @return The action description
     */
    private String getActionDescription(boolean enabled) {
        return enabled
                ? "Disable learning patterns from AI interactions"
                : "Enable learning patterns from AI interactions to reduce API costs";
    }
}