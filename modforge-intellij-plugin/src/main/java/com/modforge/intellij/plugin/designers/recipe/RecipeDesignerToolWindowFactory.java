package com.modforge.intellij.plugin.designers.recipe;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.designers.recipe.ui.RecipeDesignerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the recipe designer tool window
 */
public class RecipeDesignerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        RecipeDesignerPanel designerPanel = new RecipeDesignerPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(designerPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}