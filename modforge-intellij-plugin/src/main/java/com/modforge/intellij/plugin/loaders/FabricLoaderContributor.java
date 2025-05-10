package com.modforge.intellij.plugin.loaders;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.utils.ModLoaderContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Contributor for Fabric mod loader.
 */
public class FabricLoaderContributor implements ModLoaderContributor {
    private static final Logger LOG = Logger.getInstance(FabricLoaderContributor.class);
    private static final Pattern FABRIC_PATTERN = Pattern.compile("fabric|fabricmc", Pattern.CASE_INSENSITIVE);
    
    @Override
    @NotNull
    public String getModLoaderId() {
        return "fabric";
    }
    
    @Override
    @NotNull
    public String getModLoaderDisplayName() {
        return "Fabric Mod Loader";
    }
    
    @Override
    @NotNull
    public String getSupportedMinecraftVersions() {
        return "1.14 - 1.20.1";
    }
    
    @Override
    public boolean detectModLoader(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Look for build.gradle with Fabric dependency
        VirtualFile buildGradle = baseDir.findChild("build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray(), StandardCharsets.UTF_8);
                if (FABRIC_PATTERN.matcher(content).find()) {
                    return true;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read build.gradle", e);
            }
        }
        
        // Look for fabric.mod.json
        VirtualFile srcMainResources = findDirectory(baseDir, "src/main/resources");
        if (srcMainResources != null && srcMainResources.findChild("fabric.mod.json") != null) {
            return true;
        }
        
        return false;
    }
    
    @Override
    @NotNull
    public String generateWorkflowContent() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("name: Fabric Build\n\n");
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
        yaml.append("    - name: Grant execute permission for gradlew\n");
        yaml.append("      run: chmod +x gradlew\n");
        yaml.append("    - name: Build with Gradle\n");
        yaml.append("      uses: gradle/gradle-build-action@v2\n");
        yaml.append("      with:\n");
        yaml.append("        arguments: build\n");
        yaml.append("    - name: Upload artifacts\n");
        yaml.append("      uses: actions/upload-artifact@v3\n");
        yaml.append("      with:\n");
        yaml.append("        name: fabric-mod\n");
        yaml.append("        path: build/libs/*.jar\n");
        
        return yaml.toString();
    }
    
    @Override
    @Nullable
    public ModLoaderContributor.ModLoaderTemplate getTemplate() {
        return new ModLoaderContributor.ModLoaderTemplate(
                "fabric-template",
                "Fabric Mod Template",
                "A starter template for Fabric mods"
        );
    }
    
    /**
     * Find a directory by path.
     *
     * @param base The base directory
     * @param path The path
     * @return The directory, or null if not found
     */
    @Nullable
    private VirtualFile findDirectory(@NotNull VirtualFile base, @NotNull String path) {
        String[] parts = path.split("/");
        VirtualFile current = base;
        
        for (String part : parts) {
            current = current.findChild(part);
            if (current == null || !current.isDirectory()) {
                return null;
            }
        }
        
        return current;
    }
}