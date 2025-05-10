package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Utility for detecting Minecraft mod loaders.
 */
public class ModLoaderDetector {
    private static final Logger LOG = Logger.getInstance(ModLoaderDetector.class);
    
    // Patterns for detecting mod loaders
    private static final Pattern FORGE_PATTERN = Pattern.compile("forge|minecraftforge", Pattern.CASE_INSENSITIVE);
    private static final Pattern FABRIC_PATTERN = Pattern.compile("fabric|fabricmc", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUILT_PATTERN = Pattern.compile("quilt|quiltmc", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARCHITECTURY_PATTERN = Pattern.compile("architectury", Pattern.CASE_INSENSITIVE);
    
    private final Project project;
    
    /**
     * Create a new mod loader detector.
     *
     * @param project The project
     */
    public ModLoaderDetector(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Detect the mod loader used by the project.
     *
     * @return The mod loader, or null if not detected
     */
    @Nullable
    public String detectModLoader() {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }
        
        // First, check for build.gradle or gradle.properties at the project root,
        // as these files often contain mod loader information
        VirtualFile buildGradle = baseDir.findChild("build.gradle");
        VirtualFile buildGradleKts = baseDir.findChild("build.gradle.kts");
        VirtualFile gradleProperties = baseDir.findChild("gradle.properties");
        VirtualFile settings = baseDir.findChild("settings.gradle");
        VirtualFile settingsKts = baseDir.findChild("settings.gradle.kts");
        
        String loader = null;
        
        if (buildGradle != null) {
            loader = detectFromFile(buildGradle);
        }
        
        if (loader == null && buildGradleKts != null) {
            loader = detectFromFile(buildGradleKts);
        }
        
        if (loader == null && gradleProperties != null) {
            loader = detectFromFile(gradleProperties);
        }
        
        if (loader == null && settings != null) {
            loader = detectFromFile(settings);
        }
        
        if (loader == null && settingsKts != null) {
            loader = detectFromFile(settingsKts);
        }
        
        // If we haven't found a loader yet, check for specific files that indicate mod loaders
        if (loader == null) {
            loader = detectFromProjectStructure(baseDir);
        }
        
        return loader;
    }
    
    /**
     * Detect mod loader from file content.
     *
     * @param file The file
     * @return The mod loader, or null if not detected
     */
    @Nullable
    private String detectFromFile(@NotNull VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            
            // Check for Architectury first, as it's a multi-loader system
            if (ARCHITECTURY_PATTERN.matcher(content).find()) {
                return "architectury";
            }
            
            // Then check for specific loaders
            if (FORGE_PATTERN.matcher(content).find()) {
                return "forge";
            }
            
            if (FABRIC_PATTERN.matcher(content).find()) {
                return "fabric";
            }
            
            if (QUILT_PATTERN.matcher(content).find()) {
                return "quilt";
            }
            
            return null;
        } catch (IOException e) {
            LOG.warn("Failed to read file: " + file.getPath(), e);
            return null;
        }
    }
    
    /**
     * Detect mod loader from project structure.
     *
     * @param baseDir The project base directory
     * @return The mod loader, or null if not detected
     */
    @Nullable
    private String detectFromProjectStructure(@NotNull VirtualFile baseDir) {
        // Look for Architectury structure (multiple modules)
        VirtualFile commonDir = baseDir.findChild("common");
        VirtualFile forgeDir = baseDir.findChild("forge");
        VirtualFile fabricDir = baseDir.findChild("fabric");
        VirtualFile quiltDir = baseDir.findChild("quilt");
        
        if ((commonDir != null && commonDir.isDirectory()) && 
                ((forgeDir != null && forgeDir.isDirectory()) || 
                (fabricDir != null && fabricDir.isDirectory()) || 
                (quiltDir != null && quiltDir.isDirectory()))) {
            return "architectury";
        }
        
        // Look for specific mod loader files
        
        // Forge: mods.toml
        VirtualFile srcMainResources = findDirectory(baseDir, "src/main/resources");
        if (srcMainResources != null) {
            VirtualFile metaInf = srcMainResources.findChild("META-INF");
            if (metaInf != null && metaInf.findChild("mods.toml") != null) {
                return "forge";
            }
        }
        
        // Fabric: fabric.mod.json
        if (srcMainResources != null && srcMainResources.findChild("fabric.mod.json") != null) {
            return "fabric";
        }
        
        // Quilt: quilt.mod.json
        if (srcMainResources != null && srcMainResources.findChild("quilt.mod.json") != null) {
            return "quilt";
        }
        
        return null;
    }
    
    /**
     * Find a directory by path.
     *
     * @param base The base directory
     * @param path The path
     * @return The directory, or null if not found
     */
    @Nullable
    private VirtualFile findDirectory(@NotNull VirtualFile base, @NotNull String path) {
        String[] parts = path.split("/");
        VirtualFile current = base;
        
        for (String part : parts) {
            current = current.findChild(part);
            if (current == null || !current.isDirectory()) {
                return null;
            }
        }
        
        return current;
    }
}