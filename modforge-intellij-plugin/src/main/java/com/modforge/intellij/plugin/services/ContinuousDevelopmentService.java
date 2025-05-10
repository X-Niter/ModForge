package com.modforge.intellij.plugin.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for continuous development of mods.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    
    // Default interval between continuous development checks in milliseconds (5 minutes)
    private static final long DEFAULT_INTERVAL_MS = 5 * 60 * 1000;
    
    // Minimum interval between continuous development checks in milliseconds (1 minute)
    private static final long MIN_INTERVAL_MS = 60 * 1000;
    
    private final Project project;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private MessageBusConnection connection;
    
    // Metrics
    private final AtomicInteger taskCount = new AtomicInteger(0);
    
    /**
     * Create ContinuousDevelopmentService.
     * @param project Project
     */
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
        this.scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("ModForge Continuous Development", 1);
        
        // Subscribe to file change events
        subscribeToFileChanges();
        
        // Start continuous development if enabled
        startIfEnabled();
    }
    
    /**
     * Subscribe to file change events.
     */
    private void subscribeToFileChanges() {
        connection = project.getMessageBus().connect();
        
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                // Process file changes if continuous development is enabled
                if (isEnabled()) {
                    for (VFileEvent event : events) {
                        VirtualFile file = event.getFile();
                        
                        // Skip if file is null
                        if (file == null) {
                            continue;
                        }
                        
                        // Skip if file is not a source file
                        String fileName = file.getName();
                        if (!isSourceFile(fileName)) {
                            continue;
                        }
                        
                        // Skip if file is not in project
                        if (!file.getPath().contains(project.getBasePath())) {
                            continue;
                        }
                        
                        // File change detected, schedule a task
                        scheduleTask(5000); // 5 seconds delay to allow for multiple changes
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * Check if file is a source file.
     * @param fileName File name
     * @return Whether file is a source file
     */
    private boolean isSourceFile(String fileName) {
        return fileName.endsWith(".java") || 
               fileName.endsWith(".kt") || 
               fileName.endsWith(".groovy") || 
               fileName.endsWith(".scala") || 
               fileName.endsWith(".json");
    }
    
    /**
     * Start continuous development if enabled.
     */
    private void startIfEnabled() {
        if (isEnabled()) {
            start();
        }
    }
    
    /**
     * Check if continuous development is enabled.
     * @return Whether continuous development is enabled
     */
    public boolean isEnabled() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.isContinuousDevelopment();
    }
    
    /**
     * Check if continuous development is running.
     * @return Whether continuous development is running
     */
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }
    
    /**
     * Start continuous development.
     */
    public void start() {
        if (isRunning()) {
            stop();
        }
        
        long intervalMs = DEFAULT_INTERVAL_MS;
        
        // Schedule task
        scheduledTask = scheduler.scheduleWithFixedDelay(
            () -> performContinuousDevelopment(),
            10000, // 10 seconds initial delay
            intervalMs,
            TimeUnit.MILLISECONDS
        );
        
        LOG.info("Continuous development started");
    }
    
    /**
     * Stop continuous development.
     */
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
            
            LOG.info("Continuous development stopped");
        }
    }
    
    /**
     * Schedule a continuous development task with delay.
     * @param delayMs Delay in milliseconds
     */
    public void scheduleTask(long delayMs) {
        if (!isEnabled() || !isRunning()) {
            return;
        }
        
        // Cancel existing task
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        
        // Schedule new task with delay
        scheduledTask = scheduler.schedule(
            () -> {
                performContinuousDevelopment();
                
                // Reschedule with fixed delay
                scheduledTask = scheduler.scheduleWithFixedDelay(
                    () -> performContinuousDevelopment(),
                    DEFAULT_INTERVAL_MS,
                    DEFAULT_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
                );
            },
            delayMs,
            TimeUnit.MILLISECONDS
        );
        
        LOG.info("Continuous development task scheduled with delay " + delayMs + "ms");
    }
    
    /**
     * Perform continuous development.
     */
    private void performContinuousDevelopment() {
        try {
            // Skip if project is disposed
            if (project.isDisposed()) {
                return;
            }
            
            // Skip if not authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Skipping continuous development because not authenticated");
                return;
            }
            
            LOG.info("Performing continuous development");
            
            // TODO: Implement continuous development
            // This would involve:
            // 1. Getting all source files in the project
            // 2. Checking for compilation errors
            // 3. Fixing compilation errors
            // 4. Enhancing code
            
            // Increment task count
            taskCount.incrementAndGet();
        } catch (Exception e) {
            LOG.error("Error performing continuous development", e);
        }
    }
    
    /**
     * Get task count.
     * @return Task count
     */
    public int getTaskCount() {
        return taskCount.get();
    }
    
    /**
     * Dispose service.
     */
    public void dispose() {
        // Stop continuous development
        stop();
        
        // Disconnect from message bus
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        
        // Shutdown scheduler
        scheduler.shutdownNow();
    }
    
    /**
     * Startup activity to initialize continuous development service.
     */
    public static class ContinuousDevelopmentStartupActivity implements StartupActivity, DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            // Get continuous development service
            ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
            
            // Start if enabled
            if (service.isEnabled() && !service.isRunning()) {
                service.start();
            }
        }
    }
}