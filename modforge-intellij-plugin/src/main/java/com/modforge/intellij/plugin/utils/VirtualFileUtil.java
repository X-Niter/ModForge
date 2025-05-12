package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
     * Finds a file by path.
     *
     * @param path The path to the file.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        return ReadAction.compute(() -> {
            try {
                File file = new File(path);
                return LocalFileSystem.getInstance().findFileByIoFile(file);
            } catch (Exception e) {
                LOG.error("Failed to find file by path: " + path, e);
                return null;
            }
        });
    }

    /**
     * Finds a file by IoFile.
     *
     * @param file The file.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByIoFile(@NotNull File file) {
        return ReadAction.compute(() -> {
            try {
                return LocalFileSystem.getInstance().findFileByIoFile(file);
            } catch (Exception e) {
                LOG.error("Failed to find file by IoFile: " + file.getPath(), e);
                return null;
            }
        });
    }

    /**
     * Creates a directory.
     *
     * @param parent The parent directory.
     * @param name   The directory name.
     * @return The created directory, or null if creation failed.
     */
    @Nullable
    public static VirtualFile createDirectory(@NotNull VirtualFile parent, @NotNull String name) {
        return WriteAction.compute(() -> {
            try {
                return parent.createChildDirectory(null, name);
            } catch (IOException e) {
                LOG.error("Failed to create directory: " + name, e);
                return null;
            }
        });
    }

    /**
     * Creates a file.
     *
     * @param parent  The parent directory.
     * @param name    The file name.
     * @param content The file content.
     * @return The created file, or null if creation failed.
     */
    @Nullable
    public static VirtualFile createFile(
            @NotNull VirtualFile parent,
            @NotNull String name,
            @NotNull String content) {
        
        return WriteAction.compute(() -> {
            try {
                VirtualFile file = parent.createChildData(null, name);
                VfsUtil.saveText(file, content);
                return file;
            } catch (IOException e) {
                LOG.error("Failed to create file: " + name, e);
                return null;
            }
        });
    }

    /**
     * Reads the content of a file.
     *
     * @param file The file.
     * @return The file content, or null if reading failed.
     */
    @Nullable
    public static String readFileContent(@NotNull VirtualFile file) {
        return ReadAction.compute(() -> {
            try {
                return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error("Failed to read file content: " + file.getPath(), e);
                return null;
            }
        });
    }

    /**
     * Writes content to a file.
     *
     * @param file    The file.
     * @param content The content to write.
     * @return Whether writing was successful.
     */
    public static boolean writeFileContent(@NotNull VirtualFile file, @NotNull String content) {
        return WriteAction.compute(() -> {
            try {
                VfsUtil.saveText(file, content);
                return true;
            } catch (IOException e) {
                LOG.error("Failed to write file content: " + file.getPath(), e);
                return false;
            }
        });
    }

    /**
     * Opens a file in the editor.
     *
     * @param project The project.
     * @param file    The file.
     * @param focus   Whether to focus the editor.
     */
    public static void openFileInEditor(
            @NotNull Project project,
            @NotNull VirtualFile file,
            boolean focus) {
        
        if (!file.isValid()) {
            LOG.warn("Cannot open invalid file: " + file.getPath());
            return;
        }
        
        CompatibilityUtil.executeOnUiThread(() -> {
            try {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
                fileEditorManager.openEditor(descriptor, focus);
            } catch (Exception e) {
                LOG.error("Failed to open file in editor: " + file.getPath(), e);
            }
        });
    }

    /**
     * Gets all files under a directory.
     *
     * @param directory The directory.
     * @return A list of files.
     */
    @NotNull
    public static List<VirtualFile> getFilesUnderDirectory(@NotNull VirtualFile directory) {
        return ReadAction.compute(() -> {
            try {
                List<VirtualFile> result = new ArrayList<>();
                collectFiles(directory, result);
                return result;
            } catch (Exception e) {
                LOG.error("Failed to get files under directory: " + directory.getPath(), e);
                return List.of();
            }
        });
    }

    /**
     * Helper method to collect files recursively.
     *
     * @param file   The file or directory.
     * @param result The list to add files to.
     */
    private static void collectFiles(@NotNull VirtualFile file, @NotNull List<VirtualFile> result) {
        if (file.isDirectory()) {
            for (VirtualFile child : file.getChildren()) {
                collectFiles(child, result);
            }
        } else {
            result.add(file);
        }
    }

    /**
     * Gets all files matching a pattern.
     *
     * @param directory The directory.
     * @param pattern   The pattern.
     * @return A list of matching files.
     */
    @NotNull
    public static List<VirtualFile> getFilesMatchingPattern(
            @NotNull VirtualFile directory,
            @NotNull String pattern) {
        
        return ReadAction.compute(() -> {
            try {
                List<VirtualFile> allFiles = getFilesUnderDirectory(directory);
                List<VirtualFile> result = new ArrayList<>();
                
                for (VirtualFile file : allFiles) {
                    if (StringUtil.wildcardMatch(file.getName(), pattern)) {
                        result.add(file);
                    }
                }
                
                return result;
            } catch (Exception e) {
                LOG.error("Failed to get files matching pattern: " + pattern, e);
                return List.of();
            }
        });
    }

    /**
     * Ensures a directory exists.
     *
     * @param path The directory path.
     * @return The directory, or null if creation failed.
     */
    @Nullable
    public static VirtualFile ensureDirectoryExists(@NotNull String path) {
        try {
            Path dirPath = Paths.get(path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            File dirFile = dirPath.toFile();
            return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dirFile);
        } catch (IOException e) {
            LOG.error("Failed to ensure directory exists: " + path, e);
            return null;
        }
    }

    /**
     * Creates a file asynchronously.
     *
     * @param parent  The parent directory.
     * @param name    The file name.
     * @param content The file content.
     * @return A CompletableFuture that completes with the created file, or null if creation failed.
     */
    @NotNull
    public static CompletableFuture<VirtualFile> createFileAsync(
            @NotNull VirtualFile parent,
            @NotNull String name,
            @NotNull String content) {
        
        return ThreadUtils.supplyAsyncVirtual(() -> createFile(parent, name, content));
    }

    /**
     * Finds a file by path asynchronously.
     *
     * @param path The path to the file.
     * @return A CompletableFuture that completes with the virtual file, or null if not found.
     */
    @NotNull
    public static CompletableFuture<VirtualFile> findFileByPathAsync(@NotNull String path) {
        return ThreadUtils.supplyAsyncVirtual(() -> findFileByPath(path));
    }

    /**
     * Gets mod file by its relative path.
     *
     * @param project       The project.
     * @param relativePath  The relative path.
     * @return The file or null if not found.
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            return null;
        }
        
        return ReadAction.compute(() -> {
            try {
                return baseDir.findFileByRelativePath(relativePath);
            } catch (Exception e) {
                LOG.error("Failed to find file by relative path: " + relativePath, e);
                return null;
            }
        });
    }
}