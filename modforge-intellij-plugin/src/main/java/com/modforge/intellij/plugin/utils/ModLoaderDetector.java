package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility for detecting Minecraft mod loaders.
 */
public class ModLoaderDetector {
    private static final Logger LOG = Logger.getInstance(ModLoaderDetector.class);
    
    // These patterns are used as a fallback when no contributors are available
    private static final Pattern FORGE_PATTERN = Pattern.compile("forge|minecraftforge", Pattern.CASE_INSENSITIVE);
    private static final Pattern FABRIC_PATTERN = Pattern.compile("fabric|fabricmc", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUILT_PATTERN = Pattern.compile("quilt|quiltmc", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARCHITECTURY_PATTERN = Pattern.compile("architectury", Pattern.CASE_INSENSITIVE);
    
    private final Project project;
    
    /**
     * Get a list of all available mod loaders from the registered contributors.
     *
     * @return A list of mod loader information
     */
    public static List<ModLoaderInfo> getAvailableModLoaders() {
        List<ModLoaderInfo> loaders = new ArrayList<>();
        
        for (ModLoaderContributor contributor : ModLoaderContributor.EP_NAME.getExtensionList()) {
            loaders.add(new ModLoaderInfo(
                    contributor.getModLoaderId(),
                    contributor.getModLoaderDisplayName(),
                    contributor.getSupportedMinecraftVersions(),
                    contributor.getTemplate() != null
            ));
        }
        
        // If no contributors are registered, add some default entries
        if (loaders.isEmpty()) {
            loaders.add(new ModLoaderInfo("forge", "Minecraft Forge", "1.7.10 - 1.20.4", false));
            loaders.add(new ModLoaderInfo("fabric", "Fabric Mod Loader", "1.14 - 1.20.4", false));
            loaders.add(new ModLoaderInfo("quilt", "Quilt Mod Loader", "1.18 - 1.20.4", false));
            loaders.add(new ModLoaderInfo("architectury", "Architectury (Multi-Loader)", "1.16 - 1.20.4", false));
        }
        
        return loaders;
    }
    
    /**
     * Get a specific mod loader contributor by ID.
     *
     * @param loaderId The mod loader ID
     * @return The contributor, or null if not found
     */
    @Nullable
    public static ModLoaderContributor getContributorById(String loaderId) {
        if (loaderId == null) {
            return null;
        }
        
        for (ModLoaderContributor contributor : ModLoaderContributor.EP_NAME.getExtensionList()) {
            if (loaderId.equals(contributor.getModLoaderId())) {
                return contributor;
            }
        }
        
        return null;
    }
    
    /**
     * Generate a GitHub workflow file for a specific mod loader.
     *
     * @param loaderId The mod loader ID
     * @return The workflow content, or null if no contributor found
     */
    @Nullable
    public static String generateWorkflowForLoader(String loaderId) {
        ModLoaderContributor contributor = getContributorById(loaderId);
        return contributor != null ? contributor.generateWorkflowContent() : null;
    }
    
    /**
     * Get all available templates from all mod loader contributors.
     *
     * @return A list of templates
     */
    @NotNull
    public static List<ModLoaderContributor.ModLoaderTemplate> getAvailableTemplates() {
        List<ModLoaderContributor.ModLoaderTemplate> templates = new ArrayList<>();
        
        for (ModLoaderContributor contributor : ModLoaderContributor.EP_NAME.getExtensionList()) {
            ModLoaderContributor.ModLoaderTemplate template = contributor.getTemplate();
            if (template != null) {
                templates.add(template);
            }
        }
        
        return templates;
    }
    
    /**
     * Get a template by ID.
     *
     * @param templateId The template ID
     * @return The template, or null if not found
     */
    @Nullable
    public static ModLoaderContributor.ModLoaderTemplate getTemplateById(String templateId) {
        if (templateId == null) {
            return null;
        }
        
        for (ModLoaderContributor contributor : ModLoaderContributor.EP_NAME.getExtensionList()) {
            ModLoaderContributor.ModLoaderTemplate template = contributor.getTemplate();
            if (template != null && templateId.equals(template.getId())) {
                return template;
            }
        }
        
        return null;
    }
    
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
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            return null;
        }
        
        // Try to detect using contributors first
        for (ModLoaderContributor contributor : ModLoaderContributor.EP_NAME.getExtensionList()) {
            try {
                if (contributor.detectModLoader(project, baseDir)) {
                    LOG.info("Detected mod loader '" + contributor.getModLoaderId() + 
                            "' using contributor: " + contributor.getClass().getSimpleName());
                    return contributor.getModLoaderId();
                }
            } catch (Exception e) {
                LOG.warn("Error detecting mod loader with contributor: " + contributor.getClass().getSimpleName(), e);
            }
        }
        
        // Fallback to built-in detection if no contributors were able to detect
        LOG.info("No mod loader detected via contributors, falling back to built-in detection");
        
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
        
        if (loader != null) {
            LOG.info("Detected mod loader '" + loader + "' using built-in detection");
        } else {
            LOG.info("No mod loader detected");
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