package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for code generation input.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class CodeGenerationDialog extends DialogWrapper {
    private final JBTextArea descriptionArea;
    private final JBTextField packageField;
    private final ComboBox<String> moduleTypeComboBox;
    
    /**
     * Creates a new code generation dialog.
     *
     * @param project The project.
     * @param initialPackage The initial package.
     * @param initialModuleType The initial module type.
     */
    public CodeGenerationDialog(@Nullable Project project, @NotNull String initialPackage, @NotNull String initialModuleType) {
        super(project, true);
        
        // Initialize components
        descriptionArea = new JBTextArea();
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        packageField = new JBTextField(initialPackage);
        
        moduleTypeComboBox = new ComboBox<>(new String[]{"forge", "fabric", "quilt", "architectury"});
        moduleTypeComboBox.setSelectedItem(initialModuleType);
        
        setTitle("Generate Minecraft Mod Code");
        setOKButtonText("Generate");
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        // Description area with scroll pane
        JBScrollPane scrollPane = new JBScrollPane(descriptionArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        
        // Build the form
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Describe what mod code you want to generate:"))
                .addComponent(scrollPane)
                .addSeparator(10)
                .addLabeledComponent("Package:", packageField)
                .addLabeledComponent("Module Type:", moduleTypeComboBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setPreferredSize(new Dimension(500, 350));
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return descriptionArea;
    }
    
    /**
     * Gets the description entered by the user.
     *
     * @return The description.
     */
    public @NotNull String getDescription() {
        return descriptionArea.getText().trim();
    }
    
    /**
     * Gets the package entered by the user.
     *
     * @return The package.
     */
    public @NotNull String getTargetPackage() {
        return packageField.getText().trim();
    }
    
    /**
     * Gets the module type selected by the user.
     *
     * @return The module type.
     */
    public @NotNull String getModuleType() {
        return (String) moduleTypeComboBox.getSelectedItem();
    }
    
    @Override
    protected void doOKAction() {
        // Validate input
        if (getDescription().isEmpty()) {
            JOptionPane.showMessageDialog(
                    getContentPanel(),
                    "Please enter a description of the code to generate.",
                    "Missing Description",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        if (getTargetPackage().isEmpty()) {
            JOptionPane.showMessageDialog(
                    getContentPanel(),
                    "Please enter a package name.",
                    "Missing Package",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        super.doOKAction();
    }
}