package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBSlider;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

/**
 * Action for toggling continuous development.
 */
public class ToggleContinuousDevelopmentAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ToggleContinuousDevelopmentAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Presentation presentation = e.getPresentation();
        
        // Disable action if there's no project
        if (project == null) {
            presentation.setEnabled(false);
            presentation.setText("Toggle Continuous Development");
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            presentation.setEnabled(false);
            presentation.setText("Login to Use Continuous Development");
            return;
        }
        
        // Update text based on whether continuous development is enabled
        ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
        boolean enabled = service != null && service.isEnabled();
        
        presentation.setEnabled(true);
        presentation.setText(enabled ? "Disable Continuous Development" : "Enable Continuous Development");
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
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance(project);
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Authentication Required",
                        "You must be logged in to use continuous development."
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to use continuous development.",
                        "Authentication Required"
                );
            }
            return;
        }
        
        // Toggle continuous development
        ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
        if (service != null) {
            boolean currentState = service.isEnabled();
            
            if (currentState) {
                // Show confirmation dialog before disabling
                int result = Messages.showYesNoDialog(
                        project,
                        "Are you sure you want to disable continuous development? All automatic error fixing will be stopped.",
                        "Disable Continuous Development",
                        "Disable",
                        "Cancel",
                        null
                );
                
                if (result == Messages.YES) {
                    service.setEnabled(false);
                    
                    ModForgeSettings settings = ModForgeSettings.getInstance();
                    settings.setEnableContinuousDevelopment(false);
                    
                    Messages.showInfoMessage(
                            project,
                            "Continuous development has been disabled.",
                            "Continuous Development"
                    );
                }
            } else {
                // Show settings dialog before enabling
                ContinuousDevelopmentSettingsDialog dialog = new ContinuousDevelopmentSettingsDialog(project, service);
                if (dialog.showAndGet()) {
                    long interval = dialog.getScanInterval();
                    
                    service.setScanInterval(interval);
                    service.setEnabled(true);
                    
                    ModForgeSettings settings = ModForgeSettings.getInstance();
                    settings.setEnableContinuousDevelopment(true);
                    settings.setContinuousDevelopmentScanInterval(interval);
                    
                    Messages.showInfoMessage(
                            project,
                            "Continuous development has been enabled. " +
                                    "Your project will be automatically scanned for errors every " +
                                    formatInterval(interval) + ".",
                            "Continuous Development"
                    );
                }
            }
        }
    }
    
    /**
     * Show continuous development metrics.
     *
     * @param project The project
     */
    public void showMetrics(@NotNull Project project) {
        ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
        if (service == null) {
            return;
        }
        
        Map<String, Object> stats = service.getStatistics();
        
        boolean enabled = (boolean) stats.get("enabled");
        boolean running = (boolean) stats.get("running");
        long scanInterval = (long) stats.get("scanInterval");
        long lastScanTime = (long) stats.get("lastScanTime");
        int fixCount = (int) stats.get("fixCount");
        int errorCount = (int) stats.get("errorCount");
        int successCount = (int) stats.get("successCount");
        Map<String, String> lastActions = (Map<String, String>) stats.get("lastActions");
        
        // Format numbers
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Continuous Development Metrics:\n\n");
        sb.append("Status: ").append(enabled ? "Enabled" : "Disabled").append(", ")
                .append(running ? "Running" : "Stopped").append("\n");
        sb.append("Scan Interval: ").append(formatInterval(scanInterval)).append("\n");
        sb.append("Last Scan: ").append(lastScanTime > 0 ? new java.util.Date(lastScanTime) : "Never").append("\n");
        sb.append("Fix Count: ").append(numberFormat.format(fixCount)).append("\n");
        sb.append("Error Count: ").append(numberFormat.format(errorCount)).append("\n");
        sb.append("Success Count: ").append(numberFormat.format(successCount)).append("\n\n");
        
        sb.append("Recent Actions:\n");
        if (lastActions.isEmpty()) {
            sb.append("No actions recorded yet.\n");
        } else {
            lastActions.forEach((timestamp, action) -> {
                sb.append("- ").append(timestamp).append(": ").append(action).append("\n");
            });
        }
        
        Messages.showInfoMessage(
                project,
                sb.toString(),
                "Continuous Development Metrics"
        );
    }
    
    /**
     * Format an interval in milliseconds.
     *
     * @param interval The interval in milliseconds
     * @return A formatted string
     */
    private static String formatInterval(long interval) {
        if (interval < 1000) {
            return interval + " ms";
        } else if (interval < 60 * 1000) {
            return (interval / 1000) + " seconds";
        } else if (interval < 60 * 60 * 1000) {
            return (interval / (60 * 1000)) + " minutes";
        } else {
            return (interval / (60 * 60 * 1000)) + " hours";
        }
    }
    
    /**
     * Dialog for continuous development settings.
     */
    private static class ContinuousDevelopmentSettingsDialog extends DialogWrapper {
        private final JBSlider intervalSlider;
        private final JBLabel intervalLabel;
        
        public ContinuousDevelopmentSettingsDialog(@Nullable Project project, @NotNull ContinuousDevelopmentService service) {
            super(project);
            
            setTitle("Continuous Development Settings");
            setCancelButtonText("Cancel");
            setOKButtonText("Enable");
            
            // Interval slider
            long currentInterval = service.getScanInterval();
            intervalSlider = new JBSlider(JSlider.HORIZONTAL, 0, 6, getIntervalValue(currentInterval));
            
            // Add labels to slider
            Hashtable<Integer, JLabel> labels = new Hashtable<>();
            labels.put(0, new JLabel("30s"));
            labels.put(1, new JLabel("1m"));
            labels.put(2, new JLabel("5m"));
            labels.put(3, new JLabel("15m"));
            labels.put(4, new JLabel("30m"));
            labels.put(5, new JLabel("1h"));
            labels.put(6, new JLabel("2h"));
            intervalSlider.setLabelTable(labels);
            intervalSlider.setPaintLabels(true);
            intervalSlider.setMajorTickSpacing(1);
            intervalSlider.setPaintTicks(true);
            
            // Interval label
            intervalLabel = new JBLabel("Scan interval: " + formatInterval(getIntervalMillis(intervalSlider.getValue())));
            
            // Update label when slider changes
            intervalSlider.addChangeListener(e -> {
                int value = intervalSlider.getValue();
                intervalLabel.setText("Scan interval: " + formatInterval(getIntervalMillis(value)));
            });
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create panel
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addComponent(new JBLabel("How often should ModForge scan for errors?"))
                    .addComponent(intervalSlider)
                    .addComponent(intervalLabel)
                    .addComponentFillVertically(new JPanel(), 0);
            
            // Add hint
            JBLabel hintLabel = new JBLabel(
                    "ModForge will automatically scan your project for errors at the specified interval " +
                            "and fix them using AI. This continues even when you're not actively editing."
            );
            hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
            hintLabel.setFont(JBUI.Fonts.smallFont());
            formBuilder.addComponent(hintLabel);
            
            JPanel panel = formBuilder.getPanel();
            panel.setPreferredSize(new Dimension(400, panel.getPreferredSize().height));
            
            return panel;
        }
        
        /**
         * Get the scan interval from the settings.
         *
         * @return The scan interval in milliseconds
         */
        public long getScanInterval() {
            return getIntervalMillis(intervalSlider.getValue());
        }
        
        /**
         * Get the slider value for a given interval.
         *
         * @param intervalMillis The interval in milliseconds
         * @return The slider value
         */
        private int getIntervalValue(long intervalMillis) {
            if (intervalMillis <= 30 * 1000) {
                return 0; // 30 seconds
            } else if (intervalMillis <= 60 * 1000) {
                return 1; // 1 minute
            } else if (intervalMillis <= 5 * 60 * 1000) {
                return 2; // 5 minutes
            } else if (intervalMillis <= 15 * 60 * 1000) {
                return 3; // 15 minutes
            } else if (intervalMillis <= 30 * 60 * 1000) {
                return 4; // 30 minutes
            } else if (intervalMillis <= 60 * 60 * 1000) {
                return 5; // 1 hour
            } else {
                return 6; // 2 hours
            }
        }
        
        /**
         * Get the interval in milliseconds for a given slider value.
         *
         * @param value The slider value
         * @return The interval in milliseconds
         */
        private long getIntervalMillis(int value) {
            switch (value) {
                case 0:
                    return 30 * 1000; // 30 seconds
                case 1:
                    return 60 * 1000; // 1 minute
                case 2:
                    return 5 * 60 * 1000; // 5 minutes
                case 3:
                    return 15 * 60 * 1000; // 15 minutes
                case 4:
                    return 30 * 60 * 1000; // 30 minutes
                case 5:
                    return 60 * 60 * 1000; // 1 hour
                case 6:
                    return 2 * 60 * 60 * 1000; // 2 hours
                default:
                    return 60 * 1000; // Default to 1 minute
            }
        }
    }
}