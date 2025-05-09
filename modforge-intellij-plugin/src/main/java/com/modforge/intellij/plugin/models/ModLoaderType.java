package com.modforge.intellij.plugin.models;

/**
 * Enum representing the different types of Minecraft mod loaders.
 */
public enum ModLoaderType {
    FORGE("Forge"),
    FABRIC("Fabric"),
    QUILT("Quilt"),
    NEOFORGE("NeoForge"),
    BUKKIT("Bukkit/Spigot"),
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    ModLoaderType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the mod loader type from its display name.
     * @param name The display name
     * @return The mod loader type
     */
    public static ModLoaderType fromDisplayName(String name) {
        for (ModLoaderType type : values()) {
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}