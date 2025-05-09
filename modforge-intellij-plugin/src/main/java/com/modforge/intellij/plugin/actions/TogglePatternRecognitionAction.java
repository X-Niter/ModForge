package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle action to enable/disable pattern recognition.
 * This action toggles the pattern recognition service when invoked.
 */
public final class TogglePatternRecognitionAction extends ToggleAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        // Get pattern recognition service
        PatternRecognitionService service = PatternRecognitionService.getInstance();
        
        // Return current state
        return service.isEnabled();
    }
    
    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        // Get pattern recognition service
        PatternRecognitionService service = PatternRecognitionService.getInstance();
        
        // Update state
        if (state) {
            LOG.info("Enabling pattern recognition service");
            service.setEnabled(true);
        } else {
            LOG.info("Disabling pattern recognition service");
            service.setEnabled(false);
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