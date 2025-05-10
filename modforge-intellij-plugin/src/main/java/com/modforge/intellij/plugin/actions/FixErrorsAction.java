package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * Action for fixing errors in code using AI.
 */
public class FixErrorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(FixErrorsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            LOG.warn("Project or editor is null");
            return;
        }
        
        try {
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to fix errors.",
                        "Authentication Required"
                );
                return;
            }
            
            // Get selected code
            Document document = editor.getDocument();
            SelectionModel selectionModel = editor.getSelectionModel();
            
            if (!selectionModel.hasSelection()) {
                Messages.showInfoMessage(
                        project,
                        "Please select the code with errors to fix.",
                        "No Selection"
                );
                return;
            }
            
            String selectedCode = selectionModel.getSelectedText();
            if (selectedCode == null || selectedCode.isEmpty()) {
                Messages.showInfoMessage(
                        project,
                        "Please select the code with errors to fix.",
                        "No Selection"
                );
                return;
            }
            
            // Try to get error messages from Problems panel
            String errorMessages = getErrorMessagesFromProblemsPanel(project);
            
            if (errorMessages == null || errorMessages.isEmpty()) {
                // Ask user to enter errors manually
                errorMessages = Messages.showInputDialog(
                        project,
                        "Enter error messages to fix:",
                        "Fix Errors",
                        Messages.getQuestionIcon()
                );
                
                if (errorMessages == null || errorMessages.isEmpty()) {
                    return;
                }
            }
            
            // Try to recognize a pattern first
            PatternRecognitionService patternRecognitionService = project.getService(PatternRecognitionService.class);
            String fixedCode = patternRecognitionService.recognizeErrorFixingPattern(selectedCode, errorMessages);
            
            if (fixedCode == null) {
                // No pattern recognized, use AI service
                AutonomousCodeGenerationService codeGenerationService = project.getService(AutonomousCodeGenerationService.class);
                CompletableFuture<String> future = codeGenerationService.fixCode(selectedCode, errorMessages);
                
                // Show loading dialog
                Messages.showInfoMessage(
                        project,
                        "Fixing code...",
                        "Code Fixing"
                );
                
                // Process result when available
                future.thenAccept(code -> {
                    if (code != null) {
                        SwingUtilities.invokeLater(() -> {
                            replaceSelectedCode(editor, code);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            Messages.showErrorDialog(
                                    project,
                                    "Failed to fix code.",
                                    "Code Fixing Error"
                            );
                        });
                    }
                });
            } else {
                // Pattern recognized, replace directly
                replaceSelectedCode(editor, fixedCode);
            }
        } catch (Exception ex) {
            LOG.error("Error in fix errors action", ex);
            
            Messages.showErrorDialog(
                    project,
                    "An error occurred: " + ex.getMessage(),
                    "Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated and editor is available with selection
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        e.getPresentation().setEnabled(authManager.isAuthenticated() && 
                e.getProject() != null && 
                editor != null &&
                editor.getSelectionModel().hasSelection());
    }
    
    /**
     * Replace selected code in the editor.
     *
     * @param editor The editor
     * @param code   The code to replace with
     */
    private void replaceSelectedCode(Editor editor, String code) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        
        if (selectionModel.hasSelection()) {
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            document.replaceString(startOffset, endOffset, code);
        }
    }
    
    /**
     * Try to get error messages from the Problems panel.
     * This is a basic implementation and might need improvement.
     *
     * @param project The project
     * @return The error messages, or null if none found
     */
    @Nullable
    private String getErrorMessagesFromProblemsPanel(Project project) {
        try {
            // This is a simplified approach and might not work in all cases
            // For a more robust solution, you might need to use IntelliJ's inspection framework
            
            // For now, return null and let the user enter errors manually
            return null;
        } catch (Exception e) {
            LOG.error("Error getting error messages from Problems panel", e);
            return null;
        }
    }
}