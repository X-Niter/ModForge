package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Action for logging in to ModForge.
 */
public class LoginAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LoginAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            LOG.warn("Project is null");
            return;
        }
        
        try {
            // Check if already authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (authManager.isAuthenticated()) {
                int option = Messages.showYesNoDialog(
                        project,
                        "You are already logged in. Do you want to log out and log in again?",
                        "Already Logged In",
                        "Logout and Login Again",
                        "Cancel",
                        Messages.getQuestionIcon()
                );
                
                if (option != Messages.YES) {
                    return;
                }
                
                // Logout
                authManager.logout();
            }
            
            // Show login dialog
            LoginDialog dialog = new LoginDialog(project);
            boolean proceed = dialog.showAndGet();
            
            if (!proceed) {
                return;
            }
            
            // Get login data
            String username = dialog.getUsername();
            String password = dialog.getPassword();
            String serverUrl = dialog.getServerUrl();
            
            // Save server URL
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.setServerUrl(serverUrl);
            
            // Test connection to server
            if (!ConnectionTestUtil.testConnection(serverUrl)) {
                Messages.showErrorDialog(
                        project,
                        "Could not connect to server. Please check the server URL and try again.",
                        "Connection Error"
                );
                return;
            }
            
            // Try to login
            boolean success = false;
            String errorMessage = "Unknown error";
            
            try {
                success = authManager.login(username, password);
            } catch (Exception ex) {
                LOG.error("Error during login", ex);
                errorMessage = ex.getMessage();
            }
            
            if (success) {
                Messages.showInfoMessage(
                        project,
                        "Successfully logged in as " + username,
                        "Login Successful"
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "Login failed: " + errorMessage,
                        "Login Failed"
                );
            }
        } catch (Exception ex) {
            LOG.error("Error in login action", ex);
            
            Messages.showErrorDialog(
                    project,
                    "An error occurred: " + ex.getMessage(),
                    "Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Always enable action
        e.getPresentation().setEnabled(true);
    }
    
    /**
     * Dialog for entering login data.
     */
    private static class LoginDialog extends DialogWrapper {
        private final JBTextField usernameField;
        private final JBPasswordField passwordField;
        private final JBTextField serverUrlField;
        
        public LoginDialog(Project project) {
            super(project, true);
            
            usernameField = new JBTextField(20);
            passwordField = new JBPasswordField();
            
            // Set default server URL
            ModForgeSettings settings = ModForgeSettings.getInstance();
            serverUrlField = new JBTextField(settings.getServerUrl(), 30);
            
            setTitle("Login to ModForge");
            init();
        }
        
        @Override
        protected @Nullable ValidationInfo doValidate() {
            if (usernameField.getText().trim().isEmpty()) {
                return new ValidationInfo("Username is required", usernameField);
            }
            
            if (passwordField.getPassword().length == 0) {
                return new ValidationInfo("Password is required", passwordField);
            }
            
            if (serverUrlField.getText().trim().isEmpty()) {
                return new ValidationInfo("Server URL is required", serverUrlField);
            }
            
            return null;
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            // Build form
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Username:"), usernameField)
                    .addLabeledComponent(new JBLabel("Password:"), passwordField)
                    .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField)
                    .addComponentFillVertically(new JPanel(), 0);
            
            panel.add(formBuilder.getPanel(), BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(400, 150));
            
            return JBUI.Panels.simplePanel()
                    .addToCenter(panel);
        }
        
        public String getUsername() {
            return usernameField.getText().trim();
        }
        
        public String getPassword() {
            return new String(passwordField.getPassword());
        }
        
        public String getServerUrl() {
            return serverUrlField.getText().trim();
        }
    }
}