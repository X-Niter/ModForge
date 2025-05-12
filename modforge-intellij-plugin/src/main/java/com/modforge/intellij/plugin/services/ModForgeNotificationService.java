package com.modforge.intellij.plugin.services;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification service for ModForge.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.PROJECT)
public final class ModForgeNotificationService {
    private static final Logger LOG = Logger.getInstance(ModForgeNotificationService.class);
    private static final String NOTIFICATION_GROUP_ID = "ModForge.Notifications";
    
    private final Project project;
    private final List<Notification> activeNotifications = new ArrayList<>();

    /**
     * Creates a new instance of the notification service.
     *
     * @param project The project.
     */
    public ModForgeNotificationService(Project project) {
        this.project = project;
        LOG.info("ModForgeNotificationService initialized for project: " + project.getName());
    }

    /**
     * Gets the instance of the notification service for the specified project.
     *
     * @param project The project.
     * @return The notification service.
     */
    public static ModForgeNotificationService getInstance(@NotNull Project project) {
        return project.getService(ModForgeNotificationService.class);
    }

    /**
     * Shows an information notification.
     *
     * @param title The title.
     * @param content The content.
     * @return The notification.
     */
    @NotNull
    public Notification showInfo(@NotNull String title, @NotNull String content) {
        return showNotification(title, content, NotificationType.INFORMATION, null);
    }

    /**
     * Shows a warning notification.
     *
     * @param title The title.
     * @param content The content.
     * @return The notification.
     */
    @NotNull
    public Notification showWarning(@NotNull String title, @NotNull String content) {
        return showNotification(title, content, NotificationType.WARNING, null);
    }

    /**
     * Shows an error notification.
     *
     * @param title The title.
     * @param content The content.
     * @return The notification.
     */
    @NotNull
    public Notification showError(@NotNull String title, @NotNull String content) {
        return showNotification(title, content, NotificationType.ERROR, null);
    }

    /**
     * Shows an information notification with actions.
     *
     * @param title The title.
     * @param content The content.
     * @param actions The actions.
     * @return The notification.
     */
    @NotNull
    public Notification showInfoWithActions(
            @NotNull String title,
            @NotNull String content,
            @NotNull List<AnAction> actions) {
        return showNotification(title, content, NotificationType.INFORMATION, actions);
    }

    /**
     * Shows a warning notification with actions.
     *
     * @param title The title.
     * @param content The content.
     * @param actions The actions.
     * @return The notification.
     */
    @NotNull
    public Notification showWarningWithActions(
            @NotNull String title,
            @NotNull String content,
            @NotNull List<AnAction> actions) {
        return showNotification(title, content, NotificationType.WARNING, actions);
    }

    /**
     * Shows an error notification with actions.
     *
     * @param title The title.
     * @param content The content.
     * @param actions The actions.
     * @return The notification.
     */
    @NotNull
    public Notification showErrorWithActions(
            @NotNull String title,
            @NotNull String content,
            @NotNull List<AnAction> actions) {
        return showNotification(title, content, NotificationType.ERROR, actions);
    }

    /**
     * Shows a notification with the specified type and actions.
     *
     * @param title The title.
     * @param content The content.
     * @param type The type.
     * @param actions The actions.
     * @return The notification.
     */
    @NotNull
    public Notification showNotification(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable List<AnAction> actions) {
        
        LOG.info("Showing notification: " + title + " - " + content);
        
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, content, type);
        
        if (actions != null) {
            for (AnAction action : actions) {
                if (action instanceof NotificationAction) {
                    notification.addAction((NotificationAction) action);
                }
            }
        }
        
        // Add a close action that removes the notification from the active list
        notification.whenExpired(() -> {
            LOG.info("Notification expired: " + title);
            activeNotifications.remove(notification);
        });
        
        notification.notify(project);
        activeNotifications.add(notification);
        
        return notification;
    }

    /**
     * Expires all active notifications.
     */
    public void expireAllNotifications() {
        LOG.info("Expiring all notifications");
        
        for (Notification notification : new ArrayList<>(activeNotifications)) {
            notification.expire();
        }
        
        activeNotifications.clear();
    }
}