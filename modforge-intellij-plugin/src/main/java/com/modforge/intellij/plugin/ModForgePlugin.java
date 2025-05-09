package com.modforge.intellij.plugin;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Main plugin initialization class.
 * Handles plugin startup and initialization.
 */
public class ModForgePlugin implements StartupActivity, ProjectManagerListener {
    private static final Logger LOG = Logger.getInstance(ModForgePlugin.class);
    private static final NotificationGroup NOTIFICATION_GROUP = 
            NotificationGroupManager.getInstance().getNotificationGroup("ModForge Notifications");
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("ModForge plugin initializing for project: " + project.getName());
        
        // Register project listener
        ProjectManager.getInstance().addProjectManagerListener(project, this);
        
        // Check API key
        checkApiKey(project);
        
        // Show welcome notification
        showWelcomeNotification(project);
        
        // Schedule periodic updates check
        scheduleUpdatesCheck(project);
    }
    
    /**
     * Checks if the API key is set.
     * @param project The project
     */
    private void checkApiKey(@NotNull Project project) {
        String apiKey = ModForgeSettings.getInstance().getOpenAiApiKey();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Notification notification = NOTIFICATION_GROUP.createNotification(
                    "ModForge API Key Required",
                    "ModForge requires an OpenAI API key to function properly. " +
                    "Please configure it in the settings.",
                    NotificationType.WARNING
            );
            
            notification.addAction(NotificationAction.createSimple("Open Settings", () -> {
                // Open settings dialog
                project.getComponent("com.modforge.intellij.plugin.settings.ModForgeSettings");
            }));
            
            notification.notify(project);
        }
    }
    
    /**
     * Shows a welcome notification.
     * @param project The project
     */
    private void showWelcomeNotification(@NotNull Project project) {
        IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(
                PluginId.getId("com.modforge.intellij.plugin"));
        
        if (pluginDescriptor != null) {
            Notification notification = NOTIFICATION_GROUP.createNotification(
                    "ModForge " + pluginDescriptor.getVersion() + " Initialized",
                    "ModForge is ready to assist with your Minecraft mod development.",
                    NotificationType.INFORMATION
            );
            
            notification.addAction(NotificationAction.createSimple("Open Documentation", () -> {
                // Open browser to documentation
                com.intellij.ide.BrowserUtil.browse("https://modforge.io/docs");
            }));
            
            notification.notify(project);
        }
    }
    
    /**
     * Schedules periodic checks for updates.
     * @param project The project
     */
    private void scheduleUpdatesCheck(@NotNull Project project) {
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                () -> checkForUpdates(project),
                1, 24, TimeUnit.HOURS
        );
    }
    
    /**
     * Checks for plugin updates.
     * @param project The project
     */
    private void checkForUpdates(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking for ModForge Updates", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // This is a simplified implementation
                // In a real implementation, we would check for updates from a server
                
                // Only show notification if we find an update
                boolean updateAvailable = false;
                
                if (updateAvailable) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Notification notification = NOTIFICATION_GROUP.createNotification(
                                "ModForge Update Available",
                                "A new version of ModForge is available.",
                                NotificationType.INFORMATION
                        );
                        
                        notification.addAction(NotificationAction.createSimple("Update Now", () -> {
                            // Open plugins dialog
                            com.intellij.ide.actions.ShowPluginManagerAction.showPluginManager();
                        }));
                        
                        notification.notify(project);
                    });
                }
            }
        });
    }
    
    @Override
    public void projectOpened(@NotNull Project project) {
        LOG.info("Project opened: " + project.getName());
        
        // Initialize project-specific components
        // This is done in runActivity, but we also do it here for projects that are already open
    }
    
    @Override
    public void projectClosing(@NotNull Project project) {
        LOG.info("Project closing: " + project.getName());
        
        // Clean up project-specific resources
    }
}