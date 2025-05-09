package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Objects;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    
    // General settings
    private JBCheckBox enableAIAssistCheckBox;
    private JBCheckBox enableContinuousDevelopmentCheckBox;
    private JBCheckBox autoFixCompilationErrorsCheckBox;
    private JSpinner maxAutoFixAttemptsSpinner;
    private JSpinner maxEnhancementsPerFileSpinner;
    
    // API settings
    private JBPasswordField openAIApiKeyField;
    private JBCheckBox usePatternRecognitionCheckBox;
    private JSpinner similarityThresholdSpinner;
    
    // Web sync settings
    private JBTextField serverUrlField;
    private JBPasswordField apiTokenField;
    private JBCheckBox enableSyncCheckBox;
    private JSpinner syncIntervalSpinner;
    
    private boolean modified = false;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // Create UI components
        createUIComponents();
        
        // Create panels
        JPanel generalPanel = createGeneralPanel();
        JPanel apiPanel = createAPIPanel();
        JPanel syncPanel = createSyncPanel();
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("General", generalPanel);
        tabbedPane.addTab("API", apiPanel);
        tabbedPane.addTab("Web Sync", syncPanel);
        
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Load settings
        loadSettings();
        
        return mainPanel;
    }

    /**
     * Creates UI components.
     */
    private void createUIComponents() {
        // General settings
        enableAIAssistCheckBox = new JBCheckBox("Enable AI assist");
        enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
        autoFixCompilationErrorsCheckBox = new JBCheckBox("Auto-fix compilation errors");
        maxAutoFixAttemptsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        maxEnhancementsPerFileSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        
        // API settings
        openAIApiKeyField = new JBPasswordField();
        usePatternRecognitionCheckBox = new JBCheckBox("Use pattern recognition to reduce API calls");
        similarityThresholdSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.5, 1.0, 0.05));
        
        // Web sync settings
        serverUrlField = new JBTextField();
        apiTokenField = new JBPasswordField();
        enableSyncCheckBox = new JBCheckBox("Enable synchronization with web platform");
        syncIntervalSpinner = new JSpinner(new SpinnerNumberModel(300, 60, 3600, 60));
        
        // Add change listeners
        addChangeListeners();
    }
    
    /**
     * Creates the general settings panel.
     * @return The general settings panel
     */
    private JPanel createGeneralPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("General Settings"))
                .addComponent(enableAIAssistCheckBox)
                .addComponent(enableContinuousDevelopmentCheckBox)
                .addComponent(autoFixCompilationErrorsCheckBox)
                .addLabeledComponent("Max auto-fix attempts:", maxAutoFixAttemptsSpinner)
                .addLabeledComponent("Max enhancements per file:", maxEnhancementsPerFileSpinner)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }
    
    /**
     * Creates the API settings panel.
     * @return The API settings panel
     */
    private JPanel createAPIPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("API Settings"))
                .addLabeledComponent("OpenAI API Key:", openAIApiKeyField)
                .addComponent(usePatternRecognitionCheckBox)
                .addLabeledComponent("Similarity threshold:", similarityThresholdSpinner)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }
    
    /**
     * Creates the web sync settings panel.
     * @return The web sync settings panel
     */
    private JPanel createSyncPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Web Synchronization Settings"))
                .addLabeledComponent("Server URL:", serverUrlField)
                .addLabeledComponent("API Token:", apiTokenField)
                .addComponent(enableSyncCheckBox)
                .addLabeledComponent("Sync interval (seconds):", syncIntervalSpinner)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }
    
    /**
     * Adds change listeners to UI components.
     */
    private void addChangeListeners() {
        // General settings
        enableAIAssistCheckBox.addActionListener(e -> modified = true);
        enableContinuousDevelopmentCheckBox.addActionListener(e -> modified = true);
        autoFixCompilationErrorsCheckBox.addActionListener(e -> modified = true);
        maxAutoFixAttemptsSpinner.addChangeListener(e -> modified = true);
        maxEnhancementsPerFileSpinner.addChangeListener(e -> modified = true);
        
        // API settings
        openAIApiKeyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                modified = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                modified = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                modified = true;
            }
        });
        usePatternRecognitionCheckBox.addActionListener(e -> modified = true);
        similarityThresholdSpinner.addChangeListener(e -> modified = true);
        
        // Web sync settings
        serverUrlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                modified = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                modified = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                modified = true;
            }
        });
        apiTokenField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                modified = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                modified = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                modified = true;
            }
        });
        enableSyncCheckBox.addActionListener(e -> modified = true);
        syncIntervalSpinner.addChangeListener(e -> modified = true);
    }
    
    /**
     * Loads settings from the ModForgeSettings.
     */
    private void loadSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // General settings
        enableAIAssistCheckBox.setSelected(settings.isEnableAIAssist());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        autoFixCompilationErrorsCheckBox.setSelected(settings.isAutoFixCompilationErrors());
        maxAutoFixAttemptsSpinner.setValue(settings.getMaxAutoFixAttempts());
        maxEnhancementsPerFileSpinner.setValue(settings.getMaxEnhancementsPerFile());
        
        // API settings
        openAIApiKeyField.setText(settings.getOpenAIApiKey());
        usePatternRecognitionCheckBox.setSelected(settings.isUsePatternRecognition());
        similarityThresholdSpinner.setValue(settings.getSimilarityThreshold());
        
        // Web sync settings
        serverUrlField.setText(settings.getServerUrl());
        apiTokenField.setText(settings.getApiToken());
        enableSyncCheckBox.setSelected(settings.isEnableSync());
        syncIntervalSpinner.setValue(settings.getSyncInterval());
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // General settings
        settings.setEnableAIAssist(enableAIAssistCheckBox.isSelected());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setAutoFixCompilationErrors(autoFixCompilationErrorsCheckBox.isSelected());
        settings.setMaxAutoFixAttempts((Integer) maxAutoFixAttemptsSpinner.getValue());
        settings.setMaxEnhancementsPerFile((Integer) maxEnhancementsPerFileSpinner.getValue());
        
        // API settings
        settings.setOpenAIApiKey(new String(openAIApiKeyField.getPassword()));
        settings.setUsePatternRecognition(usePatternRecognitionCheckBox.isSelected());
        settings.setSimilarityThreshold((Double) similarityThresholdSpinner.getValue());
        
        // Web sync settings
        settings.setServerUrl(serverUrlField.getText());
        settings.setApiToken(new String(apiTokenField.getPassword()));
        settings.setEnableSync(enableSyncCheckBox.isSelected());
        settings.setSyncInterval((Integer) syncIntervalSpinner.getValue());
        
        // Reset modified flag
        modified = false;
    }

    @Override
    public void reset() {
        loadSettings();
        modified = false;
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}