package com.modforge.intellij.plugin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.CollaborationService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Action for starting or joining a collaboration session.
 */
public class StartCollaborationAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get collaboration service
        CollaborationService collaborationService = CollaborationService.getInstance(project);
        
        // Check if already connected
        if (collaborationService.isConnected()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            int result;
            if (notificationService != null) {
                result = notificationService.showYesNoDialog(
                        project,
                        "Already in Session",
                        "You are already in a collaboration session. Do you want to leave it?",
                        "Yes", 
                        "No"
                );
            } else {
                // Fallback to compatibility wrapper for Messages dialog
                result = CompatibilityUtil.showYesNoDialog(
                        project,
                        "Already in Session",
                        "You are already in a collaboration session. Do you want to leave it?",
                        "Yes",
                        "No",
                        AllIcons.General.QuestionDialog
                );
            }
            
            if (result == CompatibilityUtil.DIALOG_YES) { // Using compatibility constant (value: 0)
                collaborationService.leaveSession().thenAccept(success -> {
                    if (success) {
                        startOrJoinSession(project, collaborationService);
                    } else {
                        ModForgeNotificationService.getInstance().showErrorNotification(
                                project,
                                "Error",
                                "Failed to leave the current session"
                        );
                    }
                });
            }
        } else {
            startOrJoinSession(project, collaborationService);
        }
    }
    
    /**
     * Starts or joins a collaboration session.
     * @param project The project
     * @param collaborationService The collaboration service
     */
    private void startOrJoinSession(@NotNull Project project, @NotNull CollaborationService collaborationService) {
        CollaborationDialog dialog = new CollaborationDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        
        String username = dialog.getUsername();
        boolean startSession = dialog.isStartSession();
        
        if (startSession) {
            // Call the startSession method which now returns a CompletableFuture<String>
            collaborationService.startSession(username)
                .thenAccept(sessionId -> {
                    // Show success message with session ID
                    ModForgeNotificationService.getInstance().showInfoNotification(
                        project,
                        "Session Started",
                        "Session started with ID: " + sessionId + "\n\n" +
                        "Share this ID with your team members so they can join."
                );
                
                // Open collaboration tool window
                showCollaborationToolWindow(project);
            }).exceptionally(ex -> {
                ModForgeNotificationService.getInstance().showErrorNotification(
                        project,
                        "Error",
                        "Failed to start session: " + ex.getMessage()
                );
                return null;
            });
        } else {
            String sessionId = dialog.getSessionId();
            
            CompletableFuture<Boolean> future = collaborationService.joinSession(sessionId, username);
            
            future.thenAccept(success -> {
                if (success) {
                    ModForgeNotificationService.getInstance().showInfoNotification(
                            project,
                            "Session Joined",
                            "Successfully joined session: " + sessionId
                    );
                    
                    // Open collaboration tool window
                    showCollaborationToolWindow(project);
                } else {
                    ModForgeNotificationService.getInstance().showErrorNotification(
                            project,
                            "Error",
                            "Failed to join session: " + sessionId
                    );
                }
            }).exceptionally(ex -> {
                ModForgeNotificationService.getInstance().showErrorNotification(
                        project,
                        "Error",
                        "Failed to join session: " + ex.getMessage()
                );
                return null;
            });
        }
    }
    
    /**
     * Shows the collaboration tool window.
     * @param project The project
     */
    private void showCollaborationToolWindow(@NotNull Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ModForge");
        
        if (toolWindow != null) {
            toolWindow.show(() -> {
                // Select the Collaboration tab
                toolWindow.getContentManager().setSelectedContent(
                        toolWindow.getContentManager().findContent("Collaboration")
                );
            });
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only if a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
    
    /**
     * Dialog for starting or joining a collaboration session.
     */
    private static class CollaborationDialog extends DialogWrapper {
        private final JBTextField usernameField;
        private final JBTextField sessionIdField;
        private final JBRadioButton startSessionRadio;
        private final JBRadioButton joinSessionRadio;
        private final Project project;
        
        protected CollaborationDialog(@Nullable Project project) {
            super(project);
            this.project = project;
            
            // Get the stored username from settings
            String savedUsername = ModForgeSettings.getInstance().getUsername();
            
            // Initialize fields
            usernameField = new JBTextField(savedUsername);
            sessionIdField = new JBTextField();
            sessionIdField.setEnabled(false);
            
            startSessionRadio = new JBRadioButton("Start a new session");
            joinSessionRadio = new JBRadioButton("Join an existing session");
            
            ButtonGroup group = new ButtonGroup();
            group.add(startSessionRadio);
            group.add(joinSessionRadio);
            startSessionRadio.setSelected(true);
            
            // Add listeners
            startSessionRadio.addActionListener(e -> sessionIdField.setEnabled(false));
            joinSessionRadio.addActionListener(e -> sessionIdField.setEnabled(true));
            
            setTitle("Start or Join Collaboration");
            init();
        }
        
        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel radioPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            radioPanel.add(startSessionRadio);
            radioPanel.add(joinSessionRadio);
            
            JPanel panel = FormBuilder.createFormBuilder()
                    .addComponent(new JBLabel("Choose an option:"))
                    .addComponent(radioPanel)
                    .addLabeledComponent("Username:", usernameField)
                    .addLabeledComponent("Session ID:", sessionIdField)
                    .addComponentFillVertically(new JPanel(), 0)
                    .getPanel();
            
            panel.setPreferredSize(new Dimension(400, 150));
            panel.setBorder(JBUI.Borders.empty(10));
            return panel;
        }
        
        @Override
        protected void doOKAction() {
            Project project = getProject();
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            
            if (StringUtil.isEmpty(usernameField.getText())) {
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            null,
                            "Validation Error",
                            "Username cannot be empty"
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            getContentPanel(),
                            "Username cannot be empty",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                return;
            }
            
            if (joinSessionRadio.isSelected() && StringUtil.isEmpty(sessionIdField.getText())) {
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            null,
                            "Validation Error",
                            "Session ID cannot be empty"
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            getContentPanel(),
                            "Session ID cannot be empty",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                return;
            }
            
            // Save username to settings
            ModForgeSettings.getInstance().setUsername(usernameField.getText());
            
            super.doOKAction();
        }
        
        /**
         * Gets the username.
         * @return The username
         */
        public String getUsername() {
            return usernameField.getText();
        }
        
        /**
         * Gets the session ID.
         * @return The session ID
         */
        public String getSessionId() {
            return sessionIdField.getText();
        }
        
        /**
         * Checks if the user wants to start a session.
         * @return Whether the user wants to start a session
         */
        public boolean isStartSession() {
            return startSessionRadio.isSelected();
        }
        
        /**
         * Gets the project for this dialog.
         *
         * @return The project associated with this dialog
         */
        public Project getProject() {
            return project;
        }
    }
}