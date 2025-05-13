package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.modforge.intellij.plugin.model.ModLoaderType;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for Minecraft mod project operations.
 * This service provides operations specific to Minecraft mod projects.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    private ModLoaderType cachedModLoaderType = null;
    private String cachedMinecraftVersion = null;
    private final Map<String, String> modInfoCache = new HashMap<>();
    
    /**
     * Creates a new ModForge project service.
     *
     * @param project The project
     */
    public ModForgeProjectService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Gets the ModForge project service for a project.
     *
     * @param project The project
     * @return The ModForge project service
     */
    public static ModForgeProjectService getInstance(@NotNull Project project) {
        return project.getService(ModForgeProjectService.class);
    }
    
    /**
     * Checks if the project is a Minecraft mod project.
     *
     * @return True if the project is a Minecraft mod project, false otherwise
     */
    public boolean isModProject() {
        // Check for common mod files
        return hasBuildGradle() && (hasFabricModJson() || hasForgeToml() || hasQuiltModJson());
    }
    
    /**
     * Gets the mod loader type of the project.
     *
     * @return The mod loader type
     */
    @NotNull
    public ModLoaderType getModLoaderType() {
        if (cachedModLoaderType != null) {
            return cachedModLoaderType;
        }
        
        // Try to detect the mod loader type
        if (hasFabricModJson()) {
            cachedModLoaderType = ModLoaderType.FABRIC;
        } else if (hasForgeToml()) {
            cachedModLoaderType = ModLoaderType.FORGE;
        } else if (hasQuiltModJson()) {
            cachedModLoaderType = ModLoaderType.QUILT;
        } else if (hasArchitecturyCommon()) {
            cachedModLoaderType = ModLoaderType.ARCHITECTURY;
        } else {
            cachedModLoaderType = ModLoaderType.UNKNOWN;
        }
        
        return cachedModLoaderType;
    }
    
    /**
     * Gets the Minecraft version of the project.
     *
     * @return The Minecraft version, or an empty string if not found
     */
    @NotNull
    public String getMinecraftVersion() {
        if (cachedMinecraftVersion != null) {
            return cachedMinecraftVersion;
        }
        
        // Try to extract the version from the build.gradle file
        VirtualFile buildGradle = findFileInProject("build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray());
                Pattern pattern = Pattern.compile("minecraft[\\s]*?['\\\"](\\d+\\.\\d+(?:\\.\\d+)?)['\\\"]");
                Matcher matcher = pattern.matcher(content);
                
                if (matcher.find()) {
                    cachedMinecraftVersion = matcher.group(1);
                    return cachedMinecraftVersion;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read build.gradle", e);
            }
        }
        
        // Try to extract from gradle.properties
        VirtualFile gradleProperties = findFileInProject("gradle.properties");
        if (gradleProperties != null) {
            try {
                String content = new String(gradleProperties.contentsToByteArray());
                Pattern pattern = Pattern.compile("minecraft_version[\\s]*?=[\\s]*?(\\d+\\.\\d+(?:\\.\\d+)?)");
                Matcher matcher = pattern.matcher(content);
                
                if (matcher.find()) {
                    cachedMinecraftVersion = matcher.group(1);
                    return cachedMinecraftVersion;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read gradle.properties", e);
            }
        }
        
        // Try using CompatibilityUtil
        String basePath = project.getBasePath();
        if (basePath != null) {
            Path gradlePropertiesPath = Paths.get(basePath, "gradle.properties");
            if (Files.exists(gradlePropertiesPath)) {
                try {
                    String content = Files.readString(gradlePropertiesPath);
                    String version = CompatibilityUtil.extractVersionFromFile(content, "minecraft_version");
                    if (version != null && !version.isEmpty()) {
                        cachedMinecraftVersion = version;
                        return cachedMinecraftVersion;
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to read gradle.properties using compatibility util", e);
                }
            }
        }
        
        cachedMinecraftVersion = "";
        return cachedMinecraftVersion;
    }
    
    /**
     * Gets mod information from the project.
     *
     * @param key The key of the information
     * @return The information, or an empty string if not found
     */
    @NotNull
    public String getModInfo(@NotNull String key) {
        if (modInfoCache.containsKey(key)) {
            return modInfoCache.get(key);
        }
        
        // Try to extract from mod info files based on loader type
        ModLoaderType loaderType = getModLoaderType();
        String value = "";
        
        switch (loaderType) {
            case FABRIC:
                value = extractFromFabricModJson(key);
                break;
            case FORGE:
                value = extractFromForgeToml(key);
                break;
            case QUILT:
                value = extractFromQuiltModJson(key);
                break;
            case ARCHITECTURY:
                // Try both Fabric and Forge files
                value = extractFromFabricModJson(key);
                if (value.isEmpty()) {
                    value = extractFromForgeToml(key);
                }
                break;
            default:
                // Unknown loader type
                break;
        }
        
        modInfoCache.put(key, value);
        return value;
    }
    
    /**
     * Gets the mod ID of the project.
     *
     * @return The mod ID, or an empty string if not found
     */
    @NotNull
    public String getModId() {
        return getModInfo("modId");
    }
    
    /**
     * Gets the mod name of the project.
     *
     * @return The mod name, or an empty string if not found
     */
    @NotNull
    public String getModName() {
        return getModInfo("name");
    }
    
    /**
     * Gets the base package of the project.
     *
     * @return The base package, or an empty string if not found
     */
    @NotNull
    public String getBasePackage() {
        // First, try to read from saved configuration
        VirtualFile ideaDir = findFileInProject(".idea");
        if (ideaDir != null && ideaDir.isDirectory()) {
            VirtualFile modforgeConfig = ideaDir.findChild("modforge.xml");
            if (modforgeConfig != null) {
                try {
                    String content = new String(modforgeConfig.contentsToByteArray());
                    Pattern pattern = Pattern.compile("<basePackage>(.*?)</basePackage>");
                    Matcher matcher = pattern.matcher(content);
                    
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to read modforge.xml", e);
                }
            }
        }
        
        // Then try to infer from mod ID
        String modId = getModId();
        if (!modId.isEmpty()) {
            // Construct a package name from the mod ID
            // For example, "examplemod" -> "com.example.examplemod"
            if (!modId.contains(".")) {
                return "com.example." + modId;
            } else {
                return modId;
            }
        }
        
        return "";
    }
    
    /**
     * Checks if the project has a build.gradle file.
     *
     * @return True if the project has a build.gradle file, false otherwise
     */
    private boolean hasBuildGradle() {
        return findFileInProject("build.gradle") != null;
    }
    
    /**
     * Checks if the project has a fabric.mod.json file.
     *
     * @return True if the project has a fabric.mod.json file, false otherwise
     */
    private boolean hasFabricModJson() {
        return findFileInProject("fabric.mod.json") != null;
    }
    
    /**
     * Checks if the project has a mods.toml file.
     *
     * @return True if the project has a mods.toml file, false otherwise
     */
    private boolean hasForgeToml() {
        VirtualFile srcMainResources = findDirectoryInProject("src/main/resources");
        return srcMainResources != null && srcMainResources.findChild("META-INF") != null &&
               srcMainResources.findChild("META-INF").findChild("mods.toml") != null;
    }
    
    /**
     * Checks if the project has a quilt.mod.json file.
     *
     * @return True if the project has a quilt.mod.json file, false otherwise
     */
    private boolean hasQuiltModJson() {
        return findFileInProject("quilt.mod.json") != null;
    }
    
    /**
     * Checks if the project has Architectury common configuration.
     *
     * @return True if the project has Architectury configuration, false otherwise
     */
    private boolean hasArchitecturyCommon() {
        return findDirectoryInProject("common") != null || 
               findFileInProject("common/build.gradle") != null;
    }
    
    /**
     * Extracts information from a fabric.mod.json file.
     *
     * @param key The key of the information
     * @return The information, or an empty string if not found
     */
    @NotNull
    private String extractFromFabricModJson(@NotNull String key) {
        VirtualFile fabricModJson = findFileInProject("fabric.mod.json");
        if (fabricModJson != null) {
            try {
                String content = new String(fabricModJson.contentsToByteArray());
                
                // Map keys to JSON paths
                String jsonPath;
                switch (key) {
                    case "modId":
                        jsonPath = "\"id\"\\s*:\\s*\"([^\"]+)\"";
                        break;
                    case "name":
                        jsonPath = "\"name\"\\s*:\\s*\"([^\"]+)\"";
                        break;
                    case "version":
                        jsonPath = "\"version\"\\s*:\\s*\"([^\"]+)\"";
                        break;
                    default:
                        return "";
                }
                
                Pattern pattern = Pattern.compile(jsonPath);
                Matcher matcher = pattern.matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (IOException e) {
                LOG.warn("Failed to read fabric.mod.json", e);
            }
        }
        
        return "";
    }
    
    /**
     * Extracts information from a mods.toml file.
     *
     * @param key The key of the information
     * @return The information, or an empty string if not found
     */
    @NotNull
    private String extractFromForgeToml(@NotNull String key) {
        VirtualFile srcMainResources = findDirectoryInProject("src/main/resources");
        if (srcMainResources != null) {
            VirtualFile metaInf = srcMainResources.findChild("META-INF");
            if (metaInf != null) {
                VirtualFile modsToml = metaInf.findChild("mods.toml");
                if (modsToml != null) {
                    try {
                        String content = new String(modsToml.contentsToByteArray());
                        
                        // Map keys to TOML paths
                        String tomlPath;
                        switch (key) {
                            case "modId":
                                tomlPath = "modId\\s*=\\s*\"([^\"]+)\"";
                                break;
                            case "name":
                                tomlPath = "displayName\\s*=\\s*\"([^\"]+)\"";
                                break;
                            case "version":
                                tomlPath = "version\\s*=\\s*\"([^\"]+)\"";
                                break;
                            default:
                                return "";
                        }
                        
                        Pattern pattern = Pattern.compile(tomlPath);
                        Matcher matcher = pattern.matcher(content);
                        
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    } catch (IOException e) {
                        LOG.warn("Failed to read mods.toml", e);
                    }
                }
            }
        }
        
        return "";
    }
    
    /**
     * Extracts information from a quilt.mod.json file.
     *
     * @param key The key of the information
     * @return The information, or an empty string if not found
     */
    @NotNull
    private String extractFromQuiltModJson(@NotNull String key) {
        VirtualFile quiltModJson = findFileInProject("quilt.mod.json");
        if (quiltModJson != null) {
            try {
                String content = new String(quiltModJson.contentsToByteArray());
                
                // Map keys to JSON paths
                String jsonPath;
                switch (key) {
                    case "modId":
                        jsonPath = "\"id\"\\s*:\\s*\"([^\"]+)\"";
                        break;
                    case "name":
                        jsonPath = "\"name\"\\s*:\\s*\"([^\"]+)\"";
                        break;
                    case "version":
                        jsonPath = "\"version\"\\s*:\\s*\"([^\"]+)\"";
                        break;
                    default:
                        return "";
                }
                
                Pattern pattern = Pattern.compile(jsonPath);
                Matcher matcher = pattern.matcher(content);
                
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (IOException e) {
                LOG.warn("Failed to read quilt.mod.json", e);
            }
        }
        
        return "";
    }
    
    /**
     * Finds a file in the project by name.
     *
     * @param fileName The file name
     * @return The virtual file, or null if not found
     */
    @Nullable
    private VirtualFile findFileInProject(@NotNull String fileName) {
        Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project));
        return files.isEmpty() ? null : files.iterator().next();
    }
    
    /**
     * Finds a directory in the project by path.
     *
     * @param dirPath The directory path
     * @return The virtual file, or null if not found
     */
    @Nullable
    private VirtualFile findDirectoryInProject(@NotNull String dirPath) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            // Using CompatibilityUtil for better compatibility with IntelliJ IDEA 2025.1.1.1
            VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
            if (baseDir != null) {
                String[] parts = dirPath.split("/");
                VirtualFile current = baseDir;
                
                for (String part : parts) {
                    current = current.findChild(part);
                    if (current == null || !current.isDirectory()) {
                        return null;
                    }
                }
                
                return current;
            }
        }
        
        return null;
    }
}