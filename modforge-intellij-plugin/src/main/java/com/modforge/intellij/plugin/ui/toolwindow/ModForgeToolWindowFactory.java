package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowFactory.class);
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("Creating ModForge tool window content");
        
        // Create tool window content
        ModForgeToolWindowContent toolWindowContent = new ModForgeToolWindowContent(project, toolWindow);
        
        // Create content for tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(toolWindowContent.getContent(), "", false);
        
        // Add content to tool window
        toolWindow.getContentManager().addContent(content);
    }
}