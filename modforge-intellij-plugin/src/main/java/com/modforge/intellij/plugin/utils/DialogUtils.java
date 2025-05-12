package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for working with dialogs.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class DialogUtils {
    private static final Logger LOG = Logger.getInstance(DialogUtils.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private DialogUtils() {
        // Utility class
    }

    /**
     * Shows an information dialog.
     *
     * @param project The project.
     * @param message The message.
     * @param title   The title.
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
     * @param project The project.
     * @param message The message.
     * @param title   The title.
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

    /**
     * Shows a warning dialog.
     *
     * @param project The project.
     * @param message The message.
     * @param title   The title.
     */
    public static void showWarningDialog(@Nullable Project project, @NotNull String message, @NotNull String title) {
        executeOnUiThread(() -> {
            try {
                Messages.showWarningDialog(project, message, title);
            } catch (Exception e) {
                LOG.error("Failed to show warning dialog", e);
            }
        });
    }

    /**
     * Shows a yes/no dialog.
     *
     * @param project The project.
     * @param message The message.
     * @param title   The title.
     * @return Whether the user clicked yes.
     */
    public static boolean showYesNoDialog(@Nullable Project project, @NotNull String message, @NotNull String title) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        executeOnUiThreadAndWait(() -> {
            try {
                int answer = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
                result.set(answer == Messages.YES);
            } catch (Exception e) {
                LOG.error("Failed to show yes/no dialog", e);
            }
        });
        
        return result.get();
    }

    /**
     * Shows an input dialog.
     *
     * @param project The project.
     * @param message The message.
     * @param title   The title.
     * @param initial The initial value.
     * @return The user input.
     */
    @Nullable
    public static String showInputDialog(
            @Nullable Project project,
            @NotNull String message,
            @NotNull String title,
            @Nullable String initial) {
        
        AtomicReference<String> result = new AtomicReference<>();
        
        executeOnUiThreadAndWait(() -> {
            try {
                String input = Messages.showInputDialog(project, message, title, Messages.getQuestionIcon(), initial, null);
                result.set(input);
            } catch (Exception e) {
                LOG.error("Failed to show input dialog", e);
            }
        });
        
        return result.get();
    }

    /**
     * Shows a password dialog.
     *
     * @param project The project.
     * @param message The message.
     * @param title   The title.
     * @return The password.
     */
    @Nullable
    public static String showPasswordDialog(
            @Nullable Project project,
            @NotNull String message,
            @NotNull String title) {
        
        AtomicReference<String> result = new AtomicReference<>();
        
        executeOnUiThreadAndWait(() -> {
            try {
                String password = Messages.showPasswordDialog(project, message, title, Messages.getQuestionIcon(), null);
                result.set(password);
            } catch (Exception e) {
                LOG.error("Failed to show password dialog", e);
            }
        });
        
        return result.get();
    }

    /**
     * Shows a dialog with multiple options.
     *
     * @param project The project.
     * @param message The message.
     * @param title   The title.
     * @param options The options.
     * @return The selected option index, or -1 if canceled.
     */
    public static int showDialog(
            @Nullable Project project,
            @NotNull String message,
            @NotNull String title,
            @NotNull String[] options) {
        
        AtomicReference<Integer> result = new AtomicReference<>(-1);
        
        executeOnUiThreadAndWait(() -> {
            try {
                int choice = Messages.showDialog(project, message, title, options, 0, Messages.getQuestionIcon());
                result.set(choice);
            } catch (Exception e) {
                LOG.error("Failed to show dialog", e);
            }
        });
        
        return result.get();
    }

    /**
     * Executes a task on the UI thread.
     *
     * @param task The task to run.
     */
    public static void executeOnUiThread(@NotNull Runnable task) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeLater(task);
        }
    }

    /**
     * Executes a task on the UI thread and waits for it to complete.
     *
     * @param task The task to run.
     */
    public static void executeOnUiThreadAndWait(@NotNull Runnable task) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            try {
                ApplicationManager.getApplication().invokeAndWait(task);
            } catch (Exception e) {
                LOG.error("Failed to execute task on UI thread", e);
            }
        }
    }

    /**
     * Executes a task on the UI thread and returns a CompletableFuture.
     *
     * @param task The task to run.
     * @param <T>  The return type.
     * @return A CompletableFuture that completes with the task's result.
     */
    @NotNull
    public static <T> CompletableFuture<T> executeOnUiThreadAsync(@NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        executeOnUiThread(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
}