package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings UI for ModForge.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JTextField serverUrlField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox rememberCredentialsCheckBox;
    private JCheckBox continuousDevelopmentCheckBox;
    private JCheckBox patternRecognitionCheckBox;
    private JSpinner pollingIntervalSpinner;
    private JButton testConnectionButton;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        
        // Server URL
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(new JLabel("Server URL:"), c);
        
        c.gridx = 1;
        c.gridy = 0;
        serverUrlField = new JTextField();
        mainPanel.add(serverUrlField, c);
        
        // Test Connection
        c.gridx = 2;
        c.gridy = 0;
        testConnectionButton = new JButton("Test Connection");
        testConnectionButton.addActionListener(e -> testConnection());
        mainPanel.add(testConnectionButton, c);
        
        // Username
        c.gridx = 0;
        c.gridy = 1;
        mainPanel.add(new JLabel("Username:"), c);
        
        c.gridx = 1;
        c.gridy = 1;
        usernameField = new JTextField();
        mainPanel.add(usernameField, c);
        
        // Password
        c.gridx = 0;
        c.gridy = 2;
        mainPanel.add(new JLabel("Password:"), c);
        
        c.gridx = 1;
        c.gridy = 2;
        passwordField = new JPasswordField();
        mainPanel.add(passwordField, c);
        
        // Remember Credentials
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        rememberCredentialsCheckBox = new JCheckBox("Remember Credentials");
        mainPanel.add(rememberCredentialsCheckBox, c);
        
        // Continuous Development
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        continuousDevelopmentCheckBox = new JCheckBox("Enable Continuous Development");
        mainPanel.add(continuousDevelopmentCheckBox, c);
        
        // Pattern Recognition
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        patternRecognitionCheckBox = new JCheckBox("Enable Pattern Recognition");
        mainPanel.add(patternRecognitionCheckBox, c);
        
        // Polling Interval
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        mainPanel.add(new JLabel("Polling Interval (minutes):"), c);
        
        c.gridx = 1;
        c.gridy = 6;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 60, 1);
        pollingIntervalSpinner = new JSpinner(spinnerModel);
        mainPanel.add(pollingIntervalSpinner, c);
        
        // Load settings
        loadSettings();
        
        return mainPanel;
    }

    /**
     * Test connection to server.
     */
    private void testConnection() {
        String serverUrl = serverUrlField.getText();
        
        if (serverUrl.isEmpty()) {
            Messages.showErrorDialog("Server URL is required", "Error");
            return;
        }
        
        boolean isConnected = AuthTestUtil.testConnection(serverUrl);
        
        if (isConnected) {
            Messages.showInfoMessage("Connection successful", "Success");
        } else {
            Messages.showErrorDialog("Connection failed", "Error");
        }
    }
    
    /**
     * Load settings from ModForgeSettings.
     */
    private void loadSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        serverUrlField.setText(settings.getServerUrl());
        usernameField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        rememberCredentialsCheckBox.setSelected(settings.isRememberCredentials());
        continuousDevelopmentCheckBox.setSelected(settings.isContinuousDevelopment());
        patternRecognitionCheckBox.setSelected(settings.isPatternRecognition());
        pollingIntervalSpinner.setValue(settings.getPollingInterval() / (60 * 1000)); // Convert from milliseconds to minutes
    }
    
    /**
     * Save settings to ModForgeSettings.
     */
    private void saveSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setServerUrl(serverUrlField.getText());
        
        // If remember credentials is checked, save them
        if (rememberCredentialsCheckBox.isSelected()) {
            settings.setUsername(usernameField.getText());
            settings.setPassword(new String(passwordField.getPassword()));
        } else {
            // If not, only update if the user is not authenticated yet
            if (!settings.isAuthenticated()) {
                settings.setUsername(usernameField.getText());
                settings.setPassword(new String(passwordField.getPassword()));
            }
        }
        
        settings.setRememberCredentials(rememberCredentialsCheckBox.isSelected());
        settings.setContinuousDevelopment(continuousDevelopmentCheckBox.isSelected());
        settings.setPatternRecognition(patternRecognitionCheckBox.isSelected());
        settings.setPollingInterval((Integer) pollingIntervalSpinner.getValue() * 60 * 1000); // Convert from minutes to milliseconds
    }

    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        boolean modified = false;
        
        // Check if any field is modified
        modified |= !serverUrlField.getText().equals(settings.getServerUrl());
        modified |= !usernameField.getText().equals(settings.getUsername());
        modified |= !new String(passwordField.getPassword()).equals(settings.getPassword());
        modified |= rememberCredentialsCheckBox.isSelected() != settings.isRememberCredentials();
        modified |= continuousDevelopmentCheckBox.isSelected() != settings.isContinuousDevelopment();
        modified |= patternRecognitionCheckBox.isSelected() != settings.isPatternRecognition();
        modified |= (Integer) pollingIntervalSpinner.getValue() * 60 * 1000 != settings.getPollingInterval();
        
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        saveSettings();
        
        // If authentication is needed, re-authenticate
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.isAuthenticated()) {
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.verifyAuthentication()) {
                // Try to re-authenticate
                if (!authManager.login(settings.getUsername(), settings.getPassword())) {
                    throw new ConfigurationException("Authentication failed. Please check your credentials.");
                }
            }
        }
    }

    @Override
    public void reset() {
        loadSettings();
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        serverUrlField = null;
        usernameField = null;
        passwordField = null;
        rememberCredentialsCheckBox = null;
        continuousDevelopmentCheckBox = null;
        patternRecognitionCheckBox = null;
        pollingIntervalSpinner = null;
        testConnectionButton = null;
    }
}