package com.modforge.intellij.plugin.run;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.modforge.intellij.plugin.run.ModLoaderDetector.ModLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Utility class for integration with the Minecraft Development plugin.
 * Uses reflection to avoid direct dependencies while still leveraging its capabilities.
 */
public class MinecraftIntegrationUtil {
    
    private static final Logger LOG = Logger.getInstance(MinecraftIntegrationUtil.class);
    
    /**
     * Attempts to detect if a project is a Minecraft project using the Minecraft Development plugin
     * 
     * @param project The project to check
     * @return True if this is a Minecraft project, false otherwise
     */
    public static boolean isMinecraftProject(@NotNull Project project) {
        try {
            // Try to access MinecraftModule.getInstance(project) via reflection
            Class<?> minecraftModuleClass = Class.forName("com.demonwav.minecraft.MinecraftModule");
            Method getInstanceMethod = minecraftModuleClass.getMethod("getInstance", Project.class);
            Object minecraftModule = getInstanceMethod.invoke(null, project);
            return minecraftModule != null;
        } catch (Exception e) {
            // If we can't access the Minecraft Development plugin, fall back to our own detection
            LOG.debug("Failed to detect Minecraft project via Minecraft Development plugin, falling back to manual detection", e);
            return detectMinecraftManually(project);
        }
    }
    
    /**
     * Attempts to detect the Minecraft version using the Minecraft Development plugin
     * 
     * @param project The project to check
     * @return The Minecraft version, or null if not detected
     */
    @Nullable
    public static String getMinecraftVersion(@NotNull Project project) {
        try {
            // Try to access MinecraftModule.getInstance(project).getMinecraftVersion() via reflection
            Class<?> minecraftModuleClass = Class.forName("com.demonwav.minecraft.MinecraftModule");
            Method getInstanceMethod = minecraftModuleClass.getMethod("getInstance", Project.class);
            Object minecraftModule = getInstanceMethod.invoke(null, project);
            
            if (minecraftModule != null) {
                Method getMinecraftVersionMethod = minecraftModuleClass.getMethod("getMinecraftVersion");
                Object version = getMinecraftVersionMethod.invoke(minecraftModule);
                return version != null ? version.toString() : null;
            }
        } catch (Exception e) {
            LOG.debug("Failed to get Minecraft version via Minecraft Development plugin, falling back to manual detection", e);
        }
        
        // Fall back to our own detection
        return ModLoaderDetector.detectMinecraftVersion(project);
    }
    
    /**
     * Attempts to detect Minecraft run configurations using the Minecraft Development plugin
     * 
     * @param project The project to check
     * @return True if Minecraft run configurations are already set up, false otherwise
     */
    public static boolean hasMinecraftRunConfigurations(@NotNull Project project) {
        try {
            // Try to access via reflection
            Class<?> runConfigClass = Class.forName("com.demonwav.minecraft.MinecraftRunConfiguration");
            
            // If we can load the class without exception, let's check if any run configurations exist
            com.intellij.execution.RunManager runManager = com.intellij.execution.RunManager.getInstance(project);
            return !runManager.getConfigurationsList(runConfigClass).isEmpty();
        } catch (Exception e) {
            LOG.debug("Failed to detect Minecraft run configurations via Minecraft Development plugin", e);
            return false;
        }
    }
    
    /**
     * Gets the Minecraft SDK for the module
     * 
     * @param module The module to check
     * @return The SDK, or null if not found
     */
    @Nullable
    public static Sdk getMinecraftSdk(@NotNull Module module) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        return rootManager.getSdk();
    }
    
    /**
     * Manual detection of Minecraft projects when the Minecraft Development plugin is not accessible
     * 
     * @param project The project to check
     * @return True if this appears to be a Minecraft project, false otherwise
     */
    private static boolean detectMinecraftManually(@NotNull Project project) {
        // Check for common Minecraft mod patterns in the project
        // Look for obvious signs like build.gradle with minecraft dependencies
        
        ModLoader loader = ModLoaderDetector.detectModLoader(project, null);
        return loader != ModLoader.UNKNOWN;
    }
    
    /**
     * Find the Minecraft assets directory for the project
     * 
     * @param project The project to check
     * @return Path to the assets directory, or empty if not found
     */
    public static Optional<String> findAssetsDirectory(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) return Optional.empty();
        
        // Common locations for Minecraft assets
        String[] commonPaths = {
            ".gradle/caches/forge_gradle/assets",
            ".gradle/caches/fabric-loom/assets",
            ".gradle/caches/quilt-loom/assets",
            "run/assets", 
            "build/resources/main/assets"
        };
        
        for (String path : commonPaths) {
            File assetsDir = new File(basePath, path);
            if (assetsDir.exists() && assetsDir.isDirectory()) {
                return Optional.of(assetsDir.getAbsolutePath());
            }
        }
        
        // Fallback to a broad search in the .gradle directory
        File gradleDir = new File(basePath, ".gradle");
        if (gradleDir.exists() && gradleDir.isDirectory()) {
            return findAssetsRecursively(gradleDir, 3);  // Limit depth to avoid excessive searching
        }
        
        return Optional.empty();
    }
    
    /**
     * Recursively search for an assets directory
     * 
     * @param dir The directory to start the search from
     * @param maxDepth Maximum recursion depth
     * @return Path to the assets directory, or empty if not found
     */
    private static Optional<String> findAssetsRecursively(File dir, int maxDepth) {
        if (maxDepth <= 0) return Optional.empty();
        
        File[] files = dir.listFiles();
        if (files == null) return Optional.empty();
        
        for (File file : files) {
            if (file.isDirectory()) {
                if ("assets".equals(file.getName())) {
                    return Optional.of(file.getAbsolutePath());
                }
                
                Optional<String> found = findAssetsRecursively(file, maxDepth - 1);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        
        return Optional.empty();
    }
}