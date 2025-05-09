package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Settings for ModForge.
 * These settings are persisted between IDE restarts.
 */
@Service(Service.Level.APP)
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = {@Storage("modForgeSettings.xml")}
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // API settings
    private String apiKey = "";
    private String aiModel = "gpt-4";
    
    // Development settings
    private boolean continuousDevelopmentEnabled = true;
    private int continuousDevelopmentIntervalMinutes = 5;
    private boolean patternRecognitionEnabled = true;
    
    // Sync settings
    private String syncServerUrl = "https://modforge.io/api";
    private String syncToken = "";
    private boolean syncEnabled = false;
    private boolean autoUploadEnabled = false;
    private boolean autoDownloadEnabled = false;
    
    // UI settings
    private boolean darkTheme = true;
    
    /**
     * Gets the instance of the settings.
     * @return The settings instance
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }
    
    @Override
    public @Nullable ModForgeSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * Gets the API key.
     * @return The API key
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * Sets the API key.
     * @param apiKey The API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * Gets the AI model.
     * @return The AI model
     */
    public String getAiModel() {
        return aiModel;
    }
    
    /**
     * Sets the AI model.
     * @param aiModel The AI model
     */
    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
    
    /**
     * Checks if continuous development is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isContinuousDevelopmentEnabled() {
        return continuousDevelopmentEnabled;
    }
    
    /**
     * Sets whether continuous development is enabled.
     * @param continuousDevelopmentEnabled True to enable, false to disable
     */
    public void setContinuousDevelopmentEnabled(boolean continuousDevelopmentEnabled) {
        this.continuousDevelopmentEnabled = continuousDevelopmentEnabled;
    }
    
    /**
     * Gets the continuous development interval in minutes.
     * @return The interval in minutes
     */
    public int getContinuousDevelopmentIntervalMinutes() {
        return continuousDevelopmentIntervalMinutes;
    }
    
    /**
     * Sets the continuous development interval in minutes.
     * @param continuousDevelopmentIntervalMinutes The interval in minutes
     */
    public void setContinuousDevelopmentIntervalMinutes(int continuousDevelopmentIntervalMinutes) {
        this.continuousDevelopmentIntervalMinutes = continuousDevelopmentIntervalMinutes;
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isPatternRecognitionEnabled() {
        return patternRecognitionEnabled;
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param patternRecognitionEnabled True to enable, false to disable
     */
    public void setPatternRecognitionEnabled(boolean patternRecognitionEnabled) {
        this.patternRecognitionEnabled = patternRecognitionEnabled;
    }
    
    /**
     * Gets the sync server URL.
     * @return The sync server URL
     */
    public String getSyncServerUrl() {
        return syncServerUrl;
    }
    
    /**
     * Sets the sync server URL.
     * @param syncServerUrl The sync server URL
     */
    public void setSyncServerUrl(String syncServerUrl) {
        this.syncServerUrl = syncServerUrl;
    }
    
    /**
     * Gets the sync token.
     * @return The sync token
     */
    public String getSyncToken() {
        return syncToken;
    }
    
    /**
     * Sets the sync token.
     * @param syncToken The sync token
     */
    public void setSyncToken(String syncToken) {
        this.syncToken = syncToken;
    }
    
    /**
     * Checks if sync is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isSyncEnabled() {
        return syncEnabled;
    }
    
    /**
     * Sets whether sync is enabled.
     * @param syncEnabled True to enable, false to disable
     */
    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
    
    /**
     * Checks if auto upload is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isAutoUploadEnabled() {
        return autoUploadEnabled;
    }
    
    /**
     * Sets whether auto upload is enabled.
     * @param autoUploadEnabled True to enable, false to disable
     */
    public void setAutoUploadEnabled(boolean autoUploadEnabled) {
        this.autoUploadEnabled = autoUploadEnabled;
    }
    
    /**
     * Checks if auto download is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isAutoDownloadEnabled() {
        return autoDownloadEnabled;
    }
    
    /**
     * Sets whether auto download is enabled.
     * @param autoDownloadEnabled True to enable, false to disable
     */
    public void setAutoDownloadEnabled(boolean autoDownloadEnabled) {
        this.autoDownloadEnabled = autoDownloadEnabled;
    }
    
    /**
     * Checks if dark theme is enabled.
     * @return True if enabled, false otherwise
     */
    public boolean isDarkTheme() {
        return darkTheme;
    }
    
    /**
     * Sets whether dark theme is enabled.
     * @param darkTheme True to enable, false to disable
     */
    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }
}