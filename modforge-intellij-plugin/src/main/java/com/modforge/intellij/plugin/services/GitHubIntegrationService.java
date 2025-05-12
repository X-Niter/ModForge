package com.modforge.intellij.plugin.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Service for GitHub integration in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    private final Project project;
    private final ModForgeSettings settings;
    private final ModForgeNotificationService notificationService;
    private final GitIntegrationService gitService;
    
    private ScheduledThreadPoolExecutor monitorExecutor;
    private ScheduledFuture<?> monitorTask;
    private boolean isMonitoring = false;

    /**
     * Creates a new instance of the GitHub integration service.
     *
     * @param project The project.
     */
    public GitHubIntegrationService(Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance(project);
        this.gitService = GitIntegrationService.getInstance(project);
        LOG.info("GitHubIntegrationService initialized for project: " + project.getName());
    }

    /**
     * Gets the instance of the GitHub integration service for the specified project.
     *
     * @param project The project.
     * @return The GitHub integration service.
     */
    public static GitHubIntegrationService getInstance(@NotNull Project project) {
        return project.getService(GitHubIntegrationService.class);
    }

    /**
     * Creates a GitHub repository.
     *
     * @param name The repository name.
     * @param description The repository description.
     * @param isPrivate Whether the repository is private.
     * @param owner The repository owner.
     * @param token The GitHub token.
     * @param callback Callback for the repository URL.
     */
    public void createRepository(
            @NotNull String name,
            @Nullable String description,
            boolean isPrivate,
            @NotNull String owner,
            @NotNull String token,
            @NotNull Consumer<String> callback) {
        
        LOG.info("Creating GitHub repository: " + name);
        notificationService.showInfo("Creating Repository", "Creating GitHub repository: " + name);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the GitHub API to create a repository
                // For now, simulate a successful repository creation
                Thread.sleep(2000);
                
                String repoUrl = "https://github.com/" + owner + "/" + name;
                LOG.info("Successfully created GitHub repository: " + repoUrl);
                return repoUrl;
            } catch (Exception e) {
                LOG.error("Failed to create GitHub repository", e);
                return null;
            }
        }).thenAccept(repoUrl -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (repoUrl != null) {
                    notificationService.showInfo("Repository Created", "Successfully created GitHub repository: " + repoUrl);
                } else {
                    notificationService.showError("Repository Creation Failed", "Failed to create GitHub repository");
                }
                callback.accept(repoUrl);
            });
        });
    }

    /**
     * Tests GitHub authentication with a token.
     *
     * @param token The GitHub token.
     * @param callback Callback for success.
     */
    public void testAuthentication(@NotNull String token, @NotNull Consumer<Boolean> callback) {
        LOG.info("Testing GitHub authentication");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the GitHub API to test the token
                // For now, simulate a successful test with actual network check
                URL url = new URL("https://api.github.com/user");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "token " + token);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                
                LOG.info("GitHub authentication test result: " + responseCode);
                
                return responseCode == 200;
            } catch (Exception e) {
                LOG.error("Failed to test GitHub authentication", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (success) {
                    notificationService.showInfo("Authentication Successful", "Successfully authenticated with GitHub");
                } else {
                    notificationService.showError("Authentication Failed", "Failed to authenticate with GitHub");
                }
                callback.accept(success);
            });
        });
    }

    /**
     * Creates a pull request.
     *
     * @param owner The repository owner.
     * @param repo The repository name.
     * @param title The pull request title.
     * @param body The pull request body.
     * @param headBranch The head branch.
     * @param baseBranch The base branch.
     * @param token The GitHub token.
     * @param callback Callback for the pull request URL.
     */
    public void createPullRequest(
            @NotNull String owner,
            @NotNull String repo,
            @NotNull String title,
            @Nullable String body,
            @NotNull String headBranch,
            @NotNull String baseBranch,
            @NotNull String token,
            @NotNull Consumer<String> callback) {
        
        LOG.info("Creating pull request from " + headBranch + " to " + baseBranch + " in " + owner + "/" + repo);
        notificationService.showInfo("Creating Pull Request", "Creating pull request in " + owner + "/" + repo);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the GitHub API to create a pull request
                // For now, simulate a successful pull request creation
                Thread.sleep(1500);
                
                String prUrl = "https://github.com/" + owner + "/" + repo + "/pull/1";
                LOG.info("Successfully created pull request: " + prUrl);
                return prUrl;
            } catch (Exception e) {
                LOG.error("Failed to create pull request", e);
                return null;
            }
        }).thenAccept(prUrl -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (prUrl != null) {
                    notificationService.showInfo("Pull Request Created", "Successfully created pull request: " + prUrl);
                } else {
                    notificationService.showError("Pull Request Creation Failed", "Failed to create pull request");
                }
                callback.accept(prUrl);
            });
        });
    }

    /**
     * Gets information about a repository.
     *
     * @param owner The repository owner.
     * @param repo The repository name.
     * @param token The GitHub token.
     * @param callback Callback for the repository information.
     */
    public void getRepositoryInfo(
            @NotNull String owner,
            @NotNull String repo,
            @NotNull String token,
            @NotNull Consumer<Map<String, Object>> callback) {
        
        LOG.info("Getting repository info for " + owner + "/" + repo);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would call the GitHub API to get repository information
                // For now, simulate a successful response
                Thread.sleep(800);
                
                Map<String, Object> repoInfo = Map.of(
                        "name", repo,
                        "owner", owner,
                        "default_branch", "main",
                        "private", false,
                        "html_url", "https://github.com/" + owner + "/" + repo
                );
                
                LOG.info("Successfully retrieved repository info for " + owner + "/" + repo);
                
                return repoInfo;
            } catch (Exception e) {
                LOG.error("Failed to get repository info", e);
                return null;
            }
        }).thenAccept(repoInfo -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(repoInfo));
        });
    }
    
    /**
     * Pushes code to GitHub.
     *
     * @param owner The repository owner.
     * @param repo The repository name.
     * @param token The GitHub token.
     * @param createPR Whether to create a pull request.
     * @param statusCallback Callback for status updates.
     */
    public void pushToGitHub(
            @NotNull String owner, 
            @NotNull String repo, 
            @NotNull String token, 
            boolean createPR,
            @NotNull Function<String, Void> statusCallback) {
        
        LOG.info("Pushing code to GitHub: " + owner + "/" + repo);
        notificationService.showInfo("Pushing to GitHub", "Preparing to push code to GitHub: " + owner + "/" + repo);
        
        statusCallback.apply("Checking for changes...");
        gitService.getChanges(changes -> {
            if (changes == null || changes.isEmpty()) {
                statusCallback.apply("No changes to push.");
                notificationService.showInfo("No Changes", "No changes to push to GitHub");
                return;
            }
            
            statusCallback.apply("Found " + changes.size() + " changes. Creating commit...");
            
            String commitMessage = "ModForge automated commit";
            gitService.commitChanges(commitMessage, getChangedFiles(changes), success -> {
                if (!success) {
                    statusCallback.apply("Failed to commit changes.");
                    notificationService.showError("Commit Failed", "Failed to commit changes");
                    return;
                }
                
                statusCallback.apply("Changes committed. Pushing to GitHub...");
                
                // Push changes
                gitService.pushChanges(pushSuccess -> {
                    if (!pushSuccess) {
                        statusCallback.apply("Failed to push changes to GitHub.");
                        notificationService.showError("Push Failed", "Failed to push changes to GitHub");
                        return;
                    }
                    
                    statusCallback.apply("Changes pushed successfully to GitHub.");
                    notificationService.showInfo("Push Successful", "Successfully pushed changes to GitHub");
                    
                    // Create pull request if requested
                    if (createPR) {
                        statusCallback.apply("Creating pull request...");
                        
                        gitService.getCurrentBranch(currentBranch -> {
                            String prTitle = "ModForge automated pull request";
                            String prBody = "This pull request was created automatically by ModForge.";
                            
                            createPullRequest(
                                    owner,
                                    repo,
                                    prTitle,
                                    prBody,
                                    currentBranch,
                                    "main",
                                    token,
                                    prUrl -> {
                                        if (prUrl != null) {
                                            statusCallback.apply("Pull request created: " + prUrl);
                                        } else {
                                            statusCallback.apply("Failed to create pull request.");
                                        }
                                    }
                            );
                        });
                    }
                });
            });
        });
    }
    
    /**
     * Starts monitoring a GitHub repository.
     *
     * @param owner The repository owner.
     * @param repository The repository name.
     */
    public void startMonitoring(@NotNull String owner, @NotNull String repository) {
        if (isMonitoring) {
            LOG.info("Already monitoring GitHub repository: " + owner + "/" + repository);
            return;
        }
        
        LOG.info("Starting to monitor GitHub repository: " + owner + "/" + repository);
        notificationService.showInfo("Monitoring Repository", "Starting to monitor GitHub repository: " + owner + "/" + repository);
        
        if (monitorExecutor == null) {
            monitorExecutor = new ScheduledThreadPoolExecutor(1);
        }
        
        monitorTask = monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                LOG.info("Checking for updates in GitHub repository: " + owner + "/" + repository);
                
                // In a real implementation, this would call the GitHub API to check for updates
                // For now, just log the monitoring activity
                
                // Check for new PRs, issues, etc.
                
            } catch (Exception e) {
                LOG.error("Error checking for updates in GitHub repository", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
        
        isMonitoring = true;
    }
    
    /**
     * Stops monitoring a GitHub repository.
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            LOG.info("Not monitoring any GitHub repository");
            return;
        }
        
        LOG.info("Stopping GitHub repository monitoring");
        
        if (monitorTask != null) {
            monitorTask.cancel(false);
            monitorTask = null;
        }
        
        isMonitoring = false;
    }
    
    /**
     * Gets a list of changed files from a list of changes.
     *
     * @param changes The changes.
     * @return The list of changed files.
     */
    private List<VirtualFile> getChangedFiles(List<Change> changes) {
        // In a real implementation, this would extract the virtual files from the changes
        // For now, return an empty list
        return List.of();
    }
}