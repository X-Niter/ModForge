package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for AI-assisted development.
 */
public final class AIAssistPanel extends SimpleToolWindowPanel {
    private final Project project;
    private final AutonomousCodeGenerationService codeGenService;
    
    private JPanel mainPanel;
    private JBTabbedPane tabbedPane;
    private JTextArea promptTextArea;
    private EditorTextField codeEditorField;
    private JTextArea outputTextArea;
    private JComboBox<String> languageComboBox;
    private JButton generateButton;
    private JButton explainButton;
    private JButton documentButton;
    private JButton fixButton;
    private JPanel statusPanel;
    private JBLabel statusLabel;
    private JProgressBar progressBar;
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     */
    public AIAssistPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        initializeUI();
        
        setContent(mainPanel);
    }
    
    /**
     * Initializes the UI.
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(5));
        
        // Create top panel
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Create center panel
        tabbedPane = new JBTabbedPane();
        
        // Create input tab
        JPanel inputPanel = createInputPanel();
        tabbedPane.addTab("Input", AllIcons.Nodes.Plugin, inputPanel);
        
        // Create output tab
        JPanel outputPanel = createOutputPanel();
        tabbedPane.addTab("Output", AllIcons.Nodes.Console, outputPanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Create status panel
        statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        // Register listeners
        registerListeners();
    }
    
    /**
     * Creates the top panel.
     * @return The top panel
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        generateButton = new JButton("Generate", AllIcons.Actions.Execute);
        explainButton = new JButton("Explain", AllIcons.Actions.Help);
        documentButton = new JButton("Document", AllIcons.Actions.Documentation);
        fixButton = new JButton("Fix", AllIcons.Actions.QuickfixBulb);
        
        buttonPanel.add(generateButton);
        buttonPanel.add(explainButton);
        buttonPanel.add(documentButton);
        buttonPanel.add(fixButton);
        
        panel.add(buttonPanel, BorderLayout.WEST);
        
        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        languagePanel.add(new JBLabel("Language:"));
        
        languageComboBox = new JComboBox<>(new String[] {
                "Java",
                "Kotlin",
                "Groovy",
                "XML",
                "JSON",
                "YAML"
        });
        
        languagePanel.add(languageComboBox);
        
        panel.add(languagePanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Creates the input panel.
     * @return The input panel
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));
        
        promptPanel.add(new JBLabel("Prompt:"), BorderLayout.NORTH);
        
        promptTextArea = new JTextArea();
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        promptTextArea.setRows(5);
        
        promptPanel.add(new JBScrollPane(promptTextArea), BorderLayout.CENTER);
        
        panel.add(promptPanel, BorderLayout.NORTH);
        
        JPanel codePanel = new JPanel(new BorderLayout());
        
        codePanel.add(new JBLabel("Code:"), BorderLayout.NORTH);
        
        Document document = EditorFactory.getInstance().createDocument("");
        codeEditorField = new EditorTextField(document, project, FileTypeManager.getInstance().getFileTypeByExtension("java"), false, false);
        
        codePanel.add(codeEditorField, BorderLayout.CENTER);
        
        panel.add(codePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the output panel.
     * @return The output panel
     */
    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        
        panel.add(new JBScrollPane(outputTextArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the status panel.
     * @return The status panel
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        
        statusLabel = new JBLabel("Ready");
        panel.add(statusLabel, BorderLayout.WEST);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        panel.add(progressBar, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Registers the event listeners.
     */
    private void registerListeners() {
        generateButton.addActionListener(e -> generateCode());
        explainButton.addActionListener(e -> explainCode());
        documentButton.addActionListener(e -> documentCode());
        fixButton.addActionListener(e -> fixCode());
    }
    
    /**
     * Generates code based on the prompt.
     */
    private void generateCode() {
        String prompt = promptTextArea.getText();
        String language = (String) languageComboBox.getSelectedItem();
        
        if (prompt.isEmpty()) {
            showError("Please enter a prompt");
            return;
        }
        
        showStatus("Generating code...", true);
        
        Map<String, Object> options = new HashMap<>();
        
        CompletableFuture<String> future = codeGenService.generateCode(prompt, language, options);
        
        future.whenComplete((result, error) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (error != null) {
                    showError("Error generating code: " + error.getMessage());
                    return;
                }
                
                codeEditorField.setText(result);
                tabbedPane.setSelectedIndex(0); // Switch to input tab
                showStatus("Code generated successfully", false);
            });
        });
    }
    
    /**
     * Explains the current code.
     */
    private void explainCode() {
        String code = codeEditorField.getText();
        
        if (code.isEmpty()) {
            showError("Please enter code to explain");
            return;
        }
        
        showStatus("Explaining code...", true);
        
        CompletableFuture<String> future = codeGenService.explainCode(code, null);
        
        future.whenComplete((result, error) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (error != null) {
                    showError("Error explaining code: " + error.getMessage());
                    return;
                }
                
                outputTextArea.setText(result);
                tabbedPane.setSelectedIndex(1); // Switch to output tab
                showStatus("Code explained successfully", false);
            });
        });
    }
    
    /**
     * Documents the current code.
     */
    private void documentCode() {
        String code = codeEditorField.getText();
        
        if (code.isEmpty()) {
            showError("Please enter code to document");
            return;
        }
        
        showStatus("Documenting code...", true);
        
        CompletableFuture<String> future = codeGenService.generateDocumentation(code, null);
        
        future.whenComplete((result, error) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (error != null) {
                    showError("Error documenting code: " + error.getMessage());
                    return;
                }
                
                codeEditorField.setText(result);
                tabbedPane.setSelectedIndex(0); // Switch to input tab
                showStatus("Code documented successfully", false);
            });
        });
    }
    
    /**
     * Fixes the current code.
     */
    private void fixCode() {
        String code = codeEditorField.getText();
        
        if (code.isEmpty()) {
            showError("Please enter code to fix");
            return;
        }
        
        showStatus("Fixing code...", true);
        
        CompletableFuture<String> future = codeGenService.fixCode(code, null, null);
        
        future.whenComplete((result, error) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (error != null) {
                    showError("Error fixing code: " + error.getMessage());
                    return;
                }
                
                codeEditorField.setText(result);
                tabbedPane.setSelectedIndex(0); // Switch to input tab
                showStatus("Code fixed successfully", false);
            });
        });
    }
    
    /**
     * Shows a status message.
     * @param message The message
     * @param inProgress Whether the operation is in progress
     */
    private void showStatus(String message, boolean inProgress) {
        statusLabel.setText(message);
        
        if (inProgress) {
            statusLabel.setIcon(AllIcons.Actions.Refresh);
        } else {
            statusLabel.setIcon(null);
        }
        
        progressBar.setVisible(inProgress);
    }
    
    /**
     * Shows an error message.
     * @param message The error message
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setIcon(AllIcons.General.Error);
        progressBar.setVisible(false);
    }
}