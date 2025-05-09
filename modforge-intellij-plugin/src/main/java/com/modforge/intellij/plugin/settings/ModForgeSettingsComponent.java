package com.modforge.intellij.plugin.settings;

import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

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
        syncServerUrlField.setColumns(30);
        syncTokenField.setColumns(30);
        
        // Set up AI model combo box
        String[] aiModels = {"gpt-4", "gpt-3.5-turbo", "claude-3-opus", "claude-3-sonnet"};
        aiModelComboBox = new JComboBox<>(aiModels);
        
        // Create form
        mainPanel = FormBuilder.createFormBuilder()
                .addSeparator("API Settings")
                .addLabeledComponent(new JBLabel("API Key:"), apiKeyField, true)
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
}