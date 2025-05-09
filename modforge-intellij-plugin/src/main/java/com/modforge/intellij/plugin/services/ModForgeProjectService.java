package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.models.ModLoaderType;
import com.modforge.intellij.plugin.utils.ModDetectionUtil;
import com.modforge.intellij.plugin.utils.PatternStorageUtil;

/**
 * Project-level service for ModForge AI.
 * Manages project-specific functionality and state.
 */
@Service
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    private ModLoaderType detectedModLoader;
    private boolean isModProject = false;
    private String minecraftVersion;
    
    // Local pattern storage for this project
    private PatternStorageUtil patternStorage;
    
    // Sync status
    private boolean syncEnabled = true;
    private long lastSyncTimestamp = 0;
    
    public ModForgeProjectService(Project project) {
        this.project = project;
        this.patternStorage = new PatternStorageUtil(project);
    }
    
    /**
     * Called when the project is opened.
     * Detects project type and initializes services.
     */
    public void projectOpened() {
        LOG.info("Initializing ModForge AI for project: " + project.getName());
        
        // Detect if this is a Minecraft mod project and which mod loader it uses
        detectProjectType();
        
        // Initialize pattern storage and load local patterns
        patternStorage.initialize();
        
        // Check for sync with web platform if enabled
        if (syncEnabled) {
            syncWithWebPlatform();
        }
    }
    
    /**
     * Called when the project is closed.
     * Performs cleanup and ensures data is saved.
     */
    public void projectClosed() {
        LOG.info("Shutting down ModForge AI for project: " + project.getName());
        
        // Save any unsaved patterns
        patternStorage.savePatterns();
        
        // Final sync with web platform if enabled
        if (syncEnabled) {
            syncWithWebPlatform();
        }
    }
    
    /**
     * Detects if this is a Minecraft mod project and which mod loader it uses.
     */
    private void detectProjectType() {
        ModDetectionUtil.ProjectInfo info = ModDetectionUtil.detectModProject(project);
        this.isModProject = info.isModProject();
        this.detectedModLoader = info.getModLoaderType();
        this.minecraftVersion = info.getMinecraftVersion();
        
        LOG.info("Project detection results: " +
                "Is mod project: " + isModProject + ", " +
                "Mod loader: " + detectedModLoader + ", " +
                "Minecraft version: " + minecraftVersion);
    }
    
    /**
     * Synchronizes local patterns with the web platform.
     */
    public void syncWithWebPlatform() {
        if (!syncEnabled) {
            LOG.info("Sync is disabled for project: " + project.getName());
            return;
        }
        
        try {
            LOG.info("Syncing patterns with web platform for project: " + project.getName());
            
            // Get API service from application-level service
            AIServiceManager aiServiceManager = project.getService(AIServiceManager.class);
            
            // Upload local patterns that are new or modified
            aiServiceManager.uploadPatterns(patternStorage.getModifiedPatterns());
            
            // Download new patterns from the platform
            patternStorage.mergePatterns(aiServiceManager.downloadLatestPatterns(lastSyncTimestamp));
            
            // Update sync timestamp
            lastSyncTimestamp = System.currentTimeMillis();
            
            LOG.info("Sync completed successfully");
        } catch (Exception e) {
            LOG.error("Error syncing with web platform", e);
        }
    }
    
    /**
     * Toggles sync with web platform.
     */
    public void setSyncEnabled(boolean enabled) {
        this.syncEnabled = enabled;
        if (enabled && lastSyncTimestamp == 0) {
            // Initial sync if it was previously disabled
            syncWithWebPlatform();
        }
    }
    
    /**
     * Returns whether this is a Minecraft mod project.
     */
    public boolean isModProject() {
        return isModProject;
    }
    
    /**
     * Returns the detected mod loader type.
     */
    public ModLoaderType getModLoaderType() {
        return detectedModLoader;
    }
    
    /**
     * Returns the detected Minecraft version.
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    /**
     * Returns the pattern storage utility.
     */
    public PatternStorageUtil getPatternStorage() {
        return patternStorage;
    }
}