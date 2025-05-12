package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for starting a collaboration session.
 */
public class CollaborationDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CollaborationDialog.class);
    
    private final Project project;
    private JBTextField sessionNameField;
    private JBTextField descriptionField;
    private JCheckBox privateSessionCheckbox;
    
    /**
     * Creates a new collaboration dialog.
     *
     * @param project The project
     */
    public CollaborationDialog(@NotNull Project project) {
        super(project, true);
        this.project = project;
        
        setTitle("Start Collaboration Session");
        init();
        
        LOG.info("CollaborationDialog initialized for project: " + project.getName());
    }
    
    /**
     * Gets the project associated with this dialog.
     * 
     * @return The project
     */
    @NotNull
    public Project getProject() {
        return project;
    }
    
    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (sessionNameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Session name is required", sessionNameField);
        }
        
        return null;
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        sessionNameField = new JBTextField();
        sessionNameField.setToolTipText("Name of the collaboration session");
        
        descriptionField = new JBTextField();
        descriptionField.setToolTipText("Brief description of the session purpose");
        
        privateSessionCheckbox = new JCheckBox("Private Session");
        privateSessionCheckbox.setToolTipText("Only invited users can join this session");
        privateSessionCheckbox.setSelected(true);
        
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Session Name:"), sessionNameField, 1, false)
                .addLabeledComponent(new JBLabel("Description:"), descriptionField, 1, false)
                .addComponent(privateSessionCheckbox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setPreferredSize(new Dimension(400, 150));
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    /**
     * Gets the session name entered by the user.
     *
     * @return The session name
     */
    @NotNull
    public String getSessionName() {
        return sessionNameField.getText().trim();
    }
    
    /**
     * Gets the session description entered by the user.
     *
     * @return The session description
     */
    @NotNull
    public String getSessionDescription() {
        return descriptionField.getText().trim();
    }
    
    /**
     * Gets whether the session is private.
     *
     * @return True if private, false otherwise
     */
    public boolean isPrivateSession() {
        return privateSessionCheckbox.isSelected();
    }
}