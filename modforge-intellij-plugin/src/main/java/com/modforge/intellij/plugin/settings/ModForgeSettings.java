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
 * Persistent settings for ModForge.
 */
@Service
@State(
        name = "com.modforge.intellij.plugin.settings.ModForgeSettings",
        storages = @Storage("ModForgeSettings.xml")
)
public final class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    /**
     * Default server URL.
     */
    private static final String DEFAULT_SERVER_URL = "http://localhost:5000";
    
    /**
     * Server URL.
     */
    private String serverUrl = DEFAULT_SERVER_URL;
    
    /**
     * Whether continuous development is enabled.
     */
    private boolean continuousDevelopment = false;
    
    /**
     * Whether pattern recognition is enabled.
     */
    private boolean patternRecognition = true;
    
    /**
     * Get the instance of the settings.
     *
     * @return The instance
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
     * Get the server URL.
     *
     * @return The server URL
     */
    @NotNull
    public String getServerUrl() {
        return serverUrl != null && !serverUrl.isEmpty() ? serverUrl : DEFAULT_SERVER_URL;
    }
    
    /**
     * Set the server URL.
     *
     * @param serverUrl The server URL
     */
    public void setServerUrl(@NotNull String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    /**
     * Get whether continuous development is enabled.
     *
     * @return Whether continuous development is enabled
     */
    public boolean isContinuousDevelopment() {
        return continuousDevelopment;
    }
    
    /**
     * Set whether continuous development is enabled.
     *
     * @param continuousDevelopment Whether continuous development is enabled
     */
    public void setContinuousDevelopment(boolean continuousDevelopment) {
        this.continuousDevelopment = continuousDevelopment;
    }
    
    /**
     * Get whether pattern recognition is enabled.
     *
     * @return Whether pattern recognition is enabled
     */
    public boolean isPatternRecognition() {
        return patternRecognition;
    }
    
    /**
     * Set whether pattern recognition is enabled.
     *
     * @param patternRecognition Whether pattern recognition is enabled
     */
    public void setPatternRecognition(boolean patternRecognition) {
        this.patternRecognition = patternRecognition;
    }
}