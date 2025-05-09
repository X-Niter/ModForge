package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Action to toggle pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean enabled = settings.isPatternRecognitionEnabled();
        
        // Toggle setting
        settings.setPatternRecognitionEnabled(!enabled);
        
        // Get service
        PatternRecognitionService service = PatternRecognitionService.getInstance();
        
        if (service == null) {
            Messages.showErrorDialog(
                    project,
                    "Pattern recognition service not available.",
                    "Toggle Pattern Recognition"
            );
            return;
        }
        
        // Show status message
        if (!enabled) {
            // Get statistics
            Map<String, Object> statistics = service.getStatistics();
            int totalRequests = (int) statistics.getOrDefault("totalRequests", 0);
            int patternMatches = (int) statistics.getOrDefault("patternMatches", 0);
            double estimatedCostSaved = (double) statistics.getOrDefault("estimatedCostSaved", 0.0);
            
            Messages.showInfoDialog(
                    project,
                    "Pattern recognition has been enabled.\n\n" +
                            "This feature reduces API costs by caching similar requests.\n\n" +
                            "Statistics:\n" +
                            "Total Requests: " + totalRequests + "\n" +
                            "Pattern Matches: " + patternMatches + "\n" +
                            "Estimated Cost Saved: $" + String.format("%.2f", estimatedCostSaved),
                    "Pattern Recognition Enabled"
            );
        } else {
            Messages.showInfoDialog(
                    project,
                    "Pattern recognition has been disabled.\n\n" +
                            "All requests will now go directly to the API.",
                    "Pattern Recognition Disabled"
            );
        }
        
        // Update presentation
        update(e);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean enabled = settings.isPatternRecognitionEnabled();
        
        // Update text based on current state
        Presentation presentation = e.getPresentation();
        
        presentation.setText(enabled ? "Disable Pattern Recognition" : "Enable Pattern Recognition");
        presentation.setDescription(enabled ?
                "Disable pattern recognition to reduce API costs" :
                "Enable pattern recognition to reduce API costs");
        
        presentation.setEnabledAndVisible(true);
    }
}