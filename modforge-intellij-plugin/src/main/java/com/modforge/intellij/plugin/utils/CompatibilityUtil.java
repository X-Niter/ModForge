package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.EdtExecutorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class for IntelliJ IDEA API compatibility.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class CompatibilityUtil {
    private static final Logger LOG = Logger.getInstance(CompatibilityUtil.class);

    private CompatibilityUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Executes a task in a read action.
     *
     * @param <T> The result type.
     * @param computable The task to execute.
     * @return The result of the task.
     */
    public static <T> T computeInReadAction(@NotNull Computable<T> computable) {
        return ReadAction.compute(computable);
    }

    /**
     * Executes a task in a write action.
     *
     * @param <T> The result type.
     * @param computable The task to execute.
     * @return The result of the task.
     * @throws Exception If an error occurs.
     */
    public static <T> T computeInWriteAction(@NotNull ThrowableComputable<T, ? extends Exception> computable) throws Exception {
        return WriteAction.compute(computable);
    }

    /**
     * Executes a task in a write action, catching any exceptions.
     *
     * @param <T> The result type.
     * @param computable The task to execute.
     * @param defaultValue The default value to return if an exception occurs.
     * @return The result of the task or the default value if an exception occurs.
     */
    public static <T> T computeInWriteActionWithDefault(@NotNull Computable<T> computable, T defaultValue) {
        try {
            return WriteAction.compute(() -> computable.compute());
        } catch (Exception e) {
            LOG.error("Error executing write action", e);
            return defaultValue;
        }
    }

    /**
     * Runs a task in a read action.
     *
     * @param action The task to run.
     */
    public static void runInReadAction(@NotNull Runnable action) {
        ReadAction.run(action);
    }

    /**
     * Runs a task in a write action.
     *
     * @param action The task to run.
     */
    public static void runInWriteAction(@NotNull Runnable action) {
        try {
            WriteAction.run(action::run);
        } catch (Exception e) {
            LOG.error("Error executing write action", e);
        }
    }

    /**
     * Runs a task in a command action.
     *
     * @param project The project.
     * @param name The command name.
     * @param action The task to run.
     */
    public static void runInCommandAction(@NotNull Project project, @NotNull String name, @NotNull Runnable action) {
        CommandProcessor.getInstance().executeCommand(project, () -> runInWriteAction(action), name, null);
    }

    /**
     * Executes a task on the UI thread.
     *
     * @param action The task to run.
     */
    public static void executeOnUiThread(@NotNull Runnable action) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            action.run();
        } else {
            ApplicationManager.getApplication().invokeLater(action);
        }
    }

    /**
     * Executes a task on the UI thread with a specific modality state.
     *
     * @param action The task to run.
     * @param modalityState The modality state.
     */
    public static void executeOnUiThread(@NotNull Runnable action, @NotNull ModalityState modalityState) {
        if (ApplicationManager.getApplication().isDispatchThread() && ModalityState.current().equals(modalityState)) {
            action.run();
        } else {
            ApplicationManager.getApplication().invokeLater(action, modalityState);
        }
    }

    /**
     * Executes a task on the UI thread with a timeout.
     *
     * @param <T> The result type.
     * @param supplier The task to run.
     * @param timeout The timeout.
     * @param unit The time unit of the timeout.
     * @return A CompletableFuture with the result of the task.
     */
    public static <T> CompletableFuture<T> executeOnUiThreadWithTimeout(
            @NotNull Supplier<T> supplier,
            long timeout,
            @NotNull TimeUnit unit) {
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        if (ApplicationManager.getApplication().isDispatchThread()) {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            CompletableFuture.runAsync(() -> {
                try {
                    T result = ApplicationManager.getApplication().invokeAndWait(supplier::get);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }, EdtExecutorService.getInstance());
        }
        
        return future.orTimeout(timeout, unit);
    }

    /**
     * Gets the base directory of a project.
     * Replacement for deprecated Project.getBaseDir()
     *
     * @param project The project.
     * @return The base directory.
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        
        return VfsUtil.findFile(Paths.get(basePath), true);
    }

    /**
     * Refreshes all files in a project.
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
     * Opens a file in the editor.
     *
     * @param project The project.
     * @param file The file to open.
     * @param requestFocus Whether to request focus.
     * @return The opened editor.
     */
    @Nullable
    public static Editor openFileInEditor(
            @NotNull Project project,
            @NotNull VirtualFile file,
            boolean requestFocus) {
        
        FileEditor[] fileEditors = FileEditorManager.getInstance(project).openFile(file, requestFocus);
        
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof TextEditor) {
                Editor editor = ((TextEditor) fileEditor).getEditor();
                if (requestFocus) {
                    IdeFocusManager.getInstance(project).requestFocus(editor.getContentComponent(), true);
                }
                return editor;
            }
        }
        
        return null;
    }

    /**
     * Opens a file in the editor at a specific offset.
     *
     * @param project The project.
     * @param file The file to open.
     * @param offset The offset.
     * @param requestFocus Whether to request focus.
     * @return The opened editor.
     */
    @Nullable
    public static Editor openFileInEditor(
            @NotNull Project project,
            @NotNull VirtualFile file,
            int offset,
            boolean requestFocus) {
        
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, offset);
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, requestFocus);
        
        if (editor != null && requestFocus) {
            IdeFocusManager.getInstance(project).requestFocus(editor.getContentComponent(), true);
        }
        
        return editor;
    }

    /**
     * Gets a PsiFile from a VirtualFile.
     *
     * @param project The project.
     * @param file The virtual file.
     * @return The PsiFile.
     */
    @Nullable
    public static PsiFile getPsiFile(@NotNull Project project, @NotNull VirtualFile file) {
        return computeInReadAction(() -> PsiManager.getInstance(project).findFile(file));
    }

    /**
     * Gets the document for a VirtualFile.
     *
     * @param file The virtual file.
     * @return The document.
     */
    @Nullable
    public static Document getDocument(@NotNull VirtualFile file) {
        return computeInReadAction(() -> com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file));
    }

    /**
     * Creates a new file.
     *
     * @param parent The parent directory.
     * @param name The file name.
     * @return The created file.
     * @throws IOException If an error occurs.
     */
    @NotNull
    public static VirtualFile createNewFile(@NotNull VirtualFile parent, @NotNull String name) throws IOException {
        return computeInWriteAction(() -> parent.createChildData(null, name));
    }

    /**
     * Creates a new directory.
     *
     * @param parent The parent directory.
     * @param name The directory name.
     * @return The created directory.
     * @throws IOException If an error occurs.
     */
    @NotNull
    public static VirtualFile createNewDirectory(@NotNull VirtualFile parent, @NotNull String name) throws IOException {
        return computeInWriteAction(() -> parent.createChildDirectory(null, name));
    }

    /**
     * Writes text to a file.
     *
     * @param file The file.
     * @param text The text to write.
     * @throws IOException If an error occurs.
     */
    public static void writeToFile(@NotNull VirtualFile file, @NotNull String text) throws IOException {
        computeInWriteAction(() -> {
            file.setBinaryContent(text.getBytes());
            return null;
        });
    }

    /**
     * Deletes a file.
     *
     * @param file The file to delete.
     * @throws IOException If an error occurs.
     */
    public static void deleteFile(@NotNull VirtualFile file) throws IOException {
        computeInWriteAction(() -> {
            file.delete(null);
            return null;
        });
    }

    /**
     * Renames a file.
     *
     * @param file The file to rename.
     * @param newName The new name.
     * @throws IOException If an error occurs.
     */
    public static void renameFile(@NotNull VirtualFile file, @NotNull String newName) throws IOException {
        computeInWriteAction(() -> {
            file.rename(null, newName);
            return null;
        });
    }

    /**
     * Moves a file.
     *
     * @param file The file to move.
     * @param newParent The new parent directory.
     * @throws IOException If an error occurs.
     */
    public static void moveFile(@NotNull VirtualFile file, @NotNull VirtualFile newParent) throws IOException {
        computeInWriteAction(() -> {
            file.move(null, newParent);
            return null;
        });
    }

    /**
     * Converts a file path to a VirtualFile.
     *
     * @param path The file path.
     * @return The VirtualFile.
     */
    @Nullable
    public static VirtualFile pathToVirtualFile(@NotNull String path) {
        return VfsUtil.findFile(Paths.get(path), true);
    }

    /**
     * Converts a VirtualFile to a file path.
     *
     * @param file The VirtualFile.
     * @return The file path.
     */
    @NotNull
    public static String virtualFileToPath(@NotNull VirtualFile file) {
        return file.getPath();
    }

    /**
     * Converts a VirtualFile to a Java File.
     *
     * @param file The VirtualFile.
     * @return The Java File.
     */
    @Nullable
    public static File virtualFileToIoFile(@NotNull VirtualFile file) {
        return VfsUtil.virtualToIoFile(file);
    }

    /**
     * Converts a Java File to a VirtualFile.
     *
     * @param file The Java File.
     * @return The VirtualFile.
     */
    @Nullable
    public static VirtualFile ioFileToVirtualFile(@NotNull File file) {
        return VfsUtil.findFileByIoFile(file, true);
    }

    /**
     * Converts a Java Path to a VirtualFile.
     *
     * @param path The Java Path.
     * @return The VirtualFile.
     */
    @Nullable
    public static VirtualFile pathToVirtualFile(@NotNull Path path) {
        return VfsUtil.findFile(path, true);
    }

    /**
     * Converts a VirtualFile to a Java Path.
     *
     * @param file The VirtualFile.
     * @return The Java Path.
     */
    @NotNull
    public static Path virtualFileToPath(@NotNull VirtualFile file) {
        return Paths.get(file.getPath());
    }
}