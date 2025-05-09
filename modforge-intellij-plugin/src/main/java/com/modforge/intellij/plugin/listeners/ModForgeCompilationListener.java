package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listener for compilation events.
 * This listener tracks compilation issues in the project.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeCompilationListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationListener.class);
    private static final int MAX_ISSUES = 100;
    
    private final Project project;
    private final List<CompilationIssue> activeIssues = new CopyOnWriteArrayList<>();
    private boolean isRegistered = false;
    
    /**
     * Creates a new ModForgeCompilationListener.
     * @param project The project
     */
    public ModForgeCompilationListener(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Registers this listener with the compiler manager.
     */
    public void register() {
        if (!isRegistered) {
            CompilerManager.getInstance(project).addCompilationStatusListener(this);
            isRegistered = true;
            LOG.info("Compilation listener registered for project: " + project.getName());
        }
    }
    
    /**
     * Unregisters this listener from the compiler manager.
     */
    public void unregister() {
        if (isRegistered) {
            CompilerManager.getInstance(project).removeCompilationStatusListener(this);
            isRegistered = false;
            LOG.info("Compilation listener unregistered for project: " + project.getName());
        }
    }
    
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        // Clear active issues
        activeIssues.clear();
        
        // Process error messages
        CompilerMessage[] errorMessages = compileContext.getMessages(CompilerMessageCategory.ERROR);
        
        if (errorMessages.length > 0) {
            LOG.info("Compilation finished with " + errorMessages.length + " errors");
            
            for (int i = 0; i < Math.min(errorMessages.length, MAX_ISSUES); i++) {
                CompilerMessage message = errorMessages[i];
                
                // Create issue
                CompilationIssue issue = createIssueFromMessage(message);
                
                if (issue != null) {
                    activeIssues.add(issue);
                }
            }
        } else {
            LOG.info("Compilation finished successfully");
        }
    }
    
    /**
     * Gets the active compilation issues.
     * @return The active compilation issues
     */
    @NotNull
    public List<CompilationIssue> getActiveIssues() {
        return Collections.unmodifiableList(new ArrayList<>(activeIssues));
    }
    
    /**
     * Gets the active compilation issues for a file.
     * @param file The file path
     * @return The active compilation issues for the file
     */
    @NotNull
    public List<CompilationIssue> getActiveIssuesForFile(@NotNull String file) {
        List<CompilationIssue> issues = new ArrayList<>();
        
        for (CompilationIssue issue : activeIssues) {
            if (file.equals(issue.getFile())) {
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    /**
     * Clears all active issues.
     */
    public void clearActiveIssues() {
        activeIssues.clear();
    }
    
    /**
     * Creates a compilation issue from a compiler message.
     * @param message The compiler message
     * @return The compilation issue
     */
    @Nullable
    private CompilationIssue createIssueFromMessage(@NotNull CompilerMessage message) {
        // Get file path
        String filePath = null;
        
        if (message.getVirtualFile() != null) {
            filePath = message.getVirtualFile().getPath();
            
            // Make path relative to project
            String basePath = project.getBasePath();
            
            if (basePath != null && filePath.startsWith(basePath)) {
                filePath = filePath.substring(basePath.length());
                
                // Remove leading slash
                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                }
            }
        }
        
        // Skip issues without a file
        if (filePath == null) {
            return null;
        }
        
        // Create issue
        return new CompilationIssue(
                message.getMessage(),
                message.getLine(),
                message.getColumn(),
                filePath
        );
    }
    
    /**
     * A compilation issue.
     */
    public static final class CompilationIssue {
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
        public CompilationIssue(@NotNull String message, int line, int column, @NotNull String file) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.file = file;
        }
        
        /**
         * Gets the error message.
         * @return The error message
         */
        @NotNull
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
        @NotNull
        public String getFile() {
            return file;
        }
        
        @Override
        public String toString() {
            return message + " at " + file + ":" + line + ":" + column;
        }
    }
}