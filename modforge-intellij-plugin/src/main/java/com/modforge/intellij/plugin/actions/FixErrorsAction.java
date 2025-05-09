package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Action for fixing errors in code using AI.
 */
public class FixErrorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(FixErrorsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Fix errors action performed");
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Get editor and file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || psiFile == null) {
            Messages.showErrorDialog(
                    project,
                    "Please open a file in the editor to fix errors.",
                    "Fix Errors"
            );
            return;
        }
        
        // Get the selected text or the entire file
        String code;
        String errorMessage = null;
        
        if (editor.getSelectionModel().hasSelection()) {
            // Get selected text
            code = editor.getSelectionModel().getSelectedText();
        } else {
            // Get entire file content
            code = editor.getDocument().getText();
        }
        
        if (code == null || code.isEmpty()) {
            Messages.showErrorDialog(
                    project,
                    "No code to fix.",
                    "Fix Errors"
            );
            return;
        }
        
        // Show error message input dialog
        errorMessage = Messages.showInputDialog(
                project,
                "Enter the error message (if any):",
                "Fix Errors",
                null
        );
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("filePath", psiFile.getVirtualFile().getPath());
        
        // Fix code
        CompletableFuture<String> future = codeGenService.fixCode(code, errorMessage, options);
        
        // Handle result
        future.thenAccept(fixedCode -> {
            // Show confirmation dialog
            int result = Messages.showYesNoDialog(
                    project,
                    "Do you want to apply the fixed code?",
                    "Fix Errors",
                    "Apply",
                    "Cancel",
                    null
            );
            
            if (result == Messages.YES) {
                // Apply the fixed code
                applyFixedCode(project, editor, psiFile, fixedCode);
            } else {
                // Show the fixed code in a dialog
                Messages.showMultilineInputDialog(
                        project,
                        fixedCode,
                        "Fixed Code",
                        null,
                        null,
                        null
                );
            }
        }).exceptionally(ex -> {
            Messages.showErrorDialog(
                    project,
                    "Error fixing code: " + ex.getMessage(),
                    "Fix Errors Error"
            );
            return null;
        });
    }
    
    /**
     * Applies the fixed code to the editor.
     * @param project The project
     * @param editor The editor
     * @param psiFile The PSI file
     * @param fixedCode The fixed code
     */
    private void applyFixedCode(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull String fixedCode) {
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    Document document = editor.getDocument();
                    
                    if (editor.getSelectionModel().hasSelection()) {
                        // Replace selected text
                        int start = editor.getSelectionModel().getSelectionStart();
                        int end = editor.getSelectionModel().getSelectionEnd();
                        document.replaceString(start, end, fixedCode);
                    } else {
                        // Replace entire document
                        document.setText(fixedCode);
                    }
                    
                    // Commit document
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                    
                    LOG.info("Fixed code applied");
                } catch (Exception ex) {
                    LOG.error("Error applying fixed code", ex);
                    Messages.showErrorDialog(
                            project,
                            "Error applying fixed code: " + ex.getMessage(),
                            "Fix Errors Error"
                    );
                }
            });
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Get project and editor
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        // Enable only if project is not null and editor is not null
        e.getPresentation().setEnabled(project != null && editor != null);
    }
}