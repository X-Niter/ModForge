package com.modforge.intellij.plugin.services;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for storing ModForge AI settings.
 */
@Service
@State(
        name = "ModForgeSettings",
        storages = {@Storage("modforge-settings.xml")}
)
public final class ModForgeSettingsService implements PersistentStateComponent<ModForgeSettingsService> {
    private static final Logger LOG = Logger.getInstance(ModForgeSettingsService.class);
    
    // Constants
    private static final String DEFAULT_API_URL = "https://modforge.ai/api";
    private static final String OPENAI_API_KEY_ID = "ModForge.OpenAIApiKey";
    
    // Settings
    private String apiUrl = DEFAULT_API_URL;
    private boolean syncEnabled = true;
    private boolean useRemoteServices = true;
    private boolean enablePatternLearning = true;
    private int syncIntervalMinutes = 30;
    private boolean shareSuccessfulPatterns = true;
    private boolean shareErrorPatterns = true;
    private boolean allowMetricsCollection = true;
    
    /**
     * Gets the instance of this service.
     * @return The service instance
     */
    public static ModForgeSettingsService getInstance() {
        return ServiceManager.getService(ModForgeSettingsService.class);
    }
    
    @Override
    public @Nullable ModForgeSettingsService getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull ModForgeSettingsService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * Gets the API URL.
     * @return The API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }
    
    /**
     * Sets the API URL.
     * @param apiUrl The API URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    /**
     * Gets whether sync is enabled.
     * @return Whether sync is enabled
     */
    public boolean isSyncEnabled() {
        return syncEnabled;
    }
    
    /**
     * Sets whether sync is enabled.
     * @param syncEnabled Whether sync is enabled
     */
    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
    
    /**
     * Gets whether to use remote services for computation-intensive tasks.
     * @return Whether to use remote services
     */
    public boolean isUseRemoteServices() {
        return useRemoteServices;
    }
    
    /**
     * Sets whether to use remote services for computation-intensive tasks.
     * @param useRemoteServices Whether to use remote services
     */
    public void setUseRemoteServices(boolean useRemoteServices) {
        this.useRemoteServices = useRemoteServices;
    }
    
    /**
     * Gets whether pattern learning is enabled.
     * @return Whether pattern learning is enabled
     */
    public boolean isEnablePatternLearning() {
        return enablePatternLearning;
    }
    
    /**
     * Sets whether pattern learning is enabled.
     * @param enablePatternLearning Whether pattern learning is enabled
     */
    public void setEnablePatternLearning(boolean enablePatternLearning) {
        this.enablePatternLearning = enablePatternLearning;
    }
    
    /**
     * Gets the sync interval in minutes.
     * @return The sync interval in minutes
     */
    public int getSyncIntervalMinutes() {
        return syncIntervalMinutes;
    }
    
    /**
     * Sets the sync interval in minutes.
     * @param syncIntervalMinutes The sync interval in minutes
     */
    public void setSyncIntervalMinutes(int syncIntervalMinutes) {
        this.syncIntervalMinutes = syncIntervalMinutes;
    }
    
    /**
     * Gets whether to share successful patterns.
     * @return Whether to share successful patterns
     */
    public boolean isShareSuccessfulPatterns() {
        return shareSuccessfulPatterns;
    }
    
    /**
     * Sets whether to share successful patterns.
     * @param shareSuccessfulPatterns Whether to share successful patterns
     */
    public void setShareSuccessfulPatterns(boolean shareSuccessfulPatterns) {
        this.shareSuccessfulPatterns = shareSuccessfulPatterns;
    }
    
    /**
     * Gets whether to share error patterns.
     * @return Whether to share error patterns
     */
    public boolean isShareErrorPatterns() {
        return shareErrorPatterns;
    }
    
    /**
     * Sets whether to share error patterns.
     * @param shareErrorPatterns Whether to share error patterns
     */
    public void setShareErrorPatterns(boolean shareErrorPatterns) {
        this.shareErrorPatterns = shareErrorPatterns;
    }
    
    /**
     * Gets whether to allow metrics collection.
     * @return Whether to allow metrics collection
     */
    public boolean isAllowMetricsCollection() {
        return allowMetricsCollection;
    }
    
    /**
     * Sets whether to allow metrics collection.
     * @param allowMetricsCollection Whether to allow metrics collection
     */
    public void setAllowMetricsCollection(boolean allowMetricsCollection) {
        this.allowMetricsCollection = allowMetricsCollection;
    }
    
    /**
     * Sets the OpenAI API key.
     * @param apiKey The API key
     */
    public void setOpenAIApiKey(String apiKey) {
        CredentialAttributes attributes = createCredentialAttributes(OPENAI_API_KEY_ID);
        Credentials credentials = new Credentials("", apiKey);
        PasswordSafe.getInstance().set(attributes, credentials);
    }
    
    /**
     * Gets the OpenAI API key.
     * @return The API key, or null if it hasn't been set
     */
    @Nullable
    public String getOpenAIApiKey() {
        CredentialAttributes attributes = createCredentialAttributes(OPENAI_API_KEY_ID);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }
    
    /**
     * Creates credential attributes for the specified key.
     * @param key The key
     * @return The credential attributes
     */
    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("ModForge", key)
        );
    }
}