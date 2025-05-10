package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;

/**
 * Action for generating code with AI assistance.
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            LOG.warn("Project is null");
            return;
        }
        
        try {
            // Check authentication
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to ModForge to generate code.",
                        "Authentication Required"
                );
                return;
            }
            
            // Show code generation dialog
            CodeGenerationDialog dialog = new CodeGenerationDialog(project);
            boolean proceed = dialog.showAndGet();
            
            if (!proceed) {
                // User cancelled
                return;
            }
            
            // Get input data
            String codeDescription = dialog.getCodeDescription();
            String className = dialog.getClassName();
            String fileName = dialog.getFileName();
            
            // Check if description is empty
            if (codeDescription == null || codeDescription.trim().isEmpty()) {
                Messages.showErrorDialog(
                        project,
                        "Code description cannot be empty.",
                        "Empty Description"
                );
                return;
            }
            
            // Create input data for code generation
            JSONObject inputData = new JSONObject();
            inputData.put("description", codeDescription);
            inputData.put("className", className);
            inputData.put("fileName", fileName);
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean usePatterns = settings.isPatternRecognition();
            inputData.put("usePatterns", usePatterns);
            
            // TODO: Implement code generation using API
            // This would be replaced with actual API call when implemented
            
            // Show success message
            Messages.showInfoMessage(
                    project,
                    "Code generation requested. This may take some time.",
                    "Code Generation"
            );
        } catch (Exception ex) {
            LOG.error("Error in generate code action", ex);
            
            // Show error
            Messages.showErrorDialog(
                    project,
                    "An error occurred: " + ex.getMessage(),
                    "Error"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated and project is available
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Enable if authenticated and project is available
        e.getPresentation().setEnabled(authManager.isAuthenticated() && e.getProject() != null);
    }
    
    /**
     * Dialog for entering code generation parameters.
     */
    private static class CodeGenerationDialog extends DialogWrapper {
        private final JBTextArea descriptionField;
        private final JBTextField classNameField;
        private final JBTextField fileNameField;
        
        public CodeGenerationDialog(Project project) {
            super(project, true);
            
            descriptionField = new JBTextArea(10, 50);
            descriptionField.setLineWrap(true);
            descriptionField.setWrapStyleWord(true);
            
            classNameField = new JBTextField(30);
            fileNameField = new JBTextField(30);
            
            setTitle("Generate Code");
            init();
        }
        
        @Override
        protected @Nullable ValidationInfo doValidate() {
            // Validate description
            if (descriptionField.getText().trim().isEmpty()) {
                return new ValidationInfo("Description is required", descriptionField);
            }
            
            // Validate class name if provided
            String className = classNameField.getText().trim();
            if (!className.isEmpty() && !className.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                return new ValidationInfo("Invalid class name", classNameField);
            }
            
            // Validate file name if provided
            String fileName = fileNameField.getText().trim();
            if (!fileName.isEmpty() && !fileName.matches("[A-Za-z_][A-Za-z0-9_]*\\.[A-Za-z0-9]+")) {
                return new ValidationInfo("Invalid file name (example: MyClass.java)", fileNameField);
            }
            
            return null;
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create scroll pane for description
            JBScrollPane scrollPane = new JBScrollPane(descriptionField);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            
            // Build form
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addComponent(new JBLabel("Describe the code you want to generate:"))
                    .addComponent(scrollPane)
                    .addLabeledComponent(new JBLabel("Class name (optional):"), classNameField)
                    .addLabeledComponent(new JBLabel("File name (optional):"), fileNameField)
                    .addComponentFillVertically(new JPanel(), 0);
            
            return JBUI.Panels.simplePanel().addToCenter(formBuilder.getPanel());
        }
        
        public String getCodeDescription() {
            return descriptionField.getText();
        }
        
        public String getClassName() {
            return classNameField.getText().trim();
        }
        
        public String getFileName() {
            return fileNameField.getText().trim();
        }
    }
}