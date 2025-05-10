package com.modforge.intellij.plugin.notifications;

import com.intellij.notification.*;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for displaying ModForge notifications.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeNotificationService {
    private static final NotificationGroup MODFORGE_NOTIFICATION_GROUP = 
            NotificationGroupManager.getInstance().getNotificationGroup("ModForge Notifications");
    
    private final Project project;
    
    /**
     * Create a new notification service.
     *
     * @param project The project
     */
    public ModForgeNotificationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Show an info notification.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showInfoNotification(
            @NotNull @NlsContexts.NotificationTitle String title,
            @NotNull @NlsContexts.NotificationContent String content
    ) {
        showInfoNotification(title, content, null);
    }
    
    /**
     * Show an info notification with a URL.
     *
     * @param title   The notification title
     * @param content The notification content
     * @param url     The URL to open when the notification is clicked
     */
    public void showInfoNotification(
            @NotNull @NlsContexts.NotificationTitle String title,
            @NotNull @NlsContexts.NotificationContent String content,
            @Nullable String url
    ) {
        Notification notification = MODFORGE_NOTIFICATION_GROUP.createNotification(
                title,
                content,
                NotificationType.INFORMATION
        );
        
        if (url != null && !url.isEmpty()) {
            notification.setListener(NotificationListener.URL_OPENING_LISTENER);
            notification.setContent(content + " <a href=\"" + url + "\">Open</a>");
        }
        
        notification.notify(project);
    }
    
    /**
     * Show a warning notification.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showWarningNotification(
            @NotNull @NlsContexts.NotificationTitle String title,
            @NotNull @NlsContexts.NotificationContent String content
    ) {
        MODFORGE_NOTIFICATION_GROUP.createNotification(
                title,
                content,
                NotificationType.WARNING
        ).notify(project);
    }
    
    /**
     * Show an error notification.
     *
     * @param title   The notification title
     * @param content The notification content
     */
    public void showErrorNotification(
            @NotNull @NlsContexts.NotificationTitle String title,
            @NotNull @NlsContexts.NotificationContent String content
    ) {
        MODFORGE_NOTIFICATION_GROUP.createNotification(
                title,
                content,
                NotificationType.ERROR
        ).notify(project);
    }
}