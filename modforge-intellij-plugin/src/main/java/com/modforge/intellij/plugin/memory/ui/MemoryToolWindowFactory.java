package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.memory.monitoring.ui.MemoryMonitorPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the memory tool window
 */
public class MemoryToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(MemoryToolWindowFactory.class);
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("Creating memory tool window for project " + project.getName());
        
        try {
            // Create the memory monitor panel
            MemoryMonitorPanel monitorPanel = new MemoryMonitorPanel(project);
            
            // Add content to the tool window
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content monitorContent = contentFactory.createContent(monitorPanel, "Memory Monitor", false);
            toolWindow.getContentManager().addContent(monitorContent);
            
            // Register the monitor panel for disposal
            toolWindow.getContentManager().addContentManagerListener(new ToolWindowDisposer(monitorPanel));
            
        } catch (Exception e) {
            LOG.error("Error creating memory tool window", e);
        }
    }
    
    /**
     * Disposer for the memory tool window
     */
    private static class ToolWindowDisposer extends com.intellij.ui.content.ContentManagerAdapter {
        private final MemoryMonitorPanel monitorPanel;
        
        ToolWindowDisposer(MemoryMonitorPanel monitorPanel) {
            this.monitorPanel = monitorPanel;
        }
        
        @Override
        public void contentRemoved(@NotNull com.intellij.ui.content.ContentManagerEvent event) {
            if (monitorPanel != null) {
                monitorPanel.dispose();
            }
        }
    }
}