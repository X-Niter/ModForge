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
        
        // Show tool window
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ModForge");
        
        if (toolWindow != null) {
            toolWindow.show();
        } else {
            LOG.error("Could not find ModForge tool window");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // Only enable if we have a project
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}