package com.modforge.intellij.plugin.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.modforge.intellij.plugin.ui.ModForgeConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for ModForge.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
@State(
        name = "ModForgeSettings",
        storages = @Storage("modForgeSettings.xml")
)
public class ModForgeSettings implements PersistentStateComponent<ModForgeSettings> {
    private static final String CREDENTIAL_SERVICE_NAME = "ModForge";
    private static final String ACCESS_TOKEN_KEY = "accessToken";
    
    // Server settings
    private String serverUrl = "https://modforge.ai";
    
    // GitHub settings
    private String gitHubUsername = "";
    private boolean useGitHubAuthentication = true;
    
    // Feature settings
    private boolean patternRecognition = true;
    private boolean continuousDevelopment = false;
    private boolean analyticsEnabled = true;
    private boolean autoCommit = false;
    private boolean autoSave = true;
    private boolean autoSync = false;
    
    // Minecraft settings
    private String defaultLanguage = "java";
    private String defaultModLoader = "forge";
    private int minecraftVersion = 120;
    
    // Performance settings
    private int maxThreads = 4;
    private int requestTimeout = 30;
    private String gitExecutablePath = "";

    /**
     * Gets the instance of the settings.
     *
     * @return The settings instance.
     */
    public static ModForgeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeSettings.class);
    }

    /**
     * Gets the state of the settings.
     *
     * @return The settings state.
     */
    @Nullable
    @Override
    public ModForgeSettings getState() {
        return this;
    }

    /**
     * Loads the state of the settings.
     *
     * @param state The state to load.
     */
    @Override
    public void loadState(@NotNull ModForgeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * Opens the settings dialog.
     *
     * @param project The project.
     */
    public void openSettings(@NotNull Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeConfigurable.class);
    }

    // Getters and setters

    /**
     * Gets the server URL.
     *
     * @return The server URL.
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Sets the server URL.
     *
     * @param serverUrl The server URL.
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Gets the GitHub username.
     *
     * @return The GitHub username.
     */
    public String getGitHubUsername() {
        return gitHubUsername;
    }

    /**
     * Sets the GitHub username.
     *
     * @param gitHubUsername The GitHub username.
     */
    public void setGitHubUsername(String gitHubUsername) {
        this.gitHubUsername = gitHubUsername;
    }

    /**
     * Gets the access token from the password safe.
     *
     * @return The access token.
     */
    @Transient
    public String getAccessToken() {
        CredentialAttributes attributes = createCredentialAttributes(ACCESS_TOKEN_KEY);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    /**
     * Sets the access token in the password safe.
     *
     * @param accessToken The access token.
     */
    public void setAccessToken(@Nullable String accessToken) {
        CredentialAttributes attributes = createCredentialAttributes(ACCESS_TOKEN_KEY);
        Credentials credentials = new Credentials(gitHubUsername, accessToken);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    /**
     * Creates credential attributes for a key.
     *
     * @param key The key.
     * @return The credential attributes.
     */
    private @NotNull CredentialAttributes createCredentialAttributes(@NotNull String key) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(CREDENTIAL_SERVICE_NAME, key));
    }

    /**
     * Checks if GitHub authentication is enabled.
     *
     * @return Whether GitHub authentication is enabled.
     */
    public boolean isUseGitHubAuthentication() {
        return useGitHubAuthentication;
    }

    /**
     * Sets whether GitHub authentication is enabled.
     *
     * @param useGitHubAuthentication Whether GitHub authentication is enabled.
     */
    public void setUseGitHubAuthentication(boolean useGitHubAuthentication) {
        this.useGitHubAuthentication = useGitHubAuthentication;
    }

    /**
     * Checks if pattern recognition is enabled.
     *
     * @return Whether pattern recognition is enabled.
     */
    public boolean isPatternRecognition() {
        return patternRecognition;
    }

    /**
     * Sets whether pattern recognition is enabled.
     *
     * @param patternRecognition Whether pattern recognition is enabled.
     */
    public void setPatternRecognition(boolean patternRecognition) {
        this.patternRecognition = patternRecognition;
    }

    /**
     * Checks if continuous development is enabled.
     *
     * @return Whether continuous development is enabled.
     */
    public boolean isContinuousDevelopment() {
        return continuousDevelopment;
    }

    /**
     * Sets whether continuous development is enabled.
     *
     * @param continuousDevelopment Whether continuous development is enabled.
     */
    public void setContinuousDevelopment(boolean continuousDevelopment) {
        this.continuousDevelopment = continuousDevelopment;
    }

    /**
     * Checks if analytics is enabled.
     *
     * @return Whether analytics is enabled.
     */
    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    /**
     * Sets whether analytics is enabled.
     *
     * @param analyticsEnabled Whether analytics is enabled.
     */
    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }

    /**
     * Checks if auto commit is enabled.
     *
     * @return Whether auto commit is enabled.
     */
    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * Sets whether auto commit is enabled.
     *
     * @param autoCommit Whether auto commit is enabled.
     */
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    /**
     * Checks if auto save is enabled.
     *
     * @return Whether auto save is enabled.
     */
    public boolean isAutoSave() {
        return autoSave;
    }

    /**
     * Sets whether auto save is enabled.
     *
     * @param autoSave Whether auto save is enabled.
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    /**
     * Checks if auto sync is enabled.
     *
     * @return Whether auto sync is enabled.
     */
    public boolean isAutoSync() {
        return autoSync;
    }

    /**
     * Sets whether auto sync is enabled.
     *
     * @param autoSync Whether auto sync is enabled.
     */
    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }

    /**
     * Gets the default language.
     *
     * @return The default language.
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Sets the default language.
     *
     * @param defaultLanguage The default language.
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * Gets the default mod loader.
     *
     * @return The default mod loader.
     */
    public String getDefaultModLoader() {
        return defaultModLoader;
    }

    /**
     * Sets the default mod loader.
     *
     * @param defaultModLoader The default mod loader.
     */
    public void setDefaultModLoader(String defaultModLoader) {
        this.defaultModLoader = defaultModLoader;
    }

    /**
     * Gets the Minecraft version.
     *
     * @return The Minecraft version.
     */
    public int getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Sets the Minecraft version.
     *
     * @param minecraftVersion The Minecraft version.
     */
    public void setMinecraftVersion(int minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    /**
     * Gets the maximum number of threads.
     *
     * @return The maximum number of threads.
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Sets the maximum number of threads.
     *
     * @param maxThreads The maximum number of threads.
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Gets the request timeout in seconds.
     *
     * @return The request timeout.
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Sets the request timeout in seconds.
     *
     * @param requestTimeout The request timeout.
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Gets the Git executable path.
     *
     * @return The Git executable path.
     */
    public String getGitExecutablePath() {
        return gitExecutablePath;
    }

    /**
     * Sets the Git executable path.
     *
     * @param gitExecutablePath The Git executable path.
     */
    public void setGitExecutablePath(String gitExecutablePath) {
        this.gitExecutablePath = gitExecutablePath;
    }
}