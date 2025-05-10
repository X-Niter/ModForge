package com.modforge.intellij.plugin.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for logging in to the ModForge server.
 */
public class LoginDialog extends DialogWrapper {
    private final JBTextField usernameField;
    private final JBPasswordField passwordField;
    private final JBTextField serverUrlField;
    private final JCheckBox rememberCredentialsCheckBox;
    private final Project project;

    /**
     * Constructor.
     * @param project The project
     */
    public LoginDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        usernameField = new JBTextField(settings.getUsername());
        passwordField = new JBPasswordField();
        
        if (!settings.getPassword().isEmpty()) {
            passwordField.setText(settings.getPassword());
        }
        
        serverUrlField = new JBTextField(settings.getServerUrl());
        rememberCredentialsCheckBox = new JCheckBox("Remember credentials", settings.isRememberCredentials());
        
        setTitle("Login to ModForge");
        setOKButtonText("Login");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Server URL
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JBLabel("Server URL:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(serverUrlField, c);
        
        // Test Connection button
        c.gridx = 2;
        c.weightx = 0.0;
        JButton testConnectionButton = new JButton("Test Connection");
        panel.add(testConnectionButton, c);
        
        // Username
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        panel.add(usernameField, c);
        
        // Password
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Password:"), c);
        
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        panel.add(passwordField, c);
        
        // Remember credentials
        c.gridx = 1;
        c.gridy = 3;
        panel.add(rememberCredentialsCheckBox, c);
        
        // Status message
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        panel.add(statusLabel, c);
        
        // Add action to test connection button
        testConnectionButton.addActionListener(e -> {
            String serverUrl = serverUrlField.getText().trim();
            
            if (serverUrl.isEmpty()) {
                statusLabel.setText("Server URL cannot be empty");
                return;
            }
            
            statusLabel.setText("Testing connection...");
            statusLabel.setForeground(Color.BLUE);
            
            SwingUtilities.invokeLater(() -> {
                boolean connected = ConnectionTestUtil.testConnection(serverUrl);
                
                if (connected) {
                    statusLabel.setText("Connection successful!");
                    statusLabel.setForeground(new Color(0, 128, 0)); // Dark green
                } else {
                    statusLabel.setText("Connection failed. Please check server URL.");
                    statusLabel.setForeground(Color.RED);
                }
            });
        });
        
        return panel;
    }

    @Override
    protected void doOKAction() {
        // Save settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setServerUrl(serverUrlField.getText().trim());
        settings.setRememberCredentials(rememberCredentialsCheckBox.isSelected());
        
        if (rememberCredentialsCheckBox.isSelected()) {
            settings.setUsername(usernameField.getText().trim());
            settings.setPassword(new String(passwordField.getPassword()));
        } else {
            settings.setUsername("");
            settings.setPassword("");
        }
        
        // Try to authenticate
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        authManager.setCredentials(usernameField.getText().trim(), new String(passwordField.getPassword()));
        boolean success = authManager.authenticate();
        
        if (success) {
            super.doOKAction();
        } else {
            setErrorText("Authentication failed. Please check your credentials.");
        }
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (usernameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Username cannot be empty", usernameField);
        }
        
        if (passwordField.getPassword().length == 0) {
            return new ValidationInfo("Password cannot be empty", passwordField);
        }
        
        if (serverUrlField.getText().trim().isEmpty()) {
            return new ValidationInfo("Server URL cannot be empty", serverUrlField);
        }
        
        return null;
    }
}