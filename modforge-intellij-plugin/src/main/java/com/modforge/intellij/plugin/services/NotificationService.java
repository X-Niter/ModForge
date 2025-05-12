package com.modforge.intellij.plugin.services;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for displaying notifications to the user.
 * Handles notification grouping, display, and actions.
 */
@Service(Service.Level.PROJECT)
public final class NotificationService {
    private static final Logger LOG = Logger.getInstance(NotificationService.class);
    private static final String NOTIFICATION_GROUP_ID = "ModForge Notifications";
    
    private final Project project;
    
    /**
     * Constructor.
     * @param project The project
     */
    public NotificationService(Project project) {
        this.project = project;
        LOG.info("NotificationService created for project: " + project.getName());
    }
    
    /**
     * Show an info notification.
     * @param title The notification title
     * @param content The notification content
     */
    public void showInfoNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.INFORMATION, null, null);
    }
    
    /**
     * Show a warning notification.
     * @param title The notification title
     * @param content The notification content
     */
    public void showWarningNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.WARNING, null, null);
    }
    
    /**
     * Show an error notification.
     * @param title The notification title
     * @param content The notification content
     */
    public void showErrorNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.ERROR, null, null);
    }
    
    /**
     * Show a notification with an action.
     * @param title The notification title
     * @param content The notification content
     * @param type The notification type
     * @param actionText The action text
     * @param runnable The action to run when clicked
     */
    public void showNotification(@NotNull String title, @NotNull String content, 
                                 @NotNull NotificationType type, 
                                 @Nullable String actionText, 
                                 @Nullable Runnable runnable) {
        // Check if notifications are enabled in settings
        if (!ModForgeSettings.getInstance().isEnableNotifications()) {
            LOG.info("Notifications are disabled, not showing notification: " + title);
            return;
        }
        
        try {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(content, type)
                    .setTitle(title);
            
            if (actionText != null && runnable != null) {
                notification.addAction(new NotificationAction(actionText) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        notification.expire();
                        runnable.run();
                    }
                });
            }
            
            notification.notify(project);
            LOG.info("Showed notification: " + title);
        } catch (Exception e) {
            LOG.error("Error showing notification: " + title, e);
        }
    }
}