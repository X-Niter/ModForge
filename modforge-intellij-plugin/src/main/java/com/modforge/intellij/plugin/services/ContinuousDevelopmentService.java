package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService.CodeIssue;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that provides continuous development capabilities.
 * This service is responsible for continuously developing code by fixing errors and adding features.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    private static final long DEFAULT_CHECK_INTERVAL_MS = 60_000; // 1 minute
    
    private final Project project;
    private final AutonomousCodeGenerationService codeGenerationService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger fixedErrorCount = new AtomicInteger(0);
    private final AtomicInteger addedFeatureCount = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, Long> lastModifiedTimes = new ConcurrentHashMap<>();
    
    private ScheduledFuture<?> scheduledFuture;
    private long checkIntervalMs = DEFAULT_CHECK_INTERVAL_MS;
    
    /**
     * Creates a new ContinuousDevelopmentService.
     * @param project The project
     */
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
        this.codeGenerationService = AutonomousCodeGenerationService.getInstance(project);
        
        LOG.info("Continuous development service created for project: " + project.getName());
        
        // Load settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (settings != null) {
            if (settings.isContinuousDevelopmentEnabled()) {
                start();
            }
            
            checkIntervalMs = settings.getContinuousDevelopmentInterval();
        }
    }
    
    /**
     * Gets the continuous development service for a project.
     * @param project The project
     * @return The continuous development service
     */
    public static ContinuousDevelopmentService getInstance(@NotNull Project project) {
        return project.getService(ContinuousDevelopmentService.class);
    }
    
    /**
     * Starts continuous development.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting continuous development");
            
            // Schedule periodic checks
            scheduledFuture = scheduler.scheduleAtFixedRate(this::check, checkIntervalMs, checkIntervalMs, TimeUnit.MILLISECONDS);
            
            // Update settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            if (settings != null) {
                settings.setContinuousDevelopmentEnabled(true);
            }
        }
    }
    
    /**
     * Stops continuous development.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping continuous development");
            
            // Cancel scheduled checks
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            
            // Update settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            if (settings != null) {
                settings.setContinuousDevelopmentEnabled(false);
            }
        }
    }
    
    /**
     * Checks if continuous development is running.
     * @return Whether continuous development is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Sets the check interval.
     * @param intervalMs The check interval in milliseconds
     */
    public void setCheckInterval(long intervalMs) {
        LOG.info("Setting check interval to " + intervalMs + "ms");
        
        this.checkIntervalMs = intervalMs;
        
        // Restart if running
        if (running.get()) {
            stop();
            start();
        }
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (settings != null) {
            settings.setContinuousDevelopmentInterval(intervalMs);
        }
    }
    
    /**
     * Gets the check interval.
     * @return The check interval in milliseconds
     */
    public long getCheckInterval() {
        return checkIntervalMs;
    }
    
    /**
     * Gets statistics about continuous development.
     * @return Statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("running", running.get());
        statistics.put("fixedErrorCount", fixedErrorCount.get());
        statistics.put("addedFeatureCount", addedFeatureCount.get());
        statistics.put("checkIntervalMs", checkIntervalMs);
        
        return statistics;
    }
    
    /**
     * Performs a check for errors and fixes them.
     * This method is called periodically by the scheduler.
     */
    private void check() {
        if (!running.get()) {
            return;
        }
        
        LOG.info("Performing continuous development check");
        
        try {
            // Check for errors
            List<CodeIssue> issues = getActiveIssues();
            
            if (!issues.isEmpty()) {
                LOG.info("Found " + issues.size() + " issues to fix");
                
                // Fix issues
                fixIssues(issues);
            } else {
                LOG.info("No issues found");
                
                // Try to add features
                addFeatures();
            }
        } catch (Exception e) {
            LOG.error("Error during continuous development check", e);
        }
    }
    
    /**
     * Gets active issues in the project.
     * @return Active issues
     */
    @NotNull
    private List<CodeIssue> getActiveIssues() {
        // Get compilation issues from listener
        ModForgeCompilationListener compilationListener = project.getService(ModForgeCompilationListener.class);
        
        if (compilationListener != null) {
            List<ModForgeCompilationListener.CompilationIssue> compilationIssues = compilationListener.getActiveIssues();
            
            if (!compilationIssues.isEmpty()) {
                // Convert to code issues
                List<CodeIssue> issues = new ArrayList<>();
                
                for (ModForgeCompilationListener.CompilationIssue compilationIssue : compilationIssues) {
                    // Get file content
                    String filePath = compilationIssue.getFile();
                    String fileContent = getFileContent(filePath);
                    
                    if (fileContent != null) {
                        issues.add(new CodeIssue(
                                compilationIssue.getMessage(),
                                compilationIssue.getLine(),
                                compilationIssue.getColumn(),
                                filePath,
                                fileContent
                        ));
                    }
                }
                
                return issues;
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Gets the content of a file.
     * @param filePath The file path
     * @return The file content or null if an error occurs
     */
    @Nullable
    private String getFileContent(@NotNull String filePath) {
        try {
            // Find the file
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            
            if (file == null) {
                LOG.error("File not found: " + filePath);
                return null;
            }
            
            // Get PSI file
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            
            if (psiFile == null) {
                LOG.error("Could not get PSI file for file: " + filePath);
                return null;
            }
            
            // Get content
            return psiFile.getText();
        } catch (Exception e) {
            LOG.error("Error getting file content: " + filePath, e);
            return null;
        }
    }
    
    /**
     * Fixes issues.
     * @param issues The issues to fix
     */
    private void fixIssues(@NotNull List<CodeIssue> issues) {
        if (issues.isEmpty()) {
            return;
        }
        
        LOG.info("Fixing " + issues.size() + " issues");
        
        try {
            // Fix issues
            CompletableFuture<Map<String, String>> future = codeGenerationService.fixCodeIssues(issues, null);
            
            // Wait for result
            Map<String, String> fixedFiles = future.get(5, TimeUnit.MINUTES);
            
            if (fixedFiles.isEmpty()) {
                LOG.error("Failed to fix issues");
                return;
            }
            
            // Update fixed files
            ApplicationManager.getApplication().invokeLater(() -> {
                for (Map.Entry<String, String> entry : fixedFiles.entrySet()) {
                    String filePath = entry.getKey();
                    String fixedCode = entry.getValue();
                    
                    updateFile(filePath, fixedCode);
                }
                
                // Update statistics
                fixedErrorCount.addAndGet(issues.size());
            });
        } catch (Exception e) {
            LOG.error("Error fixing issues", e);
        }
    }
    
    /**
     * Updates a file with new content.
     * @param filePath The file path
     * @param newContent The new content
     */
    private void updateFile(@NotNull String filePath, @NotNull String newContent) {
        try {
            // Find the file
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            
            if (file == null) {
                LOG.error("File not found: " + filePath);
                return;
            }
            
            // Update file content
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    file.setBinaryContent(newContent.getBytes());
                    
                    // Update last modified time
                    lastModifiedTimes.put(filePath, file.getTimeStamp());
                    
                    LOG.info("Updated file: " + filePath);
                } catch (Exception e) {
                    LOG.error("Error updating file: " + filePath, e);
                }
            });
        } catch (Exception e) {
            LOG.error("Error updating file: " + filePath, e);
        }
    }
    
    /**
     * Adds features to the project.
     */
    private void addFeatures() {
        // This is a more complex task that would require analyzing the project
        // and determining what features to add. For now, we'll leave it as a placeholder.
        
        LOG.info("Feature addition is not implemented yet");
    }
    
    /**
     * Checks if a file has been modified since the last time it was updated.
     * @param filePath The file path
     * @return Whether the file has been modified
     */
    private boolean isFileModified(@NotNull String filePath) {
        try {
            // Find the file
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            
            if (file == null) {
                return false;
            }
            
            // Get last modified time
            Long lastModifiedTime = lastModifiedTimes.get(filePath);
            
            if (lastModifiedTime == null) {
                // File hasn't been tracked yet
                lastModifiedTimes.put(filePath, file.getTimeStamp());
                return false;
            }
            
            // Check if file has been modified
            return file.getTimeStamp() > lastModifiedTime;
        } catch (Exception e) {
            LOG.error("Error checking if file is modified: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Cleans up resources when the project is closed.
     */
    public void dispose() {
        LOG.info("Disposing continuous development service");
        
        // Stop continuous development
        stop();
        
        // Shutdown scheduler
        scheduler.shutdown();
        
        try {
            // Wait for tasks to complete
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}