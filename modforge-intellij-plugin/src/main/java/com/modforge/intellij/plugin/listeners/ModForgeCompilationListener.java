package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for compilation events.
 */
public class ModForgeCompilationListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationListener.class);
    private final Project project;
    
    /**
     * Constructor.
     * @param project The project
     */
    public ModForgeCompilationListener(Project project) {
        this.project = project;
        LOG.info("ModForgeCompilationListener created for project: " + project.getName());
    }
    
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        LOG.info("Compilation finished for project: " + project.getName() + 
                 ", aborted: " + aborted + 
                 ", errors: " + errors + 
                 ", warnings: " + warnings);
        
        // Check if we should handle compilation errors
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (!settings.isEnableContinuousDevelopment()) {
            LOG.info("Continuous development is disabled in settings, not handling compilation errors");
            return;
        }
        
        // Show notification about compilation status
        if (settings.isEnableNotifications()) {
            com.modforge.intellij.plugin.services.ModForgeNotificationService notificationService = 
                    com.modforge.intellij.plugin.services.ModForgeNotificationService.getInstance(project);
            
            if (errors > 0) {
                notificationService.showInfoNotification(
                        project,
                        "ModForge Continuous Development",
                        "Detected " + errors + " compilation errors. ModForge AI will attempt to fix them."
                );
            }
        }
        
        // If compilation failed and continuous development is running, try to fix errors
        if (errors > 0) {
            LOG.info("Compilation failed with " + errors + " errors, trying to fix");
            
            ContinuousDevelopmentService continuousDevelopmentService = 
                    project.getService(ContinuousDevelopmentService.class);
            
            if (continuousDevelopmentService != null && continuousDevelopmentService.isRunning()) {
                LOG.info("Triggering error fixing through continuous development service");
                
                // Extract error information from the compile context
                com.intellij.openapi.compiler.CompilerMessage[] errorMessages = compileContext.getMessages(com.intellij.openapi.compiler.CompilerMessageCategory.ERROR);
                
                if (errorMessages != null && errorMessages.length > 0) {
                    LOG.info("Found " + errorMessages.length + " error messages from compiler");
                    
                    // Process errors in batches to avoid overwhelming the system
                    java.util.List<String> errorDescriptions = new java.util.ArrayList<>();
                    for (com.intellij.openapi.compiler.CompilerMessage message : errorMessages) {
                        if (message != null) {
                            String errorText = message.getMessage();
                            String fileName = message.getVirtualFile() != null ? message.getVirtualFile().getName() : "unknown";
                            int line = message.getLine();
                            
                            String errorDescription = String.format("%s (file: %s, line: %d)", 
                                    errorText, fileName, line);
                            errorDescriptions.add(errorDescription);
                            
                            LOG.debug("Error details: " + errorDescription);
                        }
                    }
                    
                    // Delegate error fixing to the continuous development service
                    continuousDevelopmentService.fixCompilationErrors(errorDescriptions);
                } else {
                    LOG.warn("No error messages found in compile context despite errors > 0");
                }
            }
        }
    }
}