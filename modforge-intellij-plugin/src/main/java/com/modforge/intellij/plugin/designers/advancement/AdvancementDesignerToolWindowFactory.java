package com.modforge.intellij.plugin.designers.advancement;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.designers.advancement.ui.AdvancementDesignerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the advancement designer tool window
 */
public class AdvancementDesignerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        AdvancementDesignerPanel designerPanel = new AdvancementDesignerPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(designerPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}