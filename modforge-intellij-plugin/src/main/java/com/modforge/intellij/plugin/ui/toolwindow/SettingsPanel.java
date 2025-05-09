package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Panel for settings.
 */
public class SettingsPanel implements Disposable {
    private final Project project;
    private final ToolWindow toolWindow;
    private final SimpleToolWindowPanel panel;
    
    private JBTextField apiKeyField;
    private JBCheckBox enabledContinuousDevelopmentCheckBox;
    private JBCheckBox enablePatternRecognitionCheckBox;
    private ComboBox<String> aiModelComboBox;
    private JBTextField syncServerUrlField;
    private JBTextField syncTokenField;
    
    private final ModForgeSettings settings;
    
    /**
     * Creates a new SettingsPanel.
     * @param project The project
     * @param toolWindow The tool window
     */
    public SettingsPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.settings = ModForgeSettings.getInstance();
        
        // Create panel
        panel = new SimpleToolWindowPanel(true, true);
        
        // Init UI
        initUI();
    }
    
    /**
     * Initializes the UI.
     */
    private void initUI() {
        // Create components
        JButton openSettingsButton = new JButton("Open Settings Dialog");
        JButton applyButton = new JButton("Apply");
        
        apiKeyField = new JBPasswordField();
        apiKeyField.setColumns(30);
        
        enabledContinuousDevelopmentCheckBox = new JBCheckBox("Enable Continuous Development");
        enablePatternRecognitionCheckBox = new JBCheckBox("Enable Pattern Recognition");
        
        String[] aiModels = {"gpt-4", "gpt-3.5-turbo", "claude-3-opus", "claude-3-sonnet"};
        aiModelComboBox = new ComboBox<>(aiModels);
        
        syncServerUrlField = new JBTextField();
        syncServerUrlField.setColumns(30);
        
        syncTokenField = new JBPasswordField();
        syncTokenField.setColumns(30);
        
        // Create form
        JPanel formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("API Key:"), apiKeyField, true)
                .addLabeledComponent(new JBLabel("AI Model:"), aiModelComboBox, true)
                .addComponentToRightColumn(enabledContinuousDevelopmentCheckBox)
                .addComponentToRightColumn(enablePatternRecognitionCheckBox)
                .addSeparator(10)
                .addLabeledComponent(new JBLabel("Sync Server URL:"), syncServerUrlField, true)
                .addLabeledComponent(new JBLabel("Sync Token:"), syncTokenField, true)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Add padding
        formPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(openSettingsButton);
        buttonPanel.add(applyButton);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add to panel
        panel.setContent(mainPanel);
        
        // Load settings
        loadSettings();
        
        // Add action listeners
        openSettingsButton.addActionListener(this::onOpenSettingsClicked);
        applyButton.addActionListener(this::onApplyClicked);
        
        // Add document listeners
        apiKeyField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                settings.setApiKey(apiKeyField.getText());
            }
        });
        
        syncServerUrlField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                settings.setSyncServerUrl(syncServerUrlField.getText());
            }
        });
        
        syncTokenField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                settings.setSyncToken(syncTokenField.getText());
            }
        });
        
        // Add action listeners for checkboxes
        enabledContinuousDevelopmentCheckBox.addActionListener(e -> {
            settings.setContinuousDevelopmentEnabled(enabledContinuousDevelopmentCheckBox.isSelected());
        });
        
        enablePatternRecognitionCheckBox.addActionListener(e -> {
            settings.setPatternRecognitionEnabled(enablePatternRecognitionCheckBox.isSelected());
        });
        
        // Add action listener for combo box
        aiModelComboBox.addActionListener(e -> {
            settings.setAiModel((String) aiModelComboBox.getSelectedItem());
        });
    }
    
    /**
     * Loads settings from the settings service.
     */
    private void loadSettings() {
        apiKeyField.setText(settings.getApiKey());
        enabledContinuousDevelopmentCheckBox.setSelected(settings.isContinuousDevelopmentEnabled());
        enablePatternRecognitionCheckBox.setSelected(settings.isPatternRecognitionEnabled());
        aiModelComboBox.setSelectedItem(settings.getAiModel());
        syncServerUrlField.setText(settings.getSyncServerUrl());
        syncTokenField.setText(settings.getSyncToken());
    }
    
    /**
     * Called when the open settings button is clicked.
     * @param e The action event
     */
    private void onOpenSettingsClicked(ActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
        
        // Reload settings after dialog is closed
        loadSettings();
    }
    
    /**
     * Called when the apply button is clicked.
     * @param e The action event
     */
    private void onApplyClicked(ActionEvent e) {
        // Settings are saved automatically via document listeners
        JOptionPane.showMessageDialog(
                panel,
                "Settings applied successfully.",
                "ModForge",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Gets the content.
     * @return The content
     */
    public JComponent getContent() {
        return panel;
    }
    
    @Override
    public void dispose() {
        // Clean up resources
    }
}