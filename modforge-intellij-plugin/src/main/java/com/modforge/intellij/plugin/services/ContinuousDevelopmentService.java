package com.modforge.intellij.plugin.services;

import com.intellij.compiler.CompilerMessageImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service for continuous development of Minecraft mods.
 * This service automatically compiles, finds errors, and fixes them using AI.
 */
@Service(Service.Level.PROJECT)
public final class ContinuousDevelopmentService {
    private static final Logger LOG = Logger.getInstance(ContinuousDevelopmentService.class);
    
    private final Project project;
    private final AutonomousCodeGenerationService codeGenService;
    private final ScheduledExecutorService scheduler;
    
    private ScheduledFuture<?> scheduledTask;
    private final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> fixAttempts = new ConcurrentHashMap<>();
    
    private static final int MAX_FIX_ATTEMPTS = 3;
    private static final int DEFAULT_INTERVAL_MINUTES = 5;
    
    /**
     * Creates a new ContinuousDevelopmentService.
     * @param project The project
     */
    public ContinuousDevelopmentService(Project project) {
        this.project = project;
        this.codeGenService = AutonomousCodeGenerationService.getInstance(project);
        this.scheduler = AppExecutorUtil.getAppScheduledExecutorService();
        
        LOG.info("Continuous development service created for project: " + project.getName());
    }
    
    /**
     * Gets the continuous development service for a project.
     * @param project The project
     * @return The continuous development service
     */
    public static ContinuousDevelopmentService getInstance(@NotNull Project project) {
        return project.getService(ContinuousDevelopmentService.class);
    }
    
    /**
     * Starts continuous development.
     */
    public void start() {
        if (isRunning()) {
            LOG.info("Continuous development is already running");
            return;
        }
        
        LOG.info("Starting continuous development");
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isContinuousDevelopmentEnabled()) {
            LOG.info("Continuous development is disabled in settings");
            return;
        }
        
        // Get interval
        int intervalMinutes = settings.getContinuousDevelopmentIntervalMinutes();
        
        if (intervalMinutes <= 0) {
            intervalMinutes = DEFAULT_INTERVAL_MINUTES;
        }
        
        // Schedule task
        scheduledTask = scheduler.scheduleAtFixedRate(
                this::processDevelopmentCycle,
                0,
                intervalMinutes,
                TimeUnit.MINUTES
        );
        
        LOG.info("Continuous development started with interval: " + intervalMinutes + " minutes");
    }
    
    /**
     * Stops continuous development.
     */
    public void stop() {
        if (!isRunning()) {
            LOG.info("Continuous development is not running");
            return;
        }
        
        LOG.info("Stopping continuous development");
        
        // Cancel task
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        
        LOG.info("Continuous development stopped");
    }
    
    /**
     * Checks if continuous development is running.
     * @return True if running, false otherwise
     */
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isDone() && !scheduledTask.isCancelled();
    }
    
    /**
     * Resets error counts and fix attempts.
     */
    public void reset() {
        LOG.info("Resetting error counts and fix attempts");
        errorCounts.clear();
        fixAttempts.clear();
    }
    
    /**
     * Gets statistics about the continuous development process.
     * @return Statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Add running status
        stats.put("running", isRunning());
        
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Add interval
        stats.put("intervalMinutes", settings.getContinuousDevelopmentIntervalMinutes());
        
        // Add error counts
        stats.put("errorCount", errorCounts.size());
        stats.put("fixAttempts", fixAttempts.size());
        
        // Add files with most errors
        List<Map.Entry<String, Integer>> topErrors = errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        
        stats.put("topErrors", topErrors);
        
        return stats;
    }
    
    /**
     * Processes a development cycle.
     * This is the main method that runs periodically.
     */
    private void processDevelopmentCycle() {
        try {
            LOG.info("Processing development cycle");
            
            // Save all documents
            ApplicationManager.getApplication().invokeAndWait(() -> {
                FileDocumentManager.getInstance().saveAllDocuments();
            });
            
            // Compile project
            compileProject();
            
        } catch (Exception e) {
            LOG.error("Error processing development cycle", e);
        }
    }
    
    /**
     * Compiles the project and handles errors.
     */
    private void compileProject() {
        LOG.info("Compiling project");
        
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        
        // Create compile scope
        CompileScope scope = compilerManager.createProjectCompileScope(project);
        
        // Compile notification
        CompileStatusNotification notification = (aborted, errors, warnings, compileContext) -> {
            LOG.info("Compilation finished: aborted=" + aborted + ", errors=" + errors + ", warnings=" + warnings);
            
            if (aborted) {
                LOG.info("Compilation was aborted");
                return;
            }
            
            if (errors > 0) {
                LOG.info("Compilation had errors, attempting to fix");
                
                // Get error messages
                CompilerMessage[] errorMessages = compileContext.getMessages(CompilerMessage.Kind.ERROR);
                
                // Fix errors
                fixCompilationErrors(errorMessages);
            } else {
                LOG.info("Compilation successful");
                
                // Reset error counts for successfully compiled files
                resetSuccessfulFiles();
            }
        };
        
        // Make sure to run this on the event dispatch thread
        ApplicationManager.getApplication().invokeAndWait(() -> {
            compilerManager.compile(scope, notification);
        });
    }
    
    /**
     * Resets error counts for successfully compiled files.
     */
    private void resetSuccessfulFiles() {
        // Create a copy of the keys to avoid concurrent modification
        Set<String> fileKeys = new HashSet<>(errorCounts.keySet());
        
        for (String filePath : fileKeys) {
            try {
                // Check if the file exists
                VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
                
                if (file != null) {
                    errorCounts.remove(filePath);
                    fixAttempts.remove(filePath);
                    LOG.info("Reset error count for successfully compiled file: " + filePath);
                }
            } catch (Exception e) {
                LOG.error("Error resetting error count for file: " + filePath, e);
            }
        }
    }
    
    /**
     * Fixes compilation errors.
     * @param errorMessages The error messages
     */
    private void fixCompilationErrors(CompilerMessage[] errorMessages) {
        if (errorMessages == null || errorMessages.length == 0) {
            LOG.info("No error messages to fix");
            return;
        }
        
        LOG.info("Fixing " + errorMessages.length + " compilation errors");
        
        // Group errors by file
        Map<String, List<CompilerMessage>> errorsByFile = groupErrorsByFile(errorMessages);
        
        // Fix each file
        for (Map.Entry<String, List<CompilerMessage>> entry : errorsByFile.entrySet()) {
            String filePath = entry.getKey();
            List<CompilerMessage> errors = entry.getValue();
            
            // Check if we should attempt to fix this file
            if (!shouldAttemptFix(filePath)) {
                LOG.info("Skipping fix for file: " + filePath + " (too many attempts)");
                continue;
            }
            
            // Fix file
            fixFile(filePath, errors);
        }
    }
    
    /**
     * Groups error messages by file.
     * @param errorMessages The error messages
     * @return Errors grouped by file
     */
    private Map<String, List<CompilerMessage>> groupErrorsByFile(CompilerMessage[] errorMessages) {
        Map<String, List<CompilerMessage>> errorsByFile = new HashMap<>();
        
        for (CompilerMessage message : errorMessages) {
            if (message.getVirtualFile() == null) {
                continue;
            }
            
            String filePath = message.getVirtualFile().getPath();
            
            if (!errorsByFile.containsKey(filePath)) {
                errorsByFile.put(filePath, new ArrayList<>());
            }
            
            errorsByFile.get(filePath).add(message);
        }
        
        return errorsByFile;
    }
    
    /**
     * Checks if we should attempt to fix a file.
     * @param filePath The file path
     * @return True if we should attempt to fix, false otherwise
     */
    private boolean shouldAttemptFix(String filePath) {
        // Get fix attempts
        int attempts = fixAttempts.getOrDefault(filePath, 0);
        
        // Increment
        fixAttempts.put(filePath, attempts + 1);
        
        // Check if we should attempt
        return attempts < MAX_FIX_ATTEMPTS;
    }
    
    /**
     * Fixes a file.
     * @param filePath The file path
     * @param errors The errors
     */
    private void fixFile(String filePath, List<CompilerMessage> errors) {
        LOG.info("Fixing file: " + filePath + " with " + errors.size() + " errors");
        
        try {
            // Get file
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            
            if (file == null) {
                LOG.error("File not found: " + filePath);
                return;
            }
            
            // Get PSI file
            PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
            
            if (psiFile == null) {
                LOG.error("PSI file not found: " + filePath);
                return;
            }
            
            // Get file content
            String content = ReadAction.compute(() -> psiFile.getText());
            
            // Build error message
            StringBuilder errorMessage = new StringBuilder();
            
            for (CompilerMessage error : errors) {
                errorMessage.append(error.getMessage()).append("\n");
                errorMessage.append("Line: ").append(error.getLine()).append(", Column: ").append(error.getColumn()).append("\n\n");
            }
            
            // Fix code
            LOG.info("Fixing code with error message: " + errorMessage);
            
            CompletableFuture<String> fixedCodeFuture = codeGenService.fixCode(
                    content,
                    errorMessage.toString(),
                    Map.of("filePath", filePath)
            );
            
            // Wait for fixed code
            String fixedCode;
            
            try {
                fixedCode = fixedCodeFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.error("Error getting fixed code", e);
                return;
            }
            
            if (fixedCode == null || fixedCode.isEmpty() || fixedCode.equals(content)) {
                LOG.info("No changes in fixed code, skipping update");
                return;
            }
            
            // Update file
            updateFile(file, fixedCode);
            
            // Update error count
            int count = errorCounts.getOrDefault(filePath, 0);
            errorCounts.put(filePath, count + errors.size());
            
            LOG.info("File fixed: " + filePath);
            
        } catch (Exception e) {
            LOG.error("Error fixing file: " + filePath, e);
        }
    }
    
    /**
     * Updates a file with new content.
     * @param file The file
     * @param newContent The new content
     */
    private void updateFile(VirtualFile file, String newContent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
                    fileDocumentManager.getDocument(file).setText(newContent);
                    fileDocumentManager.saveDocument(fileDocumentManager.getDocument(file));
                    
                    LOG.info("File updated: " + file.getPath());
                    
                } catch (Exception e) {
                    LOG.error("Error updating file: " + file.getPath(), e);
                }
            });
        });
    }
}