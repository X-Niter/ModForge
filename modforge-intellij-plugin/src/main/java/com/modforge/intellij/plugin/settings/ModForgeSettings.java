package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = {@Storage(StoragePathMacros.NON_ROAMABLE_FILE)}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // User settings
    private String username = "";
    private String serverUrl = "https://api.modforge.dev";
    private boolean enableContinuousDevelopment = false;
    private int memoryThreshold = 75; // Percentage
    
    // AI settings
    private boolean usePatternLearning = true;
    private boolean preserveExistingCode = true;

    /**
     * Gets the singleton instance of the settings.
     *
     * @return The settings instance.
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
     * Checks if continuous development is enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopment;
    }

    /**
     * Sets whether continuous development is enabled.
     *
     * @param enableContinuousDevelopment True to enable, false to disable.
     */
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        this.enableContinuousDevelopment = enableContinuousDevelopment;
    }

    /**
     * Gets the memory threshold.
     *
     * @return The memory threshold (percentage).
     */
    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    /**
     * Sets the memory threshold.
     *
     * @param memoryThreshold The memory threshold (percentage).
     */
    public void setMemoryThreshold(int memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }

    /**
     * Checks if pattern learning is enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }

    /**
     * Sets whether pattern learning is enabled.
     *
     * @param usePatternLearning True to enable, false to disable.
     */
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }

    /**
     * Checks if existing code should be preserved.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isPreserveExistingCode() {
        return preserveExistingCode;
    }

    /**
     * Sets whether existing code should be preserved.
     *
     * @param preserveExistingCode True to enable, false to disable.
     */
    public void setPreserveExistingCode(boolean preserveExistingCode) {
        this.preserveExistingCode = preserveExistingCode;
    }
}