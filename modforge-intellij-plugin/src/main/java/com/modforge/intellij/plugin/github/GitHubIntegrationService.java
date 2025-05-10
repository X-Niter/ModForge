package com.modforge.intellij.plugin.github;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for GitHub integration.
 * Handles operations such as pushing mods to GitHub and creating pull requests.
 */
@Service(Service.Level.PROJECT)
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    
    private final Project project;
    private final AtomicBoolean isLinked = new AtomicBoolean(false);
    private String repoOwner;
    private String repoName;
    private String repoUrl;
    
    /**
     * Create a GitHub integration service.
     *
     * @param project The project
     */
    public GitHubIntegrationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Check if the project is linked to a GitHub repository.
     *
     * @return True if linked, false otherwise
     */
    public boolean isLinked() {
        return isLinked.get();
    }
    
    /**
     * Link the project to a GitHub repository.
     *
     * @param owner    The repository owner
     * @param name     The repository name
     * @param repoUrl  The repository URL
     * @return True if linking was successful, false otherwise
     */
    public boolean linkRepository(@NotNull String owner, @NotNull String name, @NotNull String repoUrl) {
        if (isLinked.get()) {
            LOG.info("Project is already linked to a GitHub repository");
            return false;
        }
        
        this.repoOwner = owner;
        this.repoName = name;
        this.repoUrl = repoUrl;
        
        isLinked.set(true);
        LOG.info("Project linked to GitHub repository: " + owner + "/" + name);
        
        return true;
    }
    
    /**
     * Unlink the project from a GitHub repository.
     */
    public void unlinkRepository() {
        if (!isLinked.get()) {
            LOG.info("Project is not linked to a GitHub repository");
            return;
        }
        
        this.repoOwner = null;
        this.repoName = null;
        this.repoUrl = null;
        
        isLinked.set(false);
        LOG.info("Project unlinked from GitHub repository");
    }
    
    /**
     * Get the repository owner.
     *
     * @return The repository owner
     */
    @Nullable
    public String getRepoOwner() {
        return repoOwner;
    }
    
    /**
     * Get the repository name.
     *
     * @return The repository name
     */
    @Nullable
    public String getRepoName() {
        return repoName;
    }
    
    /**
     * Get the repository URL.
     *
     * @return The repository URL
     */
    @Nullable
    public String getRepoUrl() {
        return repoUrl;
    }
    
    /**
     * Create a new GitHub repository for the project.
     *
     * @param repoName        The repository name
     * @param description     The repository description
     * @param isPrivate       Whether the repository should be private
     * @return A CompletableFuture with the repository URL if successful
     */
    @RequiresBackgroundThread
    public CompletableFuture<String> createRepository(
            @NotNull String repoName,
            @NotNull String description,
            boolean isPrivate
    ) {
        // Check if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }
        
        // Set up API request
        String serverUrl = authManager.getServerUrl();
        String token = authManager.getToken();
        
        String createRepoUrl = serverUrl.endsWith("/") ? serverUrl + "github/repo" : serverUrl + "/github/repo";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", repoName);
        requestBody.put("description", description);
        requestBody.put("private", isPrivate);
        
        // Execute request
        return TokenAuthConnectionUtil.executePost(createRepoUrl, requestBody, token)
                .thenApply(response -> {
                    try {
                        Map<String, Object> responseMap = new HashMap<>(); // Parse JSON response
                        String htmlUrl = (String) responseMap.get("html_url");
                        String cloneUrl = (String) responseMap.get("clone_url");
                        
                        // Link project to repository
                        String owner = extractOwnerFromUrl(htmlUrl);
                        linkRepository(owner, repoName, htmlUrl);
                        
                        return htmlUrl;
                    } catch (Exception e) {
                        throw new RuntimeException("Error creating GitHub repository: " + e.getMessage(), e);
                    }
                });
    }
    
    /**
     * Push project files to GitHub.
     *
     * @param files           The files to push
     * @param commitMessage   The commit message
     * @param branch          The branch name
     * @return A CompletableFuture with success status
     */
    @RequiresBackgroundThread
    public CompletableFuture<Boolean> pushFiles(
            @NotNull Map<String, String> files,
            @NotNull String commitMessage,
            @NotNull String branch
    ) {
        if (!isLinked.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Project is not linked to a GitHub repository"));
        }
        
        // Check if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }
        
        // Set up API request
        String serverUrl = authManager.getServerUrl();
        String token = authManager.getToken();
        
        String pushUrl = serverUrl.endsWith("/") ? serverUrl + "github/push" : serverUrl + "/github/push";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("owner", repoOwner);
        requestBody.put("repo", repoName);
        requestBody.put("files", files);
        requestBody.put("message", commitMessage);
        requestBody.put("branch", branch);
        
        // Execute request
        return TokenAuthConnectionUtil.executePost(pushUrl, requestBody, token)
                .thenApply(response -> true)
                .exceptionally(e -> {
                    LOG.error("Error pushing files to GitHub", e);
                    return false;
                });
    }
    
    /**
     * Create a pull request.
     *
     * @param title           The pull request title
     * @param description     The pull request description
     * @param sourceBranch    The source branch
     * @param targetBranch    The target branch
     * @return A CompletableFuture with the pull request URL if successful
     */
    @RequiresBackgroundThread
    public CompletableFuture<String> createPullRequest(
            @NotNull String title,
            @NotNull String description,
            @NotNull String sourceBranch,
            @NotNull String targetBranch
    ) {
        if (!isLinked.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Project is not linked to a GitHub repository"));
        }
        
        // Check if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }
        
        // Set up API request
        String serverUrl = authManager.getServerUrl();
        String token = authManager.getToken();
        
        String prUrl = serverUrl.endsWith("/") ? serverUrl + "github/pr" : serverUrl + "/github/pr";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("owner", repoOwner);
        requestBody.put("repo", repoName);
        requestBody.put("title", title);
        requestBody.put("description", description);
        requestBody.put("sourceBranch", sourceBranch);
        requestBody.put("targetBranch", targetBranch);
        
        // Execute request
        return TokenAuthConnectionUtil.executePost(prUrl, requestBody, token)
                .thenApply(response -> {
                    try {
                        Map<String, Object> responseMap = new HashMap<>(); // Parse JSON response
                        return (String) responseMap.get("html_url");
                    } catch (Exception e) {
                        throw new RuntimeException("Error creating pull request: " + e.getMessage(), e);
                    }
                });
    }
    
    /**
     * Extract owner from repository URL.
     *
     * @param url The repository URL
     * @return The owner
     */
    @NotNull
    private String extractOwnerFromUrl(@NotNull String url) {
        try {
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            String[] parts = path.split("/");
            if (parts.length >= 1) {
                return parts[0];
            }
        } catch (Exception e) {
            LOG.error("Error extracting owner from URL: " + url, e);
        }
        
        return "";
    }
}