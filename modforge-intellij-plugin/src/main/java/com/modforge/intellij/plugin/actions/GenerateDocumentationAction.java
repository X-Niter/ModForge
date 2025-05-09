package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Action to generate documentation for code using AI.
 */
public class GenerateDocumentationAction extends AnAction {
    private static final String TOOL_WINDOW_ID = "ModForge";
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        // If no editor is open or focused, show the tool window
        if (editor == null) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
            
            if (toolWindow != null) {
                toolWindow.show();
            }
            return;
        }
        
        // Get the selected text or the entire document
        SelectionModel selectionModel = editor.getSelectionModel();
        String text;
        
        if (selectionModel.hasSelection()) {
            text = selectionModel.getSelectedText();
        } else {
            Document document = editor.getDocument();
            text = document.getText();
        }
        
        if (text == null || text.isEmpty()) {
            Messages.showErrorDialog(project, "No code to document.", "Empty Document");
            return;
        }
        
        // Generate documentation
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        CompletableFuture<String> future = service.generateDocumentation(text, null);
        
        try {
            // Wait for the result with a timeout
            String result = future.get(60, TimeUnit.SECONDS);
            
            if (result != null && !result.isEmpty() && !result.equals(text)) {
                // Replace the selection or the entire document
                Document document = editor.getDocument();
                
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    if (selectionModel.hasSelection()) {
                        document.replaceString(
                                selectionModel.getSelectionStart(),
                                selectionModel.getSelectionEnd(),
                                result
                        );
                    } else {
                        document.replaceString(0, document.getTextLength(), result);
                    }
                });
                
                Messages.showInfoMessage(project, "Documentation generated successfully.", "Documentation Generated");
            } else {
                Messages.showErrorDialog(project, "Failed to generate documentation or no changes were needed.", "Generation Failed");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Messages.showErrorDialog(project, "Error generating documentation: " + ex.getMessage(), "Generation Error");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        e.getPresentation().setEnabledAndVisible(project != null && editor != null);
    }
}