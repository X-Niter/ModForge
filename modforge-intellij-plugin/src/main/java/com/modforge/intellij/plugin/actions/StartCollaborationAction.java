package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.modforge.intellij.plugin.collaboration.CollaborationDialog;
import com.modforge.intellij.plugin.services.CollaborationService;
import org.jetbrains.annotations.NotNull;

public class StartCollaborationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(StartCollaborationAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);

        try {
            // Check if there are any modified files
            if (hasModifiedFiles(project)) {
                int result = Messages.showYesNoDialog(
                        project,
                        "There are modified files in your workspace. Do you want to save them before starting collaboration?",
                        "Modified Files Warning",
                        "Save Changes",
                        "Cancel",
                        Messages.getQuestionIcon());

                if (result == Messages.YES) {
                    FileDocumentManager.getInstance().saveAllDocuments();
                } else {
                    return; // User canceled
                }
            }

            // Show collaboration dialog
            CollaborationDialog dialog = new CollaborationDialog(project);
            if (dialog.showAndGet()) {
                // Get the collaboration service
                CollaborationService service = project.getService(CollaborationService.class);

                // Handle successful dialog completion
                if (dialog.isStartingNewSession()) {
                    service.startSession(dialog.getUsername())
                            .thenAccept(sessionId -> Messages.showDialog(
                                    project,
                                    "Session started with ID: " + sessionId + "\n\n" +
                                            "Share this ID with your team members so they can join.",
                                    "Session Started",
                                    new String[] { "OK" },
                                    0,
                                    Messages.getInformationIcon()))
                            .exceptionally(ex -> {
                                LOG.error("Failed to start collaboration session", ex);
                                Messages.showErrorDialog(
                                        project,
                                        ex.getMessage(),
                                        "Failed to Start Session");
                                return null;
                            });
                } else {
                    service.joinSession(dialog.getSessionId(), dialog.getUsername())
                            .thenAccept(success -> {
                                if (success) {
                                    Messages.showDialog(
                                            project,
                                            "Successfully joined session.",
                                            "Joined Session",
                                            new String[] { "OK" },
                                            0,
                                            Messages.getInformationIcon());
                                } else {
                                    Messages.showErrorDialog(
                                            project,
                                            "Failed to join session. Please check the session ID and try again.",
                                            "Failed to Join Session");
                                }
                            })
                            .exceptionally(ex -> {
                                LOG.error("Failed to join collaboration session", ex);
                                Messages.showErrorDialog(
                                        project,
                                        ex.getMessage(),
                                        "Failed to Join Session");
                                return null;
                            });
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to handle collaboration action", ex);
            Messages.showErrorDialog(
                    project,
                    ex.getMessage(),
                    "Collaboration Error");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }

    private boolean hasModifiedFiles(@NotNull Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        return !changeListManager.getDefaultChangeList().getChanges().isEmpty();
    }
}