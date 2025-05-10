package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

/**
 * Main project service for ModForge.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    private boolean initialized = false;
    
    public ModForgeProjectService(Project project) {
        this.project = project;
    }
    
    /**
     * Initialize the service, checking authentication and settings.
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        LOG.info("Initializing ModForge project service");
        
        // Check authentication
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (authManager.isAuthenticated()) {
            // Verify token
            boolean valid = authManager.verifyAuthentication();
            
            if (!valid) {
                LOG.warn("Authentication token is invalid");
                
                // If credentials are saved, try to re-authenticate
                ModForgeSettings settings = ModForgeSettings.getInstance();
                if (!settings.getUsername().isEmpty() && !settings.getPassword().isEmpty()) {
                    LOG.info("Trying to re-authenticate with saved credentials");
                    authManager.login(settings.getUsername(), settings.getPassword());
                }
            } else {
                LOG.info("Authentication token is valid");
            }
        }
        
        // Check if continuous development is enabled
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.isContinuousDevelopment()) {
            LOG.info("Continuous development is enabled, starting service");
            ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
            continuousService.start();
        }
        
        initialized = true;
    }
    
    /**
     * Check if the service is initialized.
     * @return Whether the service is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get the project.
     * @return Project
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Startup activity for ModForge.
     */
    public static class ProjectStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(Project project) {
            ModForgeProjectService service = project.getService(ModForgeProjectService.class);
            service.initialize();
        }
    }
}