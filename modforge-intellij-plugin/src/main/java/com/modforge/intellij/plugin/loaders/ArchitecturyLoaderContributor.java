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
 * Contributor for Architectury, a multi-loader environment.
 */
public class ArchitecturyLoaderContributor implements ModLoaderContributor {
    private static final Logger LOG = Logger.getInstance(ArchitecturyLoaderContributor.class);
    private static final Pattern ARCHITECTURY_PATTERN = Pattern.compile("architectury", Pattern.CASE_INSENSITIVE);
    
    @Override
    @NotNull
    public String getModLoaderId() {
        return "architectury";
    }
    
    @Override
    @NotNull
    public String getModLoaderDisplayName() {
        return "Architectury (Multi-Loader)";
    }
    
    @Override
    @NotNull
    public String getSupportedMinecraftVersions() {
        return "1.16 - 1.20.1";
    }
    
    @Override
    public boolean detectModLoader(@NotNull Project project, @NotNull VirtualFile baseDir) {
        // Look for typical Architectury project structure
        VirtualFile commonDir = baseDir.findChild("common");
        VirtualFile forgeDir = baseDir.findChild("forge");
        VirtualFile fabricDir = baseDir.findChild("fabric");
        
        if ((commonDir != null && commonDir.isDirectory()) && 
                ((forgeDir != null && forgeDir.isDirectory()) || 
                (fabricDir != null && fabricDir.isDirectory()))) {
            return true;
        }
        
        // Look for settings.gradle with Architectury plugin
        VirtualFile settingsGradle = baseDir.findChild("settings.gradle");
        if (settingsGradle != null) {
            try {
                String content = new String(settingsGradle.contentsToByteArray(), StandardCharsets.UTF_8);
                if (ARCHITECTURY_PATTERN.matcher(content).find()) {
                    return true;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read settings.gradle", e);
            }
        }
        
        // Check build.gradle as well
        VirtualFile buildGradle = baseDir.findChild("build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray(), StandardCharsets.UTF_8);
                if (ARCHITECTURY_PATTERN.matcher(content).find()) {
                    return true;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read build.gradle", e);
            }
        }
        
        return false;
    }
    
    @Override
    @NotNull
    public String generateWorkflowContent() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("name: Multi-Loader Build\n\n");
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
        yaml.append("        name: multi-loader-mod\n");
        yaml.append("        path: |\n");
        yaml.append("          */build/libs/*.jar\n");
        yaml.append("          common/build/libs/*.jar\n");
        yaml.append("          forge/build/libs/*.jar\n");
        yaml.append("          fabric/build/libs/*.jar\n");
        yaml.append("          quilt/build/libs/*.jar\n");
        
        return yaml.toString();
    }
    
    @Override
    @Nullable
    public ModLoaderContributor.ModLoaderTemplate getTemplate() {
        return new ModLoaderContributor.ModLoaderTemplate(
                "architectury-template",
                "Architectury Multi-Loader Template",
                "A starter template for cross-platform Minecraft mods using Architectury"
        );
    }
}