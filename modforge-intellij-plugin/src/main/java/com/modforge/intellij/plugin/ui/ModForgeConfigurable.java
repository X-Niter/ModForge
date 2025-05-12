package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable for ModForge settings.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ModForgeConfigurable implements Configurable {
    private JPanel mainPanel;
    private JBTextField serverUrlField;
    private JSpinner requestTimeoutSpinner;
    private JBCheckBox enablePatternRecognitionCheckBox;
    private JBCheckBox enableContinuousDevelopmentCheckBox;
    private JBTextField githubUsernameField;
    private JPasswordField accessTokenField;
    
    private final ModForgeSettings settings;
    
    /**
     * Constructor.
     */
    public ModForgeConfigurable() {
        settings = ModForgeSettings.getInstance();
    }

    /**
     * Gets the display name.
     *
     * @return The display name.
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }

    /**
     * Gets the help topic.
     *
     * @return The help topic.
     */
    @Nullable
    @Override
    public String getHelpTopic() {
        return "ModForge.Settings";
    }

    /**
     * Creates the component.
     *
     * @return The component.
     */
    @Override
    public JComponent createComponent() {
        mainPanel = createSettingsPanel();
        return mainPanel;
    }

    /**
     * Checks if the settings are modified.
     *
     * @return Whether the settings are modified.
     */
    @Override
    public boolean isModified() {
        return !serverUrlField.getText().equals(settings.getServerUrl())
                || (int) requestTimeoutSpinner.getValue() != settings.getRequestTimeout()
                || enablePatternRecognitionCheckBox.isSelected() != settings.isPatternRecognition()
                || enableContinuousDevelopmentCheckBox.isSelected() != settings.isEnableContinuousDevelopment()
                || !githubUsernameField.getText().equals(settings.getGitHubUsername())
                || !new String(accessTokenField.getPassword()).equals(settings.getAccessToken());
    }

    /**
     * Applies the settings.
     *
     * @throws ConfigurationException If the settings are invalid.
     */
    @Override
    public void apply() throws ConfigurationException {
        settings.setServerUrl(serverUrlField.getText());
        settings.setRequestTimeout((int) requestTimeoutSpinner.getValue());
        settings.setPatternRecognition(enablePatternRecognitionCheckBox.isSelected());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setGitHubUsername(githubUsernameField.getText());
        settings.setAccessToken(new String(accessTokenField.getPassword()));
    }

    /**
     * Resets the settings.
     */
    @Override
    public void reset() {
        serverUrlField.setText(settings.getServerUrl());
        requestTimeoutSpinner.setValue(settings.getRequestTimeout());
        enablePatternRecognitionCheckBox.setSelected(settings.isPatternRecognition());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        githubUsernameField.setText(settings.getGitHubUsername());
        accessTokenField.setText(settings.getAccessToken());
    }

    /**
     * Creates the settings panel.
     *
     * @return The settings panel.
     */
    private JPanel createSettingsPanel() {
        // API Settings
        serverUrlField = new JBTextField(settings.getServerUrl());
        
        SpinnerNumberModel timeoutModel = new SpinnerNumberModel(
                settings.getRequestTimeout(),
                1,
                300,
                1
        );
        requestTimeoutSpinner = new JSpinner(timeoutModel);
        
        // Feature Settings
        enablePatternRecognitionCheckBox = new JBCheckBox("Enable pattern recognition", settings.isPatternRecognition());
        enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development", settings.isEnableContinuousDevelopment());
        
        // GitHub Settings
        githubUsernameField = new JBTextField(settings.getGitHubUsername());
        accessTokenField = new JPasswordField(settings.getAccessToken());
        
        // Build the form
        FormBuilder apiBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField)
                .addLabeledComponent(new JBLabel("Request timeout (seconds):"), requestTimeoutSpinner)
                .addComponentToRightColumn(new JBLabel(""))
                .addComponentToRightColumn(enablePatternRecognitionCheckBox)
                .addComponentToRightColumn(enableContinuousDevelopmentCheckBox);
        
        FormBuilder githubBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("GitHub username:"), githubUsernameField)
                .addLabeledComponent(new JBLabel("Access token:"), accessTokenField);
        
        JPanel apiPanel = apiBuilder.getPanel();
        JPanel githubPanel = githubBuilder.getPanel();
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("API Settings", apiPanel);
        tabbedPane.addTab("GitHub Settings", githubPanel);
        
        // Wrap in panel with padding
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(tabbedPane, BorderLayout.CENTER);
        wrapper.setBorder(JBUI.Borders.empty(10));
        
        return wrapper;
    }

    /**
     * Disposes the component.
     */
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        serverUrlField = null;
        requestTimeoutSpinner = null;
        enablePatternRecognitionCheckBox = null;
        enableContinuousDevelopmentCheckBox = null;
        githubUsernameField = null;
        accessTokenField = null;
    }
}