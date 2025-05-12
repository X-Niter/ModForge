package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiFile;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for fixing errors with AI.
 */
public class FixErrorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(FixErrorsAction.class);
    
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
            ModForgeNotificationService.getInstance(project).showErrorNotification(
                    "You must be logged in to fix errors.",
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
        
        if (editor == null || file == null) {
            ModForgeNotificationService.getInstance(project).showErrorNotification(
                    "Please open a file to fix errors.",
                    "No File Selected"
            );
            return;
        }
        
        // Get problems in the file
        Collection<Problem> problems = getProblemsInFile(project, file);
        
        if (problems.isEmpty()) {
            // Check with user if they want to continue
            int result = Messages.showYesNoDialog(
                    project,
                    "No compilation errors detected. Do you want to proceed with error fixing anyway?",
                    "No Errors Detected",
                    "Fix Anyway",
                    "Cancel",
                    AllIcons.General.QuestionDialog
            );
            
            if (result != Messages.YES) {
                return;
            }
        }
        
        // Get code from editor
        String code = editor.getDocument().getText();
        
        // Get all errors as a single string
        String errorMessages = formatProblems(problems);
        
        // Show dialog for error fixing
        FixErrorsDialog dialog = new FixErrorsDialog(project, code, errorMessages, file.getExtension());
        if (dialog.showAndGet()) {
            // Get updated values
            code = dialog.getCode();
            errorMessages = dialog.getErrorMessages();
            String language = dialog.getLanguage();
            
            // Fix errors
            fixErrors(project, editor, code, errorMessages, language);
        }
    }
    
    /**
     * Get all problems in a file.
     *
     * @param project The project
     * @param file    The file
     * @return A collection of problems
     */
    private Collection<Problem> getProblemsInFile(@NotNull Project project, @NotNull VirtualFile file) {
        WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
        Collection<Problem> problems = new ArrayList<>();
        
        if (problemSolver.hasProblemFilesBeneath(psiElement -> {
            if (psiElement instanceof PsiFile) {
                return ((PsiFile) psiElement).getVirtualFile().equals(file);
            }
            return false;
        })) {
            // In IntelliJ IDEA 2025.1.1.1, the signature has changed
            // Create a compatible wrapper for processProblems
            collectProblemsForFile(problemSolver, file, problems);
        }
        
        return problems;
    }
    
    /**
     * Format problems as a single string.
     *
     * @param problems The problems
     * @return A formatted string
     */
    private String formatProblems(@NotNull Collection<Problem> problems) {
        if (problems.isEmpty()) {
            return "No specific errors detected. Please describe the issue you're experiencing.";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Problem problem : problems) {
            // Compatible way to get problem description in IntelliJ IDEA 2025.1.1.1
            sb.append(getProblemDescription(problem)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Compatibility method to collect problems for a file using available APIs in IntelliJ IDEA 2025.1.1.1
     * 
     * @param problemSolver The problem solver
     * @param file The file to check
     * @param problems The collection to populate with problems
     */
    private void collectProblemsForFile(
            @NotNull WolfTheProblemSolver problemSolver,
            @NotNull VirtualFile file,
            @NotNull Collection<Problem> problems) {
        
        // Use a compatibility approach to get problems in 2025.1.1.1
        if (problemSolver.hasProblemFilesBeneath(file)) {
            // Use the stable processProblems API to directly process problems without
            // first retrieving collections that might have API changes in 2025.1.1.1
            problemSolver.processProblems(problem -> {
                VirtualFile pFile = problem.getFile();
                if (pFile != null && pFile.equals(file)) {
                    problems.add(problem);
                }
                return true;
            }, file);
        }
    }
    
    /**
     * Get the description of a problem in a compatible way
     * 
     * @param problem The problem
     * @return The description
     */
    private String getProblemDescription(@NotNull Problem problem) {
        // In IntelliJ IDEA 2025.1.1.1, the API has changed and getDescription() is no longer available
        // We need to use reflection or extract information from other stable methods
        
        // Extract information from the problem using stable methods
        StringBuilder sb = new StringBuilder();
        
        // Get the file information (stable API)
        VirtualFile file = problem.getFile();
        if (file != null) {
            sb.append("[").append(file.getName()).append("] ");
        }
        
        // Get line and column if available (stable API)
        int line = problem.getLine();
        if (line >= 0) {
            sb.append("Line ").append(line);
            
            int column = problem.getColumn();
            if (column >= 0) {
                sb.append(", Column ").append(column);
            }
            sb.append(": ");
        }
        
        // Get text attribute key name which can contain error info (stable API)
        if (problem.getTextAttributesKey() != null) {
            String keyName = problem.getTextAttributesKey().getExternalName();
            if (keyName.contains("error") || keyName.contains("warning")) {
                sb.append(keyName).append(" - ");
            }
        }
        
        // Try multiple approaches to get the description
        
        // Approach 1: Try to access the description through reflection (getDescriptionText or getText method)
        try {
            // Try multiple potential method names that might exist in different IntelliJ versions
            for (String methodName : new String[]{"getDescriptionText", "getText", "getDescription", "getMessage"}) {
                try {
                    java.lang.reflect.Method method = problem.getClass().getMethod(methodName);
                    Object result = method.invoke(problem);
                    if (result instanceof String) {
                        String description = (String) result;
                        if (description != null && !description.isEmpty()) {
                            sb.append(description);
                            return sb.toString(); // Found a valid description, return immediately
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                    // Method doesn't exist, try the next one
                }
            }
        } catch (Exception ignored) {
            // Any reflection exception, continue to fallbacks
        }
        
        // Approach 2: If reflection fails or no description found, try toString
        String problemString = problem.toString();
        if (problemString != null && !problemString.isEmpty() && !problemString.equals(problem.getClass().getName())) {
            sb.append(problemString);
        } else {
            // Approach 3: Final fallback - check if we already have some content
            if (sb.length() == 0) {
                sb.append("Error detected but no description available");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Fix errors in the code.
     *
     * @param project      The project
     * @param editor       The editor
     * @param code         The code with errors
     * @param errorMessage The error message
     * @param language     The programming language
     */
    private void fixErrors(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull String code,
            @NotNull String errorMessage,
            @NotNull String language
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
        
        // Show progress while fixing
        AtomicReference<String> fixedCode = new AtomicReference<>();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Errors") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Fix code
                    String fixed = codeGenService.fixCode(code, errorMessage, language)
                            .exceptionally(e -> {
                                LOG.error("Error fixing code", e);
                                
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showErrorDialog(
                                            project,
                                            "Error fixing code: " + e.getMessage(),
                                            "Fix Error"
                                    );
                                });
                                
                                return null;
                            })
                            .join();
                    
                    fixedCode.set(fixed);
                } catch (Exception e) {
                    LOG.error("Error fixing code", e);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Error fixing code: " + e.getMessage(),
                                "Fix Error"
                        );
                    });
                }
            }
            
            @Override
            public void onSuccess() {
                String fixed = fixedCode.get();
                if (fixed == null || fixed.isEmpty()) {
                    return;
                }
                
                // Show diff dialog
                DiffDialog dialog = new DiffDialog(project, code, fixed, language);
                if (dialog.showAndGet()) {
                    // Apply changes
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        editor.getDocument().setText(fixed);
                    });
                }
            }
        });
    }
    
    /**
     * Dialog for fixing errors.
     */
    private static class FixErrorsDialog extends DialogWrapper {
        private final JBTextArea codeField;
        private final JBTextArea errorField;
        private final JComboBox<String> languageComboBox;
        
        public FixErrorsDialog(
                @Nullable Project project,
                @NotNull String code,
                @NotNull String errorMessages,
                @Nullable String fileExtension
        ) {
            super(project);
            
            setTitle("Fix Errors with ModForge AI");
            setCancelButtonText("Cancel");
            setOKButtonText("Fix");
            
            // Code field
            codeField = new JBTextArea(code, 20, 80);
            codeField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            
            // Error field
            errorField = new JBTextArea(errorMessages, 10, 80);
            errorField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            
            // Language combo box
            languageComboBox = new JComboBox<>(new String[]{
                    "Java", "Kotlin", "JavaScript", "TypeScript", "Python", "C", "C++", "C#",
                    "Go", "Rust", "Ruby", "PHP", "Swift", "HTML", "CSS", "JSON", "XML"
            });
            
            // Set default language based on file extension
            if (fileExtension != null) {
                switch (fileExtension.toLowerCase()) {
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
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create a panel with labels and fields
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel("Code with errors:"), new JBScrollPane(codeField), 1, true)
                    .addLabeledComponent(new JBLabel("Error messages:"), new JBScrollPane(errorField), 1, true)
                    .addLabeledComponent(new JBLabel("Programming language:"), languageComboBox, 1, false)
                    .addComponentFillVertically(new JPanel(), 0);
            
            // Add a usage hint
            JBLabel hintLabel = new JBLabel("Hint: You can edit the code and error messages to provide more context.");
            hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
            hintLabel.setFont(JBUI.Fonts.smallFont());
            formBuilder.addComponent(hintLabel);
            
            JPanel panel = formBuilder.getPanel();
            panel.setPreferredSize(new Dimension(800, 600));
            
            return panel;
        }
        
        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return errorField;
        }
        
        /**
         * Get the code.
         *
         * @return The code
         */
        public String getCode() {
            return codeField.getText();
        }
        
        /**
         * Get the error messages.
         *
         * @return The error messages
         */
        public String getErrorMessages() {
            return errorField.getText();
        }
        
        /**
         * Get the selected language.
         *
         * @return The language
         */
        public String getLanguage() {
            return (String) languageComboBox.getSelectedItem();
        }
        
        @Override
        protected void doOKAction() {
            if (getCode().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Code cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            if (getErrorMessages().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Error messages cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            super.doOKAction();
        }
    }
    
    /**
     * Dialog for showing diff between original and fixed code.
     */
    private static class DiffDialog extends DialogWrapper {
        private final String originalCode;
        private final String fixedCode;
        private final String language;
        
        public DiffDialog(
                @Nullable Project project,
                @NotNull String originalCode,
                @NotNull String fixedCode,
                @NotNull String language
        ) {
            super(project);
            
            this.originalCode = originalCode;
            this.fixedCode = fixedCode;
            this.language = language;
            
            setTitle("Code Changes - " + language);
            setOKButtonText("Apply Changes");
            setCancelButtonText("Discard");
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
            
            // Original code
            JBTextArea originalCodeField = new JBTextArea(originalCode);
            originalCodeField.setEditable(false);
            originalCodeField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JBScrollPane originalScrollPane = new JBScrollPane(originalCodeField);
            JPanel originalPanel = new JPanel(new BorderLayout());
            originalPanel.add(new JBLabel("Original Code:"), BorderLayout.NORTH);
            originalPanel.add(originalScrollPane, BorderLayout.CENTER);
            
            // Fixed code
            JBTextArea fixedCodeField = new JBTextArea(fixedCode);
            fixedCodeField.setEditable(false);
            fixedCodeField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JBScrollPane fixedScrollPane = new JBScrollPane(fixedCodeField);
            JPanel fixedPanel = new JPanel(new BorderLayout());
            fixedPanel.add(new JBLabel("Fixed Code:"), BorderLayout.NORTH);
            fixedPanel.add(fixedScrollPane, BorderLayout.CENTER);
            
            panel.add(originalPanel);
            panel.add(fixedPanel);
            
            // Set preferred size
            panel.setPreferredSize(new Dimension(1000, 600));
            
            return panel;
        }
    }
}