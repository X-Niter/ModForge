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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.problems.Problem;
import com.intellij.problems.ProblemListener;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService.CodeIssue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Action to fix errors using AI.
 * This action uses the AI service to fix compilation errors in the current file.
 */
public final class FixErrorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(FixErrorsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("Fix errors action performed");
        
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (project == null || editor == null || psiFile == null) {
            return;
        }
        
        // Get compilation listener
        ModForgeCompilationListener compilationListener = project.getService(ModForgeCompilationListener.class);
        
        if (compilationListener == null) {
            Messages.showErrorDialog(
                    project,
                    "Could not access the compilation listener. Please try restarting the IDE.",
                    "Error"
            );
            return;
        }
        
        // Get issues
        List<ModForgeCompilationListener.CompilationIssue> activeIssues = compilationListener.getActiveIssues();
        
        // Filter issues for this file
        VirtualFile virtualFile = psiFile.getVirtualFile();
        
        if (virtualFile == null) {
            Messages.showErrorDialog(
                    project,
                    "The current file is not a physical file.",
                    "Error"
            );
            return;
        }
        
        String filePath = virtualFile.getPath();
        String basePath = project.getBasePath();
        
        if (basePath != null && filePath.startsWith(basePath)) {
            filePath = filePath.substring(basePath.length());
            
            // Remove leading slash
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
        }
        
        String finalFilePath = filePath;
        List<ModForgeCompilationListener.CompilationIssue> fileIssues = activeIssues.stream()
                .filter(issue -> issue.getFile().equals(finalFilePath))
                .toList();
        
        // Check if there are issues
        if (fileIssues.isEmpty()) {
            // Check for IDE-detected problems
            WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
            
            if (problemSolver.hasProblemFilesBeneath(psiFile)) {
                int result = Messages.showYesNoDialog(
                        project,
                        "There are no compilation errors, but there might be other issues. Would you like to try fixing them?",
                        "Fix Errors",
                        "Yes, Try Fixing",
                        "No, Cancel",
                        Messages.getQuestionIcon()
                );
                
                if (result != Messages.YES) {
                    return;
                }
                
                // Get code and try to fix it
                Document document = editor.getDocument();
                String code = document.getText();
                
                // Get code generation service
                AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
                
                // Show progress dialog and fix code
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Code", false) {
                    private String fixedCode;
                    
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        
                        try {
                            // Fix code
                            CompletableFuture<String> future = service.fixCode(code, null, null);
                            
                            // Wait for result
                            fixedCode = future.get();
                        } catch (Exception ex) {
                            LOG.error("Error fixing code", ex);
                            
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Messages.showErrorDialog(
                                        project,
                                        "An error occurred while fixing code: " + ex.getMessage(),
                                        "Code Fix Error"
                                );
                            });
                        }
                    }
                    
                    @Override
                    public void onSuccess() {
                        if (fixedCode == null || fixedCode.isEmpty() || fixedCode.equals(code)) {
                            Messages.showInfoMessage(
                                    project,
                                    "No improvements could be made.",
                                    "Code Fix"
                            );
                            return;
                        }
                        
                        // Update document
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            document.setText(fixedCode);
                            
                            // Commit document
                            PsiDocumentManager.getInstance(project).commitDocument(document);
                        });
                        
                        Messages.showInfoMessage(
                                project,
                                "Code has been fixed. Please check the changes.",
                                "Code Fix"
                        );
                    }
                });
                
                return;
            }
            
            Messages.showInfoMessage(
                    project,
                    "No errors found in the current file.",
                    "Fix Errors"
            );
            return;
        }
        
        // Convert to code issues
        List<CodeIssue> codeIssues = new ArrayList<>();
        
        for (ModForgeCompilationListener.CompilationIssue issue : fileIssues) {
            codeIssues.add(new CodeIssue(
                    issue.getMessage(),
                    issue.getLine(),
                    issue.getColumn(),
                    issue.getFile(),
                    psiFile.getText()
            ));
        }
        
        // Get code generation service
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        
        // Show progress dialog and fix code
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Errors", false) {
            private String fixedCode;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Build error message
                    StringBuilder errorMessage = new StringBuilder();
                    
                    for (ModForgeCompilationListener.CompilationIssue issue : fileIssues) {
                        errorMessage.append(issue.getMessage()).append(" (Line ").append(issue.getLine()).append(")\n");
                    }
                    
                    // Fix code
                    CompletableFuture<String> future = service.fixCode(psiFile.getText(), errorMessage.toString(), null);
                    
                    // Wait for result
                    fixedCode = future.get();
                } catch (Exception ex) {
                    LOG.error("Error fixing errors", ex);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "An error occurred while fixing errors: " + ex.getMessage(),
                                "Error Fix Error"
                        );
                    });
                }
            }
            
            @Override
            public void onSuccess() {
                if (fixedCode == null || fixedCode.isEmpty() || fixedCode.equals(psiFile.getText())) {
                    Messages.showInfoMessage(
                            project,
                            "No fixes could be made.",
                            "Fix Errors"
                    );
                    return;
                }
                
                // Update document
                Document document = editor.getDocument();
                
                ApplicationManager.getApplication().runWriteAction(() -> {
                    document.setText(fixedCode);
                    
                    // Commit document
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                });
                
                Messages.showInfoMessage(
                        project,
                        "Errors have been fixed. Please check the changes.",
                        "Fix Errors"
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
}