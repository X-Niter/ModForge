package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Action for logging in to ModForge.
 */
public class LoginAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LoginAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Enable action only if the user is not authenticated
        e.getPresentation().setEnabled(!authManager.isAuthenticated());
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // Check if server URL is set
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        if (serverUrl.isEmpty()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Login Error",
                        "Please set the ModForge server URL in the settings."
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "Please set the ModForge server URL in the settings.",
                        "Login Error"
                );
            }
            return;
        }
        
        // Show login dialog
        LoginDialog dialog = new LoginDialog(project);
        if (dialog.showAndGet()) {
            String username = dialog.getUsername();
            String password = dialog.getPassword();
            
            // Show progress while logging in
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Logging in to ModForge") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    
                    ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                    AtomicBoolean success = new AtomicBoolean(false);
                    
                    authManager.login(username, password)
                            .thenAccept(success::set)
                            .join(); // Wait for completion
                    
                    // Show result on UI thread
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (success.get()) {
                            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                            if (notificationService != null) {
                                notificationService.showInfoDialog(
                                        project,
                                        "Login Successful",
                                        "Successfully logged in as " + authManager.getUsername()
                                );
                            } else {
                                Messages.showInfoMessage(
                                        project,
                                        "Successfully logged in as " + authManager.getUsername(),
                                        "Login Successful"
                                );
                            }
                            
                            // Enable features that require authentication
                            enableContinuousDevelopmentIfPossible(project);
                        } else {
                            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                            if (notificationService != null) {
                                notificationService.showErrorDialog(
                                        project,
                                        "Login Failed",
                                        "Login failed. Please check your credentials and try again."
                                );
                            } else {
                                Messages.showErrorDialog(
                                        project,
                                        "Login failed. Please check your credentials and try again.",
                                        "Login Failed"
                                );
                            }
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Enable continuous development if it's enabled in settings.
     *
     * @param project The project
     */
    private void enableContinuousDevelopmentIfPossible(Project project) {
        if (project == null) {
            return;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.isEnableContinuousDevelopment()) {
            // We could automatically start continuous development here
            // but for now, we'll just let the user know it's available
            
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showInfoDialog(
                        project,
                        "Continuous Development",
                        "Continuous development is enabled in settings. You can start it from the ModForge menu."
                );
            } else {
                Messages.showInfoMessage(
                        project,
                        "Continuous development is enabled in settings. You can start it from the ModForge menu.",
                        "Continuous Development"
                );
            }
        }
    }
    
    /**
     * Dialog for logging in to ModForge.
     */
    private static class LoginDialog extends DialogWrapper {
        private final JBTextField usernameField;
        private final JBPasswordField passwordField;
        
        public LoginDialog(@Nullable Project project) {
            super(project);
            setTitle("Login to ModForge");
            setCancelButtonText("Cancel");
            setOKButtonText("Login");
            
            usernameField = new JBTextField();
            passwordField = new JBPasswordField();
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create a panel with labels and fields
            JPanel panel = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Username:"), usernameField, 1, false)
                    .addLabeledComponent(new JBLabel("Password:"), passwordField, 1, false)
                    .addComponentFillVertically(new JPanel(), 0)
                    .getPanel();
            
            panel.setPreferredSize(new Dimension(300, panel.getPreferredSize().height));
            
            return panel;
        }
        
        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return usernameField;
        }
        
        /**
         * Get the entered username.
         *
         * @return The username
         */
        public String getUsername() {
            return usernameField.getText().trim();
        }
        
        /**
         * Get the entered password.
         *
         * @return The password
         */
        public String getPassword() {
            return new String(passwordField.getPassword());
        }
        
        @Override
        protected void doOKAction() {
            Project project = getProject();
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            
            if (getUsername().isEmpty()) {
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            null,  // Use null for JComponent parent
                            "Validation Error",
                            "Username cannot be empty"
                    );
                } else {
                    Messages.showErrorDialog(
                            getContentPanel(),
                            "Username cannot be empty",
                            "Validation Error"
                    );
                }
                return;
            }
            
            if (getPassword().isEmpty()) {
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            null,  // Use null for JComponent parent
                            "Validation Error",
                            "Password cannot be empty"
                    );
                } else {
                    Messages.showErrorDialog(
                            getContentPanel(),
                            "Password cannot be empty",
                            "Validation Error"
                    );
                }
                return;
            }
            
            super.doOKAction();
        }
    }
}