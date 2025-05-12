package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for virtual file operations.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class VirtualFileUtil {
    private static final Logger LOG = Logger.getInstance(VirtualFileUtil.class);
    
    // Constants
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private VirtualFileUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Finds a file by path.
     *
     * @param path The path.
     * @return The file or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        try {
            return ReadAction.nonBlocking(() -> {
                File file = new File(path);
                return LocalFileSystem.getInstance().findFileByIoFile(file);
            }).executionContext(ApplicationManager.getApplication()::isReadAccessAllowed).submit()
                    .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Failed to find file by path: " + path, e);
            return null;
        }
    }

    /**
     * Finds a file by path relative to a base directory.
     *
     * @param baseDir The base directory.
     * @param path    The path.
     * @return The file or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByRelativePath(@NotNull VirtualFile baseDir, @NotNull String path) {
        if (!baseDir.isDirectory()) {
            LOG.error("Base directory is not a directory: " + baseDir.getPath());
            return null;
        }
        
        try {
            return ReadAction.nonBlocking(() -> {
                return baseDir.findFileByRelativePath(path);
            }).executionContext(ApplicationManager.getApplication()::isReadAccessAllowed).submit()
                    .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Failed to find file by relative path: " + path, e);
            return null;
        }
    }

    /**
     * Creates a file.
     *
     * @param directory The directory to create the file in.
     * @param fileName  The file name.
     * @param content   The file content.
     * @return The created file or null if creation failed.
     */
    @Nullable
    public static VirtualFile createFile(@NotNull VirtualFile directory, @NotNull String fileName, @NotNull String content) {
        if (!directory.isDirectory()) {
            LOG.error("Directory is not a directory: " + directory.getPath());
            return null;
        }
        
        try {
            return WriteAction.computeAndWait(() -> {
                VirtualFile file = directory.findChild(fileName);
                
                if (file == null) {
                    file = directory.createChildData(VirtualFileUtil.class, fileName);
                }
                
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                
                return file;
            });
        } catch (IOException e) {
            LOG.error("Failed to create file: " + fileName, e);
            return null;
        }
    }

    /**
     * Creates a directory.
     *
     * @param directory  The parent directory.
     * @param dirName    The directory name.
     * @return The created directory or null if creation failed.
     */
    @Nullable
    public static VirtualFile createDirectory(@NotNull VirtualFile directory, @NotNull String dirName) {
        if (!directory.isDirectory()) {
            LOG.error("Directory is not a directory: " + directory.getPath());
            return null;
        }
        
        try {
            return WriteAction.computeAndWait(() -> {
                VirtualFile dir = directory.findChild(dirName);
                
                if (dir == null) {
                    dir = directory.createChildDirectory(VirtualFileUtil.class, dirName);
                } else if (!dir.isDirectory()) {
                    LOG.error("Cannot create directory, file already exists: " + dir.getPath());
                    return null;
                }
                
                return dir;
            });
        } catch (IOException e) {
            LOG.error("Failed to create directory: " + dirName, e);
            return null;
        }
    }

    /**
     * Creates a directory structure.
     *
     * @param baseDir  The base directory.
     * @param path     The path.
     * @return The created directory or null if creation failed.
     */
    @Nullable
    public static VirtualFile createDirectoryStructure(@NotNull VirtualFile baseDir, @NotNull String path) {
        if (!baseDir.isDirectory()) {
            LOG.error("Base directory is not a directory: " + baseDir.getPath());
            return null;
        }
        
        if (path.isEmpty()) {
            return baseDir;
        }
        
        String[] parts = path.split("/");
        VirtualFile current = baseDir;
        
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            
            VirtualFile child = createDirectory(current, part);
            if (child == null) {
                return null;
            }
            
            current = child;
        }
        
        return current;
    }

    /**
     * Reads the content of a file.
     *
     * @param file The file.
     * @return The content or null if reading failed.
     */
    @Nullable
    public static String readFile(@NotNull VirtualFile file) {
        if (file.isDirectory()) {
            LOG.error("Cannot read content of a directory: " + file.getPath());
            return null;
        }
        
        try {
            return ReadAction.nonBlocking(() -> {
                try {
                    return VfsUtil.loadText(file);
                } catch (IOException e) {
                    LOG.error("Failed to read file: " + file.getPath(), e);
                    return null;
                }
            }).executionContext(ApplicationManager.getApplication()::isReadAccessAllowed).submit()
                    .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Failed to read file: " + file.getPath(), e);
            return null;
        }
    }

    /**
     * Writes content to a file.
     *
     * @param file    The file.
     * @param content The content.
     * @return Whether the operation succeeded.
     */
    public static boolean writeFile(@NotNull VirtualFile file, @NotNull String content) {
        if (file.isDirectory()) {
            LOG.error("Cannot write content to a directory: " + file.getPath());
            return false;
        }
        
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOG.error("Failed to write file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Deletes a file or directory.
     *
     * @param file The file or directory.
     * @return Whether the operation succeeded.
     */
    public static boolean delete(@NotNull VirtualFile file) {
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.delete(VirtualFileUtil.class);
                } catch (IOException e) {
                    LOG.error("Failed to delete file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Renames a file or directory.
     *
     * @param file    The file or directory.
     * @param newName The new name.
     * @return Whether the operation succeeded.
     */
    public static boolean rename(@NotNull VirtualFile file, @NotNull String newName) {
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.rename(VirtualFileUtil.class, newName);
                } catch (IOException e) {
                    LOG.error("Failed to rename file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Moves a file or directory.
     *
     * @param file        The file or directory.
     * @param newParent   The new parent directory.
     * @return Whether the operation succeeded.
     */
    public static boolean move(@NotNull VirtualFile file, @NotNull VirtualFile newParent) {
        if (!newParent.isDirectory()) {
            LOG.error("New parent is not a directory: " + newParent.getPath());
            return false;
        }
        
        try {
            WriteAction.runAndWait(() -> {
                try {
                    file.move(VirtualFileUtil.class, newParent);
                } catch (IOException e) {
                    LOG.error("Failed to move file: " + file.getPath(), e);
                    throw new RuntimeException(e);
                }
            });
            
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Copies a file or directory.
     *
     * @param file        The file or directory.
     * @param newParent   The new parent directory.
     * @param newName     The new name.
     * @return The copied file or null if copying failed.
     */
    @Nullable
    public static VirtualFile copy(@NotNull VirtualFile file, @NotNull VirtualFile newParent, @NotNull String newName) {
        if (!newParent.isDirectory()) {
            LOG.error("New parent is not a directory: " + newParent.getPath());
            return null;
        }
        
        try {
            return WriteAction.computeAndWait(() -> {
                try {
                    return file.copy(VirtualFileUtil.class, newParent, newName);
                } catch (IOException e) {
                    LOG.error("Failed to copy file: " + file.getPath(), e);
                    return null;
                }
            });
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Refreshes a file or directory.
     *
     * @param file            The file or directory.
     * @param recursive       Whether to refresh recursively.
     * @param asynchronous    Whether to refresh asynchronously.
     * @return Whether the operation succeeded.
     */
    public static boolean refresh(@NotNull VirtualFile file, boolean recursive, boolean asynchronous) {
        try {
            Application application = ApplicationManager.getApplication();
            
            if (asynchronous) {
                application.invokeLater(() -> file.refresh(false, recursive));
            } else {
                file.refresh(false, recursive);
            }
            
            return true;
        } catch (RuntimeException e) {
            LOG.error("Failed to refresh file: " + file.getPath(), e);
            return false;
        }
    }
}