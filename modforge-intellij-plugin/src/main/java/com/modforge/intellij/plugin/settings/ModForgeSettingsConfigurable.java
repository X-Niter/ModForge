package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Settings configurable for ModForge.
 * This class provides a UI for configuring ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    
    // API settings
    private JBPasswordField openAiApiKeyField;
    private ComboBox<String> openAiModelComboBox;
    
    // User settings
    private JBTextField usernameField;
    private JBTextField serverUrlField;
    
    // Feature toggles
    private JBCheckBox enableAIAssistCheckBox;
    private JBCheckBox usePatternRecognitionCheckBox;
    private JBCheckBox enableContinuousDevelopmentCheckBox;
    private JBCheckBox enableCollaborativeEditingCheckBox;
    
    // Advanced settings
    private JSpinner maxCompletionTokensSpinner;
    private JSpinner similarityThresholdSpinner;
    private JSpinner autoSaveIntervalSpinner;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        // Create UI components
        openAiApiKeyField = new JBPasswordField();
        openAiModelComboBox = new ComboBox<>(new String[]{"gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"});
        
        usernameField = new JBTextField();
        serverUrlField = new JBTextField();
        
        enableAIAssistCheckBox = new JBCheckBox("Enable AI assist");
        usePatternRecognitionCheckBox = new JBCheckBox("Use pattern recognition (reduces API usage)");
        enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
        enableCollaborativeEditingCheckBox = new JBCheckBox("Enable collaborative editing");
        
        SpinnerNumberModel tokensModel = new SpinnerNumberModel(2000, 100, 32000, 100);
        maxCompletionTokensSpinner = new JSpinner(tokensModel);
        
        SpinnerNumberModel thresholdModel = new SpinnerNumberModel(0.7, 0.0, 1.0, 0.05);
        similarityThresholdSpinner = new JSpinner(thresholdModel);
        
        SpinnerNumberModel intervalModel = new SpinnerNumberModel(5, 1, 600, 1);
        autoSaveIntervalSpinner = new JSpinner(intervalModel);
        
        // Load settings
        loadSettings();
        
        // Create panels
        JPanel apiSettingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI API Key:"), openAiApiKeyField)
                .addLabeledComponent(new JBLabel("OpenAI Model:"), openAiModelComboBox)
                .getPanel();
        
        JPanel userSettingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Username:"), usernameField)
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField)
                .getPanel();
        
        JPanel featureTogglesPanel = FormBuilder.createFormBuilder()
                .addComponent(enableAIAssistCheckBox)
                .addComponent(usePatternRecognitionCheckBox)
                .addComponent(enableContinuousDevelopmentCheckBox)
                .addComponent(enableCollaborativeEditingCheckBox)
                .getPanel();
        
        JPanel advancedSettingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Max Completion Tokens:"), maxCompletionTokensSpinner)
                .addLabeledComponent(new JBLabel("Similarity Threshold:"), similarityThresholdSpinner)
                .addLabeledComponent(new JBLabel("Auto-save Interval (seconds):"), autoSaveIntervalSpinner)
                .getPanel();
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("API Settings", apiSettingsPanel);
        tabbedPane.addTab("User Settings", userSettingsPanel);
        tabbedPane.addTab("Features", featureTogglesPanel);
        tabbedPane.addTab("Advanced", advancedSettingsPanel);
        
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // API settings
        if (!Objects.equals(String.valueOf(openAiApiKeyField.getPassword()), settings.getOpenAiApiKey())) {
            return true;
        }
        
        if (!Objects.equals(openAiModelComboBox.getSelectedItem(), settings.getOpenAiModel())) {
            return true;
        }
        
        // User settings
        if (!Objects.equals(usernameField.getText(), settings.getUsername())) {
            return true;
        }
        
        if (!Objects.equals(serverUrlField.getText(), settings.getServerUrl())) {
            return true;
        }
        
        // Feature toggles
        if (enableAIAssistCheckBox.isSelected() != settings.isEnableAIAssist()) {
            return true;
        }
        
        if (usePatternRecognitionCheckBox.isSelected() != settings.isUsePatternRecognition()) {
            return true;
        }
        
        if (enableContinuousDevelopmentCheckBox.isSelected() != settings.isEnableContinuousDevelopment()) {
            return true;
        }
        
        if (enableCollaborativeEditingCheckBox.isSelected() != settings.isEnableCollaborativeEditing()) {
            return true;
        }
        
        // Advanced settings
        if ((Integer) maxCompletionTokensSpinner.getValue() != settings.getMaxCompletionTokens()) {
            return true;
        }
        
        if (!Objects.equals(similarityThresholdSpinner.getValue(), settings.getSimilarityThreshold())) {
            return true;
        }
        
        if ((Integer) autoSaveIntervalSpinner.getValue() != settings.getAutoSaveIntervalSeconds()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // API settings
        settings.setOpenAiApiKey(String.valueOf(openAiApiKeyField.getPassword()));
        settings.setOpenAiModel((String) openAiModelComboBox.getSelectedItem());
        
        // User settings
        settings.setUsername(usernameField.getText());
        settings.setServerUrl(serverUrlField.getText());
        
        // Feature toggles
        settings.setEnableAIAssist(enableAIAssistCheckBox.isSelected());
        settings.setUsePatternRecognition(usePatternRecognitionCheckBox.isSelected());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setEnableCollaborativeEditing(enableCollaborativeEditingCheckBox.isSelected());
        
        // Advanced settings
        settings.setMaxCompletionTokens((Integer) maxCompletionTokensSpinner.getValue());
        settings.setSimilarityThreshold((Double) similarityThresholdSpinner.getValue());
        settings.setAutoSaveIntervalSeconds((Integer) autoSaveIntervalSpinner.getValue());
    }
    
    @Override
    public void reset() {
        loadSettings();
    }
    
    /**
     * Loads settings into UI components.
     */
    private void loadSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // API settings
        openAiApiKeyField.setText(settings.getOpenAiApiKey());
        openAiModelComboBox.setSelectedItem(settings.getOpenAiModel());
        
        // User settings
        usernameField.setText(settings.getUsername());
        serverUrlField.setText(settings.getServerUrl());
        
        // Feature toggles
        enableAIAssistCheckBox.setSelected(settings.isEnableAIAssist());
        usePatternRecognitionCheckBox.setSelected(settings.isUsePatternRecognition());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        enableCollaborativeEditingCheckBox.setSelected(settings.isEnableCollaborativeEditing());
        
        // Advanced settings
        maxCompletionTokensSpinner.setValue(settings.getMaxCompletionTokens());
        similarityThresholdSpinner.setValue(settings.getSimilarityThreshold());
        autoSaveIntervalSpinner.setValue(settings.getAutoSaveIntervalSeconds());
    }
    
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        openAiApiKeyField = null;
        openAiModelComboBox = null;
        usernameField = null;
        serverUrlField = null;
        enableAIAssistCheckBox = null;
        usePatternRecognitionCheckBox = null;
        enableContinuousDevelopmentCheckBox = null;
        enableCollaborativeEditingCheckBox = null;
        maxCompletionTokensSpinner = null;
        similarityThresholdSpinner = null;
        autoSaveIntervalSpinner = null;
    }
}