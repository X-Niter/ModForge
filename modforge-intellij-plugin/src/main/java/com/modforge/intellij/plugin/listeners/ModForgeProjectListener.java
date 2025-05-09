package com.modforge.intellij.plugin.listeners;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.IDEIntegrationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Project listener for autonomously monitoring and modifying code.
 * This listener hooks into project lifecycle events to detect when
 * the project is ready for analysis and modification.
 */
public class ModForgeProjectListener implements ProjectManagerListener, StartupActivity {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectListener.class);
    
    private static final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("ModForge project activity started for project: " + project.getName());
        
        // Welcome notification
        if (ModForgeSettings.getInstance().isEnableNotifications()) {
            showWelcomeNotification(project);
        }
        
        // Initialize project services
        project.getService(AIServiceManager.class);
        project.getService(AutonomousCodeGenerationService.class);
        project.getService(IDEIntegrationService.class);
        
        // Schedule autonomous code analysis
        scheduleAutonomousAnalysis(project);
    }
    
    @Override
    public void projectOpened(@NotNull Project project) {
        LOG.info("Project opened: " + project.getName());
    }
    
    @Override
    public void projectClosed(@NotNull Project project) {
        LOG.info("Project closed: " + project.getName());
        
        // Cancel scheduled tasks
        for (ScheduledFuture<?> task : scheduledTasks) {
            if (!task.isCancelled() && !task.isDone()) {
                task.cancel(true);
            }
        }
        scheduledTasks.clear();
    }
    
    /**
     * Shows a welcome notification.
     * @param project The project
     */
    private void showWelcomeNotification(@NotNull Project project) {
        Notification notification = new Notification(
                "ModForge Notifications",
                "ModForge Initialized",
                "ModForge is ready to assist with your Minecraft mod development.",
                NotificationType.INFORMATION
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Schedules autonomous code analysis.
     * @param project The project
     */
    private void scheduleAutonomousAnalysis(@NotNull Project project) {
        // Schedule periodic code analysis (every 5 minutes)
        ScheduledFuture<?> task = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                () -> analyzeProject(project),
                5, 5, TimeUnit.MINUTES
        );
        
        scheduledTasks.add(task);
    }
    
    /**
     * Analyzes the project for possible improvements.
     * @param project The project
     */
    private void analyzeProject(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        
        LOG.info("Running autonomous analysis for project: " + project.getName());
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Analyzing Project", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("ModForge: Analyzing project structure");
                
                try {
                    // Get project structure
                    IDEIntegrationService ideService = project.getService(IDEIntegrationService.class);
                    indicator.setFraction(0.2);
                    
                    if (indicator.isCanceled()) {
                        return;
                    }
                    
                    // Get project files
                    List<VirtualFile> sourceFiles = ideService.getSourceFiles();
                    indicator.setFraction(0.4);
                    
                    if (indicator.isCanceled()) {
                        return;
                    }
                    
                    // Check for pattern existence
                    int total = sourceFiles.size();
                    int count = 0;
                    
                    // Get code analysis service
                    AutonomousCodeGenerationService codeGenService = 
                            project.getService(AutonomousCodeGenerationService.class);
                    
                    // Analyze Java files
                    for (VirtualFile file : sourceFiles) {
                        if (indicator.isCanceled()) {
                            return;
                        }
                        
                        if (isJavaFile(file)) {
                            indicator.setText2("Analyzing: " + file.getName());
                            
                            // Analyze file for potential improvements
                            try {
                                codeGenService.analyzeFile(file)
                                        .thenAccept(issues -> {
                                            if (!issues.isEmpty() && ModForgeSettings.getInstance().isEnableNotifications()) {
                                                notifyIssuesFound(project, file, issues.size());
                                            }
                                        });
                            } catch (Exception e) {
                                LOG.error("Error analyzing file: " + file.getPath(), e);
                            }
                        }
                        
                        count++;
                        indicator.setFraction(0.4 + (0.6 * count / total));
                    }
                    
                    LOG.info("Autonomous analysis completed for project: " + project.getName());
                } catch (Exception e) {
                    LOG.error("Error during autonomous analysis", e);
                }
            }
        });
    }
    
    /**
     * Notifies the user of issues found.
     * @param project The project
     * @param file The file
     * @param issueCount The number of issues found
     */
    private void notifyIssuesFound(@NotNull Project project, @NotNull VirtualFile file, int issueCount) {
        Notification notification = new Notification(
                "ModForge Notifications",
                "Code Issues Found",
                "ModForge found " + issueCount + " possible improvements in " + file.getName() + ".",
                NotificationType.INFORMATION
        );
        
        notification.addAction(new com.intellij.notification.NotificationAction("Fix Issues") {
            @Override
            public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, 
                                       @NotNull Notification notification) {
                AutonomousCodeGenerationService codeGenService = 
                        project.getService(AutonomousCodeGenerationService.class);
                
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Issues", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        indicator.setText("ModForge: Fixing issues in " + file.getName());
                        
                        try {
                            codeGenService.analyzeFile(file)
                                    .thenCompose(issues -> codeGenService.fixIssues(file, issues))
                                    .thenAccept(fixedCount -> {
                                        if (fixedCount > 0) {
                                            Notification success = new Notification(
                                                    "ModForge Notifications",
                                                    "Issues Fixed",
                                                    "ModForge successfully fixed " + fixedCount + " issues in " + 
                                                            file.getName() + ".",
                                                    NotificationType.INFORMATION
                                            );
                                            
                                            Notifications.Bus.notify(success, project);
                                        }
                                    });
                        } catch (Exception ex) {
                            LOG.error("Error fixing issues", ex);
                        }
                    }
                });
                
                notification.expire();
            }
        });
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Checks if a file is a Java file.
     * @param file The file to check
     * @return Whether the file is a Java file
     */
    private boolean isJavaFile(@NotNull VirtualFile file) {
        return !file.isDirectory() && "java".equals(file.getExtension());
    }
}