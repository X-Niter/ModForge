package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Action to explain code using AI.
 */
public class ExplainCodeAction extends AnAction {
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
            Messages.showErrorDialog(project, "No code to explain.", "Empty Document");
            return;
        }
        
        // Explain code
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        CompletableFuture<String> future = service.explainCode(text, null);
        
        try {
            // Wait for the result with a timeout
            String result = future.get(60, TimeUnit.SECONDS);
            
            if (result != null && !result.isEmpty()) {
                // Show the explanation
                ExplanationDialog dialog = new ExplanationDialog(project, text, result);
                dialog.show();
            } else {
                Messages.showErrorDialog(project, "Failed to explain the code.", "Explanation Failed");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Messages.showErrorDialog(project, "Error explaining code: " + ex.getMessage(), "Explanation Error");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        e.getPresentation().setEnabledAndVisible(project != null && editor != null);
    }
    
    /**
     * Dialog to display the code explanation.
     */
    private static class ExplanationDialog extends DialogWrapper {
        private final String code;
        private final String explanation;
        
        /**
         * Creates a new ExplanationDialog.
         * @param project The project
         * @param code The code to explain
         * @param explanation The explanation
         */
        protected ExplanationDialog(@Nullable Project project, @NotNull String code, @NotNull String explanation) {
            super(project);
            this.code = code;
            this.explanation = explanation;
            setTitle("Code Explanation");
            setSize(800, 600);
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(800, 600));
            
            JTabbedPane tabbedPane = new JTabbedPane();
            
            // Code tab
            JPanel codePanel = new JPanel(new BorderLayout());
            codePanel.setBorder(JBUI.Borders.empty(5));
            
            JBTextArea codeTextArea = new JBTextArea();
            codeTextArea.setText(code);
            codeTextArea.setEditable(false);
            
            codePanel.add(new JBScrollPane(codeTextArea), BorderLayout.CENTER);
            
            tabbedPane.addTab("Code", codePanel);
            
            // Explanation tab
            JPanel explanationPanel = new JPanel(new BorderLayout());
            explanationPanel.setBorder(JBUI.Borders.empty(5));
            
            JBTextArea explanationTextArea = new JBTextArea();
            explanationTextArea.setText(explanation);
            explanationTextArea.setEditable(false);
            explanationTextArea.setLineWrap(true);
            explanationTextArea.setWrapStyleWord(true);
            
            explanationPanel.add(new JBScrollPane(explanationTextArea), BorderLayout.CENTER);
            
            tabbedPane.addTab("Explanation", explanationPanel);
            
            panel.add(tabbedPane, BorderLayout.CENTER);
            
            return panel;
        }
        
        @Override
        protected Action @NotNull [] createActions() {
            return new Action[] { getOKAction() };
        }
    }
}