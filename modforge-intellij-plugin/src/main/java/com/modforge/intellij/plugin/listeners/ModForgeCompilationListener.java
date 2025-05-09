package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for compilation events.
 * This listener is used to detect and fix compilation errors automatically.
 */
public class ModForgeCompilationListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationListener.class);
    
    private final Project project;
    private final AutonomousCodeGenerationService codeGenService;
    private final ContinuousDevelopmentService continuousDevService;
    
    /**
     * Creates a new ModForgeCompilationListener.
     * @param project The project
     */
    public ModForgeCompilationListener(@NotNull Project project) {
        this.project = project;
        this.codeGenService = AutonomousCodeGenerationService.getInstance(project);
        this.continuousDevService = ContinuousDevelopmentService.getInstance(project);
        
        LOG.info("Compilation listener created for project: " + project.getName());
    }
    
    /**
     * Registers the listener.
     */
    public void register() {
        CompilerManager.getInstance(project).addCompilationStatusListener(this);
        LOG.info("Compilation listener registered for project: " + project.getName());
    }
    
    /**
     * Unregisters the listener.
     */
    public void unregister() {
        CompilerManager.getInstance(project).removeCompilationStatusListener(this);
        LOG.info("Compilation listener unregistered for project: " + project.getName());
    }
    
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        // Check if we should handle errors
        if (aborted || errors == 0) {
            return;
        }
        
        LOG.info("Compilation finished with errors: " + errors);
        
        // Check if continuous development is enabled
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (!settings.isContinuousDevelopmentEnabled()) {
            LOG.info("Continuous development is disabled, not auto-fixing errors");
            return;
        }
        
        // Get error messages
        CompilerMessage[] errorMessages = compileContext.getMessages(CompilerMessageCategory.ERROR);
        
        // Process errors if not in continuous development mode
        // (In continuous mode, errors are handled by the continuous development service)
        if (!continuousDevService.isRunning()) {
            processCompilationErrors(errorMessages);
        }
    }
    
    /**
     * Processes compilation errors.
     * @param errorMessages The error messages
     */
    private void processCompilationErrors(CompilerMessage[] errorMessages) {
        if (errorMessages == null || errorMessages.length == 0) {
            return;
        }
        
        LOG.info("Processing " + errorMessages.length + " compilation errors");
        
        // Group errors by file
        for (CompilerMessage message : errorMessages) {
            VirtualFile file = message.getVirtualFile();
            
            if (file == null) {
                continue;
            }
            
            LOG.info("Error in file: " + file.getPath() + ", message: " + message.getMessage());
        }
    }
    
    /**
     * Converts compiler messages to code issues.
     * @param messages The compiler messages
     * @return The code issues
     */
    private List<AutonomousCodeGenerationService.CodeIssue> convertToCodeIssues(CompilerMessage[] messages) {
        List<AutonomousCodeGenerationService.CodeIssue> issues = new ArrayList<>();
        
        for (CompilerMessage message : messages) {
            VirtualFile file = message.getVirtualFile();
            
            if (file == null) {
                continue;
            }
            
            issues.add(new AutonomousCodeGenerationService.CodeIssue(
                    message.getMessage(),
                    message.getLine(),
                    message.getColumn(),
                    file.getPath(),
                    null // We don't have the code snippet here
            ));
        }
        
        return issues;
    }
}