package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for working with virtual files.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class VirtualFileUtil {
    private static final Logger LOG = Logger.getInstance(VirtualFileUtil.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private VirtualFileUtil() {
        // Utility class
    }

    /**
     * Gets a virtual file from a path.
     *
     * @param path The path.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile getVirtualFile(@NotNull String path) {
        return CompatibilityUtil.findFileByPath(path);
    }

    /**
     * Gets a virtual file from a Java File.
     *
     * @param file The file.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile getVirtualFile(@NotNull File file) {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }

    /**
     * Gets a virtual file from a Path.
     *
     * @param path The path.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile getVirtualFile(@NotNull Path path) {
        return getVirtualFile(path.toFile());
    }

    /**
     * Creates a directory.
     *
     * @param parent The parent directory.
     * @param name   The name of the new directory.
     * @return The created directory, or null if failed.
     */
    @Nullable
    public static VirtualFile createDirectory(@NotNull VirtualFile parent, @NotNull String name) {
        try {
            return ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    return parent.createChildDirectory(VirtualFileUtil.class, name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to create directory: " + name, e);
            return null;
        }
    }

    /**
     * Creates a file.
     *
     * @param parent  The parent directory.
     * @param name    The name of the new file.
     * @param content The content of the new file.
     * @return The created file, or null if failed.
     */
    @Nullable
    public static VirtualFile createFile(
            @NotNull VirtualFile parent,
            @NotNull String name,
            @NotNull String content) {
        
        try {
            return ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    VirtualFile file = parent.createChildData(VirtualFileUtil.class, name);
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                    return file;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to create file: " + name, e);
            return null;
        }
    }

    /**
     * Creates a file asynchronously.
     *
     * @param parent  The parent directory.
     * @param name    The name of the new file.
     * @param content The content of the new file.
     * @return A future that completes with the created file, or null if failed.
     */
    @NotNull
    public static CompletableFuture<VirtualFile> createFileAsync(
            @NotNull VirtualFile parent,
            @NotNull String name,
            @NotNull String content) {
        
        return CompatibilityUtil.runUnderWriteActionAsync(() -> {
            try {
                VirtualFile file = parent.createChildData(VirtualFileUtil.class, name);
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                return file;
            } catch (IOException e) {
                LOG.error("Failed to create file: " + name, e);
                return null;
            }
        });
    }

    /**
     * Writes content to a file.
     *
     * @param file    The file.
     * @param content The content.
     * @return Whether the operation was successful.
     */
    public static boolean writeToFile(@NotNull VirtualFile file, @NotNull String content) {
        try {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error("Failed to write to file: " + file.getPath(), e);
            return false;
        }
    }

    /**
     * Writes content to a file asynchronously.
     *
     * @param file    The file.
     * @param content The content.
     * @return A future that completes with whether the operation was successful.
     */
    @NotNull
    public static CompletableFuture<Boolean> writeToFileAsync(@NotNull VirtualFile file, @NotNull String content) {
        return CompatibilityUtil.runUnderWriteActionAsync(() -> {
            try {
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                return true;
            } catch (IOException e) {
                LOG.error("Failed to write to file: " + file.getPath(), e);
                return false;
            }
        });
    }

    /**
     * Reads content from a file.
     *
     * @param file The file.
     * @return The content, or null if failed.
     */
    @Nullable
    public static String readFromFile(@NotNull VirtualFile file) {
        try {
            byte[] bytes = file.contentsToByteArray();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to read from file: " + file.getPath(), e);
            return null;
        }
    }

    /**
     * Reads content from a file asynchronously.
     *
     * @param file The file.
     * @return A future that completes with the content, or null if failed.
     */
    @NotNull
    public static CompletableFuture<String> readFromFileAsync(@NotNull VirtualFile file) {
        return CompatibilityUtil.runUnderReadActionAsync(() -> {
            try {
                byte[] bytes = file.contentsToByteArray();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error("Failed to read from file: " + file.getPath(), e);
                return null;
            }
        });
    }

    /**
     * Deletes a file or directory.
     *
     * @param file The file or directory.
     * @return Whether the operation was successful.
     */
    public static boolean delete(@NotNull VirtualFile file) {
        try {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    file.delete(VirtualFileUtil.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error("Failed to delete: " + file.getPath(), e);
            return false;
        }
    }

    /**
     * Deletes a file or directory asynchronously.
     *
     * @param file The file or directory.
     * @return A future that completes with whether the operation was successful.
     */
    @NotNull
    public static CompletableFuture<Boolean> deleteAsync(@NotNull VirtualFile file) {
        return CompatibilityUtil.runUnderWriteActionAsync(() -> {
            try {
                file.delete(VirtualFileUtil.class);
                return true;
            } catch (IOException e) {
                LOG.error("Failed to delete: " + file.getPath(), e);
                return false;
            }
        });
    }

    /**
     * Gets the file type of a file.
     *
     * @param file The file.
     * @return The file type.
     */
    @NotNull
    public static FileType getFileType(@NotNull VirtualFile file) {
        return FileTypeManager.getInstance().getFileTypeByFile(file);
    }

    /**
     * Gets the file type of a file name.
     *
     * @param fileName The file name.
     * @return The file type.
     */
    @NotNull
    public static FileType getFileTypeByFileName(@NotNull String fileName) {
        return FileTypeManager.getInstance().getFileTypeByFileName(fileName);
    }

    /**
     * Refreshes a file.
     *
     * @param file The file.
     * @param async Whether to refresh asynchronously.
     * @param recursive Whether to refresh recursively.
     */
    public static void refresh(@NotNull VirtualFile file, boolean async, boolean recursive) {
        file.refresh(async, recursive);
    }

    /**
     * Refreshes a file synchronously.
     *
     * @param file The file.
     * @param recursive Whether to refresh recursively.
     */
    public static void refreshSync(@NotNull VirtualFile file, boolean recursive) {
        refresh(file, false, recursive);
    }

    /**
     * Refreshes a file asynchronously.
     *
     * @param file The file.
     * @param recursive Whether to refresh recursively.
     */
    public static void refreshAsync(@NotNull VirtualFile file, boolean recursive) {
        refresh(file, true, recursive);
    }

    /**
     * Saves all documents.
     */
    public static void saveAllDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments();
    }
    
    /**
     * Gets the relative path of a file to the project base directory.
     *
     * @param file    The file.
     * @param project The project.
     * @return The relative path, or the absolute path if the file is not under the project.
     */
    @NotNull
    public static String getRelativePath(@NotNull VirtualFile file, @NotNull Project project) {
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            return file.getPath();
        }
        
        String basePath = baseDir.getPath();
        String filePath = file.getPath();
        
        if (filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1); // +1 to skip the trailing slash
        }
        
        return filePath;
    }
}