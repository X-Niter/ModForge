package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.modforge.intellij.plugin.utils.CompatibilityUtil.JBRadioButton;
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
    private JBTextField usernameField;
    private JCheckBox privateSessionCheckbox;
    private JBRadioButton startSessionRadio;
    private JBRadioButton joinSessionRadio;
    
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
        if (usernameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Please enter your name", usernameField);
        }
        
        if (sessionNameField.getText().trim().isEmpty()) {
            String message = isStartSession() 
                ? "Please enter a session name" 
                : "Please enter the session ID";
            return new ValidationInfo(message, sessionNameField);
        }
        
        if (isStartSession() && sessionNameField.getText().trim().length() < 3) {
            return new ValidationInfo("Session name must be at least 3 characters", sessionNameField);
        }
        
        // If joining, validate that the session ID looks correct
        if (!isStartSession()) {
            String sessionId = sessionNameField.getText().trim();
            if (!sessionId.matches("^[a-zA-Z0-9_-]{4,}$")) {
                return new ValidationInfo("Invalid session ID format", sessionNameField);
            }
        }
        
        return null;
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        // User info
        usernameField = new JBTextField();
        usernameField.setToolTipText("Your display name in the collaboration session");
        
        // Try to get username from settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings != null && settings.getUsername() != null && !settings.getUsername().isEmpty()) {
            usernameField.setText(settings.getUsername());
        }
        
        // Session info
        sessionNameField = new JBTextField();
        sessionNameField.setToolTipText("Name of the collaboration session");
        
        descriptionField = new JBTextField();
        descriptionField.setToolTipText("Brief description of the session purpose");
        
        // Session options
        ButtonGroup sessionGroup = new ButtonGroup();
        startSessionRadio = new JBRadioButton("Start new session");
        joinSessionRadio = new JBRadioButton("Join existing session");
        
        sessionGroup.add(startSessionRadio);
        sessionGroup.add(joinSessionRadio);
        startSessionRadio.setSelected(true);
        
        privateSessionCheckbox = new JCheckBox("Private Session");
        privateSessionCheckbox.setToolTipText("Only invited users can join this session");
        privateSessionCheckbox.setSelected(true);
        
        // Layout
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(startSessionRadio);
        optionsPanel.add(joinSessionRadio);
        
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Your Name:"), usernameField, 1, false)
                .addSeparator()
                .addComponent(optionsPanel)
                .addLabeledComponent(new JBLabel("Session Name:"), sessionNameField, 1, false)
                .addLabeledComponent(new JBLabel("Description:"), descriptionField, 1, false)
                .addComponent(privateSessionCheckbox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Update UI based on selected option
        startSessionRadio.addActionListener(e -> updateUIForSessionType());
        joinSessionRadio.addActionListener(e -> updateUIForSessionType());
        
        panel.setPreferredSize(new Dimension(450, 220));
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    /**
     * Updates UI components based on whether starting or joining a session.
     */
    private void updateUIForSessionType() {
        boolean startingSession = startSessionRadio.isSelected();
        
        descriptionField.setEnabled(startingSession);
        privateSessionCheckbox.setEnabled(startingSession);
        
        if (!startingSession) {
            sessionNameField.setToolTipText("Enter the session ID to join");
        } else {
            sessionNameField.setToolTipText("Name of the new collaboration session");
        }
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
     * Gets the session ID entered by the user.
     * This is the same as the session name but used when joining an existing session.
     *
     * @return The session ID
     */
    @NotNull
    public String getSessionId() {
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
    
    /**
     * Gets the username entered by the user.
     *
     * @return The username
     */
    @NotNull
    public String getUsername() {
        return usernameField.getText().trim();
    }
    
    /**
     * Determines if the user wants to start a new session.
     *
     * @return True if starting a new session, false if joining an existing one
     */
    public boolean isStartSession() {
        return startSessionRadio.isSelected();
    }
}