package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Settings for ModForge.
 * This class stores settings for ModForge, such as API keys and user preferences.
 */
@State(
        name = "ModForgeSettings",
        storages = @Storage("modforge.xml")
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private static final Logger LOG = Logger.getInstance(ModForgeSettings.class);
    
    // API settings
    private String openAiApiKey = "";
    private String openAiModel = "gpt-4";
    
    // User settings
    private String username = "";
    private String serverUrl = "https://api.modforge.dev";
    
    // Feature toggles
    private boolean enableAIAssist = true;
    private boolean usePatternRecognition = true;
    private boolean enableContinuousDevelopment = true;
    private boolean enableCollaborativeEditing = true;
    
    // Advanced settings
    private int maxCompletionTokens = 2000;
    private double similarityThreshold = 0.7;
    private int autoSaveIntervalSeconds = 5;
    
    /**
     * Gets the ModForgeSettings instance.
     * @return The ModForgeSettings instance
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
    
    // OpenAI API settings
    
    /**
     * Gets the OpenAI API key.
     * @return The OpenAI API key
     */
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    
    /**
     * Sets the OpenAI API key.
     * @param openAiApiKey The OpenAI API key
     */
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
    
    /**
     * Gets the OpenAI model.
     * @return The OpenAI model
     */
    public String getOpenAiModel() {
        return openAiModel;
    }
    
    /**
     * Sets the OpenAI model.
     * @param openAiModel The OpenAI model
     */
    public void setOpenAiModel(String openAiModel) {
        this.openAiModel = openAiModel;
    }
    
    // User settings
    
    /**
     * Gets the username.
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username.
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the server URL.
     * @return The server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }
    
    /**
     * Sets the server URL.
     * @param serverUrl The server URL
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    // Feature toggles
    
    /**
     * Gets whether AI assist is enabled.
     * @return Whether AI assist is enabled
     */
    public boolean isEnableAIAssist() {
        return enableAIAssist;
    }
    
    /**
     * Sets whether AI assist is enabled.
     * @param enableAIAssist Whether AI assist is enabled
     */
    public void setEnableAIAssist(boolean enableAIAssist) {
        this.enableAIAssist = enableAIAssist;
    }
    
    /**
     * Gets whether pattern recognition is used.
     * @return Whether pattern recognition is used
     */
    public boolean isUsePatternRecognition() {
        return usePatternRecognition;
    }
    
    /**
     * Sets whether pattern recognition is used.
     * @param usePatternRecognition Whether pattern recognition is used
     */
    public void setUsePatternRecognition(boolean usePatternRecognition) {
        this.usePatternRecognition = usePatternRecognition;
    }
    
    /**
     * Gets whether continuous development is enabled.
     * @return Whether continuous development is enabled
     */
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopment;
    }
    
    /**
     * Sets whether continuous development is enabled.
     * @param enableContinuousDevelopment Whether continuous development is enabled
     */
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        this.enableContinuousDevelopment = enableContinuousDevelopment;
    }
    
    /**
     * Gets whether collaborative editing is enabled.
     * @return Whether collaborative editing is enabled
     */
    public boolean isEnableCollaborativeEditing() {
        return enableCollaborativeEditing;
    }
    
    /**
     * Sets whether collaborative editing is enabled.
     * @param enableCollaborativeEditing Whether collaborative editing is enabled
     */
    public void setEnableCollaborativeEditing(boolean enableCollaborativeEditing) {
        this.enableCollaborativeEditing = enableCollaborativeEditing;
    }
    
    // Advanced settings
    
    /**
     * Gets the maximum completion tokens.
     * @return The maximum completion tokens
     */
    public int getMaxCompletionTokens() {
        return maxCompletionTokens;
    }
    
    /**
     * Sets the maximum completion tokens.
     * @param maxCompletionTokens The maximum completion tokens
     */
    public void setMaxCompletionTokens(int maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }
    
    /**
     * Gets the similarity threshold.
     * @return The similarity threshold
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    /**
     * Sets the similarity threshold.
     * @param similarityThreshold The similarity threshold
     */
    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
    
    /**
     * Gets the auto-save interval in seconds.
     * @return The auto-save interval in seconds
     */
    public int getAutoSaveIntervalSeconds() {
        return autoSaveIntervalSeconds;
    }
    
    /**
     * Sets the auto-save interval in seconds.
     * @param autoSaveIntervalSeconds The auto-save interval in seconds
     */
    public void setAutoSaveIntervalSeconds(int autoSaveIntervalSeconds) {
        this.autoSaveIntervalSeconds = autoSaveIntervalSeconds;
    }
}