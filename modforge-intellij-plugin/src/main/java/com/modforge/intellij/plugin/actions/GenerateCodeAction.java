package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Action for generating code using AI.
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);
    
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
                        "You must be logged in to generate code.",
                        "Authentication Required"
                );
                return;
            }
            
            // Show dialog to enter prompt
            GenerateCodeDialog dialog = new GenerateCodeDialog(project);
            boolean proceed = dialog.showAndGet();
            
            if (!proceed) {
                return;
            }
            
            String prompt = dialog.getPrompt();
            
            // Try to recognize a pattern first
            PatternRecognitionService patternRecognitionService = project.getService(PatternRecognitionService.class);
            String generatedCode = patternRecognitionService.recognizeCodeGenerationPattern(prompt);
            
            if (generatedCode == null) {
                // No pattern recognized, use AI service
                AutonomousCodeGenerationService codeGenerationService = project.getService(AutonomousCodeGenerationService.class);
                CompletableFuture<String> future = codeGenerationService.generateCode(prompt);
                
                // Show loading dialog
                Messages.showInfoMessage(
                        project,
                        "Generating code...",
                        "Code Generation"
                );
                
                // Process result when available
                future.thenAccept(code -> {
                    if (code != null) {
                        SwingUtilities.invokeLater(() -> {
                            insertCodeIntoEditor(editor, code);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            Messages.showErrorDialog(
                                    project,
                                    "Failed to generate code.",
                                    "Code Generation Error"
                            );
                        });
                    }
                });
            } else {
                // Pattern recognized, insert directly
                insertCodeIntoEditor(editor, generatedCode);
            }
        } catch (Exception ex) {
            LOG.error("Error in generate code action", ex);
            
            Messages.showErrorDialog(
                    project,
                    "An error occurred: " + ex.getMessage(),
                    "Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated and editor is available
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        e.getPresentation().setEnabled(authManager.isAuthenticated() && 
                e.getProject() != null && 
                e.getData(CommonDataKeys.EDITOR) != null);
    }
    
    /**
     * Insert code into the editor.
     *
     * @param editor The editor
     * @param code   The code to insert
     */
    private void insertCodeIntoEditor(Editor editor, String code) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        
        if (selectionModel.hasSelection()) {
            // Replace selection
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            document.replaceString(startOffset, endOffset, code);
        } else {
            // Insert at cursor
            int offset = editor.getCaretModel().getOffset();
            document.insertString(offset, code);
        }
    }
    
    /**
     * Dialog for entering a prompt for code generation.
     */
    private static class GenerateCodeDialog extends DialogWrapper {
        private final JBTextArea promptField;
        
        public GenerateCodeDialog(Project project) {
            super(project, true);
            
            promptField = new JBTextArea(10, 50);
            promptField.setLineWrap(true);
            promptField.setWrapStyleWord(true);
            
            setTitle("Generate Code");
            init();
        }
        
        @Override
        protected @Nullable ValidationInfo doValidate() {
            if (promptField.getText().trim().isEmpty()) {
                return new ValidationInfo("Prompt is required", promptField);
            }
            
            return null;
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            // Build form
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Enter prompt:"), new JBScrollPane(promptField))
                    .addComponentFillVertically(new JPanel(), 0);
            
            panel.add(formBuilder.getPanel(), BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(500, 300));
            
            return JBUI.Panels.simplePanel()
                    .addToCenter(panel);
        }
        
        public String getPrompt() {
            return promptField.getText().trim();
        }
    }
}