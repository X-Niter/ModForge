package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
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
        name = "ModForgeSettings",
        storages = {@Storage("ModForgeSettings.xml")}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // API settings
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
     * Gets whether to use pattern learning.
     * @return Whether to use pattern learning
     */
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }
    
    /**
     * Sets whether to use pattern learning.
     * @param usePatternLearning Whether to use pattern learning
     */
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }
    
    /**
     * Gets the collaboration server URL.
     * @return The collaboration server URL
     */
    public String getCollaborationServerUrl() {
        return collaborationServerUrl;
    }
    
    /**
     * Sets the collaboration server URL.
     * @param collaborationServerUrl The collaboration server URL
     */
    public void setCollaborationServerUrl(String collaborationServerUrl) {
        this.collaborationServerUrl = collaborationServerUrl;
    }
    
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
     * Gets whether to generate Javadoc.
     * @return Whether to generate Javadoc
     */
    public boolean isGenerateJavadoc() {
        return generateJavadoc;
    }
    
    /**
     * Sets whether to generate Javadoc.
     * @param generateJavadoc Whether to generate Javadoc
     */
    public void setGenerateJavadoc(boolean generateJavadoc) {
        this.generateJavadoc = generateJavadoc;
    }
    
    /**
     * Gets whether to add a copyright header.
     * @return Whether to add a copyright header
     */
    public boolean isAddCopyrightHeader() {
        return addCopyrightHeader;
    }
    
    /**
     * Sets whether to add a copyright header.
     * @param addCopyrightHeader Whether to add a copyright header
     */
    public void setAddCopyrightHeader(boolean addCopyrightHeader) {
        this.addCopyrightHeader = addCopyrightHeader;
    }
    
    /**
     * Gets the copyright text.
     * @return The copyright text
     */
    public String getCopyrightText() {
        return copyrightText;
    }
    
    /**
     * Sets the copyright text.
     * @param copyrightText The copyright text
     */
    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }
    
    /**
     * Gets whether to show metrics in the status bar.
     * @return Whether to show metrics in the status bar
     */
    public boolean isShowMetricsInStatusBar() {
        return showMetricsInStatusBar;
    }
    
    /**
     * Sets whether to show metrics in the status bar.
     * @param showMetricsInStatusBar Whether to show metrics in the status bar
     */
    public void setShowMetricsInStatusBar(boolean showMetricsInStatusBar) {
        this.showMetricsInStatusBar = showMetricsInStatusBar;
    }
    
    /**
     * Gets whether to enable notifications.
     * @return Whether to enable notifications
     */
    public boolean isEnableNotifications() {
        return enableNotifications;
    }
    
    /**
     * Sets whether to enable notifications.
     * @param enableNotifications Whether to enable notifications
     */
    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }
}