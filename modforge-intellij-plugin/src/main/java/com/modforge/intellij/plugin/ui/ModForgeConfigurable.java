package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * UI for ModForge settings.
 * This class provides the UI components for the ModForge settings.
 */
public class ModForgeConfigurable {
    private final JPanel panel;
    private final JBTextField serverUrlField;
    private final JBTextField collaborationServerUrlField;
    private final JBTextField usernameField;
    private final JBPasswordField passwordField;
    private final JBTextField openAiApiKeyField;
    private final JBTextField openAiModelField;
    private final JBTextField maxTokensField;
    private final JBTextField temperatureField;
    private final JBTextField githubUsernameField;
    private final JBPasswordField githubTokenField;
    private final JBCheckBox enablePatternRecognitionCheckBox;
    private final JBCheckBox enableContinuousDevelopmentCheckBox;
    private final JBCheckBox enableNotificationsCheckBox;
    private final JBCheckBox useDarkModeCheckBox;
    private final JBCheckBox enableGitHubIntegrationCheckBox;
    
    /**
     * Creates a new ModForge configurable.
     */
    public ModForgeConfigurable() {
        // Initialize fields
        serverUrlField = new JBTextField();
        collaborationServerUrlField = new JBTextField();
        usernameField = new JBTextField();
        passwordField = new JBPasswordField();
        openAiApiKeyField = new JBTextField();
        openAiModelField = new JBTextField();
        maxTokensField = new JBTextField();
        temperatureField = new JBTextField();
        githubUsernameField = new JBTextField();
        githubTokenField = new JBPasswordField();
        enablePatternRecognitionCheckBox = new JBCheckBox("Enable Pattern Recognition");
        enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable Continuous Development");
        enableNotificationsCheckBox = new JBCheckBox("Enable Notifications");
        useDarkModeCheckBox = new JBCheckBox("Use Dark Mode");
        enableGitHubIntegrationCheckBox = new JBCheckBox("Enable GitHub Integration");
        
        // Build UI
        JPanel generalPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Server URL:", serverUrlField)
                .addLabeledComponent("Collaboration Server URL:", collaborationServerUrlField)
                .addLabeledComponent("Username:", usernameField)
                .addLabeledComponent("Password:", passwordField)
                .addComponent(enablePatternRecognitionCheckBox)
                .addComponent(enableContinuousDevelopmentCheckBox)
                .addComponent(enableNotificationsCheckBox)
                .addComponent(useDarkModeCheckBox)
                .getPanel();
        
        JPanel openAIPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("OpenAI API Key:", openAiApiKeyField)
                .addLabeledComponent("OpenAI Model:", openAiModelField)
                .addLabeledComponent("Max Tokens:", maxTokensField)
                .addLabeledComponent("Temperature:", temperatureField)
                .getPanel();
        
        JPanel githubPanel = FormBuilder.createFormBuilder()
                .addComponent(enableGitHubIntegrationCheckBox)
                .addLabeledComponent("GitHub Username:", githubUsernameField)
                .addLabeledComponent("GitHub Token:", githubTokenField)
                .getPanel();
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("General", generalPanel);
        tabbedPane.addTab("OpenAI", openAIPanel);
        tabbedPane.addTab("GitHub", githubPanel);
        
        panel = new JPanel(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Load settings
        reset();
    }
    
    /**
     * Gets the panel.
     *
     * @return The panel
     */
    public JPanel getPanel() {
        return panel;
    }
    
    /**
     * Checks if the settings have been modified.
     *
     * @return True if the settings have been modified, false otherwise
     */
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !serverUrlField.getText().equals(settings.getServerUrl())
                || !collaborationServerUrlField.getText().equals(settings.getCollaborationServerUrl())
                || !usernameField.getText().equals(settings.getUsername())
                || !String.valueOf(passwordField.getPassword()).equals(settings.getPassword())
                || !openAiApiKeyField.getText().equals(settings.getOpenAiApiKey())
                || !openAiModelField.getText().equals(settings.getOpenAiModel())
                || !maxTokensField.getText().equals(String.valueOf(settings.getMaxTokens()))
                || !temperatureField.getText().equals(String.valueOf(settings.getTemperature()))
                || !githubUsernameField.getText().equals(settings.getGitHubUsername())
                || !String.valueOf(githubTokenField.getPassword()).equals(settings.getGithubToken())
                || enablePatternRecognitionCheckBox.isSelected() != settings.isPatternRecognition()
                || enableContinuousDevelopmentCheckBox.isSelected() != settings.isEnableContinuousDevelopment()
                || enableNotificationsCheckBox.isSelected() != settings.isEnableNotifications()
                || useDarkModeCheckBox.isSelected() != settings.isUseDarkMode()
                || enableGitHubIntegrationCheckBox.isSelected() != settings.isEnableGitHubIntegration();
    }
    
    /**
     * Applies the settings.
     *
     * @throws ConfigurationException If there's an error applying the settings
     */
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setServerUrl(serverUrlField.getText());
        settings.setCollaborationServerUrl(collaborationServerUrlField.getText());
        settings.setUsername(usernameField.getText());
        settings.setPassword(String.valueOf(passwordField.getPassword()));
        settings.setOpenAiApiKey(openAiApiKeyField.getText());
        settings.setOpenAiModel(openAiModelField.getText());
        
        try {
            settings.setMaxTokens(Integer.parseInt(maxTokensField.getText()));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Max Tokens must be a valid integer");
        }
        
        try {
            settings.setTemperature(Double.parseDouble(temperatureField.getText()));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Temperature must be a valid number");
        }
        
        settings.setGitHubUsername(githubUsernameField.getText());
        settings.setGithubToken(String.valueOf(githubTokenField.getPassword()));
        settings.setPatternRecognition(enablePatternRecognitionCheckBox.isSelected());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setEnableNotifications(enableNotificationsCheckBox.isSelected());
        settings.setUseDarkMode(useDarkModeCheckBox.isSelected());
        settings.setEnableGitHubIntegration(enableGitHubIntegrationCheckBox.isSelected());
    }
    
    /**
     * Resets the settings.
     */
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        serverUrlField.setText(settings.getServerUrl());
        collaborationServerUrlField.setText(settings.getCollaborationServerUrl());
        usernameField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        openAiApiKeyField.setText(settings.getOpenAiApiKey());
        openAiModelField.setText(settings.getOpenAiModel());
        maxTokensField.setText(String.valueOf(settings.getMaxTokens()));
        temperatureField.setText(String.valueOf(settings.getTemperature()));
        githubUsernameField.setText(settings.getGitHubUsername());
        githubTokenField.setText(settings.getGithubToken());
        enablePatternRecognitionCheckBox.setSelected(settings.isPatternRecognition());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        enableNotificationsCheckBox.setSelected(settings.isEnableNotifications());
        useDarkModeCheckBox.setSelected(settings.isUseDarkMode());
        enableGitHubIntegrationCheckBox.setSelected(settings.isEnableGitHubIntegration());
    }
}