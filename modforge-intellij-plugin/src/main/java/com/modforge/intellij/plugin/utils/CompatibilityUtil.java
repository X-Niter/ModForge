package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Utility class for compatibility across different IntelliJ IDEA versions.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class CompatibilityUtil {
    private static final Logger LOG = Logger.getInstance(CompatibilityUtil.class);
    
    // Timeout constants
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private CompatibilityUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Runs a task with read access.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return The result of the task.
     */
    public static <T> T runWithReadAccess(@NotNull Computable<T> task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isReadAccessAllowed()) {
            return task.compute();
        } else {
            try {
                return ReadAction.nonBlocking(task::compute)
                        .executionContext(application::isReadAccessAllowed)
                        .submit()
                        .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Failed to run task with read access", e);
                throw new RuntimeException("Failed to run task with read access", e);
            }
        }
    }

    /**
     * Runs a task with write access.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return The result of the task.
     */
    public static <T> T runWithWriteAccess(@NotNull ThrowableComputable<T, Throwable> task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isWriteAccessAllowed()) {
            try {
                return task.compute();
            } catch (Throwable e) {
                LOG.error("Failed to run task with write access", e);
                throw new RuntimeException("Failed to run task with write access", e);
            }
        } else {
            try {
                return WriteAction.computeAndWait(task);
            } catch (Throwable e) {
                LOG.error("Failed to run task with write access", e);
                throw new RuntimeException("Failed to run task with write access", e);
            }
        }
    }

    /**
     * Runs a task with read access.
     *
     * @param task The task to run.
     */
    public static void runWithReadAccess(@NotNull Runnable task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isReadAccessAllowed()) {
            task.run();
        } else {
            try {
                ReadAction.nonBlocking(() -> {
                    task.run();
                    return null;
                })
                        .executionContext(application::isReadAccessAllowed)
                        .submit()
                        .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Failed to run task with read access", e);
                throw new RuntimeException("Failed to run task with read access", e);
            }
        }
    }

    /**
     * Runs a task with write access.
     *
     * @param task The task to run.
     */
    public static void runWithWriteAccess(@NotNull Runnable task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isWriteAccessAllowed()) {
            task.run();
        } else {
            try {
                WriteAction.runAndWait(task::run);
            } catch (Throwable e) {
                LOG.error("Failed to run task with write access", e);
                throw new RuntimeException("Failed to run task with write access", e);
            }
        }
    }

    /**
     * Gets a document for a PSI file.
     *
     * @param project The project.
     * @param file    The PSI file.
     * @return The document or null if it doesn't exist.
     */
    @Nullable
    public static Document getDocument(@NotNull Project project, @NotNull PsiFile file) {
        return runWithReadAccess(() -> PsiDocumentManager.getInstance(project).getDocument(file));
    }

    /**
     * Commits a document.
     *
     * @param project  The project.
     * @param document The document.
     */
    public static void commitDocument(@NotNull Project project, @NotNull Document document) {
        runWithWriteAccess(() -> {
            PsiDocumentManager.getInstance(project).commitDocument(document);
            return null;
        });
    }

    /**
     * Runs a task synchronously on the UI thread.
     *
     * @param task The task to run.
     */
    public static void runOnUIThread(@NotNull Runnable task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isDispatchThread()) {
            task.run();
        } else {
            application.invokeAndWait(task);
        }
    }

    /**
     * Runs a task asynchronously on the UI thread.
     *
     * @param task The task to run.
     */
    public static void runOnUIThreadAsync(@NotNull Runnable task) {
        ApplicationManager.getApplication().invokeLater(task);
    }

    /**
     * Runs a callable with a timeout.
     *
     * @param callable The callable to run.
     * @param timeout  The timeout in seconds.
     * @param <T>      The return type.
     * @return The result of the callable.
     * @throws TimeoutException      If the timeout is exceeded.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the callable throws an exception.
     */
    public static <T> T runWithTimeout(@NotNull Callable<T> callable, long timeout)
            throws TimeoutException, InterruptedException, ExecutionException {
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get(timeout, TimeUnit.SECONDS);
    }

    /**
     * Runs a supplier with a timeout.
     *
     * @param supplier The supplier to run.
     * @param timeout  The timeout in seconds.
     * @param <T>      The return type.
     * @return The result of the supplier.
     * @throws TimeoutException      If the timeout is exceeded.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the supplier throws an exception.
     */
    public static <T> T runWithTimeout(@NotNull Supplier<T> supplier, long timeout)
            throws TimeoutException, InterruptedException, ExecutionException {
        
        return ThreadUtils.supplyAsyncVirtual(supplier).get(timeout, TimeUnit.SECONDS);
    }

    /**
     * Safely runs a runnable, logging any exceptions.
     *
     * @param runnable  The runnable to run.
     * @param errorMsg  The error message to log.
     */
    public static void runSafely(@NotNull Runnable runnable, @NotNull String errorMsg) {
        try {
            runnable.run();
        } catch (Exception e) {
            LOG.error(errorMsg, e);
        }
    }

    /**
     * Safely runs a supplier, logging any exceptions.
     *
     * @param supplier  The supplier to run.
     * @param errorMsg  The error message to log.
     * @param fallback  The fallback value.
     * @param <T>       The return type.
     * @return The result of the supplier or the fallback value.
     */
    public static <T> T runSafely(@NotNull Supplier<T> supplier, @NotNull String errorMsg, @Nullable T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            LOG.error(errorMsg, e);
            return fallback;
        }
    }
    
    /**
     * Gets the base directory of a project.
     *
     * @param project The project.
     * @return The base directory or null if not found.
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@Nullable Project project) {
        if (project == null) {
            LOG.warn("Project is null");
            return null;
        }
        
        try {
            return ReadAction.compute(() -> project.getBaseDir());
        } catch (Exception e) {
            LOG.error("Failed to get project base directory", e);
            return null;
        }
    }
    
    /**
     * Gets a mod file by its relative path.
     *
     * @param project       The project.
     * @param relativePath  The relative path.
     * @return The file or null if not found.
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir == null) {
            return null;
        }
        
        try {
            return ReadAction.compute(() -> baseDir.findFileByRelativePath(relativePath));
        } catch (Exception e) {
            LOG.error("Failed to find file by relative path: " + relativePath, e);
            return null;
        }
    }
    
    /**
     * Executes a task on the UI thread.
     *
     * @param task The task to run.
     */
    public static void executeOnUiThread(@NotNull Runnable task) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isDispatchThread()) {
            task.run();
        } else {
            application.invokeLater(task);
        }
    }
    
    /**
     * Computes a value with write access.
     *
     * @param action The action to run.
     * @param <T>    The return type.
     * @return The result of the action.
     */
    public static <T> T computeInWriteAction(@NotNull Computable<T> action) {
        Application application = ApplicationManager.getApplication();
        
        if (application.isWriteAccessAllowed()) {
            return action.compute();
        } else {
            try {
                return WriteAction.computeAndWait(action);
            } catch (Throwable e) {
                LOG.error("Failed to compute in write action", e);
                throw new RuntimeException("Failed to compute in write action", e);
            }
        }
    }
    
    /**
     * Opens a file in the editor.
     *
     * @param project   The project.
     * @param file      The file.
     * @param focus     Whether to focus the editor.
     */
    public static void openFileInEditor(@NotNull Project project, @NotNull VirtualFile file, boolean focus) {
        executeOnUiThread(() -> {
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
     * Shows an info dialog.
     *
     * @param project  The project.
     * @param message  The message.
     * @param title    The title.
     */
    public static void showInfoDialog(@Nullable Project project, @NotNull String message, @NotNull String title) {
        executeOnUiThread(() -> {
            try {
                Messages.showInfoMessage(project, message, title);
            } catch (Exception e) {
                LOG.error("Failed to show info dialog", e);
            }
        });
    }
    
    /**
     * Shows an error dialog.
     *
     * @param project  The project.
     * @param message  The message.
     * @param title    The title.
     */
    public static void showErrorDialog(@Nullable Project project, @NotNull String message, @NotNull String title) {
        executeOnUiThread(() -> {
            try {
                Messages.showErrorDialog(project, message, title);
            } catch (Exception e) {
                LOG.error("Failed to show error dialog", e);
            }
        });
    }
}