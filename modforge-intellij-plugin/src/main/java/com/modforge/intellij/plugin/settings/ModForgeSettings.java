package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Settings for the ModForge plugin.
 */
@State(
        name = "ModForgeSettings",
        storages = @Storage("ModForgeSettings.xml")
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    /**
     * Default server URL.
     */
    private static final String DEFAULT_SERVER_URL = "http://localhost:5000";
    
    private String serverUrl = DEFAULT_SERVER_URL;
    private String lastUsername = "";
    private boolean rememberMe = false;
    private boolean useDarkMode = true;
    private boolean enableContinuousDevelopment = false;
    private boolean enablePatternRecognition = true;
    private boolean enableGitHubIntegration = false;
    private String githubToken = "";
    private int maxApiRequestsPerDay = 100;

    /**
     * Get the settings instance.
     *
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
     * Get the server URL.
     *
     * @return The server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Set the server URL.
     *
     * @param serverUrl The server URL
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Get the last username.
     *
     * @return The last username
     */
    public String getLastUsername() {
        return lastUsername;
    }

    /**
     * Set the last username.
     *
     * @param lastUsername The last username
     */
    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
    }

    /**
     * Check if "Remember me" is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isRememberMe() {
        return rememberMe;
    }

    /**
     * Set if "Remember me" is enabled.
     *
     * @param rememberMe True to enable, false to disable
     */
    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    /**
     * Check if dark mode is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isUseDarkMode() {
        return useDarkMode;
    }

    /**
     * Set if dark mode is enabled.
     *
     * @param useDarkMode True to enable, false to disable
     */
    public void setUseDarkMode(boolean useDarkMode) {
        this.useDarkMode = useDarkMode;
    }

    /**
     * Check if continuous development is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopment;
    }

    /**
     * Set if continuous development is enabled.
     *
     * @param enableContinuousDevelopment True to enable, false to disable
     */
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        this.enableContinuousDevelopment = enableContinuousDevelopment;
    }

    /**
     * Check if pattern recognition is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnablePatternRecognition() {
        return enablePatternRecognition;
    }

    /**
     * Set if pattern recognition is enabled.
     *
     * @param enablePatternRecognition True to enable, false to disable
     */
    public void setEnablePatternRecognition(boolean enablePatternRecognition) {
        this.enablePatternRecognition = enablePatternRecognition;
    }

    /**
     * Check if GitHub integration is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnableGitHubIntegration() {
        return enableGitHubIntegration;
    }

    /**
     * Set if GitHub integration is enabled.
     *
     * @param enableGitHubIntegration True to enable, false to disable
     */
    public void setEnableGitHubIntegration(boolean enableGitHubIntegration) {
        this.enableGitHubIntegration = enableGitHubIntegration;
    }

    /**
     * Get the GitHub token.
     *
     * @return The GitHub token
     */
    public String getGithubToken() {
        return githubToken;
    }

    /**
     * Set the GitHub token.
     *
     * @param githubToken The GitHub token
     */
    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    /**
     * Get the maximum number of API requests per day.
     *
     * @return The maximum number of API requests per day
     */
    public int getMaxApiRequestsPerDay() {
        return maxApiRequestsPerDay;
    }

    /**
     * Set the maximum number of API requests per day.
     *
     * @param maxApiRequestsPerDay The maximum number of API requests per day
     */
    public void setMaxApiRequestsPerDay(int maxApiRequestsPerDay) {
        this.maxApiRequestsPerDay = maxApiRequestsPerDay;
    }

    /**
     * Reset settings to defaults.
     */
    public void resetToDefaults() {
        this.serverUrl = DEFAULT_SERVER_URL;
        this.lastUsername = "";
        this.rememberMe = false;
        this.useDarkMode = true;
        this.enableContinuousDevelopment = false;
        this.enablePatternRecognition = true;
        this.enableGitHubIntegration = false;
        this.githubToken = "";
        this.maxApiRequestsPerDay = 100;
    }
}