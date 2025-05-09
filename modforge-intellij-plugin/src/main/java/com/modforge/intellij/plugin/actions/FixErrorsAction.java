package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Action to fix errors using AI.
 */
public class FixErrorsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
        
        // Get document
        Document document = editor.getDocument();
        
        // Get compilation listener
        ModForgeCompilationListener compilationListener = ModForgeCompilationListener.getInstance(project);
        
        if (compilationListener == null) {
            Messages.showErrorDialog(
                    project,
                    "Compilation listener not available.",
                    "Fix Errors"
            );
            return;
        }
        
        // Get issues for the current file
        String filePath = psiFile.getVirtualFile().getPath();
        String basePath = project.getBasePath();
        
        if (basePath != null && filePath.startsWith(basePath)) {
            filePath = filePath.substring(basePath.length());
            
            // Remove leading slash
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
        }
        
        List<ModForgeCompilationListener.CompilationIssue> issues = compilationListener.getActiveIssuesForFile(filePath);
        
        if (issues.isEmpty()) {
            Messages.showInfoDialog(
                    project,
                    "No compilation errors found in this file.",
                    "Fix Errors"
            );
            return;
        }
        
        // Build error message
        String errorMessage = issues.stream()
                .map(issue -> issue.getMessage() + " (Line " + issue.getLine() + ")")
                .collect(Collectors.joining("\n"));
        
        // Get code
        String code = document.getText();
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Run in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Errors", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Analyzing errors...");
                indicator.setFraction(0.2);
                
                try {
                    // Fix code
                    String fixedCode = codeGenService.fixCode(code, errorMessage, null).get();
                    
                    if (fixedCode == null || fixedCode.isEmpty() || fixedCode.equals(code)) {
                        Messages.showErrorDialog(
                                project,
                                "Failed to fix errors.",
                                "Fix Errors"
                        );
                        return;
                    }
                    
                    indicator.setText("Applying fixes...");
                    indicator.setFraction(0.8);
                    
                    // Update document
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        document.setText(fixedCode);
                        PsiDocumentManager.getInstance(project).commitDocument(document);
                    });
                    
                    indicator.setFraction(1.0);
                } catch (Exception ex) {
                    Messages.showErrorDialog(
                            project,
                            "Error fixing code: " + ex.getMessage(),
                            "Fix Errors"
                    );
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