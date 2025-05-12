package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Action for toggling pattern recognition.
 */
public class TogglePatternRecognitionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TogglePatternRecognitionAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Presentation presentation = e.getPresentation();
        
        // Disable action if there's no project
        if (project == null) {
            presentation.setEnabled(false);
            presentation.setText("Toggle Pattern Recognition");
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            presentation.setEnabled(false);
            presentation.setText("Login to Use Pattern Recognition");
            return;
        }
        
        // Update text based on whether pattern recognition is enabled
        PatternRecognitionService service = project.getService(PatternRecognitionService.class);
        boolean enabled = service != null && service.isEnabled();
        
        presentation.setEnabled(true);
        presentation.setText(enabled ? "Disable Pattern Recognition" : "Enable Pattern Recognition");
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Authentication Required",
                        "You must be logged in to use pattern recognition."
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to use pattern recognition.",
                        "Authentication Required"
                );
            }
            return;
        }
        
        // Toggle pattern recognition
        PatternRecognitionService service = project.getService(PatternRecognitionService.class);
        if (service != null) {
            boolean currentState = service.isEnabled();
            
            if (currentState) {
                // Show confirmation dialog before disabling
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                int result;
                if (notificationService != null) {
                    result = notificationService.showYesNoDialog(
                            project,
                            "Disable Pattern Recognition",
                            "Are you sure you want to disable pattern recognition? This will increase API usage and costs.",
                            "Disable",
                            "Cancel"
                    );
                } else {
                    result = Messages.showYesNoDialog(
                            project,
                            "Are you sure you want to disable pattern recognition? This will increase API usage and costs.",
                            "Disable Pattern Recognition",
                            "Disable",
                            "Cancel",
                            null
                    );
                }
                
                if (result == Messages.YES) {
                    service.setEnabled(false);
                    
                    ModForgeSettings settings = ModForgeSettings.getInstance();
                    settings.setEnablePatternRecognition(false);
                    
                    if (notificationService != null) {
                        notificationService.showInfoDialog(
                                project,
                                "Pattern Recognition",
                                "Pattern recognition has been disabled."
                        );
                    } else {
                        Messages.showInfoMessage(
                                project,
                                "Pattern recognition has been disabled.",
                                "Pattern Recognition"
                        );
                    }
                }
            } else {
                // Enable pattern recognition
                service.setEnabled(true);
                
                ModForgeSettings settings = ModForgeSettings.getInstance();
                settings.setEnablePatternRecognition(true);
                
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                if (notificationService != null) {
                    notificationService.showInfoDialog(
                            project,
                            "Pattern Recognition",
                            "Pattern recognition has been enabled."
                    );
                } else {
                    Messages.showInfoMessage(
                            project,
                            "Pattern recognition has been enabled.",
                            "Pattern Recognition"
                    );
                }
            }
        }
    }
    
    /**
     * Show pattern recognition metrics.
     *
     * @param project The project
     */
    public void showMetrics(@NotNull Project project) {
        PatternRecognitionService service = project.getService(PatternRecognitionService.class);
        if (service == null) {
            return;
        }
        
        Map<String, Object> metrics = service.getMetrics();
        
        boolean enabled = (boolean) metrics.get("enabled");
        int totalRequests = (int) metrics.get("totalRequests");
        int patternMatches = (int) metrics.get("patternMatches");
        int apiCalls = (int) metrics.get("apiCalls");
        double estimatedTokensSaved = (double) metrics.get("estimatedTokensSaved");
        double estimatedCostSaved = (double) metrics.get("estimatedCostSaved");
        Map<String, Integer> patternCountsByType = (Map<String, Integer>) metrics.get("patternCountsByType");
        
        // Format numbers
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Pattern Recognition Metrics:\n\n");
        sb.append("Status: ").append(enabled ? "Enabled" : "Disabled").append("\n");
        sb.append("Total Requests: ").append(numberFormat.format(totalRequests)).append("\n");
        sb.append("Pattern Matches: ").append(numberFormat.format(patternMatches)).append("\n");
        sb.append("API Calls: ").append(numberFormat.format(apiCalls)).append("\n");
        sb.append("Estimated Tokens Saved: ").append(numberFormat.format(estimatedTokensSaved)).append("\n");
        sb.append("Estimated Cost Saved: ").append(currencyFormat.format(estimatedCostSaved)).append("\n\n");
        
        sb.append("Patterns by Type:\n");
        patternCountsByType.forEach((type, count) -> {
            sb.append("- ").append(type).append(": ").append(numberFormat.format(count)).append("\n");
        });
        
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        if (notificationService != null) {
            notificationService.showInfoDialog(
                    project,
                    "Pattern Recognition Metrics",
                    sb.toString()
            );
        } else {
            Messages.showInfoMessage(
                    project,
                    sb.toString(),
                    "Pattern Recognition Metrics"
            );
        }
    }
}