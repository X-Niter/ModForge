package com.modforge.intellij.plugin.services;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for showing notifications in the IDE.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class ModForgeNotificationService {
    private static final String DISPLAY_ID = "ModForge";
    private static final NotificationGroup INFO_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("ModForge.Info");
    private static final NotificationGroup WARNING_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("ModForge.Warning");
    private static final NotificationGroup ERROR_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("ModForge.Error");

    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static ModForgeNotificationService getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeNotificationService.class);
    }

    /**
     * Shows an information notification.
     *
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showInfo(@NotNull String title, @NotNull String content) {
        showNotification(INFO_GROUP, title, content, NotificationType.INFORMATION, null);
    }

    /**
     * Shows an information notification with a project.
     *
     * @param project The project.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showInfo(@NotNull Project project, @NotNull String title, @NotNull String content) {
        showNotification(INFO_GROUP, title, content, NotificationType.INFORMATION, project);
    }

    /**
     * Shows a warning notification.
     *
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showWarning(@NotNull String title, @NotNull String content) {
        showNotification(WARNING_GROUP, title, content, NotificationType.WARNING, null);
    }

    /**
     * Shows a warning notification with a project.
     *
     * @param project The project.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showWarning(@NotNull Project project, @NotNull String title, @NotNull String content) {
        showNotification(WARNING_GROUP, title, content, NotificationType.WARNING, project);
    }

    /**
     * Shows an error notification.
     *
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showError(@NotNull String title, @NotNull String content) {
        showNotification(ERROR_GROUP, title, content, NotificationType.ERROR, null);
    }

    /**
     * Shows an error notification with a project.
     *
     * @param project The project.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showError(@NotNull Project project, @NotNull String title, @NotNull String content) {
        showNotification(ERROR_GROUP, title, content, NotificationType.ERROR, project);
    }

    /**
     * Shows a notification.
     *
     * @param group   The notification group.
     * @param title   The notification title.
     * @param content The notification content.
     * @param type    The notification type.
     * @param project The project.
     */
    private void showNotification(
            @NotNull NotificationGroup group,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable Project project) {
        
        Notification notification = group.createNotification(title, content, type);
        
        if (project != null && !project.isDisposed()) {
            notification.notify(project);
        } else {
            notification.notify(null);
        }
    }

    /**
     * Shows a balloon notification.
     *
     * @param title   The notification title.
     * @param content The notification content.
     * @param type    The notification type.
     * @param project The project.
     */
    public void showBalloon(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable Project project) {
        
        NotificationGroup balloonGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("ModForge.Balloon");
        
        Notification notification = balloonGroup.createNotification(title, content, type);
        
        if (project != null && !project.isDisposed()) {
            notification.notify(project);
        } else {
            notification.notify(null);
        }
    }

    /**
     * Shows a notification with actions.
     *
     * @param title         The notification title.
     * @param content       The notification content.
     * @param type          The notification type.
     * @param project       The project.
     * @param actionLabels  The action labels.
     * @param actionHandler The action handler.
     */
    public void showNotificationWithActions(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable Project project,
            @NotNull String[] actionLabels,
            @NotNull NotificationListener.UrlOpeningListener actionHandler) {
        
        NotificationGroup group;
        
        switch (type) {
            case INFORMATION:
                group = INFO_GROUP;
                break;
            case WARNING:
                group = WARNING_GROUP;
                break;
            case ERROR:
                group = ERROR_GROUP;
                break;
            default:
                group = INFO_GROUP;
        }
        
        Notification notification = group.createNotification(title, content, type);
        
        for (String label : actionLabels) {
            notification.addAction(NotificationAction.createSimple(label, () -> {
                actionHandler.hyperlinkActivated(notification, null);
            }));
        }
        
        if (project != null && !project.isDisposed()) {
            notification.notify(project);
        } else {
            notification.notify(null);
        }
    }

    /**
     * Shows a sticky notification that stays until explicitly closed.
     *
     * @param title   The notification title.
     * @param content The notification content.
     * @param type    The notification type.
     * @param project The project.
     * @return The notification object that can be used to dismiss the notification.
     */
    @NotNull
    public Notification showStickyNotification(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable Project project) {
        
        NotificationGroup group;
        
        switch (type) {
            case INFORMATION:
                group = INFO_GROUP;
                break;
            case WARNING:
                group = WARNING_GROUP;
                break;
            case ERROR:
                group = ERROR_GROUP;
                break;
            default:
                group = INFO_GROUP;
        }
        
        Notification notification = group.createNotification(title, content, type);
        notification.setImportant(true);
        
        if (project != null && !project.isDisposed()) {
            notification.notify(project);
        } else {
            notification.notify(null);
        }
        
        return notification;
    }

    /**
     * Dismisses a notification.
     *
     * @param notification The notification to dismiss.
     */
    public void dismissNotification(@NotNull Notification notification) {
        notification.expire();
    }
}