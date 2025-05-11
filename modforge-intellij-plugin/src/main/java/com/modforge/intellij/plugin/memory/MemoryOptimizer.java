package com.modforge.intellij.plugin.memory;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.CacheUpdater;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.caches.CacheUpdaterFacade;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MemoryOptimizer is responsible for performing memory optimizations at different levels
 */
public class MemoryOptimizer implements Disposable {
    private static final Logger LOG = Logger.getInstance(MemoryOptimizer.class);
    
    private final Project project;
    private final AtomicBoolean optimizing = new AtomicBoolean(false);
    private final List<OptimizationListener> listeners = new ArrayList<>();
    
    /**
     * Optimization levels for memory optimization
     */
    public enum OptimizationLevel {
        MINIMAL,       // Basic cleanup with minimal impact
        CONSERVATIVE,  // Standard cleanup with low impact
        NORMAL,        // Balanced cleanup with moderate impact
        AGGRESSIVE     // Deep cleanup with higher impact
    }
    
    /**
     * Constructor
     * 
     * @param project The project for this optimizer
     */
    public MemoryOptimizer(Project project) {
        this.project = project;
    }
    
    /**
     * Optimize memory at the specified level
     * 
     * @param level The optimization level
     */
    public void optimize(OptimizationLevel level) {
        if (project.isDisposed()) {
            LOG.warn("Cannot optimize: project is disposed");
            return;
        }
        
        if (optimizing.compareAndSet(false, true)) {
            LOG.info("Starting memory optimization at level " + level);
            notifyOptimizationStarted(level);
            
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Memory Optimization", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setIndeterminate(false);
                        indicator.setText("Preparing optimization...");
                        indicator.setFraction(0.0);
                        
                        long beforeMemory = MemoryUtils.getUsedMemory();
                        LOG.info("Memory before optimization: " + MemoryUtils.formatMemorySize(beforeMemory));
                        
                        // Always perform minimal optimizations
                        performMinimalOptimizations(indicator);
                        
                        // Perform higher level optimizations based on the specified level
                        if (level.ordinal() >= OptimizationLevel.CONSERVATIVE.ordinal()) {
                            performConservativeOptimizations(indicator);
                        }
                        
                        if (level.ordinal() >= OptimizationLevel.NORMAL.ordinal()) {
                            performNormalOptimizations(indicator);
                        }
                        
                        if (level.ordinal() >= OptimizationLevel.AGGRESSIVE.ordinal()) {
                            performAggressiveOptimizations(indicator);
                        }
                        
                        // Final steps
                        indicator.setText("Requesting garbage collection...");
                        indicator.setFraction(0.95);
                        MemoryUtils.requestGarbageCollection();
                        
                        indicator.setText("Optimization complete");
                        indicator.setFraction(1.0);
                        
                        long afterMemory = MemoryUtils.getUsedMemory();
                        long memoryReduction = beforeMemory - afterMemory;
                        double percentReduction = beforeMemory > 0 ? (memoryReduction * 100.0 / beforeMemory) : 0.0;
                        
                        LOG.info("Memory after optimization: " + MemoryUtils.formatMemorySize(afterMemory));
                        LOG.info("Memory reduction: " + MemoryUtils.formatMemorySize(memoryReduction) + 
                                " (" + String.format("%.2f", percentReduction) + "%)");
                        
                        notifyOptimizationCompleted(level, beforeMemory, afterMemory);
                    } catch (Exception e) {
                        LOG.error("Error during memory optimization", e);
                        notifyOptimizationFailed(level, e);
                    } finally {
                        optimizing.set(false);
                    }
                }
            });
        } else {
            LOG.warn("Memory optimization already in progress");
        }
    }
    
    /**
     * Perform minimal optimizations (level 0) - lowest impact
     * 
     * @param indicator Progress indicator
     */
    private void performMinimalOptimizations(ProgressIndicator indicator) {
        indicator.setText("Performing minimal optimizations...");
        indicator.setFraction(0.1);
        
        LOG.info("Performing minimal optimizations");
        
        // Process pending UI events
        UIUtil.dispatchAllInvocationEvents();
        indicator.setFraction(0.15);
        
        // Clear editor caches
        ApplicationManager.getApplication().invokeLater(() -> {
            EditorFactory.getInstance().refreshAllEditors();
        });
        indicator.setFraction(0.2);
        
        // Commit documents
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiDocumentManager.getInstance(project).commitAllDocuments();
        });
        indicator.setFraction(0.25);
    }
    
    /**
     * Perform conservative optimizations (level 1) - low impact
     * 
     * @param indicator Progress indicator
     */
    private void performConservativeOptimizations(ProgressIndicator indicator) {
        indicator.setText("Performing conservative optimizations...");
        indicator.setFraction(0.3);
        
        LOG.info("Performing conservative optimizations");
        
        // Clear PSI caches
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiManager psiManager = PsiManager.getInstance(project);
            if (psiManager instanceof PsiManagerImpl) {
                ((PsiManagerImpl) psiManager).dropPsiCaches();
            }
        });
        indicator.setFraction(0.35);
        
        // Process pending UI events again
        UIUtil.dispatchAllInvocationEvents();
        indicator.setFraction(0.4);
    }
    
    /**
     * Perform normal optimizations (level 2) - moderate impact
     * 
     * @param indicator Progress indicator
     */
    private void performNormalOptimizations(ProgressIndicator indicator) {
        indicator.setText("Performing normal optimizations...");
        indicator.setFraction(0.5);
        
        LOG.info("Performing normal optimizations");
        
        // Clear inspector caches
        ApplicationManager.getApplication().invokeLater(() -> {
            DaemonCodeAnalyzer.getInstance(project).restart();
        });
        indicator.setFraction(0.6);
        
        // Clear event queue
        IdeEventQueue.getInstance().flushQueue();
        indicator.setFraction(0.65);
        
        // Process pending UI events again
        UIUtil.dispatchAllInvocationEvents();
        indicator.setFraction(0.7);
    }
    
    /**
     * Perform aggressive optimizations (level 3) - higher impact
     * 
     * @param indicator Progress indicator
     */
    private void performAggressiveOptimizations(ProgressIndicator indicator) {
        indicator.setText("Performing aggressive optimizations...");
        indicator.setFraction(0.75);
        
        LOG.info("Performing aggressive optimizations");
        
        // Update all caches
        ApplicationManager.getApplication().invokeLater(() -> {
            CacheUpdaterFacade.getInstance(project).processAllCacheUpdaters(new CacheUpdater() {
                @Override
                public void processFile(int fileId) {}
                
                @Override
                public int[] queryNeededFiles() {
                    return new int[0];
                }
                
                @Override
                public void update() {}
            });
        });
        indicator.setFraction(0.85);
        
        // Process pending UI events again
        UIUtil.dispatchAllInvocationEvents();
        indicator.setFraction(0.9);
    }
    
    /**
     * Check if optimization is in progress
     * 
     * @return True if optimization is in progress
     */
    public boolean isOptimizing() {
        return optimizing.get();
    }
    
    /**
     * Add an optimization listener
     * 
     * @param listener The listener to add
     */
    public void addOptimizationListener(OptimizationListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove an optimization listener
     * 
     * @param listener The listener to remove
     */
    public void removeOptimizationListener(OptimizationListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Notify listeners that optimization has started
     * 
     * @param level The optimization level
     */
    private void notifyOptimizationStarted(OptimizationLevel level) {
        List<OptimizationListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (OptimizationListener listener : listenersCopy) {
            try {
                listener.optimizationStarted(level);
            } catch (Exception e) {
                LOG.warn("Error notifying optimization listener", e);
            }
        }
    }
    
    /**
     * Notify listeners that optimization has completed
     * 
     * @param level The optimization level
     * @param beforeMemory Memory usage before optimization
     * @param afterMemory Memory usage after optimization
     */
    private void notifyOptimizationCompleted(OptimizationLevel level, long beforeMemory, long afterMemory) {
        List<OptimizationListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (OptimizationListener listener : listenersCopy) {
            try {
                listener.optimizationCompleted(level, beforeMemory, afterMemory);
            } catch (Exception e) {
                LOG.warn("Error notifying optimization listener", e);
            }
        }
    }
    
    /**
     * Notify listeners that optimization has failed
     * 
     * @param level The optimization level
     * @param error The error that occurred
     */
    private void notifyOptimizationFailed(OptimizationLevel level, Exception error) {
        List<OptimizationListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (OptimizationListener listener : listenersCopy) {
            try {
                listener.optimizationFailed(level, error);
            } catch (Exception e) {
                LOG.warn("Error notifying optimization listener", e);
            }
        }
    }
    
    /**
     * Reset the optimizer state and perform a quick optimization
     * This is useful when recovering from memory issues
     */
    public void reset() {
        LOG.info("Resetting memory optimizer");
        
        // Reset optimization state
        optimizing.set(false);
        
        // Perform a minimal optimization in the background
        if (!project.isDisposed()) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // Basic cleanup
                    cancelRunningTasks();
                    clearCaches();
                    MemoryUtils.requestGarbageCollection();
                    LOG.info("Memory optimizer reset completed");
                } catch (Exception e) {
                    LOG.error("Error during memory optimizer reset", e);
                }
            });
        }
    }
    
    @Override
    public void dispose() {
        listeners.clear();
    }
    
    /**
     * Interface for optimization listeners
     */
    public interface OptimizationListener {
        /**
         * Called when optimization starts
         * 
         * @param level The optimization level
         */
        default void optimizationStarted(OptimizationLevel level) {}
        
        /**
         * Called when optimization completes successfully
         * 
         * @param level The optimization level
         * @param beforeMemory Memory usage before optimization
         * @param afterMemory Memory usage after optimization
         */
        default void optimizationCompleted(OptimizationLevel level, long beforeMemory, long afterMemory) {}
        
        /**
         * Called when optimization fails
         * 
         * @param level The optimization level
         * @param error The error that occurred
         */
        default void optimizationFailed(OptimizationLevel level, Exception error) {}
    }
}