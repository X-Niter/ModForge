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
 * Action for generating documentation for code using AI.
 */
public class GenerateDocumentationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateDocumentationAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Generate documentation action performed");
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Get editor and file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || psiFile == null) {
            Messages.showErrorDialog(
                    project,
                    "Please open a file in the editor to generate documentation.",
                    "Generate Documentation"
            );
            return;
        }
        
        // Get the selected text or the entire file
        String code;
        
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
                    "No code to document.",
                    "Generate Documentation"
            );
            return;
        }
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("filePath", psiFile.getVirtualFile().getPath());
        
        // Generate documentation
        CompletableFuture<String> future = codeGenService.generateDocumentation(code, options);
        
        // Handle result
        future.thenAccept(documentedCode -> {
            // Show confirmation dialog
            int result = Messages.showYesNoDialog(
                    project,
                    "Do you want to apply the documented code?",
                    "Generate Documentation",
                    "Apply",
                    "Cancel",
                    null
            );
            
            if (result == Messages.YES) {
                // Apply the documented code
                applyDocumentedCode(project, editor, psiFile, documentedCode);
            } else {
                // Show the documented code in a dialog
                Messages.showMultilineInputDialog(
                        project,
                        documentedCode,
                        "Documented Code",
                        null,
                        null,
                        null
                );
            }
        }).exceptionally(ex -> {
            Messages.showErrorDialog(
                    project,
                    "Error generating documentation: " + ex.getMessage(),
                    "Generate Documentation Error"
            );
            return null;
        });
    }
    
    /**
     * Applies the documented code to the editor.
     * @param project The project
     * @param editor The editor
     * @param psiFile The PSI file
     * @param documentedCode The documented code
     */
    private void applyDocumentedCode(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull String documentedCode) {
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    Document document = editor.getDocument();
                    
                    if (editor.getSelectionModel().hasSelection()) {
                        // Replace selected text
                        int start = editor.getSelectionModel().getSelectionStart();
                        int end = editor.getSelectionModel().getSelectionEnd();
                        document.replaceString(start, end, documentedCode);
                    } else {
                        // Replace entire document
                        document.setText(documentedCode);
                    }
                    
                    // Commit document
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                    
                    LOG.info("Documented code applied");
                } catch (Exception ex) {
                    LOG.error("Error applying documented code", ex);
                    Messages.showErrorDialog(
                            project,
                            "Error applying documented code: " + ex.getMessage(),
                            "Generate Documentation Error"
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