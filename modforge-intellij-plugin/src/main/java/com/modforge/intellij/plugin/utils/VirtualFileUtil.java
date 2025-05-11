package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

/**
 * Utility class for working with VirtualFile operations.
 */
public class VirtualFileUtil {

    /**
     * Converts a java.nio.file.Path to a VirtualFile.
     *
     * @param path The Path to convert
     * @return The VirtualFile, or null if the file doesn't exist
     */
    @Nullable
    public static VirtualFile pathToVirtualFile(@NotNull Path path) {
        return LocalFileSystem.getInstance().findFileByIoFile(path.toFile());
    }

    /**
     * Converts a java.io.File to a VirtualFile.
     *
     * @param file The File to convert
     * @return The VirtualFile, or null if the file doesn't exist
     */
    @Nullable
    public static VirtualFile fileToVirtualFile(@NotNull File file) {
        return LocalFileSystem.getInstance().findFileByIoFile(file);
    }

    /**
     * Converts a path string to a VirtualFile.
     *
     * @param path The path string to convert
     * @return The VirtualFile, or null if the path doesn't exist
     */
    @Nullable
    public static VirtualFile stringToVirtualFile(@NotNull String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * Refreshes a VirtualFile to ensure its contents are up-to-date.
     *
     * @param file The VirtualFile to refresh
     * @return The refreshed VirtualFile, or null if the file is null
     */
    @Nullable
    public static VirtualFile refreshAndGetFile(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        file.refresh(false, false);
        return file;
    }
    
    /**
     * Refreshes all file systems and synchronizes with the local file system.
     * This is a thread-safe wrapper around the file system refresh functionality.
     */
    public static void refreshAll() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                LocalFileSystem.getInstance().refresh(true);
            });
        });
    }
    
    /**
     * Gets a virtual file by a relative path from the project base directory.
     * 
     * @param project the project
     * @param relativePath the relative path from the project base directory
     * @return the virtual file or null if it doesn't exist
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        return baseDir != null ? baseDir.findFileByRelativePath(relativePath) : null;
    }
}