package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
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
        
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ModForge");
        
        if (toolWindow != null) {
            toolWindow.show(null);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable action if we have a project
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}