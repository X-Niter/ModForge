package com.modforge.intellij.plugin.utils.error;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for handling errors.
 */
public class ErrorHandler {
    private static final Logger LOG = Logger.getInstance(ErrorHandler.class);
    
    private ErrorHandler() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Handle an exception and show an error dialog.
     *
     * @param project       The project
     * @param title         The dialog title
     * @param message       The message
     * @param e             The exception
     * @param showStackTrace Whether to show the stack trace
     */
    public static void handleException(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String message,
            @NotNull Throwable e,
            boolean showStackTrace
    ) {
        LOG.error(message, e);
        
        if (showStackTrace) {
            String stackTrace = getStackTrace(e);
            com.modforge.intellij.plugin.utils.CompatibilityUtil.showErrorDialog(
                    project,
                    message + "\n\n" + stackTrace,
                    title
            );
        } else {
            com.modforge.intellij.plugin.utils.CompatibilityUtil.showErrorDialog(
                    project,
                    message + "\n\n" + e.getMessage(),
                    title
            );
        }
    }
    
    /**
     * Handle an exception and show an error notification.
     *
     * @param project   The project
     * @param title     The notification title
     * @param message   The message
     * @param e         The exception
     * @return The notification ID
     */
    @NotNull
    public static String handleExceptionWithNotification(
            @NotNull Project project,
            @NotNull String title,
            @NotNull String message,
            @NotNull Throwable e
    ) {
        LOG.error(message, e);
        
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        if (notificationService == null) {
            return "";
        }
        
        String content = message + "\n\n" + e.getMessage();
        return notificationService.showNotification(title, content, NotificationType.ERROR, null);
    }
    
    /**
     * Handle completable future exceptions.
     *
     * @param future        The future
     * @param project       The project
     * @param title         The error title
     * @param message       The error message
     * @param successHandler The success handler
     * @param <T>           The result type
     */
    public static <T> void handleFuture(
            @NotNull CompletableFuture<T> future,
            @NotNull Project project,
            @NotNull String title,
            @NotNull String message,
            @NotNull Consumer<T> successHandler
    ) {
        future.thenAccept(successHandler)
                .exceptionally(e -> {
                    if (e instanceof CompletionException) {
                        e = e.getCause();
                    }
                    
                    handleExceptionWithNotification(project, title, message, e);
                    return null;
                });
    }
    
    /**
     * Run an action with error handling.
     *
     * @param project       The project
     * @param title         The error title
     * @param message       The error message
     * @param action        The action
     */
    public static void runWithErrorHandling(
            @NotNull Project project,
            @NotNull String title,
            @NotNull String message,
            @NotNull Runnable action
    ) {
        try {
            action.run();
        } catch (Exception e) {
            handleExceptionWithNotification(project, title, message, e);
        }
    }
    
    /**
     * Run a function with error handling.
     *
     * @param project       The project
     * @param title         The error title
     * @param message       The error message
     * @param function      The function
     * @param defaultValue  The default value to return on error
     * @param <T>           The result type
     * @return The function result, or defaultValue on error
     */
    public static <T> T computeWithErrorHandling(
            @NotNull Project project,
            @NotNull String title,
            @NotNull String message,
            @NotNull Function<Void, T> function,
            @Nullable T defaultValue
    ) {
        try {
            return function.apply(null);
        } catch (Exception e) {
            handleExceptionWithNotification(project, title, message, e);
            return defaultValue;
        }
    }
    
    /**
     * Get the stack trace of an exception as a string.
     *
     * @param e The exception
     * @return The stack trace
     */
    @NotNull
    public static String getStackTrace(@NotNull Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        e.printStackTrace(ps);
        ps.flush();
        return out.toString();
    }
    
    /**
     * Get the root cause of an exception.
     *
     * @param e The exception
     * @return The root cause
     */
    @NotNull
    public static Throwable getRootCause(@NotNull Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null || cause == e) {
            return e;
        }
        return getRootCause(cause);
    }
}