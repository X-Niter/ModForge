package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Action for toggling pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction implements Toggleable {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Toggle pattern recognition action performed");
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Toggle pattern recognition
        boolean newValue = !settings.isPatternRecognitionEnabled();
        settings.setPatternRecognitionEnabled(newValue);
        
        if (newValue) {
            Messages.showInfoMessage(
                    project,
                    "Pattern recognition is now enabled. ModForge will learn from your interactions to reduce API usage.",
                    "Pattern Recognition Enabled"
            );
        } else {
            Messages.showInfoMessage(
                    project,
                    "Pattern recognition is now disabled. All API requests will be sent directly to OpenAI.",
                    "Pattern Recognition Disabled"
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
        Toggleable.setSelected(e.getPresentation(), settings.isPatternRecognitionEnabled());
    }
}