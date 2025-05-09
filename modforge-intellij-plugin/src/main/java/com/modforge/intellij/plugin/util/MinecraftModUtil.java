package com.modforge.intellij.plugin.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with Minecraft mods.
 */
public class MinecraftModUtil {
    private static final Logger LOG = Logger.getInstance(MinecraftModUtil.class);
    
    // Regex patterns for extracting mod information
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("MOD_ID\\s*=\\s*\"([^\"]+)\"|@Mod\\(\"([^\"]+)\"\\)|modId\\s*=\\s*\"([^\"]+)\"|\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MOD_NAME_PATTERN = Pattern.compile("MOD_NAME\\s*=\\s*\"([^\"]+)\"|displayName\\s*=\\s*\"([^\"]+)\"|name\\s*=\\s*\"([^\"]+)\"|\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MOD_VERSION_PATTERN = Pattern.compile("VERSION\\s*=\\s*\"([^\"]+)\"|version\\s*=\\s*\"([^\"]+)\"|\"version\"\\s*:\\s*\"([^\"]+)\"");
    
    /**
     * Gets the mod ID for the project.
     * @param project The project
     * @return The mod ID or null if not found
     */
    @Nullable
    public static String getModId(@NotNull Project project) {
        // Get the mod loader
        ModLoaderDetector.ModLoader modLoader = ModLoaderDetector.detectModLoader(project);
        
        // Get the project base directory
        VirtualFile baseDir = project.getBaseDir();
        
        // Try to find the mod ID based on the mod loader
        switch (modLoader) {
            case FORGE:
                return getForgeModId(project, baseDir);
            case FABRIC:
                return getFabricModId(project, baseDir);
            case QUILT:
                return getQuiltModId(project, baseDir);
            case ARCHITECTURY:
                // Try each loader in order
                String modId = getForgeModId(project, baseDir);
                if (modId != null) return modId;
                
                modId = getFabricModId(project, baseDir);
                if (modId != null) return modId;
                
                return getQuiltModId(project, baseDir);
            default:
                // Unknown mod loader, try to find any mod ID
                return findAnyModId(project, baseDir);
        }
    }
    
    /**
     * Gets the mod name for the project.
     * @param project The project
     * @return The mod name or null if not found
     */
    @Nullable
    public static String getModName(@NotNull Project project) {
        // Get the mod loader
        ModLoaderDetector.ModLoader modLoader = ModLoaderDetector.detectModLoader(project);
        
        // Get the project base directory
        VirtualFile baseDir = project.getBaseDir();
        
        // Try to find the mod name based on the mod loader
        switch (modLoader) {
            case FORGE:
                return getForgeModName(project, baseDir);
            case FABRIC:
                return getFabricModName(project, baseDir);
            case QUILT:
                return getQuiltModName(project, baseDir);
            case ARCHITECTURY:
                // Try each loader in order
                String modName = getForgeModName(project, baseDir);
                if (modName != null) return modName;
                
                modName = getFabricModName(project, baseDir);
                if (modName != null) return modName;
                
                return getQuiltModName(project, baseDir);
            default:
                // Unknown mod loader, try to find any mod name
                return findAnyModName(project, baseDir);
        }
    }
    
    /**
     * Gets the mod version for the project.
     * @param project The project
     * @return The mod version or null if not found
     */
    @Nullable
    public static String getModVersion(@NotNull Project project) {
        // Get the mod loader
        ModLoaderDetector.ModLoader modLoader = ModLoaderDetector.detectModLoader(project);
        
        // Get the project base directory
        VirtualFile baseDir = project.getBaseDir();
        
        // Try to find the mod version based on the mod loader
        switch (modLoader) {
            case FORGE:
                return getForgeModVersion(project, baseDir);
            case FABRIC:
                return getFabricModVersion(project, baseDir);
            case QUILT:
                return getQuiltModVersion(project, baseDir);
            case ARCHITECTURY:
                // Try each loader in order
                String modVersion = getForgeModVersion(project, baseDir);
                if (modVersion != null) return modVersion;
                
                modVersion = getFabricModVersion(project, baseDir);
                if (modVersion != null) return modVersion;
                
                return getQuiltModVersion(project, baseDir);
            default:
                // Unknown mod loader, try to find any mod version
                return findAnyModVersion(project, baseDir);
        }
    }
    
    /**
     * Gets the mod ID for a Forge mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod ID or null if not found
     */
    @Nullable
    private static String getForgeModId(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for mods.toml
        VirtualFile modsToml = baseDir.findFileByRelativePath("src/main/resources/META-INF/mods.toml");
        
        if (modsToml != null) {
            try {
                String content = new String(modsToml.contentsToByteArray());
                
                // Try to match mod ID
                Matcher matcher = Pattern.compile("modId\\s*=\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading mods.toml", e);
            }
        }
        
        // Check for @Mod annotation in Java files
        return findModAnnotation(project, baseDir);
    }
    
    /**
     * Gets the mod name for a Forge mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod name or null if not found
     */
    @Nullable
    private static String getForgeModName(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for mods.toml
        VirtualFile modsToml = baseDir.findFileByRelativePath("src/main/resources/META-INF/mods.toml");
        
        if (modsToml != null) {
            try {
                String content = new String(modsToml.contentsToByteArray());
                
                // Try to match mod name
                Matcher matcher = Pattern.compile("displayName\\s*=\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading mods.toml", e);
            }
        }
        
        return findAnyModName(project, baseDir);
    }
    
    /**
     * Gets the mod version for a Forge mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod version or null if not found
     */
    @Nullable
    private static String getForgeModVersion(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for mods.toml
        VirtualFile modsToml = baseDir.findFileByRelativePath("src/main/resources/META-INF/mods.toml");
        
        if (modsToml != null) {
            try {
                String content = new String(modsToml.contentsToByteArray());
                
                // Try to match mod version
                Matcher matcher = Pattern.compile("version\\s*=\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading mods.toml", e);
            }
        }
        
        return findAnyModVersion(project, baseDir);
    }
    
    /**
     * Gets the mod ID for a Fabric mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod ID or null if not found
     */
    @Nullable
    private static String getFabricModId(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for fabric.mod.json
        VirtualFile fabricModJson = baseDir.findFileByRelativePath("src/main/resources/fabric.mod.json");
        
        if (fabricModJson != null) {
            try {
                String content = new String(fabricModJson.contentsToByteArray());
                
                // Try to match mod ID
                Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading fabric.mod.json", e);
            }
        }
        
        return findAnyModId(project, baseDir);
    }
    
    /**
     * Gets the mod name for a Fabric mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod name or null if not found
     */
    @Nullable
    private static String getFabricModName(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for fabric.mod.json
        VirtualFile fabricModJson = baseDir.findFileByRelativePath("src/main/resources/fabric.mod.json");
        
        if (fabricModJson != null) {
            try {
                String content = new String(fabricModJson.contentsToByteArray());
                
                // Try to match mod name
                Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading fabric.mod.json", e);
            }
        }
        
        return findAnyModName(project, baseDir);
    }
    
    /**
     * Gets the mod version for a Fabric mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod version or null if not found
     */
    @Nullable
    private static String getFabricModVersion(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for fabric.mod.json
        VirtualFile fabricModJson = baseDir.findFileByRelativePath("src/main/resources/fabric.mod.json");
        
        if (fabricModJson != null) {
            try {
                String content = new String(fabricModJson.contentsToByteArray());
                
                // Try to match mod version
                Matcher matcher = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading fabric.mod.json", e);
            }
        }
        
        return findAnyModVersion(project, baseDir);
    }
    
    /**
     * Gets the mod ID for a Quilt mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod ID or null if not found
     */
    @Nullable
    private static String getQuiltModId(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for quilt.mod.json
        VirtualFile quiltModJson = baseDir.findFileByRelativePath("src/main/resources/quilt.mod.json");
        
        if (quiltModJson != null) {
            try {
                String content = new String(quiltModJson.contentsToByteArray());
                
                // Try to match mod ID
                Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading quilt.mod.json", e);
            }
        }
        
        return findAnyModId(project, baseDir);
    }
    
    /**
     * Gets the mod name for a Quilt mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod name or null if not found
     */
    @Nullable
    private static String getQuiltModName(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for quilt.mod.json
        VirtualFile quiltModJson = baseDir.findFileByRelativePath("src/main/resources/quilt.mod.json");
        
        if (quiltModJson != null) {
            try {
                String content = new String(quiltModJson.contentsToByteArray());
                
                // Try to match mod name
                Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading quilt.mod.json", e);
            }
        }
        
        return findAnyModName(project, baseDir);
    }
    
    /**
     * Gets the mod version for a Quilt mod.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod version or null if not found
     */
    @Nullable
    private static String getQuiltModVersion(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Check for quilt.mod.json
        VirtualFile quiltModJson = baseDir.findFileByRelativePath("src/main/resources/quilt.mod.json");
        
        if (quiltModJson != null) {
            try {
                String content = new String(quiltModJson.contentsToByteArray());
                
                // Try to match mod version
                Matcher matcher = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading quilt.mod.json", e);
            }
        }
        
        return findAnyModVersion(project, baseDir);
    }
    
    /**
     * Finds any mod ID in the project.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod ID or null if not found
     */
    @Nullable
    private static String findAnyModId(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Try to find the mod ID from the @Mod annotation
        String modId = findModAnnotation(project, baseDir);
        if (modId != null) {
            return modId;
        }
        
        // Try to find the mod ID from any Java file
        List<VirtualFile> javaFiles = findJavaFiles(baseDir);
        
        for (VirtualFile file : javaFiles) {
            try {
                String content = new String(file.contentsToByteArray());
                
                // Try to match mod ID
                Matcher matcher = MOD_ID_PATTERN.matcher(content);
                
                if (matcher.find()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (matcher.group(i) != null) {
                            return matcher.group(i);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error reading file: " + file.getPath(), e);
            }
        }
        
        return null;
    }
    
    /**
     * Finds any mod name in the project.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod name or null if not found
     */
    @Nullable
    private static String findAnyModName(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Try to find the mod name from any Java file
        List<VirtualFile> javaFiles = findJavaFiles(baseDir);
        
        for (VirtualFile file : javaFiles) {
            try {
                String content = new String(file.contentsToByteArray());
                
                // Try to match mod name
                Matcher matcher = MOD_NAME_PATTERN.matcher(content);
                
                if (matcher.find()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (matcher.group(i) != null) {
                            return matcher.group(i);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error reading file: " + file.getPath(), e);
            }
        }
        
        return null;
    }
    
    /**
     * Finds any mod version in the project.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod version or null if not found
     */
    @Nullable
    private static String findAnyModVersion(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Try to find the mod version from any Java file
        List<VirtualFile> javaFiles = findJavaFiles(baseDir);
        
        for (VirtualFile file : javaFiles) {
            try {
                String content = new String(file.contentsToByteArray());
                
                // Try to match mod version
                Matcher matcher = MOD_VERSION_PATTERN.matcher(content);
                
                if (matcher.find()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (matcher.group(i) != null) {
                            return matcher.group(i);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error reading file: " + file.getPath(), e);
            }
        }
        
        return null;
    }
    
    /**
     * Finds the mod ID from the @Mod annotation.
     * @param project The project
     * @param baseDir The project base directory
     * @return The mod ID or null if not found
     */
    @Nullable
    private static String findModAnnotation(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Try to find the mod ID from the @Mod annotation
        List<VirtualFile> javaFiles = findJavaFiles(baseDir);
        
        for (VirtualFile file : javaFiles) {
            try {
                String content = new String(file.contentsToByteArray());
                
                // Try to match @Mod annotation
                Matcher matcher = Pattern.compile("@Mod\\(\"([^\"]+)\"\\)").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                // Try to match @Mod annotation with value
                matcher = Pattern.compile("@Mod\\(value\\s*=\\s*\"([^\"]+)\"\\)").matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                LOG.error("Error reading file: " + file.getPath(), e);
            }
        }
        
        return null;
    }
    
    /**
     * Finds all Java files in the directory and subdirectories.
     * @param dir The directory
     * @return The list of Java files
     */
    @NotNull
    private static List<VirtualFile> findJavaFiles(@NotNull VirtualFile dir) {
        List<VirtualFile> javaFiles = new ArrayList<>();
        
        // Find all Java files in the directory
        findJavaFilesRecursive(dir, javaFiles);
        
        return javaFiles;
    }
    
    /**
     * Recursively finds all Java files in the directory and subdirectories.
     * @param dir The directory
     * @param javaFiles The list of Java files to populate
     */
    private static void findJavaFilesRecursive(@NotNull VirtualFile dir, @NotNull List<VirtualFile> javaFiles) {
        if (!dir.isDirectory()) {
            return;
        }
        
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                findJavaFilesRecursive(child, javaFiles);
            } else if (child.getExtension() != null && child.getExtension().equals("java")) {
                javaFiles.add(child);
            }
        }
    }
}