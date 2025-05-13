package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.modforge.intellij.plugin.model.ModLoaderType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for detecting whether a project is a Minecraft mod and which mod loader it uses.
 */
public class ModDetectionUtil {
    private static final Logger LOG = Logger.getInstance(ModDetectionUtil.class);
    
    // Patterns for detecting Minecraft version in build.gradle files
    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("minecraft\\s*{[^}]*version\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.DOTALL);
    private static final Pattern MC_VERSION_ALT_PATTERN = Pattern.compile("minecraft_version\\s*=\\s*['\"]([^'\"]+)['\"]");
    
    /**
     * Detects whether the specified project is a Minecraft mod and which mod loader it uses.
     * @param project The project to check
     * @return Information about the project
     */
    public static ProjectInfo detectModProject(@NotNull Project project) {
        LOG.info("Detecting mod project: " + project.getName());
        
        ProjectInfo info = new ProjectInfo();
        info.setModProject(false);
        info.setModLoaderType(ModLoaderType.UNKNOWN);
        
        try {
            // Check for Forge
            if (isForgeProject(project)) {
                info.setModProject(true);
                info.setModLoaderType(ModLoaderType.FORGE);
                info.setMinecraftVersion(detectMinecraftVersion(project));
                LOG.info("Detected Forge project: " + project.getName());
                return info;
            }
            
            // Check for NeoForge
            if (isNeoForgeProject(project)) {
                info.setModProject(true);
                info.setModLoaderType(ModLoaderType.NEOFORGE);
                info.setMinecraftVersion(detectMinecraftVersion(project));
                LOG.info("Detected NeoForge project: " + project.getName());
                return info;
            }
            
            // Check for Fabric
            if (isFabricProject(project)) {
                info.setModProject(true);
                info.setModLoaderType(ModLoaderType.FABRIC);
                info.setMinecraftVersion(detectMinecraftVersion(project));
                LOG.info("Detected Fabric project: " + project.getName());
                return info;
            }
            
            // Check for Quilt
            if (isQuiltProject(project)) {
                info.setModProject(true);
                info.setModLoaderType(ModLoaderType.QUILT);
                info.setMinecraftVersion(detectMinecraftVersion(project));
                LOG.info("Detected Quilt project: " + project.getName());
                return info;
            }
            
            // Check for Bukkit/Spigot
            if (isBukkitProject(project)) {
                info.setModProject(true);
                info.setModLoaderType(ModLoaderType.BUKKIT);
                info.setMinecraftVersion(detectMinecraftVersion(project));
                LOG.info("Detected Bukkit/Spigot project: " + project.getName());
                return info;
            }
            
            LOG.info("Not a Minecraft mod project: " + project.getName());
            return info;
        } catch (Exception e) {
            LOG.error("Error detecting mod project", e);
            return info;
        }
    }
    
    /**
     * Checks whether the specified project is a Forge mod.
     * @param project The project to check
     * @return Whether the project is a Forge mod
     */
    private static boolean isForgeProject(@NotNull Project project) {
        // Look for mods.toml file (Forge 1.13+)
        Collection<VirtualFile> modsTomlFiles = FilenameIndex.getVirtualFilesByName("mods.toml", GlobalSearchScope.projectScope(project));
        if (!modsTomlFiles.isEmpty()) {
            return true;
        }
        
        // Look for @Mod annotation in Java files
        return hasModAnnotation(project);
    }
    
    /**
     * Checks whether the specified project is a NeoForge mod.
     * @param project The project to check
     * @return Whether the project is a NeoForge mod
     */
    private static boolean isNeoForgeProject(@NotNull Project project) {
        // Check for NeoForge dependency in build.gradle
        Collection<VirtualFile> buildGradleFiles = FilenameIndex.getVirtualFilesByName("build.gradle", GlobalSearchScope.projectScope(project));
        for (VirtualFile buildGradleFile : buildGradleFiles) {
            try {
                String content = new String(buildGradleFile.contentsToByteArray());
                if (content.contains("neoforge") || content.contains("neoForge")) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle", e);
            }
        }
        
        return false;
    }
    
    /**
     * Checks whether the specified project is a Fabric mod.
     * @param project The project to check
     * @return Whether the project is a Fabric mod
     */
    private static boolean isFabricProject(@NotNull Project project) {
        // Look for fabric.mod.json file
        Collection<VirtualFile> fabricModJsonFiles = FilenameIndex.getVirtualFilesByName("fabric.mod.json", GlobalSearchScope.projectScope(project));
        return !fabricModJsonFiles.isEmpty();
    }
    
    /**
     * Checks whether the specified project is a Quilt mod.
     * @param project The project to check
     * @return Whether the project is a Quilt mod
     */
    private static boolean isQuiltProject(@NotNull Project project) {
        // Look for quilt.mod.json file
        Collection<VirtualFile> quiltModJsonFiles = FilenameIndex.getVirtualFilesByName("quilt.mod.json", GlobalSearchScope.projectScope(project));
        return !quiltModJsonFiles.isEmpty();
    }
    
    /**
     * Checks whether the specified project is a Bukkit/Spigot plugin.
     * @param project The project to check
     * @return Whether the project is a Bukkit/Spigot plugin
     */
    private static boolean isBukkitProject(@NotNull Project project) {
        // Look for plugin.yml file
        Collection<VirtualFile> pluginYmlFiles = FilenameIndex.getVirtualFilesByName("plugin.yml", GlobalSearchScope.projectScope(project));
        return !pluginYmlFiles.isEmpty();
    }
    
    /**
     * Checks whether the specified project has the @Mod annotation in any Java files.
     * @param project The project to check
     * @return Whether the project has the @Mod annotation
     */
    private static boolean hasModAnnotation(@NotNull Project project) {
        // This is a simplified check - in a real implementation, you would use PSI to find annotations
        // but this requires advanced Java PSI parsing which is beyond the scope of this example
        
        // For now, just check if any Java file contains "@Mod"
        Collection<VirtualFile> javaFiles = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));
        for (VirtualFile javaFile : javaFiles) {
            try {
                String content = new String(javaFile.contentsToByteArray());
                if (content.contains("@Mod")) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn("Error reading Java file", e);
            }
        }
        
        return false;
    }
    
    /**
     * Detects the Minecraft version of the specified project.
     * @param project The project to check
     * @return The Minecraft version, or null if it couldn't be detected
     */
    private static String detectMinecraftVersion(@NotNull Project project) {
        // Try to detect from build.gradle file
        Collection<VirtualFile> buildGradleFiles = FilenameIndex.getVirtualFilesByName("build.gradle", GlobalSearchScope.projectScope(project));
        for (VirtualFile buildGradleFile : buildGradleFiles) {
            try {
                String content = new String(buildGradleFile.contentsToByteArray());
                
                // Try to match minecraft version patterns
                Matcher matcher = MC_VERSION_PATTERN.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                matcher = MC_VERSION_ALT_PATTERN.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle", e);
            }
        }
        
        // Try to detect from gradle.properties file
        Collection<VirtualFile> gradlePropertiesFiles = FilenameIndex.getVirtualFilesByName("gradle.properties", GlobalSearchScope.projectScope(project));
        for (VirtualFile gradlePropertiesFile : gradlePropertiesFiles) {
            try {
                String content = new String(gradlePropertiesFile.contentsToByteArray());
                
                // Look for minecraft_version property
                Matcher matcher = MC_VERSION_ALT_PATTERN.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.warn("Error reading gradle.properties", e);
            }
        }
        
        // Couldn't detect, return null
        return null;
    }
    
    /**
     * Class containing information about a project.
     */
    public static class ProjectInfo {
        private boolean isModProject;
        private ModLoaderType modLoaderType;
        private String minecraftVersion;
        
        public boolean isModProject() {
            return isModProject;
        }
        
        public void setModProject(boolean modProject) {
            isModProject = modProject;
        }
        
        public ModLoaderType getModLoaderType() {
            return modLoaderType;
        }
        
        public void setModLoaderType(ModLoaderType modLoaderType) {
            this.modLoaderType = modLoaderType;
        }
        
        public String getMinecraftVersion() {
            return minecraftVersion;
        }
        
        public void setMinecraftVersion(String minecraftVersion) {
            this.minecraftVersion = minecraftVersion;
        }
    }
}