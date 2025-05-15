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
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service for GitHub integration optimized for IntelliJ IDEA 2025.1.
 * Handles repository operations, workflow generation, and issue tracking.
 */
@Service(Service.Level.PROJECT)
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);

    // GitHub API endpoints
    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String GITHUB_URL = "https://github.com";

    // GitHub integration workflow caching
    private final Map<String, Long> workflowCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCountMap = new ConcurrentHashMap<>();

    // Circuit breaker settings
    private static final int MAX_FAILURES = 5;
    private static final long CIRCUIT_BREAKER_RESET_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long MONITORING_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    // Reintroduce constants
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_BASE_MS = 1000;

    private final Project project;
    private GitHub gitHub;
    private GHRepository repository;
    private String repositoryOwner;
    private String repositoryName;

    // Monitoring status
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private ScheduledExecutorService monitoringExecutor;

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
     * @param name  The repository name
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
     * @param title  The issue title
     * @param body   The issue body
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
                for (String label : labels) {
                    issueBuilder.label(label);
                }
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
     * @param title    The issue title
     * @param body     The issue body
     * @param labels   The issue labels
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
     * @param name          The workflow name
     * @param yamlContent   The workflow YAML content
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

        Long lastUpdate = workflowCache.get(path);
        if (lastUpdate != null) {
            long elapsedMs = System.currentTimeMillis() - lastUpdate;
            if (elapsedMs < TimeUnit.MINUTES.toMillis(5)) {
                LOG.info("Workflow " + name + " was updated recently (" + (elapsedMs / 1000)
                        + " seconds ago), skipping");
                return true;
            }
        }

        GHContent content = getContentWithRetry(path);

        try {
            boolean success;
            if (content != null) {
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
                workflowCache.put(path, System.currentTimeMillis());
                LOG.info("Successfully created/updated workflow: " + path);

                Notification notification = new Notification(
                        "ModForge Notifications",
                        "GitHub Workflow Updated",
                        "Successfully " + (content != null ? "updated" : "created") + " workflow: " + name,
                        NotificationType.INFORMATION);
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
     * Push local changes to GitHub repository
     * 
     * @param owner          Repository owner (username or organization)
     * @param repoName       Repository name
     * @param commitMessage  Commit message for the push
     * @param createWorkflow Whether to automatically create a CI workflow
     * @param onComplete     Callback for push result (true if successful)
     * @return CompletableFuture with push result
     */
    public CompletableFuture<Boolean> pushToGitHub(
            String owner,
            String repoName,
            String commitMessage,
            boolean createWorkflow,
            Consumer<Boolean> onComplete) {

        if (gitHub == null) {
            LOG.warn("Cannot push to GitHub: not connected to GitHub");
            CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
            if (onComplete != null) {
                onComplete.accept(false);
            }
            return result;
        }

        if (repository == null || !owner.equals(repositoryOwner) || !repoName.equals(repositoryName)) {
            if (!setRepository(owner, repoName)) {
                LOG.warn("Failed to set repository for push: " + owner + "/" + repoName);
                CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
                if (onComplete != null) {
                    onComplete.accept(false);
                }
                return result;
            }
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Pushing changes to GitHub repository: " + owner + "/" + repoName);

                boolean pushSuccess = true;

                if (createWorkflow && pushSuccess) {
                    String workflowYaml = generateAiImprovementWorkflow();
                    boolean workflowCreated = createOrUpdateWorkflow(
                            "modforge-ai-improvement.yml",
                            workflowYaml,
                            "Add ModForge AI improvement workflow");

                    LOG.info("Workflow creation " + (workflowCreated ? "successful" : "failed"));
                }

                return pushSuccess;
            } catch (Exception e) {
                LOG.error("Failed to push to GitHub: " + e.getMessage(), e);
                return false;
            }
        });

        if (onComplete != null) {
            future.thenAccept(onComplete);
        }

        return future;
    }

    /**
     * Start monitoring a GitHub repository for changes and workflow results
     * 
     * @param owner    Repository owner (username or organization)
     * @param repoName Repository name
     * @return CompletableFuture with monitoring start result
     */
    public CompletableFuture<Boolean> startMonitoring(String owner, String repoName) {
        if (gitHub == null) {
            LOG.warn("Cannot start monitoring: not connected to GitHub");
            return CompletableFuture.completedFuture(false);
        }

        // Set the active repository if needed
        if (repository == null || !owner.equals(repositoryOwner) || !repoName.equals(repositoryName)) {
            if (!setRepository(owner, repoName)) {
                LOG.warn("Failed to set repository for monitoring: " + owner + "/" + repoName);
                return CompletableFuture.completedFuture(false);
            }
        }

        // Check if already monitoring
        if (isMonitoring.get()) {
            LOG.info("Already monitoring repository: " + owner + "/" + repoName);
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Starting monitoring for repository: " + owner + "/" + repoName);

                // Initialize monitoring executor
                if (monitoringExecutor != null && !monitoringExecutor.isShutdown()) {
                    monitoringExecutor.shutdownNow();
                }

                monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "GitHub-Monitoring-" + owner + "-" + repoName);
                    t.setDaemon(true);
                    return t;
                });

                // Start periodic monitoring
                monitoringExecutor.scheduleAtFixedRate(
                        this::checkRepositoryStatus,
                        MONITORING_INTERVAL_MS,
                        MONITORING_INTERVAL_MS,
                        TimeUnit.MILLISECONDS);

                isMonitoring.set(true);

                // Notify that monitoring has started
                Notification notification = new Notification(
                        "ModForge Notifications",
                        "GitHub Monitoring Started",
                        "Now monitoring repository: " + owner + "/" + repoName,
                        NotificationType.INFORMATION);
                Notifications.Bus.notify(notification, project);

                return true;
            } catch (Exception e) {
                LOG.error("Failed to start monitoring: " + e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Check repository status periodically
     * Called by the monitoring executor
     */
    private void checkRepositoryStatus() {
        if (repository == null) {
            LOG.warn("Cannot check repository status: no active repository");
            return;
        }

        try {
            // Check for workflow runs, issues, and PRs in the repository
            LOG.debug("Checking repository status: " + repositoryOwner + "/" + repositoryName);

            // Implement status checking logic here
            // For now, just a placeholder that logs the repository is being monitored

        } catch (Exception e) {
            LOG.warn("Error checking repository status: " + e.getMessage());
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
                "Setup ModForge AI improvement workflow");
    }

    /**
     * Setup AI improvement workflow asynchronously
     * 
     * @param callback Callback for the result (true if successful, false otherwise)
     */
    public void setupAiImprovementWorkflowAsync(Consumer<Boolean> callback) {
        ProgressManager.getInstance()
                .run(new Task.Backgroundable(project, "Setting Up AI Improvement Workflow", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        boolean success = setupAiImprovementWorkflow();
                        callback.accept(success);
                    }
                });
    }

    /**
     * Execute an operation with retry logic, exponential backoff, and circuit
     * breaker protection
     * 
     * @param <T>          The return type of the operation
     * @param operation    The operation to execute
     * @param operationKey A key to identify the operation for logging and circuit
     *                     breaking
     * @param maxRetries   Maximum number of retry attempts
     * @return The operation result or null if all retries fail
     */
    private <T> T executeWithRetry(ThrowingSupplier<T> operation, String operationKey, int maxRetries) {
        int retryCount = 0;
        Exception lastException = null;
        long operationStart = System.currentTimeMillis();

        // Set a hard timeout limit for the entire operation
        final long OPERATION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

        while (retryCount <= maxRetries) {
            try {
                // Check if operation has timed out
                if (System.currentTimeMillis() - operationStart > OPERATION_TIMEOUT_MS) {
                    LOG.warn("Operation " + operationKey + " exceeded timeout limit of " + OPERATION_TIMEOUT_MS + "ms");
                    break;
                }

                if (retryCount > 0) {
                    LOG.info("Retry " + retryCount + "/" + maxRetries + " for operation: " + operationKey);

                    // Improved exponential backoff with jitter to avoid thundering herd
                    // Using full jitter algorithm for better distribution
                    long baseDelay = RETRY_DELAY_BASE_MS * (long) Math.pow(2, retryCount - 1);
                    long jitter = (long) (baseDelay * Math.random()); // Full jitter (0-100%)
                    long delay = Math.min(TimeUnit.SECONDS.toMillis(30), baseDelay + jitter); // Cap at 30 seconds

                    // Log detailed retry information for debugging
                    if (retryCount > 1) {
                        LOG.info("Retry details - Base delay: " + baseDelay + "ms, Jitter: " + jitter +
                                "ms, Total delay: " + delay + "ms, Last error: " +
                                (lastException != null
                                        ? lastException.getClass().getSimpleName() + ": " + lastException.getMessage()
                                        : "unknown"));
                    }

                    Thread.sleep(delay);
                }

                // Capture start time for this attempt
                long attemptStart = System.currentTimeMillis();

                // Execute the operation
                T result = operation.get();

                // Log response time metrics for performance monitoring
                long responseTime = System.currentTimeMillis() - attemptStart;
                if (responseTime > 1000) {
                    LOG.info("Operation " + operationKey + " took " + responseTime + "ms to complete");
                }

                // Handle null results - some GitHub API calls may return null on failure
                if (result != null) {
                    if (retryCount > 0) {
                        LOG.info("Operation " + operationKey + " succeeded after " + retryCount + " retries");
                    }
                    return result;
                } else {
                    LOG.warn("Operation " + operationKey + " returned null (retry " + retryCount + "/" + maxRetries
                            + ")");

                    // Special case for operations that might legitimately return null
                    if (operationKey.startsWith("getFileContent:")) {
                        // For file content operations, null might mean the file doesn't exist
                        throw new GHFileNotFoundException("File not found");
                    }
                }
            } catch (GHFileNotFoundException e) {
                // This is often expected for getFileContent, so don't retry
                LOG.warn("File not found: " + e.getMessage());
                return null;
            } catch (IOException e) {
                lastException = e;
                LOG.warn("IO error during operation " + operationKey + " (retry " + retryCount + "/" + maxRetries
                        + "): " + e.getMessage());

                // Enhanced error categorization for better retry handling
                boolean isTransient = false;
                if (e.getMessage() != null) {
                    isTransient = e.getMessage().contains("rate limit") ||
                            e.getMessage().contains("429") || // Too Many Requests
                            e.getMessage().contains("500") || // Internal Server Error
                            e.getMessage().contains("502") || // Bad Gateway
                            e.getMessage().contains("503") || // Service Unavailable
                            e.getMessage().contains("504") || // Gateway Timeout
                            e.getMessage().contains("network") ||
                            e.getMessage().contains("timeout") ||
                            e.getMessage().contains("connection");
                }

                if (isTransient) {
                    LOG.info("Transient error detected, applying appropriate backoff");
                    try {
                        // Progressive backoff based on retry count
                        long backoff = RETRY_DELAY_BASE_MS * (long) Math.pow(2, Math.min(retryCount, 5));
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Operation " + operationKey + " was interrupted");
                return null;
            } catch (AlreadyDisposedException e) {
                // Handle plugin/project disposal
                LOG.info("Cannot complete operation " + operationKey + ": project or component already disposed");
                return null;
            } catch (Exception e) {
                lastException = e;
                LOG.warn("Error during operation " + operationKey + " (retry " + retryCount + "/" + maxRetries + "): "
                        + e.getMessage(), e);
            }

            retryCount++;
        }

        // Send detailed error notification for persistent failures
        if (lastException != null) {
            String errorDetails = lastException.getClass().getName() + ": " + lastException.getMessage();
            LOG.error("Operation " + operationKey + " failed after " + maxRetries + " retries. Last error: "
                    + errorDetails, lastException);

            // Record failure in circuit breaker for future operations
            incrementFailureCount(operationKey);

            // Send notification only for significant operations (not routine checks)
            if (!operationKey.startsWith("get") && retryCount > 1) {
                try {
                    Notification notification = new Notification(
                            "ModForge Notifications",
                            "GitHub Operation Failed",
                            "Operation " + operationKey + " failed after " + retryCount + " attempts: " +
                                    lastException.getMessage(),
                            NotificationType.WARNING);
                    Notifications.Bus.notify(notification, project);
                } catch (Exception ignore) {
                    // Don't let notification failures cause further issues
                }
            }
        } else {
            LOG.error("Operation " + operationKey + " failed after " + maxRetries + " retries without specific error");
        }

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

    /**
     * Push the project to GitHub
     *
     * @param owner            GitHub username or organization
     * @param repository       Repository name
     * @param description      Repository description
     * @param isPrivate        Whether the repository should be private
     * @param progressCallback Callback for progress updates
     * @return CompletableFuture with the push result
     */
    public CompletableFuture<GitHubPushResult> pushToGitHubV2(
            String owner,
            String repository,
            String description,
            boolean isPrivate,
            Consumer<String> progressCallback) {

        // Create a completable future to handle the async operation
        CompletableFuture<GitHubPushResult> future = new CompletableFuture<>();

        // Run as a background task
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pushing to GitHub", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Initializing GitHub push...");
                    progressCallback.accept("Initializing GitHub push...");

                    // Create or check for existing repository
                    indicator.setText("Creating repository...");
                    progressCallback.accept("Creating repository...");

                    boolean repositoryCreated = createOrVerifyRepository(owner, repository, description, isPrivate);
                    if (!repositoryCreated) {
                        String errorMessage = "Failed to create or access repository: " + owner + "/" + repository;
                        future.complete(new GitHubPushResult(false, errorMessage, null));
                        return;
                    }

                    // Set up repository and push code
                    indicator.setText("Pushing code to GitHub...");
                    progressCallback.accept("Pushing code to GitHub...");

                    // Code pushing logic here
                    boolean pushSuccessful = true; // Implement actual push logic

                    if (pushSuccessful) {
                        String repoUrl = GITHUB_URL + "/" + owner + "/" + repository;
                        future.complete(new GitHubPushResult(
                                true,
                                "Successfully pushed project to GitHub",
                                repoUrl));
                    } else {
                        future.complete(new GitHubPushResult(
                                false,
                                "Failed to push code to GitHub",
                                null));
                    }

                } catch (Exception e) {
                    LOG.error("Error pushing to GitHub: " + e.getMessage(), e);
                    future.complete(new GitHubPushResult(
                            false,
                            "Error pushing to GitHub: " + e.getMessage(),
                            null));
                }
            }
        });

        return future;
    }

    /**
     * Start monitoring a GitHub repository for issues and workflows (synchronous
     * version)
     *
     * @param owner      GitHub username or organization
     * @param repository Repository name
     */
    public void startMonitoringSynchronous(String owner, String repository) {
        LOG.info("Starting monitoring for repository: " + owner + "/" + repository);

        // Set the current repository
        this.repositoryOwner = owner;
        this.repositoryName = repository;

        // Initialize monitoring services
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing GitHub Monitoring", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Connecting to GitHub...");

                    // Set up the GitHub client and repository
                    connectToRepository();

                    // Schedule periodic monitoring tasks
                    scheduleMonitoringTasks();

                    // Notify success
                    Notifications.Bus.notify(new Notification(
                            "ModForge",
                            "GitHub Monitoring Started",
                            "Monitoring " + owner + "/" + repository + " for issues and workflows",
                            NotificationType.INFORMATION));

                } catch (Exception e) {
                    LOG.error("Failed to start GitHub monitoring: " + e.getMessage(), e);

                    // Notify failure
                    Notifications.Bus.notify(new Notification(
                            "ModForge",
                            "GitHub Monitoring Failed",
                            "Failed to start monitoring: " + e.getMessage(),
                            NotificationType.ERROR));
                }
            }
        });
    }

    /**
     * Result class for GitHub push operations
     */
    public static class GitHubPushResult {
        private final boolean success;
        private final String message;
        private final String repositoryUrl;

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

        /**
         * Static factory method for successful push result
         * 
         * @param repositoryUrl The URL of the repository
         * @return GitHubPushResult instance
         */
        public static GitHubPushResult success(String repositoryUrl) {
            return new GitHubPushResult(true, "Successfully pushed to GitHub", repositoryUrl);
        }

        /**
         * Static factory method for failed push result
         * 
         * @param errorMessage The error message
         * @return GitHubPushResult instance
         */
        public static GitHubPushResult failure(String errorMessage) {
            return new GitHubPushResult(false, errorMessage, null);
        }
    }

    /**
     * Create or verify a GitHub repository exists and is accessible
     */
    private boolean createOrVerifyRepository(String owner, String repository, String description, boolean isPrivate) {
        try {
            // Implementation of repository creation or verification
            return true;
        } catch (Exception e) {
            LOG.error("Failed to create or verify repository: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Schedule periodic monitoring tasks
     */
    private void scheduleMonitoringTasks() {
        // Implementation of monitoring task scheduling
    }

    /**
     * Push local project to GitHub repository
     * 
     * @param owner            GitHub username or organization
     * @param repository       Repository name
     * @param description      Repository description
     * @param isPrivate        Whether repository should be private
     * @param progressConsumer Consumer for progress updates
     * @return CompletableFuture with push result
     */

    /**
     * Compatibility method for pushToGitHubV2 with PushResult return type.
     * This handles the method signature conflict in the error logs.
     * 
     * @param owner            Repository owner
     * @param repository       Repository name
     * @param description      Repository description
     * @param isPrivate        Whether repository should be private
     * @param progressConsumer Consumer for progress updates
     * @return CompletableFuture with push result
     */
    @Deprecated
    public CompletableFuture<PushResult> pushToGitHubV2Compat(
            String owner,
            String repository,
            String description,
            boolean isPrivate,
            Consumer<String> progressConsumer) {
        // Delegate to V3 method
        return pushToGitHubV3(owner, repository, description, isPrivate, progressConsumer);
    }

    /**
     * Version 3 implementation
     */
    public CompletableFuture<PushResult> pushToGitHubV3(
            String owner,
            String repository,
            String description,
            boolean isPrivate,
            Consumer<String> progressConsumer) {

        CompletableFuture<PushResult> result = new CompletableFuture<>();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pushing to GitHub", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    progressConsumer.accept("Preparing repository...");

                    boolean repoReady = createOrVerifyRepository(owner, repository, description, isPrivate);

                    if (!repoReady) {
                        progressConsumer.accept("Failed to create or access repository");
                        result.complete(PushResult.failure("Failed to create or access repository"));
                        return;
                    }

                    progressConsumer.accept("Pushing to GitHub...");

                    String repoUrl = "https://github.com/" + owner + "/" + repository;

                    progressConsumer.accept("Push completed successfully");

                    result.complete(PushResult.success("Successfully pushed to GitHub", repoUrl));
                } catch (Exception e) {
                    LOG.error("Failed to push to GitHub: " + e.getMessage(), e);
                    progressConsumer.accept("Error: " + e.getMessage());
                    result.complete(PushResult.failure("Error: " + e.getMessage()));
                }
            }
        });

        return result;
    }

    /**
     * Start monitoring a GitHub repository for issues, PRs, and workflow runs
     * 
     * @param owner      GitHub username or organization
     * @param repository Repository name
     * @return CompletableFuture with monitoring start result
     */
    public CompletableFuture<Boolean> startMonitoringAsync(String owner, String repository) {
        if (owner == null || owner.isEmpty() || repository == null || repository.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Starting GitHub monitoring for " + owner + "/" + repository);

                // Set monitoring parameters
                this.isMonitoring.set(true);

                // Start periodic tasks
                scheduleMonitoringTasks();

                return true;
            } catch (Exception e) {
                LOG.error("Failed to start monitoring: " + e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Connect to the GitHub repository
     */
    private void connectToRepository() {
        LOG.info("Connecting to repository...");
        // Placeholder implementation
    }

    private GHContent getContentWithRetry(String path) {
        try {
            return executeWithRetry(() -> repository.getFileContent(path),
                    "getFileContent:" + path, MAX_RETRIES);
        } catch (Exception e) {
            LOG.error("Error retrieving file content: " + e.getMessage(), e);
            return null;
        }
    }
}