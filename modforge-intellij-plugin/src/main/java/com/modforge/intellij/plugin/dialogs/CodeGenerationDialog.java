package com.modforge.intellij.plugin.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CodeGenerationDialog extends DialogWrapper {
    private final Project project;
    private final String targetPackage;
    private final String moduleType;
    private String generatedCode;
    private String description;
    private JBTextField fileNameField;
    private JBTextField descriptionField;

    public CodeGenerationDialog(@NotNull Project project, String targetPackage, String moduleType) {
        super(project);
        this.project = project;
        this.targetPackage = targetPackage;
        this.moduleType = moduleType;
        init();
        setTitle("Generate Code");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        // Main content panel with GridLayout
        JBPanel<?> contentPanel = new JBPanel<>(new GridLayout(3, 1, 5, 5));

        // Add file name field
        JBPanel<?> fileNamePanel = new JBPanel<>(new BorderLayout());
        fileNamePanel.add(new JLabel("File Name:"), BorderLayout.WEST);
        fileNameField = new JBTextField();
        fileNameField.setText("GeneratedCode.java");
        fileNamePanel.add(fileNameField, BorderLayout.CENTER);
        contentPanel.add(fileNamePanel);

        // Add description field
        JBPanel<?> descriptionPanel = new JBPanel<>(new BorderLayout());
        descriptionPanel.add(new JLabel("Description:"), BorderLayout.WEST);
        descriptionField = new JBTextField();
        descriptionPanel.add(descriptionField, BorderLayout.CENTER);
        contentPanel.add(descriptionPanel);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public void setGeneratedCode(String code) {
        this.generatedCode = code;
    }

    public String getFileName() {
        return fileNameField.getText();
    }

    public String getDescription() {
        return descriptionField.getText();
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public String getModuleType() {
        return moduleType;
    }
}
