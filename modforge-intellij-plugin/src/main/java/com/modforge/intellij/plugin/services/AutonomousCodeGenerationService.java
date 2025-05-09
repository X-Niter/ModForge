package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.ai.PatternRecognitionService.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

/**
 * Service that provides autonomous code generation capabilities.
 * This service is responsible for generating and modifying code using AI.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    private final PatternRecognitionService patternRecognitionService;
    
    /**
     * Creates a new AutonomousCodeGenerationService.
     * @param project The project
     */
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
        this.aiServiceManager = AIServiceManager.getInstance();
        this.patternRecognitionService = PatternRecognitionService.getInstance();
        
        LOG.info("Autonomous code generation service created for project: " + project.getName());
    }
    
    /**
     * Gets the autonomous code generation service for a project.
     * @param project The project
     * @return The autonomous code generation service
     */
    public static AutonomousCodeGenerationService getInstance(@NotNull Project project) {
        return project.getService(AutonomousCodeGenerationService.class);
    }
    
    /**
     * Represents a code issue.
     */
    public static class CodeIssue {
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
         * @param code The code snippet
         */
        public CodeIssue(String message, int line, int column, String file, String code) {
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
        
        /**
         * Gets the code snippet.
         * @return The code snippet
         */
        public String getCode() {
            return code;
        }
    }
    
    /**
     * Generates code based on a prompt.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return A future that completes with the generated code
     */
    public CompletableFuture<String> generateCode(@NotNull String prompt, @NotNull String language, 
                                                @Nullable Map<String, Object> options) {
        LOG.info("Generating code for prompt: " + prompt);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Add language to options
        requestOptions.put("language", language);
        
        // Check if a pattern matches
        Pattern pattern = patternRecognitionService.findCodePattern(prompt, requestOptions);
        
        if (pattern != null) {
            LOG.info("Found matching pattern for code generation");
            
            // Get output from pattern
            String output = pattern.getOutput();
            
            // Record pattern result
            patternRecognitionService.recordPatternResult(pattern, true);
            
            return CompletableFuture.completedFuture(output);
        }
        
        // No pattern matched, use AI service
        LOG.info("No matching pattern found, using AI service for code generation");
        
        // Create system prompt for code generation
        String systemPrompt = "You are an expert Minecraft mod developer. " +
                "Generate high-quality, well-documented " + language + " code based on the user's request. " +
                "Focus on creating efficient, maintainable code that follows best practices for Minecraft modding. " +
                "Make sure the generated code is complete and ready to use. " +
                "Only output the code, no explanations or markdown formatting.";
        
        requestOptions.put("systemPrompt", systemPrompt);
        requestOptions.put("temperature", 0.2);
        requestOptions.put("maxTokens", 2048);
        
        // Generate code using AI service
        CompletableFuture<String> future = aiServiceManager.generateChatCompletion(prompt, requestOptions);
        
        // Store pattern when finished
        future.thenAccept(code -> {
            if (code != null && !code.isEmpty()) {
                patternRecognitionService.storeCodePattern(prompt, code, requestOptions);
            }
        });
        
        return future;
    }
    
    /**
     * Fixes code based on an error message.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return A future that completes with the fixed code
     */
    public CompletableFuture<String> fixCode(@NotNull String code, @Nullable String errorMessage, 
                                          @Nullable Map<String, Object> options) {
        LOG.info("Fixing code with error: " + (errorMessage != null ? errorMessage : "No error message"));
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Check if a pattern matches
        String patternInput = errorMessage != null ? code + "\n\nError: " + errorMessage : code;
        Pattern pattern = patternRecognitionService.findErrorPattern(patternInput, requestOptions);
        
        if (pattern != null) {
            LOG.info("Found matching pattern for error fixing");
            
            // Get output from pattern
            String output = pattern.getOutput();
            
            // Record pattern result
            patternRecognitionService.recordPatternResult(pattern, true);
            
            return CompletableFuture.completedFuture(output);
        }
        
        // No pattern matched, use AI service
        LOG.info("No matching pattern found, using AI service for error fixing");
        
        // Create prompt for error fixing
        String prompt = "Fix the following code:\n\n" + code;
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            prompt += "\n\nError: " + errorMessage;
        }
        
        // Create system prompt for error fixing
        String systemPrompt = "You are an expert Minecraft mod developer and debugging assistant. " +
                "Fix the provided code to resolve any errors or issues. " +
                "Maintain the original functionality while making the code more robust and efficient. " +
                "Only output the fixed code, no explanations or markdown formatting.";
        
        requestOptions.put("systemPrompt", systemPrompt);
        requestOptions.put("temperature", 0.1);
        requestOptions.put("maxTokens", 2048);
        
        // Fix code using AI service
        CompletableFuture<String> future = aiServiceManager.generateChatCompletion(prompt, requestOptions);
        
        // Store pattern when finished
        future.thenAccept(fixedCode -> {
            if (fixedCode != null && !fixedCode.isEmpty() && !fixedCode.equals(code)) {
                patternRecognitionService.storeErrorPattern(patternInput, fixedCode, requestOptions);
            }
        });
        
        return future;
    }
    
    /**
     * Generates documentation for code.
     * @param code The code to document
     * @param options Additional options
     * @return A future that completes with the documented code
     */
    public CompletableFuture<String> generateDocumentation(@NotNull String code, @Nullable Map<String, Object> options) {
        LOG.info("Generating documentation for code");
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Check if a pattern matches
        Pattern pattern = patternRecognitionService.findDocumentationPattern(code, requestOptions);
        
        if (pattern != null) {
            LOG.info("Found matching pattern for documentation generation");
            
            // Get output from pattern
            String output = pattern.getOutput();
            
            // Record pattern result
            patternRecognitionService.recordPatternResult(pattern, true);
            
            return CompletableFuture.completedFuture(output);
        }
        
        // No pattern matched, use AI service
        LOG.info("No matching pattern found, using AI service for documentation generation");
        
        // Create prompt for documentation generation
        String prompt = "Generate documentation for the following code:\n\n" + code;
        
        // Create system prompt for documentation generation
        String systemPrompt = "You are an expert Minecraft mod developer and technical writer. " +
                "Generate comprehensive JavaDoc/KDoc style documentation for the provided code. " +
                "Include class-level documentation, method documentation with parameter and return value descriptions, " +
                "and field documentation. Maintain the original code while adding detailed comments. " +
                "Only output the documented code, no explanations or markdown formatting.";
        
        requestOptions.put("systemPrompt", systemPrompt);
        requestOptions.put("temperature", 0.1);
        requestOptions.put("maxTokens", 2048);
        
        // Generate documentation using AI service
        CompletableFuture<String> future = aiServiceManager.generateChatCompletion(prompt, requestOptions);
        
        // Store pattern when finished
        future.thenAccept(documentedCode -> {
            if (documentedCode != null && !documentedCode.isEmpty() && !documentedCode.equals(code)) {
                patternRecognitionService.storeDocumentationPattern(code, documentedCode, requestOptions);
            }
        });
        
        return future;
    }
    
    /**
     * Explains code.
     * @param code The code to explain
     * @param options Additional options
     * @return A future that completes with the explanation
     */
    public CompletableFuture<String> explainCode(@NotNull String code, @Nullable Map<String, Object> options) {
        LOG.info("Explaining code");
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Create prompt for explanation
        String prompt = "Explain the following code:\n\n" + code;
        
        // Create system prompt for explanation
        String systemPrompt = "You are an expert Minecraft mod developer and technical writer. " +
                "Explain the following code in detail, including its purpose, functionality, and any notable patterns or techniques. " +
                "Be thorough but concise, and focus on helping the user understand the code completely. " +
                "Use examples where appropriate to illustrate key concepts.";
        
        requestOptions.put("systemPrompt", systemPrompt);
        requestOptions.put("temperature", 0.3);
        requestOptions.put("maxTokens", 2048);
        
        // Generate explanation using AI service
        return aiServiceManager.generateChatCompletion(prompt, requestOptions);
    }
    
    /**
     * Generates a chat response.
     * @param prompt The prompt
     * @param options Additional options
     * @return A future that completes with the response
     */
    public CompletableFuture<String> generateChatResponse(@NotNull String prompt, @Nullable Map<String, Object> options) {
        LOG.info("Generating chat response for prompt: " + prompt);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Create system prompt for chat
        String systemPrompt = "You are an expert Minecraft mod developer assistant named ModForge. " +
                "Provide helpful, concise, and accurate responses to questions about Minecraft modding. " +
                "Prioritize code examples and practical solutions. " +
                "Consider multiple mod loaders (Forge, Fabric, Quilt) and Minecraft versions in your answers. " +
                "If you're uncertain, be honest about your limitations.";
        
        requestOptions.put("systemPrompt", systemPrompt);
        requestOptions.put("temperature", 0.7);
        requestOptions.put("maxTokens", 2048);
        
        // Generate chat response using AI service
        return aiServiceManager.generateChatCompletion(prompt, requestOptions);
    }
    
    /**
     * Fixes a list of code issues in a batch.
     * @param issues The code issues to fix
     * @param options Additional options
     * @return A future that completes with a map of file paths to fixed code
     */
    public CompletableFuture<Map<String, String>> fixCodeIssues(@NotNull List<CodeIssue> issues, @Nullable Map<String, Object> options) {
        LOG.info("Fixing " + issues.size() + " code issues");
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Group issues by file
        Map<String, List<CodeIssue>> issuesByFile = new HashMap<>();
        
        for (CodeIssue issue : issues) {
            if (!issuesByFile.containsKey(issue.getFile())) {
                issuesByFile.put(issue.getFile(), new ArrayList<>());
            }
            
            issuesByFile.get(issue.getFile()).add(issue);
        }
        
        // Fix each file
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        
        for (Map.Entry<String, List<CodeIssue>> entry : issuesByFile.entrySet()) {
            String filePath = entry.getKey();
            List<CodeIssue> fileIssues = entry.getValue();
            
            // Get file content
            String fileContent = getFileContent(filePath);
            
            if (fileContent == null) {
                LOG.error("Could not get content for file: " + filePath);
                continue;
            }
            
            // Create error message
            StringBuilder errorMessage = new StringBuilder();
            
            for (CodeIssue issue : fileIssues) {
                errorMessage.append(issue.getMessage()).append(" (Line ").append(issue.getLine()).append(")\n");
            }
            
            // Create options for file
            Map<String, Object> fileOptions = new HashMap<>(requestOptions);
            fileOptions.put("filePath", filePath);
            
            // Fix code
            CompletableFuture<String> future = fixCode(fileContent, errorMessage.toString(), fileOptions);
            
            futures.put(filePath, future);
        }
        
        // Combine futures
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, String> result = new HashMap<>();
                    
                    for (Map.Entry<String, CompletableFuture<String>> entry : futures.entrySet()) {
                        try {
                            result.put(entry.getKey(), entry.getValue().get());
                        } catch (InterruptedException | ExecutionException e) {
                            LOG.error("Error getting fixed code for file: " + entry.getKey(), e);
                        }
                    }
                    
                    return result;
                });
    }
    
    /**
     * Gets the content of a file.
     * @param filePath The file path
     * @return The file content or null if an error occurs
     */
    @Nullable
    private String getFileContent(@NotNull String filePath) {
        try {
            // Find the file
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            
            if (file == null) {
                LOG.error("File not found: " + filePath);
                return null;
            }
            
            // Get document
            Document document = FileDocumentManager.getInstance().getDocument(file);
            
            if (document == null) {
                LOG.error("Could not get document for file: " + filePath);
                return null;
            }
            
            // Get content
            return document.getText();
        } catch (Exception e) {
            LOG.error("Error getting file content: " + filePath, e);
            return null;
        }
    }
}