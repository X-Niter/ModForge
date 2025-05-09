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
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Action for generating code using AI.
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Generate code action performed");
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Get editor and file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        // Detect language
        String language = "java";
        if (psiFile != null) {
            String extension = psiFile.getFileType().getDefaultExtension();
            if (extension.equals("kt")) {
                language = "kotlin";
            } else if (extension.equals("js") || extension.equals("jsx") || extension.equals("ts") || extension.equals("tsx")) {
                language = "javascript";
            }
        }
        
        // Show dialog
        String prompt = Messages.showInputDialog(
                project,
                "Enter a description of the code you want to generate:",
                "Generate Code",
                null
        );
        
        if (prompt == null || prompt.isEmpty()) {
            return;
        }
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("language", language);
        
        // Generate code
        CompletableFuture<String> future = codeGenService.generateCode(prompt, language, options);
        
        // Handle result
        future.thenAccept(code -> {
            // Show result dialog
            Messages.showMultilineInputDialog(
                    project,
                    code,
                    "Generated Code",
                    null,
                    null,
                    null
            );
        }).exceptionally(ex -> {
            Messages.showErrorDialog(
                    project,
                    "Error generating code: " + ex.getMessage(),
                    "Generate Code Error"
            );
            return null;
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        
        // Enable only if project is not null
        e.getPresentation().setEnabled(project != null);
    }
}