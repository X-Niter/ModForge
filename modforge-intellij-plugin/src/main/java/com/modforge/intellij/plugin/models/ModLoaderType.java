package com.modforge.intellij.plugin.models;

/**
 * Enum representing different Minecraft mod loader types.
 * This enum defines the supported mod loaders in the ModForge system.
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
     * NeoForge mod loader (a fork of Forge).
     */
    NEOFORGE("NeoForge"),
    
    /**
     * Bukkit server plugin API.
     */
    BUKKIT("Bukkit"),
    
    /**
     * Spigot server API (extends Bukkit).
     */
    SPIGOT("Spigot"),
    
    /**
     * Paper server API (extends Spigot).
     */
    PAPER("Paper"),
    
    /**
     * Architectury mod loader (cross-platform compatibility layer).
     */
    ARCHITECTURY("Architectury"),
    
    /**
     * Unknown or unspecified mod loader.
     */
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    /**
     * Constructs a mod loader type with the given display name.
     *
     * @param displayName The human-readable display name
     */
    ModLoaderType(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the human-readable display name of the mod loader.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets a ModLoaderType by its display name.
     *
     * @param name The display name to search for
     * @return The corresponding ModLoaderType, or UNKNOWN if not found
     */
    public static ModLoaderType fromDisplayName(String name) {
        if (name == null) {
            return UNKNOWN;
        }
        
        for (ModLoaderType type : values()) {
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Checks if this mod loader is compatible with the given mod loader.
     *
     * @param other The other mod loader to check compatibility with
     * @return True if the mod loaders are compatible, false otherwise
     */
    public boolean isCompatibleWith(ModLoaderType other) {
        if (this == other) {
            return true;
        }
        
        // Architectury is compatible with Forge and Fabric
        if (this == ARCHITECTURY) {
            return other == FORGE || other == FABRIC || other == QUILT || other == NEOFORGE;
        }
        
        if (other == ARCHITECTURY) {
            return this == FORGE || this == FABRIC || this == QUILT || this == NEOFORGE;
        }
        
        // Bukkit family compatibility
        if (this == BUKKIT && (other == SPIGOT || other == PAPER)) {
            return true;
        }
        
        if (other == BUKKIT && (this == SPIGOT || this == PAPER)) {
            return true;
        }
        
        // Spigot and Paper are compatible with each other
        if ((this == SPIGOT && other == PAPER) || (this == PAPER && other == SPIGOT)) {
            return true;
        }
        
        // NeoForge and Forge have some compatibility
        if ((this == FORGE && other == NEOFORGE) || (this == NEOFORGE && other == FORGE)) {
            return true;
        }
        
        return false;
    }
}