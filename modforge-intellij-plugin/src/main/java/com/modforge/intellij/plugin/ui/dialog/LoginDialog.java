package com.modforge.intellij.plugin.ui.dialog;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for login to ModForge server.
 */
public class LoginDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(LoginDialog.class);
    
    private JBTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox rememberCheckBox;
    
    public LoginDialog(Project project) {
        super(project);
        setTitle("Login to ModForge");
        setOKButtonText("Login");
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        // Create UI components
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.insets(5);
        
        // Username
        c.gridx = 0;
        c.gridy = 0;
        JBLabel usernameLabel = new JBLabel("Username:");
        panel.add(usernameLabel, c);
        
        c.gridx = 1;
        c.gridy = 0;
        usernameField = new JBTextField();
        panel.add(usernameField, c);
        
        // Password
        c.gridx = 0;
        c.gridy = 1;
        JBLabel passwordLabel = new JBLabel("Password:");
        panel.add(passwordLabel, c);
        
        c.gridx = 1;
        c.gridy = 1;
        passwordField = new JPasswordField();
        panel.add(passwordField, c);
        
        // Remember credentials
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        rememberCheckBox = new JCheckBox("Remember credentials");
        panel.add(rememberCheckBox, c);
        
        // Load saved credentials if available
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (!settings.getUsername().isEmpty()) {
            usernameField.setText(settings.getUsername());
            passwordField.setText(settings.getPassword());
        }
        rememberCheckBox.setSelected(settings.isRememberCredentials());
        
        return panel;
    }
    
    @Override
    protected @Nullable ValidationInfo doValidate() {
        // Validate username and password
        if (usernameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Username cannot be empty", usernameField);
        }
        
        if (passwordField.getPassword().length == 0) {
            return new ValidationInfo("Password cannot be empty", passwordField);
        }
        
        return null;
    }
    
    @Override
    protected void doOKAction() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean remember = rememberCheckBox.isSelected();
        
        LOG.info("Logging in with username: " + username);
        
        // Save "remember credentials" setting
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setRememberCredentials(remember);
        
        // Try to login
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        boolean success = authManager.login(username, password);
        
        if (success) {
            LOG.info("Login successful");
            super.doOKAction();
        } else {
            LOG.info("Login failed");
            setErrorText("Login failed. Please check your credentials.");
        }
    }
}