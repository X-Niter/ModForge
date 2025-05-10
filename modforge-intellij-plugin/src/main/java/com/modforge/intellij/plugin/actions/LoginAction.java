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
                Messages.showInfoMessage(
                        project,
                        "You are already logged in as " + authManager.getUsername(),
                        "Already Authenticated"
                );
                return;
            }
            
            // Show login dialog
            LoginDialog dialog = new LoginDialog(project);
            boolean proceed = dialog.showAndGet();
            
            if (!proceed) {
                return;
            }
            
            String username = dialog.getUsername();
            String password = dialog.getPassword();
            boolean rememberMe = dialog.isRememberMe();
            
            // Save remember me setting
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.setRememberMe(rememberMe);
            
            // Perform login
            authManager.login(username, password)
                    .thenAccept(success -> {
                        if (success) {
                            SwingUtilities.invokeLater(() -> {
                                Messages.showInfoMessage(
                                        project,
                                        "Successfully logged in as " + username,
                                        "Login Successful"
                                );
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                Messages.showErrorDialog(
                                        project,
                                        "Failed to log in. Please check your credentials.",
                                        "Login Failed"
                                );
                            });
                        }
                    });
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
        // Only enable if not authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        e.getPresentation().setEnabled(!authManager.isAuthenticated());
    }
    
    /**
     * Dialog for entering login credentials.
     */
    private static class LoginDialog extends DialogWrapper {
        private final JBTextField usernameField;
        private final JBPasswordField passwordField;
        private final JCheckBox rememberMeCheckbox;
        
        public LoginDialog(Project project) {
            super(project, true);
            
            // Get last username from settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String lastUsername = settings.getLastUsername();
            boolean rememberMe = settings.isRememberMe();
            
            usernameField = new JBTextField(lastUsername, 20);
            passwordField = new JBPasswordField();
            rememberMeCheckbox = new JCheckBox("Remember me", rememberMe);
            
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
            
            return null;
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            // Build form
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Username:"), usernameField)
                    .addLabeledComponent(new JBLabel("Password:"), passwordField)
                    .addComponent(rememberMeCheckbox);
            
            panel.add(formBuilder.getPanel(), BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(300, 150));
            
            return JBUI.Panels.simplePanel()
                    .addToCenter(panel);
        }
        
        public String getUsername() {
            return usernameField.getText().trim();
        }
        
        public String getPassword() {
            return new String(passwordField.getPassword());
        }
        
        public boolean isRememberMe() {
            return rememberMeCheckbox.isSelected();
        }
    }
}