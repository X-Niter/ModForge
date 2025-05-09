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
 * Persistent settings for ModForge plugin.
 */
@State(
        name = "ModForgeSettings",
        storages = {@Storage("ModForgeSettings.xml")}
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // General settings
    private boolean enableAIAssist = true;
    private boolean enableContinuousDevelopment = true;
    private boolean autoFixCompilationErrors = true;
    private int maxAutoFixAttempts = 3;
    private int maxEnhancementsPerFile = 5;
    
    // API settings
    private String openAIApiKey = "";
    private boolean usePatternRecognition = true;
    private double similarityThreshold = 0.7;
    
    // Web sync settings
    private String serverUrl = "https://modforge.io/api";
    private String apiToken = "";
    private boolean enableSync = false;
    private int syncInterval = 300; // 5 minutes
    
    /**
     * Gets the ModForge settings instance.
     * @return The ModForge settings
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
     * Checks if AI assist is enabled.
     * @return True if AI assist is enabled
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
     * Checks if continuous development is enabled.
     * @return True if continuous development is enabled
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
     * Checks if automatic compilation error fixing is enabled.
     * @return True if automatic compilation error fixing is enabled
     */
    public boolean isAutoFixCompilationErrors() {
        return autoFixCompilationErrors;
    }

    /**
     * Sets whether automatic compilation error fixing is enabled.
     * @param autoFixCompilationErrors Whether automatic compilation error fixing is enabled
     */
    public void setAutoFixCompilationErrors(boolean autoFixCompilationErrors) {
        this.autoFixCompilationErrors = autoFixCompilationErrors;
    }

    /**
     * Gets the maximum number of automatic fix attempts.
     * @return The maximum number of automatic fix attempts
     */
    public int getMaxAutoFixAttempts() {
        return maxAutoFixAttempts;
    }

    /**
     * Sets the maximum number of automatic fix attempts.
     * @param maxAutoFixAttempts The maximum number of automatic fix attempts
     */
    public void setMaxAutoFixAttempts(int maxAutoFixAttempts) {
        this.maxAutoFixAttempts = maxAutoFixAttempts;
    }

    /**
     * Gets the maximum number of enhancements per file.
     * @return The maximum number of enhancements per file
     */
    public int getMaxEnhancementsPerFile() {
        return maxEnhancementsPerFile;
    }

    /**
     * Sets the maximum number of enhancements per file.
     * @param maxEnhancementsPerFile The maximum number of enhancements per file
     */
    public void setMaxEnhancementsPerFile(int maxEnhancementsPerFile) {
        this.maxEnhancementsPerFile = maxEnhancementsPerFile;
    }

    /**
     * Gets the OpenAI API key.
     * @return The OpenAI API key
     */
    public String getOpenAIApiKey() {
        return openAIApiKey;
    }

    /**
     * Sets the OpenAI API key.
     * @param openAIApiKey The OpenAI API key
     */
    public void setOpenAIApiKey(String openAIApiKey) {
        this.openAIApiKey = openAIApiKey;
    }

    /**
     * Checks if pattern recognition is enabled.
     * @return True if pattern recognition is enabled
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
     * Gets the similarity threshold for pattern matching.
     * @return The similarity threshold
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Sets the similarity threshold for pattern matching.
     * @param similarityThreshold The similarity threshold
     */
    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
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

    /**
     * Gets the API token.
     * @return The API token
     */
    public String getApiToken() {
        return apiToken;
    }

    /**
     * Sets the API token.
     * @param apiToken The API token
     */
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    /**
     * Checks if synchronization is enabled.
     * @return True if synchronization is enabled
     */
    public boolean isEnableSync() {
        return enableSync;
    }

    /**
     * Sets whether synchronization is enabled.
     * @param enableSync Whether synchronization is enabled
     */
    public void setEnableSync(boolean enableSync) {
        this.enableSync = enableSync;
    }

    /**
     * Gets the synchronization interval in seconds.
     * @return The synchronization interval
     */
    public int getSyncInterval() {
        return syncInterval;
    }

    /**
     * Sets the synchronization interval in seconds.
     * @param syncInterval The synchronization interval
     */
    public void setSyncInterval(int syncInterval) {
        this.syncInterval = syncInterval;
    }
}