package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for project open and close events.
 * Initializes ModForge AI services when a project is opened.
 */
public class ModForgeProjectManagerListener implements ProjectManagerListener {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectManagerListener.class);

    @Override
    public void projectOpened(@NotNull Project project) {
        LOG.info("ModForge AI: Project opened: " + project.getName());
        
        // Initialize project-level services
        project.getService(ModForgeProjectService.class).projectOpened();
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        LOG.info("ModForge AI: Project closed: " + project.getName());
        
        // Clean up project-level services
        project.getService(ModForgeProjectService.class).projectClosed();
    }
}