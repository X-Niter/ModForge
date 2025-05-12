package com.modforge.intellij.plugin.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration.RunType;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that monitors and manages Minecraft run configurations.
 * Provides automatic configuration and maintenance of run configurations.
 * Implements StartupActivity to check configurations on project startup.
 */
public class MinecraftRunConfigurationService implements StartupActivity.DumbAware {
    
    private static final Logger LOG = Logger.getInstance(MinecraftRunConfigurationService.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        // Check if this is a Minecraft project
        if (!MinecraftIntegrationUtil.isMinecraftProject(project)) {
            LOG.debug("Not a Minecraft project, skipping run configuration setup");
            return;
        }
        
        // Only suggest configurations if none exist and this is a Minecraft project
        if (!hasModForgeRunConfigurations(project) && 
            !MinecraftIntegrationUtil.hasMinecraftRunConfigurations(project)) {
            
            suggestAutoConfiguration(project);
        }
        
        // Register file listener to detect changes to build.gradle/build.gradle.kts
        registerBuildFileListener(project);
    }
    
    /**
     * Check if there are any ModForge run configurations
     * 
     * @param project The project to check
     * @return True if ModForge run configurations exist, false otherwise
     */
    private boolean hasModForgeRunConfigurations(@NotNull Project project) {
        RunManager runManager = RunManager.getInstance(project);
        List<RunConfiguration> configs = runManager.getAllConfigurationsList();
        
        return configs.stream()
                .anyMatch(config -> config instanceof MinecraftRunConfiguration);
    }
    
    /**
     * Suggest automatic configuration with a notification
     * 
     * @param project The project to configure
     */
    private void suggestAutoConfiguration(@NotNull Project project) {
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        if (notificationService != null) {
            notificationService.showInfoWithAction(
                    "Minecraft Project Detected",
                    "ModForge can automatically configure run configurations for this Minecraft project.",
                    "Configure",
                    () -> new AutoConfigureMinecraftRunAction().actionPerformed(null)
            );
        }
    }
    
    /**
     * Register a listener for build file changes to update run configurations
     * 
     * @param project The project to monitor
     */
    private void registerBuildFileListener(@NotNull Project project) {
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                String fileName = event.getFileName();
                if ("build.gradle".equals(fileName) || "build.gradle.kts".equals(fileName) || 
                    "gradle.properties".equals(fileName)) {
                    
                    updateRunConfigurations(project);
                }
            }
        });
    }
    
    /**
     * Update run configurations based on project changes
     * 
     * @param project The project to update
     */
    private void updateRunConfigurations(@NotNull Project project) {
        // Only update if this is a Minecraft project
        if (!MinecraftIntegrationUtil.isMinecraftProject(project)) {
            return;
        }
        
        // Get all ModForge run configurations
        RunManager runManager = RunManager.getInstance(project);
        List<MinecraftRunConfiguration> configs = runManager.getAllConfigurationsList().stream()
                .filter(config -> config instanceof MinecraftRunConfiguration)
                .map(config -> (MinecraftRunConfiguration) config)
                .collect(Collectors.toList());
        
        if (configs.isEmpty()) {
            return;
        }
        
        // Update configurations with latest project information
        String mcVersion = MinecraftIntegrationUtil.getMinecraftVersion(project);
        ModLoaderDetector.ModLoader modLoader = ModLoaderDetector.detectModLoader(project, null);
        
        // Find main module
        Module mainModule = findMainModule(project);
        if (mainModule == null) {
            LOG.warn("Could not find main module for Minecraft project");
            return;
        }
        
        LOG.info("Updating Minecraft run configurations: " + 
                "version=" + (mcVersion != null ? mcVersion : "unknown") + 
                ", loader=" + modLoader.getDisplayName());
        
        // Update each configuration
        boolean configsChanged = false;
        for (MinecraftRunConfiguration config : configs) {
            boolean changed = updateRunConfiguration(config, mcVersion, modLoader, project);
            configsChanged = configsChanged || changed;
        }
        
        // Notify if configurations were updated
        if (configsChanged) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showInfo(
                        "Run Configurations Updated",
                        "Minecraft run configurations have been updated based on project changes."
                );
            }
        }
    }
    
    /**
     * Update a specific run configuration
     * 
     * @param config The configuration to update
     * @param mcVersion The detected Minecraft version
     * @param modLoader The detected mod loader from ModLoaderDetector
     * @param project The project
     * @return True if the configuration was changed, false otherwise
     */
    private boolean updateRunConfiguration(MinecraftRunConfiguration config, String mcVersion, 
                                          ModLoaderDetector.ModLoader modLoader, Project project) {
        boolean changed = false;
        
        // Update the asset index based on Minecraft version if available
        if (mcVersion != null) {
            String assetIndex = getMajorMinorVersion(mcVersion);
            String currentArgs = config.getProgramArgs();
            
            if (config.getRunType() == RunType.CLIENT && 
                    currentArgs.contains("--assetIndex") && 
                    !currentArgs.contains("--assetIndex " + assetIndex)) {
                
                // Extract the current asset index
                String oldAssetIndex = null;
                String[] args = currentArgs.split("\\s+");
                for (int i = 0; i < args.length - 1; i++) {
                    if ("--assetIndex".equals(args[i])) {
                        oldAssetIndex = args[i + 1];
                        break;
                    }
                }
                
                if (oldAssetIndex != null && !oldAssetIndex.equals(assetIndex)) {
                    // Replace the asset index
                    String newArgs = currentArgs.replace(
                            "--assetIndex " + oldAssetIndex,
                            "--assetIndex " + assetIndex
                    );
                    config.setProgramArgs(newArgs);
                    changed = true;
                    
                    LOG.info("Updated asset index in run configuration from " + 
                            oldAssetIndex + " to " + assetIndex);
                }
            }
        }
        
        // Additional updates can be added here as needed
        
        return changed;
    }
    
    /**
     * Get the major.minor version from a full version string
     * 
     * @param version The full version string (e.g., "1.19.2")
     * @return The major.minor version (e.g., "1.19")
     */
    private String getMajorMinorVersion(String version) {
        if (version == null) return "1.19";  // Default fallback
        
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        
        return version;
    }
    
    /**
     * Find the main Minecraft module in the project
     * 
     * @param project The project to search
     * @return The main module, or null if not found
     */
    private Module findMainModule(Project project) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        
        // If there's only one module, use that
        if (modules.length == 1) {
            return modules[0];
        }
        
        // Look for a module that matches typical Minecraft mod patterns
        for (Module module : modules) {
            String moduleName = module.getName().toLowerCase();
            if (moduleName.contains("mod") || moduleName.contains("minecraft") || 
                moduleName.contains("forge") || moduleName.contains("fabric")) {
                return module;
            }
        }
        
        // Default to the first module if we can't find a better match
        return modules.length > 0 ? modules[0] : null;
    }
}