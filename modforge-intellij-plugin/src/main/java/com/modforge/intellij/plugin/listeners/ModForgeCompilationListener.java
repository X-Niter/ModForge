package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.compiler.*;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listener for compilation events in the project.
 * This listener is responsible for tracking compilation errors.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeCompilationListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationListener.class);
    
    private final Project project;
    private final List<CompilationIssue> activeIssues = new CopyOnWriteArrayList<>();
    
    /**
     * Represents a compilation issue.
     */
    public static class CompilationIssue {
        private final String message;
        private final int line;
        private final int column;
        private final String file;
        
        /**
         * Creates a new CompilationIssue.
         * @param message The error message
         * @param line The line number
         * @param column The column number
         * @param file The file path
         */
        public CompilationIssue(String message, int line, int column, String file) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.file = file;
        }
        
        /**
         * Gets the error message.
         * @return The error message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the line number.
         * @return The line number
         */
        public int getLine() {
            return line;
        }
        
        /**
         * Gets the column number.
         * @return The column number
         */
        public int getColumn() {
            return column;
        }
        
        /**
         * Gets the file path.
         * @return The file path
         */
        public String getFile() {
            return file;
        }
    }
    
    /**
     * Creates a new ModForgeCompilationListener.
     * @param project The project
     */
    public ModForgeCompilationListener(Project project) {
        this.project = project;
        
        // Register listener
        CompilerManager.getInstance(project).addCompilationStatusListener(this);
        
        LOG.info("Compilation listener registered for project: " + project.getName());
    }
    
    /**
     * Gets active compilation issues.
     * @return Active compilation issues
     */
    @NotNull
    public List<CompilationIssue> getActiveIssues() {
        return new ArrayList<>(activeIssues);
    }
    
    /**
     * Called when compilation is complete.
     * @param aborted Whether compilation was aborted
     * @param errors Number of errors
     * @param warnings Number of warnings
     * @param compileContext The compile context
     */
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        LOG.info("Compilation finished with " + errors + " errors and " + warnings + " warnings");
        
        // If compilation was aborted or there are no errors, don't process
        if (aborted || errors == 0) {
            // Clear active issues
            activeIssues.clear();
            return;
        }
        
        // Process compilation errors
        processCompilationErrors(compileContext);
    }
    
    /**
     * Processes compilation errors from the compile context.
     * @param compileContext The compile context
     */
    private void processCompilationErrors(@NotNull CompileContext compileContext) {
        // Clear active issues
        activeIssues.clear();
        
        // Get compilation messages
        CompilerMessage[] messages = compileContext.getMessages(CompilerMessageCategory.ERROR);
        
        // Process messages
        for (CompilerMessage message : messages) {
            // Skip messages without a file
            if (message.getVirtualFile() == null) {
                continue;
            }
            
            // Get file path
            String filePath = message.getVirtualFile().getPath();
            
            // Get project base path
            String basePath = project.getBasePath();
            
            // Convert to relative path
            if (basePath != null && filePath.startsWith(basePath)) {
                filePath = filePath.substring(basePath.length());
                
                // Remove leading slash
                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                }
            }
            
            // Create issue
            CompilationIssue issue = new CompilationIssue(
                    message.getMessage(),
                    message.getLine(),
                    message.getColumn(),
                    filePath
            );
            
            // Add to active issues
            activeIssues.add(issue);
            
            LOG.info("Added compilation issue: " + issue.getMessage() + " at " + issue.getFile() + ":" + issue.getLine());
        }
    }
    
    /**
     * Called before compilation.
     * @param isRebuild Whether compilation is a rebuild
     * @param compileContext The compile context
     */
    @Override
    public void compilationStarted(boolean isRebuild, @NotNull CompileContext compileContext) {
        LOG.info("Compilation started" + (isRebuild ? " (rebuild)" : ""));
    }
    
    /**
     * Called when compilation is complete.
     * @param isRebuild Whether compilation is a rebuild
     * @param isCompilable Whether compilation can be performed
     * @param isAutomaticCompilation Whether compilation is automatic
     * @param compileContext The compile context
     */
    @Override
    public void buildFinished(@NotNull CompileContext compileContext) {
        LOG.info("Build finished");
    }
    
    /**
     * Called when automatic compilation is performed.
     * @param isRebuild Whether compilation is a rebuild
     * @param compileContext The compile context
     */
    @Override
    public void automakeCompilationFinished(int errors, int warnings, @NotNull CompileContext compileContext) {
        LOG.info("Automake compilation finished with " + errors + " errors and " + warnings + " warnings");
        
        // Process compilation errors if there are any
        if (errors > 0) {
            processCompilationErrors(compileContext);
        } else {
            // Clear active issues
            activeIssues.clear();
        }
    }
    
    /**
     * Called when the file status has changed.
     * @param isNotChanged Whether the file is unchanged
     * @param compileContext The compile context
     */
    @Override
    public void fileGenerated(@NotNull String outputRoot, @NotNull String relativePath) {
        LOG.info("File generated: " + outputRoot + "/" + relativePath);
    }
    
    /**
     * Disposes the listener.
     */
    public void dispose() {
        // Unregister listener
        CompilerManager.getInstance(project).removeCompilationStatusListener(this);
        
        LOG.info("Compilation listener unregistered for project: " + project.getName());
        
        // Clear active issues
        activeIssues.clear();
    }
}