package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Action for generating code using AI.
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get editor and file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || psiFile == null) {
            Messages.showErrorDialog(project, "No editor is active", "Generate Code Error");
            return;
        }
        
        // Check if file is Java file
        if (!(psiFile instanceof PsiJavaFile)) {
            Messages.showErrorDialog(project, "This action is only available for Java files", "Generate Code Error");
            return;
        }
        
        // Check if API key is set
        String apiKey = ModForgeSettings.getInstance().getOpenAiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Messages.showErrorDialog(project, "OpenAI API key is not set. Please configure it in the settings.", 
                    "Generate Code Error");
            return;
        }
        
        // Show prompt dialog
        PromptDialog dialog = new PromptDialog(project);
        if (!dialog.showAndGet()) {
            return; // User cancelled
        }
        
        String prompt = dialog.getPrompt();
        if (prompt.isEmpty()) {
            return; // Empty prompt
        }
        
        // Generate code
        generateCode(project, editor, psiFile, prompt);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        // Enable action only if we have a project, editor, and file
        e.getPresentation().setEnabled(project != null && editor != null && psiFile != null);
    }
    
    /**
     * Generates code based on a prompt.
     * @param project The project
     * @param editor The editor
     * @param psiFile The PSI file
     * @param prompt The prompt
     */
    private void generateCode(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile, 
                             @NotNull String prompt) {
        // Get AI service manager
        AIServiceManager aiServiceManager = AIServiceManager.getInstance(project);
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Code", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Generating code based on your prompt...");
                
                try {
                    // Determine programming language
                    String language = "java";
                    
                    // Build context
                    Map<String, Object> options = new HashMap<>();
                    options.put("filename", psiFile.getName());
                    
                    // Add file content as context if needed
                    // options.put("fileContent", psiFile.getText());
                    
                    // Generate code
                    String generatedCode = aiServiceManager.generateCode(prompt, language, options)
                            .get(30, TimeUnit.SECONDS);
                    
                    if (generatedCode == null || generatedCode.trim().isEmpty()) {
                        throw new Exception("Generated code is empty");
                    }
                    
                    // Insert code at cursor position
                    insertCodeAtCursor(project, editor, generatedCode);
                } catch (Exception ex) {
                    LOG.error("Error generating code", ex);
                    
                    // Show error
                    SwingUtilities.invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error generating code: " + ex.getMessage(), 
                                "Generate Code Error");
                    });
                }
            }
        });
    }
    
    /**
     * Inserts code at the cursor position.
     * @param project The project
     * @param editor The editor
     * @param code The code to insert
     */
    private void insertCodeAtCursor(@NotNull Project project, @NotNull Editor editor, @NotNull String code) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        
        // Insert code
        Runnable runnable = () -> document.insertString(offset, code);
        
        SwingUtilities.invokeLater(() -> {
            com.intellij.openapi.command.CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(runnable),
                    "Generate Code",
                    null
            );
            
            // Commit document
            PsiDocumentManager.getInstance(project).commitDocument(document);
            
            // Analyze for potential issues
            VirtualFile file = PsiDocumentManager.getInstance(project).getPsiFile(document).getVirtualFile();
            AutonomousCodeGenerationService codeGenService = project.getService(AutonomousCodeGenerationService.class);
            
            codeGenService.analyzeFile(file)
                    .thenAccept(issues -> {
                        if (!issues.isEmpty()) {
                            // Log issues
                            LOG.info("Found " + issues.size() + " issues in generated code");
                            
                            // (Optionally) Fix issues automatically
                            /*
                            codeGenService.fixIssues(file, issues)
                                    .thenAccept(fixedCount -> {
                                        if (fixedCount > 0) {
                                            LOG.info("Fixed " + fixedCount + " issues in generated code");
                                        }
                                    });
                            */
                        }
                    });
        });
    }
    
    /**
     * Dialog for entering a prompt.
     */
    private static class PromptDialog extends DialogWrapper {
        private final JBTextArea promptArea;
        
        /**
         * Creates a new PromptDialog.
         * @param project The project
         */
        public PromptDialog(@Nullable Project project) {
            super(project);
            setTitle("Generate Code");
            
            promptArea = new JBTextArea(10, 50);
            promptArea.setLineWrap(true);
            promptArea.setWrapStyleWord(true);
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(10));
            
            panel.add(new JBLabel("Enter your code generation prompt:"), BorderLayout.NORTH);
            panel.add(new JBScrollPane(promptArea), BorderLayout.CENTER);
            
            // Add help text
            JBLabel helpLabel = new JBLabel("<html><body>" +
                    "Examples:<br>" +
                    "- \"Create a Java class for a Minecraft block that emits light when right-clicked\"<br>" +
                    "- \"Generate a method to calculate the distance between two 3D vectors\"<br>" +
                    "- \"Create a data model class for a player's inventory with getters and setters\"" +
                    "</body></html>");
            helpLabel.setBorder(JBUI.Borders.emptyTop(10));
            
            panel.add(helpLabel, BorderLayout.SOUTH);
            
            return panel;
        }
        
        /**
         * Gets the prompt.
         * @return The prompt
         */
        public String getPrompt() {
            return promptArea.getText().trim();
        }
    }
}