package com.modforge.intellij.plugin.notifications;

import com.intellij.notification.*;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for showing notifications to the user.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeNotificationService {
    private static final Logger LOG = Logger.getInstance(ModForgeNotificationService.class);
    
    public static final NotificationGroup GROUP = NotificationGroupManager.getInstance()
            .getNotificationGroup("ModForge Notifications");
    
    private final Project project;
    private final Map<String, Notification> activeNotifications = new HashMap<>();
    
    /**
     * Create a notification service.
     *
     * @param project The project
     */
    public ModForgeNotificationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Show an info notification.
     *
     * @param title   The title
     * @param content The content
     * @return The notification ID
     */
    @NotNull
    public String showInfoNotification(@NotNull String title, @NotNull String content) {
        return showNotification(title, content, NotificationType.INFORMATION, null);
    }
    
    /**
     * Show a warning notification.
     *
     * @param title   The title
     * @param content The content
     * @return The notification ID
     */
    @NotNull
    public String showWarningNotification(@NotNull String title, @NotNull String content) {
        return showNotification(title, content, NotificationType.WARNING, null);
    }
    
    /**
     * Show an error notification.
     *
     * @param title   The title
     * @param content The content
     * @return The notification ID
     */
    @NotNull
    public String showErrorNotification(@NotNull String title, @NotNull String content) {
        return showNotification(title, content, NotificationType.ERROR, null);
    }
    
    /**
     * Show a notification with a hyperlink listener.
     *
     * @param title       The title
     * @param content     The content
     * @param type        The notification type
     * @param listener    The hyperlink listener
     * @return The notification ID
     */
    @NotNull
    public String showNotification(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @Nullable NotificationListener listener
    ) {
        LOG.info("Showing notification: " + title + " - " + content);
        
        String id = generateId(title);
        
        Notification notification = GROUP.createNotification(title, content, type);
        if (listener != null) {
            notification.setListener(listener);
        }
        
        notification.notify(project);
        
        // Remember notification
        activeNotifications.put(id, notification);
        
        return id;
    }
    
    /**
     * Show a notification with actions.
     *
     * @param title   The title
     * @param content The content
     * @param type    The notification type
     * @param actions The actions
     * @return The notification ID
     */
    @NotNull
    public String showNotificationWithActions(
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @NotNull NotificationAction... actions
    ) {
        LOG.info("Showing notification with actions: " + title + " - " + content);
        
        String id = generateId(title);
        
        Notification notification = GROUP.createNotification(title, content, type);
        for (NotificationAction action : actions) {
            notification.addAction(action);
        }
        
        notification.notify(project);
        
        // Remember notification
        activeNotifications.put(id, notification);
        
        return id;
    }
    
    /**
     * Update an existing notification.
     *
     * @param id          The notification ID
     * @param newContent  The new content
     * @param type        The new notification type
     * @return True if the notification was updated, false otherwise
     */
    public boolean updateNotification(
            @NotNull String id,
            @NotNull String newContent,
            @NotNull NotificationType type
    ) {
        Notification notification = activeNotifications.get(id);
        if (notification == null) {
            LOG.warn("Cannot update notification with ID " + id + ": not found");
            return false;
        }
        
        notification.update(notification.getTitle(), newContent, type);
        return true;
    }
    
    /**
     * Expire a notification.
     *
     * @param id The notification ID
     * @return True if the notification was expired, false otherwise
     */
    public boolean expireNotification(@NotNull String id) {
        Notification notification = activeNotifications.get(id);
        if (notification == null) {
            LOG.warn("Cannot expire notification with ID " + id + ": not found");
            return false;
        }
        
        notification.expire();
        activeNotifications.remove(id);
        return true;
    }
    
    /**
     * Expire all notifications.
     */
    public void expireAllNotifications() {
        for (Notification notification : activeNotifications.values()) {
            notification.expire();
        }
        
        activeNotifications.clear();
    }
    
    /**
     * Generate a unique ID for a notification.
     *
     * @param title The notification title
     * @return The ID
     */
    @NotNull
    private String generateId(@NotNull String title) {
        String baseId = title.replaceAll("\\s+", "-").toLowerCase();
        
        if (!activeNotifications.containsKey(baseId)) {
            return baseId;
        }
        
        // Add a unique suffix
        int suffix = 1;
        while (activeNotifications.containsKey(baseId + "-" + suffix)) {
            suffix++;
        }
        
        return baseId + "-" + suffix;
    }
    
    /**
     * Create a hyperlink listener.
     *
     * @param handler The handler
     * @return The listener
     */
    @NotNull
    public static NotificationListener createHyperlinkListener(@NotNull HyperlinkHandler handler) {
        return new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                handler.handleHyperlink(e.getDescription());
            }
        };
    }
    
    /**
     * Interface for handling hyperlinks in notifications.
     */
    public interface HyperlinkHandler {
        /**
         * Handle a hyperlink.
         *
         * @param url The URL
         */
        void handleHyperlink(@NotNull String url);
    }
}