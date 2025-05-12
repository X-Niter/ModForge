package com.modforge.intellij.plugin.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility class for detecting Minecraft mod loaders.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ModLoaderDetector {
    private static final Logger LOG = Logger.getInstance(ModLoaderDetector.class);
    
    // Mod loader types
    public static final String FORGE = "forge";
    public static final String FABRIC = "fabric";
    public static final String QUILT = "quilt";
    public static final String ARCHITECTURY = "architectury";
    public static final String UNKNOWN = "unknown";
    
    /**
     * Enum representing Minecraft mod loaders
     * This is referenced by several parts of the codebase
     */
    public enum ModLoader {
        FORGE("Forge", ModLoaderDetector.FORGE),
        FABRIC("Fabric", ModLoaderDetector.FABRIC),
        QUILT("Quilt", ModLoaderDetector.QUILT),
        ARCHITECTURY("Architectury", ModLoaderDetector.ARCHITECTURY),
        UNKNOWN("Unknown", ModLoaderDetector.UNKNOWN);
        
        private final String displayName;
        private final String id;
        
        ModLoader(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getId() {
            return id;
        }
        
        /**
         * Get ModLoader by ID
         * @param id The ID of the mod loader
         * @return The ModLoader enum value
         */
        public static ModLoader fromId(String id) {
            return Arrays.stream(values())
                .filter(loader -> loader.getId().equals(id))
                .findFirst()
                .orElse(UNKNOWN);
        }
        
        /**
         * Get all ModLoader values as a list of IDs
         * @return List of mod loader IDs
         */
        public static List<String> getAllLoaderIds() {
            return Arrays.stream(values())
                .map(ModLoader::getId)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ModLoaderDetector() {
        // Utility class
    }

    /**
     * Detects the mod loader type from a project.
     *
     * @param project The project.
     * @return The mod loader type.
     */
    @NotNull
    public static String detectModLoader(@NotNull Project project) {
        VirtualFile projectDir = CompatibilityUtil.getProjectBaseDir(project);
        if (projectDir == null) {
            LOG.warn("Failed to get project base directory");
            return UNKNOWN;
        }
        
        return ReadAction.compute(() -> {
            try {
                // Check for gradle files
                VirtualFile buildGradle = projectDir.findChild("build.gradle");
                VirtualFile buildGradleKts = projectDir.findChild("build.gradle.kts");
                
                if (buildGradle != null || buildGradleKts != null) {
                    String gradleContent = "";
                    
                    if (buildGradle != null) {
                        gradleContent = new String(buildGradle.contentsToByteArray());
                    } else if (buildGradleKts != null) {
                        gradleContent = new String(buildGradleKts.contentsToByteArray());
                    }
                    
                    if (gradleContent.contains("forge")) {
                        if (gradleContent.contains("architectury") || gradleContent.contains("dev.architectury")) {
                            return ARCHITECTURY;
                        }
                        return FORGE;
                    } else if (gradleContent.contains("fabric") || gradleContent.contains("net.fabricmc")) {
                        if (gradleContent.contains("architectury") || gradleContent.contains("dev.architectury")) {
                            return ARCHITECTURY;
                        }
                        return FABRIC;
                    } else if (gradleContent.contains("quilt") || gradleContent.contains("org.quiltmc")) {
                        if (gradleContent.contains("architectury") || gradleContent.contains("dev.architectury")) {
                            return ARCHITECTURY;
                        }
                        return QUILT;
                    }
                }
                
                // Check for mod files
                VirtualFile srcMainResources = findDirectory(projectDir, "src/main/resources");
                
                if (srcMainResources != null) {
                    VirtualFile metaInf = srcMainResources.findChild("META-INF");
                    
                    if (metaInf != null) {
                        VirtualFile modsToml = metaInf.findChild("mods.toml");
                        
                        if (modsToml != null) {
                            return FORGE;
                        }
                    }
                    
                    VirtualFile fabricModJson = srcMainResources.findChild("fabric.mod.json");
                    if (fabricModJson != null) {
                        return FABRIC;
                    }
                    
                    VirtualFile quiltModJson = srcMainResources.findChild("quilt.mod.json");
                    if (quiltModJson != null) {
                        return QUILT;
                    }
                }
                
                return UNKNOWN;
            } catch (Exception e) {
                LOG.error("Failed to detect mod loader", e);
                return UNKNOWN;
            }
        });
    }

    /**
     * Detects all mod loaders from a project.
     *
     * @param project The project.
     * @return A list of detected mod loaders.
     */
    @NotNull
    public static List<String> detectAllModLoaders(@NotNull Project project) {
        List<String> result = new ArrayList<>();
        
        VirtualFile projectDir = CompatibilityUtil.getProjectBaseDir(project);
        if (projectDir == null) {
            LOG.warn("Failed to get project base directory");
            return result;
        }
        
        return ReadAction.compute(() -> {
            try {
                // Check for gradle files
                VirtualFile buildGradle = projectDir.findChild("build.gradle");
                VirtualFile buildGradleKts = projectDir.findChild("build.gradle.kts");
                
                if (buildGradle != null || buildGradleKts != null) {
                    String gradleContent = "";
                    
                    if (buildGradle != null) {
                        gradleContent = new String(buildGradle.contentsToByteArray());
                    } else if (buildGradleKts != null) {
                        gradleContent = new String(buildGradleKts.contentsToByteArray());
                    }
                    
                    if (gradleContent.contains("forge")) {
                        result.add(FORGE);
                    }
                    
                    if (gradleContent.contains("fabric") || gradleContent.contains("net.fabricmc")) {
                        result.add(FABRIC);
                    }
                    
                    if (gradleContent.contains("quilt") || gradleContent.contains("org.quiltmc")) {
                        result.add(QUILT);
                    }
                    
                    if (gradleContent.contains("architectury") || gradleContent.contains("dev.architectury")) {
                        result.add(ARCHITECTURY);
                    }
                }
                
                // Check for mod files
                VirtualFile srcMainResources = findDirectory(projectDir, "src/main/resources");
                
                if (srcMainResources != null) {
                    VirtualFile metaInf = srcMainResources.findChild("META-INF");
                    
                    if (metaInf != null) {
                        VirtualFile modsToml = metaInf.findChild("mods.toml");
                        
                        if (modsToml != null && !result.contains(FORGE)) {
                            result.add(FORGE);
                        }
                    }
                    
                    VirtualFile fabricModJson = srcMainResources.findChild("fabric.mod.json");
                    if (fabricModJson != null && !result.contains(FABRIC)) {
                        result.add(FABRIC);
                    }
                    
                    VirtualFile quiltModJson = srcMainResources.findChild("quilt.mod.json");
                    if (quiltModJson != null && !result.contains(QUILT)) {
                        result.add(QUILT);
                    }
                }
                
                if (result.isEmpty()) {
                    result.add(UNKNOWN);
                }
                
                return result;
            } catch (Exception e) {
                LOG.error("Failed to detect mod loaders", e);
                result.add(UNKNOWN);
                return result;
            }
        });
    }

    /**
     * Finds a directory by its path.
     *
     * @param base The base directory.
     * @param path The path relative to the base directory.
     * @return The directory, or null if not found.
     */
    @Nullable
    private static VirtualFile findDirectory(@NotNull VirtualFile base, @NotNull String path) {
        return ReadAction.compute(() -> {
            try {
                VirtualFile dir = base.findFileByRelativePath(path);
                
                if (dir != null && dir.isDirectory()) {
                    return dir;
                }
                
                return null;
            } catch (Exception e) {
                LOG.error("Failed to find directory: " + path, e);
                return null;
            }
        });
    }
}