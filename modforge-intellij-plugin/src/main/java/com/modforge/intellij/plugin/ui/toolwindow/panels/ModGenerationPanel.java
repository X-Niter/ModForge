package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousIdeCoordinatorService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for mod generation using AI.
 */
public class ModGenerationPanel {
    private static final Logger LOG = Logger.getInstance(ModGenerationPanel.class);
    
    private final Project project;
    private final AutonomousIdeCoordinatorService coordinatorService;
    
    private JPanel mainPanel;
    private JBTextField modNameField;
    private JBTextField modIdField;
    private JBTextField modVersionField;
    private ComboBox<String> modLoaderComboBox;
    private ComboBox<String> minecraftVersionComboBox;
    private EditorTextField descriptionField;
    private JButton generateButton;
    private JButton analyzeButton;
    private JButton optimizeButton;
    private JTextArea consoleTextArea;
    
    /**
     * Creates a new ModGenerationPanel.
     * @param project The project
     */
    public ModGenerationPanel(@NotNull Project project) {
        this.project = project;
        this.coordinatorService = AutonomousIdeCoordinatorService.getInstance(project);
        createPanel();
    }
    
    /**
     * Creates the panel.
     */
    private void createPanel() {
        // Initialize fields
        modNameField = new JBTextField();
        modNameField.setEmptyText("Your mod name");
        
        modIdField = new JBTextField();
        modIdField.setEmptyText("your_mod_id");
        
        modVersionField = new JBTextField("1.0.0");
        
        // Mod loaders
        modLoaderComboBox = new ComboBox<>(new String[] {
                "Forge", "Fabric", "Quilt", "Architectury (Multi-loader)"
        });
        
        // Minecraft versions
        minecraftVersionComboBox = new ComboBox<>(new String[] {
                "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.16.5"
        });
        
        // Description field
        descriptionField = new EditorTextField();
        descriptionField.setPreferredSize(new Dimension(400, 200));
        
        // Create buttons
        generateButton = new JButton("Generate Mod");
        generateButton.addActionListener(e -> generateMod());
        
        analyzeButton = new JButton("Analyze Project");
        analyzeButton.addActionListener(e -> analyzeProject());
        
        optimizeButton = new JButton("Optimize Code");
        optimizeButton.addActionListener(e -> optimizeProject());
        
        // Create console
        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JBScrollPane consoleScrollPane = new JBScrollPane(consoleTextArea);
        consoleScrollPane.setPreferredSize(new Dimension(400, 200));
        
        // Create form
        JPanel formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Mod Name:", modNameField)
                .addLabeledComponent("Mod ID:", modIdField)
                .addLabeledComponent("Version:", modVersionField)
                .addLabeledComponent("Mod Loader:", modLoaderComboBox)
                .addLabeledComponent("Minecraft Version:", minecraftVersionComboBox)
                .addSeparator()
                .addLabeledComponent("Description:", descriptionField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(JBUI.Borders.empty(10, 0));
        buttonPanel.add(generateButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(optimizeButton);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(BorderFactory.createTitledBorder("Output"));
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(consolePanel, BorderLayout.CENTER);
        
        // Add border
        mainPanel.setBorder(JBUI.Borders.empty(10));
    }
    
    /**
     * Generates a mod based on the entered information.
     */
    private void generateMod() {
        // Validate input
        if (modNameField.getText().trim().isEmpty()) {
            Messages.showErrorDialog(mainPanel, "Please enter a mod name", "Error");
            return;
        }
        
        if (modIdField.getText().trim().isEmpty()) {
            Messages.showErrorDialog(mainPanel, "Please enter a mod ID", "Error");
            return;
        }
        
        if (modVersionField.getText().trim().isEmpty()) {
            Messages.showErrorDialog(mainPanel, "Please enter a version", "Error");
            return;
        }
        
        // Disable buttons
        setButtonsEnabled(false);
        
        // Clear console
        consoleTextArea.setText("");
        
        // Log input
        log("Generating mod...");
        log("Mod Name: " + modNameField.getText());
        log("Mod ID: " + modIdField.getText());
        log("Version: " + modVersionField.getText());
        log("Mod Loader: " + modLoaderComboBox.getSelectedItem());
        log("Minecraft Version: " + minecraftVersionComboBox.getSelectedItem());
        log("Description: " + descriptionField.getText());
        log("");
        
        // Generate mod
        String requirements = String.format(
                "Create a Minecraft mod with the following details:\\n" +
                "Name: %s\\n" +
                "ID: %s\\n" +
                "Version: %s\\n" +
                "Mod Loader: %s\\n" +
                "Minecraft Version: %s\\n" +
                "Description: %s",
                modNameField.getText(),
                modIdField.getText(),
                modVersionField.getText(),
                modLoaderComboBox.getSelectedItem(),
                minecraftVersionComboBox.getSelectedItem(),
                descriptionField.getText()
        );
        
        CompletableFuture<AutonomousIdeCoordinatorService.ProjectCreationSummary> future = 
                coordinatorService.createProjectFromRequirements(requirements);
        
        future.thenAccept(summary -> {
            SwingUtilities.invokeLater(() -> {
                log("Mod generation completed successfully!");
                log("");
                log("Created files:");
                for (String file : summary.getCreatedFiles()) {
                    log("- " + file);
                }
                
                // Enable buttons
                setButtonsEnabled(true);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                log("Error generating mod: " + ex.getMessage());
                
                // Enable buttons
                setButtonsEnabled(true);
            });
            return null;
        });
    }
    
    /**
     * Analyzes the current project.
     */
    private void analyzeProject() {
        // Disable buttons
        setButtonsEnabled(false);
        
        // Clear console
        consoleTextArea.setText("");
        
        // Log
        log("Analyzing project...");
        log("");
        
        // Analyze project
        CompletableFuture<List<AutonomousIdeCoordinatorService.CodeIssue>> future = 
                coordinatorService.analyzeBestPractices();
        
        future.thenAccept(issues -> {
            SwingUtilities.invokeLater(() -> {
                log("Analysis completed successfully!");
                log("");
                log("Found " + issues.size() + " issues:");
                for (AutonomousIdeCoordinatorService.CodeIssue issue : issues) {
                    log("- " + issue.getFilePath() + " (line " + issue.getLine() + "): " + 
                            issue.getDescription() + " [" + issue.getIssueType() + "]");
                }
                
                // Enable buttons
                setButtonsEnabled(true);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                log("Error analyzing project: " + ex.getMessage());
                
                // Enable buttons
                setButtonsEnabled(true);
            });
            return null;
        });
    }
    
    /**
     * Optimizes the current project.
     */
    private void optimizeProject() {
        // Disable buttons
        setButtonsEnabled(false);
        
        // Clear console
        consoleTextArea.setText("");
        
        // Log
        log("Optimizing project...");
        log("");
        
        // Optimize project
        CompletableFuture<AutonomousIdeCoordinatorService.OptimizationSummary> future = 
                coordinatorService.optimizeProject();
        
        future.thenAccept(summary -> {
            SwingUtilities.invokeLater(() -> {
                log("Optimization completed successfully!");
                log("");
                log("Found " + summary.getIssuesFound() + " issues in " + summary.getFilesWithIssues() + 
                        " files (out of " + summary.getTotalFiles() + " total files)");
                log("Fixed " + summary.getIssuesFixed() + " issues");
                log("");
                log("Applied fixes:");
                for (String file : summary.getAppliedFixes().keySet()) {
                    log("- " + file + ":");
                    for (String fix : summary.getAppliedFixes().get(file)) {
                        log("  - " + fix);
                    }
                }
                
                // Enable buttons
                setButtonsEnabled(true);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                log("Error optimizing project: " + ex.getMessage());
                
                // Enable buttons
                setButtonsEnabled(true);
            });
            return null;
        });
    }
    
    /**
     * Enables or disables the buttons.
     * @param enabled Whether the buttons should be enabled
     */
    private void setButtonsEnabled(boolean enabled) {
        generateButton.setEnabled(enabled);
        analyzeButton.setEnabled(enabled);
        optimizeButton.setEnabled(enabled);
    }
    
    /**
     * Logs a message to the console.
     * @param message The message to log
     */
    private void log(String message) {
        consoleTextArea.append(message + "\n");
        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
    }
    
    /**
     * Gets the main panel.
     * @return The main panel
     */
    public JComponent getContent() {
        return mainPanel;
    }
}