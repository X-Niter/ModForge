package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryListener;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemorySnapshot;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Status bar widget to display memory usage
 */
public class MemoryStatusBarWidget implements StatusBarWidget.TextPresentation, MemoryListener {
    private static final String ID = "ModForgeMemoryWidget";
    private static final DecimalFormat FORMAT = new DecimalFormat("#0.0");
    
    private StatusBar statusBar;
    private MemorySnapshot lastSnapshot;
    private ScheduledFuture<?> updateTask;
    
    /**
     * Factory for creating the widget
     */
    public static class Factory implements StatusBarWidgetFactory {
        @Override
        public @NotNull String getId() {
            return ID;
        }
        
        @Override
        public @Nls @NotNull String getDisplayName() {
            return "ModForge Memory";
        }
        
        @Override
        public boolean isAvailable(@NotNull Project project) {
            return true;
        }
        
        @Override
        public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
            return new MemoryStatusBarWidget();
        }
        
        @Override
        public void disposeWidget(@NotNull StatusBarWidget widget) {
            if (widget instanceof MemoryStatusBarWidget) {
                ((MemoryStatusBarWidget) widget).dispose();
            }
        }
        
        @Override
        public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
            return true;
        }
    }
    
    @Override
    public @NotNull String ID() {
        return ID;
    }
    
    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
        
        // Register as memory listener
        MemoryManager.getInstance().addListener(this);
        
        // Schedule periodic updates
        updateTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            () -> {
                lastSnapshot = MemoryManager.getInstance().getCurrentSnapshot();
                if (statusBar != null) {
                    statusBar.updateWidget(ID());
                }
            },
            1, 5, TimeUnit.SECONDS
        );
        
        // Initial update
        lastSnapshot = MemoryManager.getInstance().getCurrentSnapshot();
    }
    
    @Override
    public void dispose() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        MemoryManager.getInstance().removeListener(this);
        statusBar = null;
    }
    
    @Override
    public Object getPresentation() {
        return this;
    }
    
    @Override
    public @NotNull String getText() {
        if (lastSnapshot == null) {
            return "Memory: N/A";
        }
        
        return String.format("Memory: %s MB / %s MB (%s%%)",
                          FORMAT.format(lastSnapshot.getUsedHeapMB()),
                          FORMAT.format(lastSnapshot.getMaxHeapMB()),
                          FORMAT.format(lastSnapshot.getUsagePercentage()));
    }
    
    @Override
    public @Nullable String getTooltipText() {
        if (lastSnapshot == null) {
            return "Memory information not available";
        }
        
        return String.format("<html><body>" +
                          "Heap Memory: %s MB / %s MB (%.1f%%)<br>" +
                          "Non-Heap Memory: %s MB / %s MB<br>" +
                          "Committed Heap: %s MB" +
                          "</body></html>",
                          FORMAT.format(lastSnapshot.getUsedHeapMB()),
                          FORMAT.format(lastSnapshot.getMaxHeapMB()),
                          lastSnapshot.getUsagePercentage(),
                          FORMAT.format(lastSnapshot.getUsedNonHeapMB()),
                          FORMAT.format(lastSnapshot.getMaxNonHeapMB()),
                          FORMAT.format(lastSnapshot.getCommittedHeap() / (1024.0 * 1024.0)));
    }
    
    @Override
    public float getAlignment() {
        return 0.0f;
    }
    
    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return e -> {
            if (e.getClickCount() == 1) {
                // Request a GC run on click
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    System.gc();
                    lastSnapshot = MemoryManager.getInstance().getCurrentSnapshot();
                    if (statusBar != null) {
                        statusBar.updateWidget(ID());
                    }
                });
            }
        };
    }
    
    @Override
    public void onWarningMemoryPressure(MemorySnapshot snapshot) {
        lastSnapshot = snapshot;
        if (statusBar != null) {
            ApplicationManager.getApplication().invokeLater(() -> statusBar.updateWidget(ID()));
        }
    }
    
    @Override
    public void onCriticalMemoryPressure(MemorySnapshot snapshot) {
        lastSnapshot = snapshot;
        if (statusBar != null) {
            ApplicationManager.getApplication().invokeLater(() -> statusBar.updateWidget(ID()));
        }
    }
    
    @Override
    public void onEmergencyMemoryPressure(MemorySnapshot snapshot) {
        lastSnapshot = snapshot;
        if (statusBar != null) {
            ApplicationManager.getApplication().invokeLater(() -> statusBar.updateWidget(ID()));
        }
    }
    
    @Override
    public void onNormalMemory(MemorySnapshot snapshot) {
        lastSnapshot = snapshot;
        if (statusBar != null) {
            ApplicationManager.getApplication().invokeLater(() -> statusBar.updateWidget(ID()));
        }
    }
}