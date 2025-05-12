package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Utility class for handling compatibility issues between different IntelliJ IDEA versions.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class CompatibilityUtil {
    private static final Logger LOG = Logger.getInstance(CompatibilityUtil.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private CompatibilityUtil() {
        // Utility class
    }

    /**
     * Gets the project base directory.
     * Replacement for deprecated Project.getBaseDir()
     *
     * @param project The project.
     * @return The project base directory, or null if not found.
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@Nullable Project project) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        
        // Use ProjectUtil.guessProjectDir which is the recommended way in 2025.1
        return ProjectUtil.guessProjectDir(project);
    }

    /**
     * Runs a task under a read action.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return The result of the task.
     */
    @Nullable
    public static <T> T runUnderReadAction(@NotNull Supplier<T> task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isReadAccessAllowed()) {
            return task.get();
        } else {
            return ReadAction.compute(task::get);
        }
    }

    /**
     * Runs a task under a write action.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return The result of the task.
     * @throws Exception If an error occurs.
     */
    @Nullable
    public static <T> T runUnderWriteAction(@NotNull Callable<T> task) throws Exception {
        Application application = ApplicationManager.getApplication();
        
        if (application.isWriteAccessAllowed()) {
            return task.call();
        } else {
            return WriteAction.compute(task::call);
        }
    }

    /**
     * Runs a task under a read action asynchronously.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return A future that completes with the result of the task.
     */
    @NotNull
    public static <T> CompletableFuture<T> runUnderReadActionAsync(@NotNull Supplier<T> task) {
        return ReadAction.nonBlocking(task::get).submit(ApplicationManager.getApplication().getExecutorService());
    }

    /**
     * Runs a task under a write action asynchronously.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return A future that completes with the result of the task.
     */
    @NotNull
    public static <T> CompletableFuture<T> runUnderWriteActionAsync(@NotNull Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return runUnderWriteAction(task);
            } catch (Exception e) {
                LOG.error("Failed to run task under write action", e);
                throw new RuntimeException(e);
            }
        }, ApplicationManager.getApplication().getExecutorService());
    }

    /**
     * Creates a virtual file listener.
     *
     * @param callback The callback to invoke when the file is changed.
     * @return The listener ID for later removal.
     */
    @NotNull
    public static String createVirtualFileListener(@NotNull Runnable callback) {
        // TODO: This is a mock implementation.
        // In the real code, we would register a VFS listener.
        return "listener-" + System.currentTimeMillis();
    }

    /**
     * Removes a virtual file listener.
     *
     * @param listenerId The listener ID.
     */
    public static void removeVirtualFileListener(@NotNull String listenerId) {
        // TODO: This is a mock implementation.
        // In the real code, we would unregister the VFS listener.
    }

    /**
     * Runs a task on the UI thread.
     *
     * @param task The task to run.
     */
    public static void runOnUiThread(@NotNull Runnable task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isDispatchThread()) {
            task.run();
        } else {
            application.invokeLater(task);
        }
    }
    
    /**
     * Executes a task on the UI thread.
     * This is an alias for runOnUiThread to maintain backward compatibility.
     *
     * @param task The task to run.
     */
    public static void executeOnUiThread(@NotNull Runnable task) {
        runOnUiThread(task);
    }

    /**
     * Runs a task on the UI thread and waits for it to complete.
     *
     * @param task The task to run.
     */
    public static void runOnUiThreadAndWait(@NotNull Runnable task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isDispatchThread()) {
            task.run();
        } else {
            try {
                application.invokeAndWait(task);
            } catch (Exception e) {
                LOG.error("Failed to run task on UI thread", e);
            }
        }
    }

    /**
     * Gets the IntelliJ IDEA major version.
     *
     * @return The major version.
     */
    @NotNull
    public static String getIdeaMajorVersion() {
        // TODO: This is a mock implementation.
        // In the real code, we would get the actual IDE version.
        return "2025.1";
    }
    
    /**
     * Computes a result in a write action.
     *
     * @param computable The computable task.
     * @param <T>        The return type.
     * @return The result of the computation.
     */
    @Nullable
    public static <T> T computeInWriteAction(@NotNull Callable<T> computable) {
        try {
            return WriteAction.compute(computable::call);
        } catch (Exception e) {
            LOG.error("Error executing write action", e);
            return null;
        }
    }
    
    /**
     * Refreshes all files in the project.
     *
     * @param project The project.
     */
    public static void refreshAll(@NotNull Project project) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir != null) {
            baseDir.refresh(true, true);
        }
    }
    
    /**
     * Checks if the current IntelliJ IDEA version is at least the given version.
     *
     * @param minVersion The minimum version.
     * @return Whether the current version is at least the given version.
     */
    public static boolean isIdeaVersionAtLeast(@NotNull String minVersion) {
        String currentVersion = getIdeaMajorVersion();
        
        // Simple version comparison for now
        return currentVersion.compareTo(minVersion) >= 0;
    }
    
    /**
     * Opens a file in the editor.
     *
     * @param project       The project.
     * @param file          The file to open.
     * @param requestFocus  Whether to request focus.
     * @return Whether the file was opened.
     */
    public static boolean openFileInEditor(
            @NotNull Project project,
            @NotNull VirtualFile file,
            boolean requestFocus) {
        
        if (!file.isValid()) {
            LOG.warn("Cannot open invalid file: " + file.getPath());
            return false;
        }
        
        runOnUiThread(() -> {
            try {
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                        .openFile(file, requestFocus);
            } catch (Exception e) {
                LOG.error("Failed to open file in editor: " + file.getPath(), e);
            }
        });
        
        return true;
    }
    
    /**
     * Gets a mod file by relative path.
     *
     * @param project      The project.
     * @param relativePath The relative path.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir == null) {
            LOG.warn("Project base directory not found");
            return null;
        }
        
        return baseDir.findFileByRelativePath(relativePath);
    }}