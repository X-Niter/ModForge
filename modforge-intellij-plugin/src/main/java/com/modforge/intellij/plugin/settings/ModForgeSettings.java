package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
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
    
    @Attribute("requestTimeout")
    private int requestTimeout = 30;
    
    @Attribute("enablePatternRecognition")
    private boolean enablePatternRecognition = true;
    
    @Attribute("enableContinuousDevelopment")
    private boolean enableContinuousDevelopment = false;
    
    // GitHub settings
    @Attribute("githubUsername")
    private String githubUsername = "";
    
    @Transient
    private String accessToken = "";
    
    /**
     * Default constructor.
     */
    public ModForgeSettings() {
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
     */
    @NotNull
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Sets the server URL.
     *
     * @param serverUrl The server URL.
     */
    public void setServerUrl(@NotNull String serverUrl) {
        this.serverUrl = serverUrl;
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

    /**
     * Gets the access token.
     *
     * @return The access token.
     */
    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     *
     * @param accessToken The access token.
     */
    public void setAccessToken(@Nullable String accessToken) {
        this.accessToken = accessToken;
    }
    
    /**
     * Opens the settings dialog.
     *
     * @param project The project.
     */
    public void openSettings(@Nullable Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeConfigurable.class);
    }
}