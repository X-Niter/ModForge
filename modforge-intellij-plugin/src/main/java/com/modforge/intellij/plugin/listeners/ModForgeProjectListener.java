package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for project events.
 * This listener is used to detect when projects are opened or closed.
 */
public class ModForgeProjectListener implements StartupActivity, ProjectManagerListener {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectListener.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("ModForge project listener startup activity for project: " + project.getName());
        
        // Get project service
        ModForgeProjectService projectService = ModForgeProjectService.getInstance(project);
        
        // Call project opened
        projectService.projectOpened();
    }
    
    @Override
    public void projectOpened(@NotNull Project project) {
        LOG.info("Project opened: " + project.getName());
        
        // Get project service
        ModForgeProjectService projectService = ModForgeProjectService.getInstance(project);
        
        // Call project opened
        projectService.projectOpened();
    }
    
    @Override
    public void projectClosed(@NotNull Project project) {
        LOG.info("Project closed: " + project.getName());
        
        // Get project service
        ModForgeProjectService projectService = ModForgeProjectService.getInstance(project);
        
        // Call project closed
        projectService.projectClosed();
    }
}