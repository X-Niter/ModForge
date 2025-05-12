package com.modforge.intellij.plugin.services;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for managing notifications.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class ModForgeNotificationService {
    private static final Logger LOG = Logger.getInstance(ModForgeNotificationService.class);
    
    // Notification groups
    private static final String INFO_GROUP_ID = "ModForge.Info";
    private static final String WARNING_GROUP_ID = "ModForge.Warning";
    private static final String ERROR_GROUP_ID = "ModForge.Error";
    private static final String BALLOON_GROUP_ID = "ModForge.Balloon";
    
    // Notification types
    private static final NotificationType INFO_TYPE = NotificationType.INFORMATION;
    private static final NotificationType WARNING_TYPE = NotificationType.WARNING;
    private static final NotificationType ERROR_TYPE = NotificationType.ERROR;
    
    /**
     * Constructor.
     */
    public ModForgeNotificationService() {
        // Register notification groups
        NotificationGroupManager manager = NotificationGroupManager.getInstance();
        
        // Check if groups are already registered
        boolean infoGroupExists = findNotificationGroup(INFO_GROUP_ID) != null;
        boolean warningGroupExists = findNotificationGroup(WARNING_GROUP_ID) != null;
        boolean errorGroupExists = findNotificationGroup(ERROR_GROUP_ID) != null;
        boolean balloonGroupExists = findNotificationGroup(BALLOON_GROUP_ID) != null;
        
        // Log notification group status
        LOG.info("Notification groups status: Info=" + infoGroupExists +
                ", Warning=" + warningGroupExists +
                ", Error=" + errorGroupExists +
                ", Balloon=" + balloonGroupExists);
    }

    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static ModForgeNotificationService getInstance() {
        return ApplicationManager.getApplication().getService(ModForgeNotificationService.class);
    }

    /**
     * Shows an info notification.
     *
     * @param project The project.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showInfo(@Nullable Project project, @NotNull String title, @NotNull String content) {
        show(project, INFO_GROUP_ID, INFO_TYPE, title, content);
    }

    /**
     * Shows a warning notification.
     *
     * @param project The project.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showWarning(@Nullable Project project, @NotNull String title, @NotNull String content) {
        show(project, WARNING_GROUP_ID, WARNING_TYPE, title, content);
    }

    /**
     * Shows an error notification.
     *
     * @param project The project.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showError(@Nullable Project project, @NotNull String title, @NotNull String content) {
        show(project, ERROR_GROUP_ID, ERROR_TYPE, title, content);
    }

    /**
     * Shows a balloon notification.
     *
     * @param project The project.
     * @param type    The notification type.
     * @param title   The notification title.
     * @param content The notification content.
     */
    public void showBalloon(@Nullable Project project, @NotNull NotificationType type, @NotNull String title, @NotNull String content) {
        show(project, BALLOON_GROUP_ID, type, title, content);
    }

    /**
     * Shows a notification.
     *
     * @param project  The project.
     * @param groupId  The notification group ID.
     * @param type     The notification type.
     * @param title    The notification title.
     * @param content  The notification content.
     */
    private void show(@Nullable Project project, @NotNull String groupId, @NotNull NotificationType type, @NotNull String title, @NotNull String content) {
        NotificationGroup group = findNotificationGroup(groupId);
        
        if (group == null) {
            LOG.error("Notification group not found: " + groupId + ". Using a fallback group.");
            
            // Create a fallback notification group
            group = NotificationGroupManager.getInstance().getNotificationGroup(BALLOON_GROUP_ID);
            
            if (group == null) {
                // Last resort fallback
                LOG.error("Fallback notification group not found. Cannot show notification.");
                return;
            }
        }
        
        try {
            Notification notification = group.createNotification(title, content, type);
            notification.notify(project);
        } catch (Exception e) {
            LOG.error("Failed to show notification", e);
        }
    }

    /**
     * Finds a notification group by ID.
     *
     * @param groupId The notification group ID.
     * @return The notification group or null if not found.
     */
    @Nullable
    private NotificationGroup findNotificationGroup(@NotNull String groupId) {
        try {
            return NotificationGroupManager.getInstance().getNotificationGroup(groupId);
        } catch (Exception e) {
            LOG.error("Failed to find notification group: " + groupId, e);
            return null;
        }
    }

    /**
     * Shows a sticky notification that requires user acknowledgment.
     *
     * @param project  The project.
     * @param type     The notification type.
     * @param title    The notification title.
     * @param content  The notification content.
     * @param listener The notification listener.
     */
    public void showStickyNotification(
            @Nullable Project project,
            @NotNull NotificationType type,
            @NotNull String title,
            @NotNull String content,
            @Nullable NotificationListener listener) {
        
        NotificationGroup group = findNotificationGroup(BALLOON_GROUP_ID);
        
        if (group == null) {
            LOG.error("Notification group not found: " + BALLOON_GROUP_ID);
            return;
        }
        
        try {
            Notification notification = group.createNotification(title, content, type);
            
            if (listener != null) {
                notification.setListener(listener);
            }
            
            // Make the notification sticky
            notification.setImportant(true);
            
            notification.notify(project);
        } catch (Exception e) {
            LOG.error("Failed to show sticky notification", e);
        }
    }

    /**
     * Shows a notification with an action.
     *
     * @param project        The project.
     * @param type           The notification type.
     * @param title          The notification title.
     * @param content        The notification content.
     * @param actionText     The action text.
     * @param actionCallback The action callback.
     */
    public void showNotificationWithAction(
            @Nullable Project project,
            @NotNull NotificationType type,
            @NotNull String title,
            @NotNull String content,
            @NotNull String actionText,
            @NotNull Runnable actionCallback) {
        
        NotificationGroup group = findNotificationGroup(BALLOON_GROUP_ID);
        
        if (group == null) {
            LOG.error("Notification group not found: " + BALLOON_GROUP_ID);
            return;
        }
        
        try {
            Notification notification = group.createNotification(title, content, type);
            
            notification.addAction(NotificationAction.createSimple(actionText, actionCallback));
            
            notification.notify(project);
        } catch (Exception e) {
            LOG.error("Failed to show notification with action", e);
        }
    }
}