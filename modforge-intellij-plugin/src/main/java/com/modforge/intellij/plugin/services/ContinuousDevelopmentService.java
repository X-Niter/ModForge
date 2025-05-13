package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
        
        // Register for project closing using ProjectManagerListener
        ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
            @Override
            public void projectClosing(@NotNull Project closingProject) {
                if (closingProject.equals(project)) {
                    stop();
                }
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
            
            // Show notification to user
            ModForgeNotificationService.getInstance(project).showWarningNotification(
                    project,
                    "ModForge Continuous Development",
                    "Cannot start continuous development: You need to be authenticated."
            );
            
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
        
        // Notify user
        ModForgeNotificationService.getInstance(project).showInfoNotification(
                project,
                "ModForge Continuous Development",
                "Continuous development started for project " + project.getName()
        );
        
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
        
        // Notify user
        ModForgeNotificationService.getInstance(project).showInfoNotification(
                project,
                "ModForge Continuous Development",
                "Continuous development stopped for project " + project.getName()
        );
        
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
            
            // Get all files with problems using DaemonCodeAnalyzer
            DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            if (codeAnalyzer == null) {
                return;
            }
            
            // Check if there are problems
            if (!CompatibilityUtil.hasProblems(project)) {
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
            
            // Get all problem files using compatibility helper
            Collection<VirtualFile> problemFiles = CompatibilityUtil.getProblemFiles(project);
            
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
            DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            
            fixCount.incrementAndGet();
            for (VirtualFile file : problemFiles) {
                // Skip if file is not valid
                if (!file.isValid()) {
                    continue;
                }
                
                // Get problems for file using compatibility helper
                @SuppressWarnings("unchecked")
                Collection<CompatibilityUtil.Problem> problems = (Collection<CompatibilityUtil.Problem>)(Collection<?>) CompatibilityUtil.getProblemsForFile(project, file);
                
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
                    // Use reflection to get problem description (compatibility with 2025.1.1.1)
                    try {
                        // Try multiple potential method names that might exist in different IntelliJ versions
                        boolean descriptionFound = false;
                        for (String methodName : new String[]{"getDescriptionText", "getText", "getDescription", "getMessage"}) {
                            try {
                                java.lang.reflect.Method method = problem.getClass().getMethod(methodName);
                                Object result = method.invoke(problem);
                                if (result instanceof String) {
                                    String description = (String) result;
                                    if (description != null && !description.isEmpty()) {
                                        errorMessage.append(description).append("\n");
                                        descriptionFound = true;
                                        break; // Found a valid description, stop trying methods
                                    }
                                }
                            } catch (NoSuchMethodException ignored) {
                                // Try next method name
                            }
                        }
                        
                        // If no description method worked, try to build one from line and column info
                        if (!descriptionFound) {
                            StringBuilder sb = new StringBuilder();
                            
                            // Try to get line info
                            try {
                                java.lang.reflect.Method getLineMethod = problem.getClass().getMethod("getLine");
                                Object lineObj = getLineMethod.invoke(problem);
                                if (lineObj instanceof Integer) {
                                    int line = (Integer) lineObj;
                                    if (line >= 0) {
                                        sb.append("Line ").append(line);
                                        
                                        // Try to get column info
                                        try {
                                            java.lang.reflect.Method getColumnMethod = problem.getClass().getMethod("getColumn");
                                            Object columnObj = getColumnMethod.invoke(problem);
                                            if (columnObj instanceof Integer) {
                                                int column = (Integer) columnObj;
                                                if (column >= 0) {
                                                    sb.append(", Column ").append(column);
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Ignore column errors
                                        }
                                        
                                        sb.append(": Error in code");
                                        errorMessage.append(sb).append("\n");
                                        descriptionFound = true;
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore line errors
                            }
                            
                            // If we still have no description, add a generic fallback
                            if (!descriptionFound) {
                                errorMessage.append("Unknown error in file\n");
                            }
                        }
                    } catch (Exception e) {
                        errorMessage.append("Error accessing problem details: ").append(e.getMessage()).append("\n");
                    }
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
                            // Using CompatibilityUtil for better compatibility with IntelliJ IDEA 2025.1.1.1
                            CompatibilityUtil.runWriteAction(() -> {
                                try {
                                    // Write fixed code to file
                                    file.setBinaryContent(fixedCode.getBytes());
                                    
                                    // Record successful fix
                                    addActionLog("Fixed problems in file " + file.getName());
                                    
                                    // Notify user about successful fix
                                    ModForgeNotificationService.getInstance(project).showInfoNotification(
                                            project,
                                            "ModForge Continuous Development",
                                            "Fixed compilation issues in " + file.getName()
                                    );
                                } catch (Exception e) {
                                    LOG.error("Error writing fixed code to file " + file.getName(), e);
                                    
                                    // Notify user about the error
                                    ModForgeNotificationService.getInstance(project).showErrorNotification(
                                            project,
                                            "ModForge Continuous Development Error",
                                            "Failed to write fixed code to file " + file.getName() + ": " + e.getMessage()
                                    );
                                }
                            });
                        });
                    }
                } catch (Exception e) {
                    LOG.error("Error fixing code in file " + file.getName(), e);
                    
                    // Notify user about the error
                    ModForgeNotificationService.getInstance(project).showErrorNotification(
                            project,
                            "ModForge AI Error",
                            "Failed to fix code in " + file.getName() + ": " + e.getMessage()
                    );
                }
            }
        } catch (Exception e) {
            LOG.error("Error fixing problems", e);
            
            // Notify user about the error
            ModForgeNotificationService.getInstance(project).showErrorNotification(
                    project,
                    "ModForge Continuous Development Error",
                    "Failed to fix compilation problems: " + e.getMessage()
            );
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
     * Flag to enable/disable reduced features mode
     * This mode limits the scope and complexity of operations to reduce memory consumption
     */
    private final AtomicBoolean reducedFeaturesMode = new AtomicBoolean(false);
    
    /**
     * Set reduced features mode
     * This mode limits the scope and complexity of development operations
     * to reduce memory consumption during high memory pressure
     * 
     * @param enabled True to enable reduced features mode, false to disable
     */
    public void setReducedFeaturesMode(boolean enabled) {
        LOG.info((enabled ? "Enabling" : "Disabling") + " reduced features mode");
        reducedFeaturesMode.set(enabled);
    }
    
    /**
     * Check if reduced features mode is enabled
     * 
     * @return True if reduced features mode is enabled
     */
    public boolean isReducedFeaturesMode() {
        return reducedFeaturesMode.get();
    }
    
    /**
     * Execute a single development cycle
     * This is the main method for performing continuous development tasks
     */
    public void executeDevelopmentCycle() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        LOG.info("Executing development cycle (reduced features: " + reducedFeaturesMode.get() + ")");
        
        try {
            // First scan for problems
            scan();
            
            // Then perform enhancement
            performEnhancement();
            
            // Log completion
            LOG.info("Development cycle complete");
            addActionLog("Development cycle completed successfully");
            
        } catch (Exception e) {
            LOG.error("Error executing development cycle", e);
            addActionLog("Error executing development cycle: " + e.getMessage());
        }
    }
    
    /**
     * Perform lightweight development cycle
     * This only performs essential maintenance tasks and minimal operations
     * to avoid high memory consumption during critical memory pressure
     */
    public void performLightweightCycle() {
        if (!running.get() || project.isDisposed()) {
            return;
        }
        
        LOG.info("Executing lightweight development cycle");
        
        try {
            // Just scan for problems
            scan();
            
            // Skip enhancement and other intensive operations
            
            // Log completion
            LOG.info("Lightweight development cycle complete");
            addActionLog("Lightweight development cycle completed successfully");
            
        } catch (Exception e) {
            LOG.error("Error executing lightweight development cycle", e);
            addActionLog("Error executing lightweight development cycle: " + e.getMessage());
        }
    }
    
    /**
     * Fix compilation errors reported from the compiler
     * This method handles compilation errors from the ModForgeCompilationListener
     * 
     * @param errorDescriptions List of error descriptions from the compiler
     */
    public void fixCompilationErrors(@NotNull List<String> errorDescriptions) {
        if (!running.get() || project.isDisposed()) {
            LOG.info("Cannot fix compilation errors: service not running or project disposed");
            return;
        }
        
        if (errorDescriptions.isEmpty()) {
            LOG.info("No compilation errors to fix");
            return;
        }
        
        LOG.info("Fixing " + errorDescriptions.size() + " compilation errors");
        addActionLog("Attempting to fix " + errorDescriptions.size() + " compilation errors");
        
        // Notify user that AI is working on fixing errors
        ModForgeNotificationService.getInstance(project).showInfoNotification(
                project,
                "ModForge AI Working",
                "Attempting to fix " + errorDescriptions.size() + " compilation errors..."
        );
        
        try {
            // Get code generation service
            AutonomousCodeGenerationService codeGenService =
                    project.getService(AutonomousCodeGenerationService.class);
            
            if (codeGenService == null) {
                LOG.error("Code generation service is not available");
                
                // Notify user about the error
                ModForgeNotificationService.getInstance(project).showErrorNotification(
                        project,
                        "ModForge Error",
                        "Cannot fix compilation errors: AI code generation service is not available"
                );
                
                return;
            }
            
            // Build error message from error descriptions
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Compilation errors detected:\n\n");
            
            for (String errorDescription : errorDescriptions) {
                errorMessage.append("- ").append(errorDescription).append("\n");
            }
            
            // Extract file path patterns to identify problematic files
            Set<String> potentialFileNames = new HashSet<>();
            for (String errorDesc : errorDescriptions) {
                // Extract file name using regex for "file: X"
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("file:\\s*([^,)]+)");
                java.util.regex.Matcher matcher = pattern.matcher(errorDesc);
                if (matcher.find()) {
                    potentialFileNames.add(matcher.group(1).trim());
                }
            }
            
            // Find files that match potential file names
            PsiManager psiManager = PsiManager.getInstance(project);
            FixCompilationTask fixTask = new FixCompilationTask(
                    project, errorMessage.toString(), potentialFileNames, psiManager, codeGenService);
            
            // Run fix task in background
            ApplicationManager.getApplication().executeOnPooledThread(fixTask);
            
            // Record attempt
            fixCount.incrementAndGet();
            
        } catch (Exception e) {
            LOG.error("Error fixing compilation errors", e);
            addActionLog("Error fixing compilation errors: " + e.getMessage());
        }
    }
    
    /**
     * Task to fix compilation errors
     * Handles the actual fixing of compilation errors in a background thread
     */
    private class FixCompilationTask implements Runnable {
        private final Project project;
        private final String errorMessage;
        private final Set<String> potentialFileNames;
        private final PsiManager psiManager;
        private final AutonomousCodeGenerationService codeGenService;
        
        FixCompilationTask(Project project, String errorMessage, Set<String> potentialFileNames,
                           PsiManager psiManager, AutonomousCodeGenerationService codeGenService) {
            this.project = project;
            this.errorMessage = errorMessage;
            this.potentialFileNames = potentialFileNames;
            this.psiManager = psiManager;
            this.codeGenService = codeGenService;
        }
        
        @Override
        public void run() {
            try {
                if (project.isDisposed() || !running.get()) {
                    return;
                }
                
                // Find files from potential file names
                Set<VirtualFile> filesToFix = new HashSet<>();
                com.intellij.openapi.vfs.VirtualFileManager vfm = com.intellij.openapi.vfs.VirtualFileManager.getInstance();
                
                // First try direct file paths
                for (String fileName : potentialFileNames) {
                    com.intellij.openapi.vfs.VirtualFile file = vfm.findFileByUrl("file://" + fileName);
                    if (file != null && file.exists()) {
                        filesToFix.add(file);
                    }
                }
                
                // If no files found, search in project
                if (filesToFix.isEmpty()) {
                    // Search in project content roots
                    com.intellij.openapi.roots.ProjectRootManager rootManager = com.intellij.openapi.roots.ProjectRootManager.getInstance(project);
                    com.intellij.openapi.vfs.VirtualFile[] contentRoots = rootManager.getContentRoots();
                    
                    for (com.intellij.openapi.vfs.VirtualFile root : contentRoots) {
                        for (String fileName : potentialFileNames) {
                            // Use recursion to find files in subdirectories
                            findFiles(root, fileName, filesToFix);
                        }
                    }
                }
                
                // Fix each file
                LOG.info("Found " + filesToFix.size() + " files to fix from compilation errors");
                
                for (VirtualFile file : filesToFix) {
                    fixFile(file);
                }
                
                LOG.info("Compilation error fixing task complete");
                addActionLog("Compilation error fixing completed for " + filesToFix.size() + " files");
                
            } catch (Exception e) {
                LOG.error("Error in fix compilation task", e);
                addActionLog("Error fixing compilation errors: " + e.getMessage());
            }
        }
        
        private void findFiles(VirtualFile dir, String fileName, Set<VirtualFile> results) {
            if (!dir.isDirectory()) {
                return;
            }
            
            for (VirtualFile child : dir.getChildren()) {
                if (child.isDirectory()) {
                    findFiles(child, fileName, results);
                } else if (child.getName().equals(fileName)) {
                    results.add(child);
                }
            }
        }
        
        private void fixFile(VirtualFile file) {
            try {
                // Get file content
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile == null) {
                    return;
                }
                
                String code = psiFile.getText();
                
                // Get language from file extension
                String language = getLanguageFromExtension(file.getExtension());
                
                // Fix code
                LOG.info("Attempting to fix compilation errors in " + file.getName());
                String fixedCode = codeGenService.fixCode(code, errorMessage, language)
                        .exceptionally(e -> {
                            LOG.error("Error fixing compilation errors in file " + file.getName(), e);
                            
                            // Notify user about the error
                            ModForgeNotificationService.getInstance(project).showErrorNotification(
                                    project,
                                    "ModForge AI Error",
                                    "Failed to generate fixes for " + file.getName() + ": " + e.getMessage()
                            );
                            
                            return null;
                        })
                        .join();
                
                // Apply fixed code
                if (fixedCode != null && !fixedCode.isEmpty() && !fixedCode.equals(code)) {
                    LOG.info("Applying fixed code to file " + file.getName());
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // Using CompatibilityUtil for better compatibility with IntelliJ IDEA 2025.1.1.1
                        CompatibilityUtil.runWriteAction(() -> {
                            try {
                                // Write fixed code to file
                                file.setBinaryContent(fixedCode.getBytes());
                                
                                // Record successful fix
                                addActionLog("Fixed compilation errors in file " + file.getName());
                                
                                // Notify user about successful fix
                                ModForgeNotificationService.getInstance(project).showInfoNotification(
                                        project,
                                        "ModForge AI Success",
                                        "Fixed compilation errors in " + file.getName()
                                );
                            } catch (Exception e) {
                                LOG.error("Error writing fixed code to file " + file.getName(), e);
                                
                                // Notify user about the error
                                ModForgeNotificationService.getInstance(project).showErrorNotification(
                                        project,
                                        "ModForge Error",
                                        "Failed to write fixes to " + file.getName() + ": " + e.getMessage()
                                );
                            }
                        });
                    });
                }
            } catch (Exception e) {
                LOG.error("Error fixing compilation errors in file " + file.getName(), e);
            }
        }
    }
    
    /**
     * Perform code enhancement
     * This method looks for opportunities to improve code quality, modularity, etc.
     */
    private void performEnhancement() {
        if (!running.get() || project.isDisposed() || reducedFeaturesMode.get()) {
            // Skip enhancement in reduced features mode
            return;
        }
        
        try {
            LOG.info("Performing code enhancement");
            addActionLog("Performing code enhancement");
            
            // This is where enhancement logic would go
            // Since this is just the interface integration, we'll leave it as a placeholder
            
            LOG.info("Code enhancement complete");
            
        } catch (Exception e) {
            LOG.error("Error performing code enhancement", e);
            addActionLog("Error performing code enhancement: " + e.getMessage());
        }
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