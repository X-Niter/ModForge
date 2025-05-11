package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for working with VirtualFiles.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class VirtualFileUtil {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VirtualFileUtil.class);

    /**
     * Finds a file by its path.
     *
     * @param path The path to find.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * Finds a file by Java File object.
     *
     * @param file The file to find.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByIoFile(@NotNull File file) {
        return VfsUtil.findFileByIoFile(file, true);
    }

    /**
     * Creates directories for a path.
     *
     * @param parent The parent directory.
     * @param relativePath The relative path.
     * @return The created directory, or null if creation failed.
     */
    @Nullable
    public static VirtualFile createDirectories(@NotNull VirtualFile parent, @NotNull String relativePath) {
        String[] parts = relativePath.split("/");
        VirtualFile current = parent;
        
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            
            try {
                VirtualFile child = current.findChild(part);
                if (child == null) {
                    child = current.createChildDirectory(null, part);
                }
                current = child;
            } catch (IOException e) {
                LOG.error("Failed to create directory: " + part, e);
                return null;
            }
        }
        
        return current;
    }

    /**
     * Creates a file with the given content.
     *
     * @param parent The parent directory.
     * @param name The file name.
     * @param content The file content.
     * @return The created file, or null if creation failed.
     */
    @Nullable
    public static VirtualFile createFile(@NotNull VirtualFile parent, @NotNull String name, @NotNull String content) {
        try {
            VirtualFile file = parent.findChild(name);
            if (file == null) {
                file = parent.createChildData(null, name);
            }
            
            VfsUtil.saveText(file, content);
            return file;
        } catch (IOException e) {
            LOG.error("Failed to create file: " + name, e);
            return null;
        }
    }

    /**
     * Gets the relative path between two virtual files.
     *
     * @param ancestor The ancestor file.
     * @param descendant The descendant file.
     * @return The relative path, or null if not found.
     */
    @Nullable
    public static String getRelativePath(@NotNull VirtualFile ancestor, @NotNull VirtualFile descendant) {
        return VfsUtil.getRelativePath(descendant, ancestor);
    }

    /**
     * Gets the relative path between a virtual file and a project base directory.
     *
     * @param project The project.
     * @param file The file.
     * @return The relative path, or null if not found.
     */
    @Nullable
    public static String getRelativePath(@NotNull Project project, @NotNull VirtualFile file) {
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            return null;
        }
        
        return getRelativePath(baseDir, file);
    }

    /**
     * Gets the extension of a virtual file.
     *
     * @param file The file.
     * @return The extension, or an empty string if none.
     */
    @NotNull
    public static String getExtension(@NotNull VirtualFile file) {
        return file.getExtension() != null ? file.getExtension() : "";
    }

    /**
     * Gets the name without extension.
     *
     * @param file The file.
     * @return The name without extension.
     */
    @NotNull
    public static String getNameWithoutExtension(@NotNull VirtualFile file) {
        return file.getNameWithoutExtension();
    }

    /**
     * Finds all files with the given extension in a directory.
     *
     * @param dir The directory.
     * @param extension The extension to search for.
     * @param recursive Whether to search recursively.
     * @return The list of files found.
     */
    @NotNull
    public static List<VirtualFile> findFilesWithExtension(
            @NotNull VirtualFile dir,
            @NotNull String extension,
            boolean recursive) {
        
        List<VirtualFile> result = new ArrayList<>();
        findFilesWithExtensionRecursive(dir, extension, result, recursive);
        return result;
    }

    /**
     * Updates the content of a file.
     *
     * @param file The file to update.
     * @param content The new content.
     * @return Whether the update was successful.
     */
    public static boolean updateFileContent(@NotNull VirtualFile file, @NotNull String content) {
        try {
            CompatibilityUtil.runWriteAction(() -> {
                try {
                    VfsUtil.saveText(file, content);
                } catch (IOException e) {
                    LOG.error("Failed to update file content: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            LOG.error("Failed to update file content in write action: " + file.getPath(), e);
            return false;
        }
    }

    /**
     * Finds a directory by its path, creating it if it doesn't exist.
     *
     * @param path The path to find or create.
     * @return The directory, or null if creation failed.
     */
    @Nullable
    public static VirtualFile findOrCreateDirectory(@NotNull String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LOG.error("Failed to create directories: " + path);
                return null;
            }
        }
        
        return findFileByIoFile(file);
    }

    /**
     * Finds a file by its path, creating it if it doesn't exist.
     *
     * @param path The path to find or create.
     * @param defaultContent The default content for a new file.
     * @return The file, or null if creation failed.
     */
    @Nullable
    public static VirtualFile findOrCreateFile(@NotNull String path, @NotNull String defaultContent) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs()) {
                        LOG.error("Failed to create parent directories: " + parent.getPath());
                        return null;
                    }
                }
                
                if (!file.createNewFile()) {
                    LOG.error("Failed to create file: " + path);
                    return null;
                }
            } catch (IOException e) {
                LOG.error("Failed to create file: " + path, e);
                return null;
            }
        }
        
        VirtualFile virtualFile = findFileByIoFile(file);
        if (virtualFile != null && defaultContent.length() > 0) {
            updateFileContent(virtualFile, defaultContent);
        }
        
        return virtualFile;
    }

    /**
     * Private recursive helper for findFilesWithExtension.
     */
    private static void findFilesWithExtensionRecursive(
            @NotNull VirtualFile dir,
            @NotNull String extension,
            @NotNull List<VirtualFile> result,
            boolean recursive) {
        
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                if (recursive) {
                    findFilesWithExtensionRecursive(child, extension, result, true);
                }
            } else if (extension.equals(getExtension(child))) {
                result.add(child);
            }
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private VirtualFileUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}