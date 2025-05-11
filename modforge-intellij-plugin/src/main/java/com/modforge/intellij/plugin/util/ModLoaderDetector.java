package com.modforge.intellij.plugin.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for detecting the Minecraft mod loader used in a project.
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
    
    private static final Set<String> FORGE_INDICATORS = new HashSet<>(Arrays.asList(
            "minecraftforge", 
            "forge",
            "net.minecraftforge",
            "mods.toml"
    ));
    
    private static final Set<String> FABRIC_INDICATORS = new HashSet<>(Arrays.asList(
            "fabricmc",
            "fabric",
            "net.fabricmc",
            "fabric.mod.json"
    ));
    
    private static final Set<String> QUILT_INDICATORS = new HashSet<>(Arrays.asList(
            "quiltmc",
            "quilt",
            "org.quiltmc",
            "quilt.mod.json"
    ));
    
    private static final Set<String> ARCHITECTURY_INDICATORS = new HashSet<>(Arrays.asList(
            "architectury",
            "dev.architectury",
            "architectury.common"
    ));
    
    /**
     * Detects the mod loader used in the project.
     * @param project The project
     * @return The detected mod loader
     */
    @NotNull
    public static ModLoader detectModLoader(@NotNull Project project) {
        LOG.info("Detecting mod loader for project: " + project.getName());
        
        // Check for mod loader config files
        VirtualFile projectDir = CompatibilityUtil.getProjectBaseDir(project);
        
        // Check for Forge mods.toml
        if (hasFile(projectDir, "src/main/resources/META-INF/mods.toml")) {
            LOG.info("Detected Forge mod loader");
            return ModLoader.FORGE;
        }
        
        // Check for Fabric fabric.mod.json
        if (hasFile(projectDir, "src/main/resources/fabric.mod.json")) {
            LOG.info("Detected Fabric mod loader");
            return ModLoader.FABRIC;
        }
        
        // Check for Quilt quilt.mod.json
        if (hasFile(projectDir, "src/main/resources/quilt.mod.json")) {
            LOG.info("Detected Quilt mod loader");
            return ModLoader.QUILT;
        }
        
        // Check for multiple mod loaders (Architectury)
        boolean hasForge = hasForgeIndicators(projectDir);
        boolean hasFabric = hasFabricIndicators(projectDir);
        
        if (hasForge && hasFabric) {
            LOG.info("Detected multiple mod loaders, likely using Architectury");
            return ModLoader.ARCHITECTURY;
        }
        
        if (hasForge) {
            LOG.info("Detected Forge mod loader from indicators");
            return ModLoader.FORGE;
        }
        
        if (hasFabric) {
            LOG.info("Detected Fabric mod loader from indicators");
            return ModLoader.FABRIC;
        }
        
        // Look for build.gradle or build.gradle.kts indicators
        ModLoader gradleDetected = detectModLoaderFromGradle(projectDir);
        if (gradleDetected != ModLoader.UNKNOWN) {
            return gradleDetected;
        }
        
        LOG.info("Could not detect mod loader, assuming UNKNOWN");
        return ModLoader.UNKNOWN;
    }
    
    /**
     * Checks if the project has Forge indicators.
     * @param projectDir The project directory
     * @return True if Forge indicators are found, false otherwise
     */
    private static boolean hasForgeIndicators(@NotNull VirtualFile projectDir) {
        // Check import statements in java files
        return hasImportStatements(projectDir, FORGE_INDICATORS);
    }
    
    /**
     * Checks if the project has Fabric indicators.
     * @param projectDir The project directory
     * @return True if Fabric indicators are found, false otherwise
     */
    private static boolean hasFabricIndicators(@NotNull VirtualFile projectDir) {
        // Check import statements in java files
        return hasImportStatements(projectDir, FABRIC_INDICATORS);
    }
    
    /**
     * Checks if the project has import statements matching the given indicators.
     * @param projectDir The project directory
     * @param indicators The import indicators to check for
     * @return True if matching import statements are found, false otherwise
     */
    private static boolean hasImportStatements(@NotNull VirtualFile projectDir, @NotNull Set<String> indicators) {
        // Since we can't easily scan all java files without loading the actual PSI,
        // we'll check for common patterns in the directory structure
        
        for (String indicator : indicators) {
            // Check if there are directories matching the pattern
            if (hasDirectory(projectDir, "src/main/java/" + indicator.replace('.', '/'))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Detects the mod loader from Gradle build files.
     * @param projectDir The project directory
     * @return The detected mod loader
     */
    @NotNull
    private static ModLoader detectModLoaderFromGradle(@NotNull VirtualFile projectDir) {
        // Check build.gradle
        VirtualFile buildGradle = projectDir.findChild("build.gradle");
        
        if (buildGradle == null) {
            // Check build.gradle.kts
            buildGradle = projectDir.findChild("build.gradle.kts");
        }
        
        if (buildGradle == null) {
            return ModLoader.UNKNOWN;
        }
        
        try {
            String content = new String(buildGradle.contentsToByteArray());
            
            if (content.contains("net.minecraftforge") || 
                content.contains("minecraft.version") && content.contains("forge.version")) {
                LOG.info("Detected Forge mod loader from Gradle");
                return ModLoader.FORGE;
            }
            
            if (content.contains("net.fabricmc") || 
                content.contains("fabric-loader") || 
                content.contains("fabric.loom")) {
                LOG.info("Detected Fabric mod loader from Gradle");
                return ModLoader.FABRIC;
            }
            
            if (content.contains("org.quiltmc") || 
                content.contains("quilt-loader") || 
                content.contains("quilt.loom")) {
                LOG.info("Detected Quilt mod loader from Gradle");
                return ModLoader.QUILT;
            }
            
            if (content.contains("dev.architectury") || content.contains("architectury.common")) {
                LOG.info("Detected Architectury from Gradle");
                return ModLoader.ARCHITECTURY;
            }
        } catch (Exception e) {
            LOG.error("Error reading Gradle file", e);
        }
        
        return ModLoader.UNKNOWN;
    }
    
    /**
     * Checks if the directory has a file at the given path.
     * @param dir The directory
     * @param path The relative path to the file
     * @return True if the file exists, false otherwise
     */
    private static boolean hasFile(@NotNull VirtualFile dir, @NotNull String path) {
        VirtualFile file = dir.findFileByRelativePath(path);
        return file != null && file.exists() && !file.isDirectory();
    }
    
    /**
     * Checks if the directory has a subdirectory at the given path.
     * @param dir The directory
     * @param path The relative path to the subdirectory
     * @return True if the subdirectory exists, false otherwise
     */
    private static boolean hasDirectory(@NotNull VirtualFile dir, @NotNull String path) {
        VirtualFile subDir = dir.findFileByRelativePath(path);
        return subDir != null && subDir.exists() && subDir.isDirectory();
    }
    
    /**
     * Gets the Minecraft version from the project.
     * @param project The project
     * @return The Minecraft version or null if not found
     */
    @Nullable
    public static String detectMinecraftVersion(@NotNull Project project) {
        // Detect mod loader
        ModLoader modLoader = detectModLoader(project);
        
        // Get project directory
        VirtualFile projectDir = CompatibilityUtil.getProjectBaseDir(project);
        
        // Check for version in Gradle build files
        String gradleVersion = getMinecraftVersionFromGradle(projectDir);
        if (gradleVersion != null) {
            return gradleVersion;
        }
        
        // Check for version in mod configuration files
        switch (modLoader) {
            case FORGE:
                return getMinecraftVersionFromForgeConfig(project, projectDir);
            case FABRIC:
                return getMinecraftVersionFromFabricConfig(project, projectDir);
            case QUILT:
                return getMinecraftVersionFromQuiltConfig(project, projectDir);
            default:
                return null;
        }
    }
    
    /**
     * Gets the Minecraft version from Gradle build files.
     * @param projectDir The project directory
     * @return The Minecraft version or null if not found
     */
    @Nullable
    private static String getMinecraftVersionFromGradle(@NotNull VirtualFile projectDir) {
        // Check build.gradle
        VirtualFile buildGradle = projectDir.findChild("build.gradle");
        
        if (buildGradle == null) {
            // Check build.gradle.kts
            buildGradle = projectDir.findChild("build.gradle.kts");
        }
        
        if (buildGradle == null) {
            return null;
        }
        
        try {
            String content = new String(buildGradle.contentsToByteArray());
            
            // Try to match minecraft version
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "minecraft[\\s\\w.']*version[\\s\\w.']*=?[\\s\\w.']*'([^']+)'");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Try another pattern
            pattern = java.util.regex.Pattern.compile(
                    "minecraft[\\s\\w.']*version[\\s\\w.']*=?[\\s\\w.']*\"([^\"]+)\"");
            matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.error("Error reading Gradle file", e);
        }
        
        return null;
    }
    
    /**
     * Gets the Minecraft version from Forge configuration.
     * @param project The project
     * @param projectDir The project directory
     * @return The Minecraft version or null if not found
     */
    @Nullable
    private static String getMinecraftVersionFromForgeConfig(@NotNull Project project, @NotNull VirtualFile projectDir) {
        // Check for mods.toml
        VirtualFile modsToml = projectDir.findFileByRelativePath("src/main/resources/META-INF/mods.toml");
        
        if (modsToml == null) {
            return null;
        }
        
        try {
            String content = new String(modsToml.contentsToByteArray());
            
            // Try to match minecraft version
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "minecraft\\s*=\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.error("Error reading mods.toml", e);
        }
        
        return null;
    }
    
    /**
     * Gets the Minecraft version from Fabric configuration.
     * @param project The project
     * @param projectDir The project directory
     * @return The Minecraft version or null if not found
     */
    @Nullable
    private static String getMinecraftVersionFromFabricConfig(@NotNull Project project, @NotNull VirtualFile projectDir) {
        // Check for fabric.mod.json
        VirtualFile fabricModJson = projectDir.findFileByRelativePath("src/main/resources/fabric.mod.json");
        
        if (fabricModJson == null) {
            return null;
        }
        
        try {
            String content = new String(fabricModJson.contentsToByteArray());
            
            // Try to match minecraft version
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\"minecraft\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.error("Error reading fabric.mod.json", e);
        }
        
        return null;
    }
    
    /**
     * Gets the Minecraft version from Quilt configuration.
     * @param project The project
     * @param projectDir The project directory
     * @return The Minecraft version or null if not found
     */
    @Nullable
    private static String getMinecraftVersionFromQuiltConfig(@NotNull Project project, @NotNull VirtualFile projectDir) {
        // Check for quilt.mod.json
        VirtualFile quiltModJson = projectDir.findFileByRelativePath("src/main/resources/quilt.mod.json");
        
        if (quiltModJson == null) {
            return null;
        }
        
        try {
            String content = new String(quiltModJson.contentsToByteArray());
            
            // Try to match minecraft version
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\"minecraft\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.error("Error reading quilt.mod.json", e);
        }
        
        return null;
    }
}