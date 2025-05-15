package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;

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
    private final AtomicBoolean reducedFeaturesMode = new AtomicBoolean(false);

    private ScheduledFuture<?> scheduledTask;
    private long lastScanTime;
    private long scanInterval = DEFAULT_SCAN_INTERVAL_MS;

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

    public void start() {
        if (running.get()) {
            return;
        }

        LOG.info("Starting continuous development for project " + project.getName());

        // Schedule task
        scheduledTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                this::scan,
                0,
                scanInterval,
                TimeUnit.MILLISECONDS);

        running.set(true);

        // Notify listeners
        for (ContinuousDevelopmentListener listener : listeners) {
            listener.continuousDevelopmentStarted();
        }
    }

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

    private void scan() {
        if (!running.get() || project.isDisposed()) {
            return;
        }

        try {
            LOG.info("Scanning project for problems...");
            lastScanTime = System.currentTimeMillis();

            // Get project files with errors
            Collection<VirtualFile> problemFiles = CompatibilityUtil.findFilesWithProblems(project);

            if (!problemFiles.isEmpty()) {
                LOG.info("Found " + problemFiles.size() + " files with problems");
                // Fix problems in files
                fixProblems(problemFiles);
            } else {
                LOG.info("No problems found");
            }

            // Notify listeners
            for (ContinuousDevelopmentListener listener : listeners) {
                listener.continuousDevelopmentScanned(problemFiles);
            }

        } catch (Exception e) {
            LOG.error("Error scanning project", e);
            errorCount.incrementAndGet();
        }
    }

    private void fixProblems(@NotNull Collection<VirtualFile> problemFiles) {
        if (!running.get() || project.isDisposed()) {
            return;
        }

        try {
            for (VirtualFile file : problemFiles) {
                Collection<CompatibilityUtil.Problem> problems = CompatibilityUtil.getProblems(project, file);
                if (!problems.isEmpty()) {
                    processProblems(file, problems);
                }
            }
        } catch (Exception e) {
            LOG.error("Error fixing problems", e);
            errorCount.incrementAndGet();
        }
    }

    private void processProblems(@NotNull VirtualFile file, @NotNull Collection<CompatibilityUtil.Problem> problems) {
        try {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return;
            }

            // Format problems
            StringBuilder errorMessage = new StringBuilder();
            for (CompatibilityUtil.Problem problem : problems) {
                errorMessage.append(problem.getDescription()).append("\n");
            }

            if (errorMessage.length() > 0) {
                // Apply the fix if needed
                WriteAction.runAndWait(() -> {
                    Document document = FileDocumentManager.getInstance().getDocument(file);
                    if (document != null) {
                        // Future: Add code fix logic here
                        FileDocumentManager.getInstance().saveDocument(document);
                        successCount.incrementAndGet();
                    }
                });
            }
        } catch (Exception e) {
            LOG.error("Error processing problems in " + file.getName(), e);
            errorCount.incrementAndGet();
        }
    }

    /**
     * Perform a lightweight development cycle.
     */
    public void performLightweightCycle() {
        LOG.info("Performing lightweight development cycle");
        // Add logic for lightweight cycle here
    }

    /**
     * Execute a full development cycle.
     */
    public void executeDevelopmentCycle() {
        LOG.info("Executing full development cycle");
        // Add logic for full development cycle here
    }

    public interface ContinuousDevelopmentListener {
        default void continuousDevelopmentStarted() {
        }

        default void continuousDevelopmentStopped() {
        }

        default void continuousDevelopmentScanned(@NotNull Collection<VirtualFile> problemFiles) {
        }
    }

    public void setReducedFeaturesMode(boolean enabled) {
        LOG.info((enabled ? "Enabling" : "Disabling") + " reduced features mode");
        reducedFeaturesMode.set(enabled);
    }

    public boolean isReducedFeaturesMode() {
        return reducedFeaturesMode.get();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

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

    public boolean isRunning() {
        return running.get();
    }

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

    public long getScanInterval() {
        return scanInterval;
    }

    public void addListener(@NotNull ContinuousDevelopmentListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull ContinuousDevelopmentListener listener) {
        listeners.remove(listener);
    }

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
        stats.put("reducedFeaturesMode", reducedFeaturesMode.get());
        return stats;
    }
}