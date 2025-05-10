package com.modforge.intellij.plugin;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.modforge.intellij.plugin.utils.CompatibilityValidator;
import com.modforge.intellij.plugin.utils.EnvironmentValidator;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin activator that runs at startup to check compatibility and set up the plugin.
 */
public class ModForgePluginActivator implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(ModForgePluginActivator.class);
    private static final String PLUGIN_ID = "com.modforge.intellij.plugin";

    @Override
    public void runActivity(@NotNull Project project) {
        // Only run this once per plugin load
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        LOG.info("ModForge plugin activating for project: " + project.getName());
        
        // Check compatibility
        checkCompatibility(project);
        
        // Register project open/close listener
        ApplicationManager.getApplication().getMessageBus().connect()
            .subscribe(ProjectManagerListener.TOPIC, new ProjectManagerListener() {
                @Override
                public void projectOpened(@NotNull Project project) {
                    LOG.info("Project opened: " + project.getName());
                }

                @Override
                public void projectClosed(@NotNull Project project) {
                    LOG.info("Project closed: " + project.getName());
                }
            });
    }
    
    /**
     * Check if the plugin is compatible with the current environment.
     * 
     * @param project The current project
     */
    private void checkCompatibility(Project project) {
        try {
            LOG.info("Checking ModForge plugin compatibility...");
            
            // Check basic compatibility first
            boolean isCompatible = CompatibilityValidator.checkAllCompatibility();
            
            if (!isCompatible) {
                // Show warning notification
                String version = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))
                        .getVersion();
                
                Notification notification = new Notification(
                        "ModForge Notifications",
                        "ModForge Plugin Compatibility Warning",
                        "The ModForge plugin (v" + version + ") may not be fully compatible with your current environment. " +
                        "Some features may not work correctly. Please check the logs for details.",
                        NotificationType.WARNING
                );
                
                Notifications.Bus.notify(notification, project);
                
                LOG.warn("ModForge plugin compatibility check failed. Plugin may not function correctly.");
            } else {
                LOG.info("ModForge plugin compatibility check passed.");
                
                // Also check JDK and IDE version with the more comprehensive validator
                EnvironmentValidator.validateEnvironment(project);
            }
        } catch (Exception e) {
            LOG.error("Error checking compatibility: " + e.getMessage(), e);
        }
    }
}