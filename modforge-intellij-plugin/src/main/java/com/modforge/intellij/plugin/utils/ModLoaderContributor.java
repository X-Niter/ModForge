package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Extension point interface for detecting mod loaders.
 * Plugins can implement this interface to contribute their own mod loader detection logic.
 */
public interface ModLoaderContributor {
    ExtensionPointName<ModLoaderContributor> EP_NAME =
            ExtensionPointName.create("com.modforge.intellij.plugin.modLoaderContributor");
    
    /**
     * Detect if a mod loader is supported by this contributor in the given project.
     *
     * @param project The project to check
     * @return True if this contributor can detect its mod loader in the project, false otherwise
     */
    boolean isModLoaderSupported(@NotNull Project project);
    
    /**
     * Get the mod loader type provided by this contributor.
     *
     * @return The mod loader type, as a string
     */
    @NotNull
    String getModLoaderType();
    
    /**
     * Get the display name of the mod loader.
     *
     * @return The display name
     */
    @NotNull
    String getDisplayName();
    
    /**
     * Get the priority of this contributor.
     * Higher priority contributors will be checked first.
     *
     * @return The priority, defaults to 0
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Check if this mod loader is compatible with another mod loader.
     * This is used to determine cross-loader compatibility.
     *
     * @param otherLoader The other mod loader type
     * @return True if compatible, false otherwise
     */
    default boolean isCompatibleWith(@NotNull String otherLoader) {
        return false;
    }
}