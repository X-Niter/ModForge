package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.generation.MinecraftCodeGenerator;
import com.modforge.intellij.plugin.notifications.ModForgeNotificationService;
import com.modforge.intellij.plugin.util.ModLoaderDetector.ModLoader;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Action for generating Minecraft code from a description
 * Provides a dialog for inputting code description and parameters
 */
public class GenerateMinecraftCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateMinecraftCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        // Get the currently selected directory or default to project root
        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedDir == null || !selectedDir.isDirectory()) {
            selectedDir = CompatibilityUtil.getProjectBaseDir(project);
        }
        
        // Get default target directory
        String defaultTargetDirectory = selectedDir.getPath();
        
        // Show dialog to get code generation parameters
        CodeGenerationDialog dialog = new CodeGenerationDialog(project, defaultTargetDirectory);
        if (!dialog.showAndGet()) {
            // User cancelled
            return;
        }
        
        // Get the parameters from the dialog
        String description = dialog.getDescription();
        String targetDirectory = dialog.getTargetDirectory();
        String loaderHint = dialog.getSelectedLoader();
        
        // Get the code generator service
        MinecraftCodeGenerator codeGenerator = project.getService(MinecraftCodeGenerator.class);
        if (codeGenerator == null) {
            Messages.showErrorDialog(project, "Code generator service not available", "Error");
            return;
        }
        
        // Generate the code
        codeGenerator.generateCode(description, targetDirectory, loaderHint)
            .thenAccept(generatedCode -> {
                // Show success notification
                SwingUtilities.invokeLater(() -> {
                    ModForgeNotificationService notificationService = project.getService(ModForgeNotificationService.class);
                    if (notificationService != null) {
                        String message = "Generated " + generatedCode.getCodeType().name().toLowerCase() + 
                                       " code for " + generatedCode.getClassName() + " at " + 
                                       generatedCode.getFilePath();
                        
                        notificationService.showInfo("Code Generation Success", message);
                    }
                });
            })
            .exceptionally(ex -> {
                // Show error notification
                SwingUtilities.invokeLater(() -> {
                    String errorMessage = "Code generation failed: " + ex.getMessage();
                    LOG.error(errorMessage, ex);
                    
                    ModForgeNotificationService notificationService = project.getService(ModForgeNotificationService.class);
                    if (notificationService != null) {
                        notificationService.showError("Code Generation Error", errorMessage);
                    }
                });
                
                return null;
            });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only if we have a project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
    
    /**
     * Dialog for getting code generation parameters
     */
    private static class CodeGenerationDialog extends DialogWrapper {
        private final Project project;
        private final JBTextArea descriptionTextArea;
        private final JBTextField targetDirectoryField;
        private final JComboBox<String> loaderComboBox;
        
        public CodeGenerationDialog(Project project, String defaultTargetDirectory) {
            super(project);
            this.project = project;
            
            // Create UI components
            descriptionTextArea = new JBTextArea(10, 50);
            descriptionTextArea.setLineWrap(true);
            descriptionTextArea.setWrapStyleWord(true);
            descriptionTextArea.setFont(UIUtil.getLabelFont());
            
            targetDirectoryField = new JBTextField(defaultTargetDirectory);
            
            loaderComboBox = new JComboBox<>(
                    Arrays.stream(ModLoader.values())
                          .filter(loader -> loader != ModLoader.UNKNOWN)
                          .map(ModLoader::getDisplayName)
                          .toArray(String[]::new));
            
            // Set title
            setTitle("Generate Minecraft Code");
            
            // Initialize dialog
            init();
        }
        
        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            // Create form layout
            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.setBorder(JBUI.Borders.empty(10));
            
            // Description section
            JPanel descriptionPanel = new JPanel(new BorderLayout());
            descriptionPanel.add(new JBLabel("Enter a description of the code to generate:"), BorderLayout.NORTH);
            descriptionPanel.add(new JBScrollPane(descriptionTextArea), BorderLayout.CENTER);
            
            // Target directory section
            JPanel targetDirPanel = new JPanel(new BorderLayout());
            targetDirPanel.add(new JBLabel("Target directory:"), BorderLayout.WEST);
            targetDirPanel.add(targetDirectoryField, BorderLayout.CENTER);
            
            JButton browseButton = new JButton("Browse...");
            browseButton.addActionListener(e -> browseForTargetDirectory());
            targetDirPanel.add(browseButton, BorderLayout.EAST);
            
            // Mod loader section
            JPanel loaderPanel = new JPanel(new BorderLayout());
            loaderPanel.add(new JBLabel("Mod Loader:"), BorderLayout.WEST);
            loaderPanel.add(loaderComboBox, BorderLayout.CENTER);
            
            // Add all sections to main panel
            JPanel optionsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
            optionsPanel.add(targetDirPanel);
            optionsPanel.add(loaderPanel);
            
            panel.add(descriptionPanel, BorderLayout.CENTER);
            panel.add(optionsPanel, BorderLayout.SOUTH);
            
            // Add a tip at the bottom
            JPanel tipPanel = new JPanel(new BorderLayout());
            JBLabel tipLabel = new JBLabel("Tip: Describe the code in detail for better results. Include type, properties, and behavior.");
            tipLabel.setForeground(JBColor.GRAY);
            tipPanel.add(tipLabel, BorderLayout.CENTER);
            
            panel.add(tipPanel, BorderLayout.NORTH);
            
            return panel;
        }
        
        /**
         * Browse for a target directory
         */
        private void browseForTargetDirectory() {
            JFileChooser fileChooser = new JFileChooser(targetDirectoryField.getText());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Select Target Directory");
            
            if (fileChooser.showOpenDialog(getContentPanel()) == JFileChooser.APPROVE_OPTION) {
                targetDirectoryField.setText(fileChooser.getSelectedFile().getPath());
            }
        }
        
        @Nullable
        @Override
        protected ValidationInfo doValidate() {
            if (descriptionTextArea.getText().trim().isEmpty()) {
                return new ValidationInfo("Please enter a description", descriptionTextArea);
            }
            
            if (targetDirectoryField.getText().trim().isEmpty()) {
                return new ValidationInfo("Please select a target directory", targetDirectoryField);
            }
            
            return null;
        }
        
        public String getDescription() {
            return descriptionTextArea.getText().trim();
        }
        
        public String getTargetDirectory() {
            return targetDirectoryField.getText().trim();
        }
        
        public String getSelectedLoader() {
            return (String) loaderComboBox.getSelectedItem();
        }
    }
}