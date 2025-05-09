package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JBPasswordField openAiApiKeyField;
    private JBTextField usernameField;
    private JBTextField collaborationServerUrlField;
    private JBCheckBox enableContinuousRefactoringCheckBox;
    private JBCheckBox enableAIAssistCheckBox;
    private JBTextField maxTokensPerRequestField;
    private ComboBox<String> openAiModelComboBox;
    private JBCheckBox usePatternRecognitionCheckBox;
    private JBTextField collaborationRefreshRateField;
    private JBLabel apiKeyWarningLabel;
    
    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());
        
        // Create fields
        openAiApiKeyField = new JBPasswordField();
        usernameField = new JBTextField();
        collaborationServerUrlField = new JBTextField();
        enableContinuousRefactoringCheckBox = new JBCheckBox("Enable continuous refactoring");
        enableAIAssistCheckBox = new JBCheckBox("Enable AI assist");
        maxTokensPerRequestField = new JBTextField();
        openAiModelComboBox = new ComboBox<>(new String[]{"gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"});
        usePatternRecognitionCheckBox = new JBCheckBox("Use pattern recognition (reduces API usage)");
        collaborationRefreshRateField = new JBTextField();
        apiKeyWarningLabel = new JBLabel("Warning: API key not set");
        apiKeyWarningLabel.setForeground(JBColor.RED);
        apiKeyWarningLabel.setVisible(false);
        
        // Add document listener to API key field to show/hide warning
        openAiApiKeyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateApiKeyWarning();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateApiKeyWarning();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateApiKeyWarning();
            }
            
            private void updateApiKeyWarning() {
                String apiKey = String.valueOf(openAiApiKeyField.getPassword());
                apiKeyWarningLabel.setVisible(apiKey.isEmpty());
            }
        });
        
        // Build UI
        JPanel settingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI API Key:"), 
                        UI.PanelFactory.panel(openAiApiKeyField).withComment(
                                "Required for AI code generation and refactoring").createPanel())
                .addComponentToRightColumn(apiKeyWarningLabel)
                .addLabeledComponent(new JBLabel("OpenAI Model:"), openAiModelComboBox)
                .addLabeledComponent(new JBLabel("Max Tokens per Request:"), maxTokensPerRequestField)
                .addLabeledComponent(new JBLabel("Username:"), usernameField)
                .addLabeledComponent(new JBLabel("Collaboration Server URL:"), collaborationServerUrlField)
                .addLabeledComponent(new JBLabel("Collaboration Refresh Rate (ms):"), collaborationRefreshRateField)
                .addComponentToRightColumn(enableContinuousRefactoringCheckBox)
                .addComponentToRightColumn(enableAIAssistCheckBox)
                .addComponentToRightColumn(usePatternRecognitionCheckBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Create titled panel
        JPanel titledPanel = UI.PanelFactory.panel(settingsPanel)
                .withLabel("ModForge Settings")
                .withComment("Configure settings for the ModForge plugin")
                .createPanel();
        
        mainPanel.add(titledPanel, BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Load settings
        reset();
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        String apiKey = String.valueOf(openAiApiKeyField.getPassword());
        String username = usernameField.getText();
        String serverUrl = collaborationServerUrlField.getText();
        boolean continuousRefactoring = enableContinuousRefactoringCheckBox.isSelected();
        boolean aiAssist = enableAIAssistCheckBox.isSelected();
        int maxTokens = getIntFromField(maxTokensPerRequestField, settings.getMaxTokensPerRequest());
        String model = (String) openAiModelComboBox.getSelectedItem();
        boolean patternRecognition = usePatternRecognitionCheckBox.isSelected();
        int refreshRate = getIntFromField(collaborationRefreshRateField, settings.getCollaborationRefreshRate());
        
        return !apiKey.equals(settings.getOpenAiApiKey()) ||
                !username.equals(settings.getUsername()) ||
                !serverUrl.equals(settings.getCollaborationServerUrl()) ||
                continuousRefactoring != settings.isEnableContinuousRefactoring() ||
                aiAssist != settings.isEnableAIAssist() ||
                maxTokens != settings.getMaxTokensPerRequest() ||
                (model != null && !model.equals(settings.getOpenAiModel())) ||
                patternRecognition != settings.isUsePatternRecognition() ||
                refreshRate != settings.getCollaborationRefreshRate();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Validate input
        int maxTokens = getIntFromField(maxTokensPerRequestField, settings.getMaxTokensPerRequest());
        if (maxTokens <= 0) {
            throw new ConfigurationException("Max tokens per request must be positive");
        }
        
        int refreshRate = getIntFromField(collaborationRefreshRateField, settings.getCollaborationRefreshRate());
        if (refreshRate <= 0) {
            throw new ConfigurationException("Collaboration refresh rate must be positive");
        }
        
        // Save settings
        settings.setOpenAiApiKey(String.valueOf(openAiApiKeyField.getPassword()));
        settings.setUsername(usernameField.getText());
        settings.setCollaborationServerUrl(collaborationServerUrlField.getText());
        settings.setEnableContinuousRefactoring(enableContinuousRefactoringCheckBox.isSelected());
        settings.setEnableAIAssist(enableAIAssistCheckBox.isSelected());
        settings.setMaxTokensPerRequest(maxTokens);
        
        String model = (String) openAiModelComboBox.getSelectedItem();
        if (model != null) {
            settings.setOpenAiModel(model);
        }
        
        settings.setUsePatternRecognition(usePatternRecognitionCheckBox.isSelected());
        settings.setCollaborationRefreshRate(refreshRate);
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        openAiApiKeyField.setText(settings.getOpenAiApiKey());
        usernameField.setText(settings.getUsername());
        collaborationServerUrlField.setText(settings.getCollaborationServerUrl());
        enableContinuousRefactoringCheckBox.setSelected(settings.isEnableContinuousRefactoring());
        enableAIAssistCheckBox.setSelected(settings.isEnableAIAssist());
        maxTokensPerRequestField.setText(String.valueOf(settings.getMaxTokensPerRequest()));
        openAiModelComboBox.setSelectedItem(settings.getOpenAiModel());
        usePatternRecognitionCheckBox.setSelected(settings.isUsePatternRecognition());
        collaborationRefreshRateField.setText(String.valueOf(settings.getCollaborationRefreshRate()));
        
        // Check if API key is set
        String apiKey = settings.getOpenAiApiKey();
        apiKeyWarningLabel.setVisible(apiKey.isEmpty());
    }
    
    /**
     * Gets an integer from a text field.
     * @param field The text field
     * @param defaultValue The default value
     * @return The integer value
     */
    private int getIntFromField(JBTextField field, int defaultValue) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}