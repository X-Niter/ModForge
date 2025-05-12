package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Utility class for working with VirtualFiles.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class VirtualFileUtil {
    private static final Logger LOG = Logger.getInstance(VirtualFileUtil.class);

    private VirtualFileUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Finds a file in the project by its path.
     *
     * @param path The file path.
     * @return The VirtualFile or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * Finds a file in the project by its Path object.
     *
     * @param path The file Path.
     * @return The VirtualFile or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull Path path) {
        return VfsUtil.findFile(path, true);
    }

    /**
     * Creates a new directory in the specified parent directory.
     *
     * @param parent    The parent directory.
     * @param dirName   The name of the directory to create.
     * @return The created directory or null if creation failed.
     */
    @Nullable
    public static VirtualFile createDirectory(@NotNull VirtualFile parent, @NotNull String dirName) {
        if (!parent.isDirectory()) {
            LOG.error("Parent is not a directory: " + parent.getPath());
            return null;
        }

        AtomicReference<VirtualFile> result = new AtomicReference<>();
        try {
            WriteAction.runAndWait(() -> {
                try {
                    result.set(parent.createChildDirectory(null, dirName));
                } catch (IOException e) {
                    LOG.error("Failed to create directory: " + dirName, e);
                }
            });
        } catch (Exception e) {
            LOG.error("Error during write action for directory creation", e);
        }

        return result.get();
    }

    /**
     * Creates a new file in the specified parent directory.
     *
     * @param parent    The parent directory.
     * @param fileName  The name of the file to create.
     * @param content   The content of the file.
     * @return The created file or null if creation failed.
     */
    @Nullable
    public static VirtualFile createFile(@NotNull VirtualFile parent, @NotNull String fileName, @NotNull String content) {
        if (!parent.isDirectory()) {
            LOG.error("Parent is not a directory: " + parent.getPath());
            return null;
        }

        AtomicReference<VirtualFile> result = new AtomicReference<>();
        try {
            WriteAction.runAndWait(() -> {
                try {
                    VirtualFile file = parent.createChildData(null, fileName);
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                    result.set(file);
                } catch (IOException e) {
                    LOG.error("Failed to create file: " + fileName, e);
                }
            });
        } catch (Exception e) {
            LOG.error("Error during write action for file creation", e);
        }

        return result.get();
    }

    /**
     * Writes content to a file.
     *
     * @param file      The file to write to.
     * @param content   The content to write.
     * @return True if the write was successful, false otherwise.
     */
    public static boolean writeToFile(@NotNull VirtualFile file, @NotNull String content) {
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOG.error("Failed to write to file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error("Error during write action for file write", e);
            return false;
        }
    }

    /**
     * Reads the content of a file.
     *
     * @param file The file to read.
     * @return The content of the file or null if reading failed.
     */
    @Nullable
    public static String readFile(@NotNull VirtualFile file) {
        try {
            return VfsUtilCore.loadText(file);
        } catch (IOException e) {
            LOG.error("Failed to read file: " + file.getPath(), e);
            return null;
        }
    }

    /**
     * Deletes a file or directory.
     *
     * @param file The file or directory to delete.
     * @return True if the deletion was successful, false otherwise.
     */
    public static boolean delete(@NotNull VirtualFile file) {
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.delete(null);
                } catch (IOException e) {
                    LOG.error("Failed to delete file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error("Error during write action for file deletion", e);
            return false;
        }
    }

    /**
     * Renames a file or directory.
     *
     * @param file    The file or directory to rename.
     * @param newName The new name.
     * @return True if the rename was successful, false otherwise.
     */
    public static boolean rename(@NotNull VirtualFile file, @NotNull String newName) {
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.rename(null, newName);
                } catch (IOException e) {
                    LOG.error("Failed to rename file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error("Error during write action for file rename", e);
            return false;
        }
    }

    /**
     * Creates a directory structure from a path.
     *
     * @param baseDir The base directory.
     * @param path    The path to create.
     * @return The created directory or null if creation failed.
     */
    @Nullable
    public static VirtualFile createDirectoryStructure(@NotNull VirtualFile baseDir, @NotNull String path) {
        if (!baseDir.isDirectory()) {
            LOG.error("Base directory is not a directory: " + baseDir.getPath());
            return null;
        }

        // Split the path and create each directory in turn
        String[] parts = path.split("/");
        VirtualFile current = baseDir;

        for (String part : parts) {
            if (StringUtil.isEmpty(part)) {
                continue;
            }

            VirtualFile child = current.findChild(part);
            if (child == null) {
                child = createDirectory(current, part);
                if (child == null) {
                    return null;
                }
            } else if (!child.isDirectory()) {
                LOG.error("Path component is not a directory: " + child.getPath());
                return null;
            }

            current = child;
        }

        return current;
    }

    /**
     * Finds all files matching a predicate recursively in a directory.
     *
     * @param dir       The directory to search in.
     * @param predicate The predicate to match.
     * @return A list of matching files.
     */
    @NotNull
    public static List<VirtualFile> findFilesRecursively(@NotNull VirtualFile dir, @NotNull Predicate<VirtualFile> predicate) {
        List<VirtualFile> result = new ArrayList<>();
        findFilesRecursively(dir, predicate, result);
        return result;
    }

    private static void findFilesRecursively(@NotNull VirtualFile dir, @NotNull Predicate<VirtualFile> predicate, @NotNull List<VirtualFile> result) {
        if (!dir.isDirectory()) {
            return;
        }

        for (VirtualFile file : dir.getChildren()) {
            if (file.isDirectory()) {
                findFilesRecursively(file, predicate, result);
            } else if (predicate.test(file)) {
                result.add(file);
            }
        }
    }

    /**
     * Gets the relative path of a file to a base directory.
     *
     * @param file    The file.
     * @param baseDir The base directory.
     * @return The relative path or null if the file is not under the base directory.
     */
    @Nullable
    public static String getRelativePath(@NotNull VirtualFile file, @NotNull VirtualFile baseDir) {
        if (!baseDir.isDirectory()) {
            LOG.error("Base directory is not a directory: " + baseDir.getPath());
            return null;
        }

        return VfsUtilCore.getRelativePath(file, baseDir);
    }

    /**
     * Gets the relative path of a file to a project base directory.
     *
     * @param file    The file.
     * @param project The project.
     * @return The relative path or null if the file is not under the project base directory.
     */
    @Nullable
    public static String getRelativePathToProject(@NotNull VirtualFile file, @NotNull Project project) {
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            LOG.error("Project base directory is null");
            return null;
        }

        return getRelativePath(file, baseDir);
    }

    /**
     * Refreshes a file.
     *
     * @param file The file to refresh.
     */
    public static void refreshFile(@NotNull VirtualFile file) {
        file.refresh(false, false);
    }

    /**
     * Refreshes a file with children.
     *
     * @param file The file to refresh.
     */
    public static void refreshFileWithChildren(@NotNull VirtualFile file) {
        file.refresh(true, true);
    }

    /**
     * Saves all documents.
     */
    public static void saveAllDocuments() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
        });
    }

    /**
     * Creates a Virtual File from a Path.
     *
     * @param path The path to convert.
     * @return The VirtualFile or null if conversion failed.
     */
    @Nullable
    public static VirtualFile pathToVirtualFile(@NotNull String path) {
        return VfsUtil.findFile(Paths.get(path), true);
    }

    /**
     * Creates a Path from a VirtualFile.
     *
     * @param file The file to convert.
     * @return The Path.
     */
    @NotNull
    public static Path virtualFileToPath(@NotNull VirtualFile file) {
        return Paths.get(file.getPath());
    }

    /**
     * Moves a file to a new parent directory.
     *
     * @param file      The file to move.
     * @param newParent The new parent directory.
     * @return True if the move was successful, false otherwise.
     */
    public static boolean moveFile(@NotNull VirtualFile file, @NotNull VirtualFile newParent) {
        if (!newParent.isDirectory()) {
            LOG.error("New parent is not a directory: " + newParent.getPath());
            return false;
        }

        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.move(null, newParent);
                } catch (IOException e) {
                    LOG.error("Failed to move file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error("Error during write action for file move", e);
            return false;
        }
    }

    /**
     * Copies a file to a new parent directory.
     *
     * @param file      The file to copy.
     * @param newParent The new parent directory.
     * @param newName   The new name.
     * @return The copied file or null if copying failed.
     */
    @Nullable
    public static VirtualFile copyFile(@NotNull VirtualFile file, @NotNull VirtualFile newParent, @NotNull String newName) {
        if (!newParent.isDirectory()) {
            LOG.error("New parent is not a directory: " + newParent.getPath());
            return null;
        }

        AtomicReference<VirtualFile> result = new AtomicReference<>();
        try {
            WriteAction.runAndWait(() -> {
                try {
                    result.set(file.copy(null, newParent, newName));
                } catch (IOException e) {
                    LOG.error("Failed to copy file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            return result.get();
        } catch (Exception e) {
            LOG.error("Error during write action for file copy", e);
            return null;
        }
    }

    /**
     * Gets the file extension.
     *
     * @param file The file.
     * @return The file extension or an empty string if none.
     */
    @NotNull
    public static String getFileExtension(@NotNull VirtualFile file) {
        return file.getExtension() != null ? file.getExtension() : "";
    }

    /**
     * Checks if a file exists at the given path.
     *
     * @param path The file path.
     * @return True if the file exists, false otherwise.
     */
    public static boolean fileExists(@NotNull String path) {
        return findFileByPath(path) != null;
    }

    /**
     * Gets the project relative path for a file path.
     *
     * @param project  The project.
     * @param filePath The file path.
     * @return The project relative path or null if the file is not under the project base directory.
     */
    @Nullable
    public static String getProjectRelativePath(@NotNull Project project, @NotNull String filePath) {
        VirtualFile file = findFileByPath(filePath);
        if (file == null) {
            return null;
        }

        return getRelativePathToProject(file, project);
    }
}