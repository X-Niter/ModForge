package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Utility for detecting Minecraft mod loaders.
 */
public class ModLoaderDetector {
    private static final Logger LOG = Logger.getInstance(ModLoaderDetector.class);
    
    /**
     * Enum representing different Minecraft mod loaders.
     */
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
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    /**
     * Detect the mod loader used in the project.
     *
     * @param project The project
     * @return The detected mod loader, or ModLoader.UNKNOWN if none is detected
     */
    public static ModLoader detectModLoader(@NotNull Project project) {
        LOG.info("Detecting mod loader for project " + project.getName());
        
        // Check for Architectury first (multi-loader)
        if (isArchitecturyProject(project)) {
            LOG.info("Detected Architectury in project " + project.getName());
            return ModLoader.ARCHITECTURY;
        }
        
        // Check for Forge
        if (isForgeProject(project)) {
            LOG.info("Detected Forge in project " + project.getName());
            return ModLoader.FORGE;
        }
        
        // Check for Fabric
        if (isFabricProject(project)) {
            LOG.info("Detected Fabric in project " + project.getName());
            return ModLoader.FABRIC;
        }
        
        // Check for Quilt
        if (isQuiltProject(project)) {
            LOG.info("Detected Quilt in project " + project.getName());
            return ModLoader.QUILT;
        }
        
        LOG.info("No mod loader detected in project " + project.getName());
        return ModLoader.UNKNOWN;
    }
    
    /**
     * Detect all mod loaders used in the project.
     *
     * @param project The project
     * @return A set of all detected mod loaders
     */
    public static Set<ModLoader> detectAllModLoaders(@NotNull Project project) {
        LOG.info("Detecting all mod loaders for project " + project.getName());
        
        Set<ModLoader> loaders = EnumSet.noneOf(ModLoader.class);
        
        // Check for Architectury (multi-loader)
        if (isArchitecturyProject(project)) {
            LOG.info("Detected Architectury in project " + project.getName());
            loaders.add(ModLoader.ARCHITECTURY);
        }
        
        // Check for Forge
        if (isForgeProject(project)) {
            LOG.info("Detected Forge in project " + project.getName());
            loaders.add(ModLoader.FORGE);
        }
        
        // Check for Fabric
        if (isFabricProject(project)) {
            LOG.info("Detected Fabric in project " + project.getName());
            loaders.add(ModLoader.FABRIC);
        }
        
        // Check for Quilt
        if (isQuiltProject(project)) {
            LOG.info("Detected Quilt in project " + project.getName());
            loaders.add(ModLoader.QUILT);
        }
        
        if (loaders.isEmpty()) {
            LOG.info("No mod loaders detected in project " + project.getName());
            loaders.add(ModLoader.UNKNOWN);
        }
        
        return Collections.unmodifiableSet(loaders);
    }
    
    /**
     * Check if the project uses Forge.
     *
     * @param project The project
     * @return True if the project uses Forge, false otherwise
     */
    private static boolean isForgeProject(@NotNull Project project) {
        // Check for forge config file
        VirtualFile forgeToml = findFileInProject(project, "forge.toml");
        if (forgeToml != null) {
            return true;
        }
        
        // Check for mods.toml with forge modid
        VirtualFile modsToml = findFileInProject(project, "mods.toml");
        if (modsToml != null) {
            try {
                String content = new String(modsToml.contentsToByteArray());
                return content.contains("modId") && content.contains("forge");
            } catch (Exception e) {
                LOG.warn("Error reading mods.toml", e);
            }
        }
        
        // Check for forge gradle plugin
        VirtualFile buildGradle = findFileInProject(project, "build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray());
                return content.contains("net.minecraftforge") || content.contains("forge-gradle");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle", e);
            }
        }
        
        // Check for forge dependencies in gradle
        VirtualFile buildGradleKts = findFileInProject(project, "build.gradle.kts");
        if (buildGradleKts != null) {
            try {
                String content = new String(buildGradleKts.contentsToByteArray());
                return content.contains("net.minecraftforge") || content.contains("forge-gradle");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle.kts", e);
            }
        }
        
        // Check for forge annotations in code
        List<VirtualFile> javaFiles = findFilesInProject(project, "java", 10);
        for (VirtualFile javaFile : javaFiles) {
            try {
                String content = new String(javaFile.contentsToByteArray());
                if (content.contains("@Mod") && content.contains("net.minecraftforge")) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn("Error reading Java file", e);
            }
        }
        
        return false;
    }
    
    /**
     * Check if the project uses Fabric.
     *
     * @param project The project
     * @return True if the project uses Fabric, false otherwise
     */
    private static boolean isFabricProject(@NotNull Project project) {
        // Check for fabric.mod.json
        VirtualFile fabricModJson = findFileInProject(project, "fabric.mod.json");
        if (fabricModJson != null) {
            return true;
        }
        
        // Check for fabric gradle plugin
        VirtualFile buildGradle = findFileInProject(project, "build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray());
                return content.contains("fabric-loom") || content.contains("fabricmc");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle", e);
            }
        }
        
        // Check for fabric dependencies in gradle
        VirtualFile buildGradleKts = findFileInProject(project, "build.gradle.kts");
        if (buildGradleKts != null) {
            try {
                String content = new String(buildGradleKts.contentsToByteArray());
                return content.contains("fabric-loom") || content.contains("fabricmc");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle.kts", e);
            }
        }
        
        // Check for fabric annotations in code
        List<VirtualFile> javaFiles = findFilesInProject(project, "java", 10);
        for (VirtualFile javaFile : javaFiles) {
            try {
                String content = new String(javaFile.contentsToByteArray());
                if (content.contains("implements ModInitializer") && content.contains("net.fabricmc")) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn("Error reading Java file", e);
            }
        }
        
        return false;
    }
    
    /**
     * Check if the project uses Quilt.
     *
     * @param project The project
     * @return True if the project uses Quilt, false otherwise
     */
    private static boolean isQuiltProject(@NotNull Project project) {
        // Check for quilt.mod.json
        VirtualFile quiltModJson = findFileInProject(project, "quilt.mod.json");
        if (quiltModJson != null) {
            return true;
        }
        
        // Check for quilt gradle plugin
        VirtualFile buildGradle = findFileInProject(project, "build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray());
                return content.contains("quilt-loom") || content.contains("quiltmc");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle", e);
            }
        }
        
        // Check for quilt dependencies in gradle
        VirtualFile buildGradleKts = findFileInProject(project, "build.gradle.kts");
        if (buildGradleKts != null) {
            try {
                String content = new String(buildGradleKts.contentsToByteArray());
                return content.contains("quilt-loom") || content.contains("quiltmc");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle.kts", e);
            }
        }
        
        // Check for quilt annotations in code
        List<VirtualFile> javaFiles = findFilesInProject(project, "java", 10);
        for (VirtualFile javaFile : javaFiles) {
            try {
                String content = new String(javaFile.contentsToByteArray());
                if (content.contains("QuiltLoader") || content.contains("org.quiltmc")) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn("Error reading Java file", e);
            }
        }
        
        return false;
    }
    
    /**
     * Check if the project uses Architectury.
     *
     * @param project The project
     * @return True if the project uses Architectury, false otherwise
     */
    private static boolean isArchitecturyProject(@NotNull Project project) {
        // Check for architectury gradle plugin
        VirtualFile buildGradle = findFileInProject(project, "build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray());
                return content.contains("architectury-plugin") || content.contains("dev.architectury");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle", e);
            }
        }
        
        // Check for architectury dependencies in gradle
        VirtualFile buildGradleKts = findFileInProject(project, "build.gradle.kts");
        if (buildGradleKts != null) {
            try {
                String content = new String(buildGradleKts.contentsToByteArray());
                return content.contains("architectury-plugin") || content.contains("dev.architectury");
            } catch (Exception e) {
                LOG.warn("Error reading build.gradle.kts", e);
            }
        }
        
        // Check for architectury imports in code
        List<VirtualFile> javaFiles = findFilesInProject(project, "java", 10);
        for (VirtualFile javaFile : javaFiles) {
            try {
                String content = new String(javaFile.contentsToByteArray());
                if (content.contains("import dev.architectury") || content.contains("Architectury.platform()")) {
                    return true;
                }
            } catch (Exception e) {
                LOG.warn("Error reading Java file", e);
            }
        }
        
        // Check for common, fabric, and forge folders
        VirtualFile common = findDirectoryInProject(project, "common");
        VirtualFile fabric = findDirectoryInProject(project, "fabric");
        VirtualFile forge = findDirectoryInProject(project, "forge");
        
        return common != null && (fabric != null || forge != null);
    }
    
    /**
     * Find a file in the project.
     *
     * @param project  The project
     * @param fileName The file name
     * @return The file, or null if not found
     */
    @Nullable
    private static VirtualFile findFileInProject(@NotNull Project project, @NotNull String fileName) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root : roots) {
            VirtualFile file = findFileRecursive(root, fileName, 5);
            if (file != null) {
                return file;
            }
        }
        return null;
    }
    
    /**
     * Find a directory in the project.
     *
     * @param project   The project
     * @param dirName   The directory name
     * @return The directory, or null if not found
     */
    @Nullable
    private static VirtualFile findDirectoryInProject(@NotNull Project project, @NotNull String dirName) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root : roots) {
            VirtualFile dir = findDirectoryRecursive(root, dirName, 3);
            if (dir != null) {
                return dir;
            }
        }
        return null;
    }
    
    /**
     * Find multiple files in the project with a specific extension.
     *
     * @param project   The project
     * @param extension The file extension
     * @param limit     The maximum number of files to return
     * @return A list of files
     */
    @NotNull
    private static List<VirtualFile> findFilesInProject(@NotNull Project project, @NotNull String extension, int limit) {
        List<VirtualFile> files = new ArrayList<>();
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root : roots) {
            findFilesWithExtensionRecursive(root, extension, files, limit, 5);
            if (files.size() >= limit) {
                break;
            }
        }
        return files;
    }
    
    /**
     * Find a file recursively.
     *
     * @param dir       The directory to search
     * @param fileName  The file name
     * @param maxDepth  The maximum recursion depth
     * @return The file, or null if not found
     */
    @Nullable
    private static VirtualFile findFileRecursive(@NotNull VirtualFile dir, @NotNull String fileName, int maxDepth) {
        if (!dir.isDirectory() || maxDepth <= 0) {
            return null;
        }
        
        for (VirtualFile child : dir.getChildren()) {
            if (!child.isDirectory() && fileName.equals(child.getName())) {
                return child;
            }
        }
        
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                VirtualFile file = findFileRecursive(child, fileName, maxDepth - 1);
                if (file != null) {
                    return file;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a directory recursively.
     *
     * @param dir       The directory to search
     * @param dirName   The directory name
     * @param maxDepth  The maximum recursion depth
     * @return The directory, or null if not found
     */
    @Nullable
    private static VirtualFile findDirectoryRecursive(@NotNull VirtualFile dir, @NotNull String dirName, int maxDepth) {
        if (!dir.isDirectory() || maxDepth <= 0) {
            return null;
        }
        
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                if (dirName.equals(child.getName())) {
                    return child;
                }
                
                VirtualFile subDir = findDirectoryRecursive(child, dirName, maxDepth - 1);
                if (subDir != null) {
                    return subDir;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find files with a specific extension recursively.
     *
     * @param dir       The directory to search
     * @param extension The file extension
     * @param files     The list to add files to
     * @param limit     The maximum number of files to find
     * @param maxDepth  The maximum recursion depth
     */
    private static void findFilesWithExtensionRecursive(
            @NotNull VirtualFile dir,
            @NotNull String extension,
            @NotNull List<VirtualFile> files,
            int limit,
            int maxDepth
    ) {
        if (!dir.isDirectory() || maxDepth <= 0 || files.size() >= limit) {
            return;
        }
        
        for (VirtualFile child : dir.getChildren()) {
            if (!child.isDirectory() && extension.equals(child.getExtension())) {
                files.add(child);
                if (files.size() >= limit) {
                    return;
                }
            }
        }
        
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                findFilesWithExtensionRecursive(child, extension, files, limit, maxDepth - 1);
                if (files.size() >= limit) {
                    return;
                }
            }
        }
    }
}