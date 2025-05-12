package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Retrieves a mod file using its relative path from the project root.
     * 
     * @param project The project
     * @param relativePath The relative path from the project root
     * @return The VirtualFile if found, null otherwise
     */
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir == null) {
            LOG.warn("Project base directory not found");
            return null;
        }
        
        return baseDir.findFileByRelativePath(relativePath);
    }
    
    /**
     * Executes the given task on the UI thread.
     * This method is deprecated - use runOnUiThread instead.
     * 
     * @param runnable The task to execute
     */
    @Deprecated
    public static void executeOnUiThread(Runnable runnable) {
        runOnUiThread(runnable);
    }
    
    /**
     * Computes a result in a write action.
     * 
     * @param supplier The supplier that computes the result
     * @param <T> The type of the result
     * @return The computed result
     */
    public static <T> T computeInWriteAction(Supplier<T> supplier) {
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            return supplier.get();
        } else {
            try {
                return WriteAction.compute(supplier::get);
            } catch (Exception e) {
                LOG.error("Error executing write action", e);
                return null;
            }
        }
    }
    
    /**
     * Opens a file in the editor.
     * 
     * @param project The project
     * @param file The file to open
     * @param requestFocus Whether to request focus
     */
    public static void openFileInEditor(@NotNull Project project, VirtualFile file, boolean requestFocus) {
        if (file == null) {
            LOG.warn("Cannot open null file in editor");
            return;
        }
        
        executeOnUiThread(() -> {
            FileEditorManager.getInstance(project).openFile(file, requestFocus);
        });
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

    // Map to store listeners for removal
    private static final java.util.Map<String, com.intellij.openapi.vfs.VirtualFileListener> fileListeners = 
            new java.util.concurrent.ConcurrentHashMap<>();
            
    /**
     * Creates a virtual file listener.
     *
     * @param callback The callback to invoke when the file is changed.
     * @return The listener ID for later removal.
     */
    @NotNull
    public static String createVirtualFileListener(@NotNull Runnable callback) {
        String listenerId = "listener-" + System.currentTimeMillis();
        
        // Create a virtual file listener that invokes the callback on any file change
        com.intellij.openapi.vfs.VirtualFileListener listener = new com.intellij.openapi.vfs.VirtualFileListener() {
            public void contentsChanged(@NotNull VFileEvent event) {
                callback.run();
            }
            
            public void fileCreated(@NotNull VFileEvent event) {
                callback.run();
            }
            
            public void fileDeleted(@NotNull VFileEvent event) {
                callback.run();
            }
            
            public void fileMoved(@NotNull VFileMoveEvent event) {
                callback.run();
            }
            
            public void fileCopied(@NotNull VFileCopyEvent event) {
                callback.run();
            }
        };
        
        // Register the listener with the VFS
        com.intellij.openapi.vfs.VirtualFileManager.getInstance().addVirtualFileListener(listener);
        
        // Store the listener for later removal
        fileListeners.put(listenerId, listener);
        
        return listenerId;
    }

    /**
     * Removes a virtual file listener.
     *
     * @param listenerId The listener ID.
     */
    public static void removeVirtualFileListener(@NotNull String listenerId) {
        com.intellij.openapi.vfs.VirtualFileListener listener = fileListeners.remove(listenerId);
        if (listener != null) {
            com.intellij.openapi.vfs.VirtualFileManager.getInstance().removeVirtualFileListener(listener);
        }
    }
    
    /**
     * Gets the ContentFactory instance in a compatible way across IntelliJ versions.
     * Handles the deprecated ContentFactory.SERVICE.getInstance() pattern.
     *
     * @return The ContentFactory instance
     */
    @NotNull
    public static com.intellij.ui.content.ContentFactory getContentFactory() {
        // In IntelliJ 2025.1+, ContentFactory.SERVICE is deprecated
        // We use the new API: ContentFactory.getInstance()
        return com.intellij.ui.content.ContentFactory.getInstance();
    }
    
    /**
     * A utility method to handle project opened events in a compatible way.
     * This should be used by listeners that implement ProjectManagerListener.
     *
     * @param project The project that was opened
     * @param handler The handler to execute when a project is opened
     */
    public static void handleProjectOpened(@NotNull Project project, @NotNull java.util.function.Consumer<Project> handler) {
        LOG.info("Handling project opened event for project: " + project.getName());
        // Execute the handler
        handler.accept(project);
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
     * Executes a task on the UI thread and returns a value.
     * This method is deprecated - use other alternatives instead.
     *
     * @param <T> The return type.
     * @param task The supplier to execute.
     * @return The result of the task.
     */
    @Deprecated
    public static <T> T executeOnUiThreadAndGet(@NotNull java.util.function.Supplier<T> task) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return task.get();
        } else {
            final java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
            runOnUiThreadAndWait(() -> result.set(task.get()));
            return result.get();
        }
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
        // Use ApplicationInfo to get the current version
        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        
        // Get the full version and extract the major parts (year and first component)
        String fullVersion = appInfo.getFullVersion();
        
        try {
            // Parse version like "2025.1.1.1" to get "2025.1"
            String[] parts = fullVersion.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1];
            } else if (parts.length == 1) {
                return parts[0];
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse IDE version: " + fullVersion, e);
        }
        
        // Fall back to current API year
        return appInfo.getCurrentApiVersion();
    }
    
    /**
     * Computes a result in a write action.
     *
     * @param computable The computable task.
     * @param <T>        The return type.
     * @return The result of the computation.
     */
    /**
     * Computes a result in a write action using a Callable.
     * This is different from the Supplier version and is used when 
     * the computation might throw checked exceptions.
     * 
     * @param <T> The return type
     * @param computable The callable to execute
     * @return The result of the computation, or null if an error occurred
     */
    public static <T> T computeInWriteActionCallable(@NotNull Callable<T> computable) {
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
        
        try {
            // Split versions into components
            String[] currentParts = currentVersion.split("\\.");
            String[] minParts = minVersion.split("\\.");
            
            // Compare each part numerically
            for (int i = 0; i < Math.min(currentParts.length, minParts.length); i++) {
                int currentNum = extractNumber(currentParts[i]);
                int minNum = extractNumber(minParts[i]);
                
                if (currentNum < minNum) {
                    return false;
                } else if (currentNum > minNum) {
                    return true;
                }
                // If equal, continue to the next component
            }
            
            // If we get here, all compared components are equal
            // Longer version is considered newer (e.g., 2023.1.1 > 2023.1)
            return currentParts.length >= minParts.length;
        } catch (Exception e) {
            LOG.warn("Failed to compare version " + currentVersion + " with " + minVersion, e);
            // Default to true to avoid blocking functionality
            return true;
        }
    }
    
    /**
     * Extracts the numeric part from a version component.
     * For example, from "2023a" extracts 2023.
     * 
     * @param part The version component.
     * @return The numeric part.
     */
    private static int extractNumber(String part) {
        StringBuilder numStr = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                numStr.append(c);
            } else {
                break;
            }
        }
        
        return numStr.length() > 0 ? Integer.parseInt(numStr.toString()) : 0;
    }
    
    /**
     * Shows an information dialog as a compatibility wrapper around Messages.showInfoDialog
     * which has different parameters across IntelliJ versions.
     *
     * @param project The project
     * @param message The message to display
     * @param title The dialog title
     */
    public static void showInfoDialog(Project project, String message, String title) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Try the method with project parameter first (older versions)
                try {
                    Messages.class.getMethod("showInfoDialog", Project.class, String.class, String.class)
                            .invoke(null, project, message, title);
                    return;
                } catch (NoSuchMethodException e) {
                    // Method not found, try alternative
                    LOG.debug("showInfoDialog with Project parameter not found, trying alternative", e);
                }
                
                // Try the method without project parameter (newer versions)
                Messages.class.getMethod("showInfoDialog", String.class, String.class)
                        .invoke(null, message, title);
            } catch (Exception e) {
                LOG.error("Failed to show info dialog", e);
                // Fallback to simple notification if dialogs fail
                ModForgeNotificationService.getInstance().showInfoNotification(
                        project,
                        title,
                        message
                );
            }
        });
    }
    
    /**
     * Shows an error dialog as a compatibility wrapper around Messages.showErrorDialog
     * which has different parameters across IntelliJ versions.
     *
     * @param project The project
     * @param message The message to display
     * @param title The dialog title
     */
    public static void showErrorDialog(Project project, String message, String title) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Try the method with project parameter first (older versions)
                try {
                    Messages.class.getMethod("showErrorDialog", Project.class, String.class, String.class)
                            .invoke(null, project, message, title);
                    return;
                } catch (NoSuchMethodException e) {
                    // Method not found, try alternative
                    LOG.debug("showErrorDialog with Project parameter not found, trying alternative", e);
                }
                
                // Try the method without project parameter (newer versions)
                Messages.class.getMethod("showErrorDialog", String.class, String.class)
                        .invoke(null, message, title);
            } catch (Exception e) {
                LOG.error("Failed to show error dialog", e);
                // Fallback to simple notification if dialogs fail
                ModForgeNotificationService.getInstance().showErrorNotification(
                        project, 
                        title,
                        message
                );
            }
        });
    }
}