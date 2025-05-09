package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for AI assistance in the tool window.
 */
public class AIAssistPanel extends SimpleToolWindowPanel {
    private final Project project;
    private final AutonomousCodeGenerationService codeGenerationService;
    
    private JPanel mainPanel;
    private JBTextArea promptTextArea;
    private Editor outputEditor;
    private JComboBox<String> languageComboBox;
    private JComboBox<String> taskComboBox;
    private JButton generateButton;
    private JButton clearButton;
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     */
    public AIAssistPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.codeGenerationService = AutonomousCodeGenerationService.getInstance(project);
        
        initializeUI();
        
        setContent(mainPanel);
    }
    
    /**
     * Initializes the UI.
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(5));
        
        // Create toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(ActionManager.getInstance().getAction("ModForge.GenerateCode"));
        actionGroup.add(ActionManager.getInstance().getAction("ModForge.FixErrors"));
        actionGroup.add(ActionManager.getInstance().getAction("ModForge.ExplainCode"));
        actionGroup.add(ActionManager.getInstance().getAction("ModForge.GenerateDocumentation"));
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ModForgeAIAssist", actionGroup, true);
        toolbar.setTargetComponent(mainPanel);
        
        mainPanel.add(toolbar.getComponent(), BorderLayout.NORTH);
        
        // Create content panel
        JBSplitter splitter = new JBSplitter(true, 0.3f);
        
        // Input panel
        JPanel inputPanel = createInputPanel();
        splitter.setFirstComponent(inputPanel);
        
        // Output panel
        JPanel outputPanel = createOutputPanel();
        splitter.setSecondComponent(outputPanel);
        
        mainPanel.add(splitter, BorderLayout.CENTER);
    }
    
    /**
     * Creates the input panel.
     * @return The input panel
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(JBUI.Borders.empty(5));
        
        // Prompt input
        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.add(new JBLabel("Enter your prompt:"), BorderLayout.NORTH);
        
        promptTextArea = new JBTextArea();
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        
        promptPanel.add(new JBScrollPane(promptTextArea), BorderLayout.CENTER);
        
        panel.add(promptPanel, BorderLayout.CENTER);
        
        // Controls panel
        JPanel controlsPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        
        // Task selection
        JPanel taskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        taskPanel.add(new JBLabel("Task:"));
        
        taskComboBox = new JComboBox<>(new String[] {
                "Generate Code",
                "Fix Errors",
                "Explain Code",
                "Generate Documentation"
        });
        
        taskPanel.add(taskComboBox);
        
        // Language selection
        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        generateButton = new JButton("Generate", AllIcons.Actions.Execute);
        generateButton.addActionListener(e -> generateContent());
        
        clearButton = new JButton("Clear", AllIcons.Actions.GC);
        clearButton.addActionListener(e -> {
            promptTextArea.setText("");
            updateOutputEditor("");
        });
        
        buttonPanel.add(generateButton);
        buttonPanel.add(clearButton);
        
        controlsPanel.add(taskPanel);
        controlsPanel.add(languagePanel);
        controlsPanel.add(buttonPanel);
        
        panel.add(controlsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Creates the output panel.
     * @return The output panel
     */
    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        panel.add(new JBLabel("Output:"), BorderLayout.NORTH);
        
        // Create editor
        Document document = EditorFactory.getInstance().createDocument("");
        outputEditor = EditorFactory.getInstance().createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension("java"), false);
        
        // Configure editor
        EditorSettings settings = outputEditor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(true);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);
        settings.setAdditionalColumnsCount(3);
        settings.setAdditionalLinesCount(3);
        settings.setCaretRowShown(true);
        
        if (outputEditor instanceof EditorEx) {
            ((EditorEx) outputEditor).setHighlighter(EditorHighlighterFactory.getInstance().createHighlighter(project, "java"));
            
            // Set the editor scheme to match the global scheme
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            ((EditorEx) outputEditor).setColorsScheme(scheme);
        }
        
        panel.add(outputEditor.getComponent(), BorderLayout.CENTER);
        
        // Register editor disposal
        Disposer.register(this, () -> EditorFactory.getInstance().releaseEditor(outputEditor));
        
        return panel;
    }
    
    /**
     * Updates the output editor with the given content.
     * @param content The content
     */
    private void updateOutputEditor(String content) {
        // Get the selected language
        String language = (String) languageComboBox.getSelectedItem();
        
        if (language == null) {
            language = "Java";
        }
        
        // Update editor syntax highlighting
        if (outputEditor instanceof EditorEx) {
            FileType fileType = getFileTypeForLanguage(language);
            ((EditorEx) outputEditor).setHighlighter(EditorHighlighterFactory.getInstance().createHighlighter(project, fileType.getName()));
        }
        
        // Update editor content
        ApplicationManager.getApplication().runWriteAction(() -> {
            Document document = outputEditor.getDocument();
            document.setText(content);
        });
    }
    
    /**
     * Gets the file type for the given language.
     * @param language The language
     * @return The file type
     */
    private FileType getFileTypeForLanguage(String language) {
        switch (language) {
            case "Kotlin":
                return FileTypeManager.getInstance().getFileTypeByExtension("kt");
            case "Groovy":
                return FileTypeManager.getInstance().getFileTypeByExtension("groovy");
            case "XML":
                return FileTypeManager.getInstance().getFileTypeByExtension("xml");
            case "JSON":
                return FileTypeManager.getInstance().getFileTypeByExtension("json");
            case "YAML":
                return FileTypeManager.getInstance().getFileTypeByExtension("yml");
            case "Java":
            default:
                return FileTypeManager.getInstance().getFileTypeByExtension("java");
        }
    }
    
    /**
     * Generates content based on the prompt and selected task.
     */
    private void generateContent() {
        String prompt = promptTextArea.getText();
        
        if (prompt == null || prompt.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "Please enter a prompt.",
                    "Empty Prompt",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String task = (String) taskComboBox.getSelectedItem();
        String language = (String) languageComboBox.getSelectedItem();
        
        if (task == null) {
            task = "Generate Code";
        }
        
        if (language == null) {
            language = "Java";
        }
        
        // Disable controls during generation
        setControlsEnabled(false);
        
        // Clear output
        updateOutputEditor("");
        
        // Generate content
        CompletableFuture<String> future;
        
        switch (task) {
            case "Fix Errors":
                future = codeGenerationService.fixCode(prompt, "Unknown error", null);
                break;
            case "Explain Code":
                future = codeGenerationService.explainCode(prompt, null);
                break;
            case "Generate Documentation":
                future = codeGenerationService.generateDocumentation(prompt, null);
                break;
            case "Generate Code":
            default:
                future = codeGenerationService.generateCode(prompt, language, null);
                break;
        }
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            mainPanel,
                            "Error generating content: " + error.getMessage(),
                            "Generation Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    setControlsEnabled(true);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    updateOutputEditor(result != null ? result : "");
                    setControlsEnabled(true);
                });
            }
        });
    }
    
    /**
     * Enables or disables controls.
     * @param enabled Whether controls should be enabled
     */
    private void setControlsEnabled(boolean enabled) {
        promptTextArea.setEnabled(enabled);
        taskComboBox.setEnabled(enabled);
        languageComboBox.setEnabled(enabled);
        generateButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }
}