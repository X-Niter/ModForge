package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for maintaining compatibility with different versions of IntelliJ IDEA.
 * Contains methods to handle API changes between versions.
 */
public class CompatibilityUtil {

    /**
     * Gets the project base directory in a way that's compatible with all IntelliJ versions.
     * Replaces the deprecated Project.getBaseDir() method with modern equivalents.
     *
     * @param project The project
     * @return The project base directory, or null if not available
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@NotNull Project project) {
        try {
            // First try the newer API (2020.3+): ProjectUtil.guessProjectDir()
            try {
                Class<?> projectUtilClass = Class.forName("com.intellij.openapi.project.ProjectUtil");
                java.lang.reflect.Method guessProjectDirMethod = 
                        projectUtilClass.getMethod("guessProjectDir", Project.class);
                return (VirtualFile) guessProjectDirMethod.invoke(null, project);
            } catch (Exception ignored) {
                // Fall back to older API
            }
            
            // Fall back to the deprecated method for older versions
            try {
                java.lang.reflect.Method getBaseDirMethod = Project.class.getMethod("getBaseDir");
                return (VirtualFile) getBaseDirMethod.invoke(project);
            } catch (Exception ignored) {
                // Neither method is available
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}