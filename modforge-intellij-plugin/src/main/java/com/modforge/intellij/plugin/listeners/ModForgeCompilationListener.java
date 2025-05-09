package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener for compilation events.
 * Monitors compilation errors and triggers automatic fixes if enabled.
 */
public class ModForgeCompilationListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationListener.class);
    private final Project project;
    private AtomicInteger autoFixCount = new AtomicInteger(0);
    
    /**
     * Creates a new ModForgeCompilationListener.
     * @param project The project
     */
    public ModForgeCompilationListener(Project project) {
        this.project = project;
        
        // Register with compiler manager
        CompilerManager.getInstance(project).addCompilationStatusListener(this);
        
        LOG.info("ModForge compilation listener registered for project: " + project.getName());
    }
    
    /**
     * Disposes the listener.
     */
    public void dispose() {
        // Unregister from compiler manager
        CompilerManager.getInstance(project).removeCompilationStatusListener(this);
        
        LOG.info("ModForge compilation listener unregistered for project: " + project.getName());
    }

    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
        LOG.info("Compilation finished with " + errors + " errors and " + warnings + " warnings");
        
        // Check if automatic error fixing is enabled
        if (!ModForgeSettings.getInstance().isAutoFixCompilationErrors()) {
            LOG.info("Automatic error fixing is disabled");
            return;
        }
        
        // Check if there are errors
        if (errors == 0) {
            LOG.info("No errors to fix");
            
            // Reset auto fix count on successful compilation
            autoFixCount.set(0);
            return;
        }
        
        // Get the compile scope
        CompileScope scope = compileContext.getCompileScope();
        
        // Get affected files
        VirtualFile[] files = scope.getFiles(null, true);
        
        if (files.length == 0) {
            LOG.info("No files in compile scope");
            return;
        }
        
        // Check if we've exceeded the maximum auto fix count
        if (autoFixCount.get() >= ModForgeSettings.getInstance().getMaxAutoFixAttempts()) {
            LOG.info("Maximum auto fix attempts exceeded");
            return;
        }
        
        // Collect error messages by file
        Map<VirtualFile, List<String>> fileErrors = new HashMap<>();
        
        for (CompilerMessage message : compileContext.getMessages(CompilerMessage.Kind.ERROR)) {
            VirtualFile file = message.getVirtualFile();
            
            if (file == null) {
                continue;
            }
            
            // Add file to map if it doesn't exist
            if (!fileErrors.containsKey(file)) {
                fileErrors.put(file, new ArrayList<>());
            }
            
            // Add error message
            fileErrors.get(file).add(message.getMessage());
        }
        
        // Fix errors
        if (!fileErrors.isEmpty()) {
            // Increment auto fix count
            autoFixCount.incrementAndGet();
            
            LOG.info("Attempting to fix " + fileErrors.size() + " files with errors (attempt " + autoFixCount.get() + ")");
            
            // Get autonomous code generation service
            AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
            
            // Fix each file
            for (Map.Entry<VirtualFile, List<String>> entry : fileErrors.entrySet()) {
                VirtualFile file = entry.getKey();
                List<String> errorMessages = entry.getValue();
                
                LOG.info("Fixing " + errorMessages.size() + " errors in " + file.getName());
                
                // Fix errors in file
                codeGenService.fixErrorsInFile(file, errorMessages)
                        .thenAccept(fixed -> {
                            if (fixed) {
                                LOG.info("Fixed errors in " + file.getName());
                                
                                // Recompile file
                                CompilerManager.getInstance(project).compile(new VirtualFile[]{file}, null);
                            } else {
                                LOG.info("Failed to fix errors in " + file.getName());
                            }
                        })
                        .exceptionally(ex -> {
                            LOG.error("Error fixing errors in " + file.getName(), ex);
                            return null;
                        });
            }
        }
    }

    @Override
    public void fileGenerated(String outputRoot, String relativePath) {
        // Not used, but required by interface
    }
}