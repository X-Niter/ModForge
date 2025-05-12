package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for GitHub integration in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.APP)
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    private final ModForgeSettings settings;

    /**
     * Gets the singleton instance of the GitHub integration service.
     *
     * @return The GitHub integration service.
     */
    public static GitHubIntegrationService getInstance() {
        return ApplicationManager.getApplication().getService(GitHubIntegrationService.class);
    }

    /**
     * Creates a new instance of the GitHub integration service.
     */
    public GitHubIntegrationService() {
        this.settings = ModForgeSettings.getInstance();
        LOG.info("GitHubIntegrationService initialized");
    }

    /**
     * Checks if GitHub integration is configured.
     *
     * @return True if configured, false otherwise.
     */
    public boolean isConfigured() {
        return settings.getGitHubUsername() != null && !settings.getGitHubUsername().isEmpty() &&
               settings.getAccessToken() != null && !settings.getAccessToken().isEmpty();
    }

    /**
     * Creates a GitHub pull request.
     *
     * @param title The PR title.
     * @param body The PR body.
     * @param baseBranch The base branch.
     * @param headBranch The head branch.
     * @param repositoryOwner The repository owner.
     * @param repositoryName The repository name.
     * @param callback Callback for the PR URL.
     */
    public void createPullRequest(
            String title,
            String body,
            String baseBranch,
            String headBranch,
            String repositoryOwner,
            String repositoryName,
            Consumer<String> callback) {
        
        if (!isConfigured()) {
            callback.accept(null);
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Creating GitHub pull request for " + repositoryOwner + "/" + repositoryName);
                
                // In a real implementation, this would use the GitHub API
                // For now, simulate a successful API call
                Thread.sleep(1000);
                
                String prUrl = "https://github.com/" + repositoryOwner + "/" + repositoryName + "/pull/1";
                LOG.info("Created GitHub pull request: " + prUrl);
                
                return prUrl;
            } catch (Exception e) {
                LOG.error("Failed to create GitHub pull request", e);
                return null;
            }
        }).thenAccept(prUrl -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(prUrl));
        });
    }

    /**
     * Gets GitHub issue details.
     *
     * @param repositoryOwner The repository owner.
     * @param repositoryName The repository name.
     * @param issueNumber The issue number.
     * @param callback Callback for the issue details.
     */
    public void getIssueDetails(
            String repositoryOwner,
            String repositoryName,
            int issueNumber,
            Consumer<String> callback) {
        
        if (!isConfigured()) {
            callback.accept(null);
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Getting GitHub issue details for " + repositoryOwner + "/" + repositoryName + "#" + issueNumber);
                
                // In a real implementation, this would use the GitHub API
                // For now, simulate a successful API call
                Thread.sleep(1000);
                
                String issueDetails = "Title: Example Issue\n" +
                                    "Body: This is an example issue for testing the GitHub integration.\n" +
                                    "Labels: enhancement, good first issue\n" +
                                    "Status: open";
                
                LOG.info("Got GitHub issue details for " + repositoryOwner + "/" + repositoryName + "#" + issueNumber);
                
                return issueDetails;
            } catch (Exception e) {
                LOG.error("Failed to get GitHub issue details", e);
                return null;
            }
        }).thenAccept(issueDetails -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(issueDetails));
        });
    }

    /**
     * Posts a comment on a GitHub issue.
     *
     * @param repositoryOwner The repository owner.
     * @param repositoryName The repository name.
     * @param issueNumber The issue number.
     * @param comment The comment text.
     * @param callback Callback for success.
     */
    public void postIssueComment(
            String repositoryOwner,
            String repositoryName,
            int issueNumber,
            String comment,
            Consumer<Boolean> callback) {
        
        if (!isConfigured()) {
            callback.accept(false);
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Posting comment on GitHub issue " + repositoryOwner + "/" + repositoryName + "#" + issueNumber);
                
                // In a real implementation, this would use the GitHub API
                // For now, simulate a successful API call
                Thread.sleep(1000);
                
                LOG.info("Posted comment on GitHub issue " + repositoryOwner + "/" + repositoryName + "#" + issueNumber);
                
                return true;
            } catch (Exception e) {
                LOG.error("Failed to post GitHub issue comment", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(success));
        });
    }

    /**
     * Gets the repository details.
     *
     * @param repositoryOwner The repository owner.
     * @param repositoryName The repository name.
     * @param callback Callback for the repository details.
     */
    public void getRepositoryDetails(
            String repositoryOwner,
            String repositoryName,
            Consumer<String> callback) {
        
        if (!isConfigured()) {
            callback.accept(null);
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Getting GitHub repository details for " + repositoryOwner + "/" + repositoryName);
                
                // In a real implementation, this would use the GitHub API
                // For now, simulate a successful API call
                Thread.sleep(1000);
                
                String repoDetails = "Name: " + repositoryName + "\n" +
                                  "Owner: " + repositoryOwner + "\n" +
                                  "Stars: 42\n" +
                                  "Forks: 13\n" +
                                  "Open Issues: 7\n" +
                                  "Default Branch: main";
                
                LOG.info("Got GitHub repository details for " + repositoryOwner + "/" + repositoryName);
                
                return repoDetails;
            } catch (Exception e) {
                LOG.error("Failed to get GitHub repository details", e);
                return null;
            }
        }).thenAccept(repoDetails -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(repoDetails));
        });
    }
}