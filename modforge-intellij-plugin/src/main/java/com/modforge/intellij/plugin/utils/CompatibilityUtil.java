package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for handling compatibility with different IntelliJ versions.
 * This helps maintain backward compatibility while supporting newer IntelliJ API.
 */
public class CompatibilityUtil {

    /**
     * Gets the base directory for a project, handling API changes between IntelliJ versions.
     * Replaces the deprecated Project.getBaseDir() method with a compatible implementation.
     *
     * @param project the IntelliJ project
     * @return the base directory virtual file, or null if not available
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@NotNull Project project) {
        // In IntelliJ 2020.3+, getBasePath() is the preferred method
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        
        // Convert the path to a VirtualFile
        Path path = Paths.get(basePath);
        return VirtualFileUtil.pathToVirtualFile(path);
    }
    
    /**
     * Gets the base directory path for a project as a string.
     *
     * @param project the IntelliJ project
     * @return the base directory path, or null if not available
     */
    @Nullable
    public static String getProjectBasePath(@NotNull Project project) {
        return project.getBasePath();
    }
}