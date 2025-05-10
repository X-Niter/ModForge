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
 * Persistent settings for ModForge.
 */
@Service
@State(
        name = "ModForgeSettings",
        storages = {
                @Storage("modforge.xml")
        }
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String serverUrl = "http://localhost:5000";
    private String username = "";
    private String password = "";
    private String accessToken = "";
    private boolean authenticated = false;
    private boolean rememberCredentials = true;
    private boolean continuousDevelopment = false;
    private boolean patternRecognition = true;
    private int pollingInterval = 5 * 60 * 1000; // 5 minutes in milliseconds
    
    /**
     * Get instance.
     * @return Instance
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
        this.serverUrl = serverUrl;
    }

    /**
     * Get username.
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set username.
     * @param username Username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get password.
     * @return Password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set password.
     * @param password Password
     */
    public void setPassword(String password) {
        this.password = password;
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
        this.accessToken = accessToken;
    }

    /**
     * Check if authenticated.
     * @return Whether the user is authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Set authentication status.
     * @param authenticated Whether the user is authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * Check if credentials should be remembered.
     * @return Whether credentials should be remembered
     */
    public boolean isRememberCredentials() {
        return rememberCredentials;
    }

    /**
     * Set whether credentials should be remembered.
     * @param rememberCredentials Whether credentials should be remembered
     */
    public void setRememberCredentials(boolean rememberCredentials) {
        this.rememberCredentials = rememberCredentials;
    }

    /**
     * Check if continuous development is enabled.
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
     * Check if pattern recognition is enabled.
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

    /**
     * Get polling interval.
     * @return Polling interval in milliseconds
     */
    public int getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Set polling interval.
     * @param pollingInterval Polling interval in milliseconds
     */
    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }
}