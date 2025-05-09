package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for generating code using AI.
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only if a project is open and AI assist is enabled
        Project project = e.getProject();
        boolean enabled = project != null && ModForgeSettings.getInstance().isEnableAIAssist();
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (editor == null) {
            Messages.showInfoMessage(project, "Please open a file in the editor to generate code.", "No Editor");
            return;
        }

        // Get selected text if any
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        // Create and show dialog
        GenerateCodeDialog dialog = new GenerateCodeDialog(project, editor, virtualFile, selectedText);
        dialog.show();

        // If canceled, return
        if (!dialog.isOK()) {
            return;
        }

        // Get prompt
        String prompt = dialog.getPrompt();
        if (prompt.isEmpty()) {
            Messages.showWarningDialog(project, "Please enter a prompt for code generation.", "Empty Prompt");
            return;
        }

        // Get language
        String language = dialog.getLanguage();

        // Create options
        Map<String, Object> options = new HashMap<>();
        
        if (virtualFile != null) {
            options.put("fileName", virtualFile.getName());
            options.put("fileExtension", virtualFile.getExtension());
        }

        // Generate code
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        Messages.showInfoMessage(project, "Generating code. This may take a few moments...", "Generating Code");
        
        AtomicReference<String> generatedCode = new AtomicReference<>("");
        
        try {
            codeGenService.generateCode(prompt, language, options)
                    .thenAccept(code -> {
                        generatedCode.set(code);
                        
                        // Insert code into editor
                        ApplicationManager.getApplication().invokeLater(() -> {
                            insertCodeIntoEditor(project, editor, code);
                        });
                    })
                    .exceptionally(ex -> {
                        LOG.error("Error generating code", ex);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showErrorDialog(project, "Error generating code: " + ex.getMessage(), "Error");
                        });
                        return null;
                    });
        } catch (Exception ex) {
            LOG.error("Error generating code", ex);
            Messages.showErrorDialog(project, "Error generating code: " + ex.getMessage(), "Error");
        }
    }

    /**
     * Inserts code into the editor.
     * @param project The project
     * @param editor The editor
     * @param code The code to insert
     */
    private void insertCodeIntoEditor(@NotNull Project project, @NotNull Editor editor, @NotNull String code) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        
        // Get insert position
        final int start;
        final int end;
        
        if (selectionModel.hasSelection()) {
            start = selectionModel.getSelectionStart();
            end = selectionModel.getSelectionEnd();
        } else {
            start = end = editor.getCaretModel().getOffset();
        }
        
        // Insert code
        WriteCommandAction.runWriteCommandAction(project, () -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                if (start != end) {
                    document.replaceString(start, end, code);
                } else {
                    document.insertString(start, code);
                }
            }, "Insert Generated Code", null);
        });
    }

    /**
     * Dialog for generating code.
     */
    private static class GenerateCodeDialog extends DialogWrapper {
        private final JBTextArea promptArea;
        private final JComboBox<String> languageComboBox;
        private final Project project;
        private final Editor editor;
        private final VirtualFile file;

        /**
         * Creates a new GenerateCodeDialog.
         * @param project The project
         * @param editor The editor
         * @param file The file
         * @param selectedText The selected text
         */
        public GenerateCodeDialog(@NotNull Project project, @NotNull Editor editor, @Nullable VirtualFile file, 
                                @Nullable String selectedText) {
            super(project, true);
            this.project = project;
            this.editor = editor;
            this.file = file;

            // Create UI components
            promptArea = new JBTextArea(10, 50);
            promptArea.setLineWrap(true);
            promptArea.setWrapStyleWord(true);
            
            if (selectedText != null && !selectedText.isEmpty()) {
                promptArea.setText("Please explain this code and suggest improvements:\n\n" + selectedText);
            } else {
                promptArea.setText("Generate a Minecraft mod class that:");
            }

            // Create language combo box
            languageComboBox = new JComboBox<>(new String[]{"java", "kotlin", "javascript", "typescript", "python"});
            
            // Set default language based on file extension
            if (file != null) {
                String extension = file.getExtension();
                if (extension != null) {
                    switch (extension.toLowerCase()) {
                        case "java":
                            languageComboBox.setSelectedItem("java");
                            break;
                        case "kt":
                            languageComboBox.setSelectedItem("kotlin");
                            break;
                        case "js":
                            languageComboBox.setSelectedItem("javascript");
                            break;
                        case "ts":
                            languageComboBox.setSelectedItem("typescript");
                            break;
                        case "py":
                            languageComboBox.setSelectedItem("python");
                            break;
                    }
                }
            }

            setTitle("Generate Code");
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            JPanel promptPanel = new JPanel(new BorderLayout());
            promptPanel.add(new JBLabel("Enter your prompt:"), BorderLayout.NORTH);
            promptPanel.add(new JBScrollPane(promptArea), BorderLayout.CENTER);
            
            JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            languagePanel.add(new JBLabel("Language:"));
            languagePanel.add(languageComboBox);
            
            panel.add(promptPanel, BorderLayout.CENTER);
            panel.add(languagePanel, BorderLayout.SOUTH);
            
            Border border = JBUI.Borders.empty(10);
            panel.setBorder(border);
            
            return panel;
        }

        /**
         * Gets the prompt.
         * @return The prompt
         */
        public String getPrompt() {
            return promptArea.getText().trim();
        }

        /**
         * Gets the language.
         * @return The language
         */
        public String getLanguage() {
            return (String) languageComboBox.getSelectedItem();
        }
    }
}