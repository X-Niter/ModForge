package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle pattern recognition.
 */
public class TogglePatternRecognitionAction extends ToggleAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.isPatternRecognition();
    }
    
    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setPatternRecognition(state);
        
        // Log the change
        if (state) {
            LOG.info("Enabling pattern recognition");
            
            // Get pattern recognition service and force a sync
            PatternRecognitionService service = project.getService(PatternRecognitionService.class);
            service.forceSyncMetrics();
        } else {
            LOG.info("Disabling pattern recognition");
        }
        
        // Update presentation
        String text = state ? "Disable Pattern Recognition" : "Enable Pattern Recognition";
        e.getPresentation().setText(text);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        
        // Update text based on current state
        boolean selected = isSelected(e);
        String text = selected ? "Disable Pattern Recognition" : "Enable Pattern Recognition";
        e.getPresentation().setText(text);
    }
}