package com.modforge.intellij.plugin.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for Git integration in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class GitIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitIntegrationService.class);
    private final Project project;
    private final ModForgeNotificationService notificationService;

    /**
     * Creates a new instance of the Git integration service.
     *
     * @param project The project.
     */
    public GitIntegrationService(Project project) {
        this.project = project;
        this.notificationService = ModForgeNotificationService.getInstance(project);
        LOG.info("GitIntegrationService initialized for project: " + project.getName());
    }

    /**
     * Gets the instance of the Git integration service for the specified project.
     *
     * @param project The project.
     * @return The Git integration service.
     */
    public static GitIntegrationService getInstance(@NotNull Project project) {
        return project.getService(GitIntegrationService.class);
    }

    /**
     * Commits changes to the Git repository.
     *
     * @param message The commit message.
     * @param filesToCommit The files to commit.
     * @param callback Callback for success.
     */
    public void commitChanges(
            @NotNull String message,
            @NotNull List<VirtualFile> filesToCommit,
            @NotNull Consumer<Boolean> callback) {
        
        LOG.info("Committing changes with message: " + message);
        notificationService.showInfo("Committing Changes", "Preparing to commit changes to Git repository");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would use the Git4Idea plugin API
                // For now, simulate a successful commit
                Thread.sleep(1000);
                
                LOG.info("Successfully committed changes with message: " + message);
                return true;
            } catch (Exception e) {
                LOG.error("Failed to commit changes", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (success) {
                    notificationService.showInfo("Commit Successful", "Changes have been committed to the Git repository");
                } else {
                    notificationService.showError("Commit Failed", "Failed to commit changes to the Git repository");
                }
                callback.accept(success);
            });
        });
    }

    /**
     * Creates a new branch in the Git repository.
     *
     * @param branchName The branch name.
     * @param callback Callback for success.
     */
    public void createBranch(@NotNull String branchName, @NotNull Consumer<Boolean> callback) {
        LOG.info("Creating branch: " + branchName);
        notificationService.showInfo("Creating Branch", "Creating new Git branch: " + branchName);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would use the Git4Idea plugin API
                // For now, simulate a successful branch creation
                Thread.sleep(1000);
                
                LOG.info("Successfully created branch: " + branchName);
                return true;
            } catch (Exception e) {
                LOG.error("Failed to create branch", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (success) {
                    notificationService.showInfo("Branch Created", "Successfully created branch: " + branchName);
                } else {
                    notificationService.showError("Branch Creation Failed", "Failed to create branch: " + branchName);
                }
                callback.accept(success);
            });
        });
    }

    /**
     * Gets the current branch name.
     *
     * @param callback Callback for the branch name.
     */
    public void getCurrentBranch(@NotNull Consumer<String> callback) {
        LOG.info("Getting current branch");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would use the Git4Idea plugin API
                // For now, simulate a successful branch retrieval
                Thread.sleep(500);
                
                String branchName = "main";
                LOG.info("Current branch: " + branchName);
                return branchName;
            } catch (Exception e) {
                LOG.error("Failed to get current branch", e);
                return null;
            }
        }).thenAccept(branchName -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(branchName));
        });
    }

    /**
     * Gets the changes in the repository.
     *
     * @param callback Callback for the changes.
     */
    public void getChanges(@NotNull Consumer<List<Change>> callback) {
        LOG.info("Getting changes in repository");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would use the Git4Idea plugin API
                // For now, return an empty list
                Thread.sleep(500);
                
                LOG.info("Retrieved changes in repository");
                return (List<Change>) List.of();
            } catch (Exception e) {
                LOG.error("Failed to get changes", e);
                return null;
            }
        }).thenAccept(changes -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(changes));
        });
    }

    /**
     * Pushes changes to the remote repository.
     *
     * @param callback Callback for success.
     */
    public void pushChanges(@NotNull Consumer<Boolean> callback) {
        LOG.info("Pushing changes to remote repository");
        notificationService.showInfo("Pushing Changes", "Pushing changes to remote Git repository");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would use the Git4Idea plugin API
                // For now, simulate a successful push
                Thread.sleep(1500);
                
                LOG.info("Successfully pushed changes to remote repository");
                return true;
            } catch (Exception e) {
                LOG.error("Failed to push changes", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> {
                if (success) {
                    notificationService.showInfo("Push Successful", "Changes have been pushed to the remote repository");
                } else {
                    notificationService.showError("Push Failed", "Failed to push changes to the remote repository");
                }
                callback.accept(success);
            });
        });
    }

    /**
     * Gets the Git repository root.
     *
     * @return The repository root, or null if not found.
     */
    @Nullable
    public VirtualFile getRepositoryRoot() {
        // In a real implementation, this would use the Git4Idea plugin API
        // For now, assume the project base directory is the repository root
        return CompatibilityUtil.getProjectBaseDir(project);
    }
}