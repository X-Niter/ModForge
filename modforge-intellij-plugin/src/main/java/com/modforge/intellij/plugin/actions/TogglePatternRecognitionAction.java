package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle pattern recognition.
 * This action enables or disables the pattern recognition service.
 */
public final class TogglePatternRecognitionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("Toggle pattern recognition action performed");
        
        // Get pattern recognition service
        PatternRecognitionService service = PatternRecognitionService.getInstance();
        
        // Toggle pattern recognition
        service.setEnabled(!service.isEnabled());
        
        // Update presentation
        updatePresentation(e.getPresentation(), service.isEnabled());
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Show action always
        e.getPresentation().setEnabledAndVisible(true);
        
        // Get pattern recognition service
        PatternRecognitionService service = PatternRecognitionService.getInstance();
        
        // Update presentation
        updatePresentation(e.getPresentation(), service.isEnabled());
    }
    
    /**
     * Updates the action presentation.
     * @param presentation The presentation to update
     * @param isEnabled Whether pattern recognition is enabled
     */
    private void updatePresentation(@NotNull Presentation presentation, boolean isEnabled) {
        if (isEnabled) {
            presentation.setText("Disable Pattern Recognition");
            presentation.setDescription("Disable pattern recognition to reduce API usage");
        } else {
            presentation.setText("Enable Pattern Recognition");
            presentation.setDescription("Enable pattern recognition to reduce API usage");
        }
    }
}