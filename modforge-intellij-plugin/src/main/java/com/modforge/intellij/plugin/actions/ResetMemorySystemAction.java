package com.modforge.intellij.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.modforge.intellij.plugin.memory.MemoryManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action that resets the entire memory management system
 * This provides a way for users to manually reset memory components when needed
 */
public class ResetMemorySystemAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ResetMemorySystemAction.class);
    private static final javax.swing.Icon ICON = IconLoader.getIcon("/icons/reset_memory.svg", ResetMemorySystemAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        LOG.info("Manual memory system reset requested");
        
        // Perform the reset in a background task with a progress indicator
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Resetting Memory Management System", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Resetting memory management components...");
                
                try {
                    // Get the memory manager and reset the system
                    MemoryManager memoryManager = MemoryManager.getInstance();
                    if (memoryManager != null) {
                        memoryManager.resetMemorySystem();
                        
                        // Show success notification
                        Notification notification = new Notification(
                                "ModForge",
                                "Memory System Reset",
                                "Memory management system has been successfully reset",
                                NotificationType.INFORMATION
                        );
                        Notifications.Bus.notify(notification, project);
                        
                        LOG.info("Manual memory system reset completed successfully");
                    } else {
                        // Show error notification
                        Notification notification = new Notification(
                                "ModForge",
                                "Memory System Reset",
                                "Failed to reset memory management system: Memory manager is not available",
                                NotificationType.ERROR
                        );
                        Notifications.Bus.notify(notification, project);
                        
                        LOG.error("Manual memory system reset failed: Memory manager is not available");
                    }
                } catch (Exception ex) {
                    // Log error and show notification
                    LOG.error("Error during manual memory system reset", ex);
                    
                    Notification notification = new Notification(
                            "ModForge",
                            "Memory System Reset",
                            "Failed to reset memory management system: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    Notifications.Bus.notify(notification, project);
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only if we have a valid project
        Project project = e.getProject();
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(project != null && !project.isDisposed());
        
        // Set icon
        presentation.setIcon(ICON);
    }
}