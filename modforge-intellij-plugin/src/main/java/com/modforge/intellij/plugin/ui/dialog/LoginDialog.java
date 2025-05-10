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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for logging in to the ModForge server.
 */
public class LoginDialog extends DialogWrapper {
    private final JBTextField serverUrlField = new JBTextField();
    private final JBTextField usernameField = new JBTextField();
    private final JBPasswordField passwordField = new JBPasswordField();
    private final JLabel statusLabel = new JLabel();
    
    /**
     * Creates a new login dialog.
     * @param project The project
     */
    public LoginDialog(@Nullable Project project) {
        super(project);
        setTitle("Login to ModForge");
        setResizable(false);
        
        // Initialize fields with current settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        serverUrlField.setText(settings.getServerUrl());
        usernameField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        
        init();
    }
    
    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (serverUrlField.getText().trim().isEmpty()) {
            return new ValidationInfo("Server URL is required", serverUrlField);
        }
        
        if (usernameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Username is required", usernameField);
        }
        
        if (passwordField.getPassword().length == 0) {
            return new ValidationInfo("Password is required", passwordField);
        }
        
        return null;
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
        serverUrlField.setPreferredSize(new Dimension(300, serverUrlField.getPreferredSize().height));
        panel.add(serverUrlField, c);
        
        // Username
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(usernameField, c);
        
        // Password
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        panel.add(new JBLabel("Password:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(passwordField, c);
        
        // Status label
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        panel.add(statusLabel, c);
        
        return panel;
    }
    
    @Override
    protected void doOKAction() {
        // Save settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setServerUrl(serverUrlField.getText().trim());
        settings.setUsername(usernameField.getText().trim());
        settings.setPassword(new String(passwordField.getPassword()));
        
        // Try to authenticate
        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));
        
        if (AuthenticationManager.getInstance().authenticate()) {
            super.doOKAction();
        } else {
            statusLabel.setText("Authentication failed. Please check your credentials.");
            statusLabel.setForeground(Color.RED);
        }
    }
}