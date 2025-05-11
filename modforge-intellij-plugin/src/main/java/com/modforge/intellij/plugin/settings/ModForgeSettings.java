package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for ModForge.
 */
@Service
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = {@Storage("ModForgeSettings.xml")}
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String serverUrl = "https://modforge.com/api";
    private boolean useDarkMode = true;
    private boolean enableContinuousDevelopment = false;
    private boolean enablePatternRecognition = true;
    private boolean enableGitHubIntegration = false;
    private String githubToken = "";
    private String githubUsername = "";
    private String githubRepository = "";
    private boolean autoMonitorRepository = true;
    private boolean autoRespondToIssues = true;
    private int maxApiRequestsPerDay = 100;
    
    /**
     * Get the instance of ModForgeSettings.
     *
     * @return The instance
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }
    
    /**
     * Get the server URL.
     *
     * @return The server URL
     */
    @NotNull
    public String getServerUrl() {
        return serverUrl;
    }
    
    /**
     * Set the server URL.
     *
     * @param serverUrl The server URL
     */
    public void setServerUrl(@NotNull String serverUrl) {
        this.serverUrl = serverUrl;
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
    @NotNull
    public String getGithubToken() {
        return githubToken;
    }
    
    /**
     * Set the GitHub token.
     *
     * @param githubToken The GitHub token
     */
    public void setGithubToken(@NotNull String githubToken) {
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
     * Get the GitHub username.
     *
     * @return The GitHub username
     */
    @NotNull
    public String getGitHubUsername() {
        return githubUsername;
    }
    
    /**
     * Set the GitHub username.
     *
     * @param githubUsername The GitHub username
     */
    public void setGitHubUsername(@NotNull String githubUsername) {
        this.githubUsername = githubUsername;
    }
    
    /**
     * Get the GitHub repository.
     *
     * @return The GitHub repository
     */
    @NotNull
    public String getGitHubRepository() {
        return githubRepository;
    }
    
    /**
     * Set the GitHub repository.
     *
     * @param githubRepository The GitHub repository
     */
    public void setGitHubRepository(@NotNull String githubRepository) {
        this.githubRepository = githubRepository;
    }
    
    /**
     * Check if auto-monitoring of repositories is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isAutoMonitorRepository() {
        return autoMonitorRepository;
    }
    
    /**
     * Set if auto-monitoring of repositories is enabled.
     *
     * @param autoMonitorRepository True to enable, false to disable
     */
    public void setAutoMonitorRepository(boolean autoMonitorRepository) {
        this.autoMonitorRepository = autoMonitorRepository;
    }
    
    /**
     * Check if auto-responding to issues is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isAutoRespondToIssues() {
        return autoRespondToIssues;
    }
    
    /**
     * Set if auto-responding to issues is enabled.
     *
     * @param autoRespondToIssues True to enable, false to disable
     */
    public void setAutoRespondToIssues(boolean autoRespondToIssues) {
        this.autoRespondToIssues = autoRespondToIssues;
    }
    
    /**
     * Open the settings dialog.
     *
     * @param project The project
     */
    public void openSettings(@Nullable Project project) {
        if (project == null) {
            return;
        }
        
        ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                "ModForge AI Settings"
        );
    }
    
    /**
     * Reset settings to defaults.
     */
    public void resetToDefaults() {
        serverUrl = "https://modforge.com/api";
        useDarkMode = true;
        enableContinuousDevelopment = false;
        enablePatternRecognition = true;
        enableGitHubIntegration = false;
        githubToken = "";
        githubUsername = "";
        githubRepository = "";
        autoMonitorRepository = true;
        autoRespondToIssues = true;
        maxApiRequestsPerDay = 100;
    }
    
    @Nullable
    @Override
    public ModForgeSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * Check if pattern recognition is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isPatternRecognition() {
        return enablePatternRecognition;
    }
    
    /**
     * Get the access token for ModForge API access.
     *
     * @return The access token
     */
    public String getAccessToken() {
        // For backward compatibility, default to GitHub token if no separate access token is stored
        return githubToken;
    }
}