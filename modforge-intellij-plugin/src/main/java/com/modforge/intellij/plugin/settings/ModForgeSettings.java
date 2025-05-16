package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
@State(name = "com.modforge.intellij.plugin.settings.ModForgeSettings", storages = {
        @Storage("modforge.xml")
})
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // API settings
    @Attribute("apiUrl")
    private String apiUrl = "https://modforge.ai/api";

    @Attribute("token")
    private String token = "";

    @Attribute("enablePatternRecognition")
    private boolean patternRecognition = true;

    @Attribute("enableContinuousDevelopment")
    private boolean enableContinuousDevelopment = false;

    @Attribute("continuousDevelopmentScanInterval")
    private long continuousDevelopmentScanInterval = 60000; // 1 minute default

    @Attribute("apiKey")
    private String apiKey = "";

    @Attribute("username")
    private String username = "";

    @Attribute("gitHubUsername")
    private String gitHubUsername = "";

    @Attribute("password")
    private String password = "";

    @Attribute("openAiApiKey")
    private String openAiApiKey = "";

    @Attribute("openAiModel")
    private String openAiModel = "gpt-4-turbo";

    @Attribute("maxTokens")
    private int maxTokens = 4096;

    @Attribute("temperature")
    private double temperature = 0.7;

    @Attribute("authenticated")
    private boolean authenticated = false;

    @Attribute("userId")
    private String userId = "";

    // Notification settings
    @Attribute("enableNotifications")
    private boolean enableNotifications = true;

    // Additional settings fields
    @Attribute("useDarkMode")
    private boolean useDarkMode = false;

    @Attribute("enableGitHubIntegration")
    private boolean enableGitHubIntegration = false;

    @Attribute("autoMonitorRepository")
    private boolean autoMonitorRepository = false;

    @Attribute("autoRespondToIssues")
    private boolean autoRespondToIssues = false;

    @Attribute("maxApiRequestsPerDay")
    private int maxApiRequestsPerDay = 100;

    @Attribute("forgeSupported")
    private boolean forgeSupported = true;

    @Attribute("fabricSupported")
    private boolean fabricSupported = false;

    @Attribute("quiltSupported")
    private boolean quiltSupported = false;

    @Attribute("checkIntervalMs")
    private long checkIntervalMs = 60000;

    @Attribute("updateFrequencyMinutes")
    private int updateFrequencyMinutes = 10;

    @Attribute("githubToken")
    private String githubToken = "";

    @Attribute("githubRepository")
    private String githubRepository = "";

    @Attribute("enableAIAssist")
    private boolean enableAIAssist = false;

    @Attribute("usePatternRecognition")
    private boolean usePatternRecognition = false;

    @Attribute("rememberCredentials")
    private boolean rememberCredentials = false;

    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }

    /**
     * Gets the instance of ModForgeSettings for a given project.
     *
     * @param project The project
     * @return The ModForgeSettings instance
     */
    public static ModForgeSettings getInstance(@NotNull Project project) {
        return project.getService(ModForgeSettings.class);
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

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isPatternRecognition() {
        return patternRecognition;
    }

    public void setPatternRecognition(boolean patternRecognition) {
        this.patternRecognition = patternRecognition;
    }

    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopment;
    }

    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        this.enableContinuousDevelopment = enableContinuousDevelopment;
    }

    public long getContinuousDevelopmentScanInterval() {
        return continuousDevelopmentScanInterval;
    }

    public void setContinuousDevelopmentScanInterval(long intervalMs) {
        this.continuousDevelopmentScanInterval = intervalMs;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Nullable
    public String getGitHubUsername() {
        return gitHubUsername;
    }

    public void setGitHubUsername(String username) {
        this.gitHubUsername = username;
    }

    public boolean isAuthenticated() {
        return !getAccessToken().isEmpty();
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEnablePatternRecognition(boolean enabled) {
        this.patternRecognition = enabled;
    }

    @NotNull
    public String getServerUrl() {
        return apiUrl; // Using existing apiUrl field
    }

    @NotNull
    public String getAccessToken() {
        return token; // Using existing token field
    }

    public void setServerUrl(String serverUrl) {
        this.apiUrl = serverUrl;
    }

    public void setAccessToken(String accessToken) {
        this.token = accessToken;
    }

    @NotNull
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public void setOpenAiApiKey(String apiKey) {
        this.openAiApiKey = apiKey;
    }

    @NotNull
    public String getOpenAiModel() {
        return openAiModel;
    }

    public void setOpenAiModel(String model) {
        this.openAiModel = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int tokens) {
        this.maxTokens = tokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temp) {
        this.temperature = temp;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEnableNotifications() {
        return enableNotifications;
    }

    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    public boolean isUseDarkMode() {
        return useDarkMode;
    }

    public void setUseDarkMode(boolean useDarkMode) {
        this.useDarkMode = useDarkMode;
    }

    public boolean isEnableGitHubIntegration() {
        return enableGitHubIntegration;
    }

    public void setEnableGitHubIntegration(boolean enableGitHubIntegration) {
        this.enableGitHubIntegration = enableGitHubIntegration;
    }

    public boolean isAutoMonitorRepository() {
        return autoMonitorRepository;
    }

    public void setAutoMonitorRepository(boolean autoMonitorRepository) {
        this.autoMonitorRepository = autoMonitorRepository;
    }

    public boolean isAutoRespondToIssues() {
        return autoRespondToIssues;
    }

    public void setAutoRespondToIssues(boolean autoRespondToIssues) {
        this.autoRespondToIssues = autoRespondToIssues;
    }

    public int getMaxApiRequestsPerDay() {
        return maxApiRequestsPerDay;
    }

    public void setMaxApiRequestsPerDay(int maxApiRequestsPerDay) {
        this.maxApiRequestsPerDay = maxApiRequestsPerDay;
    }

    public boolean isForgeSupported() {
        return forgeSupported;
    }

    public void setForgeSupported(boolean forgeSupported) {
        this.forgeSupported = forgeSupported;
    }

    public boolean isFabricSupported() {
        return fabricSupported;
    }

    public void setFabricSupported(boolean fabricSupported) {
        this.fabricSupported = fabricSupported;
    }

    public boolean isQuiltSupported() {
        return quiltSupported;
    }

    public void setQuiltSupported(boolean quiltSupported) {
        this.quiltSupported = quiltSupported;
    }

    public long getCheckIntervalMs() {
        return checkIntervalMs;
    }

    public void setCheckIntervalMs(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }

    public int getUpdateFrequencyMinutes() {
        return updateFrequencyMinutes;
    }

    public void setUpdateFrequencyMinutes(int updateFrequencyMinutes) {
        this.updateFrequencyMinutes = updateFrequencyMinutes;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public String getGitHubRepository() {
        return githubRepository;
    }

    public void setGitHubRepository(String githubRepository) {
        this.githubRepository = githubRepository;
    }

    public boolean isEnableAIAssist() {
        return enableAIAssist;
    }

    public void setEnableAIAssist(boolean enableAIAssist) {
        this.enableAIAssist = enableAIAssist;
    }

    public boolean isUsePatternRecognition() {
        return usePatternRecognition;
    }

    public void setUsePatternRecognition(boolean usePatternRecognition) {
        this.usePatternRecognition = usePatternRecognition;
    }

    public boolean isRememberCredentials() {
        return rememberCredentials;
    }

    public void setRememberCredentials(boolean rememberCredentials) {
        this.rememberCredentials = rememberCredentials;
    }

    /**
     * Shows the settings dialog.
     *
     * @param project The project
     */
    public static void showSettings(@Nullable Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "ModForge Settings");
    }

    /**
     * Opens the ModForge settings dialog.
     *
     * @param project The current project
     */
    public void openSettings(@NotNull Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                "ModForge AI");
    }

    public String getGitHubToken() {
        // If you store a GitHub token, return it here. Otherwise, return token.
        return token;
    }

    public String getCollaborationServerUrl() {
        // If you have a dedicated field, return it. Otherwise, return apiUrl.
        return apiUrl;
    }

    // Aliases for compatibility with usages across the codebase
    public boolean isEnablePatternRecognition() {
        return isPatternRecognition();
    }

    public boolean isContinuousDevelopmentEnabled() {
        return isEnableContinuousDevelopment();
    }

    public void setContinuousDevelopmentEnabled(boolean enabled) {
        setEnableContinuousDevelopment(enabled);
    }

    public boolean isPatternRecognitionEnabled() {
        return isPatternRecognition();
    }

    public void setPatternRecognitionEnabled(boolean enabled) {
        setPatternRecognition(enabled);
    }

    // Reset all settings to their default values
    public void resetToDefaults() {
        apiUrl = "https://modforge.ai/api";
        token = "";
        patternRecognition = true;
        enableContinuousDevelopment = false;
        continuousDevelopmentScanInterval = 60000;
        apiKey = "";
        username = "";
        gitHubUsername = "";
        password = "";
        openAiApiKey = "";
        openAiModel = "gpt-4-turbo";
        maxTokens = 4096;
        temperature = 0.7;
        authenticated = false;
        userId = "";
        enableNotifications = true;
        useDarkMode = false;
        enableGitHubIntegration = false;
        autoMonitorRepository = false;
        autoRespondToIssues = false;
        maxApiRequestsPerDay = 100;
        forgeSupported = true;
        fabricSupported = false;
        quiltSupported = false;
        checkIntervalMs = 60000;
        updateFrequencyMinutes = 10;
        githubToken = "";
        githubRepository = "";
        enableAIAssist = false;
        usePatternRecognition = false;
        rememberCredentials = false;
    }

    public void setGitHubToken(String token) {
        this.githubToken = token;
    }

    public void setAccessToken(String token) {
        this.token = token;
    }

    public void setCollaborationServerUrl(String url) {
        /* compatibility stub */ }

    public String getGitExecutablePath() {
        return "git";
    }

    public boolean isContinuousDevelopment() {
        return enableContinuousDevelopment;
    }

    public double getPatternMatchingThreshold() {
        return 0.8;
    }

    public java.util.List<Object> getMemoryHistory() {
        return java.util.Collections.emptyList();
    }
}