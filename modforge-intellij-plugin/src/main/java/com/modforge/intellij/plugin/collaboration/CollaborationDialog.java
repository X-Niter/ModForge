package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CollaborationDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CollaborationDialog.class);

    private final Project project;
    private JBTextField sessionNameField;
    private JBTextField descriptionField;
    private JBTextField usernameField;
    private JBTextField sessionIdField;
    private JCheckBox privateSessionCheckbox;
    private JBRadioButton startSessionRadio;
    private JBRadioButton joinSessionRadio;
    private ButtonGroup buttonGroup;
    private boolean isStartNewSession;

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

        // When joining a session, validate session ID format
        if (!startSessionRadio.isSelected()) {
            String sessionId = sessionIdField.getText().trim();
            if (sessionId.isEmpty()) {
                return new ValidationInfo("Please enter a session ID", sessionIdField);
            }
            if (!sessionId.matches("^[a-zA-Z0-9_-]{4,}$")) {
                return new ValidationInfo("Invalid session ID format", sessionIdField);
            }
            return null;
        }

        // When starting a session, validate session name
        String sessionName = sessionNameField.getText().trim();
        if (sessionName.isEmpty()) {
            return new ValidationInfo("Please enter a session name", sessionNameField);
        }
        if (sessionName.length() < 3) {
            return new ValidationInfo("Session name must be at least 3 characters", sessionNameField);
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
        sessionNameField.setToolTipText("Name of the new collaboration session");

        descriptionField = new JBTextField();
        descriptionField.setToolTipText("Brief description of the session purpose");

        sessionIdField = new JBTextField();
        sessionIdField.setToolTipText("ID of the session to join");
        sessionIdField.setEnabled(false); // Initially disabled as we start in "new session" mode

        // Session options
        buttonGroup = new ButtonGroup();
        startSessionRadio = new JBRadioButton("Start new session");
        joinSessionRadio = new JBRadioButton("Join existing session");

        buttonGroup.add(startSessionRadio);
        buttonGroup.add(joinSessionRadio);
        startSessionRadio.setSelected(true);

        privateSessionCheckbox = new JCheckBox("Private Session");
        privateSessionCheckbox.setToolTipText("Only invited users can join this session");
        privateSessionCheckbox.setSelected(true);

        // Layout components
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(startSessionRadio);
        optionsPanel.add(joinSessionRadio);

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Your Name:"), usernameField, 1, false)
                .addSeparator()
                .addComponent(optionsPanel)
                .addLabeledComponent(new JBLabel("Session Name:"), sessionNameField, 1, false)
                .addLabeledComponent(new JBLabel("Session ID:"), sessionIdField, 1, false)
                .addLabeledComponent(new JBLabel("Description:"), descriptionField, 1, false)
                .addComponent(privateSessionCheckbox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        // Add listeners for radio buttons to update UI state
        startSessionRadio.addItemListener(e -> updateUIForSessionType());
        joinSessionRadio.addItemListener(e -> updateUIForSessionType());

        panel.setPreferredSize(new Dimension(450, 250));
        panel.setBorder(JBUI.Borders.empty(10));

        return panel;
    }

    private void updateUIForSessionType() {
        boolean isStartingSession = startSessionRadio.isSelected();
        sessionNameField.setEnabled(isStartingSession);
        sessionIdField.setEnabled(!isStartingSession);
        privateSessionCheckbox.setEnabled(isStartingSession);

        if (isStartingSession) {
            sessionIdField.setText("");
        } else {
            sessionNameField.setText("");
        }
    }

    @Override
    protected void init() {
        super.init();

        // Call the method to set up initial UI state
        updateUIForSessionType();

        startSessionRadio.addItemListener(e -> updateUIForSessionType());
        joinSessionRadio.addItemListener(e -> updateUIForSessionType());
    }

    @Override
    protected void doOKAction() {
        // Save username to settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings != null) {
            settings.setUsername(getUsername());
        }

        super.doOKAction();
    }

    /**
     * Gets if this is a new session being started.
     *
     * @return true if starting a new session, false if joining
     */
    public boolean isStartNewSession() {
        return isStartNewSession;
    }

    /**
     * Checks if starting a new session was selected.
     * 
     * @return true if starting a new session, false if joining
     */
    public boolean isStartingNewSession() {
        return startSessionRadio.isSelected();
    }

    /**
     * Gets the session name/ID.
     * 
     * @return the session name or ID
     */
    public String getSessionName() {
        return sessionNameField.getText().trim();
    }

    /**
     * Gets the session ID for joining.
     * 
     * @return the session ID
     */
    public String getSessionId() {
        return sessionIdField.getText().trim();
    }

    /**
     * Gets the session description.
     *
     * @return The session description
     */
    @NotNull
    public String getDescription() {
        return descriptionField.getText().trim();
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
     * Checks if this is a private session.
     *
     * @return true if private, false if public
     */
    public boolean isPrivateSession() {
        return privateSessionCheckbox.isSelected();
    }
}