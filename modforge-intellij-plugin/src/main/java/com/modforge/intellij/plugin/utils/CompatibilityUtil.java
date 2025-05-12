package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for compatibility with IntelliJ IDEA 2025.1.1.1.
 * Contains functions to make the plugin work with the latest APIs.
 */
public final class CompatibilityUtil {
    private CompatibilityUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Runs a task on the UI thread.
     *
     * @param runnable The task to run.
     */
    public static void executeOnUiThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    /**
     * Runs a task on the UI thread after a delay.
     *
     * @param runnable The task to run.
     * @param delayMillis The delay in milliseconds.
     */
    public static void executeOnUiThreadWithDelay(@NotNull Runnable runnable, long delayMillis) {
        CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS)
                .execute(() -> executeOnUiThread(runnable));
    }

    /**
     * Runs a task in a read action.
     *
     * @param action The action to run.
     * @param <T> The return type.
     * @return The result of the action.
     */
    public static <T> T runReadAction(@NotNull Supplier<T> action) {
        return ReadAction.compute(action::get);
    }

    /**
     * Runs a task in a write action.
     *
     * @param action The action to run.
     * @param <T> The return type.
     * @return The result of the action.
     * @throws Throwable If an error occurs.
     */
    public static <T> T runWriteAction(@NotNull Callable<T> action) throws Throwable {
        return WriteAction.computeAndWait(action);
    }

    /**
     * Gets the project base directory.
     * Replacement for deprecated Project.getBaseDir()
     *
     * @param project The project.
     * @return The base directory.
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@NotNull Project project) {
        return ProjectUtil.guessProjectDir(project);
    }

    /**
     * Refreshes all files in the project.
     *
     * @param project The project.
     */
    public static void refreshAll(@NotNull Project project) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir != null) {
            refreshFile(baseDir, true);
        }
    }

    /**
     * Refreshes a file or directory.
     *
     * @param file The file or directory.
     * @param recursive Whether to refresh recursively.
     */
    public static void refreshFile(@NotNull VirtualFile file, boolean recursive) {
        file.refresh(false, recursive);
    }

    /**
     * Finds a file in the file system.
     *
     * @param path The path to the file.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * Finds a file by a path relative to the project base directory.
     *
     * @param project The project.
     * @param relativePath The relative path.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir == null) return null;
        
        return baseDir.findFileByRelativePath(relativePath);
    }

    /**
     * Opens a file in the editor.
     *
     * @param project The project.
     * @param file The file to open.
     * @param requestFocus Whether to request focus.
     */
    public static void openFileInEditor(@NotNull Project project, @NotNull VirtualFile file, boolean requestFocus) {
        executeOnUiThread(() -> {
            FileEditorManager.getInstance(project).openFile(file, requestFocus);
        });
    }

    /**
     * Opens a file in the editor at a specific position.
     *
     * @param project The project.
     * @param file The file to open.
     * @param line The line to navigate to.
     * @param column The column to navigate to.
     * @param requestFocus Whether to request focus.
     */
    public static void openFileInEditor(
            @NotNull Project project,
            @NotNull VirtualFile file,
            int line,
            int column,
            boolean requestFocus) {
        
        executeOnUiThread(() -> {
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, line, column);
            FileEditorManager.getInstance(project).openEditor(descriptor, requestFocus);
        });
    }

    /**
     * Gets the PSI file for a virtual file.
     *
     * @param project The project.
     * @param file The virtual file.
     * @return The PSI file, or null if not found.
     */
    @Nullable
    public static PsiFile getPsiFile(@NotNull Project project, @NotNull VirtualFile file) {
        return runReadAction(() -> PsiManager.getInstance(project).findFile(file));
    }

    /**
     * Gets the document for a virtual file.
     *
     * @param project The project.
     * @param file The virtual file.
     * @return The document, or null if not found.
     */
    @Nullable
    public static Document getDocument(@NotNull Project project, @NotNull VirtualFile file) {
        return runReadAction(() -> com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file));
    }

    /**
     * Executes a computation in a write action.
     *
     * @param supplier The supplier to execute.
     * @param <T> The return type.
     * @return The result of the computation.
     */
    public static <T> T computeInWriteAction(@NotNull Supplier<T> supplier) {
        try {
            return WriteAction.computeAndWait(supplier::get);
        } catch (Throwable t) {
            throw new RuntimeException("Error executing write action", t);
        }
    }

    /**
     * Executes a computation in a read action.
     *
     * @param supplier The supplier to execute.
     * @param <T> The return type.
     * @return The result of the computation.
     */
    public static <T> T computeInReadAction(@NotNull Supplier<T> supplier) {
        return ReadAction.compute(supplier::get);
    }

    /**
     * Runs a task on the UI thread with virtual thread optimization for Java 21.
     * Uses the getUIThreadExecutorService method if available.
     *
     * @param runnable The task to run.
     */
    public static void runOnUIThread(@NotNull Runnable runnable) {
        executeOnUiThread(runnable);
    }

    /**
     * Computes asynchronously in a read action with thread optimization.
     *
     * @param computation The computation to perform.
     * @param <T> The return type.
     * @return A CompletableFuture that will complete with the result.
     */
    public static <T> CompletableFuture<T> computeReadAsync(@NotNull Supplier<T> computation) {
        return ReadAction.nonBlocking(computation::get)
                .submit(ApplicationManager.getApplication().getCoroutineScope())
                .asCompletableFuture();
    }

    /**
     * Gets the selected text in the current editor.
     *
     * @param project The project.
     * @return The selected text, or null if no text is selected.
     */
    @Nullable
    public static String getSelectedText(@NotNull Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return null;
        
        return runReadAction(() -> {
            String selectedText = editor.getSelectionModel().getSelectedText();
            if (selectedText == null || selectedText.isEmpty()) {
                return null;
            }
            return selectedText;
        });
    }

    /**
     * Gets the file path from a virtual file.
     *
     * @param virtualFile The virtual file.
     * @return The file path, or null if the virtual file is null.
     */
    @Nullable
    public static String getFilePath(@Nullable VirtualFile virtualFile) {
        if (virtualFile == null) return null;
        return virtualFile.getPath();
    }

    /**
     * Gets the file extension from a virtual file.
     *
     * @param virtualFile The virtual file.
     * @return The file extension, or null if the virtual file is null.
     */
    @Nullable
    public static String getFileExtension(@Nullable VirtualFile virtualFile) {
        if (virtualFile == null) return null;
        return virtualFile.getExtension();
    }

    /**
     * Gets the file name from a virtual file.
     *
     * @param virtualFile The virtual file.
     * @return The file name, or null if the virtual file is null.
     */
    @Nullable
    public static String getFileName(@Nullable VirtualFile virtualFile) {
        if (virtualFile == null) return null;
        return virtualFile.getName();
    }

    /**
     * Creates a virtual file from a regular file.
     *
     * @param file The regular file.
     * @return The virtual file, or null if the file is null.
     */
    @Nullable
    public static VirtualFile getVirtualFile(@Nullable File file) {
        if (file == null) return null;
        return LocalFileSystem.getInstance().findFileByIoFile(file);
    }

    /**
     * Waits for a CompletableFuture to complete with a timeout.
     *
     * @param future The future to wait for.
     * @param timeoutMillis The timeout in milliseconds.
     * @param <T> The return type.
     * @return The result of the future.
     * @throws TimeoutException If the timeout is exceeded.
     * @throws ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted.
     */
    public static <T> T waitWithTimeout(
            @NotNull CompletableFuture<T> future,
            long timeoutMillis) throws TimeoutException, ExecutionException, InterruptedException {
        
        return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the current IDE theme (light or dark) for compatibility.
     *
     * @return "light" or "dark".
     */
    @NotNull
    public static String getCurrentTheme() {
        return com.intellij.ide.ui.LafManager.getInstance().getCurrentLookAndFeel().isDark() ? "dark" : "light";
    }
}