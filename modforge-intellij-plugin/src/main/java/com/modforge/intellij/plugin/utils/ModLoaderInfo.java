package com.modforge.intellij.plugin.utils;

/**
 * Information about a mod loader.
 */
public class ModLoaderInfo {
    private final String id;
    private final String displayName;
    private final String supportedVersions;
    private final boolean hasTemplate;
    
    public ModLoaderInfo(String id, String displayName, String supportedVersions, boolean hasTemplate) {
        this.id = id;
        this.displayName = displayName;
        this.supportedVersions = supportedVersions;
        this.hasTemplate = hasTemplate;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSupportedVersions() {
        return supportedVersions;
    }
    
    public boolean hasTemplate() {
        return hasTemplate;
    }
}