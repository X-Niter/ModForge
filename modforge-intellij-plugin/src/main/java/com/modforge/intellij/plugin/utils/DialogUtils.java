package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.modforge.intellij.plugin.ui.dialogs.GenerateImplementationDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for dialogs.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class DialogUtils {
    
    /**
     * Private constructor.
     */
    private DialogUtils() {
        // Utility class
    }
    
    /**
     * Shows an error message.
     *
     * @param project The project.
     * @param title   The title.
     * @param message The message.
     */
    public static void showErrorMessage(@Nullable Project project, @NotNull String title, @NotNull String message) {
        SwingUtilities.invokeLater(() ->
                Messages.showErrorDialog(project, message, title)
        );
    }
    
    /**
     * Shows an information message.
     *
     * @param project The project.
     * @param title   The title.
     * @param message The message.
     */
    public static void showInfoMessage(@Nullable Project project, @NotNull String title, @NotNull String message) {
        SwingUtilities.invokeLater(() ->
                Messages.showInfoMessage(project, message, title)
        );
    }
    
    /**
     * Shows a warning message.
     *
     * @param project The project.
     * @param title   The title.
     * @param message The message.
     */
    public static void showWarningMessage(@Nullable Project project, @NotNull String title, @NotNull String message) {
        SwingUtilities.invokeLater(() ->
                Messages.showWarningDialog(project, message, title)
        );
    }
    
    /**
     * Shows a confirmation dialog.
     *
     * @param project The project.
     * @param title   The title.
     * @param message The message.
     * @return The future with the result.
     */
    public static CompletableFuture<Boolean> showConfirmationDialog(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String message) {
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        SwingUtilities.invokeLater(() -> {
            int result = Messages.showYesNoDialog(
                    project,
                    message,
                    title,
                    Messages.getQuestionIcon()
            );
            
            future.complete(result == Messages.YES);
        });
        
        return future;
    }
    
    /**
     * Shows a status bar notification.
     *
     * @param project The project.
     * @param message The message.
     */
    public static void showStatusBarNotification(@NotNull Project project, @NotNull String message) {
        SwingUtilities.invokeLater(() -> {
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        });
    }
    
    /**
     * Shows the generate implementation dialog.
     *
     * @param project The project.
     * @return The dialog.
     */
    public static GenerateImplementationDialog showGenerateImplementationDialog(@NotNull Project project) {
        GenerateImplementationDialog dialog = new GenerateImplementationDialog(project);
        dialog.show();
        return dialog;
    }
    
    /**
     * Shows an input dialog with a text field.
     *
     * @param project     The project.
     * @param title       The title.
     * @param message     The message.
     * @param initialValue The initial value.
     * @return The future with the result.
     */
    public static CompletableFuture<String> showInputDialog(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String message,
            @Nullable String initialValue) {
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        SwingUtilities.invokeLater(() -> {
            String result = Messages.showInputDialog(
                    project,
                    message,
                    title,
                    Messages.getQuestionIcon(),
                    initialValue,
                    null
            );
            
            future.complete(result);
        });
        
        return future;
    }
    
    /**
     * Shows a custom dialog.
     *
     * @param dialog The dialog.
     * @param <T>    The dialog type.
     * @return The future with the dialog.
     */
    public static <T extends DialogWrapper> CompletableFuture<T> showDialog(@NotNull T dialog) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        SwingUtilities.invokeLater(() -> {
            dialog.show();
            future.complete(dialog);
        });
        
        return future;
    }
    
    /**
     * Executes the given task on the UI thread.
     * This method is deprecated and should be replaced with CompatibilityUtil.runOnUiThread.
     * 
     * @param runnable The task to execute
     */
    @Deprecated
    public static void executeOnUiThread(@NotNull Runnable runnable) {
        CompatibilityUtil.runOnUiThread(runnable);
    }
}