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
}