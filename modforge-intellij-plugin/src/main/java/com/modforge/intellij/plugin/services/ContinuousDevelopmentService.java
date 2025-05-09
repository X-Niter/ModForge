package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener.CompilationIssue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for continuous development.
 * This service continuously checks for and fixes errors in a project.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    private static final long DEFAULT_CHECK_INTERVAL_MS = 60_000L; // 1 minute
    
    private final Project project;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger fixedErrorCount = new AtomicInteger(0);
    private final AtomicInteger addedFeatureCount = new AtomicInteger(0);
    
    private ScheduledFuture<?> scheduledFuture;
    private long checkIntervalMs = DEFAULT_CHECK_INTERVAL_MS;
    
    /**
     * Creates a new ContinuousDevelopmentService.
     * @param project The project
     */
    public ContinuousDevelopmentService(@NotNull Project project) {
        this.project = project;
        this.scheduler = AppExecutorUtil.createBoundedScheduledExecutorService(
                "ModForge-ContinuousDevelopment",
                1
        );
    }
    
    /**
     * Gets the instance of the service for a project.
     * @param project The project
     * @return The continuous development service
     */
    public static ContinuousDevelopmentService getInstance(@NotNull Project project) {
        return project.getService(ContinuousDevelopmentService.class);
    }
    
    /**
     * Starts the continuous development service.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Stop any existing schedule
            stop(false);
            
            // Schedule periodic checks
            scheduledFuture = scheduler.scheduleAtFixedRate(
                    this::check,
                    checkIntervalMs,
                    checkIntervalMs,
                    TimeUnit.MILLISECONDS
            );
            
            LOG.info("Continuous development service started with interval " + checkIntervalMs + "ms");
        }
    }
    
    /**
     * Stops the continuous development service.
     */
    public void stop() {
        stop(true);
    }
    
    /**
     * Stops the continuous development service.
     * @param updateRunningFlag Whether to update the running flag
     */
    private void stop(boolean updateRunningFlag) {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        
        if (updateRunningFlag) {
            running.set(false);
            LOG.info("Continuous development service stopped");
        }
    }
    
    /**
     * Checks if the continuous development service is running.
     * @return {@code true} if the service is running, {@code false} otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Gets the check interval.
     * @return The check interval in milliseconds
     */
    public long getCheckInterval() {
        return checkIntervalMs;
    }
    
    /**
     * Sets the check interval.
     * @param intervalMs The check interval in milliseconds
     */
    public void setCheckInterval(long intervalMs) {
        if (intervalMs < 1000) {
            intervalMs = 1000; // Minimum 1 second
        }
        
        checkIntervalMs = intervalMs;
        
        // Restart if running
        if (running.get()) {
            start();
        }
        
        LOG.info("Continuous development check interval set to " + intervalMs + "ms");
    }
    
    /**
     * Gets statistics for the continuous development service.
     * @return The statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("running", running.get());
        statistics.put("checkIntervalMs", checkIntervalMs);
        statistics.put("fixedErrorCount", fixedErrorCount.get());
        statistics.put("addedFeatureCount", addedFeatureCount.get());
        
        return statistics;
    }
    
    /**
     * Resets the statistics.
     */
    public void resetStatistics() {
        fixedErrorCount.set(0);
        addedFeatureCount.set(0);
    }
    
    /**
     * Checks for errors and fixes them.
     */
    private void check() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        // Check compilation issues
        ModForgeCompilationListener compilationListener = project.getService(ModForgeCompilationListener.class);
        
        if (compilationListener == null) {
            LOG.warn("Compilation listener not available");
            return;
        }
        
        List<CompilationIssue> activeIssues = compilationListener.getActiveIssues();
        
        if (!activeIssues.isEmpty()) {
            // Issue found, fix them
            fixIssues(activeIssues);
        } else {
            // No issues, try to compile the project
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    CompilerManager.getInstance(project).make(null);
                }
            });
        }
    }
    
    /**
     * Fixes compilation issues.
     * @param issues The compilation issues
     */
    private void fixIssues(@NotNull List<CompilationIssue> issues) {
        if (issues.isEmpty() || !running.get() || project.isDisposed()) {
            return;
        }
        
        // Group issues by file
        Map<String, List<CompilationIssue>> issuesByFile = new HashMap<>();
        
        for (CompilationIssue issue : issues) {
            issuesByFile.computeIfAbsent(issue.getFile(), k -> new java.util.ArrayList<>()).add(issue);
        }
        
        // Process each file
        for (Map.Entry<String, List<CompilationIssue>> entry : issuesByFile.entrySet()) {
            String file = entry.getKey();
            List<CompilationIssue> fileIssues = entry.getValue();
            
            // Skip empty issues
            if (fileIssues.isEmpty()) {
                continue;
            }
            
            // Get the actual file content
            String fileContent = getFileContent(file);
            
            if (fileContent == null) {
                LOG.warn("Failed to get content for file: " + file);
                continue;
            }
            
            // Fix the issues
            fixFileIssues(file, fileContent, fileIssues);
        }
    }
    
    /**
     * Gets the content of a file.
     * @param filePath The file path
     * @return The file content or {@code null} if the file is not found
     */
    @Nullable
    private String getFileContent(@NotNull String filePath) {
        // Ensure path is absolute
        if (!filePath.startsWith("/")) {
            String basePath = project.getBasePath();
            
            if (basePath != null) {
                filePath = basePath + "/" + filePath;
            }
        }
        
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
        } catch (Exception e) {
            LOG.warn("Failed to read file: " + filePath, e);
            return null;
        }
    }
    
    /**
     * Fixes issues in a file.
     * @param file The file path
     * @param fileContent The file content
     * @param issues The issues
     */
    private void fixFileIssues(@NotNull String file, @NotNull String fileContent, @NotNull List<CompilationIssue> issues) {
        // Run as a background task
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Issues in " + file, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!running.get() || project.isDisposed()) {
                    return;
                }
                
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);
                
                // Build error message
                StringBuilder errorMessage = new StringBuilder();
                
                for (CompilationIssue issue : issues) {
                    errorMessage.append(issue.getMessage())
                            .append(" (Line ")
                            .append(issue.getLine())
                            .append(")\n");
                }
                
                // Get code generation service
                AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
                
                try {
                    // Fix code
                    indicator.setText("Analyzing issues...");
                    indicator.setFraction(0.2);
                    
                    String fixedCode = service.fixCode(fileContent, errorMessage.toString(), null).get();
                    
                    indicator.setText("Applying fixes...");
                    indicator.setFraction(0.7);
                    
                    if (fixedCode != null && !fixedCode.isEmpty() && !fixedCode.equals(fileContent)) {
                        // Write fixed code to file
                        if (writeFileContent(file, fixedCode)) {
                            LOG.info("Fixed issues in file: " + file);
                            fixedErrorCount.incrementAndGet();
                        }
                    }
                    
                    indicator.setFraction(1.0);
                } catch (Exception e) {
                    LOG.error("Error fixing issues in file: " + file, e);
                }
            }
        });
    }
    
    /**
     * Writes content to a file.
     * @param filePath The file path
     * @param content The content
     * @return {@code true} if successful, {@code false} otherwise
     */
    private boolean writeFileContent(@NotNull String filePath, @NotNull String content) {
        // Ensure path is absolute
        if (!filePath.startsWith("/")) {
            String basePath = project.getBasePath();
            
            if (basePath != null) {
                filePath = basePath + "/" + filePath;
            }
        }
        
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), content.getBytes());
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to write file: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Disposes the service.
     */
    public void dispose() {
        stop();
        scheduler.shutdown();
    }
}