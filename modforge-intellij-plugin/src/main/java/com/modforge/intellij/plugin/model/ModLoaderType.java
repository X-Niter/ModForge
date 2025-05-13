package com.modforge.intellij.plugin.model;

import org.jetbrains.annotations.NotNull;

/**
 * Enum for Minecraft mod loader types.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public enum ModLoaderType {
    /**
     * Forge mod loader.
     */
    FORGE("Forge"),
    
    /**
     * Fabric mod loader.
     */
    FABRIC("Fabric"),
    
    /**
     * Quilt mod loader.
     */
    QUILT("Quilt"),
    
    /**
     * Architectury mod loader (multi-loader).
     */
    ARCHITECTURY("Architectury"),
    
    /**
     * Unknown mod loader.
     */
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    ModLoaderType(String displayName) {
        this.displayName = displayName;
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
     * Gets the mod loader type from a display name.
     *
     * @param displayName The display name
     * @return The mod loader type
     */
    @NotNull
    public static ModLoaderType fromDisplayName(@NotNull String displayName) {
        for (ModLoaderType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        return UNKNOWN;
    }
    
    /**
     * Gets the default file extension for mods of this type.
     *
     * @return The default file extension
     */
    @NotNull
    public String getDefaultFileExtension() {
        switch (this) {
            case FORGE:
            case FABRIC:
            case QUILT:
            case ARCHITECTURY:
                return ".jar";
            default:
                return ".jar";
        }
    }
    
    /**
     * Gets the default configuration file for this mod loader.
     *
     * @return The default configuration file path
     */
    @NotNull
    public String getDefaultConfigFile() {
        switch (this) {
            case FORGE:
                return "src/main/resources/META-INF/mods.toml";
            case FABRIC:
                return "fabric.mod.json";
            case QUILT:
                return "quilt.mod.json";
            case ARCHITECTURY:
                return "common/build.gradle";
            default:
                return "";
        }
    }
}