package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings UI for ModForge.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    
    // AI settings components
    private JBPasswordField apiKeyField;
    private JSpinner maxTokensField;
    private JCheckBox usePatternLearningField;
    
    // Collaboration settings components
    private JBTextField serverUrlField;
    private JBTextField usernameField;
    
    // Code generation settings components
    private JCheckBox generateJavadocField;
    private JCheckBox addCopyrightHeaderField;
    private JBTextField copyrightTextField;
    
    // UI settings components
    private JCheckBox showMetricsInStatusBarField;
    private JCheckBox enableNotificationsField;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }

    @Override
    public @Nullable JComponent createComponent() {
        // Initialize fields
        apiKeyField = new JBPasswordField();
        apiKeyField.setEmptyText("Enter your OpenAI API key");
        
        maxTokensField = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        
        usePatternLearningField = new JCheckBox("Use pattern learning to reduce API costs");
        
        serverUrlField = new JBTextField();
        serverUrlField.setEmptyText("WebSocket server URL for collaboration");
        
        usernameField = new JBTextField();
        usernameField.setEmptyText("Your username for collaboration");
        
        generateJavadocField = new JCheckBox("Generate Javadoc comments for generated code");
        
        addCopyrightHeaderField = new JCheckBox("Add copyright header to generated files");
        
        copyrightTextField = new JBTextField();
        copyrightTextField.setEmptyText("Copyright text (${YEAR} will be replaced with current year)");
        
        showMetricsInStatusBarField = new JCheckBox("Show metrics in status bar");
        
        enableNotificationsField = new JCheckBox("Enable notifications");
        
        // Create tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // AI settings tab
        JPanel aiSettingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("OpenAI API Key:", apiKeyField)
                .addLabeledComponent("Max Tokens per Request:", maxTokensField)
                .addComponent(usePatternLearningField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        tabbedPane.addTab("AI Settings", aiSettingsPanel);
        
        // Collaboration settings tab
        JPanel collaborationSettingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Server URL:", serverUrlField)
                .addLabeledComponent("Username:", usernameField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        tabbedPane.addTab("Collaboration", collaborationSettingsPanel);
        
        // Code generation settings tab
        JPanel codeGenSettingsPanel = FormBuilder.createFormBuilder()
                .addComponent(generateJavadocField)
                .addComponent(addCopyrightHeaderField)
                .addLabeledComponent("Copyright Text:", copyrightTextField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        tabbedPane.addTab("Code Generation", codeGenSettingsPanel);
        
        // UI settings tab
        JPanel uiSettingsPanel = FormBuilder.createFormBuilder()
                .addComponent(showMetricsInStatusBarField)
                .addComponent(enableNotificationsField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        tabbedPane.addTab("UI", uiSettingsPanel);
        
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add help text
        JPanel helpPanel = new JPanel(new BorderLayout());
        JBLabel helpLabel = new JBLabel("<html><body><p>ModForge settings for AI-powered Minecraft mod development.</p></body></html>");
        helpLabel.setBorder(JBUI.Borders.empty(10));
        helpPanel.add(helpLabel, BorderLayout.CENTER);
        mainPanel.add(helpPanel, BorderLayout.NORTH);
        
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !String.valueOf(apiKeyField.getPassword()).equals(settings.getOpenAiApiKey()) ||
                (Integer) maxTokensField.getValue() != settings.getMaxTokensPerRequest() ||
                usePatternLearningField.isSelected() != settings.isUsePatternLearning() ||
                !serverUrlField.getText().equals(settings.getCollaborationServerUrl()) ||
                !usernameField.getText().equals(settings.getUsername()) ||
                generateJavadocField.isSelected() != settings.isGenerateJavadoc() ||
                addCopyrightHeaderField.isSelected() != settings.isAddCopyrightHeader() ||
                !copyrightTextField.getText().equals(settings.getCopyrightText()) ||
                showMetricsInStatusBarField.isSelected() != settings.isShowMetricsInStatusBar() ||
                enableNotificationsField.isSelected() != settings.isEnableNotifications();
    }

    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Validate API key
        String apiKey = String.valueOf(apiKeyField.getPassword());
        if (apiKey.isEmpty()) {
            throw new ConfigurationException("OpenAI API key cannot be empty");
        }
        
        // Validate server URL
        String serverUrl = serverUrlField.getText();
        if (serverUrl.isEmpty()) {
            throw new ConfigurationException("Collaboration server URL cannot be empty");
        }
        
        // Apply settings
        settings.setOpenAiApiKey(apiKey);
        settings.setMaxTokensPerRequest((Integer) maxTokensField.getValue());
        settings.setUsePatternLearning(usePatternLearningField.isSelected());
        settings.setCollaborationServerUrl(serverUrl);
        settings.setUsername(usernameField.getText());
        settings.setGenerateJavadoc(generateJavadocField.isSelected());
        settings.setAddCopyrightHeader(addCopyrightHeaderField.isSelected());
        settings.setCopyrightText(copyrightTextField.getText());
        settings.setShowMetricsInStatusBar(showMetricsInStatusBarField.isSelected());
        settings.setEnableNotifications(enableNotificationsField.isSelected());
    }

    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Reset fields to settings values
        apiKeyField.setText(settings.getOpenAiApiKey());
        maxTokensField.setValue(settings.getMaxTokensPerRequest());
        usePatternLearningField.setSelected(settings.isUsePatternLearning());
        serverUrlField.setText(settings.getCollaborationServerUrl());
        usernameField.setText(settings.getUsername());
        generateJavadocField.setSelected(settings.isGenerateJavadoc());
        addCopyrightHeaderField.setSelected(settings.isAddCopyrightHeader());
        copyrightTextField.setText(settings.getCopyrightText());
        showMetricsInStatusBarField.setSelected(settings.isShowMetricsInStatusBar());
        enableNotificationsField.setSelected(settings.isEnableNotifications());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}