package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Panel for the Settings tab in the tool window.
 * This panel provides quick access to basic settings.
 */
public final class SettingsPanel {
    private final Project project;
    private final ModForgeSettings settings;
    private final PatternRecognitionService patternRecognitionService;
    private final ContinuousDevelopmentService continuousDevelopmentService;
    
    private JPanel mainPanel;
    private JBCheckBox continuousDevelopmentCheckBox;
    private JBCheckBox patternRecognitionCheckBox;
    private JComboBox<String> continuousDevelopmentIntervalComboBox;
    
    /**
     * Creates a new SettingsPanel.
     * @param project The project
     */
    public SettingsPanel(@NotNull Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.patternRecognitionService = PatternRecognitionService.getInstance();
        this.continuousDevelopmentService = ContinuousDevelopmentService.getInstance(project);
        
        createUI();
    }
    
    /**
     * Gets the panel content.
     * @return The panel content
     */
    @NotNull
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Creates the UI for the panel.
     */
    private void createUI() {
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create settings panel
        JPanel settingsPanel = createSettingsPanel();
        
        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(settingsPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        // Add scroll pane to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Creates the settings panel.
     * @return The settings panel
     */
    @NotNull
    private JPanel createSettingsPanel() {
        // Create continuous development settings
        JPanel continuousDevPanel = createContinuousDevelopmentPanel();
        
        // Create pattern recognition settings
        JPanel patternRecognitionPanel = createPatternRecognitionPanel();
        
        // Create API settings section
        JPanel apiSettingsPanel = createApiSettingsPanel();
        
        // Create advanced settings section
        JPanel advancedSettingsPanel = createAdvancedSettingsPanel();
        
        // Create settings panel
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Quick Settings", UIUtil.ComponentStyle.LARGE))
                .addVerticalGap(10)
                .addComponent(new JBLabel("Continuous Development", UIUtil.ComponentStyle.REGULAR))
                .addComponentFillVertically(continuousDevPanel, 0)
                .addVerticalGap(20)
                .addComponent(new JBLabel("Pattern Recognition", UIUtil.ComponentStyle.REGULAR))
                .addComponentFillVertically(patternRecognitionPanel, 0)
                .addVerticalGap(20)
                .addComponent(new JBLabel("API Settings", UIUtil.ComponentStyle.REGULAR))
                .addComponentFillVertically(apiSettingsPanel, 0)
                .addVerticalGap(20)
                .addComponent(new JBLabel("Advanced Settings", UIUtil.ComponentStyle.REGULAR))
                .addComponentFillVertically(advancedSettingsPanel, 0)
                .addVerticalGap(10)
                .getPanel();
    }
    
    /**
     * Creates the continuous development panel.
     * @return The continuous development panel
     */
    @NotNull
    private JPanel createContinuousDevelopmentPanel() {
        // Create check box
        continuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
        continuousDevelopmentCheckBox.setSelected(continuousDevelopmentService.isRunning());
        continuousDevelopmentCheckBox.addChangeListener(e -> {
            if (continuousDevelopmentCheckBox.isSelected()) {
                continuousDevelopmentService.start();
            } else {
                continuousDevelopmentService.stop();
            }
            
            continuousDevelopmentIntervalComboBox.setEnabled(continuousDevelopmentCheckBox.isSelected());
        });
        
        // Create interval combo box
        continuousDevelopmentIntervalComboBox = new JComboBox<>(new String[] {
                "30 seconds",
                "1 minute",
                "5 minutes",
                "15 minutes",
                "30 minutes",
                "1 hour"
        });
        continuousDevelopmentIntervalComboBox.setEnabled(continuousDevelopmentCheckBox.isSelected());
        continuousDevelopmentIntervalComboBox.setSelectedItem(getIntervalString(continuousDevelopmentService.getCheckInterval()));
        continuousDevelopmentIntervalComboBox.addActionListener(e -> {
            long intervalMs = getSelectedIntervalMs();
            continuousDevelopmentService.setCheckInterval(intervalMs);
        });
        
        // Create panel
        return FormBuilder.createFormBuilder()
                .addComponent(continuousDevelopmentCheckBox)
                .addLabeledComponent("Check Interval:", continuousDevelopmentIntervalComboBox)
                .addVerticalGap(5)
                .getPanel();
    }
    
    /**
     * Creates the pattern recognition panel.
     * @return The pattern recognition panel
     */
    @NotNull
    private JPanel createPatternRecognitionPanel() {
        // Create check box
        patternRecognitionCheckBox = new JBCheckBox("Enable pattern recognition to reduce API usage");
        patternRecognitionCheckBox.setSelected(patternRecognitionService.isEnabled());
        patternRecognitionCheckBox.addChangeListener(e -> {
            patternRecognitionService.setEnabled(patternRecognitionCheckBox.isSelected());
        });
        
        // Create clear patterns button
        JButton clearPatternsButton = new JButton("Clear Patterns");
        clearPatternsButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "Are you sure you want to clear all patterns? This cannot be undone.",
                    "Clear Patterns",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                patternRecognitionService.clearPatterns();
                JOptionPane.showMessageDialog(
                        mainPanel,
                        "All patterns have been cleared.",
                        "Patterns Cleared",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        
        // Create panel with button aligned to the right
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(clearPatternsButton, BorderLayout.EAST);
        
        // Create panel
        return FormBuilder.createFormBuilder()
                .addComponent(patternRecognitionCheckBox)
                .addComponent(buttonPanel)
                .addVerticalGap(5)
                .getPanel();
    }
    
    /**
     * Creates the API settings panel.
     * @return The API settings panel
     */
    @NotNull
    private JPanel createApiSettingsPanel() {
        // Create API key status label
        boolean hasApiKey = !settings.getOpenAiApiKey().isEmpty();
        
        JBLabel apiKeyStatusLabel = new JBLabel(
                hasApiKey ? "OpenAI API Key: Configured" : "OpenAI API Key: Not configured",
                hasApiKey ? AllIcons.General.InspectionsOK : AllIcons.General.Error,
                JBLabel.LEFT
        );
        
        // Create panel
        return FormBuilder.createFormBuilder()
                .addComponent(apiKeyStatusLabel)
                .addComponent(createSettingsLink())
                .addVerticalGap(5)
                .getPanel();
    }
    
    /**
     * Creates the advanced settings panel.
     * @return The advanced settings panel
     */
    @NotNull
    private JPanel createAdvancedSettingsPanel() {
        // Create UI components
        JBLabel syncStatusLabel = new JBLabel(
                settings.isSyncWithWebEnabled() ? "Web Sync: Enabled" : "Web Sync: Disabled",
                settings.isSyncWithWebEnabled() ? AllIcons.General.InspectionsOK : AllIcons.General.BalloonInformation,
                JBLabel.LEFT
        );
        
        // Create panel
        return FormBuilder.createFormBuilder()
                .addComponent(syncStatusLabel)
                .addComponent(createSettingsLink())
                .addVerticalGap(5)
                .getPanel();
    }
    
    /**
     * Creates a settings link.
     * @return The settings link
     */
    @NotNull
    private JComponent createSettingsLink() {
        ActionLink settingsLink = new ActionLink("Open full settings...", e -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
        });
        
        return settingsLink;
    }
    
    /**
     * Gets the selected interval in milliseconds.
     * @return The selected interval in milliseconds
     */
    private long getSelectedIntervalMs() {
        String selected = (String) continuousDevelopmentIntervalComboBox.getSelectedItem();
        
        if (selected == null) {
            return 60_000; // 1 minute default
        }
        
        return switch (selected) {
            case "30 seconds" -> 30_000L;
            case "5 minutes" -> 300_000L;
            case "15 minutes" -> 900_000L;
            case "30 minutes" -> 1_800_000L;
            case "1 hour" -> 3_600_000L;
            default -> 60_000L; // 1 minute default
        };
    }
    
    /**
     * Gets the interval string for the given milliseconds.
     * @param intervalMs The interval in milliseconds
     * @return The interval string
     */
    @NotNull
    private String getIntervalString(long intervalMs) {
        return switch ((int) (intervalMs / 1000)) {
            case 30 -> "30 seconds";
            case 300 -> "5 minutes";
            case 900 -> "15 minutes";
            case 1800 -> "30 minutes";
            case 3600 -> "1 hour";
            default -> "1 minute";
        };
    }
}