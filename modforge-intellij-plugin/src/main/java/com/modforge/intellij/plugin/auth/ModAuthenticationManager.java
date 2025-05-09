package com.modforge.intellij.plugin.auth;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.modforge.intellij.plugin.github.GitHubIntegrationService;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Authentication manager for ModForge plugin, providing secure token storage
 * and GitHub/API authentication. Optimized for IntelliJ IDEA 2025.1.
 */
@Service(Service.Level.APP)
@State(
    name = "ModForgeAuthenticationManager",
    storages = @Storage("modForgeAuth.xml")
)
public final class ModAuthenticationManager implements PersistentStateComponent<ModAuthenticationManager> {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    // Credential keys for various services
    private static final String GITHUB_TOKEN_KEY = "ModForge:GitHubToken";
    private static final String OPENAI_API_KEY = "ModForge:OpenAIApiKey";
    
    // GitHub APIs
    private static final String GITHUB_API_TEST_URL = "https://api.github.com/rate_limit";
    
    // State refresh intervals
    private static final long TOKEN_VALIDATION_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10);
    
    // Token validation timestamps
    private final AtomicLong lastGitHubTokenValidation = new AtomicLong(0);
    
    // Persistent state
    private String gitHubUsername = "";
    private boolean rememberGitHubCredentials = true;
    
    // Transient state
    @Transient
    private boolean gitHubTokenValid = false;
    
    @Transient
    private boolean openAIKeyValid = false;
    
    /**
     * Get the instance of the authentication manager
     * 
     * @return The authentication manager instance
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }
    
    /**
     * Set the GitHub token
     * 
     * @param token The GitHub token
     * @param remember Whether to remember the token
     */
    public void setGitHubToken(String token, boolean remember) {
        if (token == null || token.isEmpty()) {
            clearGitHubToken();
            return;
        }
        
        rememberGitHubCredentials = remember;
        
        // Store token securely
        CredentialAttributes attributes = createCredentialAttributes(GITHUB_TOKEN_KEY);
        Credentials credentials = new Credentials(gitHubUsername, token);
        PasswordSafe.getInstance().set(attributes, credentials);
        
        // Reset validation state
        gitHubTokenValid = false;
        lastGitHubTokenValidation.set(0);
        
        LOG.info("GitHub token updated" + (remember ? " (remembered)" : ""));
    }
    
    /**
     * Get the GitHub token
     * 
     * @return The GitHub token or null if not set
     */
    @Nullable
    public String getGitHubToken() {
        CredentialAttributes attributes = createCredentialAttributes(GITHUB_TOKEN_KEY);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }
    
    /**
     * Set the OpenAI API key
     * 
     * @param apiKey The OpenAI API key
     */
    public void setOpenAIApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            clearOpenAIApiKey();
            return;
        }
        
        // Store API key securely
        CredentialAttributes attributes = createCredentialAttributes(OPENAI_API_KEY);
        Credentials credentials = new Credentials("openai", apiKey);
        PasswordSafe.getInstance().set(attributes, credentials);
        
        // Reset validation state
        openAIKeyValid = false;
        
        LOG.info("OpenAI API key updated");
    }
    
    /**
     * Get the OpenAI API key
     * 
     * @return The OpenAI API key or null if not set
     */
    @Nullable
    public String getOpenAIApiKey() {
        CredentialAttributes attributes = createCredentialAttributes(OPENAI_API_KEY);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }
    
    /**
     * Check if the GitHub token is valid
     * 
     * @return true if token is valid, false otherwise
     */
    public boolean isGitHubTokenValid() {
        if (!gitHubTokenValid) {
            validateGitHubToken();
        }
        return gitHubTokenValid;
    }
    
    /**
     * Check if the OpenAI API key is valid
     * 
     * @return true if API key is valid, false otherwise
     */
    public boolean isOpenAIApiKeyValid() {
        return openAIKeyValid;
    }
    
    /**
     * Validate the GitHub token
     */
    public void validateGitHubToken() {
        String token = getGitHubToken();
        if (token == null || token.isEmpty()) {
            gitHubTokenValid = false;
            return;
        }
        
        // Avoid too frequent validations
        long now = System.currentTimeMillis();
        long lastValidation = lastGitHubTokenValidation.get();
        if (gitHubTokenValid && now - lastValidation < TOKEN_VALIDATION_INTERVAL_MS) {
            return;
        }
        
        gitHubTokenValid = TokenAuthConnectionUtil.testToken(GITHUB_API_TEST_URL, token);
        lastGitHubTokenValidation.set(now);
        
        LOG.info("GitHub token validation: " + (gitHubTokenValid ? "valid" : "invalid"));
    }
    
    /**
     * Validate the GitHub token asynchronously
     * 
     * @param callback Callback for validation result
     */
    public void validateGitHubTokenAsync(Consumer<Boolean> callback) {
        String token = getGitHubToken();
        if (token == null || token.isEmpty()) {
            gitHubTokenValid = false;
            callback.accept(false);
            return;
        }
        
        // Avoid too frequent validations
        long now = System.currentTimeMillis();
        long lastValidation = lastGitHubTokenValidation.get();
        if (gitHubTokenValid && now - lastValidation < TOKEN_VALIDATION_INTERVAL_MS) {
            callback.accept(true);
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            boolean valid = TokenAuthConnectionUtil.testToken(GITHUB_API_TEST_URL, token);
            if (valid) {
                gitHubTokenValid = true;
                lastGitHubTokenValidation.set(System.currentTimeMillis());
            }
            return valid;
        }, Executors.newVirtualThreadPerTaskExecutor()).thenAccept(valid -> {
            ApplicationManager.getApplication().invokeLater(() -> callback.accept(valid));
        });
    }
    
    /**
     * Clear the GitHub token
     */
    public void clearGitHubToken() {
        CredentialAttributes attributes = createCredentialAttributes(GITHUB_TOKEN_KEY);
        PasswordSafe.getInstance().set(attributes, null);
        gitHubTokenValid = false;
        LOG.info("GitHub token cleared");
    }
    
    /**
     * Clear the OpenAI API key
     */
    public void clearOpenAIApiKey() {
        CredentialAttributes attributes = createCredentialAttributes(OPENAI_API_KEY);
        PasswordSafe.getInstance().set(attributes, null);
        openAIKeyValid = false;
        LOG.info("OpenAI API key cleared");
    }
    
    /**
     * Create credential attributes for secure storage
     * 
     * @param key The credential key
     * @return The credential attributes
     */
    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("ModForge", key)
        );
    }
    
    @Override
    public ModAuthenticationManager getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull ModAuthenticationManager state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * Get the GitHub username
     * 
     * @return The GitHub username
     */
    public String getGitHubUsername() {
        return gitHubUsername;
    }
    
    /**
     * Set the GitHub username
     * 
     * @param gitHubUsername The GitHub username
     */
    public void setGitHubUsername(String gitHubUsername) {
        this.gitHubUsername = gitHubUsername;
    }
    
    /**
     * Check if GitHub credentials should be remembered
     * 
     * @return true if credentials should be remembered, false otherwise
     */
    public boolean isRememberGitHubCredentials() {
        return rememberGitHubCredentials;
    }
    
    /**
     * Set whether GitHub credentials should be remembered
     * 
     * @param rememberGitHubCredentials true if credentials should be remembered, false otherwise
     */
    public void setRememberGitHubCredentials(boolean rememberGitHubCredentials) {
        this.rememberGitHubCredentials = rememberGitHubCredentials;
    }
}