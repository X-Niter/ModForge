package com.modforge.intellij.plugin.memory.visualization;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool window factory for memory visualization
 */
public class MemoryVisualizationToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(MemoryVisualizationToolWindowFactory.class);
    private static final Map<Project, MemoryVisualizationPanel> projectPanels = new HashMap<>();
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            // Create memory visualization panel
            MemoryVisualizationPanel visualizationPanel = new MemoryVisualizationPanel(project);
            projectPanels.put(project, visualizationPanel);
            
            // Create content
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(visualizationPanel, "Memory Trends", false);
            toolWindow.getContentManager().addContent(content);
            
            // Register project disposal handler
            project.getMessageBus().connect().subscribe(Project.TOPIC, new Project.ProjectListener() {
                @Override
                public void projectClosing(@NotNull Project project) {
                    disposePanel(project);
                }
            });
            
        } catch (Exception e) {
            LOG.error("Error creating memory visualization tool window", e);
        }
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        // Set tool window icon
        toolWindow.setIcon(com.intellij.openapi.util.IconLoader.getIcon("/icons/memory.svg", getClass()));
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
    
    /**
     * Dispose the visualization panel for a project
     * 
     * @param project The project
     */
    private void disposePanel(Project project) {
        MemoryVisualizationPanel panel = projectPanels.remove(project);
        if (panel != null) {
            panel.dispose();
        }
    }
    
    /**
     * Get the visualization panel for a project
     * 
     * @param project The project
     * @return The visualization panel, or null if not found
     */
    public static MemoryVisualizationPanel getVisualizationPanel(Project project) {
        return projectPanels.get(project);
    }
}