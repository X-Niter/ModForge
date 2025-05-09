package com.modforge.intellij.plugin.listeners;

import com.intellij.compiler.CompilerMessageImpl;
import com.intellij.compiler.ProblemsView;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for compilation events.
 */
@Service
public final class ModForgeCompilationListener implements CompilationStatusListener {
    private static final Logger LOG = Logger.getInstance(ModForgeCompilationListener.class);
    
    private final Project project;
    private final Map<String, List<CompilationIssue>> activeIssuesByFile = new ConcurrentHashMap<>();
    
    /**
     * Gets the instance of this listener for the specified project.
     * @param project The project
     * @return The listener instance
     */
    public static ModForgeCompilationListener getInstance(@NotNull Project project) {
        return project.getService(ModForgeCompilationListener.class);
    }
    
    /**
     * Creates a new instance of this listener.
     * @param project The project
     */
    public ModForgeCompilationListener(Project project) {
        this.project = project;
        
        // Register for compilation status events
        project.getMessageBus().connect().subscribe(CompilationStatusListener.TOPIC, this);
        
        LOG.info("ModForge compilation listener initialized");
    }
    
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        LOG.info("Compilation finished: aborted=" + aborted + ", errors=" + errors + ", warnings=" + warnings);
        
        if (errors > 0) {
            processCompilationErrors(compileContext);
        } else {
            // Clear active issues if compilation succeeded
            activeIssuesByFile.clear();
        }
    }
    
    /**
     * Processes compilation errors from the compile context.
     * @param compileContext The compile context
     */
    private void processCompilationErrors(@NotNull CompileContext compileContext) {
        // Clear previous issues
        activeIssuesByFile.clear();
        
        // Get all messages
        CompilerMessage[] messages = compileContext.getMessages(CompilerMessage.Kind.ERROR);
        
        for (CompilerMessage message : messages) {
            String filePath = message.getVirtualFile() != null ? message.getVirtualFile().getPath() : null;
            
            if (filePath == null) {
                continue;
            }
            
            // Normalize path
            String normalizedPath = normalizeFilePath(filePath);
            
            // Create compilation issue
            CompilationIssue issue = new CompilationIssue(
                    normalizedPath,
                    message.getMessage(),
                    message.getLine(),
                    message.getColumn()
            );
            
            // Add to map
            activeIssuesByFile.computeIfAbsent(normalizedPath, k -> new ArrayList<>()).add(issue);
            
            LOG.info("Added compilation issue: " + issue);
        }
    }
    
    /**
     * Normalizes a file path by removing the project base path if present.
     * @param filePath The file path to normalize
     * @return The normalized file path
     */
    private String normalizeFilePath(String filePath) {
        if (filePath == null) {
            return "";
        }
        
        String basePath = project.getBasePath();
        
        if (basePath != null && filePath.startsWith(basePath)) {
            filePath = filePath.substring(basePath.length());
            
            // Remove leading slash
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
        }
        
        return filePath;
    }
    
    /**
     * Gets all active issues for a specific file.
     * @param filePath The file path
     * @return The list of compilation issues for the file
     */
    public List<CompilationIssue> getActiveIssuesForFile(String filePath) {
        if (filePath == null) {
            return Collections.emptyList();
        }
        
        String normalizedPath = normalizeFilePath(filePath);
        
        return activeIssuesByFile.getOrDefault(normalizedPath, Collections.emptyList());
    }
    
    /**
     * Gets all active issues across all files.
     * @return The list of all compilation issues
     */
    public List<CompilationIssue> getAllActiveIssues() {
        List<CompilationIssue> allIssues = new ArrayList<>();
        
        for (List<CompilationIssue> issues : activeIssuesByFile.values()) {
            allIssues.addAll(issues);
        }
        
        return allIssues;
    }
    
    /**
     * A compilation issue.
     */
    public static class CompilationIssue {
        private final String filePath;
        private final String message;
        private final int line;
        private final int column;
        
        /**
         * Creates a new compilation issue.
         * @param filePath The file path
         * @param message The error message
         * @param line The line number (1-based)
         * @param column The column number (1-based)
         */
        public CompilationIssue(String filePath, String message, int line, int column) {
            this.filePath = filePath;
            this.message = message;
            this.line = line;
            this.column = column;
        }
        
        /**
         * Gets the file path.
         * @return The file path
         */
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * Gets the error message.
         * @return The error message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the line number (1-based).
         * @return The line number
         */
        public int getLine() {
            return line;
        }
        
        /**
         * Gets the column number (1-based).
         * @return The column number
         */
        public int getColumn() {
            return column;
        }
        
        @Override
        public String toString() {
            return "CompilationIssue{" +
                    "filePath='" + filePath + '\'' +
                    ", message='" + message + '\'' +
                    ", line=" + line +
                    ", column=" + column +
                    '}';
        }
    }
}