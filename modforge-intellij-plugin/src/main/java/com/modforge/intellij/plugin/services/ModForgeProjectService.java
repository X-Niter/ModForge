package com.modforge.intellij.plugin.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Service for managing ModForge projects.
 */
public class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    private final Project project;
    
    /**
     * Constructor.
     * @param project The project
     */
    public ModForgeProjectService(Project project) {
        this.project = project;
        LOG.info("ModForgeProjectService created for project: " + project.getName());
    }
    
    /**
     * Initialize the service.
     */
    public void initialize() {
        LOG.info("Initializing ModForge project service for project: " + project.getName());
        
        // TODO: Initialize project service
    }
    
    /**
     * Activity that runs on project startup.
     */
    public static class ProjectStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            LOG.info("Running ModForge project startup activity for project: " + project.getName());
            
            // Get project service
            ModForgeProjectService projectService = project.getService(ModForgeProjectService.class);
            
            // Initialize service
            if (projectService != null) {
                projectService.initialize();
            }
        }
    }
}