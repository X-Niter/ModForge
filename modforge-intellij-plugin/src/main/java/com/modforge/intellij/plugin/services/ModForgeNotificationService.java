package com.modforge.intellij.plugin.services;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for showing notifications in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ModForgeNotificationService {
    private final Project project;
    private static final String NOTIFICATION_GROUP_ID = "ModForge Notifications";

    public ModForgeNotificationService(Project project) {
        this.project = project;
    }

    /**
     * Shows an information notification.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showInfo(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.INFORMATION, null);
    }
    
    /**
     * Shows an information notification with automatic expiry.
     * This version is used by AutonomousCodeGenerationService and other services.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showInfoNotification(@NotNull String title, @NotNull String content) {
        showInfo(title, content);
    }

    /**
     * Shows an error notification.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showError(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.ERROR, null);
    }

    /**
     * Shows a warning notification.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showWarning(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.WARNING, null);
    }

    /**
     * Shows an information notification with actions.
     *
     * @param title   The notification title
     * @param content The notification content
     * @param actions The actions to add to the notification
     */
    public void showInfoWithActions(@NotNull String title, @NotNull String content, @NotNull List<AnAction> actions) {
        showNotification(title, content, NotificationType.INFORMATION, actions);
    }

    /**
     * Shows an error notification with actions.
     *
     * @param title   The notification title
     * @param content The notification content
     * @param actions The actions to add to the notification
     */
    public void showErrorWithActions(@NotNull String title, @NotNull String content, @NotNull List<AnAction> actions) {
        showNotification(title, content, NotificationType.ERROR, actions);
    }

    /**
     * Shows a warning notification with actions.
     *
     * @param title   The notification title
     * @param content The notification content
     * @param actions The actions to add to the notification
     */
    public void showWarningWithActions(@NotNull String title, @NotNull String content, @NotNull List<AnAction> actions) {
        showNotification(title, content, NotificationType.WARNING, actions);
    }

    /**
     * Shows a notification with the specified type and optional actions.
     *
     * @param title       The notification title
     * @param content     The notification content
     * @param type        The notification type
     * @param actions     The actions to add to the notification, or null for no actions
     */
    private void showNotification(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable List<AnAction> actions) {
        
        // Create notification using the notification group manager
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content, type)
                .setTitle(title);

        // Add actions if provided
        if (actions != null && !actions.isEmpty()) {
            for (AnAction action : actions) {
                if (action instanceof NotificationAction) {
                    notification.addAction((NotificationAction) action);
                }
            }
        }

        // Show the notification
        notification.notify(project);
    }

    /**
     * Creates an action builder for building notification actions.
     *
     * @return A new action builder
     */
    public ActionBuilder createActionBuilder() {
        return new ActionBuilder();
    }

    /**
     * Builder class for creating notification actions.
     */
    public static class ActionBuilder {
        private final List<AnAction> actions = new ArrayList<>();

        /**
         * Adds an action to the builder.
         *
         * @param action The action to add
         * @return This builder
         */
        public ActionBuilder addAction(@NotNull AnAction action) {
            actions.add(action);
            return this;
        }

        /**
         * Gets the list of actions.
         *
         * @return The list of actions
         */
        public List<AnAction> getActions() {
            return actions;
        }
    }
}