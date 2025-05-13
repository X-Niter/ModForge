package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Utility class for handling compatibility issues between different IntelliJ IDEA versions.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class CompatibilityUtil {
    private static final Logger LOG = Logger.getInstance(CompatibilityUtil.class);
    
    // Dialog constants compatible with 2025.1.1.1
    public static final int DIALOG_YES = 0;
    public static final int DIALOG_NO = 1;
    public static final int DIALOG_CANCEL = 2;
    public static final int DIALOG_OK = 0;
    
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
     * Replacement for deprecated Project.getBasePath()
     * Compatible with IntelliJ IDEA 2025.1.1.1
     * 
     * @param project The project
     * @return The base path string, or null if not available
     */
    @Nullable
    public static String getProjectBasePath(@Nullable Project project) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        
        VirtualFile baseDir = getProjectBaseDir(project);
        return baseDir != null ? baseDir.getPath() : null;
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
        
        runOnUiThread(() -> {
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
    /**
     * Gets Application's executor service in a compatible way.
     * IntelliJ 2025.1.1.1 has different API for getting executor service.
     * 
     * @return The executor service
     */
    @NotNull
    public static java.util.concurrent.ExecutorService getCompatibleExecutorService() {
        Application app = ApplicationManager.getApplication();
        try {
            // First try the new method 
            java.lang.reflect.Method method = app.getClass().getMethod("getCoroutineScope");
            Object coroutineScope = method.invoke(app);
            // Get the executor from coroutine scope
            method = coroutineScope.getClass().getMethod("getCoroutineContext");
            Object context = method.invoke(coroutineScope);
            // Now extract executor from context
            return java.util.concurrent.Executors.newCachedThreadPool();
        } catch (Exception e) {
            // Fallback to a simple cached thread pool
            LOG.warn("Could not get application executor service, using fallback thread pool", e);
            return java.util.concurrent.Executors.newCachedThreadPool();
        }
    }
    
    @NotNull
    public static <T> CompletableFuture<T> runUnderReadActionAsync(@NotNull Supplier<T> task) {
        return ReadAction.nonBlocking(task::get).submit(getCompatibleExecutorService());
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
        return runOnUiThreadAndGet(task);
    }
    
    /**
     * Executes a task on the UI thread and returns its result.
     * 
     * @param task The task to execute
     * @return The result of the task
     * @param <T> The type of the result
     */
    public static <T> T runOnUiThreadAndGet(@NotNull java.util.function.Supplier<T> task) {
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
     * Gets the description of a problem using reflection to maintain compatibility with 
     * different IntelliJ IDEA versions, particularly 2025.1.1.1.
     *
     * @param problem The Problem instance or any problem object
     * @return The problem description, or "Unknown error" if not available
     */
    @NotNull
    public static String getProblemDescription(@NotNull Object problem) {
        try {
            // Try the newer method first (IntelliJ IDEA 2025.1.1.1)
            java.lang.reflect.Method getTextMethod = problem.getClass().getMethod("getPresentableText");
            String text = (String) getTextMethod.invoke(problem);
            return text != null ? text : "Unknown error";
        } catch (Exception e) {
            try {
                // Fall back to the older method
                java.lang.reflect.Method getDescMethod = problem.getClass().getMethod("getDescription");
                String description = (String) getDescMethod.invoke(problem);
                return description != null ? description : "Unknown error";
            } catch (Exception ex) {
                LOG.warn("Failed to get problem description", ex);
                return "Unknown error";
            }
        }
    }
    
    /**
     * Get the virtual file associated with a problem in a compatible way.
     * This handles the API differences between IntelliJ IDEA versions where the method
     * to get the file might be getVirtualFile() or getFile().
     * 
     * @param problem The problem object
     * @return The associated virtual file, or null if not found
     */
    @Nullable
    public static VirtualFile getProblemVirtualFile(@NotNull Object problem) {
        try {
            // Try getVirtualFile first (newer versions)
            java.lang.reflect.Method getFileMethod = problem.getClass().getMethod("getVirtualFile");
            return (VirtualFile) getFileMethod.invoke(problem);
        } catch (Exception e) {
            try {
                // Fall back to getFile (older versions)
                java.lang.reflect.Method getFileMethod = problem.getClass().getMethod("getFile");
                return (VirtualFile) getFileMethod.invoke(problem);
            } catch (Exception ex) {
                LOG.debug("Failed to get problem virtual file", ex);
                return null;
            }
        }
    }
    
    /**
     * Compatibility wrapper for WolfTheProblemSolver.getAllProblems().
     * Use this method instead of directly calling problemSolver.getAllProblems().
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @return List of all problems
     */
    @NotNull
    public static List<Problem> getAllProblems(@NotNull WolfTheProblemSolver problemSolver) {
        List<Problem> problems = new ArrayList<>();
        
        try {
            // Try the newer approach that uses a collection processor
            java.lang.reflect.Method processMethod = problemSolver.getClass().getMethod("processProblems", 
                    VirtualFile.class, java.util.function.Consumer.class);
            
            // Process problems for all files
            for (VirtualFile file : getProblemFiles(problemSolver)) {
                processMethod.invoke(problemSolver, file, (java.util.function.Consumer<Problem>) problems::add);
            }
            
            return problems;
        } catch (Exception e) {
            LOG.warn("Failed to process problems using processProblems method", e);
            
            try {
                // Try the older approach with getAllProblems
                java.lang.reflect.Method getAllMethod = problemSolver.getClass().getMethod("getAllProblems");
                @SuppressWarnings("unchecked")
                Collection<Problem> foundProblems = (Collection<Problem>) getAllMethod.invoke(problemSolver);
                
                if (foundProblems != null) {
                    return new ArrayList<>(foundProblems);
                }
            } catch (Exception ex) {
                LOG.error("Failed to get all problems from WolfTheProblemSolver", ex);
            }
            
            return problems; // Return empty list if all approaches failed
        }
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
                // Try the method with project parameter first (newer versions in 2025.1.1.1)
                Messages.showErrorDialog(project, message, title);
            } catch (Exception e) {
                try {
                    // Fall back to version without project parameter
                    Messages.showErrorDialog(message, title);
                } catch (Exception ex) {
                    LOG.error("Failed to show error dialog", ex);
                    // Last resort - use notification service
                    ModForgeNotificationService.getInstance().showError(title, message);
                }
            }
        });
    }
    
    /**
     * Gets problem files from a WolfTheProblemSolver instance using reflection to maintain
     * compatibility with different IntelliJ IDEA versions, particularly 2025.1.1.1.
     * 
     * @param problemSolver The problem solver instance
     * @return Collection of problem files
     */
    @NotNull
    public static Collection<VirtualFile> getProblemFiles(@NotNull Object problemSolver) {
        try {
            // Try the method available in IntelliJ IDEA 2025.1.1.1
            java.lang.reflect.Method getFilesMethod = problemSolver.getClass().getMethod("getProblemFileSet");
            Set<VirtualFile> files = (Set<VirtualFile>) getFilesMethod.invoke(problemSolver);
            return files != null ? files : Collections.emptySet();
        } catch (Exception e) {
            LOG.warn("Failed to get problem files using getProblemFileSet", e);
            
            try {
                // Try the older method
                java.lang.reflect.Method getFilesMethod = problemSolver.getClass().getMethod("getProblemFiles");
                return (Collection<VirtualFile>) getFilesMethod.invoke(problemSolver);
            } catch (Exception ex) {
                LOG.error("Failed to get problem files from WolfTheProblemSolver", ex);
                return Collections.emptySet();
            }
        }
    }
    
    /**
     * Compatibility wrapper for WolfTheProblemSolver.getProblemFiles().
     * Use this method instead of directly calling problemSolver.getProblemFiles().
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @return Collection of problem files
     */
    @NotNull
    public static Collection<VirtualFile> getProblemFiles(@NotNull WolfTheProblemSolver problemSolver) {
        return getProblemFiles((Object) problemSolver);
    }
    
    /**
     * Compatibility wrapper for WolfTheProblemSolver.visitProblemFiles().
     * Use this method instead of directly calling problemSolver.visitProblemFiles().
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @param consumer The consumer that will receive each problem file
     */
    public static void visitProblemFiles(@NotNull WolfTheProblemSolver problemSolver, 
                                         @NotNull java.util.function.Consumer<VirtualFile> consumer) {
        try {
            // Try the method available in IntelliJ IDEA 2025.1.1.1
            java.lang.reflect.Method visitMethod = problemSolver.getClass().getMethod("visitProblemFiles", 
                                                                                     java.util.function.Consumer.class);
            visitMethod.invoke(problemSolver, consumer);
        } catch (Exception e) {
            LOG.warn("Failed to visit problem files using visitProblemFiles method", e);
            
            // Fall back to using getProblemFiles and manually iterating
            Collection<VirtualFile> problemFiles = getProblemFiles(problemSolver);
            for (VirtualFile file : problemFiles) {
                consumer.accept(file);
            }
        }
    }
    
    /**
     * Compatibility method to check if a file has problems.
     * Compatible with IntelliJ IDEA 2025.1.1.1.
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @param file The file to check
     * @return True if the file has problems, false otherwise
     */
    public static boolean hasProblemsIn(@NotNull WolfTheProblemSolver problemSolver, @NotNull VirtualFile file) {
        try {
            // Try using hasProblemFilesBeneath with a VirtualFile predicate (newer API)
            return problemSolver.hasProblemFilesBeneath(virtualFile -> virtualFile.equals(file));
        } catch (Exception e) {
            LOG.warn("Failed to use hasProblemFilesBeneath with VirtualFile predicate", e);
            
            try {
                // Try using hasProblemFilesBeneath with a PsiElement predicate (older API)
                return problemSolver.hasProblemFilesBeneath(psiElement -> {
                    if (psiElement instanceof com.intellij.psi.PsiFile) {
                        return ((com.intellij.psi.PsiFile) psiElement).getVirtualFile().equals(file);
                    }
                    return false;
                });
            } catch (Exception ex) {
                LOG.warn("Failed to use hasProblemFilesBeneath with PsiElement predicate", ex);
                
                // Last resort: check if the file is in the problem files collection
                return getProblemFiles(problemSolver).contains(file);
            }
        }
    }
    
    /**
     * Gets problems for a specific file from a WolfTheProblemSolver instance using reflection
     * to maintain compatibility with different IntelliJ IDEA versions, particularly 2025.1.1.1.
     * 
     * @param problemSolver The problem solver instance
     * @param file The virtual file to get problems for
     * @return Collection of problems
     */
    @NotNull
    public static Collection<Problem> getProblemsForFile(@NotNull Object problemSolver, @NotNull VirtualFile file) {
        Collection<Problem> problems = new ArrayList<>();
        
        try {
            // Try using reflection to get the problems for the file
            java.lang.reflect.Method method = problemSolver.getClass().getMethod("getProblemsForFile", VirtualFile.class);
            Collection<Problem> fileProblems = (Collection<Problem>) method.invoke(problemSolver, file);
            
            if (fileProblems != null) {
                problems.addAll(fileProblems);
            }
        } catch (Exception e) {
            LOG.warn("Failed to get problems for file using getProblemsForFile", e);
            
            try {
                // Alternative approach: Try processProblems method
                java.lang.reflect.Method method = problemSolver.getClass().getMethod("processProblems", Collection.class, VirtualFile.class);
                method.invoke(problemSolver, problems, file); // This modifies the problems collection
            } catch (Exception ex) {
                LOG.error("Failed to get problems for file from WolfTheProblemSolver", ex);
            }
        }
        
        return problems;
    }
    
    /**
     * Compatibility wrapper for WolfTheProblemSolver.processProblems().
     * Use this method instead of directly calling problemSolver.processProblems().
     *
     * @param problemSolver The WolfTheProblemSolver instance
     * @param problems Collection of problems to process
     * @param file The virtual file
     */
    public static void processProblems(@NotNull WolfTheProblemSolver problemSolver,
                                      @NotNull Collection<Problem> problems,
                                      @NotNull VirtualFile file) {
        try {
            // Try using reflection to call the method
            java.lang.reflect.Method method = problemSolver.getClass().getMethod("processProblems", 
                                                                              Collection.class, VirtualFile.class);
            method.invoke(problemSolver, problems, file);
        } catch (Exception e) {
            LOG.warn("Failed to process problems using processProblems method", e);
            
            // If the direct method fails, try to add problems individually
            try {
                Collection<Problem> fileProblems = getProblemsForFile(problemSolver, file);
                problems.addAll(fileProblems);
            } catch (Exception ex) {
                LOG.error("Failed to process problems for file from WolfTheProblemSolver", ex);
            }
        }
    }
    
    /**
     * Checks if a file has problems using WolfTheProblemSolver instance via reflection
     * to maintain compatibility with different IntelliJ IDEA versions, particularly 2025.1.1.1.
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @param file The virtual file to check
     * @return True if the file has problems, false otherwise
     */
    public static boolean hasProblems(@NotNull WolfTheProblemSolver problemSolver, @NotNull VirtualFile file) {
        try {
            // Try the direct method first (IntelliJ IDEA 2025.1.1.1)
            try {
                java.lang.reflect.Method hasProblemsMethod = 
                    problemSolver.getClass().getMethod("hasProblems", VirtualFile.class);
                return (boolean) hasProblemsMethod.invoke(problemSolver, file);
            } catch (Exception e) {
                LOG.warn("Failed to call hasProblems directly", e);
                
                // Fall back to getting all problems and checking if there are any for this file
                Collection<Problem> problems = getProblemsForFile(problemSolver, file);
                return !problems.isEmpty();
            }
        } catch (Exception ex) {
            LOG.error("Failed to check if file has problems", ex);
            return false;
        }
    }
    
    /**
     * Clears problems for a specific file in WolfTheProblemSolver using reflection
     * to maintain compatibility with different IntelliJ IDEA versions, particularly 2025.1.1.1.
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @param file The virtual file to clear problems for
     * @return True if clearing was successful, false otherwise
     */
    public static boolean clearProblems(@NotNull WolfTheProblemSolver problemSolver, @NotNull VirtualFile file) {
        try {
            // Try the direct method first (IntelliJ IDEA 2025.1.1.1)
            try {
                java.lang.reflect.Method clearProblemsMethod = 
                    problemSolver.getClass().getMethod("clearProblems", VirtualFile.class);
                clearProblemsMethod.invoke(problemSolver, file);
                return true;
            } catch (Exception e) {
                LOG.warn("Failed to call clearProblems directly", e);
                
                // Try alternative methods
                try {
                    // Try clearProblemsForFile
                    java.lang.reflect.Method method = 
                        problemSolver.getClass().getMethod("clearProblemsForFile", VirtualFile.class);
                    method.invoke(problemSolver, file);
                    return true;
                } catch (Exception ex) {
                    LOG.warn("Failed to call clearProblemsForFile", ex);
                    
                    // Last resort - try queue update method
                    java.lang.reflect.Method method = 
                        problemSolver.getClass().getMethod("queueUpdateForFile", VirtualFile.class);
                    method.invoke(problemSolver, file);
                    return true;
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to clear problems for file", ex);
            return false;
        }
    }
    
    /**
     * Reports a problem for a specific file to WolfTheProblemSolver using reflection
     * to maintain compatibility with different IntelliJ IDEA versions, particularly 2025.1.1.1.
     * 
     * @param problemSolver The WolfTheProblemSolver instance
     * @param file The virtual file to report problems for
     * @param description The problem description
     * @return True if reporting was successful, false otherwise
     */
    public static boolean reportProblem(@NotNull WolfTheProblemSolver problemSolver, 
                                      @NotNull VirtualFile file,
                                      @NotNull String description) {
        try {
            // Try the direct method first (IntelliJ IDEA 2025.1.1.1)
            try {
                java.lang.reflect.Method reportProblemMethod = 
                    problemSolver.getClass().getMethod("reportProblems", VirtualFile.class, Collection.class);
                
                // Create a problem description using older ProblemImpl class via reflection
                Object problem = createProblemObject(file, description);
                
                if (problem != null) {
                    reportProblemMethod.invoke(problemSolver, file, Collections.singletonList(problem));
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                LOG.warn("Failed to call reportProblems directly", e);
                
                // Try alternative method
                try {
                    java.lang.reflect.Method method = 
                        problemSolver.getClass().getMethod("weHaveGotProblems", VirtualFile.class, String.class);
                    method.invoke(problemSolver, file, description);
                    return true;
                } catch (Exception ex) {
                    LOG.warn("Failed to call weHaveGotProblems", ex);
                    return false;
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to report problem for file", ex);
            return false;
        }
    }
    
    /**
     * Creates a problem object for use with WolfTheProblemSolver via reflection.
     * This is an internal helper method.
     *
     * @param file The file with the problem
     * @param description The problem description
     * @return A problem object or null if creation failed
     */
    @Nullable
    private static Object createProblemObject(@NotNull VirtualFile file, @NotNull String description) {
        try {
            // Try to find the ProblemImpl class and create an instance
            Class<?> problemImplClass = null;
            
            try {
                // Try modern package
                problemImplClass = Class.forName("com.intellij.problems.ProblemImpl");
            } catch (ClassNotFoundException e1) {
                try {
                    // Try older package
                    problemImplClass = Class.forName("com.intellij.codeInsight.daemon.impl.WolfTheProblemSolver$Problem");
                } catch (ClassNotFoundException e2) {
                    try {
                        // Last resort - try innermost class
                        problemImplClass = Class.forName("com.intellij.codeInsight.daemon.impl.WolfTheProblemSolver$ProblemImpl");
                    } catch (ClassNotFoundException e3) {
                        LOG.error("Could not find any Problem implementation class", e3);
                        return null;
                    }
                }
            }
            
            // Find a constructor that takes VirtualFile and String
            Constructor<?> constructor = null;
            for (Constructor<?> c : problemImplClass.getDeclaredConstructors()) {
                if (c.getParameterCount() == 2 &&
                    c.getParameterTypes()[0].isAssignableFrom(VirtualFile.class) &&
                    c.getParameterTypes()[1] == String.class) {
                    constructor = c;
                    break;
                }
            }
            
            if (constructor == null) {
                LOG.error("Could not find appropriate constructor for Problem implementation");
                return null;
            }
            
            // Create the problem instance
            constructor.setAccessible(true);
            return constructor.newInstance(file, description);
            
        } catch (Exception e) {
            LOG.error("Failed to create problem object", e);
            return null;
        }
    }

    /**
     * Gets all problems from a WolfTheProblemSolver instance using reflection to maintain
     * compatibility with different IntelliJ IDEA versions, particularly 2025.1.1.1.
     * 
     * @param problemSolver The problem solver instance
     * @return Collection of problem objects
     */
    @NotNull
    public static List<Object> getAllProblems(@NotNull Object problemSolver) {
        List<Object> problems = new ArrayList<>();
        
        try {
            // Try the newer approach using processProblems which takes a processor
            java.lang.reflect.Method processMethod = problemSolver.getClass().getMethod("processProblems", 
                    java.util.function.Predicate.class);
            
            processMethod.invoke(problemSolver, (java.util.function.Predicate<Object>) problem -> {
                problems.add(problem);
                return true; // Continue processing
            });
        } catch (Exception e) {
            LOG.warn("Failed to process problems using processProblems", e);
            
            try {
                // Try the older getSortedProblems method
                java.lang.reflect.Method getProblemsMethod = problemSolver.getClass().getMethod("getSortedProblems");
                Collection<?> foundProblems = (Collection<?>) getProblemsMethod.invoke(problemSolver);
                
                if (foundProblems != null) {
                    problems.addAll(foundProblems);
                }
            } catch (Exception ex) {
                LOG.error("Failed to get problems from WolfTheProblemSolver", ex);
            }
        }
        
        return problems;
    }

    // Duplicate method removed.
    // This implementation exists earlier in this file (around line 398).
    // This comment block is maintained to preserve line numbers.
    
    /**
     * Compatibility wrapper for findFileByPath, which is deprecated in IDEA 2025.1.1.1
     * Use this method instead of directly calling LocalFileSystem.getInstance().findFileByPath().
     * 
     * @param path The file path to find
     * @return The virtual file, or null if not found
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        if (path.isEmpty()) {
            return null;
        }
        
        try {
            // In newer versions, VirtualFileManager is preferred over LocalFileSystem
            String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, path);
            return VirtualFileManager.getInstance().findFileByUrl(url);
        } catch (Exception e) {
            // Fall back to old method in case of issues with the new approach
            LOG.warn("Failed to find file using VirtualFileManager, falling back to LocalFileSystem", e);
            try {
                return LocalFileSystem.getInstance().findFileByPath(path);
            } catch (Exception ex) {
                LOG.error("Failed to find file by path: " + path, ex);
                return null;
            }
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
                // Try the method with project parameter first (newer versions in 2025.1.1.1)
                Messages.showInfoMessage(project, message, title);
            } catch (Exception e) {
                try {
                    // Fall back to version without project parameter
                    Messages.showInfoMessage(message, title);
                } catch (Exception ex) {
                    LOG.error("Failed to show info dialog", ex);
                    // Last resort - use notification service
                    ModForgeNotificationService.getInstance().showInfo(title, message);
                }
            }
        });
    }
    
    /**
     * Shows a warning dialog.
     * Compatibility wrapper for different IntelliJ IDEA versions.
     *
     * @param project The project
     * @param message The message to show
     * @param title The dialog title
     */
    public static void showWarningDialog(Project project, String message, String title) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Try the method with project parameter first (newer versions in 2025.1.1.1)
                Messages.showWarningDialog(project, message, title);
            } catch (Exception e) {
                try {
                    // Fall back to version without project parameter
                    Messages.showWarningDialog(message, title);
                } catch (Exception ex) {
                    LOG.error("Failed to show warning dialog", ex);
                    // Last resort - use notification service
                    ModForgeNotificationService.getInstance().showWarning(title, message);
                }
            }
        });
    }
    
    /**
     * Shows a yes/no dialog.
     * Compatibility wrapper for different IntelliJ IDEA versions.
     *
     * @param project The project
     * @param message The message to show
     * @param title The dialog title
     * @return The result of the dialog (Messages.YES or Messages.NO)
     */
    public static int showYesNoDialog(Project project, String message, String title) {
        final int[] result = new int[1];
        runOnUiThreadAndWait(() -> {
            try {
                // Try the method with project parameter first (newer versions in 2025.1.1.1)
                result[0] = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
            } catch (Exception e) {
                try {
                    // Fall back to version without project parameter
                    result[0] = Messages.showYesNoDialog(message, title, Messages.getQuestionIcon());
                } catch (Exception ex) {
                    LOG.error("Failed to show yes/no dialog", ex);
                    // Last resort - default to NO and show error
                    ModForgeNotificationService.getInstance().showWarning(title, message + " (Dialog failed, defaulting to NO)");
                    result[0] = DIALOG_NO;
                }
            }
        });
        return result[0];
    }
    
    /**
     * Shows a Yes/No dialog with custom button text and icon.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     * 
     * Note: The parameter order differs from Messages.showYesNoDialog
     * In this method: (project, title, message, yesText, noText, icon)
     * In Messages: (project, message, title, yesText, noText, icon)
     *
     * @param project The project
     * @param title The title
     * @param message The message
     * @param yesText The text for the Yes button
     * @param noText The text for the No button
     * @param icon The icon to display
     * @return The dialog result (DIALOG_YES or DIALOG_NO)
     */
    public static int showYesNoDialog(Project project, String title, String message, 
                                     String yesText, String noText, @Nullable javax.swing.Icon icon) {
        final int[] result = new int[1];
        
        runOnUiThreadAndWait(() -> {
            try {
                // Swap title and message to match Messages.showYesNoDialog parameter order
                result[0] = Messages.showYesNoDialog(
                    project, 
                    message,  // IntelliJ API expects message first, then title
                    title, 
                    yesText, 
                    noText, 
                    icon != null ? icon : Messages.getQuestionIcon()
                );
            } catch (Exception ex) {
                LOG.error("Failed to show custom Yes/No dialog with project parameter", ex);
                try {
                    // Fall back to standard Yes/No dialog
                    result[0] = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
                } catch (Exception e) {
                    LOG.error("Failed to show Yes/No dialog", e);
                    result[0] = DIALOG_NO;
                }
            }
        });
        
        return result[0];
    }
    
    /**
     * Shows an input dialog to prompt the user for text.
     * Compatibility wrapper for different IntelliJ IDEA versions.
     *
     * @param project The project
     * @param message The prompt message
     * @param title The dialog title
     * @param initialValue The initial value to display
     * @return The value entered by the user, or null if canceled
     */
    @Nullable
    public static String showInputDialog(@Nullable Project project, @NotNull String message, 
                                     @NotNull String title, @Nullable String initialValue) {
        try {
            return Messages.showInputDialog(project, message, title, null, initialValue, null);
        } catch (Exception e) {
            LOG.warn("Failed to show input dialog with project parameter", e);
            try {
                return Messages.showInputDialog(message, title, null, initialValue, null);
            } catch (Exception ex) {
                LOG.error("Failed to show input dialog", ex);
                return null;
            }
        }
    }
    
    /**
     * Shows a choose dialog (combo box) to select an option.
     * Compatibility wrapper for different IntelliJ IDEA versions.
     *
     * @param project The project
     * @param message The prompt message
     * @param title The dialog title
     * @param options The available options
     * @param initialValue The initially selected value
     * @return The selected index, or -1 if canceled
     */
    public static int showChooseDialog(@Nullable Project project, @NotNull String message,
                                   @NotNull String title, @NotNull String[] options,
                                   @Nullable String initialValue) {
        try {
            return Messages.showChooseDialog(project, message, title, options, initialValue, null);
        } catch (Exception e) {
            LOG.warn("Failed to show choose dialog with project parameter", e);
            try {
                return Messages.showChooseDialog(message, title, options, initialValue, null);
            } catch (Exception ex) {
                LOG.error("Failed to show choose dialog", ex);
                return -1;
            }
        }
    }
    
    /**
     * Shows an error dialog with a message.
     * 
     * @param project The project (can be null)
     * @param message The message
     * @param title The title
     */
    public static void showErrorDialogWithProject(@Nullable Project project, @NotNull String message, @NotNull String title) {
        runOnUiThreadAndWait(() -> {
            try {
                if (project == null || !project.isDisposed()) {
                    Messages.showErrorDialog(project, message, title);
                }
            } catch (Exception ex) {
                LOG.error("Failed to show error dialog", ex);
                // Fallback to notification
                ModForgeNotificationService.getInstance().showError(title, message);
            }
        });
    }
    
    /**
     * Shows an info dialog with a message.
     * 
     * @param project The project (can be null)
     * @param message The message
     * @param title The title
     */
    public static void showInfoDialogWithProject(@Nullable Project project, @NotNull String message, @NotNull String title) {
        runOnUiThreadAndWait(() -> {
            try {
                if (project == null || !project.isDisposed()) {
                    Messages.showInfoMessage(project, message, title);
                }
            } catch (Exception ex) {
                LOG.error("Failed to show info dialog", ex);
                // Fallback to notification
                ModForgeNotificationService.getInstance().showInfo(title, message);
            }
        });
    }
    
    /**
     * Shows an input dialog to get text from the user.
     * 
     * @param project The project (can be null)
     * @param message The message
     * @param title The title
     * @param initialValue The initial value
     * @return The entered text, or null if canceled
     */
    public static String showInputDialogWithProject(@Nullable Project project, @NotNull String message, 
                                         @NotNull String title, @Nullable String initialValue) {
        final String[] result = new String[1];
        
        runOnUiThreadAndWait(() -> {
            try {
                if (project == null || !project.isDisposed()) {
                    result[0] = Messages.showInputDialog(project, message, title, null, initialValue, null);
                } else {
                    result[0] = Messages.showInputDialog(message, title, null, initialValue, null);
                }
            } catch (Exception ex) {
                LOG.error("Failed to show input dialog", ex);
                // Cannot fall back for input dialog
                ModForgeNotificationService.getInstance().showError(title, 
                        "Could not show input dialog: " + message);
            }
        });
        return result[0];
    }
    
    /**
     * Executes a read action with proper compatibility handling.
     * Replacement for deprecated ApplicationManager.getApplication().runReadAction
     *
     * @param computable The computation to run.
     * @param <T> The return type.
     * @return The result of the computation.
     */
    public static <T> T runReadAction(@NotNull com.intellij.openapi.util.Computable<T> computable) {
        return ReadAction.compute(computable::compute);
    }
    
    /**
     * Executes a write action with proper compatibility handling.
     * Replacement for deprecated ApplicationManager.getApplication().runWriteAction
     *
     * @param runnable The runnable to execute.
     */
    public static void runWriteAction(@NotNull Runnable runnable) {
        WriteAction.run(runnable::run);
    }
    
    /**
     * Executes a write action with proper compatibility handling and returns a result.
     * Replacement for deprecated ApplicationManager.getApplication().runWriteAction
     *
     * @param computable The computation to run.
     * @param <T> The return type.
     * @return The result of the computation.
     */
    public static <T> T computeInWriteAction(@NotNull com.intellij.openapi.util.Computable<T> computable) {
        return WriteAction.compute(computable::compute);
    }
    
    /**
     * Compatibility class for JBRadioButton that may be missing in some IntelliJ IDEA versions.
     * This provides a replacement using standard JRadioButton with IntelliJ styling.
     */
    public static class JBRadioButton extends JRadioButton {
        private static final Logger LOG = Logger.getInstance(JBRadioButton.class);
        
        public JBRadioButton() {
            super();
            applyIntelliJStyling();
        }
        
        public JBRadioButton(String text) {
            super(text);
            applyIntelliJStyling();
        }
        
        public JBRadioButton(String text, boolean selected) {
            super(text, selected);
            applyIntelliJStyling();
        }
        
        /**
         * Applies IntelliJ-specific styling to make this look like JBRadioButton
         */
        private void applyIntelliJStyling() {
            try {
                // Try to use UIUtil or JBUI for styling if available
                try {
                    // Try to access JBUI class for proper margin and styling
                    Class<?> jbuiClass = Class.forName("com.intellij.util.ui.JBUI");
                    java.lang.reflect.Method createEmptyBorderMethod = jbuiClass.getMethod("emptyInsets");
                    Object insets = createEmptyBorderMethod.invoke(null);
                    
                    if (insets instanceof Insets) {
                        ((Insets) insets).left = 4;
                        ((Insets) insets).right = 4;
                        setBorder(BorderFactory.createEmptyBorder(
                            ((Insets) insets).top, 
                            ((Insets) insets).left, 
                            ((Insets) insets).bottom, 
                            ((Insets) insets).right
                        ));
                    }
                } catch (Exception e) {
                    LOG.debug("Could not apply JBUI styling to JBRadioButton: " + e.getMessage());
                    // Fall back to standard styling
                    setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
                }
                
                // Try to get IntelliJ font
                try {
                    Class<?> uiUtilClass = Class.forName("com.intellij.util.ui.UIUtil");
                    java.lang.reflect.Method getLabelFontMethod = uiUtilClass.getMethod("getLabelFont");
                    Object labelFont = getLabelFontMethod.invoke(null);
                    
                    if (labelFont instanceof Font) {
                        setFont((Font) labelFont);
                    }
                } catch (Exception e) {
                    LOG.debug("Could not apply UIUtil font to JBRadioButton: " + e.getMessage());
                    // Keep default font
                }
            } catch (Exception ex) {
                LOG.debug("Failed to apply IntelliJ styling to JBRadioButton", ex);
            }
        }
    }
}