package com.modforge.intellij.plugin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dialog for generating implementation code.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class GenerateImplementationDialog extends DialogWrapper {
    // UI components
    private JBTextField outputPathField;
    private JBTextArea promptArea;
    private ComboBox<String> modLoaderComboBox;
    private JBTextArea resultArea;
    
    // Selected values
    private String selectedModLoader;
    private String promptText;
    private String outputPath;
    
    // Services
    private final AutonomousCodeGenerationService codeGenerationService;
    
    // Constants
    private static final List<String> MOD_LOADERS = Arrays.asList("Forge", "Fabric", "Quilt", "Architectury");
    
    /**
     * Constructor.
     *
     * @param project The project.
     */
    public GenerateImplementationDialog(@NotNull Project project) {
        super(project, true);
        
        codeGenerationService = AutonomousCodeGenerationService.getInstance();
        
        setTitle("Generate Implementation");
        setOKButtonText("Generate");
        
        init();
    }

    /**
     * Creates the center panel.
     *
     * @return The center panel.
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(600, 400));
        
        // Input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = JBUI.insets(5);
        
        // Mod loader selection
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        inputPanel.add(new JBLabel("Mod Loader:"), c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        modLoaderComboBox = new ComboBox<>(MOD_LOADERS.toArray(new String[0]));
        modLoaderComboBox.setSelectedItem("Forge"); // Default selection
        inputPanel.add(modLoaderComboBox, c);
        
        // Output path
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        inputPanel.add(new JBLabel("Output Path:"), c);
        
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        outputPathField = new JBTextField();
        inputPanel.add(outputPathField, c);
        
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 0.0;
        JButton browseButton = new JButton("...");
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Select Output File");
            
            if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                outputPathField.setText(selectedFile.getAbsolutePath());
            }
        });
        inputPanel.add(browseButton, c);
        
        // Prompt
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        inputPanel.add(new JBLabel("Prompt:"), c);
        
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        promptArea = new JBTextArea();
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        JBScrollPane promptScrollPane = new JBScrollPane(promptArea);
        promptScrollPane.setPreferredSize(new Dimension(500, 100));
        inputPanel.add(promptScrollPane, c);
        
        // Result area
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.weighty = 2.0;
        resultArea = new JBTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JBScrollPane resultScrollPane = new JBScrollPane(resultArea);
        resultScrollPane.setPreferredSize(new Dimension(500, 200));
        inputPanel.add(resultScrollPane, c);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Validates the input.
     *
     * @return Validation info if validation failed, null otherwise.
     */
    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        // Check output path
        String path = outputPathField.getText().trim();
        if (path.isEmpty()) {
            return new ValidationInfo("Output path cannot be empty", outputPathField);
        }
        
        // Check if the directory exists
        File parent = Paths.get(path).getParent().toFile();
        if (!parent.exists() && !parent.mkdirs()) {
            return new ValidationInfo("Cannot create output directory", outputPathField);
        }
        
        // Check prompt
        String prompt = promptArea.getText().trim();
        if (prompt.isEmpty()) {
            return new ValidationInfo("Prompt cannot be empty", promptArea);
        }
        
        return null;
    }

    /**
     * Called when the OK button is clicked.
     */
    @Override
    protected void doOKAction() {
        // Collect input values
        selectedModLoader = (String) modLoaderComboBox.getSelectedItem();
        promptText = promptArea.getText().trim();
        outputPath = outputPathField.getText().trim();
        
        // Augment prompt with mod loader info
        String augmentedPrompt = "Using " + selectedModLoader + " mod loader, implement the following: " + promptText;
        
        // Disable OK button and show processing message
        setOKActionEnabled(false);
        resultArea.setText("Generating code...");
        
        // Call the service
        CompletableFuture<List<String>> future = codeGenerationService.generateModCode(
                getProject(),
                augmentedPrompt,
                outputPath
        );
        
        // Handle result
        future.whenComplete((results, throwable) -> {
            if (throwable != null) {
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText("Error: " + throwable.getMessage());
                    setOKActionEnabled(true);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText(String.join("\n", results));
                    setOKActionEnabled(true);
                    // Only close if successful
                    if (!results.isEmpty() && !results.get(0).startsWith("Error:")) {
                        close(OK_EXIT_CODE);
                    }
                });
            }
        });
    }

    /**
     * Gets the selected mod loader.
     *
     * @return The selected mod loader.
     */
    public String getSelectedModLoader() {
        return selectedModLoader;
    }

    /**
     * Gets the prompt text.
     *
     * @return The prompt text.
     */
    public String getPromptText() {
        return promptText;
    }

    /**
     * Gets the output path.
     *
     * @return The output path.
     */
    public String getOutputPath() {
        return outputPath;
    }
    
    /**
     * Gets the output path field component.
     *
     * @return The output path field component.
     */
    public JBTextField getOutputPathField() {
        return outputPathField;
    }
    
    /**
     * Gets the prompt area component.
     *
     * @return The prompt area component.
     */
    public JBTextArea getComponent() {
        return promptArea;
    }
}