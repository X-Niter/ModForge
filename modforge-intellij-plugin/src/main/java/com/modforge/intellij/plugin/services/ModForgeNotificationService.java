package com.modforge.intellij.plugin.services;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for ModForge notifications.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class ModForgeNotificationService {
    private static final Logger LOG = Logger.getInstance(ModForgeNotificationService.class);
    
    private static final NotificationGroup NOTIFICATION_GROUP = 
            NotificationGroupManager.getInstance().getNotificationGroup("ModForge Notifications");
    
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
}