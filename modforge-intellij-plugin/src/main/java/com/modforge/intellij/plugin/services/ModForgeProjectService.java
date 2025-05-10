package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

/**
 * Project service for ModForge.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    
    /**
     * Create a new ModForge project service.
     * @param project The project
     */
    public ModForgeProjectService(Project project) {
        this.project = project;
        
        LOG.info("ModForge project service created for project: " + project.getName());
        
        // Initialize services
        initializeServices();
    }
    
    /**
     * Initialize ModForge services.
     */
    private void initializeServices() {
        try {
            LOG.info("Initializing ModForge services");
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            
            // Check if continuous development is enabled
            if (settings.isContinuousDevelopment()) {
                LOG.info("Continuous development is enabled, starting service");
                
                // Start continuous development service
                ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
                if (continuousService != null) {
                    continuousService.start();
                } else {
                    LOG.error("ContinuousDevelopmentService is null");
                }
            }
        } catch (Exception e) {
            LOG.error("Error initializing ModForge services", e);
        }
    }
    
    /**
     * Get project info.
     * @return Project info as JSONObject
     */
    public JSONObject getProjectInfo() {
        try {
            JSONObject info = new JSONObject();
            
            // Add project info
            info.put("name", project.getName());
            info.put("basePath", project.getBasePath());
            info.put("isDefault", project.isDefault());
            
            // Add authentication info
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            info.put("authenticated", authManager.isAuthenticated());
            
            if (authManager.isAuthenticated()) {
                info.put("username", authManager.getUsername());
            }
            
            return info;
        } catch (Exception e) {
            LOG.error("Error getting project info", e);
            return new JSONObject();
        }
    }
    
    /**
     * Startup activity for ModForge.
     */
    public static class ModForgeStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            LOG.info("ModForge startup activity for project: " + project.getName());
            
            // Get project service (this will initialize it)
            project.getService(ModForgeProjectService.class);
        }
    }
}