package com.modforge.github;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles GitHub credentials for the ModForge IntelliJ plugin
 * Uses multiple fallback mechanisms for retrieving GitHub tokens
 */
@Service
public final class GitHubCredentialsManager {
    private static final Logger LOG = Logger.getInstance(GitHubCredentialsManager.class);
    private static final String SERVICE_NAME = "ModForge GitHub";
    private static final String CREDENTIAL_KEY = "github-token";
    
    // Pattern to match GitHub token in git credential file
    private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile("(?:protocol=https|host=github\\.com|username=.*|password=)([a-zA-Z0-9_]+)");
    
    public static GitHubCredentialsManager getInstance() {
        return ApplicationManager.getApplication().getService(GitHubCredentialsManager.class);
    }
    
    /**
     * Get GitHub token using all available methods, with fallbacks
     * Order:
     * 1. IDE password safe
     * 2. User entered token
     * 3. Git credentials store
     * 4. ENV var
     */
    @Nullable
    public String getGitHubToken(Project project) {
        // First try to get from IDE secure storage
        String token = getTokenFromPasswordSafe();
        
        // Next, try to get from Git credential store
        if (token == null) {
            token = getTokenFromGitCredentials();
        }
        
        // Finally, check environment variable
        if (token == null) {
            token = System.getenv("GITHUB_TOKEN");
        }
        
        // If still null, ask user for token
        if (token == null && project != null) {
            token = promptForToken(project);
            if (token != null && !token.isEmpty()) {
                saveTokenToPasswordSafe(token);
            }
        }
        
        return token;
    }
    
    /**
     * Save token to IDE secure storage
     */
    public void saveTokenToPasswordSafe(@NotNull String token) {
        CredentialAttributes attributes = createCredentialAttributes();
        Credentials credentials = new Credentials(CREDENTIAL_KEY, token);
        PasswordSafe.getInstance().set(attributes, credentials);
    }
    
    /**
     * Retrieve token from IDE secure storage
     */
    @Nullable
    public String getTokenFromPasswordSafe() {
        CredentialAttributes attributes = createCredentialAttributes();
        return Optional.ofNullable(PasswordSafe.getInstance().get(attributes))
                .map(Credentials::getPasswordAsString)
                .orElse(null);
    }
    
    /**
     * Clear saved token
     */
    public void clearToken() {
        CredentialAttributes attributes = createCredentialAttributes();
        PasswordSafe.getInstance().set(attributes, null);
    }
    
    /**
     * Create credential attributes for storing GitHub token
     */
    @NotNull
    private CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SERVICE_NAME, CREDENTIAL_KEY)
        );
    }
    
    /**
     * Try to get GitHub token from Git credentials store
     */
    @Nullable
    private String getTokenFromGitCredentials() {
        // Try to find the Git credentials file
        String userHome = System.getProperty("user.home");
        if (userHome == null) return null;
        
        // Common locations for Git credentials
        Path[] possibleLocations = {
                Paths.get(userHome, ".git-credentials"),
                Paths.get(userHome, ".config", "git", "credentials"),
                Paths.get(userHome, ".gitconfig")
        };
        
        for (Path path : possibleLocations) {
            File file = path.toFile();
            if (!file.exists() || !file.canRead()) continue;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("github.com") && line.contains("://")) {
                        // Extract token from URL format
                        int tokenStart = line.lastIndexOf(':') + 1;
                        int tokenEnd = line.indexOf('@');
                        if (tokenStart > 0 && tokenEnd > tokenStart) {
                            return line.substring(tokenStart, tokenEnd);
                        }
                    } else if (line.contains("github.com") && line.contains("password=")) {
                        // Extract token from credential format
                        Matcher matcher = GITHUB_TOKEN_PATTERN.matcher(line);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to read Git credentials file: " + path, e);
            }
        }
        
        return null;
    }
    
    /**
     * Prompt user to enter GitHub token
     */
    @Nullable
    private String promptForToken(Project project) {
        return Messages.showInputDialog(
                project,
                "Enter your GitHub personal access token:",
                "GitHub Authentication",
                Messages.getQuestionIcon(),
                "",
                null
        );
    }
}