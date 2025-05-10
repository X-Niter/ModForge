package com.modforge.intellij.plugin.ui.login;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for logging in to ModForge.
 */
public class LoginDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(LoginDialog.class);
    
    private final JBTextField serverUrlField;
    private final JBTextField usernameField;
    private final JBPasswordField passwordField;
    private final JBCheckBox rememberCredentialsCheckBox;
    private final JLabel statusLabel;
    
    private final Project project;
    
    /**
     * Create a login dialog.
     * @param project IntelliJ project (can be null for application-level actions)
     */
    public LoginDialog(@Nullable Project project) {
        super(project, true);
        this.project = project;
        
        // Get saved settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Initialize UI components
        serverUrlField = new JBTextField(settings.getServerUrl(), 30);
        usernameField = new JBTextField(settings.getUsername(), 30);
        passwordField = new JBPasswordField();
        
        // Only set password from settings if remember credentials is enabled
        if (settings.isRememberCredentials() && !settings.getPassword().isEmpty()) {
            passwordField.setText(settings.getPassword());
        }
        
        rememberCredentialsCheckBox = new JBCheckBox("Remember credentials", settings.isRememberCredentials());
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIUtil.getContextHelpForeground());
        
        setTitle("ModForge Login");
        init();
    }
    
    @Override
    protected @Nullable ValidationInfo doValidate() {
        // Validate server URL
        String serverUrl = serverUrlField.getText().trim();
        if (serverUrl.isEmpty()) {
            return new ValidationInfo("Server URL is required", serverUrlField);
        }
        
        // Simple URL validation
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            return new ValidationInfo("Server URL must start with http:// or https://", serverUrlField);
        }
        
        // Validate username
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            return new ValidationInfo("Username is required", usernameField);
        }
        
        // Validate password
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            return new ValidationInfo("Password is required", passwordField);
        }
        
        return null;
    }
    
    @Override
    protected JComponent createCenterPanel() {
        // Create main form
        FormBuilder formBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent("Server URL:", serverUrlField, true)
                .addLabeledComponent("Username:", usernameField, true)
                .addLabeledComponent("Password:", passwordField, true)
                .addComponent(rememberCredentialsCheckBox)
                .addComponentFillVertically(new JPanel(), 5);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(formBuilder.getPanel(), BorderLayout.CENTER);
        
        // Add status label at the bottom
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        panel.setPreferredSize(new Dimension(400, 200));
        return JBUI.Panels.simplePanel().addToCenter(panel);
    }
    
    @Override
    protected void doOKAction() {
        try {
            String serverUrl = serverUrlField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            boolean rememberCredentials = rememberCredentialsCheckBox.isSelected();
            
            // Update status
            statusLabel.setText("Logging in...");
            statusLabel.setForeground(UIUtil.getContextHelpForeground());
            
            // Check server connection
            boolean serverAvailable = ConnectionTestUtil.testConnection(serverUrl);
            
            if (!serverAvailable) {
                statusLabel.setText("Server not available");
                statusLabel.setForeground(new Color(200, 0, 0)); // Red
                return;
            }
            
            // Attempt to log in
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            JSONObject response = authManager.login(serverUrl, username, password);
            
            // Check response
            if (response == null) {
                statusLabel.setText("Login failed");
                statusLabel.setForeground(new Color(200, 0, 0)); // Red
                return;
            }
            
            // Check for success
            if (!authManager.isAuthenticated()) {
                String message = "Authentication failed";
                
                if (response.containsKey("message")) {
                    Object messageObj = response.get("message");
                    if (messageObj != null) {
                        message = messageObj.toString();
                    }
                }
                
                statusLabel.setText(message);
                statusLabel.setForeground(new Color(200, 0, 0)); // Red
                return;
            }
            
            // Save settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.setServerUrl(serverUrl);
            settings.setUsername(username);
            
            // Only save password if remember credentials is enabled
            if (rememberCredentials) {
                settings.setPassword(password);
            } else {
                settings.setPassword("");
            }
            
            settings.setRememberCredentials(rememberCredentials);
            
            // Log success
            LOG.info("Successfully logged in as " + username);
            
            // Close dialog
            super.doOKAction();
        } catch (Exception e) {
            LOG.error("Error during login", e);
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setForeground(new Color(200, 0, 0)); // Red
        }
    }
}