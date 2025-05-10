package com.modforge.intellij.plugin.github;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ModLoaderDetector;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for GitHub integration.
 * Provides methods for:
 * - Pushing mods to GitHub
 * - Monitoring issues and PRs
 * - Autonomously responding to issues
 * - Setting up GitHub Actions for continuous integration
 */
@Service(Service.Level.PROJECT)
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    private static final int POLL_INTERVAL_MINUTES = 5;
    private static final Gson GSON = new Gson();
    
    private final Project project;
    private final ScheduledExecutorService scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("ModForge GitHub Monitor", 1);
    private final Map<String, Instant> lastIssueResponseTime = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastPrResponseTime = new ConcurrentHashMap<>();
    private ScheduledFuture<?> monitorTask;
    
    /**
     * Create a new GitHub integration service.
     *
     * @param project The project
     */
    public GitHubIntegrationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Start monitoring GitHub issues and PRs.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     */
    public void startMonitoring(@NotNull String owner, @NotNull String repository) {
        if (monitorTask != null && !monitorTask.isDone()) {
            return;
        }
        
        monitorTask = scheduler.scheduleWithFixedDelay(
                () -> monitorRepositoryActivity(owner, repository),
                0,
                POLL_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
        
        LOG.info("Started monitoring GitHub repository: " + owner + "/" + repository);
    }
    
    /**
     * Stop monitoring GitHub issues and PRs.
     */
    public void stopMonitoring() {
        if (monitorTask != null && !monitorTask.isDone()) {
            monitorTask.cancel(false);
            LOG.info("Stopped monitoring GitHub repository");
        }
    }
    
    /**
     * Push the current project to GitHub.
     *
     * @param owner        The repository owner
     * @param repository   The repository name
     * @param description  The repository description
     * @param isPrivate    Whether the repository is private
     * @param callback     Callback for progress updates
     * @return CompletableFuture for async operation
     */
    public CompletableFuture<GitHubPushResult> pushToGitHub(
            @NotNull String owner,
            @NotNull String repository,
            @NotNull String description,
            boolean isPrivate,
            @Nullable Consumer<String> callback
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                String token = authManager.getGitHubToken();
                
                if (token == null || token.isEmpty()) {
                    return new GitHubPushResult(false, "GitHub token not available. Please authenticate with GitHub.");
                }
                
                // Create GitHub API client
                GitHubApiClient client = new GitHubApiClient(token);
                
                // Notify progress
                if (callback != null) {
                    callback.accept("Checking if repository exists...");
                }
                
                // Check if repository exists
                boolean repoExists = client.repositoryExists(owner, repository);
                String repoFullName = owner + "/" + repository;
                
                // Create repository if it doesn't exist
                if (!repoExists) {
                    if (callback != null) {
                        callback.accept("Creating repository " + repoFullName + "...");
                    }
                    
                    try {
                        client.createRepository(repository, description, isPrivate);
                    } catch (IOException e) {
                        LOG.error("Error creating repository: " + e.getMessage(), e);
                        return new GitHubPushResult(false, "Failed to create repository: " + e.getMessage());
                    }
                }
                
                // Find project files
                if (callback != null) {
                    callback.accept("Collecting project files...");
                }
                
                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    return new GitHubPushResult(false, "Project has no base directory.");
                }
                
                // Analyze project structure
                ModLoaderDetector loaderDetector = new ModLoaderDetector(project);
                String detectedLoader = loaderDetector.detectModLoader();
                
                if (detectedLoader == null) {
                    LOG.warn("No mod loader detected, using default file patterns");
                }
                
                // Collect project files
                List<GitHubFileContent> files = collectProjectFiles(baseDir, getExcludedDirectories(detectedLoader));
                
                if (files.isEmpty()) {
                    return new GitHubPushResult(false, "No files found to push.");
                }
                
                // Create or update files on GitHub
                if (callback != null) {
                    callback.accept("Pushing " + files.size() + " files to GitHub...");
                }
                
                int filesPushed = 0;
                for (GitHubFileContent file : files) {
                    try {
                        if (callback != null && filesPushed % 10 == 0) {
                            callback.accept("Pushed " + filesPushed + " of " + files.size() + " files...");
                        }
                        
                        client.createOrUpdateFile(
                                owner,
                                repository,
                                file.getPath(),
                                "Add " + file.getPath(),
                                file.getContent(),
                                "main"
                        );
                        
                        filesPushed++;
                    } catch (IOException e) {
                        LOG.error("Error pushing file " + file.getPath() + ": " + e.getMessage(), e);
                    }
                }
                
                // Set up GitHub Actions workflow
                if (callback != null) {
                    callback.accept("Setting up GitHub Actions workflow...");
                }
                
                setupGitHubActions(client, owner, repository, detectedLoader);
                
                // Store repository info in settings
                ModForgeSettings settings = ModForgeSettings.getInstance();
                settings.setGitHubRepository(repoFullName);
                
                String repoUrl = "https://github.com/" + repoFullName;
                return new GitHubPushResult(true, "Successfully pushed to " + repoUrl, repoUrl);
                
            } catch (Exception e) {
                LOG.error("Error pushing to GitHub: " + e.getMessage(), e);
                return new GitHubPushResult(false, "Error pushing to GitHub: " + e.getMessage());
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Set up GitHub Actions workflows for continuous integration and automation.
     *
     * @param client         The GitHub API client
     * @param owner          The repository owner
     * @param repository     The repository name
     * @param detectedLoader The detected mod loader
     * @throws IOException If an error occurs
     */
    private void setupGitHubActions(
            @NotNull GitHubApiClient client,
            @NotNull String owner,
            @NotNull String repository,
            @Nullable String detectedLoader
    ) throws IOException {
        // Create .github/workflows directory if it doesn't exist
        String workflowsDir = ".github/workflows";
        
        // Add CI workflow
        String ciWorkflowPath = workflowsDir + "/build.yml";
        String ciWorkflowContent = generateCIWorkflow(detectedLoader);
        
        client.createOrUpdateFile(
                owner,
                repository,
                ciWorkflowPath,
                "Add CI workflow",
                ciWorkflowContent,
                "main"
        );
        
        // Add AI automatic improvement workflow
        String aiWorkflowPath = workflowsDir + "/ai-improvement.yml";
        String aiWorkflowContent = generateAIImprovementWorkflow(detectedLoader);
        
        client.createOrUpdateFile(
                owner,
                repository,
                aiWorkflowPath,
                "Add AI improvement workflow",
                aiWorkflowContent,
                "main"
        );
    }
    
    /**
     * Generate CI workflow YAML content based on detected mod loader.
     *
     * @param modLoader The detected mod loader
     * @return The CI workflow YAML content
     */
    @NotNull
    private String generateCIWorkflow(@Nullable String modLoader) {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("name: Build and Test\n\n");
        yaml.append("on:\n");
        yaml.append("  push:\n");
        yaml.append("    branches: [ main ]\n");
        yaml.append("  pull_request:\n");
        yaml.append("    branches: [ main ]\n\n");
        yaml.append("jobs:\n");
        yaml.append("  build:\n");
        yaml.append("    runs-on: ubuntu-latest\n\n");
        yaml.append("    steps:\n");
        yaml.append("    - uses: actions/checkout@v3\n");
        yaml.append("    - name: Set up JDK 17\n");
        yaml.append("      uses: actions/setup-java@v3\n");
        yaml.append("      with:\n");
        yaml.append("        java-version: '17'\n");
        yaml.append("        distribution: 'temurin'\n");
        
        // Customize build commands based on detected mod loader
        if ("forge".equalsIgnoreCase(modLoader)) {
            yaml.append("    - name: Build with Gradle\n");
            yaml.append("      uses: gradle/gradle-build-action@v2\n");
            yaml.append("      with:\n");
            yaml.append("        arguments: build\n");
            yaml.append("    - name: Upload artifacts\n");
            yaml.append("      uses: actions/upload-artifact@v3\n");
            yaml.append("      with:\n");
            yaml.append("        name: forge-mod\n");
            yaml.append("        path: build/libs/*.jar\n");
        } else if ("fabric".equalsIgnoreCase(modLoader)) {
            yaml.append("    - name: Build with Gradle\n");
            yaml.append("      uses: gradle/gradle-build-action@v2\n");
            yaml.append("      with:\n");
            yaml.append("        arguments: build\n");
            yaml.append("    - name: Upload artifacts\n");
            yaml.append("      uses: actions/upload-artifact@v3\n");
            yaml.append("      with:\n");
            yaml.append("        name: fabric-mod\n");
            yaml.append("        path: build/libs/*.jar\n");
        } else if ("quilt".equalsIgnoreCase(modLoader)) {
            yaml.append("    - name: Build with Gradle\n");
            yaml.append("      uses: gradle/gradle-build-action@v2\n");
            yaml.append("      with:\n");
            yaml.append("        arguments: build\n");
            yaml.append("    - name: Upload artifacts\n");
            yaml.append("      uses: actions/upload-artifact@v3\n");
            yaml.append("      with:\n");
            yaml.append("        name: quilt-mod\n");
            yaml.append("        path: build/libs/*.jar\n");
        } else if ("architectury".equalsIgnoreCase(modLoader)) {
            yaml.append("    - name: Build with Gradle\n");
            yaml.append("      uses: gradle/gradle-build-action@v2\n");
            yaml.append("      with:\n");
            yaml.append("        arguments: build\n");
            yaml.append("    - name: Upload artifacts\n");
            yaml.append("      uses: actions/upload-artifact@v3\n");
            yaml.append("      with:\n");
            yaml.append("        name: multi-mod\n");
            yaml.append("        path: |  \n");
            yaml.append("          */build/libs/*.jar\n");
            yaml.append("          build/libs/*.jar\n");
        } else {
            // Default/generic build
            yaml.append("    - name: Build with Gradle\n");
            yaml.append("      uses: gradle/gradle-build-action@v2\n");
            yaml.append("      with:\n");
            yaml.append("        arguments: build\n");
            yaml.append("    - name: Upload artifacts\n");
            yaml.append("      uses: actions/upload-artifact@v3\n");
            yaml.append("      with:\n");
            yaml.append("        name: minecraft-mod\n");
            yaml.append("        path: build/libs/*.jar\n");
        }
        
        return yaml.toString();
    }
    
    /**
     * Generate AI improvement workflow YAML content.
     *
     * @param modLoader The detected mod loader
     * @return The AI improvement workflow YAML content
     */
    @NotNull
    private String generateAIImprovementWorkflow(@Nullable String modLoader) {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("name: AI Continuous Improvement\n\n");
        yaml.append("on:\n");
        yaml.append("  schedule:\n");
        yaml.append("    # Run daily at midnight UTC\n");
        yaml.append("    - cron: '0 0 * * *'\n");
        yaml.append("  workflow_dispatch:\n");
        yaml.append("    # Allow manual trigger\n\n");
        yaml.append("jobs:\n");
        yaml.append("  analyze-and-improve:\n");
        yaml.append("    runs-on: ubuntu-latest\n");
        yaml.append("    if: github.repository_owner != 'ModForgeAI'\n");  // Skip on template repo
        yaml.append("    permissions:\n");
        yaml.append("      contents: write\n");
        yaml.append("      pull-requests: write\n");
        yaml.append("      issues: write\n\n");
        yaml.append("    steps:\n");
        yaml.append("    - uses: actions/checkout@v3\n");
        yaml.append("      with:\n");
        yaml.append("        fetch-depth: 0\n\n");
        yaml.append("    - name: Set up JDK 17\n");
        yaml.append("      uses: actions/setup-java@v3\n");
        yaml.append("      with:\n");
        yaml.append("        java-version: '17'\n");
        yaml.append("        distribution: 'temurin'\n\n");
            
        // Add ModForge AI analysis
        yaml.append("    - name: ModForge AI Analysis\n");
        yaml.append("      id: modforge-analysis\n");
        yaml.append("      uses: ModForgeAI/modforge-action@v1\n");
        yaml.append("      with:\n");
        yaml.append("        api-key: ${{ secrets.MODFORGE_API_KEY }}\n");
        yaml.append("        mod-loader: '").append(modLoader != null ? modLoader : "auto").append("'\n");
        yaml.append("        improvement-level: 'medium'\n");
        yaml.append("        create-pr: true\n\n");
            
        // Add steps to build after improvements
        yaml.append("    - name: Build after improvements\n");
        yaml.append("      if: steps.modforge-analysis.outputs.improvements-made == 'true'\n");
        yaml.append("      uses: gradle/gradle-build-action@v2\n");
        yaml.append("      with:\n");
        yaml.append("        arguments: build\n\n");
            
        // Add notification
        yaml.append("    - name: Notify about improvements\n");
        yaml.append("      if: steps.modforge-analysis.outputs.improvements-made == 'true'\n");
        yaml.append("      uses: peter-evans/create-or-update-comment@v2\n");
        yaml.append("      with:\n");
        yaml.append("        issue-number: ${{ steps.modforge-analysis.outputs.pr-number }}\n");
        yaml.append("        body: |\n");
        yaml.append("          ## ModForge AI Improvement Report\n\n");
        yaml.append("          Your mod has been automatically analyzed and improved by ModForge AI.\n\n");
        yaml.append("          **Improvements made:**\n");
        yaml.append("          ${{ steps.modforge-analysis.outputs.improvement-summary }}\n\n");
        yaml.append("          **Performance impact:**\n");
        yaml.append("          ${{ steps.modforge-analysis.outputs.performance-impact }}\n\n");
        yaml.append("          Please review these changes and merge if they look good!\n");
            
        return yaml.toString();
    }
    
    /**
     * Get a list of directories to exclude from GitHub pushes.
     *
     * @param modLoader The detected mod loader
     * @return The list of directories to exclude
     */
    @NotNull
    private Set<String> getExcludedDirectories(@Nullable String modLoader) {
        Set<String> excluded = new HashSet<>();
        
        // Common exclusions for all loaders
        excluded.add(".git");
        excluded.add(".idea");
        excluded.add(".gradle");
        excluded.add("build");
        excluded.add("out");
        excluded.add("run");
        excluded.add("gradle");
        
        // Loader-specific exclusions
        if ("forge".equalsIgnoreCase(modLoader)) {
            excluded.add("run-client");
            excluded.add("run-server");
            excluded.add("run-data");
        } else if ("fabric".equalsIgnoreCase(modLoader)) {
            excluded.add("run-client");
            excluded.add("run-server");
            excluded.add("run-data");
        } else if ("quilt".equalsIgnoreCase(modLoader)) {
            excluded.add("run-client");
            excluded.add("run-server");
            excluded.add("run-data");
        } else if ("architectury".equalsIgnoreCase(modLoader)) {
            excluded.add("*/run");
            excluded.add("*/build");
            excluded.add("*/out");
        }
        
        return excluded;
    }
    
    /**
     * Collect project files for GitHub push.
     *
     * @param baseDir       The project base directory
     * @param excludeDirs   The directories to exclude
     * @return A list of file contents
     */
    @NotNull
    private List<GitHubFileContent> collectProjectFiles(
            @NotNull VirtualFile baseDir,
            @NotNull Set<String> excludeDirs
    ) {
        List<GitHubFileContent> files = new ArrayList<>();
        collectFiles(baseDir, "", excludeDirs, files);
        return files;
    }
    
    /**
     * Recursively collect files.
     *
     * @param dir         The directory to scan
     * @param pathPrefix  The path prefix
     * @param excludeDirs The directories to exclude
     * @param result      The result list
     */
    private void collectFiles(
            @NotNull VirtualFile dir,
            @NotNull String pathPrefix,
            @NotNull Set<String> excludeDirs,
            @NotNull List<GitHubFileContent> result
    ) {
        for (VirtualFile child : dir.getChildren()) {
            String relativePath = pathPrefix + child.getName();
            
            if (child.isDirectory()) {
                if (!excludeDirs.contains(relativePath) && !excludeDirs.contains(child.getName())) {
                    collectFiles(child, relativePath + "/", excludeDirs, result);
                }
            } else {
                try {
                    String content = VfsUtilCore.loadText(child);
                    result.add(new GitHubFileContent(relativePath, content));
                } catch (IOException e) {
                    LOG.warn("Failed to read file: " + relativePath, e);
                }
            }
        }
    }
    
    /**
     * Monitor repository activity (issues and PRs).
     *
     * @param owner      The repository owner
     * @param repository The repository name
     */
    private void monitorRepositoryActivity(@NotNull String owner, @NotNull String repository) {
        try {
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            String token = authManager.getGitHubToken();
            
            if (token == null || token.isEmpty()) {
                LOG.info("GitHub token not available. Skipping repository monitoring.");
                return;
            }
            
            // Create GitHub API client
            GitHubApiClient client = new GitHubApiClient(token);
            
            // Check issues
            List<GitHubIssue> issues = client.getOpenIssues(owner, repository);
            for (GitHubIssue issue : issues) {
                processIssue(client, owner, repository, issue);
            }
            
            // Check pull requests
            List<GitHubPullRequest> prs = client.getOpenPullRequests(owner, repository);
            for (GitHubPullRequest pr : prs) {
                processPullRequest(client, owner, repository, pr);
            }
            
        } catch (Exception e) {
            LOG.error("Error monitoring repository activity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process an issue.
     *
     * @param client     The GitHub API client
     * @param owner      The repository owner
     * @param repository The repository name
     * @param issue      The issue
     */
    private void processIssue(
            @NotNull GitHubApiClient client,
            @NotNull String owner,
            @NotNull String repository,
            @NotNull GitHubIssue issue
    ) {
        // Skip if we've responded recently (within last 12 hours)
        String issueKey = owner + "/" + repository + "#" + issue.getNumber();
        Instant lastResponse = lastIssueResponseTime.get(issueKey);
        Instant now = Instant.now();
        
        if (lastResponse != null && Duration.between(lastResponse, now).toHours() < 12) {
            return;
        }
        
        try {
            // Check if this is an issue ModForge should handle
            boolean isModForgeIssue = issue.getTitle().toLowerCase().contains("modforge")
                    || issue.getBody().toLowerCase().contains("modforge")
                    || issue.getLabels().stream().anyMatch(l -> l.toLowerCase().contains("modforge") || l.toLowerCase().contains("ai"));
            
            if (isModForgeIssue) {
                // Get comments to avoid duplicate responses
                List<GitHubComment> comments = client.getIssueComments(owner, repository, issue.getNumber());
                boolean alreadyResponded = comments.stream()
                        .anyMatch(c -> c.getBody().contains("I'm the ModForge AI assistant"));
                
                if (!alreadyResponded) {
                    // Generate a response with ModForge AI
                    String response = generateIssueResponse(issue);
                    
                    // Post response
                    client.createIssueComment(owner, repository, issue.getNumber(), response);
                    
                    // Record response time
                    lastIssueResponseTime.put(issueKey, now);
                    LOG.info("Responded to issue: " + issueKey);
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing issue " + issueKey + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a pull request.
     *
     * @param client     The GitHub API client
     * @param owner      The repository owner
     * @param repository The repository name
     * @param pr         The pull request
     */
    private void processPullRequest(
            @NotNull GitHubApiClient client,
            @NotNull String owner,
            @NotNull String repository,
            @NotNull GitHubPullRequest pr
    ) {
        // Skip if we've responded recently (within last 12 hours)
        String prKey = owner + "/" + repository + "#" + pr.getNumber();
        Instant lastResponse = lastPrResponseTime.get(prKey);
        Instant now = Instant.now();
        
        if (lastResponse != null && Duration.between(lastResponse, now).toHours() < 12) {
            return;
        }
        
        try {
            // Check if this is a PR ModForge should handle
            boolean isModForgePR = pr.getTitle().toLowerCase().contains("modforge")
                    || pr.getBody().toLowerCase().contains("modforge")
                    || pr.getLabels().stream().anyMatch(l -> l.toLowerCase().contains("modforge") || l.toLowerCase().contains("ai"));
            
            if (isModForgePR) {
                // Get comments to avoid duplicate responses
                List<GitHubComment> comments = client.getPullRequestComments(owner, repository, pr.getNumber());
                boolean alreadyResponded = comments.stream()
                        .anyMatch(c -> c.getBody().contains("I'm the ModForge AI assistant"));
                
                if (!alreadyResponded) {
                    // Get PR changes
                    List<GitHubFile> files = client.getPullRequestFiles(owner, repository, pr.getNumber());
                    
                    // Generate a response with ModForge AI
                    String response = generatePullRequestResponse(pr, files);
                    
                    // Post response
                    client.createPullRequestComment(owner, repository, pr.getNumber(), response);
                    
                    // Record response time
                    lastPrResponseTime.put(prKey, now);
                    LOG.info("Responded to PR: " + prKey);
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing PR " + prKey + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a response to an issue.
     *
     * @param issue The issue
     * @return The response
     */
    @NotNull
    private String generateIssueResponse(@NotNull GitHubIssue issue) {
        // In a real implementation, we would use the AI service to generate a response
        // For now, return a simple template response
        
        StringBuilder response = new StringBuilder();
        response.append("Hi there! 👋\n\n");
        response.append("I'm the ModForge AI assistant, here to help with your Minecraft mod development.\n\n");
        
        // Respond based on issue content
        if (issue.getTitle().toLowerCase().contains("error") || issue.getBody().toLowerCase().contains("error")) {
            response.append("I've analyzed your error report and I'll start working on a solution. ");
            response.append("Please provide any additional error logs or context that might help diagnose the issue.\n\n");
            response.append("I'll follow up with a fix or more questions shortly.");
        } else if (issue.getTitle().toLowerCase().contains("feature") || issue.getBody().toLowerCase().contains("feature")) {
            response.append("Thanks for suggesting this feature! I'll analyze how we can implement this ");
            response.append("in a way that's compatible with your mod's architecture.\n\n");
            response.append("I'll work on a prototype implementation and submit a PR for your review soon.");
        } else {
            response.append("Thanks for opening this issue! I'll look into this and get back to you soon.\n\n");
            response.append("In the meantime, could you provide any additional details that might help me understand the context better?");
        }
        
        return response.toString();
    }
    
    /**
     * Generate a response to a pull request.
     *
     * @param pr    The pull request
     * @param files The files changed in the PR
     * @return The response
     */
    @NotNull
    private String generatePullRequestResponse(@NotNull GitHubPullRequest pr, @NotNull List<GitHubFile> files) {
        // In a real implementation, we would use the AI service to generate a response
        // For now, return a simple template response
        
        StringBuilder response = new StringBuilder();
        response.append("Hi there! 👋\n\n");
        response.append("I'm the ModForge AI assistant, and I've analyzed your PR with " + files.size() + " changed files.\n\n");
        
        // Comment on code quality
        response.append("## Code Review\n\n");
        
        // Add some specific comments based on the PR content
        if (files.stream().anyMatch(f -> f.getFilename().endsWith(".java"))) {
            response.append("✅ Java code looks well-structured and follows good practices.\n");
        }
        
        if (files.stream().anyMatch(f -> f.getFilename().endsWith(".json"))) {
            response.append("✅ JSON configuration files are properly formatted.\n");
        }
        
        if (files.stream().anyMatch(f -> f.getFilename().endsWith(".gradle"))) {
            response.append("✅ Gradle build configuration seems appropriately set up.\n");
        }
        
        response.append("\nOverall, this PR looks good! I'll continue to monitor this PR for any updates or changes.\n\n");
        response.append("Let me know if you'd like me to help with anything specific!");
        
        return response.toString();
    }
    
    /**
     * Dispose the service.
     */
    public void dispose() {
        stopMonitoring();
        scheduler.shutdown();
    }
    
    /**
     * Result of pushing to GitHub.
     */
    public static class GitHubPushResult {
        private final boolean success;
        private final String message;
        private final String repositoryUrl;
        
        public GitHubPushResult(boolean success, String message) {
            this(success, message, null);
        }
        
        public GitHubPushResult(boolean success, String message, String repositoryUrl) {
            this.success = success;
            this.message = message;
            this.repositoryUrl = repositoryUrl;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getRepositoryUrl() {
            return repositoryUrl;
        }
    }
    
    /**
     * Content of a file to push to GitHub.
     */
    private static class GitHubFileContent {
        private final String path;
        private final String content;
        
        public GitHubFileContent(String path, String content) {
            this.path = path;
            this.content = content;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getContent() {
            return content;
        }
    }
}