package com.modforge.intellij.plugin.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Component for ModForge settings.
 */
public class ModForgeSettingsComponent {
    private final JPanel myMainPanel;
    private final JBTextField serverUrlField = new JBTextField();
    private final JBTextField usernameField = new JBTextField();
    private final JBPasswordField passwordField = new JBPasswordField();
    private final JBCheckBox rememberCredentialsCheckBox = new JBCheckBox("Remember credentials");
    private final JBCheckBox enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
    private final JBTextField continuousDevelopmentFrequencyField = new JBTextField();
    private final JBCheckBox enableAIGenerationCheckBox = new JBCheckBox("Enable AI-assisted code generation");
    private final JBCheckBox usePatternLearningCheckBox = new JBCheckBox("Use pattern learning to reduce API calls");
    private final JBTextField githubTokenField = new JBTextField();
    private final JBTextField githubUsernameField = new JBTextField();
    private final JLabel connectionStatusLabel = new JLabel(" ");
    private final JButton testConnectionButton = new JButton("Test Connection");
    
    /**
     * Constructor.
     */
    public ModForgeSettingsComponent() {
        // Server section
        JPanel serverPanel = createServerPanel();
        
        // Authentication section
        JPanel authPanel = createAuthenticationPanel();
        
        // Continuous development section
        JPanel continuousDevPanel = createContinuousDevPanel();
        
        // AI generation section
        JPanel aiPanel = createAIPanel();
        
        // GitHub integration section
        JPanel githubPanel = createGitHubPanel();
        
        // Main panel with all sections
        myMainPanel = FormBuilder.createFormBuilder()
                .addComponent(serverPanel)
                .addComponent(authPanel)
                .addComponent(continuousDevPanel)
                .addComponent(aiPanel)
                .addComponent(githubPanel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Add action to test connection button
        testConnectionButton.addActionListener(e -> testConnection());
    }
    
    /**
     * Create server panel.
     * @return The server panel
     */
    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField, 1, false)
                .addComponent(testConnectionButton)
                .addComponent(connectionStatusLabel)
                .getPanel();
        
        panel.add(UI.PanelFactory.panel(innerPanel)
                .withLabel("Server Settings")
                .withComment("Configure the ModForge server connection.")
                .createPanel(), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create authentication panel.
     * @return The authentication panel
     */
    private JPanel createAuthenticationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Username:"), usernameField, 1, false)
                .addLabeledComponent(new JBLabel("Password:"), passwordField, 1, false)
                .addComponent(rememberCredentialsCheckBox)
                .getPanel();
        
        panel.add(UI.PanelFactory.panel(innerPanel)
                .withLabel("Authentication")
                .withComment("Configure your ModForge credentials.")
                .createPanel(), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create continuous development panel.
     * @return The continuous development panel
     */
    private JPanel createContinuousDevPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = FormBuilder.createFormBuilder()
                .addComponent(enableContinuousDevelopmentCheckBox)
                .addLabeledComponent(new JBLabel("Check interval (minutes):"), continuousDevelopmentFrequencyField, 1, false)
                .getPanel();
        
        panel.add(UI.PanelFactory.panel(innerPanel)
                .withLabel("Continuous Development")
                .withComment("Configure continuous development settings.")
                .createPanel(), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create AI panel.
     * @return The AI panel
     */
    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = FormBuilder.createFormBuilder()
                .addComponent(enableAIGenerationCheckBox)
                .addComponent(usePatternLearningCheckBox)
                .getPanel();
        
        panel.add(UI.PanelFactory.panel(innerPanel)
                .withLabel("AI-Assisted Development")
                .withComment("Configure AI-assisted development settings.")
                .createPanel(), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create GitHub panel.
     * @return The GitHub panel
     */
    private JPanel createGitHubPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("GitHub Username:"), githubUsernameField, 1, false)
                .addLabeledComponent(new JBLabel("GitHub Token:"), githubTokenField, 1, false)
                .getPanel();
        
        panel.add(UI.PanelFactory.panel(innerPanel)
                .withLabel("GitHub Integration")
                .withComment("Configure GitHub integration settings.")
                .createPanel(), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Test connection to server.
     */
    private void testConnection() {
        String serverUrl = serverUrlField.getText().trim();
        
        if (serverUrl.isEmpty()) {
            connectionStatusLabel.setText("Server URL cannot be empty");
            connectionStatusLabel.setForeground(Color.RED);
            return;
        }
        
        connectionStatusLabel.setText("Testing connection...");
        connectionStatusLabel.setForeground(Color.BLUE);
        
        SwingUtilities.invokeLater(() -> {
            boolean connected = ConnectionTestUtil.testConnection(serverUrl);
            
            if (connected) {
                connectionStatusLabel.setText("Connection successful!");
                connectionStatusLabel.setForeground(new Color(0, 128, 0)); // Dark green
            } else {
                connectionStatusLabel.setText("Connection failed. Please check server URL.");
                connectionStatusLabel.setForeground(Color.RED);
            }
        });
    }
    
    /**
     * Get the main panel.
     * @return The main panel
     */
    public @NotNull JPanel getPanel() {
        return myMainPanel;
    }
    
    /**
     * Get preferred focus component.
     * @return The preferred focus component
     */
    public @NotNull JComponent getPreferredFocusedComponent() {
        return serverUrlField;
    }
    
    /**
     * Get server URL.
     * @return The server URL
     */
    public @NotNull String getServerUrl() {
        return serverUrlField.getText().trim();
    }
    
    /**
     * Set server URL.
     * @param serverUrl The server URL
     */
    public void setServerUrl(@NotNull String serverUrl) {
        serverUrlField.setText(serverUrl);
    }
    
    /**
     * Get username.
     * @return The username
     */
    public @NotNull String getUsername() {
        return usernameField.getText().trim();
    }
    
    /**
     * Set username.
     * @param username The username
     */
    public void setUsername(@NotNull String username) {
        usernameField.setText(username);
    }
    
    /**
     * Get password.
     * @return The password
     */
    public @NotNull String getPassword() {
        return new String(passwordField.getPassword());
    }
    
    /**
     * Set password.
     * @param password The password
     */
    public void setPassword(@NotNull String password) {
        passwordField.setText(password);
    }
    
    /**
     * Get whether to remember credentials.
     * @return Whether to remember credentials
     */
    public boolean isRememberCredentials() {
        return rememberCredentialsCheckBox.isSelected();
    }
    
    /**
     * Set whether to remember credentials.
     * @param rememberCredentials Whether to remember credentials
     */
    public void setRememberCredentials(boolean rememberCredentials) {
        rememberCredentialsCheckBox.setSelected(rememberCredentials);
    }
    
    /**
     * Get whether to enable continuous development.
     * @return Whether to enable continuous development
     */
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopmentCheckBox.isSelected();
    }
    
    /**
     * Set whether to enable continuous development.
     * @param enableContinuousDevelopment Whether to enable continuous development
     */
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        enableContinuousDevelopmentCheckBox.setSelected(enableContinuousDevelopment);
    }
    
    /**
     * Get continuous development frequency.
     * @return The continuous development frequency
     */
    public int getContinuousDevelopmentFrequency() {
        try {
            return Integer.parseInt(continuousDevelopmentFrequencyField.getText().trim());
        } catch (NumberFormatException e) {
            return 5; // Default
        }
    }
    
    /**
     * Set continuous development frequency.
     * @param continuousDevelopmentFrequency The continuous development frequency
     */
    public void setContinuousDevelopmentFrequency(int continuousDevelopmentFrequency) {
        continuousDevelopmentFrequencyField.setText(String.valueOf(continuousDevelopmentFrequency));
    }
    
    /**
     * Get whether to enable AI generation.
     * @return Whether to enable AI generation
     */
    public boolean isEnableAIGeneration() {
        return enableAIGenerationCheckBox.isSelected();
    }
    
    /**
     * Set whether to enable AI generation.
     * @param enableAIGeneration Whether to enable AI generation
     */
    public void setEnableAIGeneration(boolean enableAIGeneration) {
        enableAIGenerationCheckBox.setSelected(enableAIGeneration);
    }
    
    /**
     * Get whether to use pattern learning.
     * @return Whether to use pattern learning
     */
    public boolean isUsePatternLearning() {
        return usePatternLearningCheckBox.isSelected();
    }
    
    /**
     * Set whether to use pattern learning.
     * @param usePatternLearning Whether to use pattern learning
     */
    public void setUsePatternLearning(boolean usePatternLearning) {
        usePatternLearningCheckBox.setSelected(usePatternLearning);
    }
    
    /**
     * Get GitHub token.
     * @return The GitHub token
     */
    public @NotNull String getGithubToken() {
        return githubTokenField.getText().trim();
    }
    
    /**
     * Set GitHub token.
     * @param githubToken The GitHub token
     */
    public void setGithubToken(@NotNull String githubToken) {
        githubTokenField.setText(githubToken);
    }
    
    /**
     * Get GitHub username.
     * @return The GitHub username
     */
    public @NotNull String getGithubUsername() {
        return githubUsernameField.getText().trim();
    }
    
    /**
     * Set GitHub username.
     * @param githubUsername The GitHub username
     */
    public void setGithubUsername(@NotNull String githubUsername) {
        githubUsernameField.setText(githubUsername);
    }
}