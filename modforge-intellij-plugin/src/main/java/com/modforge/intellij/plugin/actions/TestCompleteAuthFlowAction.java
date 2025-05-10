package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Test the complete authentication flow from username/password to token authentication.
 * This is a comprehensive test that tries all authentication methods.
 */
public class TestCompleteAuthFlowAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TestCompleteAuthFlowAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get authentication credentials
        AuthTestDialog dialog = new AuthTestDialog(project);
        if (!dialog.showAndGet()) {
            return; // User cancelled
        }
        
        String username = dialog.getUsername();
        String password = dialog.getPassword();
        
        if (username.isEmpty() || password.isEmpty()) {
            showNotification(project, "Authentication Test Failed", 
                    "Username and password are required.", 
                    NotificationType.ERROR);
            return;
        }
        
        // Perform the complete authentication flow test
        String results = AuthTestUtil.testCompleteAuthFlow(username, password);
        
        // Show results in a dialog
        Messages.showInfoMessage(project, results, "Complete Authentication Flow Test Results");
        
        // Log the test completion
        LOG.info("Completed testing complete authentication flow");
        
        // Show notification
        showNotification(project, "Authentication Flow Test Completed", 
                "The complete authentication flow test has been completed. Check the dialog for results.", 
                NotificationType.INFORMATION);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable action if we have a project
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
    
    private void showNotification(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }
    
    /**
     * Dialog for entering username and password.
     */
    private static class AuthTestDialog extends DialogWrapper {
        private final JBTextField usernameField;
        private final JBPasswordField passwordField;
        
        public AuthTestDialog(@Nullable Project project) {
            super(project);
            
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            usernameField = new JBTextField(settings.getUsername());
            passwordField = new JBPasswordField();
            
            if (!settings.getPassword().isEmpty()) {
                passwordField.setText(settings.getPassword());
            }
            
            setTitle("Authentication Flow Test");
            setOKButtonText("Test Authentication Flow");
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(JBUI.Borders.empty(10));
            
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = JBUI.insets(5);
            
            // Introduction
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            panel.add(new JBLabel("This will test the complete authentication flow:"), c);
            
            c.gridy = 1;
            panel.add(new JBLabel("1. Login to get a token"), c);
            
            c.gridy = 2;
            panel.add(new JBLabel("2. Verify the token works on various endpoints"), c);
            
            c.gridy = 3;
            panel.add(new JBLabel("Enter your ModForge credentials:"), c);
            
            // Username
            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = 1;
            panel.add(new JBLabel("Username:"), c);
            
            c.gridx = 1;
            c.weightx = 1.0;
            panel.add(usernameField, c);
            
            // Password
            c.gridx = 0;
            c.gridy = 5;
            c.weightx = 0.0;
            panel.add(new JBLabel("Password:"), c);
            
            c.gridx = 1;
            c.weightx = 1.0;
            panel.add(passwordField, c);
            
            return panel;
        }
        
        public String getUsername() {
            return usernameField.getText().trim();
        }
        
        public String getPassword() {
            return new String(passwordField.getPassword());
        }
    }
}