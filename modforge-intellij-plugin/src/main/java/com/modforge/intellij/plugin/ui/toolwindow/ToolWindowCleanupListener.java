package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for tool window content manager events.
 * This listener cleans up resources when the tool window content is disposed.
 */
public final class ToolWindowCleanupListener implements ContentManagerListener {
    private static final Logger LOG = Logger.getInstance(ToolWindowCleanupListener.class);
    private final MetricsPanel metricsPanel;
    
    /**
     * Creates a new ToolWindowCleanupListener.
     * @param metricsPanel The metrics panel to clean up
     */
    public ToolWindowCleanupListener(@NotNull MetricsPanel metricsPanel) {
        this.metricsPanel = metricsPanel;
    }
    
    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
        // Do nothing
    }
    
    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
        Content content = event.getContent();
        
        if (content.getComponent() == metricsPanel.getContent()) {
            LOG.info("Metrics panel content removed, stopping refresh timer");
            metricsPanel.stopRefreshTimer();
        }
    }
    
    @Override
    public void contentRemoveQuery(@NotNull ContentManagerEvent event) {
        // Do nothing
    }
    
    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
        // Do nothing
    }
}