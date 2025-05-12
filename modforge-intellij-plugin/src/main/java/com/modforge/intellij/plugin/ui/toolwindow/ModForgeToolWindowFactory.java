package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.GitHubIntegrationService;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for creating the ModForge tool window.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ModForgeToolWindowFactory implements ToolWindowFactory {
    
    /**
     * Creates the tool window.
     *
     * @param project     The project.
     * @param toolWindow  The tool window.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // Create content for the Dashboard tab
        JComponent dashboardPanel = createDashboardPanel(project);
        Content dashboardContent = contentFactory.createContent(dashboardPanel, "Dashboard", false);
        
        // Create content for the Logs tab
        JComponent logsPanel = createLogsPanel(project);
        Content logsContent = contentFactory.createContent(logsPanel, "Logs", false);
        
        // Create content for the Settings tab
        JComponent settingsPanel = createSettingsPanel(project);
        Content settingsContent = contentFactory.createContent(settingsPanel, "Settings", false);
        
        // Add all content to the tool window
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(dashboardContent);
        contentManager.addContent(logsContent);
        contentManager.addContent(settingsContent);
    }

    /**
     * Creates the dashboard panel.
     *
     * @param project The project.
     * @return The dashboard panel.
     */
    private JComponent createDashboardPanel(@NotNull Project project) {
        ModAuthenticationManager authManager = ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
        GitHubIntegrationService gitHubService = ApplicationManager.getApplication().getService(GitHubIntegrationService.class);
        AutonomousCodeGenerationService codeGenService = ApplicationManager.getApplication().getService(AutonomousCodeGenerationService.class);
        
        // Create dashboard panel with sections
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        
        // Status section
        JPanel statusPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        statusPanel.setBorder(JBUI.Borders.empty(10));
        
        statusPanel.add(new JBLabel("Authentication:"));
        JLabel authStatusLabel = new JBLabel(authManager.isAuthenticated() ? "Authenticated" : "Not authenticated");
        authStatusLabel.setForeground(authManager.isAuthenticated() ? JBColor.GREEN : JBColor.RED);
        statusPanel.add(authStatusLabel);
        
        statusPanel.add(new JBLabel("GitHub Integration:"));
        JLabel githubStatusLabel = new JBLabel(gitHubService.isAuthenticated() ? "Connected" : "Not connected");
        githubStatusLabel.setForeground(gitHubService.isAuthenticated() ? JBColor.GREEN : JBColor.RED);
        statusPanel.add(githubStatusLabel);
        
        statusPanel.add(new JBLabel("Continuous Development:"));
        JLabel cdStatusLabel = new JBLabel(codeGenService.isRunningContinuously() ? "Running" : "Stopped");
        cdStatusLabel.setForeground(codeGenService.isRunningContinuously() ? JBColor.GREEN : JBColor.ORANGE);
        statusPanel.add(cdStatusLabel);
        
        // Quick actions section
        JPanel actionsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        actionsPanel.setBorder(JBUI.Borders.empty(10));
        
        JButton generateCodeButton = new JButton("Generate Code");
        generateCodeButton.addActionListener(e -> {
            // This would open the generate code dialog
            // For now, just show a message
            showMessage(project, "Generate Code", "This would open the generate code dialog");
        });
        actionsPanel.add(generateCodeButton);
        
        JButton fixErrorsButton = new JButton("Fix Errors");
        fixErrorsButton.addActionListener(e -> {
            // This would open the fix errors dialog
            // For now, just show a message
            showMessage(project, "Fix Errors", "This would open the fix errors dialog");
        });
        actionsPanel.add(fixErrorsButton);
        
        JButton toggleCDButton = new JButton(codeGenService.isRunningContinuously() ? "Stop Continuous Development" : "Start Continuous Development");
        toggleCDButton.addActionListener(e -> {
            if (codeGenService.isRunningContinuously()) {
                codeGenService.stopContinuousDevelopment();
                toggleCDButton.setText("Start Continuous Development");
                cdStatusLabel.setText("Stopped");
                cdStatusLabel.setForeground(JBColor.ORANGE);
            } else {
                codeGenService.startContinuousDevelopment(project);
                toggleCDButton.setText("Stop Continuous Development");
                cdStatusLabel.setText("Running");
                cdStatusLabel.setForeground(JBColor.GREEN);
            }
        });
        actionsPanel.add(toggleCDButton);
        
        // Pattern learning stats
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(JBUI.Borders.empty(10));
        
        JTextArea statsArea = new JTextArea(codeGenService.getPatternLearningStats());
        statsArea.setEditable(false);
        statsArea.setBackground(null);
        statsArea.setBorder(null);
        statsPanel.add(statsArea, BorderLayout.CENTER);
        
        JButton clearCacheButton = new JButton("Clear Pattern Cache");
        clearCacheButton.addActionListener(e -> {
            codeGenService.clearPatternCache();
            statsArea.setText(codeGenService.getPatternLearningStats());
        });
        statsPanel.add(clearCacheButton, BorderLayout.SOUTH);
        
        // Put all sections together
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(statusPanel, BorderLayout.NORTH);
        northPanel.add(actionsPanel, BorderLayout.CENTER);
        
        dashboardPanel.add(northPanel, BorderLayout.NORTH);
        dashboardPanel.add(statsPanel, BorderLayout.CENTER);
        
        return dashboardPanel;
    }

    /**
     * Creates the logs panel.
     *
     * @param project The project.
     * @return The logs panel.
     */
    private JComponent createLogsPanel(@NotNull Project project) {
        // Create logs panel
        JPanel logsPanel = new JPanel(new BorderLayout());
        
        JTextArea logsArea = new JTextArea();
        logsArea.setEditable(false);
        logsArea.setText("Logs will be displayed here.");
        
        JScrollPane scrollPane = new JBScrollPane(logsArea);
        logsPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> logsArea.setText(""));
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            // This would refresh the logs
            // For now, just append a message
            logsArea.append("\nLogs refreshed at " + new java.util.Date());
        });
        
        controlPanel.add(clearButton);
        controlPanel.add(refreshButton);
        
        logsPanel.add(controlPanel, BorderLayout.SOUTH);
        
        return logsPanel;
    }

    /**
     * Creates the settings panel.
     *
     * @param project The project.
     * @return The settings panel.
     */
    private JComponent createSettingsPanel(@NotNull Project project) {
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Create settings panel
        JPanel settingsPanel = new JPanel(new BorderLayout());
        
        // Server settings
        JPanel serverPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        serverPanel.setBorder(BorderFactory.createTitledBorder("Server Settings"));
        
        serverPanel.add(new JBLabel("Server URL:"));
        JTextField serverUrlField = new JTextField(settings.getServerUrl());
        serverPanel.add(serverUrlField);
        
        serverPanel.add(new JBLabel("Request Timeout (seconds):"));
        SpinnerNumberModel timeoutModel = new SpinnerNumberModel(
                settings.getRequestTimeout(),
                1,
                300,
                1
        );
        JSpinner timeoutSpinner = new JSpinner(timeoutModel);
        serverPanel.add(timeoutSpinner);
        
        // Feature settings
        JPanel featurePanel = new JPanel(new GridLayout(0, 1, 0, 5));
        featurePanel.setBorder(BorderFactory.createTitledBorder("Features"));
        
        JCheckBox patternRecognitionCheckBox = new JCheckBox("Enable Pattern Recognition", settings.isPatternRecognition());
        featurePanel.add(patternRecognitionCheckBox);
        
        JCheckBox continuousDevelopmentCheckBox = new JCheckBox("Enable Continuous Development", settings.isEnableContinuousDevelopment());
        featurePanel.add(continuousDevelopmentCheckBox);
        
        // GitHub settings
        JPanel githubPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        githubPanel.setBorder(BorderFactory.createTitledBorder("GitHub Integration"));
        
        githubPanel.add(new JBLabel("GitHub Username:"));
        JTextField usernameField = new JTextField(settings.getGitHubUsername());
        githubPanel.add(usernameField);
        
        githubPanel.add(new JBLabel("Access Token:"));
        JPasswordField tokenField = new JPasswordField(settings.getAccessToken());
        githubPanel.add(tokenField);
        
        // Add panels to settings panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(serverPanel, BorderLayout.NORTH);
        topPanel.add(featurePanel, BorderLayout.CENTER);
        
        settingsPanel.add(topPanel, BorderLayout.NORTH);
        settingsPanel.add(githubPanel, BorderLayout.CENTER);
        
        // Add save button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            // Save settings
            settings.setServerUrl(serverUrlField.getText());
            settings.setRequestTimeout((int) timeoutSpinner.getValue());
            settings.setPatternRecognition(patternRecognitionCheckBox.isSelected());
            settings.setEnableContinuousDevelopment(continuousDevelopmentCheckBox.isSelected());
            settings.setGitHubUsername(usernameField.getText());
            settings.setAccessToken(new String(tokenField.getPassword()));
            
            // Show message
            showMessage(project, "Settings Saved", "Your settings have been saved");
        });
        buttonPanel.add(saveButton);
        
        settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return new JBScrollPane(settingsPanel);
    }

    /**
     * Shows a message dialog.
     *
     * @param project The project.
     * @param title   The title.
     * @param message The message.
     */
    private void showMessage(@NotNull Project project, @NotNull String title, @NotNull String message) {
        JOptionPane.showMessageDialog(
                ToolWindowManager.getInstance(project).getToolWindow("ModForge").getComponent(),
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}