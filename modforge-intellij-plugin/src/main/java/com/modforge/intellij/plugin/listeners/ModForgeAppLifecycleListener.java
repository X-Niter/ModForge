package com.modforge.intellij.plugin.listeners;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Listener for application lifecycle events.
 * This listener manages global plugin state across the entire IDE.
 */
public class ModForgeAppLifecycleListener implements AppLifecycleListener {
    private static final Logger LOG = Logger.getInstance(ModForgeAppLifecycleListener.class);
    
    private ScheduledFuture<?> updateCheckTask;
    private MessageBusConnection messageBusConnection;
    
    @Override
    public void appStarted() {
        LOG.info("ModForge application started");
        
        // Initialize global state
        initializeGlobalState();
        
        // Schedule update checks
        scheduleUpdateChecks();
        
        // Register for global events
        registerGlobalEvents();
    }
    
    @Override
    public void appWillBeClosed(boolean isRestart) {
        LOG.info("ModForge application will be closed, isRestart: " + isRestart);
        
        // Cancel update checks
        if (updateCheckTask != null && !updateCheckTask.isCancelled() && !updateCheckTask.isDone()) {
            updateCheckTask.cancel(true);
            updateCheckTask = null;
        }
        
        // Unregister from global events
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
            messageBusConnection = null;
        }
        
        // Cleanup global state
        cleanupGlobalState();
    }
    
    /**
     * Initializes global state.
     */
    private void initializeGlobalState() {
        LOG.info("Initializing ModForge global state");
        
        // Load settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Initialize global services if needed
    }
    
    /**
     * Schedules update checks.
     */
    private void scheduleUpdateChecks() {
        LOG.info("Scheduling ModForge update checks");
        
        // Schedule update checks (once a day)
        updateCheckTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                this::checkForUpdates,
                1, 24, TimeUnit.HOURS
        );
    }
    
    /**
     * Registers for global events.
     */
    private void registerGlobalEvents() {
        LOG.info("Registering ModForge global events");
        
        // Register for application-wide events
        messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        
        // Add more event subscriptions as needed
    }
    
    /**
     * Cleans up global state.
     */
    private void cleanupGlobalState() {
        LOG.info("Cleaning up ModForge global state");
        
        // Save settings if needed
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Clean up any global resources
    }
    
    /**
     * Checks for updates.
     */
    private void checkForUpdates() {
        LOG.info("Checking for ModForge updates");
        
        // In a real implementation, this would check for updates from a server
        boolean updateAvailable = false;
        
        if (updateAvailable) {
            notifyUpdateAvailable();
        }
    }
    
    /**
     * Notifies the user of an available update.
     */
    private void notifyUpdateAvailable() {
        LOG.info("ModForge update available");
        
        // Notify all open projects
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (!project.isDisposed()) {
                com.intellij.notification.Notification notification = new com.intellij.notification.Notification(
                        "ModForge Notifications",
                        "ModForge Update Available",
                        "A new version of ModForge is available. Please update to get the latest features and fixes.",
                        com.intellij.notification.NotificationType.INFORMATION
                );
                
                notification.addAction(new com.intellij.notification.NotificationAction("Update Now") {
                    @Override
                    public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, 
                                               @NotNull com.intellij.notification.Notification notification) {
                        // In a real implementation, this would initiate the update process
                        com.intellij.ide.actions.ShowPluginManagerAction.showPluginManager(e);
                        notification.expire();
                    }
                });
                
                com.intellij.notification.Notifications.Bus.notify(notification, project);
            }
        }
    }
}