package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Panel for AI assistance.
 * This panel provides AI-powered assistance for code generation, fixing, and documentation.
 */
public class AIAssistPanel {
    private static final Logger LOG = Logger.getInstance(AIAssistPanel.class);
    
    private final Project project;
    private final JPanel mainPanel;
    private final JBTextArea promptArea;
    private final ComboBox<String> modelComboBox;
    private final ComboBox<String> actionComboBox;
    private final JButton generateButton;
    private Editor responseEditor;
    private Document responseDocument;
    private final JBLabel statusLabel;
    private final JProgressBar progressBar;
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     */
    public AIAssistPanel(@NotNull Project project) {
        this.project = project;
        
        mainPanel = new JBPanel<>(new BorderLayout());
        
        // Create UI components
        promptArea = new JBTextArea();
        promptArea.setRows(5);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setBorder(JBUI.Borders.empty(5));
        
        // Create models combo box with the latest models
        modelComboBox = new ComboBox<>(new String[]{"gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"});
        // Select the configured model, falling back to gpt-4o if the configured model is gpt-4 (which is outdated)
        String configuredModel = ModForgeSettings.getInstance().getOpenAiModel();
        if ("gpt-4".equals(configuredModel)) {
            configuredModel = "gpt-4o"; // automatically upgrade from gpt-4 to gpt-4o
            ModForgeSettings.getInstance().setOpenAiModel(configuredModel);
        }
        modelComboBox.setSelectedItem(configuredModel);
        modelComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String model = (String) e.getItem();
                ModForgeSettings.getInstance().setOpenAiModel(model);
            }
        });
        
        // Create action combo box
        actionComboBox = new ComboBox<>(new String[]{
                "Generate Code",
                "Fix Code",
                "Generate Documentation",
                "Explain Code"
        });
        
        // Create generate button
        generateButton = new JButton("Generate");
        generateButton.addActionListener(this::onGenerateClick);
        
        // Create response editor
        responseDocument = EditorFactory.getInstance().createDocument("");
        responseEditor = EditorFactory.getInstance().createEditor(
                responseDocument,
                project,
                FileTypeManager.getInstance().getFileTypeByExtension("java"),
                false
        );
        
        // Set editor settings
        if (responseEditor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) responseEditor;
            editorEx.setHorizontalScrollbarVisible(true);
            editorEx.setVerticalScrollbarVisible(true);
            editorEx.setCaretVisible(true);
            editorEx.setEmbeddedIntoDialogWrapper(true);
            
            // Set highlighter
            editorEx.setHighlighter(EditorHighlighterFactory.getInstance().createHighlighter(
                    project,
                    FileTypeManager.getInstance().getFileTypeByExtension("java")
            ));
        }
        
        // Create status label
        statusLabel = new JBLabel("");
        statusLabel.setBorder(JBUI.Borders.empty(5, 10));
        
        // Create progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        
        // Create action toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // Add Insert to Editor action
        actionGroup.add(new AnAction("Insert to Editor", "Insert to current editor", AllIcons.Actions.MenuPaste) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                insertToEditor();
            }
            
            @Override
            public void update(@NotNull AnActionEvent e) {
                // Enable only if there is a response and an active editor
                e.getPresentation().setEnabled(
                        responseDocument.getText().length() > 0 &&
                                FileEditorManager.getInstance(project).getSelectedTextEditor() != null
                );
            }
        });
        
        // Add Clear action
        actionGroup.add(new AnAction("Clear", "Clear prompt and response", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                promptArea.setText("");
                responseDocument.setText("");
                statusLabel.setText("");
                IdeFocusManager.getInstance(project).requestFocus(promptArea, true);
            }
        });
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "ModForgeAIAssist",
                actionGroup,
                true
        );
        
        // Create panels
        JPanel controlPanel = new JBPanel<>(new BorderLayout());
        
        JPanel promptPanel = new JBPanel<>(new BorderLayout());
        promptPanel.setBorder(JBUI.Borders.empty(5));
        promptPanel.add(new JBLabel("Enter your prompt:"), BorderLayout.NORTH);
        promptPanel.add(new JBScrollPane(promptArea), BorderLayout.CENTER);
        
        JPanel optionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(new JBLabel("Action:"));
        optionsPanel.add(actionComboBox);
        optionsPanel.add(new JBLabel("Model:"));
        optionsPanel.add(modelComboBox);
        optionsPanel.add(generateButton);
        
        JPanel statusPanel = new JBPanel<>(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        
        controlPanel.add(promptPanel, BorderLayout.CENTER);
        controlPanel.add(optionsPanel, BorderLayout.NORTH);
        controlPanel.add(statusPanel, BorderLayout.SOUTH);
        
        JPanel responsePanel = new JBPanel<>(new BorderLayout());
        responsePanel.setBorder(JBUI.Borders.empty(5));
        responsePanel.add(new JBLabel("Response:"), BorderLayout.NORTH);
        responsePanel.add(responseEditor.getComponent(), BorderLayout.CENTER);
        
        // Create splitter
        OnePixelSplitter splitter = new OnePixelSplitter(true, 0.3f);
        splitter.setFirstComponent(controlPanel);
        splitter.setSecondComponent(responsePanel);
        
        // Create toolbar wrapper
        BorderLayoutPanel toolbarWrapper = JBUI.Panels.simplePanel();
        toolbarWrapper.addToLeft(toolbar.getComponent());
        
        // Set up main panel
        mainPanel.add(splitter, BorderLayout.CENTER);
        mainPanel.add(toolbarWrapper, BorderLayout.NORTH);
    }
    
    /**
     * Gets the main panel.
     * @return The main panel
     */
    @NotNull
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Disposes the panel.
     */
    public void dispose() {
        if (responseEditor != null) {
            EditorFactory.getInstance().releaseEditor(responseEditor);
            responseEditor = null;
        }
    }
    
    /**
     * Handles Generate button click.
     * @param e The action event
     */
    private void onGenerateClick(ActionEvent e) {
        // Get prompt
        String prompt = promptArea.getText().trim();
        if (prompt.isEmpty()) {
            showError("Please enter a prompt");
            return;
        }
        
        // Get action
        String action = (String) actionComboBox.getSelectedItem();
        if (action == null) {
            showError("Please select an action");
            return;
        }
        
        // Check API key
        String apiKey = ModForgeSettings.getInstance().getOpenAiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            showError("OpenAI API key is not set. Please configure it in the settings.");
            return;
        }
        
        // Get model
        String model = (String) modelComboBox.getSelectedItem();
        if (model == null) {
            model = "gpt-4o"; // Use the newest OpenAI model
        }
        
        // Show progress
        progressBar.setVisible(true);
        generateButton.setEnabled(false);
        statusLabel.setText("Generating...");
        statusLabel.setForeground(JBColor.GRAY);
        
        // Get active file and editor
        Editor activeEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        VirtualFile activeFile = activeEditor != null ? 
                FileEditorManager.getInstance(project).getSelectedFiles().length > 0 ? 
                        FileEditorManager.getInstance(project).getSelectedFiles()[0] : null : null;
        
        // Get selected text
        String selectedText = "";
        if (activeEditor != null) {
            selectedText = activeEditor.getSelectionModel().getSelectedText();
        }
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        
        if (activeFile != null) {
            options.put("fileName", activeFile.getName());
            options.put("fileExtension", activeFile.getExtension());
            
            // Get file type
            FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(activeFile);
            if (fileType != null) {
                options.put("fileType", fileType.getName());
            }
        }
        
        // Call AI service
        AIServiceManager aiServiceManager = AIServiceManager.getInstance(project);
        CompletableFuture<String> future;
        
        switch (action) {
            case "Generate Code":
                future = aiServiceManager.generateCode(prompt, getLanguageFromFile(activeFile), options);
                break;
                
            case "Fix Code":
                String code = selectedText != null && !selectedText.isEmpty() ? selectedText : prompt;
                future = aiServiceManager.fixCode(code, null, options);
                break;
                
            case "Generate Documentation":
                String codeToDocument = selectedText != null && !selectedText.isEmpty() ? selectedText : prompt;
                future = aiServiceManager.generateDocumentation(codeToDocument, options);
                break;
                
            case "Explain Code":
                String codeToExplain = selectedText != null && !selectedText.isEmpty() ? selectedText : prompt;
                future = aiServiceManager.explainCode(codeToExplain, options);
                break;
                
            default:
                showError("Unknown action: " + action);
                return;
        }
        
        // Handle response
        future.whenComplete((response, exception) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                progressBar.setVisible(false);
                generateButton.setEnabled(true);
                
                if (exception != null) {
                    LOG.error("Error generating response", exception);
                    showError("Error: " + exception.getMessage());
                    return;
                }
                
                // Show response
                if (response == null || response.isEmpty()) {
                    showError("No response received");
                    return;
                }
                
                responseDocument.setText(response);
                statusLabel.setText("Generated successfully");
                statusLabel.setForeground(JBColor.GRAY);
                
                // Highlight syntax based on file type
                if (responseEditor instanceof EditorEx && activeFile != null) {
                    EditorEx editorEx = (EditorEx) responseEditor;
                    editorEx.setHighlighter(EditorHighlighterFactory.getInstance().createHighlighter(
                            project,
                            FileTypeManager.getInstance().getFileTypeByFile(activeFile)
                    ));
                }
            });
        }).exceptionally(ex -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                progressBar.setVisible(false);
                generateButton.setEnabled(true);
                showError("Error: " + ex.getMessage());
            });
            return null;
        });
    }
    
    /**
     * Gets the programming language from a file.
     * @param file The file
     * @return The programming language
     */
    @NotNull
    private String getLanguageFromFile(@Nullable VirtualFile file) {
        if (file == null) {
            return "java";
        }
        
        String extension = file.getExtension();
        if (extension == null) {
            return "java";
        }
        
        switch (extension.toLowerCase()) {
            case "java":
                return "java";
            case "kt":
            case "kts":
                return "kotlin";
            case "js":
            case "jsx":
            case "ts":
            case "tsx":
                return "javascript";
            case "py":
                return "python";
            case "rb":
                return "ruby";
            case "go":
                return "go";
            case "cs":
                return "csharp";
            case "cpp":
            case "cc":
            case "cxx":
            case "c":
            case "h":
            case "hpp":
                return "cpp";
            case "php":
                return "php";
            case "rs":
                return "rust";
            case "swift":
                return "swift";
            case "scala":
                return "scala";
            case "groovy":
                return "groovy";
            case "dart":
                return "dart";
            default:
                return "java";
        }
    }
    
    /**
     * Inserts the response text into the active editor.
     */
    private void insertToEditor() {
        Editor activeEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (activeEditor == null) {
            showError("No active editor");
            return;
        }
        
        String text = responseDocument.getText();
        if (text.isEmpty()) {
            showError("No response to insert");
            return;
        }
        
        // Insert text
        ApplicationManager.getApplication().runWriteAction(() -> {
            CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> {
                        // Replace selection or insert at caret
                        if (activeEditor.getSelectionModel().hasSelection()) {
                            int start = activeEditor.getSelectionModel().getSelectionStart();
                            int end = activeEditor.getSelectionModel().getSelectionEnd();
                            activeEditor.getDocument().replaceString(start, end, text);
                        } else {
                            int offset = activeEditor.getCaretModel().getOffset();
                            activeEditor.getDocument().insertString(offset, text);
                        }
                    },
                    "Insert AI Response",
                    null
            );
        });
        
        statusLabel.setText("Inserted to editor");
        statusLabel.setForeground(JBColor.GRAY);
    }
    
    /**
     * Shows an error message.
     * @param message The error message
     */
    private void showError(@NotNull String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(JBColor.RED);
        LOG.warn(message);
    }
}