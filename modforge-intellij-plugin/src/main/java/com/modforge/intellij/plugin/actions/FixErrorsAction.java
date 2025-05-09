package com.modforge.intellij.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorNotificationPanel;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService.CodeIssue;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Action for fixing errors in code using AI.
 */
public class FixErrorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(FixErrorsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get editor and file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || psiFile == null) {
            Messages.showErrorDialog(project, "No editor is active", "Fix Errors Error");
            return;
        }
        
        // Check if API key is set
        String apiKey = ModForgeSettings.getInstance().getOpenAiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Messages.showErrorDialog(project, "OpenAI API key is not set. Please configure it in the settings.", 
                    "Fix Errors Error");
            return;
        }
        
        // Check if user wants to proceed
        int result = Messages.showYesNoDialog(project, 
                "ModForge will analyze the code and fix any detected issues. Continue?", 
                "Fix Errors", 
                "Fix Issues", 
                "Cancel", 
                Messages.getQuestionIcon());
        
        if (result != Messages.YES) {
            return; // User cancelled
        }
        
        // Fix errors
        fixErrors(project, psiFile);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        // Enable action only if we have a project, editor, and file
        e.getPresentation().setEnabled(project != null && editor != null && psiFile != null);
    }
    
    /**
     * Fixes errors in a file.
     * @param project The project
     * @param psiFile The PSI file
     */
    private void fixErrors(@NotNull Project project, @NotNull PsiFile psiFile) {
        // Get autonomous code generation service
        AutonomousCodeGenerationService codeGenService = project.getService(AutonomousCodeGenerationService.class);
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Errors", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Analyzing code for issues...");
                indicator.setFraction(0.1);
                
                try {
                    // Analyze file for issues
                    CompletableFuture<List<CodeIssue>> analysisFuture = codeGenService.analyzeFile(
                            psiFile.getVirtualFile());
                    
                    // Wait for analysis to complete
                    List<CodeIssue> issues = analysisFuture.get(30, TimeUnit.SECONDS);
                    
                    indicator.setFraction(0.5);
                    indicator.setText("Found " + issues.size() + " issues. Fixing...");
                    
                    if (issues.isEmpty()) {
                        // No issues found
                        notifyNoIssuesFound(project);
                        return;
                    }
                    
                    // Fix issues
                    CompletableFuture<Integer> fixFuture = codeGenService.fixIssues(
                            psiFile.getVirtualFile(), issues);
                    
                    // Wait for fix to complete
                    int fixedCount = fixFuture.get(30, TimeUnit.SECONDS);
                    
                    indicator.setFraction(1.0);
                    indicator.setText("Fixed " + fixedCount + " issues");
                    
                    // Notify user
                    notifyIssuesFixed(project, fixedCount, issues.size());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    LOG.error("Fix errors interrupted", ex);
                    
                    notifyError(project, "Operation interrupted");
                } catch (ExecutionException | TimeoutException ex) {
                    LOG.error("Error fixing errors", ex);
                    
                    notifyError(project, ex.getMessage());
                }
            }
        });
    }
    
    /**
     * Notifies the user that no issues were found.
     * @param project The project
     */
    private void notifyNoIssuesFound(@NotNull Project project) {
        Notification notification = new Notification(
                "ModForge Notifications",
                "No Issues Found",
                "ModForge did not find any issues to fix in the file.",
                NotificationType.INFORMATION
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Notifies the user that issues were fixed.
     * @param project The project
     * @param fixedCount The number of fixed issues
     * @param totalCount The total number of issues
     */
    private void notifyIssuesFixed(@NotNull Project project, int fixedCount, int totalCount) {
        NotificationType type = fixedCount == totalCount ? 
                NotificationType.INFORMATION : NotificationType.WARNING;
        
        String title = fixedCount > 0 ? "Issues Fixed" : "No Issues Fixed";
        String message = fixedCount > 0 ? 
                "ModForge fixed " + fixedCount + " out of " + totalCount + " issues in the file." :
                "ModForge could not fix any of the " + totalCount + " issues in the file.";
        
        Notification notification = new Notification(
                "ModForge Notifications",
                title,
                message,
                type
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Notifies the user of an error.
     * @param project The project
     * @param errorMessage The error message
     */
    private void notifyError(@NotNull Project project, @NotNull String errorMessage) {
        Notification notification = new Notification(
                "ModForge Notifications",
                "Error Fixing Issues",
                "ModForge encountered an error while fixing issues: " + errorMessage,
                NotificationType.ERROR
        );
        
        Notifications.Bus.notify(notification, project);
    }
}