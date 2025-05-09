package com.modforge.intellij.plugin.actions;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeveritiesProvider;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Action to fix errors in code using AI.
 */
public class FixErrorsAction extends AnAction {
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
        
        // Get the current document text
        Document document = editor.getDocument();
        String text = document.getText();
        
        if (text.isEmpty()) {
            Messages.showErrorDialog(project, "No code to fix.", "Empty Document");
            return;
        }
        
        // Collect error messages
        List<String> errorMessages = collectErrorMessages(editor);
        
        if (errorMessages.isEmpty()) {
            Messages.showInfoMessage(project, "No errors found in the current document.", "No Errors");
            return;
        }
        
        // Construct error message
        StringBuilder errorMessage = new StringBuilder();
        for (String error : errorMessages) {
            errorMessage.append(error).append("\n");
        }
        
        // Fix code
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        Map<String, Object> options = new HashMap<>();
        
        CompletableFuture<String> future = service.fixCode(text, errorMessage.toString(), options);
        
        try {
            // Wait for the result with a timeout
            String result = future.get(60, TimeUnit.SECONDS);
            
            if (result != null && !result.isEmpty() && !result.equals(text)) {
                // Replace the document text
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.replaceString(0, document.getTextLength(), result);
                });
                
                Messages.showInfoMessage(project, "Fixed " + errorMessages.size() + " errors in the document.", "Errors Fixed");
            } else {
                Messages.showErrorDialog(project, "Failed to fix errors or no changes were needed.", "Fix Failed");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Messages.showErrorDialog(project, "Error fixing code: " + ex.getMessage(), "Fix Error");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        e.getPresentation().setEnabledAndVisible(project != null && editor != null);
    }
    
    /**
     * Collects error messages from the current editor.
     * @param editor The editor
     * @return The error messages
     */
    private List<String> collectErrorMessages(@NotNull Editor editor) {
        List<String> errorMessages = new ArrayList<>();
        Document document = editor.getDocument();
        
        // For a real implementation, we would use the DaemonCodeAnalyzer to get real-time errors
        // This is a simplified approach that looks for ERROR highlights in the editor
        
        // Get the error highlights
        // Note: In a real implementation, this should be done properly through the inspection system
        // This is a placeholder for the actual implementation
        
        // For now, we'll return a dummy error message
        errorMessages.add("Cannot resolve symbol 'Unknown'");
        
        return errorMessages;
    }
}