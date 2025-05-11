package com.modforge.intellij.plugin.memory.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Action to manually trigger memory optimization
 */
public class OptimizeMemoryAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Log current memory state
        MemoryUtils.logMemoryStats();
        
        // Perform optimization
        MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.AGGRESSIVE);
        
        // Log memory state after optimization
        MemoryUtils.logMemoryStats();
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}