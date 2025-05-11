package com.modforge.intellij.plugin.memory.recovery;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettingsPanel;
import com.modforge.intellij.plugin.memory.ui.MemoryDetailsDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Notifier for memory recovery events
 * Displays notifications for memory pressure and recovery actions
 */
public class MemoryRecoveryNotifier implements MemoryRecoveryManager.RecoveryListener, Disposable {
    private static final Logger LOG = Logger.getInstance(MemoryRecoveryNotifier.class);
    
    private static final String NOTIFICATION_GROUP_ID = "ModForge Memory Management";
    private final Project project;
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryRecoveryNotifier(Project project) {
        this.project = project;
        
        // Register as recovery listener
        MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
        if (recoveryManager != null) {
            recoveryManager.addRecoveryListener(this);
        }
    }
    
    /**
     * Show a memory warning notification
     * 
     * @param pressureLevel The memory pressure level
     */
    public void showMemoryWarning(MemoryUtils.MemoryPressureLevel pressureLevel) {
        if (pressureLevel == null || pressureLevel == MemoryUtils.MemoryPressureLevel.NORMAL) {
            return;
        }
        
        String title;
        String content;
        NotificationType type;
        
        switch (pressureLevel) {
            case WARNING:
                title = "Memory Usage Warning";
                content = "Memory usage is higher than normal. Consider optimizing memory.";
                type = NotificationType.WARNING;
                break;
            case CRITICAL:
                title = "Critical Memory Usage";
                content = "Memory usage is critically high. Optimization recommended.";
                type = NotificationType.WARNING;
                break;
            case EMERGENCY:
                title = "Emergency Memory Situation";
                content = "Memory usage is at emergency levels. Immediate action required.";
                type = NotificationType.ERROR;
                break;
            default:
                return;
        }
        
        Notification notification = new Notification(
                NOTIFICATION_GROUP_ID,
                title,
                content,
                type
        );
        
        // Add actions to the notification
        notification.addAction(NotificationAction.createSimple("Optimize Memory", () -> 
                optimizeMemory(pressureLevel)));
        
        notification.addAction(NotificationAction.createSimple("Show Details", () -> 
                MemoryDetailsDialog.show(project)));
        
        notification.addAction(NotificationAction.create("Memory Settings", (e, n) -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(
                    project, 
                    MemoryManagementSettingsPanel.class);
            n.expire();
            return null;
        }));
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Optimize memory based on the pressure level
     * 
     * @param pressureLevel The pressure level
     */
    private void optimizeMemory(MemoryUtils.MemoryPressureLevel pressureLevel) {
        if (project == null || project.isDisposed()) {
            return;
        }
        
        MemoryOptimizer optimizer = project.getService(MemoryOptimizer.class);
        if (optimizer == null) {
            return;
        }
        
        MemoryOptimizer.OptimizationLevel level;
        
        switch (pressureLevel) {
            case EMERGENCY:
                level = MemoryOptimizer.OptimizationLevel.AGGRESSIVE;
                break;
            case CRITICAL:
                level = MemoryOptimizer.OptimizationLevel.NORMAL;
                break;
            default:
                level = MemoryOptimizer.OptimizationLevel.CONSERVATIVE;
                break;
        }
        
        LOG.info("Optimizing memory at level " + level + " due to pressure level " + pressureLevel);
        optimizer.optimize(level);
    }
    
    @Override
    public void onRecoveryStarted(MemoryRecoveryManager.RecoveryPriority priority) {
        String title = "Memory Recovery Started";
        String content = "Memory recovery at priority " + priority + " has started.";
        NotificationType type = NotificationType.INFORMATION;
        
        if (priority == MemoryRecoveryManager.RecoveryPriority.CRITICAL) {
            title = "Aggressive Memory Recovery";
            content = "Aggressive memory recovery actions are being performed. This may temporarily affect performance.";
            type = NotificationType.WARNING;
        }
        
        Notification notification = new Notification(
                NOTIFICATION_GROUP_ID,
                title,
                content,
                type
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    @Override
    public void onRecoveryCompleted(MemoryRecoveryManager.RecoveryPriority priority) {
        String title = "Memory Recovery Completed";
        String content = "Memory recovery at priority " + priority + " has completed successfully.";
        
        Notification notification = new Notification(
                NOTIFICATION_GROUP_ID,
                title,
                content,
                NotificationType.INFORMATION
        );
        
        notification.addAction(NotificationAction.createSimple("Show Details", () -> 
                MemoryDetailsDialog.show(project)));
        
        Notifications.Bus.notify(notification, project);
    }
    
    @Override
    public void onRecoveryFailed(MemoryRecoveryManager.RecoveryPriority priority, Exception error) {
        String title = "Memory Recovery Failed";
        String content = "Memory recovery at priority " + priority + " has failed: " + error.getMessage();
        
        Notification notification = new Notification(
                NOTIFICATION_GROUP_ID,
                title,
                content,
                NotificationType.ERROR
        );
        
        notification.addAction(NotificationAction.createSimple("Show Details", () -> 
                MemoryDetailsDialog.show(project)));
        
        // Add action to manually trigger memory optimization
        notification.addAction(NotificationAction.create("Manual Optimization", (e, n) -> {
            optimizeMemory(MemoryUtils.MemoryPressureLevel.CRITICAL);
            n.expire();
            return null;
        }));
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Dispose the notifier
     * Unregister itself as a recovery listener
     */
    @Override
    public void dispose() {
        LOG.debug("Disposing memory recovery notifier");
        
        // Unregister from recovery manager
        MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
        if (recoveryManager != null) {
            recoveryManager.removeRecoveryListener(this);
        }
    }
}