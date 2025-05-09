package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Action to generate documentation for code using AI.
 * This action uses the AI service to add documentation to the current file.
 */
public final class GenerateDocumentationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateDocumentationAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("Generate documentation action performed");
        
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (project == null || editor == null || psiFile == null) {
            return;
        }
        
        // Get selected text or use the entire file
        String codeToDocument = getCodeToDocument(editor, psiFile);
        
        if (codeToDocument.isEmpty()) {
            Messages.showInfoMessage(
                    project,
                    "No code found to document.",
                    "Generate Documentation"
            );
            return;
        }
        
        // Get code generation service
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        
        // Show progress dialog and generate documentation
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Documentation", false) {
            private String documentedCode;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Generate documentation
                    CompletableFuture<String> future = service.generateDocumentation(codeToDocument, null);
                    
                    // Wait for result
                    documentedCode = future.get();
                } catch (Exception ex) {
                    LOG.error("Error generating documentation", ex);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "An error occurred while generating documentation: " + ex.getMessage(),
                                "Documentation Generation Error"
                        );
                    });
                }
            }
            
            @Override
            public void onSuccess() {
                if (documentedCode == null || documentedCode.isEmpty() || documentedCode.equals(codeToDocument)) {
                    Messages.showInfoMessage(
                            project,
                            "No documentation could be generated or the documentation is already complete.",
                            "Generate Documentation"
                    );
                    return;
                }
                
                // Update document
                updateDocument(editor, psiFile, codeToDocument, documentedCode);
                
                Messages.showInfoMessage(
                        project,
                        "Documentation has been generated. Please check the changes.",
                        "Generate Documentation"
                );
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action if project, editor, and file are available
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        e.getPresentation().setEnabledAndVisible(project != null && editor != null && psiFile != null);
    }
    
    /**
     * Gets the code to document.
     * @param editor The editor
     * @param psiFile The PSI file
     * @return The code to document
     */
    @NotNull
    private String getCodeToDocument(@NotNull Editor editor, @NotNull PsiFile psiFile) {
        // Check if text is selected
        String selectedText = editor.getSelectionModel().getSelectedText();
        
        if (selectedText != null && !selectedText.isEmpty()) {
            return selectedText;
        }
        
        // Use the entire file
        return psiFile.getText();
    }
    
    /**
     * Updates the document with the documented code.
     * @param editor The editor
     * @param psiFile The PSI file
     * @param originalCode The original code
     * @param documentedCode The documented code
     */
    private void updateDocument(@NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull String originalCode, @NotNull String documentedCode) {
        Document document = editor.getDocument();
        
        // Check if selection or entire file
        if (editor.getSelectionModel().hasSelection()) {
            // Replace selection
            int startOffset = editor.getSelectionModel().getSelectionStart();
            int endOffset = editor.getSelectionModel().getSelectionEnd();
            
            ApplicationManager.getApplication().runWriteAction(() -> {
                document.replaceString(startOffset, endOffset, documentedCode);
                
                // Commit document
                PsiDocumentManager.getInstance(psiFile.getProject()).commitDocument(document);
            });
        } else {
            // Replace entire document
            ApplicationManager.getApplication().runWriteAction(() -> {
                document.setText(documentedCode);
                
                // Commit document
                PsiDocumentManager.getInstance(psiFile.getProject()).commitDocument(document);
            });
        }
    }
}