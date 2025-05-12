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
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.github.GitHubIntegrationService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for pushing a mod to GitHub.
 */
public class PushToGitHubAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PushToGitHubAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Check if user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Authentication Required",
                        "You need to login to ModForge before pushing to GitHub."
                );
            } else {
                CompatibilityUtil.showErrorDialog(
                        project,
                        "Authentication Required",
                        "You need to login to ModForge before pushing to GitHub."
                );
            }
            return;
        }
        
        // Check if GitHub token is available
        String token = authManager.getGitHubToken();
        if (token == null || token.isEmpty()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                int result = notificationService.showYesNoDialog(
                        project,
                        "GitHub Token Required",
                        "No GitHub token found. Do you want to configure it in the settings?",
                        "Open Settings",
                        "Cancel"
                );
                
                if (result == CompatibilityUtil.DIALOG_YES) {
                    // Open settings dialog
                    ModForgeSettings settings = ModForgeSettings.getInstance();
                    settings.openSettings(project);
                }
            } else {
                int result = notificationService.showYesNoDialog(
                        project,
                        "GitHub Token Required",
                        "No GitHub token found. Do you want to configure it in the settings?",
                        "Open Settings",
                        "Cancel"
                );
                
                if (result == CompatibilityUtil.DIALOG_YES) {
                    // Open settings dialog
                    ModForgeSettings settings = ModForgeSettings.getInstance();
                    settings.openSettings(project);
                }
            }
            return;
        }
        
        // Show push dialog
        PushToGitHubDialog dialog = new PushToGitHubDialog(project);
        if (dialog.showAndGet()) {
            String owner = dialog.getRepositoryOwner();
            String repository = dialog.getRepository();
            String description = dialog.getDescription();
            boolean isPrivate = dialog.isPrivate();
            
            // Push to GitHub
            GitHubIntegrationService gitHubService = project.getService(GitHubIntegrationService.class);
            if (gitHubService == null) {
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            project,
                            "Service Unavailable",
                            "GitHub integration service is not available."
                    );
                } else {
                    CompatibilityUtil.showErrorDialog(
                            project,
                            "Service Unavailable",
                            "GitHub integration service is not available."
                    );
                }
                return;
            }
            
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pushing to GitHub", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    AtomicReference<String> progressText = new AtomicReference<>("Initializing...");
                    
                    indicator.setText(progressText.get());
                    
                    gitHubService.pushToGitHub(
                            owner,
                            repository,
                            description,
                            isPrivate,
                            text -> {
                                progressText.set(text);
                                indicator.setText(text);
                            }
                    ).thenAccept(result -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            ModForgeNotificationService notificationService = 
                                    ModForgeNotificationService.getInstance();
                            
                            if (result.isSuccess()) {
                                if (notificationService != null) {
                                    notificationService.showInfoNotification(
                                            "Push Successful",
                                            result.getMessage(),
                                            result.getRepositoryUrl()
                                    );
                                } else {
                                    com.modforge.intellij.plugin.utils.CompatibilityUtil.showInfoDialog(
                                            project,
                                            result.getMessage(),
                                            "Push Successful"
                                    );
                                }
                                
                                // Start monitoring
                                gitHubService.startMonitoring(owner, repository);
                            } else {
                                if (notificationService != null) {
                                    notificationService.showErrorNotification(
                                            project,
                                            "Push Failed",
                                            result.getMessage()
                                    );
                                } else {
                                    CompatibilityUtil.showErrorDialog(
                                            project,
                                            "Push Failed",
                                            result.getMessage()
                                    );
                                }
                            }
                        });
                    });
                }
            });
        }
    }
    
    /**
     * Dialog for pushing to GitHub.
     */
    private static class PushToGitHubDialog extends DialogWrapper {
        private final JBTextField ownerField;
        private final JBTextField repositoryField;
        private final JBTextField descriptionField;
        private final JBCheckBox privateCheckBox;
        
        public PushToGitHubDialog(@Nullable Project project) {
            super(project);
            
            setTitle("Push to GitHub");
            
            ModForgeSettings settings = ModForgeSettings.getInstance();
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            
            // Get owner from settings or user data
            String defaultOwner = settings.getGitHubUsername();
            if (defaultOwner == null || defaultOwner.isEmpty()) {
                defaultOwner = authManager.getUsername();
            }
            
            // Create fields
            ownerField = new JBTextField(defaultOwner);
            repositoryField = new JBTextField();
            descriptionField = new JBTextField("Minecraft mod created with ModForge AI");
            privateCheckBox = new JBCheckBox("Private repository", false);
            
            // Set repository name from project name
            if (project != null) {
                String projectName = project.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
                repositoryField.setText(projectName);
            }
            
            init();
        }
        
        /**
         * Get the owner.
         *
         * @return The owner
         */
        /**
         * Gets the repository owner name.
         * NOTE: This is a custom method for our purposes, not the DialogWrapper.getOwner()
         * which returns Window.
         * 
         * @return The repository owner name
         */
        public String getRepositoryOwner() {
            return ownerField.getText().trim();
        }
        
        @Override
        public Window getOwner() {
            // Default implementation required by DialogWrapper
            return super.getOwner();
        }
        
        /**
         * Get the repository.
         *
         * @return The repository
         */
        public String getRepository() {
            return repositoryField.getText().trim();
        }
        
        /**
         * Get the description.
         *
         * @return The description
         */
        public String getDescription() {
            return descriptionField.getText().trim();
        }
        
        /**
         * Check if the repository is private.
         *
         * @return Whether the repository is private
         */
        public boolean isPrivate() {
            return privateCheckBox.isSelected();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Owner:"), ownerField)
                    .addLabeledComponent(new JBLabel("Repository:"), repositoryField)
                    .addLabeledComponent(new JBLabel("Description:"), descriptionField)
                    .addComponent(privateCheckBox);
            
            panel.add(formBuilder.getPanel(), BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(400, panel.getPreferredSize().height));
            
            return panel;
        }
        
        @Override
        protected ValidationInfo doValidate() {
            if (getRepositoryOwner().isEmpty()) {
                return new ValidationInfo("Owner is required.", ownerField);
            }
            
            if (getRepository().isEmpty()) {
                return new ValidationInfo("Repository is required.", repositoryField);
            }
            
            return null;
        }
    }
}