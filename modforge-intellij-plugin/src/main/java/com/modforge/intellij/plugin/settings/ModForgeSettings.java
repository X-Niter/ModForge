package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 */
@State(
    name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
    storages = {@Storage("ModForgeSettings.xml")}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // AI settings
    private String openAiApiKey = "";
    private int maxTokensPerRequest = 1000;
    private boolean usePatternLearning = true;
    
    // Collaboration settings
    private String collaborationServerUrl = "wss://modforge.io/ws/collaboration";
    private String username = "";
    
    // Code generation settings
    private boolean generateJavadoc = true;
    private boolean addCopyrightHeader = true;
    private String copyrightText = "Copyright (c) ${YEAR} ModForge Team";
    
    // UI settings
    private boolean showMetricsInStatusBar = true;
    private boolean enableNotifications = true;

    /**
     * Gets the instance of the settings.
     * @return The settings instance
     */
    public static ModForgeSettings getInstance() {
        return ServiceManager.getService(ModForgeSettings.class);
    }

    @Override
    @Nullable
    public ModForgeSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    // OpenAI API key
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
    
    // Max tokens per request
    public int getMaxTokensPerRequest() {
        return maxTokensPerRequest;
    }
    
    public void setMaxTokensPerRequest(int maxTokensPerRequest) {
        this.maxTokensPerRequest = maxTokensPerRequest;
    }
    
    // Use pattern learning
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }
    
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }
    
    // Collaboration server URL
    public String getCollaborationServerUrl() {
        return collaborationServerUrl;
    }
    
    public void setCollaborationServerUrl(String collaborationServerUrl) {
        this.collaborationServerUrl = collaborationServerUrl;
    }
    
    // Username
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    // Generate Javadoc
    public boolean isGenerateJavadoc() {
        return generateJavadoc;
    }
    
    public void setGenerateJavadoc(boolean generateJavadoc) {
        this.generateJavadoc = generateJavadoc;
    }
    
    // Add copyright header
    public boolean isAddCopyrightHeader() {
        return addCopyrightHeader;
    }
    
    public void setAddCopyrightHeader(boolean addCopyrightHeader) {
        this.addCopyrightHeader = addCopyrightHeader;
    }
    
    // Copyright text
    public String getCopyrightText() {
        return copyrightText;
    }
    
    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }
    
    // Show metrics in status bar
    public boolean isShowMetricsInStatusBar() {
        return showMetricsInStatusBar;
    }
    
    public void setShowMetricsInStatusBar(boolean showMetricsInStatusBar) {
        this.showMetricsInStatusBar = showMetricsInStatusBar;
    }
    
    // Enable notifications
    public boolean isEnableNotifications() {
        return enableNotifications;
    }
    
    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }
}