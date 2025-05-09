package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.modforge.intellij.plugin.ui.toolwindow.ModForgeToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Action to show the ModForge tool window.
 */
public class ShowToolWindowAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(ModForgeToolWindowFactory.TOOL_WINDOW_ID);
        
        if (toolWindow != null) {
            toolWindow.show();
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}