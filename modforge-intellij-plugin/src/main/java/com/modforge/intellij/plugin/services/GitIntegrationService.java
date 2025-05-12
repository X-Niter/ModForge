package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for Git integration in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.PROJECT)
public final class GitIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitIntegrationService.class);
    
    private final Project project;
    private final ModForgeSettings settings;
    private final ModForgeNotificationService notificationService;

    /**
     * Creates a new instance of the Git integration service.
     *
     * @param project The project.
     */
    public GitIntegrationService(Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance();
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
     * Gets the Git executable path.
     *
     * @return The Git executable path.
     */
    @NotNull
    private String getGitExecutable() {
        String configuredPath = settings.getGitExecutablePath();
        if (!configuredPath.isEmpty()) {
            return configuredPath;
        }
        
        // Use the default Git executable
        return "git";
    }

    /**
     * Executes a Git command.
     *
     * @param args The command arguments.
     * @return The command output.
     * @throws Exception If the command execution fails.
     */
    @NotNull
    private String executeGitCommand(@NotNull List<String> args) throws Exception {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            throw new IllegalStateException("Project base path is null");
        }
        
        List<String> command = new ArrayList<>();
        command.add(getGitExecutable());
        command.addAll(args);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(projectPath));
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Git command failed with exit code " + exitCode + ": " + output);
        }
        
        return output.toString().trim();
    }

    /**
     * Gets all changes in the working directory.
     *
     * @param callback Callback for the changes.
     */
    public void getChanges(@NotNull Consumer<List<Change>> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // In a real implementation, this would use the Git4Idea API to get changes
                // For now, use the git command line to simulate getting changes
                String output = executeGitCommand(List.of("status", "--porcelain"));
                
                // Parse the output to get the changes
                List<Change> changes = new ArrayList<>();
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    // Create a mock Change object for each changed file
                    changes.add(new MockChange(line));
                }
                
                LOG.info("Found " + changes.size() + " changes");
                
                return changes;
            } catch (Exception e) {
                LOG.error("Failed to get changes", e);
                return null;
            }
        }).thenAccept(changes -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(changes != null ? changes : List.of()));
        });
    }

    /**
     * Gets the current branch name.
     *
     * @param callback Callback for the branch name.
     */
    public void getCurrentBranch(@NotNull Consumer<String> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                String branch = executeGitCommand(List.of("rev-parse", "--abbrev-ref", "HEAD"));
                LOG.info("Current branch: " + branch);
                return branch;
            } catch (Exception e) {
                LOG.error("Failed to get current branch", e);
                return "main"; // Default to main if the command fails
            }
        }).thenAccept(branch -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(branch));
        });
    }

    /**
     * Creates a new branch.
     *
     * @param branchName The branch name.
     * @param callback Callback for success.
     */
    public void createBranch(@NotNull String branchName, @NotNull Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                executeGitCommand(List.of("checkout", "-b", branchName));
                LOG.info("Created branch: " + branchName);
                return true;
            } catch (Exception e) {
                LOG.error("Failed to create branch: " + branchName, e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(success));
        });
    }

    /**
     * Switches to a branch.
     *
     * @param branchName The branch name.
     * @param callback Callback for success.
     */
    public void switchBranch(@NotNull String branchName, @NotNull Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                executeGitCommand(List.of("checkout", branchName));
                LOG.info("Switched to branch: " + branchName);
                return true;
            } catch (Exception e) {
                LOG.error("Failed to switch to branch: " + branchName, e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(success));
        });
    }

    /**
     * Commits changes.
     *
     * @param message The commit message.
     * @param files The files to commit.
     * @param callback Callback for success.
     */
    public void commitChanges(
            @NotNull String message,
            @NotNull List<VirtualFile> files,
            @NotNull Consumer<Boolean> callback) {
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Add all files to staging
                executeGitCommand(List.of("add", "-A"));
                
                // Commit the changes
                executeGitCommand(List.of("commit", "-m", message));
                
                LOG.info("Committed changes with message: " + message);
                return true;
            } catch (Exception e) {
                LOG.error("Failed to commit changes", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(success));
        });
    }

    /**
     * Pushes changes to the remote repository.
     *
     * @param callback Callback for success.
     */
    public void pushChanges(@NotNull Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                getCurrentBranch(branch -> {
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            executeGitCommand(List.of("push", "origin", branch));
                            LOG.info("Pushed changes to origin/" + branch);
                            return true;
                        } catch (Exception e) {
                            LOG.error("Failed to push changes to origin/" + branch, e);
                            return false;
                        }
                    }).thenAccept(pushSuccess -> {
                        CompatibilityUtil.executeOnUiThread(() -> callback.accept(pushSuccess));
                    });
                });
                
                // Return null as the result is handled by the nested CompletableFuture
                return null;
            } catch (Exception e) {
                LOG.error("Failed to push changes", e);
                return false;
            }
        }).thenAccept(success -> {
            if (success != null && !success) {
                CompatibilityUtil.executeOnUiThread(() -> callback.accept(false));
            }
        });
    }

    /**
     * Pulls changes from the remote repository.
     *
     * @param callback Callback for success.
     */
    public void pullChanges(@NotNull Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                executeGitCommand(List.of("pull"));
                LOG.info("Pulled changes from remote");
                return true;
            } catch (Exception e) {
                LOG.error("Failed to pull changes", e);
                return false;
            }
        }).thenAccept(success -> {
            CompatibilityUtil.executeOnUiThread(() -> callback.accept(success));
        });
    }

    /**
     * Mock implementation of the Change class for testing.
     */
    private static class MockChange extends Change {
        private final String description;

        MockChange(String description) {
            super(null, null);
            this.description = description;
        }

        @Nullable
        @Override
        public String toString() {
            return description;
        }
    }
}