package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for ModForge plugin.
 */
@State(
    name = "ModForgeSettings",
    storages = {@Storage("ModForgeSettings.xml")}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private String serverUrl = "http://localhost:5000";
    private String username = "";
    private String password = "";
    private boolean rememberCredentials = true;
    private boolean authenticated = false;
    private String accessToken = "";
    private String userId = "";
    
    // Settings for continuous development
    private boolean enableContinuousDevelopment = true;
    private int continuousDevelopmentFrequency = 5; // minutes
    
    // Settings for AI-assisted development
    private boolean enableAIGeneration = true;
    private boolean usePatternLearning = true;
    
    // GitHub integration
    private String githubToken = "";
    private String githubUsername = "";
    
    /**
     * Get instance of settings.
     * @return The settings instance
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }
    
    @Override
    public @Nullable ModForgeSettings getState() {
        // Create a copy of the current state to save persistent settings
        ModForgeSettings state = new ModForgeSettings();
        state.serverUrl = this.serverUrl;
        
        // Only save sensitive data if rememberCredentials is true
        if (rememberCredentials) {
            state.username = this.username;
            state.password = this.password;
            state.accessToken = this.accessToken;
            state.userId = this.userId;
            state.authenticated = this.authenticated;
            state.githubToken = this.githubToken;
            state.githubUsername = this.githubUsername;
        } else {
            // Clear sensitive data
            state.username = "";
            state.password = "";
            state.accessToken = "";
            state.userId = "";
            state.authenticated = false;
            state.githubToken = "";
            state.githubUsername = "";
        }
        
        // Always save preferences
        state.rememberCredentials = this.rememberCredentials;
        state.enableContinuousDevelopment = this.enableContinuousDevelopment;
        state.continuousDevelopmentFrequency = this.continuousDevelopmentFrequency;
        state.enableAIGeneration = this.enableAIGeneration;
        state.usePatternLearning = this.usePatternLearning;
        
        return state;
    }
    
    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    // Server settings
    
    public String getServerUrl() {
        return serverUrl;
    }
    
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    // Authentication settings
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isRememberCredentials() {
        return rememberCredentials;
    }
    
    public void setRememberCredentials(boolean rememberCredentials) {
        this.rememberCredentials = rememberCredentials;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    // Continuous development settings
    
    public boolean isEnableContinuousDevelopment() {
        return enableContinuousDevelopment;
    }
    
    public void setEnableContinuousDevelopment(boolean enableContinuousDevelopment) {
        this.enableContinuousDevelopment = enableContinuousDevelopment;
    }
    
    public int getContinuousDevelopmentFrequency() {
        return continuousDevelopmentFrequency;
    }
    
    public void setContinuousDevelopmentFrequency(int continuousDevelopmentFrequency) {
        this.continuousDevelopmentFrequency = continuousDevelopmentFrequency;
    }
    
    // AI generation settings
    
    public boolean isEnableAIGeneration() {
        return enableAIGeneration;
    }
    
    public void setEnableAIGeneration(boolean enableAIGeneration) {
        this.enableAIGeneration = enableAIGeneration;
    }
    
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }
    
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }
    
    // GitHub integration
    
    public String getGithubToken() {
        return githubToken;
    }
    
    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }
    
    public String getGithubUsername() {
        return githubUsername;
    }
    
    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }
}