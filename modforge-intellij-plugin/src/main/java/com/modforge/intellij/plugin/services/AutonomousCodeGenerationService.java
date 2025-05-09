package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for autonomous code generation.
 * This service provides methods for generating code, analyzing files for issues,
 * and fixing issues automatically.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    private final CodeAnalysisService codeAnalysisService;
    
    // Cache of analyzed files
    private final Map<String, List<CodeIssue>> issueCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new AutonomousCodeGenerationService.
     * @param project The project
     */
    public AutonomousCodeGenerationService(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = project.getService(AIServiceManager.class);
        this.codeAnalysisService = project.getService(CodeAnalysisService.class);
        
        LOG.info("AutonomousCodeGenerationService initialized for project: " + project.getName());
    }
    
    /**
     * Gets the AutonomousCodeGenerationService instance.
     * @param project The project
     * @return The AutonomousCodeGenerationService instance
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
    public CompletableFuture<String> generateCode(@NotNull String prompt, @NotNull String language,
                                               @Nullable Map<String, Object> options) {
        LOG.info("Generating code for prompt: " + prompt);
        
        // Use AI service manager to generate code
        return aiServiceManager.generateCode(prompt, language, options);
    }
    
    /**
     * Analyzes a file for potential issues.
     * @param file The file to analyze
     * @return A future that completes with a list of issues
     */
    @NotNull
    public CompletableFuture<List<CodeIssue>> analyzeFile(@NotNull VirtualFile file) {
        LOG.info("Analyzing file: " + file.getPath());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache
                String filePath = file.getPath();
                List<CodeIssue> cachedIssues = issueCache.get(filePath);
                
                if (cachedIssues != null) {
                    return cachedIssues;
                }
                
                // Get document
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) {
                    return Collections.emptyList();
                }
                
                // Get file content
                String content = document.getText();
                if (content.isEmpty()) {
                    return Collections.emptyList();
                }
                
                // Get file extension
                String extension = file.getExtension();
                
                // Detect code issues
                List<CodeIssue> issues = new ArrayList<>();
                
                if ("java".equals(extension)) {
                    // Analyze Java file
                    issues.addAll(analyzeJavaFile(file, content));
                } else if ("kt".equals(extension)) {
                    // Analyze Kotlin file
                    issues.addAll(analyzeKotlinFile(content));
                } else if ("js".equals(extension) || "ts".equals(extension)) {
                    // Analyze JavaScript/TypeScript file
                    issues.addAll(analyzeJsFile(content));
                } else {
                    // Generic analysis
                    issues.addAll(analyzeGenericFile(content));
                }
                
                // Cache issues
                issueCache.put(filePath, issues);
                
                return issues;
            } catch (Exception e) {
                LOG.error("Error analyzing file: " + file.getPath(), e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Fixes issues in a file.
     * @param file The file to fix
     * @param issues The issues to fix
     * @return A future that completes with the number of fixed issues
     */
    @NotNull
    public CompletableFuture<Integer> fixIssues(@NotNull VirtualFile file, @NotNull List<CodeIssue> issues) {
        LOG.info("Fixing " + issues.size() + " issues in file: " + file.getPath());
        
        if (issues.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get document
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) {
                    return 0;
                }
                
                // Get file content
                String content = document.getText();
                if (content.isEmpty()) {
                    return 0;
                }
                
                // Get PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    return 0;
                }
                
                // Sort issues by position (highest offset first to avoid shifting)
                List<CodeIssue> sortedIssues = new ArrayList<>(issues);
                sortedIssues.sort((a, b) -> Integer.compare(b.getPosition(), a.getPosition()));
                
                // Fix issues
                int fixedCount = 0;
                
                for (CodeIssue issue : sortedIssues) {
                    if (issue.hasFix()) {
                        // Apply fix
                        String newContent = applyFix(content, issue);
                        
                        if (!newContent.equals(content)) {
                            // Update document
                            updateDocument(document, newContent);
                            content = newContent;
                            fixedCount++;
                            
                            // Wait a bit to avoid conflicts
                            Thread.sleep(100);
                        }
                    } else if (!issue.getDescription().isEmpty() && issue.getPosition() >= 0) {
                        // Generate fix using AI
                        try {
                            // Get relevant code snippet
                            String codeSnippet = getRelevantCodeSnippet(content, issue.getPosition(), 10);
                            
                            // Generate fix
                            String fixPrompt = "Fix the following code issue: " + issue.getDescription() + 
                                    "\n\nCode snippet:\n```\n" + codeSnippet + "\n```\n\n" +
                                    "Provide only the fixed code snippet without any explanations.";
                            
                            String fixedCode = aiServiceManager.fixCode(codeSnippet, issue.getDescription(), null)
                                    .get(30, TimeUnit.SECONDS);
                            
                            if (fixedCode != null && !fixedCode.isEmpty() && !fixedCode.equals(codeSnippet)) {
                                // Clean up AI response
                                fixedCode = cleanupAiResponse(fixedCode);
                                
                                // Apply fix
                                String newContent = content.substring(0, issue.getPosition()) +
                                        fixedCode +
                                        content.substring(issue.getPosition() + codeSnippet.length());
                                
                                // Update document
                                updateDocument(document, newContent);
                                content = newContent;
                                fixedCount++;
                                
                                // Wait a bit to avoid conflicts
                                Thread.sleep(100);
                            }
                        } catch (Exception e) {
                            LOG.error("Error generating fix for issue: " + issue.getDescription(), e);
                        }
                    }
                }
                
                // Clear cache for this file
                issueCache.remove(file.getPath());
                
                return fixedCount;
            } catch (Exception e) {
                LOG.error("Error fixing issues in file: " + file.getPath(), e);
                return 0;
            }
        });
    }
    
    /**
     * Analyzes a Java file for issues.
     * @param file The file to analyze
     * @param content The file content
     * @return A list of issues
     */
    @NotNull
    private List<CodeIssue> analyzeJavaFile(@NotNull VirtualFile file, @NotNull String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        try {
            // Get PSI file
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!(psiFile instanceof PsiJavaFile)) {
                return issues;
            }
            
            PsiJavaFile javaFile = (PsiJavaFile) psiFile;
            
            // Check for missing @Override annotations
            for (PsiClass psiClass : javaFile.getClasses()) {
                for (PsiMethod method : psiClass.getMethods()) {
                    // Skip methods that already have @Override
                    boolean hasOverride = false;
                    for (PsiAnnotation annotation : method.getAnnotations()) {
                        if (annotation.getQualifiedName() != null && 
                                annotation.getQualifiedName().equals("java.lang.Override")) {
                            hasOverride = true;
                            break;
                        }
                    }
                    
                    if (hasOverride) {
                        continue;
                    }
                    
                    // Check if method overrides a superclass or interface method
                    PsiMethod[] superMethods = method.findSuperMethods();
                    if (superMethods.length > 0) {
                        CodeIssue issue = new CodeIssue(
                                CodeIssueType.MISSING_OVERRIDE,
                                "Missing @Override annotation for method: " + method.getName(),
                                method.getTextOffset(),
                                true,
                                "Add @Override annotation"
                        );
                        
                        issues.add(issue);
                    }
                }
            }
            
            // Check for unused imports
            for (PsiImportStatement importStatement : javaFile.getImportList().getImportStatements()) {
                PsiJavaCodeReferenceElement reference = importStatement.getImportReference();
                if (reference == null) {
                    continue;
                }
                
                boolean isReferenced = ApplicationManager.getApplication().runReadAction(
                        (Computable<Boolean>) () -> {
                            PsiElement target = reference.resolve();
                            return target != null && 
                                    com.intellij.psi.codeStyle.JavaCodeStyleManager.getInstance(project)
                                            .isReferenceUsed(reference, javaFile);
                        });
                
                if (!isReferenced) {
                    CodeIssue issue = new CodeIssue(
                            CodeIssueType.UNUSED_IMPORT,
                            "Unused import: " + reference.getQualifiedName(),
                            importStatement.getTextOffset(),
                            true,
                            "Remove unused import"
                    );
                    
                    issues.add(issue);
                }
            }
            
            // Check for non-final fields that could be final
            for (PsiClass psiClass : javaFile.getClasses()) {
                for (PsiField field : psiClass.getFields()) {
                    // Skip already final fields
                    if (field.hasModifierProperty(PsiModifier.FINAL)) {
                        continue;
                    }
                    
                    // Skip static fields
                    if (field.hasModifierProperty(PsiModifier.STATIC)) {
                        continue;
                    }
                    
                    // Check if field is assigned only once
                    boolean isAssignedOnce = ApplicationManager.getApplication().runReadAction(
                            (Computable<Boolean>) () -> {
                                // Check for assignments in the class
                                PsiReference[] references = com.intellij.psi.search.searches.ReferencesSearch
                                        .search(field, GlobalSearchScope.fileScope(javaFile))
                                        .toArray(PsiReference.EMPTY_ARRAY);
                                
                                int assignmentCount = 0;
                                
                                // Count field declaration assignment
                                if (field.getInitializer() != null) {
                                    assignmentCount++;
                                }
                                
                                // Count assignments in code
                                for (PsiReference reference : references) {
                                    PsiElement element = reference.getElement();
                                    PsiElement parent = element.getParent();
                                    
                                    if (parent instanceof PsiAssignmentExpression) {
                                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) parent;
                                        if (assignment.getLExpression() == element) {
                                            assignmentCount++;
                                        }
                                    }
                                }
                                
                                return assignmentCount == 1;
                            });
                    
                    if (isAssignedOnce) {
                        CodeIssue issue = new CodeIssue(
                                CodeIssueType.FIELD_COULD_BE_FINAL,
                                "Field could be final: " + field.getName(),
                                field.getTextOffset(),
                                true,
                                "Make field final"
                        );
                        
                        issues.add(issue);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error analyzing Java file: " + file.getPath(), e);
        }
        
        // Add more Java-specific checks
        
        return issues;
    }
    
    /**
     * Analyzes a Kotlin file for issues.
     * @param content The file content
     * @return A list of issues
     */
    @NotNull
    private List<CodeIssue> analyzeKotlinFile(@NotNull String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Check for deprecated functions and classes
        Pattern deprecatedPattern = Pattern.compile("@Deprecated[\\s\\S]{0,100}fun\\s+(\\w+)|@Deprecated[\\s\\S]{0,100}class\\s+(\\w+)");
        Matcher deprecatedMatcher = deprecatedPattern.matcher(content);
        
        while (deprecatedMatcher.find()) {
            String name = deprecatedMatcher.group(1) != null ? deprecatedMatcher.group(1) : deprecatedMatcher.group(2);
            String type = deprecatedMatcher.group(1) != null ? "function" : "class";
            
            CodeIssue issue = new CodeIssue(
                    CodeIssueType.USING_DEPRECATED_API,
                    "Using deprecated " + type + ": " + name,
                    deprecatedMatcher.start(),
                    false,
                    "Replace with non-deprecated alternative"
            );
            
            issues.add(issue);
        }
        
        // Check for var that could be val
        Pattern varPattern = Pattern.compile("var\\s+(\\w+)\\s*:\\s*[\\w<>?]+\\s*=\\s*");
        Matcher varMatcher = varPattern.matcher(content);
        
        while (varMatcher.find()) {
            String name = varMatcher.group(1);
            
            // Check if variable is reassigned
            Pattern reassignPattern = Pattern.compile(name + "\\s*=");
            Matcher reassignMatcher = reassignPattern.matcher(content.substring(varMatcher.end()));
            
            if (!reassignMatcher.find()) {
                CodeIssue issue = new CodeIssue(
                        CodeIssueType.VAR_COULD_BE_VAL,
                        "Variable could be val: " + name,
                        varMatcher.start(),
                        true,
                        "Change var to val"
                );
                
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    /**
     * Analyzes a JavaScript/TypeScript file for issues.
     * @param content The file content
     * @return A list of issues
     */
    @NotNull
    private List<CodeIssue> analyzeJsFile(@NotNull String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Check for let that could be const
        Pattern letPattern = Pattern.compile("let\\s+(\\w+)\\s*=\\s*");
        Matcher letMatcher = letPattern.matcher(content);
        
        while (letMatcher.find()) {
            String name = letMatcher.group(1);
            
            // Check if variable is reassigned
            Pattern reassignPattern = Pattern.compile(name + "\\s*=");
            Matcher reassignMatcher = reassignPattern.matcher(content.substring(letMatcher.end()));
            
            if (!reassignMatcher.find()) {
                CodeIssue issue = new CodeIssue(
                        CodeIssueType.LET_COULD_BE_CONST,
                        "Variable could be const: " + name,
                        letMatcher.start(),
                        true,
                        "Change let to const"
                );
                
                issues.add(issue);
            }
        }
        
        // Check for console.log statements
        Pattern consoleLogPattern = Pattern.compile("console\\.log\\(");
        Matcher consoleLogMatcher = consoleLogPattern.matcher(content);
        
        while (consoleLogMatcher.find()) {
            CodeIssue issue = new CodeIssue(
                    CodeIssueType.CONSOLE_LOG,
                    "Console.log statement should be removed in production code",
                    consoleLogMatcher.start(),
                    true,
                    "Remove console.log statement"
            );
            
            issues.add(issue);
        }
        
        return issues;
    }
    
    /**
     * Analyzes a generic file for issues.
     * @param content The file content
     * @return A list of issues
     */
    @NotNull
    private List<CodeIssue> analyzeGenericFile(@NotNull String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Check for TODO comments
        Pattern todoPattern = Pattern.compile("//\\s*TODO[:\\s]|/\\*\\s*TODO[:\\s]|#\\s*TODO[:\\s]");
        Matcher todoMatcher = todoPattern.matcher(content);
        
        while (todoMatcher.find()) {
            CodeIssue issue = new CodeIssue(
                    CodeIssueType.TODO_COMMENT,
                    "TODO comment found",
                    todoMatcher.start(),
                    false,
                    "Implement TODO item"
            );
            
            issues.add(issue);
        }
        
        // Check for long lines
        String[] lines = content.split("\n");
        int position = 0;
        
        for (String line : lines) {
            if (line.length() > 120) {
                CodeIssue issue = new CodeIssue(
                        CodeIssueType.LINE_TOO_LONG,
                        "Line is too long (" + line.length() + " characters)",
                        position,
                        false,
                        "Break line into multiple lines"
                );
                
                issues.add(issue);
            }
            
            position += line.length() + 1; // +1 for the newline
        }
        
        return issues;
    }
    
    /**
     * Gets a relevant code snippet around a position.
     * @param content The content
     * @param position The position
     * @param contextLines The number of context lines
     * @return The code snippet
     */
    @NotNull
    private String getRelevantCodeSnippet(@NotNull String content, int position, int contextLines) {
        try {
            // Find start of the current line
            int start = position;
            while (start > 0 && content.charAt(start - 1) != '\n') {
                start--;
            }
            
            // Find end of the current line
            int end = position;
            while (end < content.length() && content.charAt(end) != '\n') {
                end++;
            }
            
            // Expand to include context lines
            int contextStart = start;
            int lineCount = 0;
            
            // Go back contextLines lines
            while (contextStart > 0 && lineCount < contextLines) {
                contextStart--;
                if (content.charAt(contextStart) == '\n') {
                    lineCount++;
                }
                
                if (contextStart == 0) {
                    break;
                }
            }
            
            if (contextStart > 0) {
                contextStart++; // Skip the last newline
            }
            
            int contextEnd = end;
            lineCount = 0;
            
            // Go forward contextLines lines
            while (contextEnd < content.length() && lineCount < contextLines) {
                if (content.charAt(contextEnd) == '\n') {
                    lineCount++;
                }
                contextEnd++;
                
                if (contextEnd == content.length()) {
                    break;
                }
            }
            
            return content.substring(contextStart, contextEnd);
        } catch (Exception e) {
            LOG.error("Error getting relevant code snippet", e);
            
            // Return a small snippet around the position as fallback
            int start = Math.max(0, position - 100);
            int end = Math.min(content.length(), position + 100);
            
            return content.substring(start, end);
        }
    }
    
    /**
     * Updates a document with new content.
     * @param document The document to update
     * @param newContent The new content
     */
    private void updateDocument(@NotNull Document document, @NotNull String newContent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> ApplicationManager.getApplication().runWriteAction(() -> {
                        document.setText(newContent);
                    }),
                    "Fix Code Issues",
                    null
            );
        });
    }
    
    /**
     * Applies a fix to content.
     * @param content The content to fix
     * @param issue The issue with the fix
     * @return The fixed content
     */
    @NotNull
    private String applyFix(@NotNull String content, @NotNull CodeIssue issue) {
        try {
            if (!issue.hasFix()) {
                return content;
            }
            
            switch (issue.getType()) {
                case UNUSED_IMPORT:
                    // Remove the entire line
                    int lineStart = content.lastIndexOf('\n', issue.getPosition()) + 1;
                    int lineEnd = content.indexOf('\n', issue.getPosition());
                    if (lineEnd == -1) {
                        lineEnd = content.length();
                    }
                    
                    return content.substring(0, lineStart) + content.substring(lineEnd);
                    
                case FIELD_COULD_BE_FINAL:
                    // Add final modifier
                    return addFinalModifier(content, issue.getPosition());
                    
                case VAR_COULD_BE_VAL:
                    // Change var to val
                    return content.substring(0, issue.getPosition()) + 
                            content.substring(issue.getPosition()).replaceFirst("var", "val");
                    
                case LET_COULD_BE_CONST:
                    // Change let to const
                    return content.substring(0, issue.getPosition()) + 
                            content.substring(issue.getPosition()).replaceFirst("let", "const");
                    
                case CONSOLE_LOG:
                    // Remove the entire console.log statement
                    return removeConsoleLog(content, issue.getPosition());
                    
                case MISSING_OVERRIDE:
                    // Add @Override annotation
                    return addOverrideAnnotation(content, issue.getPosition());
                    
                default:
                    return content;
            }
        } catch (Exception e) {
            LOG.error("Error applying fix", e);
            return content;
        }
    }
    
    /**
     * Adds a final modifier to a field.
     * @param content The content
     * @param position The position
     * @return The updated content
     */
    @NotNull
    private String addFinalModifier(@NotNull String content, int position) {
        // Find the field declaration
        int start = position;
        while (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            start--;
        }
        
        // Find modifiers
        while (start > 0 && Character.isWhitespace(content.charAt(start - 1))) {
            start--;
        }
        
        // Check for existing modifiers
        String modifiers = "";
        while (start > 0 && !Character.isWhitespace(content.charAt(start - 1)) && 
                content.charAt(start - 1) != ';' && content.charAt(start - 1) != '{' && 
                content.charAt(start - 1) != '}') {
            start--;
            
            int tempStart = start;
            while (tempStart > 0 && !Character.isWhitespace(content.charAt(tempStart - 1)) && 
                    content.charAt(tempStart - 1) != ';' && content.charAt(tempStart - 1) != '{' && 
                    content.charAt(tempStart - 1) != '}') {
                tempStart--;
            }
            
            modifiers = content.substring(tempStart, start) + " " + modifiers;
        }
        
        // Check if final is already present
        if (modifiers.contains("final")) {
            return content;
        }
        
        // Add final modifier
        String newModifiers = modifiers + "final ";
        
        return content.substring(0, start) + newModifiers + content.substring(start + modifiers.length());
    }
    
    /**
     * Removes a console.log statement.
     * @param content The content
     * @param position The position
     * @return The updated content
     */
    @NotNull
    private String removeConsoleLog(@NotNull String content, int position) {
        // Find the start of the statement
        int start = position;
        while (start > 0 && content.charAt(start - 1) != ';' && content.charAt(start - 1) != '{' && 
                content.charAt(start - 1) != '}' && content.charAt(start - 1) != '\n') {
            start--;
        }
        
        // Find the end of the statement
        int end = position;
        while (end < content.length()) {
            if (content.charAt(end) == ';') {
                end++;
                break;
            } else if (content.charAt(end) == '\n') {
                break;
            }
            end++;
        }
        
        // Remove any leading whitespace
        while (start > 0 && Character.isWhitespace(content.charAt(start - 1))) {
            start--;
        }
        
        return content.substring(0, start) + content.substring(end);
    }
    
    /**
     * Adds an @Override annotation.
     * @param content The content
     * @param position The position
     * @return The updated content
     */
    @NotNull
    private String addOverrideAnnotation(@NotNull String content, int position) {
        // Find the start of the method declaration
        int start = position;
        
        // Find the beginning of the line
        while (start > 0 && content.charAt(start - 1) != '\n') {
            start--;
        }
        
        // Get indentation
        int indentEnd = start;
        while (indentEnd < content.length() && Character.isWhitespace(content.charAt(indentEnd)) && 
                content.charAt(indentEnd) != '\n') {
            indentEnd++;
        }
        
        String indentation = content.substring(start, indentEnd);
        
        // Add @Override annotation
        return content.substring(0, start) + indentation + "@Override\n" + content.substring(start);
    }
    
    /**
     * Cleans up an AI response by removing code fences and extra whitespace.
     * @param response The AI response
     * @return The cleaned up response
     */
    @NotNull
    private String cleanupAiResponse(@NotNull String response) {
        // Remove code fences
        String cleaned = response.replaceAll("```(?:java|kotlin|javascript|js|typescript|ts)?", "");
        
        // Trim whitespace
        cleaned = cleaned.trim();
        
        return cleaned;
    }
    
    /**
     * Code issue type.
     */
    public enum CodeIssueType {
        UNUSED_IMPORT,
        MISSING_OVERRIDE,
        FIELD_COULD_BE_FINAL,
        VAR_COULD_BE_VAL,
        LET_COULD_BE_CONST,
        CONSOLE_LOG,
        TODO_COMMENT,
        LINE_TOO_LONG,
        USING_DEPRECATED_API
    }
    
    /**
     * Code issue.
     */
    public static class CodeIssue {
        private final CodeIssueType type;
        private final String description;
        private final int position;
        private final boolean hasFix;
        private final String fixDescription;
        
        /**
         * Creates a new CodeIssue.
         * @param type The issue type
         * @param description The issue description
         * @param position The issue position
         * @param hasFix Whether the issue has a fix
         * @param fixDescription The fix description
         */
        public CodeIssue(@NotNull CodeIssueType type, @NotNull String description, int position, 
                        boolean hasFix, @NotNull String fixDescription) {
            this.type = type;
            this.description = description;
            this.position = position;
            this.hasFix = hasFix;
            this.fixDescription = fixDescription;
        }
        
        /**
         * Gets the issue type.
         * @return The issue type
         */
        @NotNull
        public CodeIssueType getType() {
            return type;
        }
        
        /**
         * Gets the issue description.
         * @return The issue description
         */
        @NotNull
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the issue position.
         * @return The issue position
         */
        public int getPosition() {
            return position;
        }
        
        /**
         * Gets whether the issue has a fix.
         * @return Whether the issue has a fix
         */
        public boolean hasFix() {
            return hasFix;
        }
        
        /**
         * Gets the fix description.
         * @return The fix description
         */
        @NotNull
        public String getFixDescription() {
            return fixDescription;
        }
    }
}