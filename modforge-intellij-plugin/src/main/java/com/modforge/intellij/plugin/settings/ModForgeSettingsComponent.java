package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Objects;

/**
 * Component for ModForge settings UI.
 */
public class ModForgeSettingsComponent {
    private static final Logger LOG = Logger.getInstance(ModForgeSettingsComponent.class);
    
    private final JPanel mainPanel;
    private final JBTextField serverUrlField;
    private final JBCheckBox useDarkModeCheckbox;
    private final JBCheckBox enableContinuousDevelopmentCheckbox;
    private final JBCheckBox enablePatternRecognitionCheckbox;
    private final JBCheckBox enableGitHubIntegrationCheckbox;
    private final JBPasswordField githubTokenField;
    private final JSpinner maxApiRequestsSpinner;
    private final JButton testConnectionButton;
    private final JButton resetButton;
    
    /**
     * Create a new settings component.
     */
    public ModForgeSettingsComponent() {
        // Initialize fields
        serverUrlField = new JBTextField();
        useDarkModeCheckbox = new JBCheckBox("Use dark mode");
        enableContinuousDevelopmentCheckbox = new JBCheckBox("Enable continuous development");
        enablePatternRecognitionCheckbox = new JBCheckBox("Enable pattern recognition");
        enableGitHubIntegrationCheckbox = new JBCheckBox("Enable GitHub integration");
        githubTokenField = new JBPasswordField();
        
        // Set up spinner for max API requests
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(100, 1, 1000, 10);
        maxApiRequestsSpinner = new JSpinner(spinnerModel);
        
        // Set up buttons
        testConnectionButton = new JButton("Test Connection");
        resetButton = new JButton("Reset to Defaults");
        
        // Add button actions
        testConnectionButton.addActionListener(e -> testConnection());
        resetButton.addActionListener(e -> resetToDefaults());
        
        // Enable/disable GitHub token field based on GitHub integration checkbox
        enableGitHubIntegrationCheckbox.addActionListener(e -> 
                githubTokenField.setEnabled(enableGitHubIntegrationCheckbox.isSelected()));
        
        // Create main panel
        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField, 1, false)
                .addComponent(useDarkModeCheckbox, 1)
                .addComponent(enableContinuousDevelopmentCheckbox, 1)
                .addComponent(enablePatternRecognitionCheckbox, 1)
                .addSeparator(10)
                .addComponent(enableGitHubIntegrationCheckbox, 1)
                .addLabeledComponent(new JBLabel("GitHub Token:"), githubTokenField, 1, false)
                .addSeparator(10)
                .addLabeledComponent(new JBLabel("Max API requests per day:"), maxApiRequestsSpinner, 1, false)
                .addSeparator(10)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Add buttons to a separate panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(testConnectionButton);
        buttonPanel.add(resetButton);
        
        // Create final panel
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set initial state
        githubTokenField.setEnabled(enableGitHubIntegrationCheckbox.isSelected());
    }
    
    /**
     * Get the main panel.
     *
     * @return The main panel
     */
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * Get the preferred focus component.
     *
     * @return The preferred focus component
     */
    public JComponent getPreferredFocusedComponent() {
        return serverUrlField;
    }
    
    /**
     * Get the server URL.
     *
     * @return The server URL
     */
    @NotNull
    public String getServerUrl() {
        return serverUrlField.getText().trim();
    }
    
    /**
     * Set the server URL.
     *
     * @param serverUrl The server URL
     */
    public void setServerUrl(@NotNull String serverUrl) {
        serverUrlField.setText(serverUrl);
    }
    
    /**
     * Check if dark mode is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isUseDarkMode() {
        return useDarkModeCheckbox.isSelected();
    }
    
    /**
     * Set if dark mode is enabled.
     *
     * @param useDarkMode True to enable, false to disable
     */
    public void setUseDarkMode(boolean useDarkMode) {
        useDarkModeCheckbox.setSelected(useDarkMode);
    }
    
    /**
     * Check if continuous development is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopmentCheckbox.isSelected();
    }
    
    /**
     * Set if continuous development is enabled.
     *
     * @param enableContinuousDevelopment True to enable, false to disable
     */
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        enableContinuousDevelopmentCheckbox.setSelected(enableContinuousDevelopment);
    }
    
    /**
     * Check if pattern recognition is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnablePatternRecognition() {
        return enablePatternRecognitionCheckbox.isSelected();
    }
    
    /**
     * Set if pattern recognition is enabled.
     *
     * @param enablePatternRecognition True to enable, false to disable
     */
    public void setEnablePatternRecognition(boolean enablePatternRecognition) {
        enablePatternRecognitionCheckbox.setSelected(enablePatternRecognition);
    }
    
    /**
     * Check if GitHub integration is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnableGitHubIntegration() {
        return enableGitHubIntegrationCheckbox.isSelected();
    }
    
    /**
     * Set if GitHub integration is enabled.
     *
     * @param enableGitHubIntegration True to enable, false to disable
     */
    public void setEnableGitHubIntegration(boolean enableGitHubIntegration) {
        enableGitHubIntegrationCheckbox.setSelected(enableGitHubIntegration);
        githubTokenField.setEnabled(enableGitHubIntegration);
    }
    
    /**
     * Get the GitHub token.
     *
     * @return The GitHub token
     */
    @NotNull
    public String getGithubToken() {
        return new String(githubTokenField.getPassword());
    }
    
    /**
     * Set the GitHub token.
     *
     * @param githubToken The GitHub token
     */
    public void setGithubToken(@NotNull String githubToken) {
        githubTokenField.setText(githubToken);
    }
    
    /**
     * Get the maximum number of API requests per day.
     *
     * @return The maximum number of API requests per day
     */
    public int getMaxApiRequestsPerDay() {
        return (Integer) maxApiRequestsSpinner.getValue();
    }
    
    /**
     * Set the maximum number of API requests per day.
     *
     * @param maxApiRequestsPerDay The maximum number of API requests per day
     */
    public void setMaxApiRequestsPerDay(int maxApiRequestsPerDay) {
        maxApiRequestsSpinner.setValue(maxApiRequestsPerDay);
    }
    
    /**
     * Test the connection to the server.
     */
    private void testConnection() {
        String serverUrl = getServerUrl();
        if (serverUrl.isEmpty()) {
            Messages.showErrorDialog(
                    mainPanel,
                    "Server URL is required",
                    "Connection Test"
            );
            return;
        }
        
        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Testing...");
        
        ConnectionTestUtil.testConnection(serverUrl)
                .thenAccept(success -> {
                    SwingUtilities.invokeLater(() -> {
                        testConnectionButton.setEnabled(true);
                        testConnectionButton.setText("Test Connection");
                        
                        if (success) {
                            Messages.showInfoMessage(
                                    mainPanel,
                                    "Connection successful",
                                    "Connection Test"
                            );
                        } else {
                            Messages.showErrorDialog(
                                    mainPanel,
                                    "Connection failed. Please check the server URL.",
                                    "Connection Test"
                            );
                        }
                    });
                })
                .exceptionally(ex -> {
                    LOG.error("Error testing connection", ex);
                    
                    SwingUtilities.invokeLater(() -> {
                        testConnectionButton.setEnabled(true);
                        testConnectionButton.setText("Test Connection");
                        
                        Messages.showErrorDialog(
                                mainPanel,
                                "Error testing connection: " + ex.getMessage(),
                                "Connection Test"
                        );
                    });
                    
                    return null;
                });
    }
    
    /**
     * Reset settings to defaults.
     */
    private void resetToDefaults() {
        int result = Messages.showYesNoDialog(
                mainPanel,
                "Are you sure you want to reset all settings to defaults?",
                "Reset Settings",
                Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.resetToDefaults();
            
            setServerUrl(settings.getServerUrl());
            setUseDarkMode(settings.isUseDarkMode());
            setEnableContinuousDevelopment(settings.isEnableContinuousDevelopment());
            setEnablePatternRecognition(settings.isEnablePatternRecognition());
            setEnableGitHubIntegration(settings.isEnableGitHubIntegration());
            setGithubToken(settings.getGithubToken());
            setMaxApiRequestsPerDay(settings.getMaxApiRequestsPerDay());
        }
    }
}