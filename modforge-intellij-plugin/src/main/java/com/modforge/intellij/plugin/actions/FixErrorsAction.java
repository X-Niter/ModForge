package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService.CodeIssue;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for fixing errors in code using AI.
 */
public class FixErrorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(FixErrorsAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only if a project is open, AI assist is enabled, and a file is selected
        Project project = e.getProject();
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = project != null && 
                ModForgeSettings.getInstance().isEnableAIAssist() && 
                virtualFile != null && !virtualFile.isDirectory();
        
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (editor == null || virtualFile == null) {
            Messages.showInfoMessage(project, "Please open a file in the editor to fix errors.", "No Editor");
            return;
        }

        // Save all documents
        FileDocumentManager.getInstance().saveAllDocuments();
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("fileName", virtualFile.getName());
        
        if (virtualFile.getExtension() != null) {
            options.put("fileExtension", virtualFile.getExtension());
        }
        
        // Get selected text if any
        SelectionModel selectionModel = editor.getSelectionModel();
        final String selectedText = selectionModel.getSelectedText();
        
        // Check if we're fixing a selection or analyzing the whole file
        if (selectedText != null && !selectedText.isEmpty()) {
            // Fix selected code
            fixSelectedCode(project, editor, selectedText, options);
        } else {
            // Analyze and fix the whole file
            analyzeAndFixFile(project, editor, virtualFile);
        }
    }

    /**
     * Fixes selected code.
     * @param project The project
     * @param editor The editor
     * @param selectedText The selected text
     * @param options Additional options
     */
    private void fixSelectedCode(@NotNull Project project, @NotNull Editor editor, @NotNull String selectedText, 
                               @NotNull Map<String, Object> options) {
        // Get error message if available
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        
        // Show progress dialog
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Code...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Fix code
                    AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
                    
                    codeGenService.fixCode(selectedText, errorMessage.get(), options)
                            .thenAccept(fixedCode -> {
                                if (fixedCode.equals(selectedText)) {
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        Messages.showInfoMessage(project, "No issues found in selected code.", "No Changes");
                                    });
                                    return;
                                }
                                
                                // Insert fixed code
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    insertFixedCode(project, editor, fixedCode);
                                });
                            })
                            .exceptionally(ex -> {
                                LOG.error("Error fixing code", ex);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showErrorDialog(project, "Error fixing code: " + ex.getMessage(), "Error");
                                });
                                return null;
                            });
                } catch (Exception ex) {
                    LOG.error("Error fixing code", ex);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error fixing code: " + ex.getMessage(), "Error");
                    });
                }
            }
        });
    }

    /**
     * Analyzes and fixes a file.
     * @param project The project
     * @param editor The editor
     * @param file The file to analyze and fix
     */
    private void analyzeAndFixFile(@NotNull Project project, @NotNull Editor editor, @NotNull VirtualFile file) {
        // Show progress dialog
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Analyzing File...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Analyze file
                    AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
                    
                    codeGenService.analyzeFile(file)
                            .thenAccept(issues -> {
                                if (issues.isEmpty()) {
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        Messages.showInfoMessage(project, "No issues found in file.", "Analysis Complete");
                                    });
                                    return;
                                }
                                
                                // Fix issues
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    int choice = Messages.showYesNoDialog(
                                            project,
                                            "Found " + issues.size() + " issues. Do you want to fix them?",
                                            "Analysis Complete",
                                            "Fix Issues",
                                            "Cancel",
                                            null
                                    );
                                    
                                    if (choice == Messages.YES) {
                                        fixIssues(project, file, issues);
                                    }
                                });
                            })
                            .exceptionally(ex -> {
                                LOG.error("Error analyzing file", ex);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showErrorDialog(project, "Error analyzing file: " + ex.getMessage(), "Error");
                                });
                                return null;
                            });
                } catch (Exception ex) {
                    LOG.error("Error analyzing file", ex);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error analyzing file: " + ex.getMessage(), "Error");
                    });
                }
            }
        });
    }

    /**
     * Fixes issues in a file.
     * @param project The project
     * @param file The file
     * @param issues The issues to fix
     */
    private void fixIssues(@NotNull Project project, @NotNull VirtualFile file, @NotNull List<CodeIssue> issues) {
        // Show progress dialog
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Issues...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);
                
                try {
                    // Fix issues
                    AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
                    
                    codeGenService.fixIssues(file, issues)
                            .thenAccept(fixedCount -> {
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showInfoMessage(
                                            project,
                                            "Fixed " + fixedCount + " issues out of " + issues.size() + ".",
                                            "Fixes Complete"
                                    );
                                    
                                    // Refresh file
                                    file.refresh(false, false);
                                    
                                    // Trigger compilation if Java file
                                    if ("java".equals(file.getExtension())) {
                                        CompilerManager.getInstance(project).compile(new VirtualFile[]{file}, null);
                                    }
                                });
                            })
                            .exceptionally(ex -> {
                                LOG.error("Error fixing issues", ex);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showErrorDialog(project, "Error fixing issues: " + ex.getMessage(), "Error");
                                });
                                return null;
                            });
                } catch (Exception ex) {
                    LOG.error("Error fixing issues", ex);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error fixing issues: " + ex.getMessage(), "Error");
                    });
                }
            }
        });
    }

    /**
     * Inserts fixed code into the editor.
     * @param project The project
     * @param editor The editor
     * @param fixedCode The fixed code
     */
    private void insertFixedCode(@NotNull Project project, @NotNull Editor editor, @NotNull String fixedCode) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        
        // Get insert position
        final int start;
        final int end;
        
        if (selectionModel.hasSelection()) {
            start = selectionModel.getSelectionStart();
            end = selectionModel.getSelectionEnd();
        } else {
            start = end = editor.getCaretModel().getOffset();
        }
        
        // Insert code
        WriteCommandAction.runWriteCommandAction(project, () -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                if (start != end) {
                    document.replaceString(start, end, fixedCode);
                } else {
                    document.insertString(start, fixedCode);
                }
            }, "Insert Fixed Code", null);
        });
    }
}