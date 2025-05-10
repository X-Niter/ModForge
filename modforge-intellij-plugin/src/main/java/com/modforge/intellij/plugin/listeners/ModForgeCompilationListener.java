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
        
        // If compilation failed and continuous development is running, try to fix errors
        if (errors > 0) {
            LOG.info("Compilation failed with " + errors + " errors, trying to fix");
            
            ContinuousDevelopmentService continuousDevelopmentService = 
                    project.getService(ContinuousDevelopmentService.class);
            
            if (continuousDevelopmentService != null && continuousDevelopmentService.isRunning()) {
                LOG.info("Triggering error fixing through continuous development service");
                // TODO: Implement error fixing through continuous development service
            }
        }
    }
}