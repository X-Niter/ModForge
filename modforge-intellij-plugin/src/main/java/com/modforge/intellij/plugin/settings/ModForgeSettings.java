package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 * This class is responsible for storing and retrieving plugin settings.
 */
@State(
        name = "ModForgeSettings",
        storages = @Storage("modforge.xml")
)
@Service(Service.Level.APP)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String openAiApiKey = "";
    private boolean continuousDevelopmentEnabled = false;
    private long continuousDevelopmentInterval = 60_000; // 1 minute
    private boolean patternRecognitionEnabled = true;
    private boolean syncWithWebEnabled = false;
    private String webApiUrl = "https://modforge.io/api";
    private String webApiKey = "";
    
    /**
     * Gets the ModForge settings.
     * @return The ModForge settings
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }
    
    /**
     * Gets the OpenAI API key.
     * @return The OpenAI API key
     */
    @NotNull
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    
    /**
     * Sets the OpenAI API key.
     * @param openAiApiKey The OpenAI API key
     */
    public void setOpenAiApiKey(@NotNull String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
    
    /**
     * Checks if continuous development is enabled.
     * @return Whether continuous development is enabled
     */
    public boolean isContinuousDevelopmentEnabled() {
        return continuousDevelopmentEnabled;
    }
    
    /**
     * Sets whether continuous development is enabled.
     * @param enabled Whether continuous development is enabled
     */
    public void setContinuousDevelopmentEnabled(boolean enabled) {
        this.continuousDevelopmentEnabled = enabled;
    }
    
    /**
     * Gets the continuous development interval.
     * @return The continuous development interval in milliseconds
     */
    public long getContinuousDevelopmentInterval() {
        return continuousDevelopmentInterval;
    }
    
    /**
     * Sets the continuous development interval.
     * @param intervalMs The continuous development interval in milliseconds
     */
    public void setContinuousDevelopmentInterval(long intervalMs) {
        this.continuousDevelopmentInterval = intervalMs;
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return Whether pattern recognition is enabled
     */
    public boolean isPatternRecognitionEnabled() {
        return patternRecognitionEnabled;
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param enabled Whether pattern recognition is enabled
     */
    public void setPatternRecognitionEnabled(boolean enabled) {
        this.patternRecognitionEnabled = enabled;
    }
    
    /**
     * Checks if sync with web is enabled.
     * @return Whether sync with web is enabled
     */
    public boolean isSyncWithWebEnabled() {
        return syncWithWebEnabled;
    }
    
    /**
     * Sets whether sync with web is enabled.
     * @param enabled Whether sync with web is enabled
     */
    public void setSyncWithWebEnabled(boolean enabled) {
        this.syncWithWebEnabled = enabled;
    }
    
    /**
     * Gets the web API URL.
     * @return The web API URL
     */
    @NotNull
    public String getWebApiUrl() {
        return webApiUrl;
    }
    
    /**
     * Sets the web API URL.
     * @param webApiUrl The web API URL
     */
    public void setWebApiUrl(@NotNull String webApiUrl) {
        this.webApiUrl = webApiUrl;
    }
    
    /**
     * Gets the web API key.
     * @return The web API key
     */
    @NotNull
    public String getWebApiKey() {
        return webApiKey;
    }
    
    /**
     * Sets the web API key.
     * @param webApiKey The web API key
     */
    public void setWebApiKey(@NotNull String webApiKey) {
        this.webApiKey = webApiKey;
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
}