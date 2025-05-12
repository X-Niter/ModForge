package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.AutonomousIdeCoordinatorService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;

/**
 * Action for optimizing code in the current project.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class OptimizeCodeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get coordinator service
        AutonomousIdeCoordinatorService coordinatorService = 
                AutonomousIdeCoordinatorService.getInstance(project);
        
        // Run optimization in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Optimizing Code") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                
                try {
                    // Set progress text
                    indicator.setText("Analyzing project...");
                    indicator.setFraction(0.1);
                    
                    // Start optimization
                    AutonomousIdeCoordinatorService.OptimizationSummary summary = 
                            coordinatorService.optimizeProject().get();
                    
                    // Update progress
                    indicator.setText("Applying fixes...");
                    indicator.setFraction(0.6);
                    
                    // Show result dialog using compatible notification service
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ModForgeNotificationService.getInstance(project).showInfoNotification(
                                project,
                                "Code Optimization Complete",
                                String.format(
                                        "Optimization completed successfully!\n\n" +
                                        "Found %d issues in %d files (out of %d total files)\n" +
                                        "Fixed %d issues",
                                        summary.getIssuesFound(),
                                        summary.getFilesWithIssues(),
                                        summary.getTotalFiles(),
                                        summary.getIssuesFixed()
                                )
                        );
                    });
                } catch (Exception ex) {
                    // Show error dialog using compatible notification service
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ModForgeNotificationService.getInstance(project).showErrorNotification(
                                project,
                                "Error",
                                "Error optimizing code: " + ex.getMessage()
                        );
                    });
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only if a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}