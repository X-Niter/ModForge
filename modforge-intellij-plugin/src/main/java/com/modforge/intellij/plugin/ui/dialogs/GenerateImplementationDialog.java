package com.modforge.intellij.plugin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for getting implementation details.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class GenerateImplementationDialog extends DialogWrapper {
    private final JBTextField implementationNameField;
    private final JTextArea descriptionArea;
    private final String baseClassName;
    
    /**
     * Constructor.
     *
     * @param project      The project.
     * @param baseClassName The name of the base class.
     */
    public GenerateImplementationDialog(@Nullable Project project, @NotNull String baseClassName) {
        super(project);
        this.baseClassName = baseClassName;
        
        implementationNameField = new JBTextField(baseClassName + "Impl");
        descriptionArea = new JTextArea(5, 30);
        
        setTitle("Generate Implementation");
        init();
    }

    /**
     * Creates the dialog's center panel.
     *
     * @return The center panel.
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Name panel
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        namePanel.add(new JBLabel("Implementation Name:"), BorderLayout.NORTH);
        namePanel.add(implementationNameField, BorderLayout.CENTER);
        
        // Description panel
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JBLabel("Description (optional):"), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setPreferredSize(new Dimension(300, 100));
        descriptionPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(namePanel, BorderLayout.NORTH);
        panel.add(descriptionPanel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Validates the input.
     *
     * @return A validation info or null if valid.
     */
    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String name = getImplementationName();
        
        if (name.isEmpty()) {
            return new ValidationInfo("Implementation name cannot be empty", implementationNameField);
        }
        
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return new ValidationInfo("Implementation name must start with a valid Java identifier", implementationNameField);
        }
        
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return new ValidationInfo("Implementation name contains invalid characters", implementationNameField);
            }
        }
        
        if (name.equals(baseClassName)) {
            return new ValidationInfo("Implementation name must be different from the base class name", implementationNameField);
        }
        
        return null;
    }

    /**
     * Gets the implementation name.
     *
     * @return The implementation name.
     */
    @NotNull
    public String getImplementationName() {
        return implementationNameField.getText().trim();
    }

    /**
     * Gets the implementation description.
     *
     * @return The implementation description.
     */
    @NotNull
    public String getImplementationDescription() {
        return descriptionArea.getText().trim();
    }
}