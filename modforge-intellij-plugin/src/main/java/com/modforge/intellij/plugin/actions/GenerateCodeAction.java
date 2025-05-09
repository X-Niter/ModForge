package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Action to generate code using AI.
 */
public class GenerateCodeAction extends AnAction {
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
        
        // Get the selected text
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        
        // Show dialog to get the prompt
        GenerateCodeDialog dialog = new GenerateCodeDialog(project, selectedText);
        if (!dialog.showAndGet()) {
            return;
        }
        
        // Get the prompt from the dialog
        String prompt = dialog.getPrompt();
        String language = dialog.getLanguage();
        
        if (prompt == null || prompt.trim().isEmpty()) {
            Messages.showErrorDialog(project, "Please enter a prompt.", "Empty Prompt");
            return;
        }
        
        // Generate code
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        Map<String, Object> options = new HashMap<>();
        
        CompletableFuture<String> future = service.generateCode(prompt, language, options);
        
        try {
            // Wait for the result with a timeout
            String result = future.get(60, TimeUnit.SECONDS);
            
            if (result != null && !result.isEmpty()) {
                // Replace the selection or insert at caret
                Document document = editor.getDocument();
                
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    if (selectionModel.hasSelection()) {
                        document.replaceString(
                                selectionModel.getSelectionStart(),
                                selectionModel.getSelectionEnd(),
                                result
                        );
                    } else {
                        int offset = editor.getCaretModel().getOffset();
                        document.insertString(offset, result);
                    }
                });
            } else {
                Messages.showErrorDialog(project, "Failed to generate code.", "Generation Failed");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Messages.showErrorDialog(project, "Error generating code: " + ex.getMessage(), "Generation Error");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
    
    /**
     * Dialog to get the code generation prompt.
     */
    private static class GenerateCodeDialog extends DialogWrapper {
        private JBTextArea promptTextArea;
        private JComboBox<String> languageComboBox;
        private final String initialPrompt;
        
        /**
         * Creates a new GenerateCodeDialog.
         * @param project The project
         * @param initialPrompt The initial prompt
         */
        protected GenerateCodeDialog(@Nullable Project project, @Nullable String initialPrompt) {
            super(project);
            this.initialPrompt = initialPrompt;
            setTitle("Generate Code");
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(400, 200));
            
            // Create prompt input
            JPanel promptPanel = new JPanel(new BorderLayout());
            promptPanel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));
            
            promptPanel.add(new JLabel("Enter a prompt to generate code:"), BorderLayout.NORTH);
            
            promptTextArea = new JBTextArea();
            promptTextArea.setLineWrap(true);
            promptTextArea.setWrapStyleWord(true);
            
            if (initialPrompt != null && !initialPrompt.isEmpty()) {
                promptTextArea.setText("Generate code for: " + initialPrompt);
            }
            
            promptPanel.add(new JBScrollPane(promptTextArea), BorderLayout.CENTER);
            
            panel.add(promptPanel, BorderLayout.CENTER);
            
            // Create language selection
            JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            languagePanel.add(new JLabel("Language:"));
            
            languageComboBox = new JComboBox<>(new String[] {
                    "Java",
                    "Kotlin",
                    "Groovy",
                    "XML",
                    "JSON",
                    "YAML"
            });
            
            languagePanel.add(languageComboBox);
            
            panel.add(languagePanel, BorderLayout.SOUTH);
            
            return panel;
        }
        
        /**
         * Gets the prompt from the dialog.
         * @return The prompt
         */
        public String getPrompt() {
            return promptTextArea.getText();
        }
        
        /**
         * Gets the language from the dialog.
         * @return The language
         */
        public String getLanguage() {
            return (String) languageComboBox.getSelectedItem();
        }
    }
}