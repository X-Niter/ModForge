package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    
    public static final String TOOL_WINDOW_ID = "ModForge";
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ModForgeToolWindowPanel panel = new ModForgeToolWindowPanel(project, toolWindow);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "ModForge", false);
        toolWindow.getContentManager().addContent(content);
    }
}