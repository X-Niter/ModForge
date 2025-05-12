package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.modforge.intellij.plugin.ui.ModForgeConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
@State(
        name = "ModForgeSettings",
        storages = {@Storage("modForgeSettings.xml")}
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String username = "";
    private String serverUrl = "https://api.modforge.ai";
    private String gitHubUsername = "";
    private String accessToken = "";
    private boolean enableContinuousDevelopment = false;
    private boolean usePatternLearning = true;
    private boolean preserveExistingCode = true;
    private boolean patternRecognition = true;
    private int memoryThreshold = 75;

    /**
     * Gets the instance of the settings.
     *
     * @return The settings.
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
     * Opens the settings dialog.
     *
     * @param project The project.
     */
    public void openSettings(@NotNull Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeConfigurable.class);
    }

    /**
     * Gets the username.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the server URL.
     *
     * @return The server URL.
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Sets the server URL.
     *
     * @param serverUrl The server URL.
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Gets the GitHub username.
     *
     * @return The GitHub username.
     */
    public String getGitHubUsername() {
        return gitHubUsername;
    }

    /**
     * Sets the GitHub username.
     *
     * @param gitHubUsername The GitHub username.
     */
    public void setGitHubUsername(String gitHubUsername) {
        this.gitHubUsername = gitHubUsername;
    }

    /**
     * Gets the access token.
     *
     * @return The access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     *
     * @param accessToken The access token.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Gets whether continuous development is enabled.
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
     * Gets whether pattern learning is enabled.
     *
     * @return Whether pattern learning is enabled.
     */
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }

    /**
     * Sets whether pattern learning is enabled.
     *
     * @param usePatternLearning Whether pattern learning is enabled.
     */
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }

    /**
     * Gets whether existing code should be preserved.
     *
     * @return Whether existing code should be preserved.
     */
    public boolean isPreserveExistingCode() {
        return preserveExistingCode;
    }

    /**
     * Sets whether existing code should be preserved.
     *
     * @param preserveExistingCode Whether existing code should be preserved.
     */
    public void setPreserveExistingCode(boolean preserveExistingCode) {
        this.preserveExistingCode = preserveExistingCode;
    }

    /**
     * Gets whether pattern recognition is enabled.
     *
     * @return Whether pattern recognition is enabled.
     */
    public boolean isPatternRecognition() {
        return patternRecognition;
    }

    /**
     * Sets whether pattern recognition is enabled.
     *
     * @param patternRecognition Whether pattern recognition is enabled.
     */
    public void setPatternRecognition(boolean patternRecognition) {
        this.patternRecognition = patternRecognition;
    }

    /**
     * Gets the memory threshold.
     *
     * @return The memory threshold.
     */
    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    /**
     * Sets the memory threshold.
     *
     * @param memoryThreshold The memory threshold.
     */
    public void setMemoryThreshold(int memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }
}