package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

/**
 * Panel for configuring ModForge settings.
 * This panel provides quick access to common settings.
 */
public class SettingsPanel {
    private static final Logger LOG = Logger.getInstance(SettingsPanel.class);
    
    private final Project project;
    private final JPanel mainPanel;
    
    // Settings components
    private JBPasswordField openAiApiKeyField;
    private JBTextField usernameField;
    private JBCheckBox enableAIAssistCheckBox;
    private JBCheckBox usePatternRecognitionCheckBox;
    private ComboBox<String> openAiModelComboBox;
    private JButton applyButton;
    private JBLabel statusLabel;
    
    /**
     * Creates a new SettingsPanel.
     * @param project The project
     */
    public SettingsPanel(@NotNull Project project) {
        this.project = project;
        
        mainPanel = new JBPanel<>(new BorderLayout());
        
        // Create UI components
        openAiApiKeyField = new JBPasswordField();
        usernameField = new JBTextField();
        enableAIAssistCheckBox = new JBCheckBox("Enable AI assist");
        usePatternRecognitionCheckBox = new JBCheckBox("Use pattern recognition (reduces API usage)");
        openAiModelComboBox = new ComboBox<>(new String[]{"gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"});
        
        applyButton = new JButton("Apply");
        applyButton.addActionListener(this::onApplyClick);
        applyButton.setEnabled(false);
        
        statusLabel = new JBLabel("");
        statusLabel.setBorder(JBUI.Borders.empty(5, 10));
        
        // Load settings
        loadSettings();
        
        // Add change listeners
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyButton.setEnabled(true);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                applyButton.setEnabled(true);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                applyButton.setEnabled(true);
            }
        };
        
        openAiApiKeyField.getDocument().addDocumentListener(documentListener);
        usernameField.getDocument().addDocumentListener(documentListener);
        
        enableAIAssistCheckBox.addItemListener(e -> applyButton.setEnabled(true));
        usePatternRecognitionCheckBox.addItemListener(e -> applyButton.setEnabled(true));
        
        openAiModelComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyButton.setEnabled(true);
            }
        });
        
        // Create action toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // Add Open Settings action
        actionGroup.add(new AnAction("Open Settings", "Open ModForge Settings", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                openSettings();
            }
        });
        
        // Add Reset action
        actionGroup.add(new AnAction("Reset", "Reset to saved settings", AllIcons.Actions.Rollback) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                loadSettings();
                applyButton.setEnabled(false);
                statusLabel.setText("");
            }
        });
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "ModForgeSettings",
                actionGroup,
                true
        );
        
        // Create toolbar wrapper
        BorderLayoutPanel toolbarWrapper = JBUI.Panels.simplePanel();
        toolbarWrapper.addToLeft(toolbar.getComponent());
        
        // Create heading
        JBLabel headingLabel = new JBLabel("Quick Settings");
        headingLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 2));
        headingLabel.setBorder(JBUI.Borders.empty(10, 10, 5, 10));
        
        // Build settings form
        JPanel settingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI API Key:"), openAiApiKeyField)
                .addLabeledComponent(new JBLabel("Model:"), openAiModelComboBox)
                .addLabeledComponent(new JBLabel("Username:"), usernameField)
                .addComponentToRightColumn(enableAIAssistCheckBox)
                .addComponentToRightColumn(usePatternRecognitionCheckBox)
                .addComponentToRightColumn(applyButton)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Add bottom panel
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        
        // Create content panel
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.add(headingLabel, BorderLayout.NORTH);
        contentPanel.add(settingsPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Set up main panel
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(toolbarWrapper, BorderLayout.NORTH);
    }
    
    /**
     * Gets the main panel.
     * @return The main panel
     */
    @NotNull
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Loads settings from the settings service.
     */
    private void loadSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        openAiApiKeyField.setText(settings.getOpenAiApiKey());
        usernameField.setText(settings.getUsername());
        enableAIAssistCheckBox.setSelected(settings.isEnableAIAssist());
        usePatternRecognitionCheckBox.setSelected(settings.isUsePatternRecognition());
        openAiModelComboBox.setSelectedItem(settings.getOpenAiModel());
    }
    
    /**
     * Saves settings to the settings service.
     */
    private void saveSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setOpenAiApiKey(String.valueOf(openAiApiKeyField.getPassword()));
        settings.setUsername(usernameField.getText());
        settings.setEnableAIAssist(enableAIAssistCheckBox.isSelected());
        settings.setUsePatternRecognition(usePatternRecognitionCheckBox.isSelected());
        
        String model = (String) openAiModelComboBox.getSelectedItem();
        if (model != null) {
            settings.setOpenAiModel(model);
        }
    }
    
    /**
     * Opens the ModForge settings dialog.
     */
    private void openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
        loadSettings();
    }
    
    /**
     * Handles Apply button click.
     * @param e The action event
     */
    private void onApplyClick(ActionEvent e) {
        try {
            saveSettings();
            applyButton.setEnabled(false);
            
            statusLabel.setText("Settings saved successfully");
            statusLabel.setForeground(JBColor.GREEN);
            
            // Clear status after 3 seconds
            Timer timer = new Timer(3000, event -> {
                statusLabel.setText("");
            });
            timer.setRepeats(false);
            timer.start();
        } catch (Exception ex) {
            LOG.error("Error saving settings", ex);
            
            statusLabel.setText("Error saving settings: " + ex.getMessage());
            statusLabel.setForeground(JBColor.RED);
        }
    }
    
    /**
     * Disposes the panel.
     */
    public void dispose() {
        // Nothing to dispose
    }
}