package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Service for managing ModForge services for a project.
 * This service is responsible for starting and stopping other services.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    private final AutonomousCodeGenerationService codeGenService;
    private final ContinuousDevelopmentService continuousDevelopmentService;
    
    /**
     * Creates a new ModForgeProjectService.
     * @param project The project
     */
    public ModForgeProjectService(Project project) {
        this.project = project;
        this.codeGenService = project.getService(AutonomousCodeGenerationService.class);
        this.continuousDevelopmentService = project.getService(ContinuousDevelopmentService.class);
        
        LOG.info("ModForge project service created for project: " + project.getName());
        
        // Initialize services after project is fully loaded
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
            initializeServices();
        });
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
     * Initializes services.
     */
    private void initializeServices() {
        LOG.info("Initializing ModForge services for project: " + project.getName());
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Start continuous development if enabled
        if (settings.isContinuousDevelopmentEnabled()) {
            continuousDevelopmentService.start();
        }
    }
    
    /**
     * Called when the project is opened.
     */
    public void projectOpened() {
        LOG.info("Project opened: " + project.getName());
    }
    
    /**
     * Called when the project is closed.
     */
    public void projectClosed() {
        LOG.info("Project closed: " + project.getName());
        
        // Stop continuous development
        continuousDevelopmentService.stop();
    }
}