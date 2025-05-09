package com.modforge.intellij.plugin.services;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ml.ErrorClassifier;
import com.modforge.intellij.plugin.ml.ErrorPatternDatabase;
import com.modforge.intellij.plugin.ml.ErrorSignature;
import com.modforge.intellij.plugin.models.ErrorFix;
import com.modforge.intellij.plugin.models.ErrorResolutionRequest;
import com.modforge.intellij.plugin.models.ErrorResolutionResponse;
import com.modforge.intellij.plugin.models.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Service for resolving errors in Minecraft mods using machine learning.
 */
@Service
public final class ErrorResolutionService {
    private static final Logger LOG = Logger.getInstance(ErrorResolutionService.class);
    
    private final Project project;
    private final ExecutorService executorService;
    private final ErrorPatternDatabase patternDatabase;
    private final ErrorClassifier errorClassifier;
    private final ModForgeProjectService projectService;
    private final AIServiceManager aiServiceManager;
    
    // Stats
    private int totalErrorsProcessed = 0;
    private int errorsResolvedByPatterns = 0;
    private int errorsResolvedByAI = 0;
    private int failedResolutions = 0;
    private Map<String, Integer> errorTypeFrequency = new HashMap<>();
    private Map<String, Double> errorTypeSuccessRate = new HashMap<>();
    
    /**
     * Creates a new ErrorResolutionService.
     * @param project The project
     */
    public ErrorResolutionService(Project project) {
        this.project = project;
        this.executorService = AppExecutorUtil.getAppExecutorService();
        this.patternDatabase = new ErrorPatternDatabase(project);
        this.errorClassifier = new ErrorClassifier();
        this.projectService = project.getService(ModForgeProjectService.class);
        this.aiServiceManager = project.getService(AIServiceManager.class);
        
        // Initialize the error pattern database
        patternDatabase.initialize();
    }
    
    /**
     * Gets the instance of this service.
     * @param project The project
     * @return The service instance
     */
    public static ErrorResolutionService getInstance(@NotNull Project project) {
        return project.getService(ErrorResolutionService.class);
    }
    
    /**
     * Resolves the specified error.
     * @param errorMessage The error message
     * @param file The file containing the error
     * @param line The line number of the error
     * @param column The column number of the error
     * @return A CompletableFuture that resolves to the error fix, or null if resolution failed
     */
    public CompletableFuture<ErrorFix> resolveError(
            @NotNull String errorMessage,
            @NotNull VirtualFile file,
            int line,
            int column
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Resolving error: " + errorMessage);
                
                // Update stats
                totalErrorsProcessed++;
                updateErrorTypeStats(errorMessage);
                
                // Read the file content
                Document document = ApplicationManager.getApplication().runReadAction(
                        () -> com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)
                );
                
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getPath());
                    return null;
                }
                
                String fileContent = document.getText();
                String filePath = file.getPath();
                
                // Create error signature for pattern matching
                ErrorSignature errorSignature = new ErrorSignature(errorMessage, filePath, line, column, fileContent);
                
                // Try to resolve using patterns first
                ErrorFix patternFix = patternDatabase.findMatchingFix(errorSignature);
                
                if (patternFix != null) {
                    LOG.info("Found matching pattern for error resolution");
                    errorsResolvedByPatterns++;
                    updateErrorTypeSuccessRate(errorMessage, true);
                    return patternFix;
                }
                
                // If no pattern match, fall back to AI resolution
                LOG.info("No matching pattern found, using AI resolution");
                
                ErrorResolutionRequest request = new ErrorResolutionRequest();
                request.setErrorMessage(errorMessage);
                request.setFilePath(filePath);
                request.setFileContent(fileContent);
                request.setLineNumber(line);
                request.setColumnNumber(column);
                request.setModLoader(projectService.getModLoaderType());
                request.setMinecraftVersion(projectService.getMinecraftVersion());
                
                // Add relevant context files
                addContextFiles(request, file);
                
                // Get error type from classifier
                String errorType = errorClassifier.classifyError(errorMessage);
                request.setErrorType(errorType);
                
                // Send request to AI service
                ErrorResolutionResponse response = aiServiceManager.resolveError(request);
                
                if (response != null && response.getFixedCode() != null) {
                    LOG.info("Got AI resolution for error");
                    
                    // Create a fix from the response
                    ErrorFix fix = new ErrorFix(
                            response.getFixedCode(),
                            response.getExplanation(),
                            filePath,
                            line,
                            column
                    );
                    
                    // Store the fix as a pattern for future use
                    if (ModForgeSettingsService.getInstance().isEnablePatternLearning()) {
                        storeErrorPattern(errorSignature, fix, errorType);
                    }
                    
                    // Update stats
                    errorsResolvedByAI++;
                    updateErrorTypeSuccessRate(errorMessage, true);
                    
                    return fix;
                } else {
                    LOG.warn("AI resolution failed for error: " + errorMessage);
                    failedResolutions++;
                    updateErrorTypeSuccessRate(errorMessage, false);
                    return null;
                }
            } catch (Exception e) {
                LOG.error("Error resolving error", e);
                failedResolutions++;
                updateErrorTypeSuccessRate(errorMessage, false);
                return null;
            }
        }, executorService);
    }
    
    /**
     * Adds context files to the error resolution request.
     * @param request The error resolution request
     * @param errorFile The file containing the error
     */
    private void addContextFiles(ErrorResolutionRequest request, VirtualFile errorFile) {
        try {
            // Add relevant import files
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(
                    FileDocumentManager.getInstance().getDocument(errorFile)
            );
            
            if (psiFile == null) return;
            
            // Get all imports
            List<PsiElement> imports = new ArrayList<>();
            ApplicationManager.getApplication().runReadAction(() -> {
                imports.addAll(Arrays.asList(psiFile.getChildren()));
            });
            
            // Add up to 5 related files
            int count = 0;
            for (PsiElement element : imports) {
                if (count >= 5) break;
                
                if (element.getText().contains("import ")) {
                    // Extract the imported class
                    String importText = element.getText();
                    String className = importText.substring(importText.indexOf("import ") + 7).trim();
                    className = className.replace(";", "");
                    
                    // Resolve the class
                    PsiClass psiClass = JavaPsiFacade.getInstance(project)
                            .findClass(className, GlobalSearchScope.allScope(project));
                    
                    if (psiClass != null) {
                        VirtualFile classFile = psiClass.getContainingFile().getVirtualFile();
                        if (classFile != null && !classFile.equals(errorFile)) {
                            Document doc = FileDocumentManager.getInstance().getDocument(classFile);
                            if (doc != null) {
                                request.addContextFile(classFile.getPath(), doc.getText());
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Error adding context files to error resolution request", e);
        }
    }
    
    /**
     * Stores an error pattern for future use.
     * @param errorSignature The error signature
     * @param fix The error fix
     * @param errorType The error type
     */
    private void storeErrorPattern(ErrorSignature errorSignature, ErrorFix fix, String errorType) {
        try {
            // Create a pattern from the error and fix
            Pattern pattern = new Pattern(
                    Pattern.PatternType.ERROR_RESOLUTION,
                    errorSignature.serialize(),
                    fix.getFixedCode(),
                    projectService.getModLoaderType(),
                    projectService.getMinecraftVersion()
            );
            
            // Add metadata
            pattern.setDescription("Error fix for: " + errorType);
            
            // Store the pattern
            patternDatabase.storePattern(pattern);
            
            LOG.info("Stored error pattern for future use");
        } catch (Exception e) {
            LOG.error("Error storing error pattern", e);
        }
    }
    
    /**
     * Applies a fix to the specified file.
     * @param fix The fix to apply
     * @return Whether the fix was applied successfully
     */
    public boolean applyFix(ErrorFix fix) {
        try {
            VirtualFile file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(fix.getFilePath());
            if (file == null) {
                LOG.warn("Could not find file: " + fix.getFilePath());
                return false;
            }
            
            Document document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                LOG.warn("Could not get document for file: " + fix.getFilePath());
                return false;
            }
            
            // Apply the fix in a write action
            ApplicationManager.getApplication().runWriteAction(() -> {
                document.setText(fix.getFixedCode());
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveDocument(document);
            });
            
            // Open the file in the editor
            UIUtil.invokeLaterIfNeeded(() -> {
                FileEditorManager.getInstance(project).openFile(file, true);
            });
            
            return true;
        } catch (Exception e) {
            LOG.error("Error applying fix", e);
            return false;
        }
    }
    
    /**
     * Updates the error type frequency statistics.
     * @param errorMessage The error message
     */
    private void updateErrorTypeStats(String errorMessage) {
        String errorType = errorClassifier.classifyError(errorMessage);
        errorTypeFrequency.put(errorType, errorTypeFrequency.getOrDefault(errorType, 0) + 1);
    }
    
    /**
     * Updates the error type success rate statistics.
     * @param errorMessage The error message
     * @param success Whether the error was successfully resolved
     */
    private void updateErrorTypeSuccessRate(String errorMessage, boolean success) {
        String errorType = errorClassifier.classifyError(errorMessage);
        
        // Update success rate
        double currentRate = errorTypeSuccessRate.getOrDefault(errorType, 0.0);
        int count = errorTypeFrequency.getOrDefault(errorType, 0);
        
        if (count > 0) {
            // Weighted average: give more weight to recent results
            double newRate = (currentRate * (count - 1) + (success ? 1.0 : 0.0)) / count;
            errorTypeSuccessRate.put(errorType, newRate);
        }
    }
    
    /**
     * Gets statistics about error resolution.
     * @return Statistics about error resolution
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrorsProcessed", totalErrorsProcessed);
        stats.put("errorsResolvedByPatterns", errorsResolvedByPatterns);
        stats.put("errorsResolvedByAI", errorsResolvedByAI);
        stats.put("failedResolutions", failedResolutions);
        stats.put("errorTypeFrequency", errorTypeFrequency);
        stats.put("errorTypeSuccessRate", errorTypeSuccessRate);
        stats.put("patternDatabaseSize", patternDatabase.getPatternCount());
        
        // Calculate overall success rate
        double successRate = 0;
        if (totalErrorsProcessed > 0) {
            successRate = (double) (errorsResolvedByPatterns + errorsResolvedByAI) / totalErrorsProcessed;
        }
        stats.put("overallSuccessRate", successRate);
        
        return stats;
    }
    
    /**
     * Gets the error pattern database.
     * @return The error pattern database
     */
    public ErrorPatternDatabase getPatternDatabase() {
        return patternDatabase;
    }
    
    /**
     * FileDocumentManager for getting documents from files.
     */
    private static class FileDocumentManager {
        public static FileDocumentManager getInstance() {
            return new FileDocumentManager();
        }
        
        public Document getDocument(VirtualFile file) {
            return com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file);
        }
    }
}