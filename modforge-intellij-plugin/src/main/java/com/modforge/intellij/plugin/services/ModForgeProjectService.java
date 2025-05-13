package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.model.ModLoaderType;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
            info.put("basePath", CompatibilityUtil.getProjectBasePath(project));
            info.put("isDefault", project.isDefault());
            
            // Add mod project info
            info.put("isModProject", isModProject());
            if (isModProject()) {
                info.put("modLoaderType", getModLoaderType().name());
                info.put("minecraftVersion", getMinecraftVersion());
            }
            
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
     * Checks if the current project is a Minecraft mod project.
     *
     * @return true if this is a mod project, false otherwise
     */
    public boolean isModProject() {
        try {
            // Check for common mod project files
            String basePath = CompatibilityUtil.getProjectBasePath(project);
            if (basePath == null) {
                return false;
            }
            
            // TODO: Implement more sophisticated detection of mod projects
            // This is a simplified implementation
            
            // Check for Forge
            if (CompatibilityUtil.fileExists(project, "build.gradle") && 
                (CompatibilityUtil.fileExists(project, "src/main/resources/META-INF/mods.toml") ||
                 CompatibilityUtil.fileContainsText(project, "build.gradle", "net.minecraftforge"))) {
                return true;
            }
            
            // Check for Fabric
            if (CompatibilityUtil.fileExists(project, "fabric.mod.json") ||
                CompatibilityUtil.fileContainsText(project, "build.gradle", "fabric-loom")) {
                return true;
            }
            
            // Check for Quilt
            if (CompatibilityUtil.fileExists(project, "quilt.mod.json") ||
                CompatibilityUtil.fileContainsText(project, "build.gradle", "org.quiltmc")) {
                return true;
            }
            
            // Check for Architectury
            if (CompatibilityUtil.fileContainsText(project, "build.gradle", "architectury-plugin")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOG.warn("Error detecting mod project", e);
            return false;
        }
    }
    
    /**
     * Gets the mod loader type for the current project.
     *
     * @return the mod loader type, or UNKNOWN if not a mod project
     */
    @NotNull
    public ModLoaderType getModLoaderType() {
        try {
            if (!isModProject()) {
                return ModLoaderType.UNKNOWN;
            }
            
            // Detect mod loader type
            if (CompatibilityUtil.fileExists(project, "src/main/resources/META-INF/mods.toml") ||
                CompatibilityUtil.fileContainsText(project, "build.gradle", "net.minecraftforge")) {
                return ModLoaderType.FORGE;
            }
            
            if (CompatibilityUtil.fileExists(project, "fabric.mod.json") ||
                CompatibilityUtil.fileContainsText(project, "build.gradle", "fabric-loom")) {
                return ModLoaderType.FABRIC;
            }
            
            if (CompatibilityUtil.fileExists(project, "quilt.mod.json") ||
                CompatibilityUtil.fileContainsText(project, "build.gradle", "org.quiltmc")) {
                return ModLoaderType.QUILT;
            }
            
            if (CompatibilityUtil.fileContainsText(project, "build.gradle", "architectury-plugin")) {
                return ModLoaderType.ARCHITECTURY;
            }
            
            return ModLoaderType.UNKNOWN;
        } catch (Exception e) {
            LOG.warn("Error detecting mod loader type", e);
            return ModLoaderType.UNKNOWN;
        }
    }
    
    /**
     * Gets the Minecraft version for the current project.
     *
     * @return the Minecraft version, or null if not a mod project or version could not be determined
     */
    @Nullable
    public String getMinecraftVersion() {
        try {
            if (!isModProject()) {
                return null;
            }
            
            // Extract Minecraft version from build.gradle
            // This is a simplified implementation
            String mcVersion = CompatibilityUtil.extractVersionFromFile(
                project, 
                "build.gradle", 
                "minecraft", 
                new String[] {"minecraft '", "minecraft \""}
            );
            
            if (mcVersion != null) {
                return mcVersion;
            }
            
            // Try to extract from gradle.properties
            mcVersion = CompatibilityUtil.extractVersionFromFile(
                project,
                "gradle.properties",
                "minecraft_version",
                new String[] {"minecraft_version="}
            );
            
            if (mcVersion != null) {
                return mcVersion;
            }
            
            // Try to extract from other common files based on the loader type
            switch (getModLoaderType()) {
                case FORGE:
                    mcVersion = CompatibilityUtil.extractVersionFromFile(
                        project,
                        "src/main/resources/META-INF/mods.toml",
                        "loaderVersion",
                        new String[] {"loaderVersion=\"["}
                    );
                    break;
                    
                case FABRIC:
                    mcVersion = CompatibilityUtil.extractVersionFromFile(
                        project,
                        "fabric.mod.json",
                        "minecraft",
                        new String[] {"\"minecraft\": \""}
                    );
                    break;
                    
                case QUILT:
                    mcVersion = CompatibilityUtil.extractVersionFromFile(
                        project,
                        "quilt.mod.json",
                        "minecraft",
                        new String[] {"\"minecraft\": \""}
                    );
                    break;
                    
                default:
                    // No additional extraction
                    break;
            }
            
            return mcVersion;
        } catch (Exception e) {
            LOG.warn("Error detecting Minecraft version", e);
            return null;
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