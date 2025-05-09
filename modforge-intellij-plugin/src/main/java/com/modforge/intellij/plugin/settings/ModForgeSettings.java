package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the ModForge plugin.
 */
@Service
@State(
        name = "ModForgeSettings",
        storages = {
                @Storage("modforge-settings.xml")
        }
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    // OpenAI settings
    private String openAiApiKey = "";
    private String openAiModel = "gpt-4";
    private int maxTokens = 2048;
    private double temperature = 0.7;
    
    // Feature toggles
    private boolean continuousDevelopmentEnabled = false;
    private boolean patternRecognitionEnabled = true;
    
    // Development settings
    private long checkIntervalMs = 60_000L; // 1 minute
    private boolean autoCompileEnabled = true;
    private boolean autoFixEnabled = true;
    private boolean autoDocumentEnabled = false;
    
    // Mod loader settings
    private boolean forgeSupported = true;
    private boolean fabricSupported = true;
    private boolean quiltSupported = true;
    
    /**
     * Gets the instance of the settings.
     * @return The settings
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
    
    // OpenAI settings
    
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }
    
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
    
    public String getOpenAiModel() {
        return openAiModel;
    }
    
    public void setOpenAiModel(String openAiModel) {
        this.openAiModel = openAiModel;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    // Feature toggles
    
    public boolean isContinuousDevelopmentEnabled() {
        return continuousDevelopmentEnabled;
    }
    
    public void setContinuousDevelopmentEnabled(boolean continuousDevelopmentEnabled) {
        this.continuousDevelopmentEnabled = continuousDevelopmentEnabled;
    }
    
    public boolean isPatternRecognitionEnabled() {
        return patternRecognitionEnabled;
    }
    
    public void setPatternRecognitionEnabled(boolean patternRecognitionEnabled) {
        this.patternRecognitionEnabled = patternRecognitionEnabled;
    }
    
    // Development settings
    
    public long getCheckIntervalMs() {
        return checkIntervalMs;
    }
    
    public void setCheckIntervalMs(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }
    
    public boolean isAutoCompileEnabled() {
        return autoCompileEnabled;
    }
    
    public void setAutoCompileEnabled(boolean autoCompileEnabled) {
        this.autoCompileEnabled = autoCompileEnabled;
    }
    
    public boolean isAutoFixEnabled() {
        return autoFixEnabled;
    }
    
    public void setAutoFixEnabled(boolean autoFixEnabled) {
        this.autoFixEnabled = autoFixEnabled;
    }
    
    public boolean isAutoDocumentEnabled() {
        return autoDocumentEnabled;
    }
    
    public void setAutoDocumentEnabled(boolean autoDocumentEnabled) {
        this.autoDocumentEnabled = autoDocumentEnabled;
    }
    
    // Mod loader settings
    
    public boolean isForgeSupported() {
        return forgeSupported;
    }
    
    public void setForgeSupported(boolean forgeSupported) {
        this.forgeSupported = forgeSupported;
    }
    
    public boolean isFabricSupported() {
        return fabricSupported;
    }
    
    public void setFabricSupported(boolean fabricSupported) {
        this.fabricSupported = fabricSupported;
    }
    
    public boolean isQuiltSupported() {
        return quiltSupported;
    }
    
    public void setQuiltSupported(boolean quiltSupported) {
        this.quiltSupported = quiltSupported;
    }
}