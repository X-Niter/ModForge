package com.modforge.intellij.plugin.ui.dialog;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for logging in to ModForge server.
 */
public class LoginDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(LoginDialog.class);
    
    private JBTextField serverUrlField;
    private JBTextField usernameField;
    private JBPasswordField passwordField;
    private JCheckBox rememberCredentialsCheckBox;
    private JLabel statusLabel;
    
    /**
     * Constructor.
     * @param project The project
     */
    public LoginDialog(@Nullable Project project) {
        super(project);
        
        setTitle("Login to ModForge");
        setOKButtonText("Login");
        setSize(400, 200);
        
        init();
        
        // Load settings
        loadSettings();
    }
    
    /**
     * Load settings.
     */
    private void loadSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        serverUrlField.setText(settings.getServerUrl());
        usernameField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        rememberCredentialsCheckBox.setSelected(settings.isRememberCredentials());
    }
    
    /**
     * Save settings.
     */
    private void saveSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setServerUrl(serverUrlField.getText());
        settings.setUsername(usernameField.getText());
        settings.setPassword(String.valueOf(passwordField.getPassword()));
        settings.setRememberCredentials(rememberCredentialsCheckBox.isSelected());
    }
    
    @Override
    protected @Nullable ValidationInfo doValidate() {
        // Validate server URL
        if (serverUrlField.getText().isEmpty()) {
            return new ValidationInfo("Server URL is required", serverUrlField);
        }
        
        // Validate username
        if (usernameField.getText().isEmpty()) {
            return new ValidationInfo("Username is required", usernameField);
        }
        
        // Validate password
        if (passwordField.getPassword().length == 0) {
            return new ValidationInfo("Password is required", passwordField);
        }
        
        return null;
    }
    
    @Override
    protected void doOKAction() {
        // Save settings
        saveSettings();
        
        // Test connection
        String serverUrl = serverUrlField.getText();
        String username = usernameField.getText();
        String password = String.valueOf(passwordField.getPassword());
        
        // Test connection and get token
        boolean connected = ConnectionTestUtil.testConnection(serverUrl);
        
        if (!connected) {
            setStatusError("Could not connect to server: " + serverUrl);
            return;
        }
        
        // Test authentication
        String token = AuthTestUtil.getAccessToken(serverUrl, username, password);
        
        if (token == null) {
            setStatusError("Authentication failed. Check your credentials.");
            return;
        }
        
        // Save token
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setAccessToken(token);
        settings.setAuthenticated(true);
        
        setStatusSuccess("Successfully logged in");
        
        // Close dialog
        super.doOKAction();
    }
    
    /**
     * Set status success.
     * @param message Message
     */
    private void setStatusSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(0, 128, 0));
    }
    
    /**
     * Set status error.
     * @param message Message
     */
    private void setStatusError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.RED);
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel panel = new JBPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Server URL
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Server URL:"), c);
        
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        serverUrlField = new JBTextField();
        panel.add(serverUrlField, c);
        
        // Test connection button
        c.gridx = 3;
        c.gridwidth = 1;
        c.weightx = 0.0;
        JButton testConnectionButton = new JButton("Test");
        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverUrl = serverUrlField.getText();
                
                if (serverUrl.isEmpty()) {
                    setStatusError("Server URL is required");
                    return;
                }
                
                boolean connected = ConnectionTestUtil.testConnection(serverUrl);
                
                if (connected) {
                    setStatusSuccess("Successfully connected to server");
                } else {
                    setStatusError("Could not connect to server: " + serverUrl);
                }
            }
        });
        panel.add(testConnectionButton, c);
        
        // Username
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1.0;
        usernameField = new JBTextField();
        panel.add(usernameField, c);
        
        // Password
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Password:"), c);
        
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1.0;
        passwordField = new JBPasswordField();
        panel.add(passwordField, c);
        
        // Remember credentials
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Remember credentials:"), c);
        
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1.0;
        rememberCredentialsCheckBox = new JCheckBox();
        panel.add(rememberCredentialsCheckBox, c);
        
        // Status
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 4;
        c.weightx = 1.0;
        statusLabel = new JLabel(" ");
        panel.add(statusLabel, c);
        
        return panel;
    }
}