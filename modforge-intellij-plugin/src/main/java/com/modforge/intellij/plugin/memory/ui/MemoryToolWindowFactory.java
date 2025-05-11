package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the memory tool window
 */
public class MemoryToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MemoryInfoPanel memoryPanel = new MemoryInfoPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(memoryPanel, "", false);
        content.setDisposer(memoryPanel);
        toolWindow.getContentManager().addContent(content);
    }
}