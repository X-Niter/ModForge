package com.modforge.intellij.plugin.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enum for different Minecraft mod loader types.
 * These loaders are used to develop mods for Minecraft.
 */
public enum ModLoaderType {
    /**
     * Forge mod loader.
     */
    FORGE("Forge", "forge", "build.gradle", "minecraftforge"),
    
    /**
     * Fabric mod loader.
     */
    FABRIC("Fabric", "fabric", "build.gradle", "fabric-loader"),
    
    /**
     * Quilt mod loader.
     */
    QUILT("Quilt", "quilt", "build.gradle", "quilt-loader"),
    
    /**
     * Architectury mod loader (cross-platform).
     */
    ARCHITECTURY("Architectury", "architectury", "build.gradle", "architectury-plugin"),
    
    /**
     * Unknown mod loader.
     */
    UNKNOWN("Unknown", "", "", "");
    
    private final String displayName;
    private final String id;
    private final String buildFile;
    private final String buildGradleIdentifier;
    
    /**
     * Creates a new mod loader type.
     *
     * @param displayName The display name
     * @param id The identifier
     * @param buildFile The build file name
     * @param buildGradleIdentifier The identifier in the build.gradle file
     */
    ModLoaderType(String displayName, String id, String buildFile, String buildGradleIdentifier) {
        this.displayName = displayName;
        this.id = id;
        this.buildFile = buildFile;
        this.buildGradleIdentifier = buildGradleIdentifier;
    }
    
    /**
     * Gets the display name of the mod loader.
     *
     * @return The display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the identifier of the mod loader.
     *
     * @return The identifier
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Gets the build file name of the mod loader.
     *
     * @return The build file name
     */
    @NotNull
    public String getBuildFile() {
        return buildFile;
    }
    
    /**
     * Gets the identifier in the build.gradle file.
     *
     * @return The identifier in the build.gradle file
     */
    @NotNull
    public String getBuildGradleIdentifier() {
        return buildGradleIdentifier;
    }
    
    /**
     * Gets a mod loader type by its identifier.
     *
     * @param id The identifier
     * @return The mod loader type, or UNKNOWN if not found
     */
    @NotNull
    public static ModLoaderType fromId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return UNKNOWN;
        }
        
        for (ModLoaderType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Detects the mod loader type from a project directory.
     *
     * @param projectDir The project directory
     * @return The detected mod loader type, or UNKNOWN if not detected
     */
    @NotNull
    public static ModLoaderType detectFromProject(@NotNull String projectDir) {
        // This would normally use file inspection logic to detect the mod loader
        // For simplicity, we'll just return UNKNOWN here
        return UNKNOWN;
    }
}