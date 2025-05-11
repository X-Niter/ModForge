package com.modforge.intellij.plugin.designers.structure;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.designers.structure.ui.StructureDesignerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the structure designer tool window
 */
public class StructureDesignerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        StructureDesignerPanel designerPanel = new StructureDesignerPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(designerPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}