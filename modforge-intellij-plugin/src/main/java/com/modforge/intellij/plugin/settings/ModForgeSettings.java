package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for ModForge.
 */
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = {@Storage("ModForgeSettings.xml")}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String openAiApiKey = "";
    private boolean continuousDevelopmentEnabled = false;
    private boolean patternRecognitionEnabled = true;
    private int updateFrequencyMinutes = 5;
    private String serverUrl = "http://localhost:5000";
    private String username = "";
    private String password = "";
    private String sessionToken = "";
    private boolean authenticated = false;
    
    /**
     * Gets the instance of this settings.
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
     * Gets the OpenAI API key.
     * @return The API key
     */
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    
    /**
     * Sets the OpenAI API key.
     * @param openAiApiKey The API key
     */
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
    
    /**
     * Checks if continuous development is enabled.
     * @return True if continuous development is enabled, false otherwise
     */
    public boolean isContinuousDevelopmentEnabled() {
        return continuousDevelopmentEnabled;
    }
    
    /**
     * Sets whether continuous development is enabled.
     * @param continuousDevelopmentEnabled Whether continuous development is enabled
     */
    public void setContinuousDevelopmentEnabled(boolean continuousDevelopmentEnabled) {
        this.continuousDevelopmentEnabled = continuousDevelopmentEnabled;
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return True if pattern recognition is enabled, false otherwise
     */
    public boolean isPatternRecognitionEnabled() {
        return patternRecognitionEnabled;
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param patternRecognitionEnabled Whether pattern recognition is enabled
     */
    public void setPatternRecognitionEnabled(boolean patternRecognitionEnabled) {
        this.patternRecognitionEnabled = patternRecognitionEnabled;
    }
    
    /**
     * Gets the update frequency in minutes.
     * @return The update frequency in minutes
     */
    public int getUpdateFrequencyMinutes() {
        return updateFrequencyMinutes;
    }
    
    /**
     * Sets the update frequency in minutes.
     * @param updateFrequencyMinutes The update frequency in minutes
     */
    public void setUpdateFrequencyMinutes(int updateFrequencyMinutes) {
        this.updateFrequencyMinutes = updateFrequencyMinutes;
    }
    
    /**
     * Gets the server URL.
     * @return The server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }
    
    /**
     * Sets the server URL.
     * @param serverUrl The server URL
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    /**
     * Gets the username.
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username.
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the password.
     * @return The password
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Sets the password.
     * @param password The password
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Gets the session token.
     * @return The session token
     */
    public String getSessionToken() {
        return sessionToken;
    }
    
    /**
     * Sets the session token.
     * @param sessionToken The session token
     */
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    /**
     * Checks if the user is authenticated.
     * @return True if the user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Sets whether the user is authenticated.
     * @param authenticated Whether the user is authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    /**
     * Resets all settings to their default values.
     */
    public void resetToDefaults() {
        openAiApiKey = "";
        continuousDevelopmentEnabled = false;
        patternRecognitionEnabled = true;
        updateFrequencyMinutes = 5;
        serverUrl = "http://localhost:5000";
        username = "";
        password = "";
        sessionToken = "";
        authenticated = false;
    }
}