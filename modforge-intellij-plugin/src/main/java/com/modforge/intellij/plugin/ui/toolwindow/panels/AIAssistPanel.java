package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Panel for AI-assisted code generation and improvement.
 */
public class AIAssistPanel {
    private static final Logger LOG = Logger.getInstance(AIAssistPanel.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    
    private JPanel mainPanel;
    private JBSplitter splitter;
    private EditorTextField promptField;
    private Editor responseEditor;
    private ComboBox<String> taskTypeComboBox;
    private JButton generateButton;
    private JButton improveButton;
    private JButton explainButton;
    private JButton insertButton;
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     */
    public AIAssistPanel(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = project.getService(AIServiceManager.class);
        createPanel();
    }
    
    /**
     * Creates the panel.
     */
    private void createPanel() {
        // Create main panel
        mainPanel = new SimpleToolWindowPanel(true, true);
        
        // Create splitter
        splitter = new JBSplitter(true, 0.4f);
        splitter.setDividerWidth(3);
        splitter.getDivider().setBackground(UIUtil.getPanelBackground());
        
        // Create prompt panel
        JPanel promptPanel = createPromptPanel();
        splitter.setFirstComponent(promptPanel);
        
        // Create response panel
        JPanel responsePanel = createResponsePanel();
        splitter.setSecondComponent(responsePanel);
        
        // Create toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        // Add actions as needed
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "ModForge.AIAssist", actionGroup, true);
        toolbar.setTargetComponent(mainPanel);
        
        // Add components to main panel
        mainPanel.setToolbar(toolbar.getComponent());
        mainPanel.setContent(splitter);
    }
    
    /**
     * Creates the prompt panel.
     * @return The prompt panel
     */
    private JPanel createPromptPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Prompt"));
        
        // Create prompt field
        promptField = new EditorTextField();
        promptField.setPreferredSize(new Dimension(400, 200));
        
        // Create actions panel
        JPanel actionsPanel = new JBPanel<>(new BorderLayout());
        
        // Task type combobox
        JPanel taskTypePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        taskTypePanel.add(new JBLabel("Task:"));
        
        taskTypeComboBox = new ComboBox<>(new String[] {
                "Generate Code", "Improve Code", "Fix Issues", "Explain Code", "Add Comments"
        });
        taskTypePanel.add(taskTypeComboBox);
        
        actionsPanel.add(taskTypePanel, BorderLayout.WEST);
        
        // Buttons
        JPanel buttonsPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        
        generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> generateCode());
        buttonsPanel.add(generateButton);
        
        improveButton = new JButton("Improve");
        improveButton.addActionListener(e -> improveCode());
        buttonsPanel.add(improveButton);
        
        explainButton = new JButton("Explain");
        explainButton.addActionListener(e -> explainCode());
        buttonsPanel.add(explainButton);
        
        actionsPanel.add(buttonsPanel, BorderLayout.EAST);
        
        panel.add(promptField, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Creates the response panel.
     * @return The response panel
     */
    private JPanel createResponsePanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Response"));
        
        // Create response editor
        Document document = EditorFactory.getInstance().createDocument("");
        responseEditor = EditorFactory.getInstance().createEditor(document, project, 
                FileTypeManager.getInstance().getFileTypeByExtension("java"), false);
        
        // Configure editor
        EditorSettings settings = responseEditor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(true);
        settings.setVirtualSpace(false);
        
        // Set Java syntax highlighting
        if (responseEditor instanceof EditorEx) {
            ((EditorEx) responseEditor).setHighlighter(
                    EditorHighlighterFactory.getInstance().createJavaHighlighter(
                            null, null, project));
        }
        
        // Create actions panel
        JPanel actionsPanel = new JBPanel<>(new BorderLayout());
        
        // Buttons
        JPanel buttonsPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        
        insertButton = new JButton("Insert into Editor");
        insertButton.addActionListener(e -> insertIntoEditor());
        buttonsPanel.add(insertButton);
        
        actionsPanel.add(buttonsPanel, BorderLayout.EAST);
        
        panel.add(responseEditor.getComponent(), BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Generates code based on the prompt.
     */
    private void generateCode() {
        String prompt = promptField.getText();
        if (prompt.isEmpty()) {
            Messages.showErrorDialog(mainPanel, "Please enter a prompt", "Error");
            return;
        }
        
        setButtonsEnabled(false);
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Code", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Generating code...");
                    
                    // This is a simplified implementation
                    // In a real implementation, we would use AIServiceManager to generate code
                    AtomicReference<String> result = new AtomicReference<>("");
                    
                    // Simulate API call
                    try {
                        Thread.sleep(2000);
                        
                        // Simplified example output
                        result.set("/**\n" +
                                " * Generated code based on prompt: " + prompt + "\n" +
                                " */\n" +
                                "public class GeneratedCode {\n" +
                                "    public static void main(String[] args) {\n" +
                                "        System.out.println(\"Hello, ModForge!\");\n" +
                                "    }\n" +
                                "}");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Update UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            responseEditor.getDocument().setText(result.get());
                        } finally {
                            setButtonsEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    LOG.error("Error generating code", e);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Error generating code: " + e.getMessage(),
                                "Error"
                        );
                        setButtonsEnabled(true);
                    });
                }
            }
        });
    }
    
    /**
     * Improves the code in the current editor.
     */
    private void improveCode() {
        VirtualFile currentFile = FileEditorManager.getInstance(project).getSelectedFiles().length > 0
                ? FileEditorManager.getInstance(project).getSelectedFiles()[0]
                : null;
        
        if (currentFile == null) {
            Messages.showErrorDialog(mainPanel, "No file selected", "Error");
            return;
        }
        
        setButtonsEnabled(false);
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Improving Code", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Improving code...");
                    
                    // This is a simplified implementation
                    // In a real implementation, we would use AIServiceManager to improve code
                    AtomicReference<String> result = new AtomicReference<>("");
                    
                    // Simulate API call
                    try {
                        Thread.sleep(2000);
                        
                        // Simplified example output
                        result.set("/**\n" +
                                " * Improved version of " + currentFile.getName() + "\n" +
                                " */\n" +
                                "public class ImprovedCode {\n" +
                                "    public static void main(String[] args) {\n" +
                                "        // More efficient implementation\n" +
                                "        System.out.println(\"Hello, Improved ModForge!\");\n" +
                                "    }\n" +
                                "}");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Update UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            responseEditor.getDocument().setText(result.get());
                        } finally {
                            setButtonsEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    LOG.error("Error improving code", e);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Error improving code: " + e.getMessage(),
                                "Error"
                        );
                        setButtonsEnabled(true);
                    });
                }
            }
        });
    }
    
    /**
     * Explains the code in the current editor.
     */
    private void explainCode() {
        VirtualFile currentFile = FileEditorManager.getInstance(project).getSelectedFiles().length > 0
                ? FileEditorManager.getInstance(project).getSelectedFiles()[0]
                : null;
        
        if (currentFile == null) {
            Messages.showErrorDialog(mainPanel, "No file selected", "Error");
            return;
        }
        
        setButtonsEnabled(false);
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Explaining Code", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Explaining code...");
                    
                    // This is a simplified implementation
                    // In a real implementation, we would use AIServiceManager to explain code
                    AtomicReference<String> result = new AtomicReference<>("");
                    
                    // Simulate API call
                    try {
                        Thread.sleep(2000);
                        
                        // Simplified example output
                        result.set("# Explanation of " + currentFile.getName() + "\n\n" +
                                "This code does the following:\n\n" +
                                "1. It defines a class with a main method\n" +
                                "2. The main method prints a greeting message\n" +
                                "3. It demonstrates basic Java syntax and structure\n\n" +
                                "The code is a simple 'Hello World' program that serves as a starting point for Java applications.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Update UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            responseEditor.getDocument().setText(result.get());
                        } finally {
                            setButtonsEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    LOG.error("Error explaining code", e);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Error explaining code: " + e.getMessage(),
                                "Error"
                        );
                        setButtonsEnabled(true);
                    });
                }
            }
        });
    }
    
    /**
     * Inserts the generated code into the current editor.
     */
    private void insertIntoEditor() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            Messages.showErrorDialog(mainPanel, "No editor selected", "Error");
            return;
        }
        
        String code = responseEditor.getDocument().getText();
        if (code.isEmpty()) {
            Messages.showErrorDialog(mainPanel, "No code to insert", "Error");
            return;
        }
        
        // Insert code at caret position
        final int offset = editor.getCaretModel().getOffset();
        ApplicationManager.getApplication().runWriteAction(() -> {
            editor.getDocument().insertString(offset, code);
        });
    }
    
    /**
     * Enables or disables the buttons.
     * @param enabled Whether to enable the buttons
     */
    private void setButtonsEnabled(boolean enabled) {
        generateButton.setEnabled(enabled);
        improveButton.setEnabled(enabled);
        explainButton.setEnabled(enabled);
        insertButton.setEnabled(enabled);
    }
    
    /**
     * Gets the content component.
     * @return The content component
     */
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Releases the panel resources.
     */
    public void dispose() {
        if (responseEditor != null) {
            EditorFactory.getInstance().releaseEditor(responseEditor);
        }
    }
}