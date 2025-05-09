package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that manages continuous development tasks.
 * This service is responsible for processing files in the background,
 * enhancing them with AI-powered suggestions, and ensuring they compile correctly.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    private static final int DEFAULT_CHECK_INTERVAL_MS = 30000; // 30 seconds
    
    private final Project project;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> lastModifiedTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> enhancementCounts = new ConcurrentHashMap<>();
    private final Set<String> processingFiles = new HashSet<>();
    private final Object lock = new Object();
    
    private ScheduledFuture<?> scheduledTask;
    
    /**
     * Creates a new ContinuousDevelopmentService.
     * @param project The project
     */
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
        this.scheduler = AppExecutorUtil.getAppScheduledExecutorService();
        
        LOG.info("Continuous development service created for project: " + project.getName());
        
        // Start continuous development task if enabled
        if (ModForgeSettings.getInstance().isEnableContinuousDevelopment()) {
            startContinuousDevelopment();
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
     * Starts the continuous development task.
     */
    public void startContinuousDevelopment() {
        synchronized (lock) {
            if (scheduledTask != null && !scheduledTask.isDone()) {
                // Already running
                LOG.info("Continuous development already running for project: " + project.getName());
                return;
            }
            
            LOG.info("Starting continuous development for project: " + project.getName());
            
            // Schedule task
            scheduledTask = scheduler.scheduleWithFixedDelay(
                    this::processChangedFiles,
                    DEFAULT_CHECK_INTERVAL_MS,
                    DEFAULT_CHECK_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * Stops the continuous development task.
     */
    public void stopContinuousDevelopment() {
        synchronized (lock) {
            if (scheduledTask != null) {
                LOG.info("Stopping continuous development for project: " + project.getName());
                
                // Cancel task
                scheduledTask.cancel(false);
                scheduledTask = null;
            }
        }
    }
    
    /**
     * Stops all development tasks.
     */
    public void stopAllDevelopment() {
        synchronized (lock) {
            stopContinuousDevelopment();
            
            // Clear data
            lastModifiedTimes.clear();
            enhancementCounts.clear();
            processingFiles.clear();
        }
    }
    
    /**
     * Notifies the service that a file has changed.
     * @param file The changed file
     */
    public void fileChanged(VirtualFile file) {
        if (file == null) {
            return;
        }
        
        String path = file.getPath();
        
        // Update last modified time
        lastModifiedTimes.put(path, file.getTimeStamp());
    }
    
    /**
     * Processes changed files.
     */
    private void processChangedFiles() {
        synchronized (lock) {
            // Check if continuous development is enabled
            if (!ModForgeSettings.getInstance().isEnableContinuousDevelopment()) {
                return;
            }
            
            LOG.info("Processing changed files for project: " + project.getName());
            
            // Get files to process
            Map<String, Long> filesToProcess = new HashMap<>(lastModifiedTimes);
            
            // Process each file
            for (Map.Entry<String, Long> entry : filesToProcess.entrySet()) {
                String path = entry.getKey();
                
                // Skip if already processing
                if (processingFiles.contains(path)) {
                    continue;
                }
                
                // Get file
                VirtualFile file = ApplicationManager.getApplication().runReadAction(
                        (Computable<VirtualFile>) () -> {
                            return com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(path);
                        }
                );
                
                if (file == null || !file.exists()) {
                    // File no longer exists
                    lastModifiedTimes.remove(path);
                    enhancementCounts.remove(path);
                    continue;
                }
                
                // Check if file needs to be processed
                if (!shouldProcessFile(file)) {
                    continue;
                }
                
                // Mark as processing
                processingFiles.add(path);
                
                // Process file in background
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        processFile(file);
                    } finally {
                        // Mark as no longer processing
                        synchronized (lock) {
                            processingFiles.remove(path);
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Checks if a file should be processed.
     * @param file The file
     * @return True if the file should be processed
     */
    private boolean shouldProcessFile(VirtualFile file) {
        // Skip non-code files
        String extension = file.getExtension();
        if (extension == null || !(extension.equals("java") || extension.equals("kt"))) {
            return false;
        }
        
        // Skip if max enhancement count reached
        String path = file.getPath();
        AtomicInteger count = enhancementCounts.computeIfAbsent(path, k -> new AtomicInteger(0));
        return count.get() < ModForgeSettings.getInstance().getMaxEnhancementsPerFile();
    }
    
    /**
     * Processes a file.
     * @param file The file to process
     */
    private void processFile(VirtualFile file) {
        LOG.info("Processing file: " + file.getName());
        
        try {
            // Get service
            AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
            
            // Analyze and enhance file
            boolean enhanced = codeGenService.analyzeAndEnhanceFile(file)
                    .thenApply(result -> {
                        // Update enhancement count if enhanced
                        if (result) {
                            String path = file.getPath();
                            enhancementCounts.computeIfAbsent(path, k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                        
                        return result;
                    })
                    .exceptionally(ex -> {
                        LOG.error("Error enhancing file: " + file.getName(), ex);
                        return false;
                    })
                    .get(); // Wait for completion
            
            if (enhanced) {
                LOG.info("Enhanced file: " + file.getName());
            } else {
                LOG.info("No enhancements made to file: " + file.getName());
            }
        } catch (Exception ex) {
            LOG.error("Error processing file: " + file.getName(), ex);
        }
    }
}