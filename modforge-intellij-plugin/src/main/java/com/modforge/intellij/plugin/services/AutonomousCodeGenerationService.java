package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
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
import org.jetbrains.annotations.Nullable;
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
import java.util.regex.Pattern;

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
        
        // Create prompt for code explanation
        String prompt = "Explain the following code in detail:\n\n" + code;
        
        // Create system prompt for code explanation
        String systemPrompt = "You are an expert Minecraft mod developer and educator. " +
                "Provide a detailed explanation of the provided code. " +
                "Break down the explanation into sections: " +
                "1. Overview - What the code does at a high level " +
                "2. Key Components - The main classes, methods, and data structures " +
                "3. Flow - How the code executes step by step " +
                "4. Best Practices - Good patterns used in the code " +
                "5. Potential Improvements - Any suggestions for making the code better";
        
        requestOptions.put("systemPrompt", systemPrompt);
        requestOptions.put("temperature", 0.3);
        requestOptions.put("maxTokens", 2048);
        
        // Explain code using AI service
        return aiServiceManager.generateChatCompletion(prompt, requestOptions);
    }
    
    /**
     * Analyzes a file for issues.
     * @param file The file to analyze
     * @return A future that completes with a list of issues
     */
    public CompletableFuture<List<CodeIssue>> analyzeFile(@NotNull VirtualFile file) {
        LOG.info("Analyzing file: " + file.getName());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get document
                Document document = FileDocumentManager.getInstance().getDocument(file);
                
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getName());
                    return new ArrayList<>();
                }
                
                // Get file content
                String content = document.getText();
                
                // Get PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                
                if (psiFile == null) {
                    LOG.warn("Could not get PSI file for file: " + file.getName());
                    return new ArrayList<>();
                }
                
                // Get language
                String language = psiFile.getLanguage().getDisplayName().toLowerCase();
                
                // Create options
                Map<String, Object> options = new HashMap<>();
                options.put("language", language);
                options.put("fileName", file.getName());
                options.put("filePath", file.getPath());
                
                // Create prompt for code analysis
                String prompt = "Analyze the following code for potential issues and improvements:\n\n" + content;
                
                // Create system prompt for code analysis
                String systemPrompt = "You are an expert code analyzer and Minecraft mod developer. " +
                        "Identify potential issues, bugs, and areas for improvement in the provided code. " +
                        "Focus on issues that would cause compilation errors, runtime errors, or poor performance. " +
                        "Format your response as a JSON array of issues. Each issue should have the following fields: " +
                        "message (string), line (number), column (number), file (string), code (string snippet). " +
                        "Only include the JSON array in your response, no other text.";
                
                options.put("systemPrompt", systemPrompt);
                options.put("temperature", 0.1);
                options.put("maxTokens", 2048);
                
                // Analyze code using AI service
                String response = aiServiceManager.generateChatCompletion(prompt, options)
                        .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                // Parse response
                return parseIssues(response, file.getPath());
            } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Error analyzing file: " + file.getName(), e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Fixes issues in a file.
     * @param file The file
     * @param issues The issues to fix
     * @return A future that completes with the number of issues fixed
     */
    public CompletableFuture<Integer> fixIssues(@NotNull VirtualFile file, @NotNull List<CodeIssue> issues) {
        LOG.info("Fixing " + issues.size() + " issues in file: " + file.getName());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get document
                Document document = FileDocumentManager.getInstance().getDocument(file);
                
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getName());
                    return 0;
                }
                
                // Get file content
                String content = document.getText();
                
                // Count fixed issues
                int fixedCount = 0;
                
                // Fix each issue
                for (CodeIssue issue : issues) {
                    try {
                        // Get code snippet
                        String codeSnippet = issue.getCode();
                        
                        if (codeSnippet == null || codeSnippet.isEmpty()) {
                            LOG.warn("No code snippet for issue: " + issue.getMessage());
                            continue;
                        }
                        
                        // Fix code snippet
                        Map<String, Object> options = new HashMap<>();
                        options.put("fileName", file.getName());
                        options.put("filePath", file.getPath());
                        options.put("line", issue.getLine());
                        options.put("column", issue.getColumn());
                        
                        String fixedSnippet = fixCode(codeSnippet, issue.getMessage(), options)
                                .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        
                        if (fixedSnippet.equals(codeSnippet)) {
                            LOG.warn("Could not fix issue: " + issue.getMessage());
                            continue;
                        }
                        
                        // Replace code snippet in document
                        int startOffset = content.indexOf(codeSnippet);
                        
                        if (startOffset == -1) {
                            LOG.warn("Could not find code snippet in file: " + file.getName());
                            continue;
                        }
                        
                        int endOffset = startOffset + codeSnippet.length();
                        
                        // Update document
                        ApplicationManager.getApplication().invokeAndWait(() -> {
                            WriteCommandAction.runWriteCommandAction(project, () -> {
                                CommandProcessor.getInstance().executeCommand(project, () -> {
                                    document.replaceString(startOffset, endOffset, fixedSnippet);
                                    
                                    // Save document
                                    FileDocumentManager.getInstance().saveDocument(document);
                                }, "Fix Issue", null);
                            });
                        });
                        
                        // Update content
                        content = document.getText();
                        
                        // Increment fixed count
                        fixedCount++;
                    } catch (Exception e) {
                        LOG.error("Error fixing issue: " + issue.getMessage(), e);
                    }
                }
                
                return fixedCount;
            } catch (Exception e) {
                LOG.error("Error fixing issues in file: " + file.getName(), e);
                return 0;
            }
        });
    }
    
    /**
     * Fixes errors in a file.
     * @param file The file
     * @param errorMessages The error messages
     * @return A future that completes with a boolean indicating success
     */
    public CompletableFuture<Boolean> fixErrorsInFile(@NotNull VirtualFile file, @NotNull List<String> errorMessages) {
        LOG.info("Fixing errors in file: " + file.getName());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get document
                Document document = FileDocumentManager.getInstance().getDocument(file);
                
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getName());
                    return false;
                }
                
                // Get file content
                String content = document.getText();
                
                // Create options
                Map<String, Object> options = new HashMap<>();
                options.put("fileName", file.getName());
                options.put("filePath", file.getPath());
                
                // Join error messages
                String errorMessage = String.join("\n", errorMessages);
                
                // Fix code
                String fixedCode = fixCode(content, errorMessage, options)
                        .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                if (fixedCode.equals(content)) {
                    LOG.warn("Could not fix errors in file: " + file.getName());
                    return false;
                }
                
                // Update document
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        CommandProcessor.getInstance().executeCommand(project, () -> {
                            document.setText(fixedCode);
                            
                            // Save document
                            FileDocumentManager.getInstance().saveDocument(document);
                        }, "Fix Errors", null);
                    });
                });
                
                return true;
            } catch (Exception e) {
                LOG.error("Error fixing errors in file: " + file.getName(), e);
                return false;
            }
        });
    }
    
    /**
     * Analyzes and enhances a file.
     * @param file The file to analyze and enhance
     * @return A future that completes with a boolean indicating if enhancements were made
     */
    public CompletableFuture<Boolean> analyzeAndEnhanceFile(@NotNull VirtualFile file) {
        LOG.info("Analyzing and enhancing file: " + file.getName());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get document
                Document document = FileDocumentManager.getInstance().getDocument(file);
                
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getName());
                    return false;
                }
                
                // Get file content
                String content = document.getText();
                
                // Get PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                
                if (psiFile == null) {
                    LOG.warn("Could not get PSI file for file: " + file.getName());
                    return false;
                }
                
                // Get language
                String language = psiFile.getLanguage().getDisplayName().toLowerCase();
                
                // Create options
                Map<String, Object> options = new HashMap<>();
                options.put("language", language);
                options.put("fileName", file.getName());
                options.put("filePath", file.getPath());
                
                // Create prompt for code enhancement
                String prompt = "Enhance the following code to improve readability, performance, " +
                        "and maintainability. Focus on adding proper documentation, optimizing algorithms, " +
                        "and following Minecraft modding best practices:\n\n" + content;
                
                // Create system prompt for code enhancement
                String systemPrompt = "You are an expert Minecraft mod developer and code optimizer. " +
                        "Improve the provided code by adding comprehensive documentation, optimizing algorithms, " +
                        "and applying best practices for Minecraft modding. " +
                        "Make sure the enhanced code maintains the original functionality. " +
                        "Only output the enhanced code, no explanations or markdown formatting.";
                
                options.put("systemPrompt", systemPrompt);
                options.put("temperature", 0.1);
                options.put("maxTokens", 2048);
                
                // Enhance code using AI service
                String enhancedCode = aiServiceManager.generateChatCompletion(prompt, options)
                        .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                if (enhancedCode.equals(content)) {
                    LOG.info("No enhancements made to file: " + file.getName());
                    return false;
                }
                
                // Update document
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        CommandProcessor.getInstance().executeCommand(project, () -> {
                            document.setText(enhancedCode);
                            
                            // Save document
                            FileDocumentManager.getInstance().saveDocument(document);
                        }, "Enhance Code", null);
                    });
                });
                
                return true;
            } catch (Exception e) {
                LOG.error("Error enhancing file: " + file.getName(), e);
                return false;
            }
        });
    }
    
    /**
     * Parses issues from an API response.
     * @param response The API response
     * @param filePath The file path
     * @return A list of issues
     */
    private List<CodeIssue> parseIssues(@NotNull String response, @NotNull String filePath) {
        List<CodeIssue> issues = new ArrayList<>();
        
        try {
            // Parse JSON response
            // This is a simple extraction, in a real system we would use a JSON library
            String jsonArray = response.trim();
            
            if (jsonArray.startsWith("[") && jsonArray.endsWith("]")) {
                // Extract issues
                String[] issueStrings = jsonArray.substring(1, jsonArray.length() - 1).split("\\},\\s*\\{");
                
                for (String issueString : issueStrings) {
                    // Add braces if removed by split
                    if (!issueString.startsWith("{")) {
                        issueString = "{" + issueString;
                    }
                    if (!issueString.endsWith("}")) {
                        issueString = issueString + "}";
                    }
                    
                    // Extract fields
                    String message = extractJsonField(issueString, "message");
                    int line = extractJsonIntField(issueString, "line");
                    int column = extractJsonIntField(issueString, "column");
                    String file = extractJsonField(issueString, "file");
                    String code = extractJsonField(issueString, "code");
                    
                    if (message != null && !message.isEmpty() && code != null && !code.isEmpty()) {
                        // Use filePath if file is empty
                        if (file == null || file.isEmpty()) {
                            file = filePath;
                        }
                        
                        issues.add(new CodeIssue(message, line, column, file, code));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error parsing issues from response: " + response, e);
        }
        
        return issues;
    }
    
    /**
     * Extracts a string field from a JSON object string.
     * @param json The JSON object string
     * @param field The field name
     * @return The field value
     */
    private String extractJsonField(@NotNull String json, @NotNull String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extracts an integer field from a JSON object string.
     * @param json The JSON object string
     * @param field The field name
     * @return The field value
     */
    private int extractJsonIntField(@NotNull String json, @NotNull String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                LOG.error("Error parsing integer field: " + field, e);
            }
        }
        
        return 0;
    }
}