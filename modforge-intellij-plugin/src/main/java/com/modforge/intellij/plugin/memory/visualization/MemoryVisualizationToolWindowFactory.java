package com.modforge.intellij.plugin.memory.visualization;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool window factory for memory visualization
 */
public class MemoryVisualizationToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(MemoryVisualizationToolWindowFactory.class);
    private static final Map<Project, MemoryVisualizationPanel> projectPanels = 
            new ConcurrentHashMap<>(); // Thread-safe map for multi-threaded access
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (project.isDisposed()) {
            LOG.info("Project is already disposed, skipping memory visualization tool window creation");
            return;
        }
        
        try {
            // Check if panel already exists (prevent duplicate creation)
            MemoryVisualizationPanel visualizationPanel = projectPanels.get(project);
            
            // Create new panel if it doesn't exist
            if (visualizationPanel == null) {
                visualizationPanel = new MemoryVisualizationPanel(project);
                projectPanels.put(project, visualizationPanel);
                
                LOG.info("Created memory visualization panel for project: " + project.getName());
            } else {
                LOG.info("Reusing existing memory visualization panel for project: " + project.getName());
            }
            
            // Make sure content manager is available
            if (toolWindow.getContentManager() == null) {
                LOG.error("Content manager is null for memory visualization tool window");
                return;
            }
            
            // Create content
            ContentFactory contentFactory = ContentFactory.getInstance();
            if (contentFactory == null) {
                LOG.error("Content factory is null for memory visualization tool window");
                return;
            }
            
            Content content = contentFactory.createContent(visualizationPanel, "Memory Trends", false);
            content.setCloseable(false);
            toolWindow.getContentManager().addContent(content);
            
            // Register project disposal handler using ProjectManagerListener
            ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
                @Override
                public void projectClosing(@NotNull Project closingProject) {
                    try {
                        LOG.info("Project closing, disposing memory visualization panel: " + closingProject.getName());
                        disposePanel(closingProject);
                    } catch (Exception ex) {
                        LOG.error("Error disposing memory visualization panel", ex);
                    }
                }
            });
            
            LOG.info("Memory visualization tool window content created successfully for project: " + project.getName());
            
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
    private void disposePanel(@NotNull Project project) {
        try {
            MemoryVisualizationPanel panel = projectPanels.remove(project);
            if (panel != null) {
                LOG.info("Disposing memory visualization panel for project: " + project.getName());
                panel.dispose();
            } else {
                LOG.debug("No memory visualization panel found to dispose for project: " + project.getName());
            }
        } catch (Exception ex) {
            LOG.error("Error while disposing memory visualization panel for project: " + 
                    (project != null ? project.getName() : "null"), ex);
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