package com.modforge.intellij.plugin.settings;

import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Component for ModForge settings.
 * This class provides the UI components for the settings dialog.
 */
public class ModForgeSettingsComponent {
    private final JPanel mainPanel;
    private final JBTextField apiKeyField = new JBPasswordField();
    private final JComboBox<String> aiModelComboBox;
    private final JBCheckBox continuousDevelopmentCheckBox = new JBCheckBox("Enable Continuous Development");
    private final JBTextField continuousDevelopmentIntervalField = new JBTextField();
    private final JBCheckBox patternRecognitionCheckBox = new JBCheckBox("Enable Pattern Recognition");
    
    // ModForge server connection settings
    private final JBTextField serverUrlField = new JBTextField();
    private final JBTextField usernameField = new JBTextField();
    private final JBPasswordField passwordField = new JBPasswordField();
    private final JBLabel authStatusLabel = new JBLabel();
    private final JButton testConnectionButton = new JButton("Test Connection");
    private final JButton loginButton = new JButton("Login");
    private final JButton logoutButton = new JButton("Logout");
    
    // Synchronization settings
    private final JBTextField syncServerUrlField = new JBTextField();
    private final JBTextField syncTokenField = new JBPasswordField();
    private final JBCheckBox syncEnabledCheckBox = new JBCheckBox("Enable Synchronization");
    private final JBCheckBox autoUploadCheckBox = new JBCheckBox("Enable Auto Upload");
    private final JBCheckBox autoDownloadCheckBox = new JBCheckBox("Enable Auto Download");
    private final JBCheckBox darkThemeCheckBox = new JBCheckBox("Use Dark Theme");
    
    /**
     * Creates a new ModForgeSettingsComponent.
     */
    public ModForgeSettingsComponent() {
        // Set up components
        apiKeyField.setColumns(30);
        continuousDevelopmentIntervalField.setColumns(5);
        serverUrlField.setColumns(30);
        usernameField.setColumns(20);
        passwordField.setColumns(20);
        syncServerUrlField.setColumns(30);
        syncTokenField.setColumns(30);
        
        // Set up authentication status label
        authStatusLabel.setForeground(Color.GRAY);
        authStatusLabel.setText("Not authenticated");
        
        // Create authentication button panel
        JPanel authButtonPanel = new JPanel(new HorizontalLayout(5));
        authButtonPanel.add(testConnectionButton);
        authButtonPanel.add(loginButton);
        authButtonPanel.add(logoutButton);
        
        // Set up AI model combo box
        String[] aiModels = {"gpt-4o", "gpt-4", "gpt-3.5-turbo", "claude-3-7-sonnet-20250219", "claude-3-opus-20240229"};
        aiModelComboBox = new ComboBoxWithWidePopup<>(aiModels);
        
        // Create form
        mainPanel = FormBuilder.createFormBuilder()
                .addSeparator("ModForge Server Connection")
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField, true)
                .addLabeledComponent(new JBLabel("Username:"), usernameField, true)
                .addLabeledComponent(new JBLabel("Password:"), passwordField, true)
                .addLabeledComponent(new JBLabel("Status:"), authStatusLabel, true)
                .addComponent(authButtonPanel)
                
                .addSeparator("API Settings")
                .addLabeledComponent(new JBLabel("OpenAI API Key:"), apiKeyField, true)
                .addLabeledComponent(new JBLabel("AI Model:"), aiModelComboBox, true)
                
                .addSeparator("Development Settings")
                .addComponent(continuousDevelopmentCheckBox)
                .addLabeledComponent(new JBLabel("Check Interval (minutes):"), continuousDevelopmentIntervalField, true)
                .addComponent(patternRecognitionCheckBox)
                
                .addSeparator("Synchronization Settings")
                .addComponent(syncEnabledCheckBox)
                .addLabeledComponent(new JBLabel("Sync Server URL:"), syncServerUrlField, true)
                .addLabeledComponent(new JBLabel("Sync Token:"), syncTokenField, true)
                .addComponent(autoUploadCheckBox)
                .addComponent(autoDownloadCheckBox)
                
                .addSeparator("UI Settings")
                .addComponent(darkThemeCheckBox)
                
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Add listeners for authentication
        testConnectionButton.addActionListener(this::testConnection);
        loginButton.addActionListener(this::login);
        logoutButton.addActionListener(this::logout);
        
        // Add listeners for sync
        syncEnabledCheckBox.addChangeListener(e -> {
            boolean enabled = syncEnabledCheckBox.isSelected();
            syncServerUrlField.setEnabled(enabled);
            syncTokenField.setEnabled(enabled);
            autoUploadCheckBox.setEnabled(enabled);
            autoDownloadCheckBox.setEnabled(enabled);
        });
        
        // Add listeners for continuous development
        continuousDevelopmentCheckBox.addChangeListener(e -> {
            boolean enabled = continuousDevelopmentCheckBox.isSelected();
            continuousDevelopmentIntervalField.setEnabled(enabled);
        });
        
        // Initialize UI state from settings
        updateAuthenticationStatus();
    }
    
    /**
     * Tests the connection to the ModForge server.
     * @param event The action event
     */
    private void testConnection(ActionEvent event) {
        String serverUrl = serverUrlField.getText().trim();
        
        if (serverUrl.isEmpty()) {
            authStatusLabel.setText("Server URL is required");
            authStatusLabel.setForeground(Color.RED);
            return;
        }
        
        testConnectionButton.setEnabled(false);
        authStatusLabel.setText("Testing connection...");
        authStatusLabel.setForeground(Color.GRAY);
        
        // Run in a background thread to avoid UI freezing
        SwingUtilities.invokeLater(() -> {
            String result = ConnectionTestUtil.testServerConnection(serverUrl);
            
            if ("OK".equals(result)) {
                authStatusLabel.setText("Connection successful");
                authStatusLabel.setForeground(new Color(0, 128, 0)); // Dark green
            } else {
                authStatusLabel.setText(result);
                authStatusLabel.setForeground(Color.RED);
            }
            
            testConnectionButton.setEnabled(true);
        });
    }
    
    /**
     * Logs in to the ModForge server.
     * @param event The action event
     */
    private void login(ActionEvent event) {
        // Save current settings first
        saveServerSettings();
        
        loginButton.setEnabled(false);
        authStatusLabel.setText("Authenticating...");
        authStatusLabel.setForeground(Color.GRAY);
        
        // Run in a background thread to avoid UI freezing
        SwingUtilities.invokeLater(() -> {
            boolean success = AuthenticationManager.getInstance().authenticate();
            
            if (success) {
                authStatusLabel.setText("Authenticated successfully");
                authStatusLabel.setForeground(new Color(0, 128, 0)); // Dark green
            } else {
                authStatusLabel.setText("Authentication failed");
                authStatusLabel.setForeground(Color.RED);
            }
            
            loginButton.setEnabled(true);
            updateAuthenticationStatus();
        });
    }
    
    /**
     * Logs out from the ModForge server.
     * @param event The action event
     */
    private void logout(ActionEvent event) {
        logoutButton.setEnabled(false);
        authStatusLabel.setText("Logging out...");
        authStatusLabel.setForeground(Color.GRAY);
        
        // Run in a background thread to avoid UI freezing
        SwingUtilities.invokeLater(() -> {
            boolean success = AuthenticationManager.getInstance().logout();
            
            if (success) {
                authStatusLabel.setText("Logged out successfully");
                authStatusLabel.setForeground(Color.GRAY);
            } else {
                authStatusLabel.setText("Logout failed");
                authStatusLabel.setForeground(Color.RED);
            }
            
            logoutButton.setEnabled(true);
            updateAuthenticationStatus();
        });
    }
    
    /**
     * Saves the server settings.
     */
    private void saveServerSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setServerUrl(serverUrlField.getText().trim());
        settings.setUsername(usernameField.getText().trim());
        settings.setPassword(new String(passwordField.getPassword()));
    }
    
    /**
     * Updates the authentication status UI.
     */
    private void updateAuthenticationStatus() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean isAuthenticated = settings.isAuthenticated();
        
        logoutButton.setEnabled(isAuthenticated);
        
        if (isAuthenticated) {
            authStatusLabel.setText("Authenticated as " + settings.getUsername());
            authStatusLabel.setForeground(new Color(0, 128, 0)); // Dark green
        } else {
            authStatusLabel.setText("Not authenticated");
            authStatusLabel.setForeground(Color.GRAY);
        }
    }
    
    /**
     * Gets the main panel.
     * @return The main panel
     */
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * Gets the preferred focused component.
     * @return The preferred focused component
     */
    public JComponent getPreferredFocusedComponent() {
        return apiKeyField;
    }
    
    /**
     * Gets the API key.
     * @return The API key
     */
    @NotNull
    public String getApiKey() {
        return apiKeyField.getText();
    }
    
    /**
     * Sets the API key.
     * @param apiKey The API key
     */
    public void setApiKey(@NotNull String apiKey) {
        apiKeyField.setText(apiKey);
    }
    
    /**
     * Gets the AI model.
     * @return The AI model
     */
    @NotNull
    public String getAiModel() {
        return (String) aiModelComboBox.getSelectedItem();
    }
    
    /**
     * Sets the AI model.
     * @param aiModel The AI model
     */
    public void setAiModel(@NotNull String aiModel) {
        aiModelComboBox.setSelectedItem(aiModel);
    }
    
    /**
     * Checks if continuous development is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isContinuousDevelopmentEnabled() {
        return continuousDevelopmentCheckBox.isSelected();
    }
    
    /**
     * Sets whether continuous development is enabled.
     * @param enabled True to enable, false to disable
     */
    public void setContinuousDevelopmentEnabled(boolean enabled) {
        continuousDevelopmentCheckBox.setSelected(enabled);
        continuousDevelopmentIntervalField.setEnabled(enabled);
    }
    
    /**
     * Gets the continuous development interval in minutes.
     * @return The interval in minutes
     */
    public int getContinuousDevelopmentIntervalMinutes() {
        try {
            return Integer.parseInt(continuousDevelopmentIntervalField.getText());
        } catch (NumberFormatException e) {
            return 5; // Default
        }
    }
    
    /**
     * Sets the continuous development interval in minutes.
     * @param minutes The interval in minutes
     */
    public void setContinuousDevelopmentIntervalMinutes(int minutes) {
        continuousDevelopmentIntervalField.setText(String.valueOf(minutes));
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isPatternRecognitionEnabled() {
        return patternRecognitionCheckBox.isSelected();
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param enabled True to enable, false to disable
     */
    public void setPatternRecognitionEnabled(boolean enabled) {
        patternRecognitionCheckBox.setSelected(enabled);
    }
    
    /**
     * Gets the sync server URL.
     * @return The sync server URL
     */
    @NotNull
    public String getSyncServerUrl() {
        return syncServerUrlField.getText();
    }
    
    /**
     * Sets the sync server URL.
     * @param url The sync server URL
     */
    public void setSyncServerUrl(@NotNull String url) {
        syncServerUrlField.setText(url);
    }
    
    /**
     * Gets the sync token.
     * @return The sync token
     */
    @NotNull
    public String getSyncToken() {
        return syncTokenField.getText();
    }
    
    /**
     * Sets the sync token.
     * @param token The sync token
     */
    public void setSyncToken(@NotNull String token) {
        syncTokenField.setText(token);
    }
    
    /**
     * Checks if sync is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isSyncEnabled() {
        return syncEnabledCheckBox.isSelected();
    }
    
    /**
     * Sets whether sync is enabled.
     * @param enabled True to enable, false to disable
     */
    public void setSyncEnabled(boolean enabled) {
        syncEnabledCheckBox.setSelected(enabled);
        syncServerUrlField.setEnabled(enabled);
        syncTokenField.setEnabled(enabled);
        autoUploadCheckBox.setEnabled(enabled);
        autoDownloadCheckBox.setEnabled(enabled);
    }
    
    /**
     * Checks if auto upload is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isAutoUploadEnabled() {
        return autoUploadCheckBox.isSelected();
    }
    
    /**
     * Sets whether auto upload is enabled.
     * @param enabled True to enable, false to disable
     */
    public void setAutoUploadEnabled(boolean enabled) {
        autoUploadCheckBox.setSelected(enabled);
    }
    
    /**
     * Checks if auto download is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isAutoDownloadEnabled() {
        return autoDownloadCheckBox.isSelected();
    }
    
    /**
     * Sets whether auto download is enabled.
     * @param enabled True to enable, false to disable
     */
    public void setAutoDownloadEnabled(boolean enabled) {
        autoDownloadCheckBox.setSelected(enabled);
    }
    
    /**
     * Checks if dark theme is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isDarkTheme() {
        return darkThemeCheckBox.isSelected();
    }
    
    /**
     * Sets whether dark theme is enabled.
     * @param enabled True to enable, false to disable
     */
    public void setDarkTheme(boolean enabled) {
        darkThemeCheckBox.setSelected(enabled);
    }
    
    /**
     * Gets the server URL.
     * @return The server URL
     */
    @NotNull
    public String getServerUrl() {
        return serverUrlField.getText();
    }
    
    /**
     * Sets the server URL.
     * @param url The server URL
     */
    public void setServerUrl(@NotNull String url) {
        serverUrlField.setText(url);
    }
    
    /**
     * Gets the username.
     * @return The username
     */
    @NotNull
    public String getUsername() {
        return usernameField.getText();
    }
    
    /**
     * Sets the username.
     * @param username The username
     */
    public void setUsername(@NotNull String username) {
        usernameField.setText(username);
    }
    
    /**
     * Gets the password.
     * @return The password
     */
    @NotNull
    public String getPassword() {
        return new String(passwordField.getPassword());
    }
    
    /**
     * Sets the password.
     * @param password The password
     */
    public void setPassword(@NotNull String password) {
        passwordField.setText(password);
    }
}