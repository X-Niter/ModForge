package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for project open and close events.
 * Initializes ModForge services when a project is opened and cleans up when a project is closed.
 */
public class ModForgeProjectListener implements ProjectManagerListener, StartupActivity {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectListener.class);

    @Override
    public void projectOpened(@NotNull Project project) {
        LOG.info("ModForge project opened: " + project.getName());
        
        // Initialize the continuous development service
        ContinuousDevelopmentService.getInstance(project);
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        LOG.info("ModForge project closed: " + project.getName());
        
        // Stop any continuous development tasks
        ContinuousDevelopmentService.getInstance(project).stopAllDevelopment();
    }

    @Override
    public void runActivity(@NotNull Project project) {
        // This is called when a project is opened during IDE startup
        LOG.info("ModForge startup activity for project: " + project.getName());
        
        // Initialize ModForge services
        projectOpened(project);
    }
}