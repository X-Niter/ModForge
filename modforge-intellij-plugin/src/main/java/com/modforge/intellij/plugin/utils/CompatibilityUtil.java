package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for handling compatibility with different IntelliJ versions.
 * This helps maintain backward compatibility while supporting newer IntelliJ API.
 * 
 * Works with both older IntelliJ versions and specifically supports compatibility
 * with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).
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
    
    /**
     * Runs an action in read access with compatibility for different IntelliJ versions.
     * This wraps ApplicationManager.getApplication().runReadAction() in a way that works
     * across multiple IntelliJ versions.
     *
     * @param supplier the supplier to execute in read action
     * @param <T> the return type
     * @return the result of the read action
     */
    public static <T> T runReadAction(@NotNull Supplier<T> supplier) {
        try {
            // Using ReadAction for 2025.1.1.1 compatibility
            return ReadAction.compute(supplier::get);
        } catch (Throwable e) {
            // Fallback to traditional approach for older IntelliJ versions
            return ApplicationManager.getApplication().runReadAction(supplier::get);
        }
    }
    
    /**
     * Gets a PsiFile from a VirtualFile with compatibility across IntelliJ versions.
     * This wraps PsiManager.getInstance(project).findFile(file) with proper read action.
     *
     * @param project the project
     * @param file the virtual file
     * @return the PsiFile, or null if not available
     */
    @Nullable
    public static PsiFile getPsiFile(@NotNull Project project, @NotNull VirtualFile file) {
        return runReadAction(() -> PsiManager.getInstance(project).findFile(file));
    }
    
    /**
     * Opens a file in the editor in a way that is compatible across IntelliJ versions.
     * This wraps FileEditorManager.getInstance(project).openFile(file, true) with better compatibility.
     *
     * @param project the project
     * @param file the file to open
     * @param requestFocus whether to request focus
     * @return the FileEditors that were opened
     */
    @NotNull
    public static FileEditor[] openFileInEditor(@NotNull Project project, @NotNull VirtualFile file, boolean requestFocus) {
        try {
            // Try modern approach first - better in 2025.1+
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
            return FileEditorManager.getInstance(project).openEditor(descriptor, requestFocus);
        } catch (Throwable e) {
            // Fallback to traditional method
            return FileEditorManager.getInstance(project).openFile(file, requestFocus);
        }
    }
    
    /**
     * Checks if a file is open in the editor with compatibility across IntelliJ versions.
     *
     * @param project the project
     * @param file the file to check
     * @return true if the file is open
     */
    public static boolean isFileOpenInEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return FileEditorManager.getInstance(project).isFileOpen(file);
    }
    
    /**
     * Gets the document for a virtual file with proper error handling and compatibility.
     *
     * @param file the virtual file
     * @return the document, or null if not available
     */
    @Nullable
    public static Document getDocument(@NotNull VirtualFile file) {
        return runReadAction(() -> FileDocumentManager.getInstance().getDocument(file));
    }
    
    /**
     * Runs a task on the UI thread with compatibility across IntelliJ versions.
     * This wraps ApplicationManager.getApplication().invokeLater() with better compatibility.
     *
     * @param runnable the task to run on the UI thread
     */
    public static void runOnUIThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }
    
    /**
     * Runs a task on the UI thread with compatibility across IntelliJ versions
     * and ensures it's executed only when the project is not disposed.
     *
     * @param project the project
     * @param runnable the task to run on the UI thread
     */
    public static void runOnUIThread(@NotNull Project project, @NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                runnable.run();
            }
        });
    }
}