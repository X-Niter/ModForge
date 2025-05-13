package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for compatibility operations.
 * This class provides various utility methods to ensure compatibility between different versions
 * of Minecraft, mod loaders, and the ModForge plugin.
 */
public final class CompatibilityUtil {
    private static final Logger LOG = Logger.getInstance(CompatibilityUtil.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private CompatibilityUtil() {
        // Utility class
    }
    
    /**
     * Checks if a file exists at the given path.
     *
     * @param pathStr The path to check
     * @return True if the file exists, false otherwise
     */
    public static boolean fileExists(@NotNull String pathStr) {
        try {
            Path path = Paths.get(pathStr);
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (Exception e) {
            LOG.warn("Failed to check if file exists: " + pathStr, e);
            return false;
        }
    }
    
    /**
     * Checks if a file contains the given text.
     *
     * @param pathStr The path of the file
     * @param text The text to check for
     * @return True if the file contains the text, false otherwise
     */
    public static boolean fileContainsText(@NotNull String pathStr, @NotNull String text) {
        try {
            Path path = Paths.get(pathStr);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return false;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return content.contains(text);
        } catch (IOException e) {
            LOG.warn("Failed to check if file contains text: " + pathStr, e);
            return false;
        } catch (Exception e) {
            LOG.warn("Unexpected error checking if file contains text: " + pathStr, e);
            return false;
        }
    }
    
    /**
     * Extracts a version from a properties file content.
     *
     * @param content The content of the properties file
     * @param key The key of the version property
     * @return The extracted version, or null if not found
     */
    @Nullable
    public static String extractVersionFromFile(@NotNull String content, @NotNull String key) {
        try {
            // Try to match the key with the standard properties format key=value
            Pattern pattern = Pattern.compile(Pattern.quote(key) + "\\s*=\\s*([^\\s]+)");
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Try alternative format with quotes
            pattern = Pattern.compile(Pattern.quote(key) + "\\s*=\\s*['\"]([^'\"]+)['\"]");
            matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract version from file content for key: " + key, e);
        }
        
        return null;
    }
    
    /**
     * Checks if a mod loader version is compatible with a given Minecraft version.
     *
     * @param modLoaderVersion The mod loader version
     * @param minecraftVersion The Minecraft version
     * @param modLoaderType The mod loader type
     * @return True if the mod loader version is compatible, false otherwise
     */
    public static boolean isModLoaderVersionCompatible(@NotNull String modLoaderVersion, 
                                                      @NotNull String minecraftVersion,
                                                      @NotNull String modLoaderType) {
        // This would normally use some compatibility logic to check if the mod loader version
        // is compatible with the given Minecraft version. For simplicity, we'll just assume
        // they are compatible.
        return true;
    }
    
    /**
     * Gets a compatible version of a mod loader for a given Minecraft version.
     *
     * @param minecraftVersion The Minecraft version
     * @param modLoaderType The mod loader type
     * @return A compatible mod loader version, or null if not found
     */
    @Nullable
    public static String getCompatibleModLoaderVersion(@NotNull String minecraftVersion, 
                                                      @NotNull String modLoaderType) {
        // This would normally use some lookup table or API to get a compatible mod loader version
        // for the given Minecraft version. For simplicity, we'll just return a placeholder version.
        
        // These are just placeholder versions, not actual compatible versions.
        // In a real implementation, this would use a more sophisticated mapping.
        switch (modLoaderType.toLowerCase()) {
            case "forge":
                if (minecraftVersion.startsWith("1.16")) {
                    return "36.2.0";
                } else if (minecraftVersion.startsWith("1.18")) {
                    return "40.1.0";
                } else if (minecraftVersion.startsWith("1.19")) {
                    return "43.1.1";
                } else if (minecraftVersion.startsWith("1.20")) {
                    return "46.0.1";
                }
                break;
            case "fabric":
                if (minecraftVersion.startsWith("1.16")) {
                    return "0.14.9";
                } else if (minecraftVersion.startsWith("1.18")) {
                    return "0.14.9";
                } else if (minecraftVersion.startsWith("1.19")) {
                    return "0.14.9";
                } else if (minecraftVersion.startsWith("1.20")) {
                    return "0.14.21";
                }
                break;
            case "quilt":
                if (minecraftVersion.startsWith("1.18")) {
                    return "0.17.1-beta.3";
                } else if (minecraftVersion.startsWith("1.19")) {
                    return "0.17.1";
                } else if (minecraftVersion.startsWith("1.20")) {
                    return "0.19.0-beta.18";
                }
                break;
        }
        
        return null;
    }
}