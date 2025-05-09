package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Action to explain code using AI.
 * This action uses the AI service to generate an explanation for the selected code.
 */
public final class ExplainCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ExplainCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("Explain code action performed");
        
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (project == null || editor == null || psiFile == null) {
            return;
        }
        
        // Get selected text or use the entire file
        String codeToExplain = getCodeToExplain(editor, psiFile);
        
        if (codeToExplain.isEmpty()) {
            Messages.showInfoMessage(
                    project,
                    "No code found to explain.",
                    "Explain Code"
            );
            return;
        }
        
        // Get code generation service
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        
        // Show progress dialog and explain code
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Explaining Code", false) {
            private String explanation;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Explain code
                    CompletableFuture<String> future = service.explainCode(codeToExplain, null);
                    
                    // Wait for result
                    explanation = future.get();
                } catch (Exception ex) {
                    LOG.error("Error explaining code", ex);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "An error occurred while explaining code: " + ex.getMessage(),
                                "Code Explanation Error"
                        );
                    });
                }
            }
            
            @Override
            public void onSuccess() {
                if (explanation == null || explanation.isEmpty()) {
                    Messages.showInfoMessage(
                            project,
                            "No explanation could be generated.",
                            "Explain Code"
                    );
                    return;
                }
                
                // Show explanation in popup
                showExplanationPopup(project, explanation);
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action if project, editor, and file are available
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        e.getPresentation().setEnabledAndVisible(project != null && editor != null && psiFile != null);
    }
    
    /**
     * Gets the code to explain.
     * @param editor The editor
     * @param psiFile The PSI file
     * @return The code to explain
     */
    @NotNull
    private String getCodeToExplain(@NotNull Editor editor, @NotNull PsiFile psiFile) {
        // Check if text is selected
        String selectedText = editor.getSelectionModel().getSelectedText();
        
        if (selectedText != null && !selectedText.isEmpty()) {
            return selectedText;
        }
        
        // Use the entire file
        return psiFile.getText();
    }
    
    /**
     * Shows a popup with the explanation.
     * @param project The project
     * @param explanation The explanation
     */
    private void showExplanationPopup(@NotNull Project project, @NotNull String explanation) {
        // Create UI components
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(800, 600));
        
        JBTextArea explanationArea = new JBTextArea(explanation);
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setBorder(JBUI.Borders.empty(10));
        
        JBScrollPane scrollPane = new JBScrollPane(explanationArea);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            explanationArea.selectAll();
            explanationArea.copy();
            explanationArea.select(0, 0);
        });
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            if (panel.getParent() instanceof JComponent) {
                ((JComponent) panel.getParent()).putClientProperty("JBPopup.lightweightWindow.closing", true);
            }
        });
        
        buttonsPanel.add(copyButton);
        buttonsPanel.add(closeButton);
        
        panel.add(buttonsPanel, BorderLayout.SOUTH);
        
        // Show popup
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, explanationArea)
                .setTitle("Code Explanation")
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .createPopup()
                .showInFocusCenter();
    }
}