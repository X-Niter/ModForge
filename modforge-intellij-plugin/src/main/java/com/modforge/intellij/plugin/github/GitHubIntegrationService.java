package com.modforge.intellij.plugin.github;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.vcsUtil.VcsUtil;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service for GitHub integration optimized for IntelliJ IDEA 2025.1.
 * Handles repository operations, workflow generation, and issue tracking.
 */
@Service(Service.Level.PROJECT)
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    
    // Connection settings
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_BASE_MS = 1000;
    
    // GitHub API endpoints
    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String GITHUB_URL = "https://github.com";
    
    // GitHub integration workflow caching
    private final Map<String, Long> workflowCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCountMap = new ConcurrentHashMap<>();
    
    // Circuit breaker settings
    private static final int MAX_FAILURES = 5;
    private static final long CIRCUIT_BREAKER_RESET_MS = TimeUnit.MINUTES.toMillis(30);
    
    private final Project project;
    private GitHub gitHub;
    private GHRepository repository;
    private String repositoryOwner;
    private String repositoryName;

    public GitHubIntegrationService(Project project) {
        this.project = project;
        
        // Initialize circuit breaker maintenance
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GitHub-CircuitBreaker-Maintenance");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::resetFailedOperations, 
            CIRCUIT_BREAKER_RESET_MS, CIRCUIT_BREAKER_RESET_MS, TimeUnit.MILLISECONDS);
            
        LOG.info("GitHub Integration Service initialized with circuit breaker protection");
    }
    
    /**
     * Periodically reset circuit breakers to prevent permanent lockout
     */
    private void resetFailedOperations() {
        if (failureCountMap.isEmpty()) {
            return;
        }
        
        int resetCount = 0;
        for (Map.Entry<String, AtomicInteger> entry : failureCountMap.entrySet()) {
            if (entry.getValue().get() >= MAX_FAILURES) {
                entry.getValue().set(0);
                resetCount++;
                LOG.info("Reset circuit breaker for operation: " + entry.getKey());
            }
        }
        
        if (resetCount > 0) {
            LOG.info("Reset " + resetCount + " circuit breakers during maintenance");
        }
    }
    
    /**
     * Connect to GitHub with a token
     * 
     * @param token The GitHub access token
     * @return true if connection is successful, false otherwise
     */
    public boolean connectWithToken(String token) {
        if (token == null || token.isEmpty()) {
            LOG.warn("Cannot connect to GitHub: token is empty");
            return false;
        }
        
        try {
            // Create GitHub instance with retry capabilities
            gitHub = GitHub.connectUsingOAuth(token);
            
            // Test connection
            if (!testConnection()) {
                LOG.warn("GitHub connection test failed");
                return false;
            }
            
            LOG.info("Successfully connected to GitHub using token authentication");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to connect to GitHub: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Test the GitHub connection
     * 
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        if (gitHub == null) {
            return false;
        }
        
        try {
            // Try a simple operation with the GitHub API
            return ConnectionTestUtil.testConnectionWithRetry(GITHUB_API_URL + "/rate_limit");
        } catch (Exception e) {
            LOG.warn("GitHub connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Set the active repository for GitHub operations
     * 
     * @param owner The repository owner (username or organization)
     * @param name The repository name
     * @return true if repository is found and accessible, false otherwise
     */
    public boolean setRepository(String owner, String name) {
        if (gitHub == null) {
            LOG.warn("Cannot set repository: not connected to GitHub");
            return false;
        }
        
        if (owner == null || owner.isEmpty() || name == null || name.isEmpty()) {
            LOG.warn("Cannot set repository: owner or name is empty");
            return false;
        }
        
        try {
            this.repositoryOwner = owner;
            this.repositoryName = name;
            
            // Try to get the repository with retry logic
            repository = executeWithRetry(() -> gitHub.getRepository(owner + "/" + name),
                    "getRepository", 3);
            
            if (repository == null) {
                LOG.warn("Repository not found: " + owner + "/" + name);
                return false;
            }
            
            LOG.info("Successfully set active repository: " + owner + "/" + name);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to set repository: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the URL of the active repository
     * 
     * @return The repository URL or null if no repository is set
     */
    public String getRepositoryUrl() {
        if (repositoryOwner == null || repositoryName == null) {
            return null;
        }
        
        return GITHUB_URL + "/" + repositoryOwner + "/" + repositoryName;
    }
    
    /**
     * Create a GitHub issue in the active repository
     * 
     * @param title The issue title
     * @param body The issue body
     * @param labels The issue labels
     * @return The created issue URL or null if creation fails
     */
    @RequiresBackgroundThread
    public String createIssue(String title, String body, List<String> labels) {
        if (repository == null) {
            LOG.warn("Cannot create issue: no active repository");
            return null;
        }
        
        String operationKey = "createIssue";
        if (isCircuitBreakerOpen(operationKey)) {
            LOG.warn("Circuit breaker open for operation: " + operationKey);
            return null;
        }
        
        try {
            GHIssueBuilder issueBuilder = repository.createIssue(title)
                    .body(body);
                
            if (labels != null && !labels.isEmpty()) {
                issueBuilder.label(labels.toArray(new String[0]));
            }
            
            GHIssue issue = executeWithRetry(issueBuilder::create, operationKey, MAX_RETRIES);
            
            if (issue != null) {
                resetFailureCount(operationKey);
                LOG.info("Successfully created issue: " + issue.getHtmlUrl());
                return issue.getHtmlUrl().toString();
            } else {
                incrementFailureCount(operationKey);
                LOG.warn("Failed to create issue (null response)");
                return null;
            }
        } catch (Exception e) {
            incrementFailureCount(operationKey);
            LOG.error("Failed to create issue: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create a GitHub issue asynchronously in the active repository
     * 
     * @param title The issue title
     * @param body The issue body
     * @param labels The issue labels
     * @param callback Callback for the issue URL (null if creation fails)
     */
    public void createIssueAsync(String title, String body, List<String> labels, Consumer<String> callback) {
        if (repository == null) {
            LOG.warn("Cannot create issue: no active repository");
            callback.accept(null);
            return;
        }
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating GitHub Issue", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String issueUrl = createIssue(title, body, labels);
                if (issueUrl != null) {
                    callback.accept(issueUrl);
                } else {
                    callback.accept(null);
                }
            }
        });
    }
    
    /**
     * Create or update a GitHub workflow file in the active repository
     * 
     * @param name The workflow name
     * @param yamlContent The workflow YAML content
     * @param commitMessage The commit message
     * @return true if workflow is created or updated successfully, false otherwise
     */
    @RequiresBackgroundThread
    public boolean createOrUpdateWorkflow(String name, String yamlContent, String commitMessage) {
        if (repository == null) {
            LOG.warn("Cannot create workflow: no active repository");
            return false;
        }
        
        if (!name.endsWith(".yml")) {
            name = name + ".yml";
        }
        
        String path = ".github/workflows/" + name;
        String operationKey = "updateWorkflow:" + name;
        
        if (isCircuitBreakerOpen(operationKey)) {
            LOG.warn("Circuit breaker open for operation: " + operationKey);
            return false;
        }
        
        // Check cache to avoid too frequent updates
        Long lastUpdate = workflowCache.get(path);
        if (lastUpdate != null) {
            long elapsedMs = System.currentTimeMillis() - lastUpdate;
            if (elapsedMs < TimeUnit.MINUTES.toMillis(5)) {
                LOG.info("Workflow " + name + " was updated recently (" + (elapsedMs / 1000) + " seconds ago), skipping");
                return true;
            }
        }
        
        try {
            // Check if file exists
            GHContent content = null;
            try {
                content = executeWithRetry(() -> repository.getFileContent(path), 
                    "getFileContent:" + path, MAX_RETRIES);
            } catch (GHFileNotFoundException e) {
                // File doesn't exist, will create new
                LOG.info("Workflow file doesn't exist, will create: " + path);
            }
            
            boolean success;
            if (content != null) {
                // Update existing file
                success = executeWithRetry(() -> {
                    repository.createContent()
                        .content(yamlContent)
                        .path(path)
                        .message(commitMessage)
                        .sha(content.getSha())
                        .commit();
                    return true;
                }, operationKey, MAX_RETRIES);
            } else {
                // Create new file
                success = executeWithRetry(() -> {
                    repository.createContent()
                        .content(yamlContent)
                        .path(path)
                        .message(commitMessage)
                        .commit();
                    return true;
                }, operationKey, MAX_RETRIES);
            }
            
            if (success) {
                resetFailureCount(operationKey);
                // Update cache
                workflowCache.put(path, System.currentTimeMillis());
                LOG.info("Successfully created/updated workflow: " + path);
                
                // Show notification
                Notification notification = new Notification(
                    "ModForge Notifications",
                    "GitHub Workflow Updated",
                    "Successfully " + (content != null ? "updated" : "created") + " workflow: " + name,
                    NotificationType.INFORMATION
                );
                Notifications.Bus.notify(notification, project);
                
                return true;
            } else {
                incrementFailureCount(operationKey);
                LOG.warn("Failed to create/update workflow (operation returned false)");
                return false;
            }
        } catch (Exception e) {
            incrementFailureCount(operationKey);
            LOG.error("Failed to create/update workflow: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate a starter workflow for continuous AI improvement
     * 
     * @return The workflow YAML content
     */
    public String generateAiImprovementWorkflow() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# This file was auto-generated by ModForge\n");
        yaml.append("# It provides continuous AI assistance for this Minecraft mod\n\n");
        
        yaml.append("name: ModForge AI Improvement\n\n");
        
        yaml.append("on:\n");
        yaml.append("  push:\n");
        yaml.append("    branches: [ main, master, development ]\n");
        yaml.append("  pull_request:\n");
        yaml.append("    branches: [ main, master ]\n");
        yaml.append("  schedule:\n");
        yaml.append("    - cron: '0 0 * * *' # Run daily at midnight\n");
        yaml.append("  workflow_dispatch: # Allow manual triggering\n\n");
        
        yaml.append("jobs:\n");
        yaml.append("  analyze:\n");
        yaml.append("    name: Analyze and Improve\n");
        yaml.append("    runs-on: ubuntu-latest\n");
        yaml.append("    permissions:\n");
        yaml.append("      contents: write\n");
        yaml.append("      pull-requests: write\n");
        yaml.append("      issues: write\n\n");
        yaml.append("    steps:\n");
        yaml.append("    - uses: actions/checkout@v4\n");
        yaml.append("      with:\n");
        yaml.append("        fetch-depth: 0\n\n");
        yaml.append("    - name: Set up JDK 21\n");
        yaml.append("      uses: actions/setup-java@v4\n");
        yaml.append("      with:\n");
        yaml.append("        java-version: '21'\n");
        yaml.append("        distribution: 'temurin'\n\n");
            
        // Add ModForge AI analysis
        yaml.append("    - name: ModForge AI Analysis\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Starting automated code analysis\"\n");
        yaml.append("        # This will be enhanced with ModForge server integration\n");
        yaml.append("        ./gradlew build --scan || true\n\n");
        
        // Add automated issue creation
        yaml.append("    - name: Analyze Build Results\n");
        yaml.append("      run: |\n");
        yaml.append("        if [ -f build/reports/errors.json ]; then\n");
        yaml.append("          echo \"Found compilation errors, creating improvement plan\"\n");
        yaml.append("        fi\n\n");
        
        // Add comment on PRs
        yaml.append("    - name: Comment on PR\n");
        yaml.append("      if: github.event_name == 'pull_request'\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Posting analysis results to PR\"\n");
        yaml.append("        # This will post findings back to the PR\n\n");
        
        return yaml.toString();
    }
    
    /**
     * Create or update the AI improvement workflow in the active repository
     * 
     * @return true if workflow is created or updated successfully, false otherwise
     */
    @RequiresBackgroundThread
    public boolean setupAiImprovementWorkflow() {
        String workflowContent = generateAiImprovementWorkflow();
        return createOrUpdateWorkflow(
            "modforge-ai-improvement.yml",
            workflowContent,
            "Setup ModForge AI improvement workflow"
        );
    }
    
    /**
     * Setup AI improvement workflow asynchronously
     * 
     * @param callback Callback for the result (true if successful, false otherwise)
     */
    public void setupAiImprovementWorkflowAsync(Consumer<Boolean> callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Setting Up AI Improvement Workflow", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                boolean success = setupAiImprovementWorkflow();
                callback.accept(success);
            }
        });
    }
    
    /**
     * Execute an operation with retry logic
     * 
     * @param <T> The return type
     * @param operation The operation to execute
     * @param operationKey The operation key for failure tracking
     * @param maxRetries The maximum number of retries
     * @return The operation result or null if all retries fail
     */
    private <T> T executeWithRetry(ThrowingSupplier<T> operation, String operationKey, int maxRetries) {
        int retryCount = 0;
        
        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    LOG.info("Retry " + retryCount + "/" + maxRetries + " for operation: " + operationKey);
                    
                    // Exponential backoff
                    long delay = RETRY_DELAY_BASE_MS * (long) Math.pow(2, retryCount - 1);
                    Thread.sleep(delay);
                }
                
                T result = operation.get();
                if (result != null) {
                    if (retryCount > 0) {
                        LOG.info("Operation " + operationKey + " succeeded after " + retryCount + " retries");
                    }
                    return result;
                }
            } catch (IOException e) {
                LOG.warn("IO error during operation " + operationKey + " (retry " + retryCount + "/" + maxRetries + "): " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Operation " + operationKey + " was interrupted");
                return null;
            } catch (Exception e) {
                LOG.warn("Error during operation " + operationKey + " (retry " + retryCount + "/" + maxRetries + "): " + e.getMessage());
            }
            
            retryCount++;
        }
        
        LOG.error("Operation " + operationKey + " failed after " + maxRetries + " retries");
        return null;
    }
    
    /**
     * Check if circuit breaker is open for an operation
     * 
     * @param operationKey The operation key
     * @return true if circuit breaker is open, false otherwise
     */
    private boolean isCircuitBreakerOpen(String operationKey) {
        AtomicInteger failureCount = failureCountMap.get(operationKey);
        return failureCount != null && failureCount.get() >= MAX_FAILURES;
    }
    
    /**
     * Increment failure count for an operation
     * 
     * @param operationKey The operation key
     */
    private void incrementFailureCount(String operationKey) {
        failureCountMap.computeIfAbsent(operationKey, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Reset failure count for an operation
     * 
     * @param operationKey The operation key
     */
    private void resetFailureCount(String operationKey) {
        AtomicInteger counter = failureCountMap.get(operationKey);
        if (counter != null) {
            counter.set(0);
        }
    }
    
    /**
     * Supplier interface that can throw exceptions
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}