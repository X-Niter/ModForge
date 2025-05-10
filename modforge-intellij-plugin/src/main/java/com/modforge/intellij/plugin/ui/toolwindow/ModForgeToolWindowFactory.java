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
        try {
            ModForgeToolWindowContent toolWindowContent = new ModForgeToolWindowContent(project, toolWindow);
            
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(toolWindowContent.getComponent(), "", false);
            toolWindow.getContentManager().addContent(content);
        } catch (Exception e) {
            LOG.error("Error creating ModForge tool window content", e);
        }
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle("ModForge");
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}