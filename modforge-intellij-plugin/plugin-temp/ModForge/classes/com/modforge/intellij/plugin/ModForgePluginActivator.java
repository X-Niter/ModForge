package com.modforge.intellij.plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin activator that runs at startup for ModForge IntelliJ IDEA 2025.1 Plugin.
 */
public class ModForgePluginActivator implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(ModForgePluginActivator.class);
    private static final String PLUGIN_ID = "com.modforge.intellij.plugin";

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("ModForge plugin activating for project: " + project.getName());
        
        // Welcome notification
        Notification notification = new Notification(
            "ModForge Notifications",
            "ModForge Plugin Ready",
            "The ModForge plugin is now ready for use with IntelliJ IDEA 2025.1.",
            NotificationType.INFORMATION
        );
        
        Notifications.Bus.notify(notification, project);
    }
}
