package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action for showing the ModForge tool window.
 */
public class ShowToolWindowAction extends AnAction {
    private static final String TOOL_WINDOW_ID = "ModForge";

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only if a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get tool window manager
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        
        // Get tool window
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            // Show tool window
            toolWindow.show(null);
        }
    }
}