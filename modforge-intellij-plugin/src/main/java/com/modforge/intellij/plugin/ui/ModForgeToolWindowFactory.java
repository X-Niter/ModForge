package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.modforge.intellij.plugin.actions.FixErrorsAction;
import com.modforge.intellij.plugin.actions.GenerateCodeAction;
import com.modforge.intellij.plugin.actions.ToggleContinuousDevelopmentAction;
import com.modforge.intellij.plugin.actions.TogglePatternRecognitionAction;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowFactory.class);
    
    private JPanel mainPanel;
    private JButton loginButton;
    private JButton logoutButton;
    private JButton generateCodeButton;
    private JButton fixErrorsButton;
    private JButton toggleContinuousDevButton;
    private JButton togglePatternRecogButton;
    private JTextArea statusText;
    private JLabel authStatusLabel;
    private JLabel continuousDevStatusLabel;
    private JLabel patternRecogStatusLabel;
    private Timer updateTimer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        BorderLayoutPanel panel = new BorderLayoutPanel();
        panel.add(createContent(project), BorderLayout.CENTER);
        
        Content content = com.modforge.intellij.plugin.utils.CompatibilityUtil.getCompatibleContentFactory()
                .createContent(panel, "ModForge", false);
        toolWindow.getContentManager().addContent(content);
        
        // Start update timer
        updateTimer = new Timer(5000, e -> updateStatus(project));
        updateTimer.start();
        
        // Update status initially
        updateStatus(project);
        
        // Register for project closing using ProjectManagerListener
        ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
            @Override
            public void projectClosing(@NotNull Project closingProject) {
                if (updateTimer != null && updateTimer.isRunning()) {
                    updateTimer.stop();
                }
            }
        });
        
        initialized.set(true);
    }
    
    /**
     * Create the content panel.
     *
     * @param project The project
     * @return The content panel
     */
    private JPanel createContent(@NotNull Project project) {
        mainPanel = new JPanel(new BorderLayout());
        
        // Auth status panel
        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authStatusLabel = new JLabel("Authenticating...");
        authPanel.add(authStatusLabel);
        
        // Continuous development status
        JPanel continuousDevPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        continuousDevStatusLabel = new JLabel("Checking continuous development...");
        continuousDevPanel.add(continuousDevStatusLabel);
        
        // Pattern recognition status
        JPanel patternRecogPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        patternRecogStatusLabel = new JLabel("Checking pattern recognition...");
        patternRecogPanel.add(patternRecogStatusLabel);
        
        // Status panel
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.add(authPanel);
        statusPanel.add(continuousDevPanel);
        statusPanel.add(patternRecogPanel);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        // Login/Logout buttons
        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login(project));
        
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout(project));
        
        // Generate code button
        generateCodeButton = new JButton("Generate Code");
        generateCodeButton.addActionListener(e -> generateCode(project));
        
        // Fix errors button
        fixErrorsButton = new JButton("Fix Errors");
        fixErrorsButton.addActionListener(e -> fixErrors(project));
        
        // Toggle continuous development button
        toggleContinuousDevButton = new JButton("Toggle Continuous Development");
        toggleContinuousDevButton.addActionListener(e -> toggleContinuousDevelopment(project));
        
        // Toggle pattern recognition button
        togglePatternRecogButton = new JButton("Toggle Pattern Recognition");
        togglePatternRecogButton.addActionListener(e -> togglePatternRecognition(project));
        
        // Add buttons to panel
        buttonsPanel.add(loginButton);
        buttonsPanel.add(logoutButton);
        buttonsPanel.add(generateCodeButton);
        buttonsPanel.add(fixErrorsButton);
        buttonsPanel.add(toggleContinuousDevButton);
        buttonsPanel.add(togglePatternRecogButton);
        
        // Status text
        statusText = new JTextArea(10, 40);
        statusText.setEditable(false);
        statusText.setFont(UIUtil.getLabelFont());
        JBScrollPane scrollPane = new JBScrollPane(statusText);
        
        // Heading
        JBLabel headingLabel = new JBLabel("ModForge AI Minecraft Mod Development");
        headingLabel.setFont(JBUI.Fonts.label().biggerOn(4).asBold());
        headingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Description
        JBLabel descriptionLabel = new JBLabel("Autonomous development with multi-loader support");
        descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Top panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        
        // Add components to topPanel
        headingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(headingLabel);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(descriptionLabel);
        topPanel.add(Box.createVerticalStrut(10));
        
        // Main layout
        JPanel contentPanel = FormBuilder.createFormBuilder()
                .addComponent(statusPanel)
                .addComponent(buttonsPanel)
                .addComponentFillVertically(Box.createVerticalStrut(10), 0)
                .addSeparator()
                .addComponent(new JBLabel("Status & Activity Log"))
                .addComponent(scrollPane)
                .getPanel();
        
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Update the status display.
     *
     * @param project The project
     */
    private void updateStatus(@NotNull Project project) {
        if (!initialized.get()) {
            return;
        }
        
        // Update auth status
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        boolean isAuthenticated = authManager.isAuthenticated();
        
        SwingUtilities.invokeLater(() -> {
            if (isAuthenticated) {
                authStatusLabel.setText("Authentication Status: Logged in as " + authManager.getUsername());
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
                generateCodeButton.setEnabled(true);
                fixErrorsButton.setEnabled(true);
                toggleContinuousDevButton.setEnabled(true);
                togglePatternRecogButton.setEnabled(true);
            } else {
                authStatusLabel.setText("Authentication Status: Not logged in");
                loginButton.setEnabled(true);
                logoutButton.setEnabled(false);
                generateCodeButton.setEnabled(false);
                fixErrorsButton.setEnabled(false);
                toggleContinuousDevButton.setEnabled(false);
                togglePatternRecogButton.setEnabled(false);
            }
        });
        
        // Update continuous development status
        ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
        if (continuousService != null) {
            boolean isEnabled = continuousService.isEnabled();
            boolean isRunning = continuousService.isRunning();
            
            SwingUtilities.invokeLater(() -> {
                if (isEnabled) {
                    continuousDevStatusLabel.setText("Continuous Development: " + (isRunning ? "Running" : "Enabled but not running"));
                    toggleContinuousDevButton.setText("Disable Continuous Development");
                } else {
                    continuousDevStatusLabel.setText("Continuous Development: Disabled");
                    toggleContinuousDevButton.setText("Enable Continuous Development");
                }
            });
            
            // Update activity log
            if (isEnabled && isRunning) {
                Map<String, Object> stats = continuousService.getStatistics();
                long lastScanTime = (long) stats.getOrDefault("lastScanTime", 0L);
                int fixCount = (int) stats.getOrDefault("fixCount", 0);
                int errorCount = (int) stats.getOrDefault("errorCount", 0);
                int successCount = (int) stats.getOrDefault("successCount", 0);
                Map<String, String> lastActions = (Map<String, String>) stats.getOrDefault("lastActions", Map.of());
                
                StringBuilder sb = new StringBuilder();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sb.append("Continuous Development Status\n");
                sb.append("==========================\n");
                sb.append("Last Scan: ").append(lastScanTime > 0 ? dateFormat.format(new Date(lastScanTime)) : "Never").append("\n");
                sb.append("Fix Count: ").append(fixCount).append("\n");
                sb.append("Error Count: ").append(errorCount).append("\n");
                sb.append("Success Count: ").append(successCount).append("\n\n");
                
                sb.append("Recent Actions:\n");
                if (lastActions.isEmpty()) {
                    sb.append("No actions recorded yet.\n");
                } else {
                    lastActions.forEach((timestamp, action) -> {
                        sb.append("- ").append(timestamp).append(": ").append(action).append("\n");
                    });
                }
                
                SwingUtilities.invokeLater(() -> statusText.setText(sb.toString()));
            }
        }
        
        // Update pattern recognition status
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean patternRecogEnabled = settings.isEnablePatternRecognition();
        
        SwingUtilities.invokeLater(() -> {
            if (patternRecogEnabled) {
                patternRecogStatusLabel.setText("Pattern Recognition: Enabled");
                togglePatternRecogButton.setText("Disable Pattern Recognition");
            } else {
                patternRecogStatusLabel.setText("Pattern Recognition: Disabled");
                togglePatternRecogButton.setText("Enable Pattern Recognition");
            }
        });
    }
    
    /**
     * Log in to the ModForge server.
     *
     * @param project The project
     */
    private void login(@NotNull Project project) {
        // Create and show login dialog
        LoginDialog dialog = new LoginDialog(project);
        if (dialog.showAndGet()) {
            String username = dialog.getUsername();
            String password = dialog.getPassword();
            String serverUrl = dialog.getServerUrl();
            
            // Save server URL
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.setServerUrl(serverUrl);
            
            // Attempt login
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            authManager.login(username, password)
                    .thenAccept(success -> {
                        SwingUtilities.invokeLater(() -> {
                            updateStatus(project);
                            
                            if (success) {
                                logStatus("Logged in successfully as " + username);
                            } else {
                                logStatus("Login failed. Please check your credentials.");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        SwingUtilities.invokeLater(() -> {
                            logStatus("Login error: " + e.getMessage());
                        });
                        return null;
                    });
        }
    }
    
    /**
     * Log out from the ModForge server.
     *
     * @param project The project
     */
    private void logout(@NotNull Project project) {
        // Attempt logout
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        authManager.logout()
                .thenAccept(success -> {
                    SwingUtilities.invokeLater(() -> {
                        updateStatus(project);
                        
                        if (success) {
                            logStatus("Logged out successfully");
                        } else {
                            logStatus("Logout failed");
                        }
                    });
                })
                .exceptionally(e -> {
                    SwingUtilities.invokeLater(() -> {
                        logStatus("Logout error: " + e.getMessage());
                    });
                    return null;
                });
    }
    
    /**
     * Generate code.
     *
     * @param project The project
     */
    private void generateCode(@NotNull Project project) {
        // Create and perform action
        GenerateCodeAction action = new GenerateCodeAction();
        action.actionPerformed(createEmptyActionEvent(project));
    }
    
    /**
     * Fix errors.
     *
     * @param project The project
     */
    private void fixErrors(@NotNull Project project) {
        // Create and perform action
        FixErrorsAction action = new FixErrorsAction();
        action.actionPerformed(createEmptyActionEvent(project));
    }
    
    /**
     * Toggle continuous development.
     *
     * @param project The project
     */
    private void toggleContinuousDevelopment(@NotNull Project project) {
        // Create and perform action
        ToggleContinuousDevelopmentAction action = new ToggleContinuousDevelopmentAction();
        action.actionPerformed(createEmptyActionEvent(project));
        
        // Update status
        updateStatus(project);
    }
    
    /**
     * Toggle pattern recognition.
     *
     * @param project The project
     */
    private void togglePatternRecognition(@NotNull Project project) {
        // Create and perform action
        TogglePatternRecognitionAction action = new TogglePatternRecognitionAction();
        action.actionPerformed(createEmptyActionEvent(project));
        
        // Update status
        updateStatus(project);
    }
    
    /**
     * Create an empty action event for use with actions.
     *
     * @param project The project
     * @return The action event
     */
    private com.intellij.openapi.actionSystem.AnActionEvent createEmptyActionEvent(@NotNull Project project) {
        return new com.intellij.openapi.actionSystem.AnActionEvent(
                null,
                com.modforge.intellij.plugin.utils.CompatibilityUtil.getCompatibleDataContext(),
                "",
                new com.intellij.openapi.actionSystem.Presentation(),
                com.intellij.openapi.actionSystem.ActionManager.getInstance(),
                0
        );
    }
    
    /**
     * Log a status message.
     *
     * @param message The message
     */
    private void logStatus(@NotNull String message) {
        // Add timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        
        // Format message
        String formattedMessage = timestamp + " - " + message + "\n";
        
        // Append to status text
        SwingUtilities.invokeLater(() -> {
            statusText.append(formattedMessage);
            
            // Scroll to bottom
            statusText.setCaretPosition(statusText.getDocument().getLength());
        });
    }
    
    /**
     * Dialog for logging in to the ModForge server.
     */
    private static class LoginDialog extends com.intellij.openapi.ui.DialogWrapper {
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JTextField serverUrlField;
        
        public LoginDialog(Project project) {
            super(project);
            
            setTitle("Login to ModForge");
            setOKButtonText("Login");
            setCancelButtonText("Cancel");
            
            init();
        }
        
        @Override
        protected JComponent createCenterPanel() {
            // Create components
            usernameField = new JTextField(20);
            passwordField = new JPasswordField(20);
            
            // Set default server URL from settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            serverUrlField = new JTextField(settings.getServerUrl(), 20);
            
            // Layout
            JPanel panel = FormBuilder.createFormBuilder()
                    .addLabeledComponent("Username:", usernameField)
                    .addLabeledComponent("Password:", passwordField)
                    .addLabeledComponent("Server URL:", serverUrlField)
                    .addComponentFillVertically(new JPanel(), 0)
                    .getPanel();
            
            panel.setPreferredSize(new Dimension(300, panel.getPreferredSize().height));
            
            return panel;
        }
        
        @Override
        protected JComponent getPreferredFocusedComponent() {
            return usernameField;
        }
        
        /**
         * Get the entered username.
         *
         * @return The username
         */
        public String getUsername() {
            return usernameField.getText().trim();
        }
        
        /**
         * Get the entered password.
         *
         * @return The password
         */
        public String getPassword() {
            return new String(passwordField.getPassword());
        }
        
        /**
         * Get the entered server URL.
         *
         * @return The server URL
         */
        public String getServerUrl() {
            return serverUrlField.getText().trim();
        }
        
        @Override
        protected void doOKAction() {
            if (getUsername().isEmpty()) {
                com.modforge.intellij.plugin.utils.CompatibilityUtil.showErrorDialog(
                        null, // No project context here, using null instead
                        "Username cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            if (getPassword().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Password cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            if (getServerUrl().isEmpty()) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Server URL cannot be empty",
                        "Validation Error"
                );
                return;
            }
            
            super.doOKAction();
        }
    }
}