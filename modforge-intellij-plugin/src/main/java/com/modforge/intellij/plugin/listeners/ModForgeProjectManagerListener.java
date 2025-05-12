package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for project open and close events.
 * Initializes ModForge AI services when a project is opened.
 */
public class ModForgeProjectManagerListener implements ProjectManagerListener {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectManagerListener.class);

    /**
     * @deprecated This method is deprecated in IntelliJ IDEA 2025.1.1.1
     * Use {@link com.intellij.openapi.project.ProjectManager#TOPIC} with 
     * {@link com.intellij.openapi.project.ProjectManagerListener} instead
     */
    @Override
    @Deprecated
    public void projectOpened(@NotNull Project project) {
        // Call our compatibility method
        CompatibilityUtil.handleProjectOpened(project, p -> {
            // Initialize project-level services
            p.getService(ModForgeProjectService.class).projectOpened();
        });
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        LOG.info("ModForge AI: Project closed: " + project.getName());
        
        // Clean up project-level services
        project.getService(ModForgeProjectService.class).projectClosed();
    }
}