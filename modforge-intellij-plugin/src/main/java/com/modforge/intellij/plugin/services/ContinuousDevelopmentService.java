package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for continuous development.
 * This service monitors projects for errors and fixes them automatically.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    private static final long DEFAULT_SCAN_INTERVAL_MS = 60 * 1000; // 1 minute
    
    private final Project project;
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger fixCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> lastActions = new ConcurrentHashMap<>();
    private final ReadWriteLock statsLock = new ReentrantReadWriteLock();
    private final List<ContinuousDevelopmentListener> listeners = new CopyOnWriteArrayList<>();
    
    private ScheduledFuture<?> scheduledTask;
    private long lastScanTime;
    private long scanInterval = DEFAULT_SCAN_INTERVAL_MS;
    
    /**
     * Create a continuous development service.
     *
     * @param project The project
     */
    public ContinuousDevelopmentService(@NotNull Project project) {
        this.project = project;
        
        // Initialize from settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        enabled.set(settings.isEnableContinuousDevelopment());
        scanInterval = settings.getContinuousDevelopmentScanInterval();
        
        // Register for project closing
        project.getMessageBus().connect().subscribe(Project.TOPIC, new Project.ProjectListener() {
            @Override
            public void projectClosing(@NotNull Project project) {
                stop();
            }
        });
        
        // Start if enabled
        if (enabled.get()) {
            start();
        }
    }
    
    /**
     * Start continuous development.
     */
    public void start() {
        if (running.get()) {
            return;
        }
        
        LOG.info("Starting continuous development for project " + project.getName());
        
        // Make sure authentication is valid
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            LOG.warn("Cannot start continuous development: not authenticated");
            return;
        }
        
        // Schedule task
        scheduledTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                this::scan,
                0,
                scanInterval,
                TimeUnit.MILLISECONDS
        );
        
        running.set(true);
        
        // Notify listeners
        for (ContinuousDevelopmentListener listener : listeners) {
            listener.continuousDevelopmentStarted();
        }
    }
    
    /**
     * Stop continuous development.
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        LOG.info("Stopping continuous development for project " + project.getName());
        
        // Cancel task
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        
        running.set(false);
        
        // Notify listeners
        for (ContinuousDevelopmentListener listener : listeners) {
            listener.continuousDevelopmentStopped();
        }
    }
    
    /**
     * Scan the project for errors.
     */
    private void scan() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        try {
            LOG.info("Scanning project " + project.getName() + " for errors");
            
            // Update last scan time
            lastScanTime = System.currentTimeMillis();
            
            // Get all files with problems
            WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
            if (problemSolver == null) {
                return;
            }
            
            // Check if there are problems
            if (!problemSolver.hasProblemFilesBeneath(psiElement -> true)) {
                LOG.info("No problems found in project " + project.getName());
                
                // Record successful scan
                addActionLog("Scan successful, no problems found");
                successCount.incrementAndGet();
                
                // Notify listeners
                for (ContinuousDevelopmentListener listener : listeners) {
                    listener.continuousDevelopmentScanned(Collections.emptyList());
                }
                
                return;
            }
            
            // Get all problem files
            Collection<VirtualFile> problemFiles = new ArrayList<>();
            problemSolver.visitProblemFiles(problemFiles::add);
            
            LOG.info("Found " + problemFiles.size() + " problem files in project " + project.getName());
            
            // Record scan with problems
            addActionLog("Scan found " + problemFiles.size() + " files with problems");
            errorCount.incrementAndGet();
            
            // Notify listeners
            for (ContinuousDevelopmentListener listener : listeners) {
                listener.continuousDevelopmentScanned(problemFiles);
            }
            
            // Fix problems
            fixProblems(problemFiles);
        } catch (Exception e) {
            LOG.error("Error scanning project for errors", e);
        }
    }
    
    /**
     * Fix problems in the given files.
     *
     * @param problemFiles The files with problems
     */
    private void fixProblems(@NotNull Collection<VirtualFile> problemFiles) {
        if (problemFiles.isEmpty() || !running.get() || project.isDisposed()) {
            return;
        }
        
        try {
            LOG.info("Fixing problems in " + problemFiles.size() + " files");
            
            // Get code generation service
            AutonomousCodeGenerationService codeGenService =
                    project.getService(AutonomousCodeGenerationService.class);
            
            if (codeGenService == null) {
                LOG.error("Code generation service is not available");
                return;
            }
            
            // Fix problems in each file
            PsiManager psiManager = PsiManager.getInstance(project);
            WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
            
            fixCount.incrementAndGet();
            for (VirtualFile file : problemFiles) {
                // Skip if file is not valid
                if (!file.isValid()) {
                    continue;
                }
                
                // Get problems for file
                Collection<Problem> problems = new ArrayList<>();
                problemSolver.processProblems(problems, file);
                
                if (problems.isEmpty()) {
                    continue;
                }
                
                // Get file content
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile == null) {
                    continue;
                }
                
                String code = psiFile.getText();
                
                // Format problems
                StringBuilder errorMessage = new StringBuilder();
                for (Problem problem : problems) {
                    errorMessage.append(problem.getDescription()).append("\n");
                }
                
                // Get language from file extension
                String language = getLanguageFromExtension(file.getExtension());
                
                // Fix code
                try {
                    String fixedCode = codeGenService.fixCode(code, errorMessage.toString(), language)
                            .exceptionally(e -> {
                                LOG.error("Error fixing code in file " + file.getName(), e);
                                return null;
                            })
                            .join();
                    
                    // Apply fixed code
                    if (fixedCode != null && !fixedCode.isEmpty() && !fixedCode.equals(code)) {
                        LOG.info("Applying fixed code to file " + file.getName());
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            ApplicationManager.getApplication().runWriteAction(() -> {
                                try {
                                    // Write fixed code to file
                                    file.setBinaryContent(fixedCode.getBytes());
                                    
                                    // Record successful fix
                                    addActionLog("Fixed problems in file " + file.getName());
                                } catch (Exception e) {
                                    LOG.error("Error writing fixed code to file " + file.getName(), e);
                                }
                            });
                        });
                    }
                } catch (Exception e) {
                    LOG.error("Error fixing code in file " + file.getName(), e);
                }
            }
        } catch (Exception e) {
            LOG.error("Error fixing problems", e);
        }
    }
    
    /**
     * Get language from file extension.
     *
     * @param extension The file extension
     * @return The language name
     */
    @NotNull
    private String getLanguageFromExtension(@Nullable String extension) {
        if (extension == null) {
            return "Java"; // Default to Java for Minecraft mods
        }
        
        switch (extension.toLowerCase()) {
            case "java":
                return "Java";
            case "kt":
                return "Kotlin";
            case "js":
                return "JavaScript";
            case "ts":
                return "TypeScript";
            case "py":
                return "Python";
            case "c":
                return "C";
            case "cpp":
            case "cc":
            case "cxx":
                return "C++";
            case "cs":
                return "C#";
            case "go":
                return "Go";
            case "rs":
                return "Rust";
            case "rb":
                return "Ruby";
            case "php":
                return "PHP";
            case "swift":
                return "Swift";
            case "html":
                return "HTML";
            case "css":
                return "CSS";
            case "json":
                return "JSON";
            case "xml":
                return "XML";
            default:
                return "Java"; // Default to Java for Minecraft mods
        }
    }
    
    /**
     * Check if continuous development is enabled.
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled.get();
    }
    
    /**
     * Enable or disable continuous development.
     *
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setEnableContinuousDevelopment(enabled);
        
        // Start or stop
        if (enabled) {
            start();
        } else {
            stop();
        }
    }
    
    /**
     * Check if continuous development is running.
     *
     * @return True if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Set the scan interval.
     *
     * @param scanInterval The scan interval in milliseconds
     */
    public void setScanInterval(long scanInterval) {
        this.scanInterval = scanInterval;
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setContinuousDevelopmentScanInterval(scanInterval);
        
        // Restart if running
        if (running.get()) {
            stop();
            start();
        }
    }
    
    /**
     * Get the scan interval.
     *
     * @return The scan interval in milliseconds
     */
    public long getScanInterval() {
        return scanInterval;
    }
    
    /**
     * Add a continuous development listener.
     *
     * @param listener The listener
     */
    public void addListener(@NotNull ContinuousDevelopmentListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a continuous development listener.
     *
     * @param listener The listener
     */
    public void removeListener(@NotNull ContinuousDevelopmentListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Add an action log entry.
     *
     * @param action The action
     */
    private void addActionLog(@NotNull String action) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        
        // Remove oldest entry if we have too many
        statsLock.writeLock().lock();
        try {
            if (lastActions.size() >= 100) {
                String oldestKey = lastActions.keySet().stream()
                        .sorted()
                        .findFirst()
                        .orElse(null);
                
                if (oldestKey != null) {
                    lastActions.remove(oldestKey);
                }
            }
            
            // Add new entry
            lastActions.put(timestamp, action);
        } finally {
            statsLock.writeLock().unlock();
        }
    }
    
    /**
     * Get statistics about the continuous development service.
     *
     * @return Statistics as a map
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("enabled", enabled.get());
        stats.put("running", running.get());
        stats.put("scanInterval", scanInterval);
        stats.put("lastScanTime", lastScanTime);
        stats.put("fixCount", fixCount.get());
        stats.put("errorCount", errorCount.get());
        stats.put("successCount", successCount.get());
        
        // Get last actions
        statsLock.readLock().lock();
        try {
            Map<String, String> actions = new LinkedHashMap<>(lastActions);
            stats.put("lastActions", actions);
        } finally {
            statsLock.readLock().unlock();
        }
        
        return stats;
    }
    
    /**
     * Listener for continuous development events.
     */
    public interface ContinuousDevelopmentListener {
        /**
         * Called when continuous development is started.
         */
        default void continuousDevelopmentStarted() {
        }
        
        /**
         * Called when continuous development is stopped.
         */
        default void continuousDevelopmentStopped() {
        }
        
        /**
         * Called when continuous development has scanned for errors.
         *
         * @param problemFiles Files with problems (can be empty)
         */
        default void continuousDevelopmentScanned(@NotNull Collection<VirtualFile> problemFiles) {
        }
    }
}