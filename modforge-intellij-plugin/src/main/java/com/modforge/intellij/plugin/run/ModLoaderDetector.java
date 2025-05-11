package com.modforge.intellij.plugin.run;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utility class to detect which Minecraft mod loader is being used in a project.
 * Supports Forge, Fabric, Quilt, and Architectury.
 */
public class ModLoaderDetector {
    
    private static final Logger LOG = Logger.getInstance(ModLoaderDetector.class);
    
    public enum ModLoader {
        FORGE("Forge"),
        FABRIC("Fabric"),
        QUILT("Quilt"),
        ARCHITECTURY("Architectury"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        ModLoader(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Detects which mod loader is being used in the project
     * 
     * @param project The IntelliJ project
     * @param module The module to check (can be null to check all modules)
     * @return The detected mod loader, or UNKNOWN if no mod loader is detected
     */
    public static ModLoader detectModLoader(@NotNull Project project, @Nullable Module module) {
        List<Module> modulesToCheck;
        
        if (module != null) {
            modulesToCheck = List.of(module);
        } else {
            modulesToCheck = Arrays.asList(com.intellij.openapi.module.ModuleManager.getInstance(project).getModules());
        }
        
        // Check each module
        for (Module m : modulesToCheck) {
            ModLoader loader = checkModuleForLoader(m);
            if (loader != ModLoader.UNKNOWN) {
                return loader;
            }
        }
        
        return ModLoader.UNKNOWN;
    }
    
    private static ModLoader checkModuleForLoader(@NotNull Module module) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        VirtualFile[] contentRoots = rootManager.getContentRoots();
        
        for (VirtualFile root : contentRoots) {
            // Check build.gradle or build.gradle.kts
            VirtualFile buildGradle = root.findChild("build.gradle");
            VirtualFile buildGradleKts = root.findChild("build.gradle.kts");
            
            if (buildGradle != null) {
                ModLoader loader = checkFileForLoaderPatterns(buildGradle);
                if (loader != ModLoader.UNKNOWN) {
                    return loader;
                }
            }
            
            if (buildGradleKts != null) {
                ModLoader loader = checkFileForLoaderPatterns(buildGradleKts);
                if (loader != ModLoader.UNKNOWN) {
                    return loader;
                }
            }
            
            // Check for specific mod loader files
            if (checkForFile(root, "src/main/resources/fabric.mod.json")) {
                return ModLoader.FABRIC;
            }
            
            if (checkForFile(root, "src/main/resources/quilt.mod.json")) {
                return ModLoader.QUILT;
            }
            
            if (checkForFile(root, "src/main/resources/META-INF/mods.toml")) {
                return ModLoader.FORGE;
            }
            
            if (checkForFile(root, "architectury.common.json")) {
                return ModLoader.ARCHITECTURY;
            }
        }
        
        return ModLoader.UNKNOWN;
    }
    
    private static ModLoader checkFileForLoaderPatterns(VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            
            // Check for Architectury first as it's a multi-loader setup
            if (content.contains("architectury-plugin") || content.contains("architectury.common")) {
                return ModLoader.ARCHITECTURY;
            }
            
            // Check for Forge
            if (content.contains("net.minecraftforge.gradle") || 
                content.contains("forge") && content.contains("minecraft")) {
                return ModLoader.FORGE;
            }
            
            // Check for Fabric
            if (content.contains("fabric-loom") || content.contains("fabricmc")) {
                return ModLoader.FABRIC;
            }
            
            // Check for Quilt
            if (content.contains("org.quiltmc") || content.contains("quilt-loom")) {
                return ModLoader.QUILT;
            }
            
        } catch (IOException e) {
            LOG.warn("Failed to read file content for mod loader detection", e);
        }
        
        return ModLoader.UNKNOWN;
    }
    
    private static boolean checkForFile(VirtualFile root, String relativePath) {
        String[] parts = relativePath.split("/");
        VirtualFile current = root;
        
        for (String part : parts) {
            current = current.findChild(part);
            if (current == null) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Detects the Minecraft version being used in the project
     * 
     * @param project The IntelliJ project
     * @return The detected Minecraft version, or null if not detected
     */
    @Nullable
    public static String detectMinecraftVersion(@NotNull Project project) {
        for (Module module : com.intellij.openapi.module.ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            for (VirtualFile root : rootManager.getContentRoots()) {
                VirtualFile buildGradle = root.findChild("build.gradle");
                VirtualFile buildGradleKts = root.findChild("build.gradle.kts");
                
                if (buildGradle != null) {
                    String version = extractMinecraftVersionFromFile(buildGradle);
                    if (version != null) {
                        return version;
                    }
                }
                
                if (buildGradleKts != null) {
                    String version = extractMinecraftVersionFromFile(buildGradleKts);
                    if (version != null) {
                        return version;
                    }
                }
                
                // Also check gradle.properties
                VirtualFile gradleProps = root.findChild("gradle.properties");
                if (gradleProps != null) {
                    String version = extractMinecraftVersionFromFile(gradleProps);
                    if (version != null) {
                        return version;
                    }
                }
            }
        }
        
        return null;
    }
    
    private static String extractMinecraftVersionFromFile(VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            
            // Look for minecraft version patterns like:
            // minecraft_version=1.19.2
            // minecraft '1.19.2'
            // minecraftVersion = '1.19.2'
            
            Pattern[] patterns = {
                Pattern.compile("minecraft[_\\s]*version[\\s='\"]+(\\d+\\.\\d+(?:\\.\\d+)?)"),
                Pattern.compile("minecraft[\\s]*['\"]+(\\d+\\.\\d+(?:\\.\\d+)?)['\"]"),
                Pattern.compile("minecraft_version[\\s='\"]+(\\d+\\.\\d+(?:\\.\\d+)?)"),
                Pattern.compile("mc_version[\\s='\"]+(\\d+\\.\\d+(?:\\.\\d+)?)")
            };
            
            for (Pattern pattern : patterns) {
                var matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
        } catch (IOException e) {
            LOG.warn("Failed to read file content for Minecraft version detection", e);
        }
        
        return null;
    }
}