package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for integrating with GitHub.
 * This service provides functionality for interacting with GitHub repositories.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    
    // GitHub API base URL
    private static final String GITHUB_API_URL = "https://api.github.com";
    
    // GitHub specific headers
    private static final String GITHUB_VERSION_HEADER = "2022-11-28";
    private static final String ACCEPT_HEADER = "application/vnd.github+json";
    
    // Settings
    private final ModForgeSettings settings;
    
    // State tracking
    private final AtomicBoolean isCommitting = new AtomicBoolean(false);
    private final AtomicBoolean isPushing = new AtomicBoolean(false);
    private final AtomicBoolean isFetching = new AtomicBoolean(false);
    
    /**
     * Constructor.
     */
    public GitHubIntegrationService() {
        settings = ModForgeSettings.getInstance();
        LOG.info("GitHubIntegrationService initialized");
    }

    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static GitHubIntegrationService getInstance() {
        return ApplicationManager.getApplication().getService(GitHubIntegrationService.class);
    }

    /**
     * Gets a list of repositories for the authenticated user.
     *
     * @return A CompletableFuture with a list of repository names.
     */
    public CompletableFuture<List<String>> getRepositories() {
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot get repositories.");
            return CompletableFuture.completedFuture(List.of());
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                String url = GITHUB_API_URL + "/user/repos?per_page=100";
                String response = sendGitHubRequest(url, "GET", null);
                
                // Extract repository names with simple regex for now
                // In a real implementation, use a proper JSON parser
                List<String> repos = new ArrayList<>();
                Pattern pattern = Pattern.compile("\"full_name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);
                
                while (matcher.find()) {
                    repos.add(matcher.group(1));
                }
                
                return repos;
            } catch (Exception e) {
                LOG.error("Failed to get repositories", e);
                return List.of();
            }
        });
    }
    
    /**
     * Creates a new repository.
     *
     * @param name        The repository name.
     * @param description The repository description.
     * @param isPrivate   Whether the repository is private.
     * @return A CompletableFuture with the repository URL.
     */
    public CompletableFuture<String> createRepository(
            @NotNull String name,
            @Nullable String description,
            boolean isPrivate) {
        
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot create repository.");
            return CompletableFuture.completedFuture("");
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                String url = GITHUB_API_URL + "/user/repos";
                
                // Create JSON payload
                String payload = String.format(
                        "{\"name\":\"%s\",\"description\":\"%s\",\"private\":%b,\"auto_init\":true}",
                        name,
                        description != null ? description : "",
                        isPrivate
                );
                
                String response = sendGitHubRequest(url, "POST", payload);
                
                // Extract HTML URL with simple regex for now
                Pattern pattern = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);
                
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    LOG.error("Failed to parse repository URL from response");
                    return "";
                }
            } catch (Exception e) {
                LOG.error("Failed to create repository", e);
                return "";
            }
        });
    }
    
    /**
     * Creates an issue in a repository.
     *
     * @param repoFullName The full repository name (owner/repo).
     * @param title        The issue title.
     * @param body         The issue body.
     * @return A CompletableFuture with the issue URL.
     */
    public CompletableFuture<String> createIssue(
            @NotNull String repoFullName,
            @NotNull String title,
            @NotNull String body) {
        
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot create issue.");
            return CompletableFuture.completedFuture("");
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                String url = GITHUB_API_URL + "/repos/" + repoFullName + "/issues";
                
                // Create JSON payload
                String payload = String.format(
                        "{\"title\":\"%s\",\"body\":\"%s\"}",
                        title,
                        body.replace("\"", "\\\"").replace("\n", "\\n")
                );
                
                String response = sendGitHubRequest(url, "POST", payload);
                
                // Extract HTML URL with simple regex for now
                Pattern pattern = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);
                
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    LOG.error("Failed to parse issue URL from response");
                    return "";
                }
            } catch (Exception e) {
                LOG.error("Failed to create issue", e);
                return "";
            }
        });
    }
    
    /**
     * Creates a pull request in a repository.
     *
     * @param repoFullName The full repository name (owner/repo).
     * @param title        The pull request title.
     * @param body         The pull request body.
     * @param head         The head branch.
     * @param base         The base branch.
     * @return A CompletableFuture with the pull request URL.
     */
    public CompletableFuture<String> createPullRequest(
            @NotNull String repoFullName,
            @NotNull String title,
            @NotNull String body,
            @NotNull String head,
            @NotNull String base) {
        
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot create pull request.");
            return CompletableFuture.completedFuture("");
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                String url = GITHUB_API_URL + "/repos/" + repoFullName + "/pulls";
                
                // Create JSON payload
                String payload = String.format(
                        "{\"title\":\"%s\",\"body\":\"%s\",\"head\":\"%s\",\"base\":\"%s\"}",
                        title,
                        body.replace("\"", "\\\"").replace("\n", "\\n"),
                        head,
                        base
                );
                
                String response = sendGitHubRequest(url, "POST", payload);
                
                // Extract HTML URL with simple regex for now
                Pattern pattern = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);
                
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    LOG.error("Failed to parse pull request URL from response");
                    return "";
                }
            } catch (Exception e) {
                LOG.error("Failed to create pull request", e);
                return "";
            }
        });
    }
    
    /**
     * Creates a workflow file in a repository.
     *
     * @param repoFullName The full repository name (owner/repo).
     * @param path         The workflow file path.
     * @param content      The workflow file content.
     * @param message      The commit message.
     * @return A CompletableFuture with the workflow file URL.
     */
    public CompletableFuture<String> createWorkflowFile(
            @NotNull String repoFullName,
            @NotNull String path,
            @NotNull String content,
            @NotNull String message) {
        
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot create workflow file.");
            return CompletableFuture.completedFuture("");
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                // Ensure the path starts with .github/workflows
                String normalizedPath = path;
                if (!normalizedPath.startsWith(".github/workflows/")) {
                    normalizedPath = ".github/workflows/" + normalizedPath;
                }
                
                // Ensure the path ends with .yml or .yaml
                if (!normalizedPath.endsWith(".yml") && !normalizedPath.endsWith(".yaml")) {
                    normalizedPath += ".yml";
                }
                
                String url = GITHUB_API_URL + "/repos/" + repoFullName + "/contents/" + normalizedPath;
                
                // Base64 encode the content
                String encodedContent = java.util.Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
                
                // Create JSON payload
                String payload = String.format(
                        "{\"message\":\"%s\",\"content\":\"%s\"}",
                        message,
                        encodedContent
                );
                
                String response = sendGitHubRequest(url, "PUT", payload);
                
                // Extract HTML URL with simple regex for now
                Pattern pattern = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);
                
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    LOG.error("Failed to parse workflow file URL from response");
                    return "";
                }
            } catch (Exception e) {
                LOG.error("Failed to create workflow file", e);
                return "";
            }
        });
    }
    
    /**
     * Checks if the user is authenticated.
     *
     * @return Whether the user is authenticated.
     */
    public boolean isAuthenticated() {
        String username = settings.getGitHubUsername();
        String token = settings.getAccessToken();
        
        return username != null && !username.isEmpty() && token != null && !token.isEmpty();
    }
    
    /**
     * Validates credentials with GitHub.
     *
     * @return Whether the credentials are valid.
     */
    public boolean validateCredentials() {
        if (!isAuthenticated()) {
            return false;
        }
        
        try {
            String url = GITHUB_API_URL + "/user";
            String response = sendGitHubRequest(url, "GET", null);
            
            // Check if response contains the username
            return response.contains("\"login\"") && response.contains(settings.getGitHubUsername());
        } catch (Exception e) {
            LOG.error("Failed to validate credentials", e);
            return false;
        }
    }
    
    /**
     * Commits and pushes changes to a GitHub repository.
     *
     * @param project   The project.
     * @param repoPath  The repository path.
     * @param message   The commit message.
     * @return A CompletableFuture with whether the operation was successful.
     */
    public CompletableFuture<Boolean> commitAndPushChanges(
            @NotNull Project project,
            @NotNull String repoPath,
            @NotNull String message) {
        
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot commit and push changes.");
            return CompletableFuture.completedFuture(false);
        }
        
        if (isCommitting.get() || isPushing.get()) {
            LOG.warn("Already committing or pushing. Please wait for the operation to complete.");
            return CompletableFuture.completedFuture(false);
        }
        
        isCommitting.set(true);
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                // This is a mock implementation
                // In real code, use Git integration API to commit and push
                LOG.info("Committing and pushing changes to: " + repoPath + " with message: " + message);
                
                // Simulate some work
                ThreadUtils.sleep(2000);
                
                return true;
            } catch (Exception e) {
                LOG.error("Failed to commit and push changes", e);
                return false;
            } finally {
                isCommitting.set(false);
            }
        });
    }
    
    /**
     * Creates an autonomous workflow in a GitHub repository.
     *
     * @param repoFullName The full repository name (owner/repo).
     * @param branch       The branch to run the workflow on.
     * @return A CompletableFuture with the workflow URL.
     */
    public CompletableFuture<String> createAutonomousWorkflow(
            @NotNull String repoFullName,
            @NotNull String branch) {
        
        if (!isAuthenticated()) {
            LOG.warn("Not authenticated. Cannot create autonomous workflow.");
            return CompletableFuture.completedFuture("");
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                String workflowContent = """
                    name: ModForge Autonomous Development
                    
                    on:
                      push:
                        branches: [ %s ]
                      schedule:
                        - cron: '0 */4 * * *'  # Run every 4 hours
                    
                    jobs:
                      autonomous-development:
                        runs-on: ubuntu-latest
                        steps:
                          - uses: actions/checkout@v3
                          - uses: actions/setup-java@v3
                            with:
                              distribution: 'temurin'
                              java-version: '21'
                          - name: Analyze and Improve Code
                            run: |
                              echo "ModForge autonomous development workflow started"
                              # API calls to ModForge service will be here
                              # Analyze code, suggest improvements, etc.
                          - name: Create Pull Request
                            uses: peter-evans/create-pull-request@v4
                            with:
                              token: ${{ secrets.GITHUB_TOKEN }}
                              commit-message: "Auto-improvement: Code enhancements"
                              title: "Auto-improvement: Code enhancements"
                              body: |
                                This pull request was automatically created by the ModForge autonomous development workflow.
                                
                                Changes include:
                                - Code optimizations
                                - Bug fixes
                                - Documentation improvements
                              branch: autonomous-improvements
                    """.formatted(branch);
                
                return createWorkflowFile(
                        repoFullName,
                        "modforge-autonomous.yml",
                        workflowContent,
                        "Add ModForge autonomous development workflow"
                );
            } catch (Exception e) {
                LOG.error("Failed to create autonomous workflow", e);
                return "";
            }
        });
    }
    
    /**
     * Helper method for sending requests to the GitHub API.
     *
     * @param urlString The URL.
     * @param method    The HTTP method.
     * @param payload   The payload.
     * @return The response.
     * @throws IOException If an I/O error occurs.
     */
    private String sendGitHubRequest(
            @NotNull String urlString,
            @NotNull String method,
            @Nullable String payload) throws IOException {
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", ACCEPT_HEADER);
            connection.setRequestProperty("X-GitHub-Api-Version", GITHUB_VERSION_HEADER);
            
            // Set authentication
            String auth = settings.getGitHubUsername() + ":" + settings.getAccessToken();
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            
            // Set content type if sending payload
            if (payload != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            // Get response
            int status = connection.getResponseCode();
            
            if (status >= 200 && status < 300) {
                // Success
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                // Error
                String errorDetails;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    errorDetails = response.toString();
                }
                
                throw new IOException("HTTP error " + status + ": " + errorDetails);
            }
        } finally {
            connection.disconnect();
        }
    }
}