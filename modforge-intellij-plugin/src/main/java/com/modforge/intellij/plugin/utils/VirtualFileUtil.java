package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility class for working with virtual files.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class VirtualFileUtil {
    private static final Logger LOG = Logger.getInstance(VirtualFileUtil.class);
    
    private VirtualFileUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets a virtual file from a path.
     *
     * @param path The path.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * Gets a virtual file from a file.
     *
     * @param file The file.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByIoFile(@NotNull File file) {
        return LocalFileSystem.getInstance().findFileByIoFile(file);
    }

    /**
     * Creates a directory recursively if it doesn't exist.
     *
     * @param project The project.
     * @param basePath The base path.
     * @param directoryPath The directory path.
     * @return The created directory, or null if creation failed.
     */
    @Nullable
    public static PsiDirectory createDirectoryRecursively(
            @NotNull Project project,
            @NotNull String basePath,
            @NotNull String directoryPath) {
        
        try {
            // Find or create the base directory
            VirtualFile baseDir = findFileByPath(basePath);
            if (baseDir == null) {
                LOG.error("Base directory not found: " + basePath);
                return null;
            }
            
            // Split the directory path into components
            String[] pathComponents = directoryPath.split("/");
            
            return CompatibilityUtil.computeInWriteAction(() -> {
                try {
                    VirtualFile currentDir = baseDir;
                    PsiDirectory psiDir = PsiManager.getInstance(project).findDirectory(currentDir);
                    
                    // Create each directory in the path
                    for (String component : pathComponents) {
                        if (component.isEmpty()) continue;
                        
                        VirtualFile childDir = currentDir.findChild(component);
                        if (childDir == null) {
                            // Create the directory
                            childDir = currentDir.createChildDirectory(null, component);
                        }
                        
                        currentDir = childDir;
                        psiDir = PsiManager.getInstance(project).findDirectory(currentDir);
                    }
                    
                    return psiDir;
                } catch (IOException e) {
                    LOG.error("Failed to create directory recursively: " + directoryPath, e);
                    return null;
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to create directory recursively: " + directoryPath, e);
            return null;
        }
    }

    /**
     * Creates a file in a directory.
     *
     * @param directory The directory.
     * @param fileName The file name.
     * @param content The file content.
     * @return The created file, or null if creation failed.
     */
    @Nullable
    public static PsiFile createFile(
            @NotNull PsiDirectory directory,
            @NotNull String fileName,
            @NotNull String content) {
        
        try {
            return CompatibilityUtil.computeInWriteAction(() -> {
                // Check if the file already exists
                PsiFile existingFile = directory.findFile(fileName);
                if (existingFile != null) {
                    LOG.warn("File already exists: " + fileName);
                    
                    // Update the content of the existing file
                    Document document = FileDocumentManager.getInstance().getDocument(existingFile.getVirtualFile());
                    if (document != null) {
                        document.setText(content);
                    }
                    
                    return existingFile;
                }
                
                // Create the file
                PsiFile file = directory.createFile(fileName);
                
                // Set the content
                Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
                if (document != null) {
                    document.setText(content);
                }
                
                return file;
            });
        } catch (Exception e) {
            LOG.error("Failed to create file: " + fileName, e);
            return null;
        }
    }

    /**
     * Gets the content of a virtual file.
     *
     * @param file The virtual file.
     * @return The content, or null if reading failed.
     */
    @Nullable
    public static String getFileContent(@Nullable VirtualFile file) {
        if (file == null) return null;
        
        try {
            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to read file content", e);
            return null;
        }
    }

    /**
     * Writes content to a virtual file.
     *
     * @param file The virtual file.
     * @param content The content.
     * @return Whether writing succeeded.
     */
    public static boolean writeFileContent(@Nullable VirtualFile file, @NotNull String content) {
        if (file == null) return false;
        
        try {
            return CompatibilityUtil.computeInWriteAction(() -> {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                    return true;
                } catch (IOException e) {
                    LOG.error("Failed to write file content", e);
                    return false;
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to write file content", e);
            return false;
        }
    }

    /**
     * Deletes a virtual file.
     *
     * @param file The virtual file.
     * @return Whether deletion succeeded.
     */
    public static boolean deleteFile(@Nullable VirtualFile file) {
        if (file == null) return false;
        
        try {
            return CompatibilityUtil.computeInWriteAction(() -> {
                try {
                    file.delete(null);
                    return true;
                } catch (IOException e) {
                    LOG.error("Failed to delete file", e);
                    return false;
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to delete file", e);
            return false;
        }
    }

    /**
     * Gets all child files in a directory, recursively.
     *
     * @param directory The directory.
     * @return The list of files.
     */
    @NotNull
    public static List<VirtualFile> getAllFiles(@Nullable VirtualFile directory) {
        List<VirtualFile> result = new ArrayList<>();
        if (directory == null) return result;
        
        collectFiles(directory, result);
        return result;
    }

    /**
     * Recursive helper to collect files.
     *
     * @param directory The directory.
     * @param result The result list.
     */
    private static void collectFiles(@NotNull VirtualFile directory, @NotNull List<VirtualFile> result) {
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectFiles(child, result);
            } else {
                result.add(child);
            }
        }
    }

    /**
     * Gets all files with a specific extension in a directory, recursively.
     *
     * @param directory The directory.
     * @param extension The extension.
     * @return The list of files.
     */
    @NotNull
    public static List<VirtualFile> getFilesByExtension(
            @Nullable VirtualFile directory,
            @NotNull String extension) {
        
        List<VirtualFile> result = new ArrayList<>();
        if (directory == null) return result;
        
        collectFilesByExtension(directory, extension, result);
        return result;
    }

    /**
     * Recursive helper to collect files by extension.
     *
     * @param directory The directory.
     * @param extension The extension.
     * @param result The result list.
     */
    private static void collectFilesByExtension(
            @NotNull VirtualFile directory,
            @NotNull String extension,
            @NotNull List<VirtualFile> result) {
        
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectFilesByExtension(child, extension, result);
            } else if (extension.equalsIgnoreCase(child.getExtension())) {
                result.add(child);
            }
        }
    }

    /**
     * Gets all files matching a name pattern in a directory, recursively.
     *
     * @param directory The directory.
     * @param pattern The name pattern.
     * @return The list of files.
     */
    @NotNull
    public static List<VirtualFile> getFilesByNamePattern(
            @Nullable VirtualFile directory,
            @NotNull String pattern) {
        
        List<VirtualFile> result = new ArrayList<>();
        if (directory == null) return result;
        
        collectFilesByNamePattern(directory, pattern, result);
        return result;
    }

    /**
     * Recursive helper to collect files by name pattern.
     *
     * @param directory The directory.
     * @param pattern The name pattern.
     * @param result The result list.
     */
    private static void collectFilesByNamePattern(
            @NotNull VirtualFile directory,
            @NotNull String pattern,
            @NotNull List<VirtualFile> result) {
        
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectFilesByNamePattern(child, pattern, result);
            } else if (child.getName().matches(pattern)) {
                result.add(child);
            }
        }
    }
}