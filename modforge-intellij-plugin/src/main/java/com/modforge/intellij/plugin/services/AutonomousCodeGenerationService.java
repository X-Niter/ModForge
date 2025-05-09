package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Service for autonomous code generation.
 * This service provides methods for generating, fixing, and documenting code.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    private final PatternRecognitionService patternRecognitionService;
    private final ExecutorService executorService;
    
    /**
     * Creates a new AutonomousCodeGenerationService.
     * @param project The project
     */
    public AutonomousCodeGenerationService(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = AIServiceManager.getInstance();
        this.patternRecognitionService = PatternRecognitionService.getInstance();
        this.executorService = AppExecutorUtil.createBoundedApplicationPoolExecutor(
                "ModForge-CodeGeneration",
                4
        );
    }
    
    /**
     * Gets the instance of the service for a project.
     * @param project The project
     * @return The autonomous code generation service
     */
    public static AutonomousCodeGenerationService getInstance(@NotNull Project project) {
        return project.getService(AutonomousCodeGenerationService.class);
    }
    
    /**
     * Generates code based on a prompt.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return A future that completes with the generated code
     */
    @NotNull
    public CompletableFuture<String> generateCode(@NotNull String prompt, @NotNull String language, @Nullable Map<String, Object> options) {
        LOG.info("Generating code for prompt: " + prompt);
        
        return CompletableFuture.supplyAsync(() -> {
            // Try to find a pattern match
            String code = patternRecognitionService.findCodeGenerationMatch(prompt, language, options);
            
            if (code != null) {
                LOG.info("Using pattern match for code generation");
                return code;
            }
            
            // Use AI service
            LOG.info("Using AI service for code generation");
            
            try {
                code = aiServiceManager.generateCode(prompt, language, options);
                
                // Store pattern
                if (code != null && !code.isEmpty()) {
                    patternRecognitionService.storeCodeGenerationPattern(prompt, language, code, options);
                }
                
                return code;
            } catch (Exception e) {
                LOG.error("Error generating code", e);
                return "// Error generating code: " + e.getMessage();
            }
        }, executorService);
    }
    
    /**
     * Fixes code errors.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return A future that completes with the fixed code
     */
    @NotNull
    public CompletableFuture<String> fixCode(@NotNull String code, @Nullable String errorMessage, @Nullable Map<String, Object> options) {
        LOG.info("Fixing code errors");
        
        return CompletableFuture.supplyAsync(() -> {
            // Try to find a pattern match
            if (errorMessage != null && !errorMessage.isEmpty()) {
                String fixedCode = patternRecognitionService.findErrorFixMatch(code, errorMessage, options);
                
                if (fixedCode != null) {
                    LOG.info("Using pattern match for error fix");
                    return fixedCode;
                }
            }
            
            // Use AI service
            LOG.info("Using AI service for error fix");
            
            try {
                String fixedCode = aiServiceManager.fixCode(code, errorMessage, options);
                
                // Store pattern
                if (fixedCode != null && !fixedCode.isEmpty() && errorMessage != null && !errorMessage.isEmpty()) {
                    patternRecognitionService.storeErrorFixPattern(code, errorMessage, fixedCode, options);
                }
                
                return fixedCode;
            } catch (Exception e) {
                LOG.error("Error fixing code", e);
                return code;
            }
        }, executorService);
    }
    
    /**
     * Generates documentation for code.
     * @param code The code to document
     * @param options Additional options
     * @return A future that completes with the documented code
     */
    @NotNull
    public CompletableFuture<String> generateDocumentation(@NotNull String code, @Nullable Map<String, Object> options) {
        LOG.info("Generating documentation for code");
        
        return CompletableFuture.supplyAsync(() -> {
            // Try to find a pattern match
            String documentedCode = patternRecognitionService.findDocumentationMatch(code, options);
            
            if (documentedCode != null) {
                LOG.info("Using pattern match for documentation generation");
                return documentedCode;
            }
            
            // Use AI service
            LOG.info("Using AI service for documentation generation");
            
            try {
                documentedCode = aiServiceManager.generateDocumentation(code, options);
                
                // Store pattern
                if (documentedCode != null && !documentedCode.isEmpty()) {
                    patternRecognitionService.storeDocumentationPattern(code, documentedCode, options);
                }
                
                return documentedCode;
            } catch (Exception e) {
                LOG.error("Error generating documentation", e);
                return code;
            }
        }, executorService);
    }
    
    /**
     * Explains code.
     * @param code The code to explain
     * @param options Additional options
     * @return A future that completes with the explanation
     */
    @NotNull
    public CompletableFuture<String> explainCode(@NotNull String code, @Nullable Map<String, Object> options) {
        LOG.info("Explaining code");
        
        return CompletableFuture.supplyAsync(() -> {
            // Use AI service
            LOG.info("Using AI service for code explanation");
            
            try {
                return aiServiceManager.explainCode(code, options);
            } catch (Exception e) {
                LOG.error("Error explaining code", e);
                return "Error explaining code: " + e.getMessage();
            }
        }, executorService);
    }
    
    /**
     * Adds features to code.
     * @param code The code to add features to
     * @param featureDescription The feature description
     * @param options Additional options
     * @return A future that completes with the enhanced code
     */
    @NotNull
    public CompletableFuture<String> addFeatures(@NotNull String code, @NotNull String featureDescription, @Nullable Map<String, Object> options) {
        LOG.info("Adding features to code: " + featureDescription);
        
        return CompletableFuture.supplyAsync(() -> {
            // Try to find a pattern match
            String enhancedCode = patternRecognitionService.findFeatureAdditionMatch(code, featureDescription, options);
            
            if (enhancedCode != null) {
                LOG.info("Using pattern match for feature addition");
                return enhancedCode;
            }
            
            // Use AI service
            LOG.info("Using AI service for feature addition");
            
            try {
                enhancedCode = aiServiceManager.addFeatures(code, featureDescription, options);
                
                // Store pattern
                if (enhancedCode != null && !enhancedCode.isEmpty()) {
                    patternRecognitionService.storeFeatureAdditionPattern(code, featureDescription, enhancedCode, options);
                }
                
                return enhancedCode;
            } catch (Exception e) {
                LOG.error("Error adding features", e);
                return code;
            }
        }, executorService);
    }
    
    /**
     * A code issue.
     */
    public static final class CodeIssue {
        private final String message;
        private final int line;
        private final int column;
        private final String file;
        private final String code;
        
        /**
         * Creates a new CodeIssue.
         * @param message The error message
         * @param line The line number
         * @param column The column number
         * @param file The file path
         * @param code The code with the issue
         */
        public CodeIssue(@NotNull String message, int line, int column, @NotNull String file, @NotNull String code) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.file = file;
            this.code = code;
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
        
        /**
         * Gets the code with the issue.
         * @return The code with the issue
         */
        @NotNull
        public String getCode() {
            return code;
        }
    }
    
    /**
     * Disposes the service.
     */
    public void dispose() {
        executorService.shutdown();
    }
}