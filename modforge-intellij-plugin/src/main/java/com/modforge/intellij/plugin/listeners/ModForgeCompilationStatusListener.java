package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Listener for compilation events to detect and fix errors automatically.
 */
public class ModForgeCompilationStatusListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationStatusListener.class);
    
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        if (aborted || errors == 0) {
            return;
        }
        
        Project project = compileContext.getProject();
        if (project.isDisposed()) {
            return;
        }
        
        LOG.info("Compilation finished with " + errors + " errors and " + warnings + " warnings");
        
        if (!ModForgeSettings.getInstance().isEnableNotifications()) {
            return;
        }
        
        // Collect errors
        CompilerMessage[] allMessages = compileContext.getMessages(CompilerMessageCategory.ERROR);
        if (allMessages.length == 0) {
            return;
        }
        
        // Group errors by file
        List<ErrorGroup> errorGroups = groupErrorsByFile(allMessages);
        
        // Notify user
        for (ErrorGroup group : errorGroups) {
            if (group.getFile() != null && group.getErrors().size() > 0) {
                notifyCompilationErrors(project, group);
            }
        }
    }
    
    /**
     * Groups errors by file.
     * @param messages The compiler messages
     * @return The error groups
     */
    private List<ErrorGroup> groupErrorsByFile(CompilerMessage @NotNull [] messages) {
        List<ErrorGroup> groups = new ArrayList<>();
        
        for (CompilerMessage message : messages) {
            if (message.getVirtualFile() == null) {
                continue;
            }
            
            // Find existing group or create new one
            ErrorGroup group = groups.stream()
                    .filter(g -> g.getFile() != null && 
                            g.getFile().getPath().equals(message.getVirtualFile().getPath()))
                    .findFirst()
                    .orElse(null);
            
            if (group == null) {
                group = new ErrorGroup(message.getVirtualFile());
                groups.add(group);
            }
            
            group.addError(message);
        }
        
        return groups;
    }
    
    /**
     * Notifies the user of compilation errors.
     * @param project The project
     * @param errorGroup The error group
     */
    private void notifyCompilationErrors(@NotNull Project project, @NotNull ErrorGroup errorGroup) {
        VirtualFile file = errorGroup.getFile();
        int errorCount = errorGroup.getErrors().size();
        
        Notification notification = new Notification(
                "ModForge Notifications",
                "Compilation Errors",
                "ModForge detected " + errorCount + " compilation errors in " + file.getName() + ".",
                NotificationType.WARNING
        );
        
        // Add action to fix errors automatically
        notification.addAction(new AnAction("Fix Automatically") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                fixCompilationErrors(project, errorGroup);
                notification.expire();
            }
        });
        
        // Add action to open file
        notification.addAction(new AnAction("Open File") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                FileEditorManager.getInstance(project).openFile(file, true);
                notification.expire();
            }
        });
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Fixes compilation errors.
     * @param project The project
     * @param errorGroup The error group
     */
    private void fixCompilationErrors(@NotNull Project project, @NotNull ErrorGroup errorGroup) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fixing Compilation Errors", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("ModForge: Analyzing errors in " + errorGroup.getFile().getName());
                
                try {
                    // Build error message for AI
                    StringBuilder errorBuilder = new StringBuilder();
                    for (CompilerMessage error : errorGroup.getErrors()) {
                        errorBuilder.append(error.getMessage()).append("\n");
                        if (error.getLine() > 0) {
                            errorBuilder.append("at line ").append(error.getLine()).append("\n");
                        }
                    }
                    
                    String errorMessage = errorBuilder.toString();
                    
                    // Get autonomous code generation service
                    AutonomousCodeGenerationService codeGenService = 
                            project.getService(AutonomousCodeGenerationService.class);
                    
                    // Fix errors
                    CompletableFuture<Boolean> fixFuture = codeGenService.fixCompilerError(
                            errorGroup.getFile(), errorMessage);
                    
                    // Wait for fix to complete
                    Boolean fixed = fixFuture.get(30, TimeUnit.SECONDS);
                    
                    if (fixed) {
                        indicator.setText("ModForge: Errors fixed, recompiling...");
                        
                        // Give the system a moment to process the changes
                        Thread.sleep(500);
                        
                        // Trigger recompilation
                        AppExecutorUtil.getAppExecutorService().execute(() -> {
                            com.intellij.openapi.compiler.CompilerManager.getInstance(project)
                                    .make(null);
                        });
                        
                        // Notify user
                        Notification success = new Notification(
                                "ModForge Notifications",
                                "Errors Fixed",
                                "ModForge fixed compilation errors in " + errorGroup.getFile().getName() + 
                                        ". Recompiling...",
                                NotificationType.INFORMATION
                        );
                        
                        Notifications.Bus.notify(success, project);
                    } else {
                        // Notify user
                        Notification failure = new Notification(
                                "ModForge Notifications",
                                "Error Fix Failed",
                                "ModForge could not automatically fix all errors in " + 
                                        errorGroup.getFile().getName() + ".",
                                NotificationType.WARNING
                        );
                        
                        Notifications.Bus.notify(failure, project);
                    }
                } catch (Exception e) {
                    LOG.error("Error fixing compilation errors", e);
                    
                    // Notify user
                    Notification error = new Notification(
                            "ModForge Notifications",
                            "Error Fix Failed",
                            "ModForge encountered an error while trying to fix compilation errors: " + 
                                    e.getMessage(),
                            NotificationType.ERROR
                    );
                    
                    Notifications.Bus.notify(error, project);
                }
            }
        });
    }
    
    /**
     * Error group.
     */
    private static class ErrorGroup {
        private final VirtualFile file;
        private final List<CompilerMessage> errors = new ArrayList<>();
        
        /**
         * Creates a new ErrorGroup.
         * @param file The file
         */
        public ErrorGroup(VirtualFile file) {
            this.file = file;
        }
        
        /**
         * Gets the file.
         * @return The file
         */
        public VirtualFile getFile() {
            return file;
        }
        
        /**
         * Gets the errors.
         * @return The errors
         */
        public List<CompilerMessage> getErrors() {
            return errors;
        }
        
        /**
         * Adds an error.
         * @param error The error
         */
        public void addError(CompilerMessage error) {
            errors.add(error);
        }
    }
}