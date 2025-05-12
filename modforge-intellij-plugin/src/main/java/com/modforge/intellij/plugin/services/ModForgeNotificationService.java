package com.modforge.intellij.plugin.services;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for ModForge notifications.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class ModForgeNotificationService {
    private static final Logger LOG = Logger.getInstance(ModForgeNotificationService.class);
    
    private static final NotificationGroup NOTIFICATION_GROUP = 
            NotificationGroupManager.getInstance().getNotificationGroup("ModForge Notifications");
    
    // Constants for dialog return values
    public static final int YES = Messages.YES;
    public static final int NO = Messages.NO;
    public static final int OK = Messages.OK;
    public static final int CANCEL = Messages.CANCEL;
    
    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static ModForgeNotificationService getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeNotificationService.class);
    }
    
    /**
     * Gets the instance of the service.
     *
     * @param project The project.
     * @return The service instance.
     */
    public static ModForgeNotificationService getInstance(@Nullable Project project) {
        return getInstance();
    }
    
    /**
     * Shows an information notification.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     */
    public void showInfoNotification(@Nullable Project project, @NotNull String title, @NotNull String content) {
        showNotification(project, title, content, NotificationType.INFORMATION);
    }
    
    /**
     * Shows an information notification.
     * Simplified method without project parameter for backwards compatibility.
     *
     * @param title   The title.
     * @param content The content.
     */
    public void showInfo(@NotNull String title, @NotNull String content) {
        showInfoNotification(null, title, content);
    }
    
    /**
     * Shows an information notification.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     */
    public void showInfo(@Nullable Project project, @NotNull String title, @NotNull String content) {
        showInfoNotification(project, title, content);
    }
    
    /**
     * Shows a warning notification.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     */
    public void showWarningNotification(@Nullable Project project, @NotNull String title, @NotNull String content) {
        showNotification(project, title, content, NotificationType.WARNING);
    }
    
    /**
     * Shows an error notification.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     */
    public void showErrorNotification(@Nullable Project project, @NotNull String title, @NotNull String content) {
        showNotification(project, title, content, NotificationType.ERROR);
    }
    
    /**
     * Shows an error notification.
     * Simplified method without project parameter for backwards compatibility.
     *
     * @param title   The title.
     * @param content The content.
     */
    public void showError(@NotNull String title, @NotNull String content) {
        showErrorNotification(null, title, content);
    }
    
    /**
     * Shows an error notification.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     */
    public void showError(@Nullable Project project, @NotNull String title, @NotNull String content) {
        showErrorNotification(project, title, content);
    }
    
    /**
     * Shows a sticky notification that requires user action to dismiss.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     * @param type    The notification type.
     */
    public void showStickyNotification(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type) {
        
        Notification notification = NOTIFICATION_GROUP.createNotification(title, content, type);
        notification.setImportant(true);
        notification.notify(project);
    }
    
    /**
     * Shows a notification with an action.
     *
     * @param project      The project.
     * @param title        The title.
     * @param content      The content.
     * @param type         The notification type.
     * @param actionText   The action text.
     * @param actionRunner The action runner.
     */
    public void showNotificationWithAction(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @NotNull String actionText,
            @NotNull Runnable actionRunner) {
        
        Notification notification = NOTIFICATION_GROUP.createNotification(title, content, type);
        
        notification.addAction(NotificationAction.createSimple(actionText, () -> {
            try {
                actionRunner.run();
                notification.expire();
            } catch (Exception e) {
                LOG.error("Error executing notification action", e);
                showErrorNotification(
                        project,
                        "Action Error",
                        "Failed to execute action: " + e.getMessage()
                );
            }
        }));
        
        notification.notify(project);
    }
    
    /**
     * Shows a notification.
     *
     * @param project The project.
     * @param title   The title.
     * @param content The content.
     * @param type    The notification type.
     */
    private void showNotification(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type) {
        
        NOTIFICATION_GROUP
                .createNotification(title, content, type)
                .notify(project);
    }
    
    /**
     * Shows a dialog with Yes/No options.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param message The message to display
     * @param title   The dialog title
     * @return YES or NO constant
     */
    public int showYesNoDialog(@NotNull String message, @NotNull String title) {
        return showYesNoDialog(null, message, title, "Yes", "No", null);
    }
    
    /**
     * Shows a dialog with Yes/No options.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project The project
     * @param message The message to display
     * @param title   The dialog title
     * @return YES or NO constant
     */
    public int showYesNoDialog(@Nullable Project project, @NotNull String message, @NotNull String title) {
        return showYesNoDialog(project, message, title, "Yes", "No", null);
    }
    
    /**
     * Shows a dialog with Yes/No options.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project   The project
     * @param message   The message to display
     * @param title     The dialog title
     * @param yesText   Text for the Yes button
     * @param noText    Text for the No button
     * @param icon      Optional icon
     * @return YES or NO constant
     */
    public int showYesNoDialog(
            @Nullable Project project,
            @NotNull String message,
            @NotNull String title,
            @NotNull String yesText,
            @NotNull String noText,
            @Nullable Icon icon) {
        
        // Use application thread to show dialog
        AtomicInteger result = new AtomicInteger(NO);
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                int dialogResult = Messages.showYesNoDialog(
                        project,
                        message,
                        title,
                        yesText,
                        noText,
                        icon != null ? icon : UIUtil.getQuestionIcon()
                );
                result.set(dialogResult);
            } catch (Exception e) {
                LOG.warn("Failed to show custom Yes/No dialog with IntelliJ API, falling back to standard Yes/No", e);
                // Fall back to standard Yes/No dialog
                int dialogResult = com.modforge.intellij.plugin.utils.CompatibilityUtil.showYesNoDialog(project, message, title);
                result.set(dialogResult);
            }
        });
        
        return result.get();
    }
    
    /**
     * Shows an error dialog.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project The project
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showErrorDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            // Use CompatibilityUtil to ensure compatibility with IntelliJ IDEA 2025.1.1.1
            com.modforge.intellij.plugin.utils.CompatibilityUtil.showErrorDialog(project, message, title);
        });
    }
    
    /**
     * Shows an error dialog.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showErrorDialog(@NotNull String title, @NotNull String message) {
        showErrorDialog(null, title, message);
    }
    
    /**
     * Shows an information dialog.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project The project
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showInfoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            Messages.showInfoMessage(project, message, title);
        });
    }
    
    /**
     * Shows an information dialog.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showInfoDialog(@NotNull String title, @NotNull String message) {
        showInfoDialog(null, title, message);
    }
    
    /**
     * Shows a warning dialog.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project The project
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showWarningDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            // Use CompatibilityUtil to ensure compatibility with IntelliJ IDEA 2025.1.1.1
            com.modforge.intellij.plugin.utils.CompatibilityUtil.showWarningDialog(project, message, title);
        });
    }
    
    /**
     * Shows a warning dialog.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showWarningDialog(@NotNull String title, @NotNull String message) {
        showWarningDialog(null, title, message);
    }
    
    /**
     * Shows a dialog with OK button.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project The project
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showOkDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            Messages.showInfoMessage(project, message, title);
        });
    }
    
    /**
     * Shows a dialog with OK button.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showOkDialog(@NotNull String title, @NotNull String message) {
        showOkDialog(null, title, message);
    }

    /**
     * Shows a dialog with an input field.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project      The project
     * @param message      The message to display
     * @param title        The dialog title
     * @param initialValue The initial value to display in the input field
     * @return The text entered by the user, or null if canceled
     */
    @Nullable
    public String showInputDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title,
            @Nullable String initialValue) {
        
        // Use application thread to show dialog
        final String[] result = new String[1];
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                result[0] = Messages.showInputDialog(project, message, title, null, initialValue, null);
            } catch (Exception e) {
                LOG.warn("Failed to show input dialog with project parameter", e);
                try {
                    result[0] = Messages.showInputDialog(message, title, null, initialValue, null);
                } catch (Exception ex) {
                    LOG.error("Failed to show input dialog", ex);
                    result[0] = null;
                }
            }
        });
        
        return result[0];
    }
    
    /**
     * Shows a dialog with an input field.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param message      The message to display
     * @param title        The dialog title
     * @param initialValue The initial value to display in the input field
     * @return The text entered by the user, or null if canceled
     */
    @Nullable
    public String showInputDialog(
            @NotNull String message,
            @NotNull String title,
            @Nullable String initialValue) {
        return showInputDialog(null, message, title, initialValue);
    }
    
    /**
     * Shows a dialog with a dropdown selector.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param project      The project
     * @param message      The message to display
     * @param title        The dialog title
     * @param options      The array of options to choose from
     * @param initialValue The initially selected value
     * @return The index of the selected option, or -1 if canceled
     */
    public int showChooseDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title,
            @NotNull String[] options,
            @NotNull String initialValue) {
        
        // Use application thread to show dialog
        final int[] result = new int[1];
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                result[0] = Messages.showChooseDialog(project, message, title, options, initialValue, null);
            } catch (Exception e) {
                LOG.warn("Failed to show choose dialog with project parameter", e);
                try {
                    result[0] = Messages.showChooseDialog(message, title, options, initialValue, null);
                } catch (Exception ex) {
                    LOG.error("Failed to show choose dialog", ex);
                    result[0] = -1;
                }
            }
        });
        
        return result[0];
    }
    
    /**
     * Shows a dialog with a dropdown selector.
     * Compatible with IntelliJ IDEA 2025.1.1.1
     *
     * @param message      The message to display
     * @param title        The dialog title
     * @param options      The array of options to choose from
     * @param initialValue The initially selected value
     * @return The index of the selected option, or -1 if canceled
     */
    public int showChooseDialog(
            @NotNull String message,
            @NotNull String title,
            @NotNull String[] options,
            @NotNull String initialValue) {
        return showChooseDialog(null, message, title, options, initialValue);
    }
    
    /**
     * Shows an information message as a notification (not a dialog).
     * Added for compatibility with code that expects this method.
     *
     * @param project The project
     * @param title   The notification title
     * @param message The notification message
     */
    public void showInfoMessage(@Nullable Project project, @NotNull String title, @NotNull String message) {
        showInfoNotification(project, title, message);
    }
    
    /**
     * Shows an information message as a notification (not a dialog).
     * Added for compatibility with code that expects this method.
     *
     * @param title   The notification title
     * @param message The notification message
     */
    public void showInfoMessage(@NotNull String title, @NotNull String message) {
        showInfoMessage(null, title, message);
    }
}