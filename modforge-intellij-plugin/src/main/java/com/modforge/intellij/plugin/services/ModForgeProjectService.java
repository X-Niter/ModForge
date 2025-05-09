package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.listeners.ModForgeFileListener;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Project-level service for ModForge.
 * This service manages other services and listeners for a project.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    private final AutonomousCodeGenerationService codeGenService;
    private final ContinuousDevelopmentService continuousDevService;
    private final ModForgeFileListener fileListener;
    private final ModForgeCompilationListener compilationListener;
    
    /**
     * Creates a new ModForgeProjectService.
     * @param project The project
     */
    public ModForgeProjectService(Project project) {
        this.project = project;
        this.codeGenService = project.getService(AutonomousCodeGenerationService.class);
        this.continuousDevService = project.getService(ContinuousDevelopmentService.class);
        this.fileListener = project.getService(ModForgeFileListener.class);
        this.compilationListener = new ModForgeCompilationListener(project);
        
        LOG.info("ModForge project service created for project: " + project.getName());
        
        // Register compilation listener
        compilationListener.register();
        
        // Start continuous development if enabled
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.isContinuousDevelopmentEnabled()) {
            continuousDevService.start();
        }
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
     * Handles project opened event.
     */
    public void projectOpened() {
        LOG.info("Project opened: " + project.getName());
    }
    
    /**
     * Handles project closed event.
     */
    public void projectClosed() {
        LOG.info("Project closed: " + project.getName());
        
        // Stop continuous development
        if (continuousDevService.isRunning()) {
            continuousDevService.stop();
        }
        
        // Unregister compilation listener
        compilationListener.unregister();
    }
}