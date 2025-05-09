package com.modforge.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.models.CodeGenerationRequest;
import com.modforge.intellij.plugin.models.CodeGenerationResponse;
import com.modforge.intellij.plugin.services.AIServiceManager;
import com.modforge.intellij.plugin.services.ModForgeProjectService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Panel for generating code using AI.
 */
public class CodeGeneratorPanel {
    private static final Logger LOG = Logger.getInstance(CodeGeneratorPanel.class);
    
    private final Project project;
    private JPanel mainPanel;
    private JBTextArea promptTextArea;
    private JBTextArea resultTextArea;
    private JButton generateButton;
    private JComboBox<String> categoryComboBox;
    
    public CodeGeneratorPanel(Project project) {
        this.project = project;
        createUI();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create north panel with title and description
        JPanel northPanel = new JBPanel<>(new BorderLayout());
        
        JBLabel titleLabel = new JBLabel("AI Code Generator");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18));
        northPanel.add(titleLabel, BorderLayout.NORTH);
        
        JBLabel descriptionLabel = new JBLabel("<html><body style='width: 400px'>" +
                "Describe what you want to create, and the AI will generate the code for you. " +
                "Be specific about what you want, including any special behaviors or integrations with other mods." +
                "</body></html>");
        descriptionLabel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        northPanel.add(descriptionLabel, BorderLayout.CENTER);
        
        mainPanel.add(northPanel, BorderLayout.NORTH);
        
        // Create center panel with input and output
        JPanel centerPanel = new JBPanel<>(new GridLayout(1, 2, 10, 0));
        centerPanel.setBorder(JBUI.Borders.empty(10, 0));
        
        // Left panel - input
        JPanel inputPanel = new JBPanel<>(new BorderLayout());
        
        JPanel inputHeaderPanel = new JBPanel<>(new BorderLayout());
        JBLabel inputLabel = new JBLabel("Describe what you want to create:");
        inputLabel.setFont(inputLabel.getFont().deriveFont(Font.BOLD));
        inputHeaderPanel.add(inputLabel, BorderLayout.WEST);
        
        // Category selector
        JPanel categoryPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        JBLabel categoryLabel = new JBLabel("Category:");
        categoryComboBox = new JComboBox<>(new String[]{
                "Block", "Item", "Entity", "Biome", "Structure", "Enchantment", "Potion", "Other"
        });
        categoryPanel.add(categoryLabel);
        categoryPanel.add(categoryComboBox);
        inputHeaderPanel.add(categoryPanel, BorderLayout.EAST);
        
        inputPanel.add(inputHeaderPanel, BorderLayout.NORTH);
        
        promptTextArea = new JBTextArea();
        promptTextArea.setBorder(JBUI.Borders.empty(5));
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        promptTextArea.setText("Create a new block that...");
        JBScrollPane promptScrollPane = new JBScrollPane(promptTextArea);
        promptScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        inputPanel.add(promptScrollPane, BorderLayout.CENTER);
        
        // Generate button
        generateButton = new JButton("Generate Code");
        generateButton.addActionListener(this::onGenerateButtonClicked);
        
        JPanel generateButtonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        generateButtonPanel.add(generateButton);
        generateButtonPanel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        inputPanel.add(generateButtonPanel, BorderLayout.SOUTH);
        
        centerPanel.add(inputPanel);
        
        // Right panel - output
        JPanel outputPanel = new JBPanel<>(new BorderLayout());
        
        JBLabel outputLabel = new JBLabel("Generated Code:");
        outputLabel.setFont(outputLabel.getFont().deriveFont(Font.BOLD));
        outputPanel.add(outputLabel, BorderLayout.NORTH);
        
        resultTextArea = new JBTextArea();
        resultTextArea.setBorder(JBUI.Borders.empty(5));
        resultTextArea.setEditable(false);
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JBScrollPane resultScrollPane = new JBScrollPane(resultTextArea);
        resultScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        outputPanel.add(resultScrollPane, BorderLayout.CENTER);
        
        // Buttons for the output
        JPanel outputButtonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            if (resultTextArea.getText().isEmpty()) return;
            resultTextArea.selectAll();
            resultTextArea.copy();
            resultTextArea.select(0, 0);
        });
        
        JButton insertButton = new JButton("Insert into File");
        insertButton.addActionListener(e -> {
            if (resultTextArea.getText().isEmpty()) return;
            // This would use the IDE's PSI to insert the code into the active editor
            LOG.info("Insert into File button clicked");
        });
        
        outputButtonPanel.add(copyButton);
        outputButtonPanel.add(insertButton);
        outputButtonPanel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        outputPanel.add(outputButtonPanel, BorderLayout.SOUTH);
        
        centerPanel.add(outputPanel);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
    }
    
    /**
     * Called when the Generate button is clicked.
     * @param e The action event
     */
    private void onGenerateButtonClicked(ActionEvent e) {
        LOG.info("Generate button clicked");
        
        String prompt = promptTextArea.getText();
        if (prompt.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Please enter a description of what you want to create.",
                    "Empty Prompt",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Add category to prompt if selected
        String category = (String) categoryComboBox.getSelectedItem();
        if (category != null && !category.equals("Other")) {
            prompt = "Create a new " + category.toLowerCase() + ": " + prompt;
        }
        
        // Disable UI while generating
        setUIEnabled(false);
        resultTextArea.setText("Generating code...");
        
        // Get services
        ModForgeProjectService projectService = project.getService(ModForgeProjectService.class);
        AIServiceManager aiServiceManager = project.getService(AIServiceManager.class);
        
        // Create request
        CodeGenerationRequest request = new CodeGenerationRequest(
                prompt,
                projectService.getModLoaderType(),
                projectService.getMinecraftVersion()
        );
        
        // Use SwingWorker to perform the API call in a background thread
        SwingWorker<CodeGenerationResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected CodeGenerationResponse doInBackground() throws Exception {
                return aiServiceManager.generateCode(request);
            }
            
            @Override
            protected void done() {
                try {
                    CodeGenerationResponse response = get();
                    if (response != null) {
                        resultTextArea.setText(response.getCode());
                        
                        // If there's an explanation, append it as a comment at the end
                        if (response.getExplanation() != null && !response.getExplanation().isEmpty()) {
                            resultTextArea.append("\n\n/*\n * Explanation: \n * " +
                                    response.getExplanation().replace("\n", "\n * ") +
                                    "\n */");
                        }
                    } else {
                        resultTextArea.setText("Failed to generate code. Please try again.");
                    }
                } catch (Exception ex) {
                    LOG.error("Error generating code", ex);
                    resultTextArea.setText("Error generating code: " + ex.getMessage());
                } finally {
                    setUIEnabled(true);
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Enables or disables the UI.
     * @param enabled Whether the UI should be enabled
     */
    private void setUIEnabled(boolean enabled) {
        promptTextArea.setEnabled(enabled);
        generateButton.setEnabled(enabled);
        categoryComboBox.setEnabled(enabled);
    }
    
    /**
     * Gets the main content panel.
     * @return The main content panel
     */
    public JPanel getContent() {
        return mainPanel;
    }
}