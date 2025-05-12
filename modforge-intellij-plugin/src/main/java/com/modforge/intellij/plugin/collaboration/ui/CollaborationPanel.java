package com.modforge.intellij.plugin.collaboration.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.collaboration.CollaborationListener;
import com.modforge.intellij.plugin.collaboration.CollaborationService;
import com.modforge.intellij.plugin.collaboration.EditorOperation;
import com.modforge.intellij.plugin.collaboration.Participant;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for collaboration features.
 */
public class CollaborationPanel extends JBPanel<CollaborationPanel> implements Disposable, CollaborationListener {
    private static final Logger LOG = Logger.getInstance(CollaborationPanel.class);
    
    // The project
    private final Project project;
    
    // The collaboration service
    private final CollaborationService collaborationService;
    
    // UI components
    private JPanel sessionControlPanel;
    private JPanel connectionStatusPanel;
    private JPanel participantsPanel;
    private JBTextField usernameField;
    private JBTextField sessionIdField;
    private JButton startSessionButton;
    private JButton joinSessionButton;
    private JButton leaveSessionButton;
    private JBList<Participant> participantsList;
    private DefaultListModel<Participant> participantsListModel;
    private JBLabel statusLabel;
    
    /**
     * Creates a new CollaborationPanel.
     * @param project The project
     */
    public CollaborationPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.collaborationService = CollaborationService.getInstance(project);
        
        createUI();
        updateUI();
        
        // Register as a collaboration listener
        collaborationService.addListener(this);
        
        // Register for disposal
        Disposer.register(project, this);
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        // Create connection panel
        JPanel connectionPanel = new JBPanel<>(new BorderLayout());
        connectionPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create session control panel
        sessionControlPanel = createSessionControlPanel();
        connectionPanel.add(sessionControlPanel, BorderLayout.NORTH);
        
        // Create connection status panel
        connectionStatusPanel = createConnectionStatusPanel();
        connectionPanel.add(connectionStatusPanel, BorderLayout.CENTER);
        
        // Create participants panel
        participantsPanel = createParticipantsPanel();
        
        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, connectionPanel, participantsPanel);
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);
        
        // Add title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        JLabel titleLabel = new JLabel("Real-Time Collaboration");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);
        
        // Add info text at the bottom
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        JLabel infoLabel = new JLabel(
            "<html><body style='width: 400px'>Real-time collaboration allows multiple developers to work on the same " +
            "mod project simultaneously. Start a session and share the session ID with your team members.</body></html>"
        );
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Creates the session control panel.
     * @return The session control panel
     */
    @NotNull
    private JPanel createSessionControlPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Session Control"));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Username
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        usernameField = new JBTextField("User" + (int)(Math.random() * 1000));
        panel.add(usernameField, c);
        
        // Session ID
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        panel.add(new JBLabel("Session ID:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        sessionIdField = new JBTextField();
        sessionIdField.setEnabled(false);
        panel.add(sessionIdField, c);
        
        // Buttons
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1.0;
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER));
        
        startSessionButton = new JButton("Start Session");
        startSessionButton.addActionListener(this::startSession);
        buttonPanel.add(startSessionButton);
        
        joinSessionButton = new JButton("Join Session");
        joinSessionButton.addActionListener(this::joinSession);
        buttonPanel.add(joinSessionButton);
        
        leaveSessionButton = new JButton("Leave Session");
        leaveSessionButton.addActionListener(this::leaveSession);
        leaveSessionButton.setEnabled(false);
        buttonPanel.add(leaveSessionButton);
        
        panel.add(buttonPanel, c);
        
        return panel;
    }
    
    /**
     * Creates the connection status panel.
     * @return The connection status panel
     */
    @NotNull
    private JPanel createConnectionStatusPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Connection Status"));
        
        statusLabel = new JBLabel("Not connected to any session.");
        statusLabel.setBorder(JBUI.Borders.empty(10));
        panel.add(statusLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the participants panel.
     * @return The participants panel
     */
    @NotNull
    private JPanel createParticipantsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Participants"));
        
        // Create list model and list
        participantsListModel = new DefaultListModel<>();
        participantsList = new JBList<>(participantsListModel);
        participantsList.setCellRenderer(new ParticipantListCellRenderer());
        
        // Add list to scroll pane
        JBScrollPane scrollPane = new JBScrollPane(participantsList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Updates the UI based on the collaboration service state.
     */
    private void updateUI() {
        boolean isConnected = collaborationService.isConnected();
        boolean isHost = collaborationService.isHost();
        
        // Update session control
        usernameField.setEnabled(!isConnected);
        sessionIdField.setText(collaborationService.getSessionId());
        
        startSessionButton.setEnabled(!isConnected);
        joinSessionButton.setEnabled(!isConnected);
        leaveSessionButton.setEnabled(isConnected);
        
        // Update status label
        if (isConnected) {
            String sessionId = collaborationService.getSessionId();
            String role = isHost ? "host" : "participant";
            statusLabel.setText(String.format(
                    "Connected to session %s as %s (%s).",
                    sessionId,
                    collaborationService.getUsername(),
                    role
            ));
            statusLabel.setForeground(JBColor.GREEN);
        } else {
            statusLabel.setText("Not connected to any session.");
            statusLabel.setForeground(JBColor.GRAY);
        }
        
        // Update participants list
        updateParticipantsList();
    }
    
    /**
     * Updates the participants list.
     */
    private void updateParticipantsList() {
        List<Participant> participants = collaborationService.isConnected()
                ? collaborationService.getParticipants()
                : Collections.emptyList();
        
        participantsListModel.clear();
        for (Participant participant : participants) {
            participantsListModel.addElement(participant);
        }
    }
    
    /**
     * Starts a session.
     * @param e The action event
     */
    private void startSession(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            Messages.showErrorDialog(this, "Please enter a username.", "Error");
            return;
        }
        
        CompletableFuture<String> future = collaborationService.startSession(username);
        
        future.thenAccept(sessionId -> {
            SwingUtilities.invokeLater(() -> {
                sessionIdField.setText(sessionId);
                updateUI();
                
                // Show session ID dialog
                Messages.showInfoDialog(
                        "Session started with ID: " + sessionId + "\n\n" +
                        "Share this ID with your team members so they can join.",
                        "Session Started"
                );
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                Messages.showErrorDialog(
                        "Failed to start session: " + ex.getMessage(),
                        "Error"
                );
            });
            return null;
        });
    }
    
    /**
     * Joins a session.
     * @param e The action event
     */
    private void joinSession(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            Messages.showErrorDialog(this, "Please enter a username.", "Error");
            return;
        }
        
        String sessionId = Messages.showInputDialog(
                this,
                "Enter the session ID to join:",
                "Join Session",
                Messages.getQuestionIcon()
        );
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        
        CompletableFuture<Boolean> future = collaborationService.joinSession(sessionId.trim(), username);
        
        future.thenAccept(success -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    updateUI();
                    Messages.showInfoDialog("Successfully joined session.", "Success");
                } else {
                    Messages.showErrorDialog("Failed to join session.", "Error");
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                Messages.showErrorDialog(
                        "Failed to join session: " + ex.getMessage(),
                        "Error"
                );
            });
            return null;
        });
    }
    
    /**
     * Leaves a session.
     * @param e The action event
     */
    private void leaveSession(ActionEvent e) {
        CompletableFuture<Boolean> future = collaborationService.leaveSession();
        
        future.thenAccept(success -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    updateUI();
                    Messages.showInfoDialog("Successfully left session.", "Success");
                } else {
                    Messages.showErrorDialog("Failed to leave session.", "Error");
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                Messages.showErrorDialog(
                        "Failed to leave session: " + ex.getMessage(),
                        "Error"
                );
            });
            return null;
        });
    }
    
    /**
     * Cell renderer for participants.
     */
    private static class ParticipantListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Participant) {
                Participant participant = (Participant) value;
                setText(participant.toString());
                
                // Set icon based on whether the participant is the host
                if (participant.isHost) {
                    setIcon(UIUtil.getTreeNodeIcon(true, true, false));
                } else {
                    setIcon(UIUtil.getTreeNodeIcon(false, false, false));
                }
            }
            
            return component;
        }
    }
    
    // CollaborationListener methods
    
    @Override
    public void onSessionStarted(@NotNull String sessionId) {
        SwingUtilities.invokeLater(this::updateUI);
    }
    
    @Override
    public void onSessionJoined(@NotNull String sessionId) {
        SwingUtilities.invokeLater(this::updateUI);
    }
    
    @Override
    public void onSessionLeft(@NotNull String sessionId) {
        SwingUtilities.invokeLater(this::updateUI);
    }
    
    @Override
    public void onParticipantJoined(@NotNull Participant participant) {
        SwingUtilities.invokeLater(this::updateParticipantsList);
    }
    
    @Override
    public void onParticipantLeft(@NotNull Participant participant) {
        SwingUtilities.invokeLater(this::updateParticipantsList);
    }
    
    @Override
    public void onOperationReceived(@NotNull EditorOperation operation, @NotNull Participant participant) {
        // Nothing to do here
    }
    
    /**
     * Disposes the panel.
     */
    @Override
    public void dispose() {
        collaborationService.removeListener(this);
    }
    
    /**
     * Gets the content component.
     * @return The content component
     */
    public JComponent getContent() {
        return this;
    }
}