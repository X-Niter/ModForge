package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Action to explain code using AI.
 */
public class ExplainCodeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        
        // Get selected text
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        
        if (selectedText == null || selectedText.isEmpty()) {
            Messages.showWarningDialog(
                    project,
                    "Please select some code to explain.",
                    "Explain Code"
            );
            return;
        }
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Run in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Explaining Code", false) {
            private String explanation;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Analyzing code...");
                indicator.setFraction(0.2);
                
                try {
                    // Explain code
                    explanation = codeGenService.explainCode(selectedText, null).get();
                    
                    if (explanation == null || explanation.isEmpty()) {
                        Messages.showErrorDialog(
                                project,
                                "Failed to explain code.",
                                "Explain Code"
                        );
                        return;
                    }
                    
                    indicator.setFraction(1.0);
                } catch (Exception ex) {
                    Messages.showErrorDialog(
                            project,
                            "Error explaining code: " + ex.getMessage(),
                            "Explain Code"
                    );
                }
            }
            
            @Override
            public void onSuccess() {
                if (explanation != null && !explanation.isEmpty()) {
                    ExplanationDialog dialog = new ExplanationDialog(project, explanation);
                    dialog.show();
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable only if we have a project, editor, and text selection
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        // Check if text is selected
        SelectionModel selectionModel = editor.getSelectionModel();
        boolean hasSelection = selectionModel.hasSelection();
        
        e.getPresentation().setEnabledAndVisible(hasSelection);
    }
    
    /**
     * Dialog to display code explanation.
     */
    private static class ExplanationDialog extends DialogWrapper {
        private final String explanation;
        
        public ExplanationDialog(Project project, String explanation) {
            super(project);
            this.explanation = explanation;
            init();
            setTitle("Code Explanation");
            setSize(600, 400);
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(600, 400));
            
            JBLabel titleLabel = new JBLabel("Code Explanation");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
            titleLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
            
            JBTextArea textArea = new JBTextArea(explanation);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBorder(JBUI.Borders.empty(5));
            
            JBScrollPane scrollPane = new JBScrollPane(textArea);
            scrollPane.setBorder(JBUI.Borders.empty());
            
            panel.add(titleLabel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.setBorder(JBUI.Borders.empty(10));
            
            return panel;
        }
    }
}