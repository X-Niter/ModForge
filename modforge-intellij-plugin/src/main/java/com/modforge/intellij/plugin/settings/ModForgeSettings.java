package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 */
@State(
        name = "ModForgeSettings",
        storages = {@Storage("modforge-settings.xml")}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String openAiApiKey = "";
    private String username = "anonymous";
    private String collaborationServerUrl = "wss://modforge.io/collab";
    private boolean enableContinuousRefactoring = false;
    private boolean enableAIAssist = true;
    private int maxTokensPerRequest = 1024;
    private String openAiModel = "gpt-4";
    private boolean usePatternRecognition = true;
    private int collaborationRefreshRate = 500; // milliseconds
    
    /**
     * Gets the ModForgeSettings instance.
     * @return The ModForgeSettings instance
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
     * Gets the username.
     * @return The username
     */
    @NotNull
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username.
     * @param username The username
     */
    public void setUsername(@NotNull String username) {
        this.username = username;
    }
    
    /**
     * Gets the collaboration server URL.
     * @return The collaboration server URL
     */
    @NotNull
    public String getCollaborationServerUrl() {
        return collaborationServerUrl;
    }
    
    /**
     * Sets the collaboration server URL.
     * @param collaborationServerUrl The collaboration server URL
     */
    public void setCollaborationServerUrl(@NotNull String collaborationServerUrl) {
        this.collaborationServerUrl = collaborationServerUrl;
    }
    
    /**
     * Gets whether continuous refactoring is enabled.
     * @return Whether continuous refactoring is enabled
     */
    public boolean isEnableContinuousRefactoring() {
        return enableContinuousRefactoring;
    }
    
    /**
     * Sets whether continuous refactoring is enabled.
     * @param enableContinuousRefactoring Whether continuous refactoring is enabled
     */
    public void setEnableContinuousRefactoring(boolean enableContinuousRefactoring) {
        this.enableContinuousRefactoring = enableContinuousRefactoring;
    }
    
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
     * Gets the maximum tokens per request.
     * @return The maximum tokens per request
     */
    public int getMaxTokensPerRequest() {
        return maxTokensPerRequest;
    }
    
    /**
     * Sets the maximum tokens per request.
     * @param maxTokensPerRequest The maximum tokens per request
     */
    public void setMaxTokensPerRequest(int maxTokensPerRequest) {
        this.maxTokensPerRequest = maxTokensPerRequest;
    }
    
    /**
     * Gets the OpenAI model.
     * @return The OpenAI model
     */
    @NotNull
    public String getOpenAiModel() {
        return openAiModel;
    }
    
    /**
     * Sets the OpenAI model.
     * @param openAiModel The OpenAI model
     */
    public void setOpenAiModel(@NotNull String openAiModel) {
        this.openAiModel = openAiModel;
    }
    
    /**
     * Gets whether pattern recognition is enabled.
     * @return Whether pattern recognition is enabled
     */
    public boolean isUsePatternRecognition() {
        return usePatternRecognition;
    }
    
    /**
     * Sets whether pattern recognition is enabled.
     * @param usePatternRecognition Whether pattern recognition is enabled
     */
    public void setUsePatternRecognition(boolean usePatternRecognition) {
        this.usePatternRecognition = usePatternRecognition;
    }
    
    /**
     * Gets the collaboration refresh rate.
     * @return The collaboration refresh rate
     */
    public int getCollaborationRefreshRate() {
        return collaborationRefreshRate;
    }
    
    /**
     * Sets the collaboration refresh rate.
     * @param collaborationRefreshRate The collaboration refresh rate
     */
    public void setCollaborationRefreshRate(int collaborationRefreshRate) {
        this.collaborationRefreshRate = collaborationRefreshRate;
    }
}