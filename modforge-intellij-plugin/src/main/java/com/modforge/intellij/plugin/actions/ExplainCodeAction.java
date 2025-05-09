package com.modforge.intellij.plugin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for explaining code using AI.
 */
public class ExplainCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ExplainCodeAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only if a project is open, AI assist is enabled, and text is selected
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();
        boolean enabled = project != null && 
                ModForgeSettings.getInstance().isEnableAIAssist() && 
                hasSelection;
        
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (editor == null) {
            Messages.showInfoMessage(project, "Please open a file in the editor to explain code.", "No Editor");
            return;
        }

        // Get selected text
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        
        if (selectedText == null || selectedText.isEmpty()) {
            Messages.showInfoMessage(project, "Please select code to explain.", "No Selection");
            return;
        }

        // Create options
        Map<String, Object> options = new HashMap<>();
        
        if (virtualFile != null) {
            options.put("fileName", virtualFile.getName());
            options.put("fileExtension", virtualFile.getExtension());
        }

        // Show progress dialog
        ProgressManager.getInstance().run(
                new Task.Backgroundable(project, "Analyzing Code...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        
                        // Explain code
                        CompletableFuture<String> future = AutonomousCodeGenerationService.getInstance(project)
                                .explainCode(selectedText, options);
                        
                        try {
                            // Get explanation
                            String explanation = future.get();
                            
                            // Show explanation
                            ApplicationManager.getApplication().invokeLater(() -> {
                                showExplanationDialog(project, selectedText, explanation);
                            });
                        } catch (Exception ex) {
                            LOG.error("Error explaining code", ex);
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Messages.showErrorDialog(
                                        project,
                                        "Error explaining code: " + ex.getMessage(),
                                        "Error"
                                );
                            });
                        }
                    }
                }
        );
    }

    /**
     * Shows a dialog with the code explanation.
     * @param project The project
     * @param code The code
     * @param explanation The explanation
     */
    private void showExplanationDialog(@NotNull Project project, @NotNull String code, @NotNull String explanation) {
        ExplanationDialog dialog = new ExplanationDialog(project, code, explanation);
        dialog.show();
    }

    /**
     * Dialog for showing code explanations.
     */
    private static class ExplanationDialog extends DialogWrapper {
        private final String code;
        private final String explanation;

        /**
         * Creates a new ExplanationDialog.
         * @param project The project
         * @param code The code
         * @param explanation The explanation
         */
        public ExplanationDialog(@NotNull Project project, @NotNull String code, @NotNull String explanation) {
            super(project, true);
            this.code = code;
            this.explanation = explanation;
            
            setTitle("Code Explanation");
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(800, 600));
            
            // Create code panel
            JPanel codePanel = new JPanel(new BorderLayout());
            codePanel.add(new JBLabel("Code:"), BorderLayout.NORTH);
            
            JBTextArea codeArea = new JBTextArea(code);
            codeArea.setEditable(false);
            codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            codeArea.setBackground(UIManager.getColor("EditorPane.background"));
            codeArea.setBorder(JBUI.Borders.empty(5));
            
            codePanel.add(new JBScrollPane(codeArea), BorderLayout.CENTER);
            
            // Create explanation panel
            JPanel explanationPanel = new JPanel(new BorderLayout());
            explanationPanel.add(new JBLabel("Explanation:"), BorderLayout.NORTH);
            
            JBTextArea explanationArea = new JBTextArea(explanation);
            explanationArea.setEditable(false);
            explanationArea.setLineWrap(true);
            explanationArea.setWrapStyleWord(true);
            explanationArea.setBorder(JBUI.Borders.empty(5));
            
            explanationPanel.add(new JBScrollPane(explanationArea), BorderLayout.CENTER);
            
            // Add panels to splitter
            JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codePanel, explanationPanel);
            splitter.setDividerLocation(200);
            splitter.setResizeWeight(0.3);
            
            panel.add(splitter, BorderLayout.CENTER);
            
            // Add border
            Border border = JBUI.Borders.empty(10);
            panel.setBorder(border);
            
            return panel;
        }

        @Override
        protected Action @NotNull [] createActions() {
            // Only show OK button
            return new Action[]{getOKAction()};
        }
    }
}