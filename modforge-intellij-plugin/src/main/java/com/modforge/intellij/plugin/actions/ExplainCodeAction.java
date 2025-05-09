package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Action for explaining code using AI.
 */
public class ExplainCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ExplainCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Explain code action performed");
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Get editor and file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || psiFile == null) {
            Messages.showErrorDialog(
                    project,
                    "Please open a file in the editor to explain code.",
                    "Explain Code"
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
                    "No code to explain.",
                    "Explain Code"
            );
            return;
        }
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("filePath", psiFile.getVirtualFile().getPath());
        
        // Explain code
        CompletableFuture<String> future = codeGenService.explainCode(code, options);
        
        // Handle result
        future.thenAccept(explanation -> {
            // Show explanation dialog
            Messages.showMultilineInputDialog(
                    project,
                    explanation,
                    "Code Explanation",
                    null,
                    null,
                    null
            );
        }).exceptionally(ex -> {
            Messages.showErrorDialog(
                    project,
                    "Error explaining code: " + ex.getMessage(),
                    "Explain Code Error"
            );
            return null;
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