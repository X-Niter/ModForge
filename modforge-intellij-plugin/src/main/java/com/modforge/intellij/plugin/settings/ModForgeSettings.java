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
 * Persistent settings for ModForge plugin.
 */
@Service
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = @Storage("ModForgeSettings.xml")
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // Default server URL
    private static final String DEFAULT_SERVER_URL = "http://localhost:5000";
    
    // Server URL
    private String serverUrl = DEFAULT_SERVER_URL;
    
    // Access token
    private String accessToken = "";
    
    // Enable continuous development
    private boolean continuousDevelopment = false;
    
    // Enable pattern recognition
    private boolean patternRecognition = true;
    
    /**
     * Get instance of ModForgeSettings.
     * @return ModForgeSettings instance
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }
    
    /**
     * Get server URL.
     * @return Server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }
    
    /**
     * Set server URL.
     * @param serverUrl Server URL
     */
    public void setServerUrl(String serverUrl) {
        if (serverUrl != null) {
            this.serverUrl = serverUrl;
        }
    }
    
    /**
     * Get access token.
     * @return Access token
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * Set access token.
     * @param accessToken Access token
     */
    public void setAccessToken(String accessToken) {
        if (accessToken != null) {
            this.accessToken = accessToken;
        }
    }
    
    /**
     * Get whether continuous development is enabled.
     * @return Whether continuous development is enabled
     */
    public boolean isContinuousDevelopment() {
        return continuousDevelopment;
    }
    
    /**
     * Set whether continuous development is enabled.
     * @param continuousDevelopment Whether continuous development is enabled
     */
    public void setContinuousDevelopment(boolean continuousDevelopment) {
        this.continuousDevelopment = continuousDevelopment;
    }
    
    /**
     * Get whether pattern recognition is enabled.
     * @return Whether pattern recognition is enabled
     */
    public boolean isPatternRecognition() {
        return patternRecognition;
    }
    
    /**
     * Set whether pattern recognition is enabled.
     * @param patternRecognition Whether pattern recognition is enabled
     */
    public void setPatternRecognition(boolean patternRecognition) {
        this.patternRecognition = patternRecognition;
    }
    
    @Override
    public @Nullable ModForgeSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}