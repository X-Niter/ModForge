package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for generating code with AI.
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Presentation presentation = e.getPresentation();
        
        // Disable action if there's no project
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            presentation.setEnabled(false);
            return;
        }
        
        // Enable action if we have a project and the user is authenticated
        presentation.setEnabled(true);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Make sure the user is authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            Messages.showErrorDialog(
                    project,
                    "You must be logged in to generate code.",
                    "Authentication Required"
            );
            return;
        }
        
        // Get current editor
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        if (editor == null) {
            // If no editor is open, use the current file from the project
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            }
        }
        
        // Show dialog to get prompt
        GenerateCodeDialog dialog = new GenerateCodeDialog(project, file);
        if (dialog.showAndGet()) {
            String prompt = dialog.getPrompt();
            String language = dialog.getLanguage();
            boolean createNewFile = dialog.isCreateNewFile();
            String fileName = dialog.getFileName();
            
            // Generate code
            generateCode(project, editor, file, prompt, language, createNewFile, fileName);
        }
    }
    
    /**
     * Generate code with the given prompt.
     *
     * @param project       The project
     * @param editor        The editor (can be null if creating a new file)
     * @param file          The file (can be null if creating a new file)
     * @param prompt        The prompt for code generation
     * @param language      The programming language
     * @param createNewFile Whether to create a new file
     * @param fileName      The name of the new file (can be null if not creating a new file)
     */
    private void generateCode(
            @NotNull Project project,
            @Nullable Editor editor,
            @Nullable VirtualFile file,
            @NotNull String prompt,
            @NotNull String language,
            boolean createNewFile,
            @Nullable String fileName
    ) {
        // Get code generation service
        AutonomousCodeGenerationService codeGenService =
                project.getService(AutonomousCodeGenerationService.class);
        
        if (codeGenService == null) {
            Messages.showErrorDialog(
                    project,
                    "Code generation service is not available.",
                    "Service Unavailable"
            );
            return;
        }
        
        // Show progress while generating
        AtomicReference<String> generatedCode = new AtomicReference<>();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Code") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Generate code
                    String code = codeGenService.generateCode(prompt, file, language)
                            .exceptionally(e -> {
                                LOG.error("Error generating code", e);
                                
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showErrorDialog(
                                            project,
                                            "Error generating code: " + e.getMessage(),
                                            "Generation Error"
                                    );
                                });
                                
                                return null;
                            })
                            .join();
                    
                    generatedCode.set(code);
                } catch (Exception e) {
                    LOG.error("Error generating code", e);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Error generating code: " + e.getMessage(),
                                "Generation Error"
                        );
                    });
                }
            }
            
            @Override
            public void onSuccess() {
                String code = generatedCode.get();
                if (code == null || code.isEmpty()) {
                    return;
                }
                
                if (createNewFile) {
                    // Create new file
                    createNewFileWithCode(project, fileName, code, language);
                } else if (editor != null) {
                    // Insert into current editor
                    insertCodeIntoEditor(project, editor, code);
                }
            }
        });
    }
    
    /**
     * Insert code into the current editor.
     *
     * @param project The project
     * @param editor  The editor
     * @param code    The code to insert
     */
    private void insertCodeIntoEditor(@NotNull Project project, @NotNull Editor editor, @NotNull String code) {
        Document document = editor.getDocument();
        
        // Get the current selection
        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();
        
        // If no selection, insert at caret
        if (start == end) {
            int caretOffset = editor.getCaretModel().getOffset();
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.insertString(caretOffset, code);
            });
        } else {
            // Replace selection
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(start, end, code);
            });
        }
    }
    
    /**
     * Create a new file with the generated code.
     *
     * @param project  The project
     * @param fileName The name of the new file
     * @param code     The code to insert
     * @param language The programming language
     */
    private void createNewFileWithCode(
            @NotNull Project project,
            @Nullable String fileName,
            @NotNull String code,
            @NotNull String language
    ) {
        // TODO: Implement file creation
        
        // For now, just show the code
        CodeDisplayDialog dialog = new CodeDisplayDialog(project, code, language);
        dialog.show();
    }
    
    /**
     * Dialog for generating code.
     */
    private static class GenerateCodeDialog extends DialogWrapper {
        private final JBTextArea promptField;
        private final JComboBox<String> languageComboBox;
        private final JCheckBox createNewFileCheckBox;
        private final JTextField fileNameField;
        
        public GenerateCodeDialog(@Nullable Project project, @Nullable VirtualFile contextFile) {
            super(project);
            
            setTitle("Generate Code with ModForge AI");
            setCancelButtonText("Cancel");
            setOKButtonText("Generate");
            
            // Prompt field
            promptField = new JBTextArea(10, 50);
            promptField.setLineWrap(true);
            promptField.setWrapStyleWord(true);
            
            // Language combo box
            languageComboBox = new JComboBox<>(new String[]{
                    "Java", "Kotlin", "JavaScript", "TypeScript", "Python", "C", "C++", "C#",
                    "Go", "Rust", "Ruby", "PHP", "Swift", "HTML", "CSS", "JSON", "XML"
            });
            
            // Set default language based on file extension
            if (contextFile != null) {
                String extension = contextFile.getExtension();
                if (extension != null) {
                    switch (extension.toLowerCase()) {
                        case "java":
                            languageComboBox.setSelectedItem("Java");
                            break;
                        case "kt":
                            languageComboBox.setSelectedItem("Kotlin");
                            break;
                        case "js":
                            languageComboBox.setSelectedItem("JavaScript");
                            break;
                        case "ts":
                            languageComboBox.setSelectedItem("TypeScript");
                            break;
                        case "py":
                            languageComboBox.setSelectedItem("Python");
                            break;
                        case "c":
                            languageComboBox.setSelectedItem("C");
                            break;
                        case "cpp":
                        case "cc":
                        case "cxx":
                            languageComboBox.setSelectedItem("C++");
                            break;
                        case "cs":
                            languageComboBox.setSelectedItem("C#");
                            break;
                        case "go":
                            languageComboBox.setSelectedItem("Go");
                            break;
                        case "rs":
                            languageComboBox.setSelectedItem("Rust");
                            break;
                        case "rb":
                            languageComboBox.setSelectedItem("Ruby");
                            break;
                        case "php":
                            languageComboBox.setSelectedItem("PHP");
                            break;
                        case "swift":
                            languageComboBox.setSelectedItem("Swift");
                            break;
                        case "html":
                            languageComboBox.setSelectedItem("HTML");
                            break;
                        case "css":
                            languageComboBox.setSelectedItem("CSS");
                            break;
                        case "json":
                            languageComboBox.setSelectedItem("JSON");
                            break;
                        case "xml":
                            languageComboBox.setSelectedItem("XML");
                            break;
                    }
                }
            }
            
            // Create new file checkbox
            createNewFileCheckBox = new JCheckBox("Create new file");
            createNewFileCheckBox.setSelected(false);
            
            // File name field
            fileNameField = new JTextField();
            fileNameField.setEnabled(false);
            
            // Enable/disable file name field based on checkbox
            createNewFileCheckBox.addActionListener(e -> {
                fileNameField.setEnabled(createNewFileCheckBox.isSelected());
            });
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create a panel with labels and fields
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Write a description of the code you want to generate:"), new JBScrollPane(promptField), 1, true)
                    .addLabeledComponent(new JBLabel("Programming language:"), languageComboBox, 1, false)
                    .addComponent(createNewFileCheckBox, 1)
                    .addLabeledComponent(new JBLabel("File name:"), fileNameField, 1, false)
                    .addComponentFillVertically(new JPanel(), 0);
            
            // Add a usage hint
            JBLabel hintLabel = new JBLabel("Hint: Be as specific as possible. Include details about functionality, parameters, etc.");
            hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
            hintLabel.setFont(JBUI.Fonts.smallFont());
            formBuilder.addComponent(hintLabel);
            
            JPanel panel = formBuilder.getPanel();
            panel.setPreferredSize(new Dimension(600, panel.getPreferredSize().height));
            
            return panel;
        }
        
        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return promptField;
        }
        
        /**
         * Get the entered prompt.
         *
         * @return The prompt
         */
        public String getPrompt() {
            return promptField.getText().trim();
        }
        
        /**
         * Get the selected language.
         *
         * @return The language
         */
        public String getLanguage() {
            return (String) languageComboBox.getSelectedItem();
        }
        
        /**
         * Check if a new file should be created.
         *
         * @return True if a new file should be created, false otherwise
         */
        public boolean isCreateNewFile() {
            return createNewFileCheckBox.isSelected();
        }
        
        /**
         * Get the entered file name.
         *
         * @return The file name
         */
        public String getFileName() {
            return fileNameField.getText().trim();
        }
        
        @Override
        protected void doOKAction() {
            if (getPrompt().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Prompt cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            if (isCreateNewFile() && getFileName().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "File name cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            super.doOKAction();
        }
    }
    
    /**
     * Dialog for displaying generated code.
     */
    private static class CodeDisplayDialog extends DialogWrapper {
        private final String code;
        private final String language;
        
        public CodeDisplayDialog(@Nullable Project project, @NotNull String code, @NotNull String language) {
            super(project);
            
            this.code = code;
            this.language = language;
            
            setTitle("Generated Code - " + language);
            setOKButtonText("Close");
            setCancelButtonText("Copy to Clipboard");
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JBTextArea codeField = new JBTextArea(code);
            codeField.setEditable(false);
            codeField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            
            JBScrollPane scrollPane = new JBScrollPane(codeField);
            scrollPane.setPreferredSize(new Dimension(800, 600));
            
            return scrollPane;
        }
        
        @Override
        protected void doOKAction() {
            super.doOKAction();
        }
        
        @Override
        public void doCancelAction() {
            // Copy to clipboard
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(code);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            
            // Show notification
            Messages.showInfoMessage(
                    getContentPanel(),
                    "Code copied to clipboard",
                    "Copy Successful"
            );
            
            // Don't close the dialog
        }
    }
}