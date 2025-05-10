package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point interface for mod loader detection and support.
 * Third-party plugins can implement this to add support for additional mod loaders.
 */
public interface ModLoaderContributor {
    /**
     * Extension point name for ModLoaderContributor.
     */
    ExtensionPointName<ModLoaderContributor> EP_NAME = 
            ExtensionPointName.create("com.modforge.intellij.plugin.modLoaderContributor");
    
    /**
     * Get the ID of the mod loader.
     *
     * @return The mod loader ID
     */
    @NotNull
    String getModLoaderId();
    
    /**
     * Get the display name of the mod loader.
     *
     * @return The mod loader display name
     */
    @NotNull
    String getModLoaderDisplayName();
    
    /**
     * Get the version range that this mod loader supports.
     *
     * @return The Minecraft version range
     */
    @NotNull
    default String getSupportedMinecraftVersions() {
        return "1.16+"; // Default to a reasonable modern version
    }
    
    /**
     * Detect if this mod loader is used by the project.
     *
     * @param project The project
     * @param baseDir The project base directory
     * @return True if the mod loader is detected, false otherwise
     */
    boolean detectModLoader(@NotNull Project project, @NotNull VirtualFile baseDir);
    
    /**
     * Get the GitHub Actions workflow content for this mod loader.
     *
     * @return The workflow content
     */
    @NotNull
    default String generateWorkflowContent() {
        // Default implementation generates a basic Gradle build workflow
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("name: Build\n\n");
        yaml.append("on:\n");
        yaml.append("  push:\n");
        yaml.append("    branches: [ main ]\n");
        yaml.append("  pull_request:\n");
        yaml.append("    branches: [ main ]\n\n");
        yaml.append("jobs:\n");
        yaml.append("  build:\n");
        yaml.append("    runs-on: ubuntu-latest\n\n");
        yaml.append("    steps:\n");
        yaml.append("    - uses: actions/checkout@v3\n");
        yaml.append("    - name: Set up JDK 17\n");
        yaml.append("      uses: actions/setup-java@v3\n");
        yaml.append("      with:\n");
        yaml.append("        java-version: '17'\n");
        yaml.append("        distribution: 'temurin'\n");
        yaml.append("    - name: Build with Gradle\n");
        yaml.append("      uses: gradle/gradle-build-action@v2\n");
        yaml.append("      with:\n");
        yaml.append("        arguments: build\n");
        
        return yaml.toString();
    }
    
    /**
     * Get the project template for this mod loader.
     * 
     * @return The template, or null if not provided
     */
    @Nullable
    default ModLoaderTemplate getTemplate() {
        return null;
    }
    
    /**
     * Simple template model for mod loaders.
     */
    class ModLoaderTemplate {
        private final String id;
        private final String name;
        private final String description;
        
        public ModLoaderTemplate(@NotNull String id, @NotNull String name, @NotNull String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
        
        @NotNull
        public String getId() {
            return id;
        }
        
        @NotNull
        public String getName() {
            return name;
        }
        
        @NotNull
        public String getDescription() {
            return description;
        }
    }
}