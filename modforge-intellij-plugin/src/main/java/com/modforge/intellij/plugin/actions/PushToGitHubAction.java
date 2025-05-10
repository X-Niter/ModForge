package com.modforge.intellij.plugin.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.github.GitHubIntegrationService;
import com.modforge.intellij.plugin.notifications.ModForgeNotificationService;
import com.modforge.intellij.plugin.utils.ModLoaderDetector;
import com.modforge.intellij.plugin.utils.error.ErrorHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for pushing a mod to GitHub.
 */
public class PushToGitHubAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PushToGitHubAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Presentation presentation = e.getPresentation();
        
        // Disable action if there's no project
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            presentation.setEnabled(false);
            return;
        }
        
        // Enable action if we have a project and the user is authenticated
        presentation.setEnabled(true);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            Messages.showErrorDialog(
                    project,
                    "You must be logged in to push to GitHub.",
                    "Authentication Required"
            );
            return;
        }
        
        // Check if the project is linked to a GitHub repository
        GitHubIntegrationService githubService = project.getService(GitHubIntegrationService.class);
        if (githubService == null) {
            Messages.showErrorDialog(
                    project,
                    "GitHub integration service is not available.",
                    "Service Unavailable"
            );
            return;
        }
        
        if (githubService.isLinked()) {
            // Project is already linked to a repository, show push dialog
            showPushDialog(project, githubService);
        } else {
            // Project is not linked yet, show repository creation dialog
            showCreateRepositoryDialog(project, githubService);
        }
    }
    
    /**
     * Show a dialog for creating a new GitHub repository.
     *
     * @param project       The project
     * @param githubService The GitHub integration service
     */
    private void showCreateRepositoryDialog(@NotNull Project project, @NotNull GitHubIntegrationService githubService) {
        CreateRepositoryDialog dialog = new CreateRepositoryDialog(project);
        if (dialog.showAndGet()) {
            String repoName = dialog.getRepositoryName();
            String description = dialog.getDescription();
            boolean isPrivate = dialog.isPrivate();
            
            // Create repository
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating GitHub Repository") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    
                    // Get notification service
                    ModForgeNotificationService notificationService =
                            project.getService(ModForgeNotificationService.class);
                    
                    try {
                        String repoUrl = githubService.createRepository(repoName, description, isPrivate)
                                .exceptionally(e -> {
                                    LOG.error("Error creating GitHub repository", e);
                                    
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        if (notificationService != null) {
                                            notificationService.showErrorNotification(
                                                    "Error Creating Repository",
                                                    "Failed to create GitHub repository: " + e.getMessage()
                                            );
                                        } else {
                                            Messages.showErrorDialog(
                                                    project,
                                                    "Failed to create GitHub repository: " + e.getMessage(),
                                                    "Error Creating Repository"
                                            );
                                        }
                                    });
                                    
                                    return null;
                                })
                                .join();
                        
                        if (repoUrl != null) {
                            // Repository created successfully, now push files
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (notificationService != null) {
                                    notificationService.showInfoNotification(
                                            "Repository Created",
                                            "GitHub repository created: " + repoUrl
                                    );
                                } else {
                                    Messages.showInfoMessage(
                                            project,
                                            "GitHub repository created: " + repoUrl,
                                            "Repository Created"
                                    );
                                }
                                
                                // Show push dialog
                                showPushDialog(project, githubService);
                            });
                        }
                    } catch (Exception ex) {
                        LOG.error("Error creating GitHub repository", ex);
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (notificationService != null) {
                                notificationService.showErrorNotification(
                                        "Error Creating Repository",
                                        "Failed to create GitHub repository: " + ex.getMessage()
                                );
                            } else {
                                Messages.showErrorDialog(
                                        project,
                                        "Failed to create GitHub repository: " + ex.getMessage(),
                                        "Error Creating Repository"
                                );
                            }
                        });
                    }
                }
            });
        }
    }
    
    /**
     * Show a dialog for pushing files to GitHub.
     *
     * @param project       The project
     * @param githubService The GitHub integration service
     */
    private void showPushDialog(@NotNull Project project, @NotNull GitHubIntegrationService githubService) {
        PushDialog dialog = new PushDialog(project, githubService);
        if (dialog.showAndGet()) {
            String commitMessage = dialog.getCommitMessage();
            String branch = dialog.getBranch();
            
            // Push files
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pushing to GitHub") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    
                    // Get notification service
                    ModForgeNotificationService notificationService =
                            project.getService(ModForgeNotificationService.class);
                    
                    try {
                        // Collect files to push
                        Map<String, String> files = collectFilesToPush(project, indicator);
                        
                        if (files.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (notificationService != null) {
                                    notificationService.showWarningNotification(
                                            "No Files to Push",
                                            "No files were found to push to GitHub."
                                    );
                                } else {
                                    Messages.showWarningDialog(
                                            project,
                                            "No files were found to push to GitHub.",
                                            "No Files to Push"
                                    );
                                }
                            });
                            return;
                        }
                        
                        // Push files to GitHub
                        boolean success = githubService.pushFiles(files, commitMessage, branch)
                                .exceptionally(e -> {
                                    LOG.error("Error pushing files to GitHub", e);
                                    
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        if (notificationService != null) {
                                            notificationService.showErrorNotification(
                                                    "Error Pushing Files",
                                                    "Failed to push files to GitHub: " + e.getMessage()
                                            );
                                        } else {
                                            Messages.showErrorDialog(
                                                    project,
                                                    "Failed to push files to GitHub: " + e.getMessage(),
                                                    "Error Pushing Files"
                                            );
                                        }
                                    });
                                    
                                    return false;
                                })
                                .join();
                        
                        if (success) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (notificationService != null) {
                                    notificationService.showInfoNotification(
                                            "Push Successful",
                                            "Files pushed to GitHub successfully."
                                    );
                                } else {
                                    Messages.showInfoMessage(
                                            project,
                                            "Files pushed to GitHub successfully.",
                                            "Push Successful"
                                    );
                                }
                            });
                        }
                    } catch (Exception e) {
                        LOG.error("Error pushing files to GitHub", e);
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (notificationService != null) {
                                notificationService.showErrorNotification(
                                        "Error Pushing Files",
                                        "Failed to push files to GitHub: " + e.getMessage()
                                );
                            } else {
                                Messages.showErrorDialog(
                                        project,
                                        "Failed to push files to GitHub: " + e.getMessage(),
                                        "Error Pushing Files"
                                );
                            }
                        });
                    }
                }
            });
        }
    }
    
    /**
     * Collect files to push.
     *
     * @param project    The project
     * @param indicator  The progress indicator
     * @return A map of file paths to file contents
     */
    @NotNull
    private Map<String, String> collectFilesToPush(@NotNull Project project, @NotNull ProgressIndicator indicator) {
        Map<String, String> files = new HashMap<>();
        
        try {
            // Get project base directory
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) {
                return files;
            }
            
            // Detect mod loader
            ModLoaderDetector.ModLoader modLoader = ModLoaderDetector.detectModLoader(project);
            LOG.info("Detected mod loader: " + modLoader);
            
            // Process files
            processDirectory(baseDir, "", files, indicator);
            
            return files;
        } catch (Exception e) {
            LOG.error("Error collecting files to push", e);
            return files;
        }
    }
    
    /**
     * Process a directory recursively to collect files.
     *
     * @param dir       The directory
     * @param path      The path
     * @param files     The files map
     * @param indicator The progress indicator
     */
    private void processDirectory(
            @NotNull VirtualFile dir,
            @NotNull String path,
            @NotNull Map<String, String> files,
            @NotNull ProgressIndicator indicator
    ) {
        // Check if canceled
        if (indicator.isCanceled()) {
            return;
        }
        
        // Skip .git directory
        if (dir.getName().equals(".git")) {
            return;
        }
        
        // Skip build and out directories
        if (dir.getName().equals("build") || dir.getName().equals("out")) {
            return;
        }
        
        // Process children
        for (VirtualFile child : dir.getChildren()) {
            String childPath = path.isEmpty() ? child.getName() : path + "/" + child.getName();
            
            // Update progress
            indicator.setText("Processing " + childPath);
            
            if (child.isDirectory()) {
                // Process subdirectory
                processDirectory(child, childPath, files, indicator);
            } else {
                // Process file
                try {
                    // Skip binary files
                    if (isBinaryFile(child)) {
                        continue;
                    }
                    
                    String content = VfsUtil.loadText(child);
                    files.put(childPath, content);
                } catch (Exception e) {
                    LOG.error("Error reading file: " + childPath, e);
                }
            }
        }
    }
    
    /**
     * Check if a file is binary.
     *
     * @param file The file
     * @return True if the file is binary, false otherwise
     */
    private boolean isBinaryFile(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        
        // Common binary file extensions
        return extension.equalsIgnoreCase("class") ||
                extension.equalsIgnoreCase("jar") ||
                extension.equalsIgnoreCase("zip") ||
                extension.equalsIgnoreCase("exe") ||
                extension.equalsIgnoreCase("dll") ||
                extension.equalsIgnoreCase("so") ||
                extension.equalsIgnoreCase("dylib") ||
                extension.equalsIgnoreCase("png") ||
                extension.equalsIgnoreCase("jpg") ||
                extension.equalsIgnoreCase("jpeg") ||
                extension.equalsIgnoreCase("gif") ||
                extension.equalsIgnoreCase("bmp");
    }
    
    /**
     * Dialog for creating a new GitHub repository.
     */
    private static class CreateRepositoryDialog extends DialogWrapper {
        private final JBTextField repoNameField;
        private final JBTextField descriptionField;
        private final JCheckBox privateCheckBox;
        
        public CreateRepositoryDialog(@Nullable Project project) {
            super(project);
            
            setTitle("Create GitHub Repository");
            setOKButtonText("Create");
            setCancelButtonText("Cancel");
            
            // Repository name field
            repoNameField = new JBTextField();
            repoNameField.setEmptyText("Repository name");
            
            // Description field
            descriptionField = new JBTextField();
            descriptionField.setEmptyText("Repository description");
            
            // Private checkbox
            privateCheckBox = new JCheckBox("Private repository");
            privateCheckBox.setSelected(true);
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create a panel with labels and fields
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Repository name:"), repoNameField, 1, false)
                    .addLabeledComponent(new JBLabel("Description:"), descriptionField, 1, false)
                    .addComponent(privateCheckBox)
                    .addComponentFillVertically(new JPanel(), 0);
            
            // Add a hint
            JBLabel hintLabel = new JBLabel("Repository will be created on GitHub with the specified name and description.");
            hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
            hintLabel.setFont(JBUI.Fonts.smallFont());
            formBuilder.addComponent(hintLabel);
            
            JPanel panel = formBuilder.getPanel();
            panel.setPreferredSize(new Dimension(400, panel.getPreferredSize().height));
            
            return panel;
        }
        
        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return repoNameField;
        }
        
        /**
         * Get the repository name.
         *
         * @return The repository name
         */
        public String getRepositoryName() {
            return repoNameField.getText().trim();
        }
        
        /**
         * Get the repository description.
         *
         * @return The repository description
         */
        public String getDescription() {
            return descriptionField.getText().trim();
        }
        
        /**
         * Check if the repository should be private.
         *
         * @return True if private, false otherwise
         */
        public boolean isPrivate() {
            return privateCheckBox.isSelected();
        }
        
        @Override
        protected void doOKAction() {
            if (getRepositoryName().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Repository name cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            super.doOKAction();
        }
    }
    
    /**
     * Dialog for pushing files to GitHub.
     */
    private static class PushDialog extends DialogWrapper {
        private final JBTextField commitMessageField;
        private final JBTextField branchField;
        private final JCheckBox createPullRequestCheckBox;
        private final JBTextField prTitleField;
        private final JBTextField prDescriptionField;
        
        private final GitHubIntegrationService githubService;
        
        public PushDialog(@Nullable Project project, @NotNull GitHubIntegrationService githubService) {
            super(project);
            
            this.githubService = githubService;
            
            setTitle("Push to GitHub");
            setOKButtonText("Push");
            setCancelButtonText("Cancel");
            
            // Commit message field
            commitMessageField = new JBTextField();
            commitMessageField.setEmptyText("Commit message");
            commitMessageField.setText("Update mod files");
            
            // Branch field
            branchField = new JBTextField();
            branchField.setEmptyText("Branch name");
            branchField.setText("main");
            
            // Pull request checkbox
            createPullRequestCheckBox = new JCheckBox("Create pull request");
            createPullRequestCheckBox.setSelected(false);
            
            // PR title field
            prTitleField = new JBTextField();
            prTitleField.setEmptyText("Pull request title");
            prTitleField.setEnabled(false);
            
            // PR description field
            prDescriptionField = new JBTextField();
            prDescriptionField.setEmptyText("Pull request description");
            prDescriptionField.setEnabled(false);
            
            // Enable/disable PR fields based on checkbox
            createPullRequestCheckBox.addActionListener(e -> {
                boolean selected = createPullRequestCheckBox.isSelected();
                prTitleField.setEnabled(selected);
                prDescriptionField.setEnabled(selected);
            });
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Show repository info
            String repoOwner = githubService.getRepoOwner();
            String repoName = githubService.getRepoName();
            String repoInfo = (repoOwner != null && repoName != null) ?
                    repoOwner + "/" + repoName : "Unknown";
            
            JBLabel repoLabel = new JBLabel("Repository: " + repoInfo);
            repoLabel.setFont(JBUI.Fonts.label().biggerOn(1));
            
            // Create a panel with labels and fields
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addComponent(repoLabel)
                    .addLabeledComponent(new JBLabel("Commit message:"), commitMessageField, 1, false)
                    .addLabeledComponent(new JBLabel("Branch:"), branchField, 1, false)
                    .addSeparator()
                    .addComponent(createPullRequestCheckBox)
                    .addLabeledComponent(new JBLabel("PR title:"), prTitleField, 1, false)
                    .addLabeledComponent(new JBLabel("PR description:"), prDescriptionField, 1, false)
                    .addComponentFillVertically(new JPanel(), 0);
            
            // Add a hint
            JBLabel hintLabel = new JBLabel("Files from the project will be pushed to the specified branch.");
            hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
            hintLabel.setFont(JBUI.Fonts.smallFont());
            formBuilder.addComponent(hintLabel);
            
            JPanel panel = formBuilder.getPanel();
            panel.setPreferredSize(new Dimension(500, panel.getPreferredSize().height));
            
            return panel;
        }
        
        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return commitMessageField;
        }
        
        /**
         * Get the commit message.
         *
         * @return The commit message
         */
        public String getCommitMessage() {
            return commitMessageField.getText().trim();
        }
        
        /**
         * Get the branch name.
         *
         * @return The branch name
         */
        public String getBranch() {
            return branchField.getText().trim();
        }
        
        /**
         * Check if a pull request should be created.
         *
         * @return True if a pull request should be created, false otherwise
         */
        public boolean isCreatePullRequest() {
            return createPullRequestCheckBox.isSelected();
        }
        
        /**
         * Get the pull request title.
         *
         * @return The pull request title
         */
        public String getPrTitle() {
            return prTitleField.getText().trim();
        }
        
        /**
         * Get the pull request description.
         *
         * @return The pull request description
         */
        public String getPrDescription() {
            return prDescriptionField.getText().trim();
        }
        
        @Override
        protected void doOKAction() {
            if (getCommitMessage().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Commit message cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            if (getBranch().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Branch name cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            if (isCreatePullRequest()) {
                if (getPrTitle().isEmpty()) {
                    Messages.showErrorDialog(
                            getContentPanel(),
                            "Pull request title cannot be empty",
                            "Validation Error"
                    );
                    return;
                }
            }
            
            super.doOKAction();
        }
    }
}