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
 * This class is responsible for storing and retrieving plugin settings.
 */
@State(
        name = "ModForgeSettings",
        storages = {@Storage("modforge.xml")}
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String openAiApiKey = "";
    private String openAiModel = "gpt-4";
    private int maxTokens = 2048;
    private double temperature = 0.7;
    private boolean usePatternRecognition = true;
    private boolean syncWithWebEnabled = true;
    private String webSyncUrl = "";
    private String webSyncApiKey = "";
    
    /**
     * Gets the instance of the settings.
     * @return The settings
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
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
     * Gets the OpenAI API key.
     * @return The API key
     */
    @NotNull
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    
    /**
     * Sets the OpenAI API key.
     * @param openAiApiKey The API key
     */
    public void setOpenAiApiKey(@NotNull String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
    
    /**
     * Gets the OpenAI model.
     * @return The model
     */
    @NotNull
    public String getOpenAiModel() {
        return openAiModel;
    }
    
    /**
     * Sets the OpenAI model.
     * @param openAiModel The model
     */
    public void setOpenAiModel(@NotNull String openAiModel) {
        this.openAiModel = openAiModel;
    }
    
    /**
     * Gets the maximum tokens.
     * @return The maximum tokens
     */
    public int getMaxTokens() {
        return maxTokens;
    }
    
    /**
     * Sets the maximum tokens.
     * @param maxTokens The maximum tokens
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    /**
     * Gets the temperature.
     * @return The temperature
     */
    public double getTemperature() {
        return temperature;
    }
    
    /**
     * Sets the temperature.
     * @param temperature The temperature
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    /**
     * Checks if pattern recognition is enabled.
     * @return {@code true} if pattern recognition is enabled, {@code false} otherwise
     */
    public boolean isUsePatternRecognition() {
        return usePatternRecognition;
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param usePatternRecognition {@code true} to enable pattern recognition, {@code false} to disable it
     */
    public void setUsePatternRecognition(boolean usePatternRecognition) {
        this.usePatternRecognition = usePatternRecognition;
    }
    
    /**
     * Checks if sync with web is enabled.
     * @return {@code true} if sync with web is enabled, {@code false} otherwise
     */
    public boolean isSyncWithWebEnabled() {
        return syncWithWebEnabled;
    }
    
    /**
     * Sets whether sync with web is enabled.
     * @param syncWithWebEnabled {@code true} to enable sync with web, {@code false} to disable it
     */
    public void setSyncWithWebEnabled(boolean syncWithWebEnabled) {
        this.syncWithWebEnabled = syncWithWebEnabled;
    }
    
    /**
     * Gets the web sync URL.
     * @return The web sync URL
     */
    @NotNull
    public String getWebSyncUrl() {
        return webSyncUrl;
    }
    
    /**
     * Sets the web sync URL.
     * @param webSyncUrl The web sync URL
     */
    public void setWebSyncUrl(@NotNull String webSyncUrl) {
        this.webSyncUrl = webSyncUrl;
    }
    
    /**
     * Gets the web sync API key.
     * @return The web sync API key
     */
    @NotNull
    public String getWebSyncApiKey() {
        return webSyncApiKey;
    }
    
    /**
     * Sets the web sync API key.
     * @param webSyncApiKey The web sync API key
     */
    public void setWebSyncApiKey(@NotNull String webSyncApiKey) {
        this.webSyncApiKey = webSyncApiKey;
    }
}