package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;
import com.modforge.intellij.plugin.ui.ModForgeConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = {
                @Storage("modforge.xml")
        }
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private static final Logger LOG = Logger.getInstance(ModForgeSettings.class);
    
    // General settings
    @Attribute("serverUrl")
    private String serverUrl = "https://modforge.ai/api";
    
    @Attribute("collaborationServerUrl")
    private String collaborationServerUrl = "wss://modforge.ai/ws";
    
    @Attribute("requestTimeout")
    private int requestTimeout = 30;
    
    @Attribute("enablePatternRecognition")
    private boolean enablePatternRecognition = true;
    
    @Attribute("enableContinuousDevelopment")
    private boolean enableContinuousDevelopment = false;
    
    @Attribute("enableNotifications")
    private boolean enableNotifications = true;
    
    @Attribute("patternLearningRate")
    private double patternLearningRate = 0.5;
    
    // OpenAI settings
    @Attribute("openAiApiKey")
    private String openAiApiKey = "";
    
    @Attribute("openAiModel")
    private String openAiModel = "gpt-4o";
    
    @Attribute("maxTokens")
    private int maxTokens = 2048;
    
    @Attribute("temperature")
    private double temperature = 0.7;
    
    // GitHub settings
    @Attribute("githubUsername")
    private String githubUsername = "";
    
    @Attribute("username")
    private String username = "";
    
    @Transient
    private String accessToken = "";
    
    @Transient
    private String password = "";
    
    @Transient
    private String githubToken = "";
    
    @Transient
    private String token = "";
    
    // UI settings
    @Attribute("useDarkMode")
    private boolean useDarkMode = true;
    
    // GitHub integration settings
    @Attribute("enableGitHubIntegration")
    private boolean enableGitHubIntegration = false;
    
    @Attribute("gitHubRepository")
    private String gitHubRepository = "";
    
    @Attribute("autoMonitorRepository")
    private boolean autoMonitorRepository = false;
    
    @Attribute("autoRespondToIssues")
    private boolean autoRespondToIssues = false;
    
    @Attribute("maxApiRequestsPerDay")
    private int maxApiRequestsPerDay = 100;
    
    @Attribute("authenticated")
    private boolean authenticated = false;
    
    @Attribute("continuousDevelopmentScanInterval")
    private long continuousDevelopmentScanInterval = 60000; // Default to 1 minute
    
    /**
     * Default constructor.
     */
    public ModForgeSettings() {
    }
    
    /**
     * Gets the username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the password.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Checks if the user is authenticated based on access token.
     *
     * @return True if authenticated, false otherwise
     */
    public boolean isTokenAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }
    
    /**
     * Gets the access token.
     *
     * @return The access token
     */
    @Nullable
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * Sets the access token.
     *
     * @param accessToken The access token to set
     */
    public void setAccessToken(@Nullable String accessToken) {
        this.accessToken = accessToken;
    }
    
    /**
     * Gets the continuous development scan interval in milliseconds.
     *
     * @return The scan interval
     */
    public long getContinuousDevelopmentScanInterval() {
        return continuousDevelopmentScanInterval;
    }

    /**
     * Sets the continuous development scan interval in milliseconds.
     *
     * @param interval The scan interval to set
     */
    public void setContinuousDevelopmentScanInterval(long interval) {
        this.continuousDevelopmentScanInterval = interval;
    }
    
    /**
     * Gets the server URL.
     *
     * @return The server URL
     * @deprecated Use the annotated version {@link #getServerUrl()} with NotNull annotation
     */
    @Deprecated
    public String getServerUrlLegacy() {
        return serverUrl;
    }
    
    /**
     * Sets the server URL.
     *
     * @param serverUrl The server URL to set
     * @deprecated Use the annotated version {@link #setServerUrl(String)} with NotNull annotation
     */
    @Deprecated
    public void setServerUrlLegacy(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    /**
     * Gets the collaboration server URL.
     *
     * @return The collaboration server URL
     */
    public String getCollaborationServerUrl() {
        return collaborationServerUrl;
    }
    
    /**
     * Sets the collaboration server URL.
     *
     * @param collaborationServerUrl The collaboration server URL to set
     */
    public void setCollaborationServerUrl(String collaborationServerUrl) {
        this.collaborationServerUrl = collaborationServerUrl;
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     *
     * @param enabled True to enable, false to disable
     */
    public void setEnablePatternRecognition(boolean enabled) {
        this.enablePatternRecognition = enabled;
    }

    /**
     * Gets the instance of the settings.
     *
     * @return The settings instance.
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }
    
    /**
     * Gets the instance of the settings for a specific project.
     * Note: ModForgeSettings is an application-level service, so this ignores the project parameter.
     * This method is provided for compatibility with IntelliJ IDEA 2025.1.1.1.
     *
     * @param project The project (ignored).
     * @return The settings instance.
     */
    public static ModForgeSettings getInstance(@NotNull Project project) {
        return getInstance();
    }

    /**
     * Gets the state.
     *
     * @return The state.
     */
    @Nullable
    @Override
    public ModForgeSettings getState() {
        return this;
    }

    /**
     * Loads the state.
     *
     * @param state The state to load.
     */
    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * Gets the server URL.
     *
     * @return The server URL.
     * @deprecated Use {@link #getServerUrlLegacy()} instead to avoid duplicate method issues
     */
    @Deprecated
    @NotNull
    public String getServerUrl() {
        return getServerUrlLegacy();
    }

    /**
     * Sets the server URL.
     *
     * @param serverUrl The server URL.
     * @deprecated Use {@link #setServerUrlLegacy(String)} instead to avoid duplicate method issues
     */
    @Deprecated
    public void setServerUrl(@NotNull String serverUrl) {
        setServerUrlLegacy(serverUrl);
    }

    /**
     * Gets the request timeout in seconds.
     *
     * @return The request timeout.
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Sets the request timeout in seconds.
     *
     * @param requestTimeout The request timeout.
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Checks if pattern recognition is enabled.
     *
     * @return Whether pattern recognition is enabled.
     */
    public boolean isPatternRecognition() {
        return enablePatternRecognition;
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * Compatibility method for code that expects this method name.
     *
     * @return Whether pattern recognition is enabled.
     * @deprecated Use {@link #isPatternRecognition()} instead for consistency
     */
    @Deprecated
    public boolean isPatternRecognitionEnabled() {
        return isPatternRecognition();
    }

    /**
     * Sets whether pattern recognition is enabled.
     *
     * @param enablePatternRecognition Whether pattern recognition is enabled.
     */
    public void setPatternRecognition(boolean enablePatternRecognition) {
        this.enablePatternRecognition = enablePatternRecognition;
    }

    /**
     * Checks if continuous development is enabled.
     *
     * @return Whether continuous development is enabled.
     */
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopment;
    }

    /**
     * Sets whether continuous development is enabled.
     *
     * @param enableContinuousDevelopment Whether continuous development is enabled.
     */
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        this.enableContinuousDevelopment = enableContinuousDevelopment;
    }
    
    /**
     * Checks if notifications are enabled.
     *
     * @return Whether notifications are enabled.
     */
    public boolean isEnableNotifications() {
        return enableNotifications;
    }

    /**
     * Sets whether notifications are enabled.
     *
     * @param enableNotifications Whether notifications are enabled.
     */
    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    /**
     * Gets the GitHub username.
     *
     * @return The GitHub username.
     */
    @NotNull
    public String getGitHubUsername() {
        return githubUsername != null ? githubUsername : "";
    }

    /**
     * Sets the GitHub username.
     *
     * @param githubUsername The GitHub username.
     */
    public void setGitHubUsername(@Nullable String githubUsername) {
        this.githubUsername = githubUsername;
    }

    /* Removed duplicate getAccessToken and setAccessToken methods */
    
    /**
     * Gets the GitHub token.
     *
     * @return The GitHub token.
     */
    @Nullable
    public String getGitHubToken() {
        return githubToken;
    }

    /**
     * Sets the GitHub token.
     *
     * @param githubToken The GitHub token.
     */
    public void setGitHubToken(@Nullable String githubToken) {
        this.githubToken = githubToken;
    }
    
    /**
     * Opens the settings dialog.
     *
     * @param project The project.
     */
    public void openSettings(@Nullable Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
    }
    
    /**
     * Resets all settings to defaults.
     */
    public void resetToDefaults() {
        serverUrl = "https://modforge.ai/api";
        collaborationServerUrl = "wss://modforge.ai/ws";
        requestTimeout = 30;
        enablePatternRecognition = true;
        enableContinuousDevelopment = false;
        enableNotifications = true;
        patternLearningRate = 0.5;
        
        openAiApiKey = "";
        openAiModel = "gpt-4o";
        maxTokens = 2048;
        temperature = 0.7;
        
        githubUsername = "";
        githubToken = "";
    }
    
    /**
     * Gets the OpenAI API key.
     *
     * @return The OpenAI API key.
     */
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    /**
     * Sets the OpenAI API key.
     *
     * @param openAiApiKey The OpenAI API key.
     */
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }

    /**
     * Gets the OpenAI model.
     *
     * @return The OpenAI model.
     */
    public String getOpenAiModel() {
        return openAiModel;
    }

    /**
     * Sets the OpenAI model.
     *
     * @param openAiModel The OpenAI model.
     */
    public void setOpenAiModel(String openAiModel) {
        this.openAiModel = openAiModel;
    }

    /**
     * Gets the maximum number of tokens for OpenAI requests.
     *
     * @return The maximum number of tokens.
     */
    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * Sets the maximum number of tokens for OpenAI requests.
     *
     * @param maxTokens The maximum number of tokens.
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * Gets the temperature for OpenAI requests.
     *
     * @return The temperature.
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Sets the temperature for OpenAI requests.
     *
     * @param temperature The temperature.
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    /**
     * Gets the pattern learning rate.
     * This controls how aggressively the system learns from patterns.
     *
     * @return The pattern learning rate between 0.0 and 1.0.
     */
    public double getPatternLearningRate() {
        return patternLearningRate;
    }

    /**
     * Sets the pattern learning rate.
     * This controls how aggressively the system learns from patterns.
     *
     * @param patternLearningRate The pattern learning rate between 0.0 and 1.0.
     */
    public void setPatternLearningRate(double patternLearningRate) {
        this.patternLearningRate = Math.max(0.0, Math.min(1.0, patternLearningRate));
    }
    
    /**
     * Checks if dark mode is enabled.
     *
     * @return Whether dark mode is enabled.
     */
    public boolean isUseDarkMode() {
        return useDarkMode;
    }

    /**
     * Sets whether dark mode is enabled.
     *
     * @param useDarkMode Whether dark mode is enabled.
     */
    public void setUseDarkMode(boolean useDarkMode) {
        this.useDarkMode = useDarkMode;
    }
    
    /**
     * Checks if GitHub integration is enabled.
     *
     * @return Whether GitHub integration is enabled.
     */
    public boolean isEnableGitHubIntegration() {
        return enableGitHubIntegration;
    }

    /**
     * Sets whether GitHub integration is enabled.
     *
     * @param enableGitHubIntegration Whether GitHub integration is enabled.
     */
    public void setEnableGitHubIntegration(boolean enableGitHubIntegration) {
        this.enableGitHubIntegration = enableGitHubIntegration;
    }
    
    /**
     * Gets the GitHub token.
     * Compatibility method for older code that expects this method name.
     *
     * @return The GitHub token.
     */
    @NotNull
    public String getGithubToken() {
        return githubToken != null ? githubToken : "";
    }

    /**
     * Sets the GitHub token.
     * Compatibility method for older code that expects this method name.
     *
     * @param githubToken The GitHub token.
     */
    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }
    
    /**
     * Gets the authentication token.
     *
     * @return The authentication token
     */
    @NotNull
    public String getToken() {
        return token != null ? token : "";
    }
    
    /**
     * Sets the authentication token.
     *
     * @param token The authentication token
     */
    public void setToken(@Nullable String token) {
        this.token = token;
    }
    
    /**
     * Gets the GitHub repository.
     *
     * @return The GitHub repository.
     */
    @NotNull
    public String getGitHubRepository() {
        return gitHubRepository != null ? gitHubRepository : "";
    }

    /**
     * Sets the GitHub repository.
     *
     * @param gitHubRepository The GitHub repository.
     */
    public void setGitHubRepository(@NotNull String gitHubRepository) {
        this.gitHubRepository = gitHubRepository;
    }
    
    /**
     * Checks if automatic repository monitoring is enabled.
     *
     * @return Whether automatic repository monitoring is enabled.
     */
    public boolean isAutoMonitorRepository() {
        return autoMonitorRepository;
    }

    /**
     * Sets whether automatic repository monitoring is enabled.
     *
     * @param autoMonitorRepository Whether automatic repository monitoring is enabled.
     */
    public void setAutoMonitorRepository(boolean autoMonitorRepository) {
        this.autoMonitorRepository = autoMonitorRepository;
    }
    
    /**
     * Checks if automatic issue responses are enabled.
     *
     * @return Whether automatic issue responses are enabled.
     */
    public boolean isAutoRespondToIssues() {
        return autoRespondToIssues;
    }

    /**
     * Sets whether automatic issue responses are enabled.
     *
     * @param autoRespondToIssues Whether automatic issue responses are enabled.
     */
    public void setAutoRespondToIssues(boolean autoRespondToIssues) {
        this.autoRespondToIssues = autoRespondToIssues;
    }
    
    /**
     * Gets the maximum number of API requests per day.
     *
     * @return The maximum number of API requests per day.
     */
    public int getMaxApiRequestsPerDay() {
        return maxApiRequestsPerDay;
    }

    /**
     * Sets the maximum number of API requests per day.
     *
     * @param maxApiRequestsPerDay The maximum number of API requests per day.
     */
    public void setMaxApiRequestsPerDay(int maxApiRequestsPerDay) {
        this.maxApiRequestsPerDay = maxApiRequestsPerDay;
    }
    
    /**
     * Checks if the user is authenticated.
     * This is a compatibility method for older code that expects this method name.
     *
     * @return Whether the user is authenticated.
     */
    public boolean isAuthenticated() {
        return authenticated || (token != null && !token.isEmpty());
    }

    /**
     * Sets whether the user is authenticated.
     *
     * @param authenticated Whether the user is authenticated.
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    /**
     * Check if pattern recognition is enabled.
     * This is a convenience method alias for isPatternRecognition().
     *
     * @return Whether pattern recognition is enabled.
     */
    public boolean isEnablePatternRecognition() {
        return isPatternRecognition();
    }
}