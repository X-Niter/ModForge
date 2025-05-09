package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for AI assistance features.
 */
public class AIAssistPanel implements Disposable {
    private final Project project;
    private final ToolWindow toolWindow;
    private final SimpleToolWindowPanel panel;
    
    private JBTextArea promptArea;
    private JBTextArea responseArea;
    private JButton generateButton;
    private JComboBox<String> taskTypeComboBox;
    private JComboBox<String> languageComboBox;
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     * @param toolWindow The tool window
     */
    public AIAssistPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        
        // Create panel
        panel = new SimpleToolWindowPanel(true, true);
        
        // Init UI
        initUI();
    }
    
    /**
     * Initializes the UI.
     */
    private void initUI() {
        // Create components
        promptArea = new JBTextArea(5, 50);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        
        responseArea = new JBTextArea(15, 50);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setEditable(false);
        
        generateButton = new JButton("Generate");
        
        String[] taskTypes = {"Generate Code", "Fix Code", "Explain Code", "Generate Documentation", "Other"};
        taskTypeComboBox = new ComboBox<>(taskTypes);
        
        String[] languages = {"java", "kotlin", "javascript", "typescript", "python"};
        languageComboBox = new ComboBox<>(languages);
        
        // Create panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JPanel northPanel = new JPanel(new BorderLayout());
        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.add(new JBLabel("Enter your prompt:"), BorderLayout.NORTH);
        promptPanel.add(new JBScrollPane(promptArea), BorderLayout.CENTER);
        
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(new JBLabel("Task Type:"));
        optionsPanel.add(taskTypeComboBox);
        optionsPanel.add(Box.createHorizontalStrut(10));
        optionsPanel.add(new JBLabel("Language:"));
        optionsPanel.add(languageComboBox);
        
        northPanel.add(promptPanel, BorderLayout.CENTER);
        northPanel.add(optionsPanel, BorderLayout.SOUTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JBLabel("Response:"), BorderLayout.NORTH);
        centerPanel.add(new JBScrollPane(responseArea), BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(generateButton);
        
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        
        // Add padding
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add to panel
        panel.setContent(mainPanel);
        
        // Add action listeners
        generateButton.addActionListener(this::onGenerateClicked);
        taskTypeComboBox.addActionListener(this::onTaskTypeChanged);
    }
    
    /**
     * Called when the generate button is clicked.
     * @param e The action event
     */
    private void onGenerateClicked(ActionEvent e) {
        // Get prompt
        String prompt = promptArea.getText().trim();
        
        if (prompt.isEmpty()) {
            showMessage("Please enter a prompt.");
            return;
        }
        
        // Get task type
        String taskType = (String) taskTypeComboBox.getSelectedItem();
        
        if (taskType == null) {
            showMessage("Please select a task type.");
            return;
        }
        
        // Get language
        String language = (String) languageComboBox.getSelectedItem();
        
        if (language == null) {
            showMessage("Please select a language.");
            return;
        }
        
        // Disable button
        generateButton.setEnabled(false);
        
        // Clear response
        responseArea.setText("Generating...");
        
        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("language", language);
        
        // Get service
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        
        // Generate response
        CompletableFuture<String> future;
        
        switch (taskType) {
            case "Generate Code":
                future = service.generateCode(prompt, language, options);
                break;
            case "Fix Code":
                future = service.fixCode(prompt, null, options);
                break;
            case "Explain Code":
                future = service.explainCode(prompt, options);
                break;
            case "Generate Documentation":
                future = service.generateDocumentation(prompt, options);
                break;
            default:
                future = service.generateChatResponse(prompt, options);
                break;
        }
        
        // Handle response
        future.thenAccept(response -> {
            SwingUtilities.invokeLater(() -> {
                responseArea.setText(response);
                generateButton.setEnabled(true);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                responseArea.setText("Error: " + ex.getMessage());
                generateButton.setEnabled(true);
            });
            return null;
        });
    }
    
    /**
     * Called when the task type is changed.
     * @param e The action event
     */
    private void onTaskTypeChanged(ActionEvent e) {
        // Get task type
        String taskType = (String) taskTypeComboBox.getSelectedItem();
        
        if (taskType == null) {
            return;
        }
        
        // Update prompt based on task type
        switch (taskType) {
            case "Generate Code":
                if (promptArea.getText().isEmpty()) {
                    promptArea.setText("Generate a Minecraft mod class that:");
                }
                break;
            case "Fix Code":
                if (promptArea.getText().isEmpty()) {
                    promptArea.setText("Fix the following code:\n\n");
                }
                break;
            case "Explain Code":
                if (promptArea.getText().isEmpty()) {
                    promptArea.setText("Explain the following code:\n\n");
                }
                break;
            case "Generate Documentation":
                if (promptArea.getText().isEmpty()) {
                    promptArea.setText("Generate documentation for the following code:\n\n");
                }
                break;
        }
    }
    
    /**
     * Shows a message.
     * @param message The message
     */
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                panel,
                message,
                "ModForge",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Gets the content.
     * @return The content
     */
    public JComponent getContent() {
        return panel;
    }
    
    @Override
    public void dispose() {
        // Clean up resources
    }
}