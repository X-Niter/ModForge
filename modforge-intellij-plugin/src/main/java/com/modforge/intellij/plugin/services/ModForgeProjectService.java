package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.listeners.ModForgeFileListener;
import org.jetbrains.annotations.NotNull;

/**
 * Service that manages ModForge plugin services and listeners for a project.
 * This service is created when a project is opened and disposed when a project is closed.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    private final Project project;
    private ModForgeCompilationListener compilationListener;
    private ModForgeFileListener fileListener;
    
    /**
     * Creates a new ModForgeProjectService.
     * @param project The project
     */
    public ModForgeProjectService(Project project) {
        this.project = project;
        LOG.info("ModForge project service created for project: " + project.getName());
        
        // Initialize listeners
        initListeners();
    }
    
    /**
     * Gets the ModForge project service for a project.
     * @param project The project
     * @return The ModForge project service
     */
    public static ModForgeProjectService getInstance(@NotNull Project project) {
        return project.getService(ModForgeProjectService.class);
    }
    
    /**
     * Initializes listeners.
     */
    private void initListeners() {
        // Create compilation listener
        compilationListener = new ModForgeCompilationListener(project);
        
        // Create file listener
        fileListener = new ModForgeFileListener(project);
    }
    
    /**
     * Disposes the service.
     */
    public void dispose() {
        LOG.info("ModForge project service disposed for project: " + project.getName());
        
        // Dispose listeners
        if (compilationListener != null) {
            compilationListener.dispose();
            compilationListener = null;
        }
        
        if (fileListener != null) {
            fileListener.dispose();
            fileListener = null;
        }
    }
}