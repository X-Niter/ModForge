package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to show ModForge tool window.
 */
public class ShowToolWindowAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ShowToolWindowAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        LOG.info("Showing ModForge tool window");
        
        // Get tool window manager
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        
        // Get ModForge tool window
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ModForge");
        
        if (toolWindow != null) {
            // Show tool window
            toolWindow.show();
        } else {
            LOG.error("ModForge tool window not found");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if we have a project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}