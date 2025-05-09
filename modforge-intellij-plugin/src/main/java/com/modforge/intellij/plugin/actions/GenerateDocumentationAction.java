package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for generating documentation using AI.
 */
public class GenerateDocumentationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateDocumentationAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only if a project is open, AI assist is enabled, and a file is selected
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean enabled = project != null && 
                ModForgeSettings.getInstance().isEnableAIAssist() && 
                editor != null;
        
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

        if (editor == null) {
            Messages.showInfoMessage(project, "Please open a file in the editor to generate documentation.", "No Editor");
            return;
        }

        // Check if text is selected
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        
        if (selectedText == null || selectedText.isEmpty()) {
            Messages.showInfoMessage(project, "Please select code to document.", "No Selection");
            return;
        }

        // Create options
        Map<String, Object> options = new HashMap<>();
        
        if (virtualFile != null) {
            options.put("fileName", virtualFile.getName());
            options.put("fileExtension", virtualFile.getExtension());
        }

        // Show progress dialog
        AtomicReference<String> documentedCode = new AtomicReference<>("");
        
        ProgressManager.getInstance().run(
                new Task.Backgroundable(project, "Generating Documentation...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        
                        try {
                            // Generate documentation
                            AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
                            
                            codeGenService.generateDocumentation(selectedText, options)
                                    .thenAccept(code -> {
                                        documentedCode.set(code);
                                        
                                        // Insert documented code
                                        ApplicationManager.getApplication().invokeLater(() -> {
                                            insertDocumentedCode(project, editor, code);
                                        });
                                    })
                                    .exceptionally(ex -> {
                                        LOG.error("Error generating documentation", ex);
                                        ApplicationManager.getApplication().invokeLater(() -> {
                                            Messages.showErrorDialog(
                                                    project,
                                                    "Error generating documentation: " + ex.getMessage(),
                                                    "Error"
                                            );
                                        });
                                        return null;
                                    }).join(); // Wait for completion
                        } catch (Exception ex) {
                            LOG.error("Error generating documentation", ex);
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Messages.showErrorDialog(
                                        project,
                                        "Error generating documentation: " + ex.getMessage(),
                                        "Error"
                                );
                            });
                        }
                    }
                }
        );
    }

    /**
     * Inserts documented code into the editor.
     * @param project The project
     * @param editor The editor
     * @param documentedCode The documented code
     */
    private void insertDocumentedCode(@NotNull Project project, @NotNull Editor editor, @NotNull String documentedCode) {
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
                document.replaceString(start, end, documentedCode);
            }, "Insert Generated Documentation", null);
        });
    }
}