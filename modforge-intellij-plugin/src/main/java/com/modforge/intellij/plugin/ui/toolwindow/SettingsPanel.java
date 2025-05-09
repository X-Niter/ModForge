package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Panel for adjusting settings from the tool window.
 */
public final class SettingsPanel extends SimpleToolWindowPanel {
    private final Project project;
    private final ModForgeSettings settings;
    private final ContinuousDevelopmentService continuousDevelopmentService;
    
    private JPanel mainPanel;
    private JCheckBox continuousDevelopmentCheckbox;
    private JCheckBox patternRecognitionCheckbox;
    private JComboBox<String> checkIntervalComboBox;
    private JCheckBox autoCompileCheckbox;
    private JCheckBox autoFixCheckbox;
    private JCheckBox autoDocumentCheckbox;
    private JCheckBox forgeSupportCheckbox;
    private JCheckBox fabricSupportCheckbox;
    private JCheckBox quiltSupportCheckbox;
    
    /**
     * Creates a new SettingsPanel.
     * @param project The project
     */
    public SettingsPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.continuousDevelopmentService = ContinuousDevelopmentService.getInstance(project);
        
        initializeUI();
        
        setContent(mainPanel);
    }
    
    /**
     * Initializes the UI.
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel featureTogglesPanel = createFeatureTogglesPanel();
        JPanel developmentSettingsPanel = createDevelopmentSettingsPanel();
        JPanel modLoaderSettingsPanel = createModLoaderSettingsPanel();
        
        JPanel settingsPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        settingsPanel.add(featureTogglesPanel);
        settingsPanel.add(developmentSettingsPanel);
        settingsPanel.add(modLoaderSettingsPanel);
        
        contentPanel.add(settingsPanel, BorderLayout.NORTH);
        
        mainPanel.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton applyButton = new JButton("Apply", AllIcons.Actions.Compile);
        applyButton.addActionListener(e -> applySettings());
        
        JButton advancedButton = new JButton("Advanced Settings...", AllIcons.General.Settings);
        advancedButton.addActionListener(e -> openAdvancedSettings());
        
        buttonPanel.add(applyButton);
        buttonPanel.add(advancedButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Load current settings
        loadSettings();
    }
    
    /**
     * Creates the feature toggles panel.
     * @return The panel
     */
    private JPanel createFeatureTogglesPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("BorderColor")),
                "Feature Toggles",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        continuousDevelopmentCheckbox = new JBCheckBox("Enable Continuous Development", settings.isContinuousDevelopmentEnabled());
        patternRecognitionCheckbox = new JBCheckBox("Enable Pattern Recognition", settings.isPatternRecognitionEnabled());
        
        panel.add(continuousDevelopmentCheckbox);
        panel.add(patternRecognitionCheckbox);
        
        // Add listener to update continuous development service
        continuousDevelopmentCheckbox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                continuousDevelopmentService.start();
            } else {
                continuousDevelopmentService.stop();
            }
        });
        
        return panel;
    }
    
    /**
     * Creates the development settings panel.
     * @return The panel
     */
    private JPanel createDevelopmentSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("BorderColor")),
                "Development Settings",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        JPanel checkIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkIntervalPanel.add(new JBLabel("Check Interval:"));
        
        checkIntervalComboBox = new ComboBox<>(new String[] {
                "1 second",
                "5 seconds",
                "10 seconds",
                "30 seconds",
                "1 minute",
                "5 minutes",
                "10 minutes"
        });
        checkIntervalPanel.add(checkIntervalComboBox);
        
        autoCompileCheckbox = new JBCheckBox("Auto-Compile", settings.isAutoCompileEnabled());
        autoFixCheckbox = new JBCheckBox("Auto-Fix Errors", settings.isAutoFixEnabled());
        autoDocumentCheckbox = new JBCheckBox("Auto-Document Code", settings.isAutoDocumentEnabled());
        
        panel.add(checkIntervalPanel);
        panel.add(autoCompileCheckbox);
        panel.add(autoFixCheckbox);
        panel.add(autoDocumentCheckbox);
        
        return panel;
    }
    
    /**
     * Creates the mod loader settings panel.
     * @return The panel
     */
    private JPanel createModLoaderSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("BorderColor")),
                "Mod Loader Support",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        forgeSupportCheckbox = new JBCheckBox("Forge", settings.isForgeSupported());
        fabricSupportCheckbox = new JBCheckBox("Fabric", settings.isFabricSupported());
        quiltSupportCheckbox = new JBCheckBox("Quilt", settings.isQuiltSupported());
        
        panel.add(forgeSupportCheckbox);
        panel.add(fabricSupportCheckbox);
        panel.add(quiltSupportCheckbox);
        
        return panel;
    }
    
    /**
     * Loads the current settings.
     */
    private void loadSettings() {
        // Feature toggles
        continuousDevelopmentCheckbox.setSelected(settings.isContinuousDevelopmentEnabled());
        patternRecognitionCheckbox.setSelected(settings.isPatternRecognitionEnabled());
        
        // Development settings
        long checkIntervalMs = settings.getCheckIntervalMs();
        
        if (checkIntervalMs <= 1000) {
            checkIntervalComboBox.setSelectedItem("1 second");
        } else if (checkIntervalMs <= 5000) {
            checkIntervalComboBox.setSelectedItem("5 seconds");
        } else if (checkIntervalMs <= 10000) {
            checkIntervalComboBox.setSelectedItem("10 seconds");
        } else if (checkIntervalMs <= 30000) {
            checkIntervalComboBox.setSelectedItem("30 seconds");
        } else if (checkIntervalMs <= 60000) {
            checkIntervalComboBox.setSelectedItem("1 minute");
        } else if (checkIntervalMs <= 300000) {
            checkIntervalComboBox.setSelectedItem("5 minutes");
        } else {
            checkIntervalComboBox.setSelectedItem("10 minutes");
        }
        
        autoCompileCheckbox.setSelected(settings.isAutoCompileEnabled());
        autoFixCheckbox.setSelected(settings.isAutoFixEnabled());
        autoDocumentCheckbox.setSelected(settings.isAutoDocumentEnabled());
        
        // Mod loader settings
        forgeSupportCheckbox.setSelected(settings.isForgeSupported());
        fabricSupportCheckbox.setSelected(settings.isFabricSupported());
        quiltSupportCheckbox.setSelected(settings.isQuiltSupported());
    }
    
    /**
     * Applies the settings.
     */
    private void applySettings() {
        // Feature toggles
        settings.setContinuousDevelopmentEnabled(continuousDevelopmentCheckbox.isSelected());
        settings.setPatternRecognitionEnabled(patternRecognitionCheckbox.isSelected());
        
        // Development settings
        String checkIntervalStr = (String) checkIntervalComboBox.getSelectedItem();
        long checkIntervalMs = 60000L; // Default to 1 minute
        
        if (checkIntervalStr != null) {
            if (checkIntervalStr.equals("1 second")) {
                checkIntervalMs = 1000L;
            } else if (checkIntervalStr.equals("5 seconds")) {
                checkIntervalMs = 5000L;
            } else if (checkIntervalStr.equals("10 seconds")) {
                checkIntervalMs = 10000L;
            } else if (checkIntervalStr.equals("30 seconds")) {
                checkIntervalMs = 30000L;
            } else if (checkIntervalStr.equals("1 minute")) {
                checkIntervalMs = 60000L;
            } else if (checkIntervalStr.equals("5 minutes")) {
                checkIntervalMs = 300000L;
            } else if (checkIntervalStr.equals("10 minutes")) {
                checkIntervalMs = 600000L;
            }
        }
        
        settings.setCheckIntervalMs(checkIntervalMs);
        continuousDevelopmentService.setCheckInterval(checkIntervalMs);
        
        settings.setAutoCompileEnabled(autoCompileCheckbox.isSelected());
        settings.setAutoFixEnabled(autoFixCheckbox.isSelected());
        settings.setAutoDocumentEnabled(autoDocumentCheckbox.isSelected());
        
        // Mod loader settings
        settings.setForgeSupported(forgeSupportCheckbox.isSelected());
        settings.setFabricSupported(fabricSupportCheckbox.isSelected());
        settings.setQuiltSupported(quiltSupportCheckbox.isSelected());
        
        // Update continuous development service
        if (settings.isContinuousDevelopmentEnabled()) {
            continuousDevelopmentService.start();
        } else {
            continuousDevelopmentService.stop();
        }
    }
    
    /**
     * Opens the advanced settings dialog.
     */
    private void openAdvancedSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
        
        // Reload settings
        ApplicationManager.getApplication().invokeLater(this::loadSettings);
    }
}