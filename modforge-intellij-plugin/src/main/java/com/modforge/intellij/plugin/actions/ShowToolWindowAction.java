package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action for showing the ModForge tool window.
 */
public class ShowToolWindowAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ShowToolWindowAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Show tool window action performed");
        
        // Get tool window manager
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        
        // Get ModForge tool window
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ModForge");
        
        if (toolWindow != null) {
            // Activate tool window
            toolWindow.show();
        } else {
            LOG.error("ModForge tool window not found");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        
        // Enable only if project is not null
        e.getPresentation().setEnabled(project != null);
    }
}