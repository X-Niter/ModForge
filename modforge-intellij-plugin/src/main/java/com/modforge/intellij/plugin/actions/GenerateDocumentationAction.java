package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to generate documentation using AI.
 */
public class GenerateDocumentationAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
        
        // Get document
        Document document = editor.getDocument();
        
        // Get selected text or entire document
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        String code;
        int startOffset;
        int endOffset;
        
        if (selectedText != null && !selectedText.isEmpty()) {
            code = selectedText;
            startOffset = selectionModel.getSelectionStart();
            endOffset = selectionModel.getSelectionEnd();
        } else {
            // Use entire document if no selection
            code = document.getText();
            startOffset = 0;
            endOffset = document.getTextLength();
        }
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Run in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Documentation", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Analyzing code...");
                indicator.setFraction(0.2);
                
                try {
                    // Generate documentation
                    String documentedCode = codeGenService.generateDocumentation(code, null).get();
                    
                    if (documentedCode == null || documentedCode.isEmpty()) {
                        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                        if (notificationService != null) {
                            notificationService.showErrorDialog(
                                    project,
                                    "Generate Documentation",
                                    "Failed to generate documentation."
                            );
                        } else {
                            Messages.showErrorDialog(
                                    project,
                                    "Failed to generate documentation.",
                                    "Generate Documentation"
                            );
                        }
                        return;
                    }
                    
                    indicator.setText("Applying documentation...");
                    indicator.setFraction(0.8);
                    
                    // Update document
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        document.replaceString(startOffset, endOffset, documentedCode);
                        PsiDocumentManager.getInstance(project).commitDocument(document);
                    });
                    
                    indicator.setFraction(1.0);
                } catch (Exception ex) {
                    ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                    if (notificationService != null) {
                        notificationService.showErrorDialog(
                                project,
                                "Generate Documentation",
                                "Error generating documentation: " + ex.getMessage()
                        );
                    } else {
                        Messages.showErrorDialog(
                                project,
                                "Error generating documentation: " + ex.getMessage(),
                                "Generate Documentation"
                        );
                    }
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable only if we have a project, editor, and file
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        e.getPresentation().setEnabledAndVisible(
                project != null && editor != null && psiFile != null
        );
    }
}