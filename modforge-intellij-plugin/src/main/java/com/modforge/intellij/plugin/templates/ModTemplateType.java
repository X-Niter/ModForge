package com.modforge.intellij.plugin.templates;

import org.jetbrains.annotations.NotNull;

/**
 * Enum representing different types of mod templates.
 */
public enum ModTemplateType {
    FORGE("Forge", "Minecraft Forge"),
    FABRIC("Fabric", "Fabric Mod Loader"),
    QUILT("Quilt", "Quilt Mod Loader"),
    ARCHITECTURY("Architectury", "Architectury (multi-loader)"),
    BUKKIT("Bukkit", "Bukkit/Spigot/Paper"),
    CUSTOM("Custom", "Custom template type");
    
    private final String id;
    private final String displayName;
    
    /**
     * Create a new mod template type.
     *
     * @param id          The template type ID
     * @param displayName The template type display name
     */
    ModTemplateType(@NotNull String id, @NotNull String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Get the template type ID.
     *
     * @return The template type ID
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Get the template type display name.
     *
     * @return The template type display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get a template type by ID.
     *
     * @param id The template type ID
     * @return The template type, or CUSTOM if not found
     */
    @NotNull
    public static ModTemplateType fromId(@NotNull String id) {
        for (ModTemplateType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        
        return CUSTOM;
    }
}