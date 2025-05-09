package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for autonomous code generation.
 * This service provides capabilities for detecting missing code elements
 * and automatically generating appropriate implementations.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    
    // Patterns for detecting missing implementations
    private static final Pattern MISSING_METHOD_PATTERN = Pattern.compile(
            "(?i)(?:method|function)\\s+['\"](\\w+)['\"]\\s+(?:is|must be)\\s+(?:implemented|defined|overridden)");
    
    private static final Pattern UNUSED_IMPORT_PATTERN = Pattern.compile(
            "(?i)unused\\s+import\\s*:?\\s*['\"](\\S+)['\"]");
    
    private static final Pattern MISSING_OVERRIDE_PATTERN = Pattern.compile(
            "(?i)(?:method|function)\\s+['\"](\\w+)['\"]\\s+(?:should have|requires|must have)\\s+@Override");
    
    /**
     * Creates a new AutonomousCodeGenerationService.
     * @param project The project
     */
    public AutonomousCodeGenerationService(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = AIServiceManager.getInstance(project);
        
        LOG.info("AutonomousCodeGenerationService initialized");
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
     * Analyzes a file for missing code elements.
     * @param file The file to analyze
     * @return A list of issues found
     */
    public CompletableFuture<List<CodeIssue>> analyzeFile(@NotNull VirtualFile file) {
        LOG.info("Analyzing file: " + file.getPath());
        
        return CompletableFuture.supplyAsync(() -> {
            List<CodeIssue> issues = new ArrayList<>();
            
            try {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for: " + file.getPath());
                    return issues;
                }
                
                // Analyze Java files
                if (psiFile instanceof PsiJavaFile) {
                    PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                    
                    // Analyze classes
                    for (PsiClass psiClass : javaFile.getClasses()) {
                        issues.addAll(analyzeClass(psiClass));
                    }
                    
                    // Analyze imports
                    for (PsiImportStatement importStatement : javaFile.getImportList().getImportStatements()) {
                        if (importStatement.isOnDemand()) {
                            continue; // Skip wildcard imports
                        }
                        
                        if (!isImportUsed(importStatement, javaFile)) {
                            CodeIssue issue = new CodeIssue(
                                    CodeIssueType.UNUSED_IMPORT,
                                    importStatement.getTextRange().getStartOffset(),
                                    "Unused import: " + importStatement.getQualifiedName(),
                                    importStatement
                            );
                            issues.add(issue);
                        }
                    }
                }
                
                return issues;
            } catch (Exception e) {
                LOG.error("Error analyzing file: " + file.getPath(), e);
                return issues;
            }
        });
    }
    
    /**
     * Analyzes a class for missing code elements.
     * @param psiClass The class to analyze
     * @return A list of issues found
     */
    private List<CodeIssue> analyzeClass(@NotNull PsiClass psiClass) {
        List<CodeIssue> issues = new ArrayList<>();
        
        try {
            // Check for unimplemented methods in implemented interfaces
            for (PsiClass anInterface : psiClass.getInterfaces()) {
                for (PsiMethod interfaceMethod : anInterface.getMethods()) {
                    if (!hasImplementation(psiClass, interfaceMethod)) {
                        CodeIssue issue = new CodeIssue(
                                CodeIssueType.MISSING_METHOD,
                                psiClass.getTextRange().getEndOffset() - 1,
                                "Missing implementation of method: " + interfaceMethod.getName(),
                                interfaceMethod
                        );
                        issues.add(issue);
                    }
                }
            }
            
            // Check for missing @Override annotations
            for (PsiMethod method : psiClass.getMethods()) {
                if (isOverridingMethod(method) && !hasOverrideAnnotation(method)) {
                    CodeIssue issue = new CodeIssue(
                            CodeIssueType.MISSING_OVERRIDE,
                            method.getTextRange().getStartOffset(),
                            "Missing @Override annotation on method: " + method.getName(),
                            method
                    );
                    issues.add(issue);
                }
            }
            
            // Check for missing implementations of abstract methods
            if (psiClass.getSuperClass() != null && psiClass.getSuperClass().isValid()) {
                for (PsiMethod superMethod : psiClass.getSuperClass().getMethods()) {
                    if (superMethod.hasModifierProperty(PsiModifier.ABSTRACT) && 
                            !hasImplementation(psiClass, superMethod)) {
                        CodeIssue issue = new CodeIssue(
                                CodeIssueType.MISSING_METHOD,
                                psiClass.getTextRange().getEndOffset() - 1,
                                "Missing implementation of abstract method: " + superMethod.getName(),
                                superMethod
                        );
                        issues.add(issue);
                    }
                }
            }
            
            // Recursively analyze inner classes
            for (PsiClass innerClass : psiClass.getInnerClasses()) {
                issues.addAll(analyzeClass(innerClass));
            }
        } catch (Exception e) {
            LOG.error("Error analyzing class: " + psiClass.getName(), e);
        }
        
        return issues;
    }
    
    /**
     * Checks if a class has an implementation of a method.
     * @param psiClass The class to check
     * @param methodToCheck The method to check for
     * @return Whether the class has an implementation of the method
     */
    private boolean hasImplementation(@NotNull PsiClass psiClass, @NotNull PsiMethod methodToCheck) {
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.getName().equals(methodToCheck.getName()) && 
                    method.getParameterList().getParametersCount() == methodToCheck.getParameterList().getParametersCount()) {
                
                boolean parametersMatch = true;
                PsiParameter[] params1 = method.getParameterList().getParameters();
                PsiParameter[] params2 = methodToCheck.getParameterList().getParameters();
                
                for (int i = 0; i < params1.length; i++) {
                    if (!params1[i].getType().equals(params2[i].getType())) {
                        parametersMatch = false;
                        break;
                    }
                }
                
                if (parametersMatch) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a method is overriding a method from a superclass or interface.
     * @param method The method to check
     * @return Whether the method is overriding a method
     */
    private boolean isOverridingMethod(@NotNull PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        
        // Check superclass
        PsiClass superClass = containingClass.getSuperClass();
        if (superClass != null) {
            for (PsiMethod superMethod : superClass.getMethods()) {
                if (isOverriding(method, superMethod)) {
                    return true;
                }
            }
        }
        
        // Check interfaces
        for (PsiClass anInterface : containingClass.getInterfaces()) {
            for (PsiMethod interfaceMethod : anInterface.getMethods()) {
                if (isOverriding(method, interfaceMethod)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a method is overriding another method.
     * @param method The method to check
     * @param potentiallyOverriddenMethod The potentially overridden method
     * @return Whether method is overriding potentiallyOverriddenMethod
     */
    private boolean isOverriding(@NotNull PsiMethod method, @NotNull PsiMethod potentiallyOverriddenMethod) {
        if (!method.getName().equals(potentiallyOverriddenMethod.getName())) {
            return false;
        }
        
        if (method.getParameterList().getParametersCount() != 
                potentiallyOverriddenMethod.getParameterList().getParametersCount()) {
            return false;
        }
        
        PsiParameter[] params1 = method.getParameterList().getParameters();
        PsiParameter[] params2 = potentiallyOverriddenMethod.getParameterList().getParameters();
        
        for (int i = 0; i < params1.length; i++) {
            if (!params1[i].getType().equals(params2[i].getType())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a method has an @Override annotation.
     * @param method The method to check
     * @return Whether the method has an @Override annotation
     */
    private boolean hasOverrideAnnotation(@NotNull PsiMethod method) {
        for (PsiAnnotation annotation : method.getAnnotations()) {
            if (annotation.getQualifiedName() != null && 
                    annotation.getQualifiedName().equals("java.lang.Override")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if an import is used in a file.
     * @param importStatement The import statement to check
     * @param javaFile The Java file to check
     * @return Whether the import is used
     */
    private boolean isImportUsed(@NotNull PsiImportStatement importStatement, @NotNull PsiJavaFile javaFile) {
        String importName = importStatement.getQualifiedName();
        if (importName == null) {
            return true; // Can't determine if it's used
        }
        
        String shortName = importName.substring(importName.lastIndexOf('.') + 1);
        
        // Check if short name is used in the file
        return isShortNameUsed(shortName, javaFile);
    }
    
    /**
     * Checks if a short name is used in a file.
     * @param shortName The short name to check
     * @param javaFile The Java file to check
     * @return Whether the short name is used
     */
    private boolean isShortNameUsed(@NotNull String shortName, @NotNull PsiJavaFile javaFile) {
        // Check if the short name is used in the file text
        String fileText = javaFile.getText();
        
        // Simple heuristic: check if the short name appears outside of import statements
        int importsEndOffset = javaFile.getImportList().getTextRange().getEndOffset();
        String codeAfterImports = fileText.substring(importsEndOffset);
        
        // Look for the short name as a word boundary
        Pattern pattern = Pattern.compile("\\b" + shortName + "\\b");
        Matcher matcher = pattern.matcher(codeAfterImports);
        
        return matcher.find();
    }
    
    /**
     * Gets a description of a code element.
     * @param element The code element
     * @return A description of the code element
     */
    @NotNull
    private String getElementDescription(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            StringBuilder desc = new StringBuilder();
            
            // Add modifiers
            PsiModifierList modifierList = method.getModifierList();
            for (String modifier : new String[] {
                    PsiModifier.PUBLIC, PsiModifier.PROTECTED, PsiModifier.PRIVATE,
                    PsiModifier.STATIC, PsiModifier.ABSTRACT, PsiModifier.FINAL
            }) {
                if (modifierList.hasModifierProperty(modifier)) {
                    desc.append(modifier).append(" ");
                }
            }
            
            // Add return type
            PsiType returnType = method.getReturnType();
            if (returnType != null) {
                desc.append(returnType.getPresentableText()).append(" ");
            }
            
            // Add name
            desc.append(method.getName());
            
            // Add parameters
            desc.append("(");
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                PsiParameter parameter = parameters[i];
                desc.append(parameter.getType().getPresentableText()).append(" ").append(parameter.getName());
                if (i < parameters.length - 1) {
                    desc.append(", ");
                }
            }
            desc.append(")");
            
            // Add exceptions
            PsiClassType[] exceptions = method.getThrowsList().getReferencedTypes();
            if (exceptions.length > 0) {
                desc.append(" throws ");
                for (int i = 0; i < exceptions.length; i++) {
                    desc.append(exceptions[i].getPresentableText());
                    if (i < exceptions.length - 1) {
                        desc.append(", ");
                    }
                }
            }
            
            return desc.toString();
        } else if (element instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) element;
            return psiClass.getQualifiedName();
        } else if (element instanceof PsiField) {
            PsiField field = (PsiField) element;
            return field.getType().getPresentableText() + " " + field.getName();
        } else {
            return element.getText();
        }
    }
    
    /**
     * Fixes issues in a file.
     * @param file The file to fix
     * @param issues The issues to fix
     * @return The number of issues fixed
     */
    public CompletableFuture<Integer> fixIssues(@NotNull VirtualFile file, @NotNull List<CodeIssue> issues) {
        LOG.info("Fixing " + issues.size() + " issues in file: " + file.getPath());
        
        return CompletableFuture.supplyAsync(() -> {
            if (issues.isEmpty()) {
                return 0;
            }
            
            int fixedCount = 0;
            
            try {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getPath());
                    return 0;
                }
                
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for: " + file.getPath());
                    return 0;
                }
                
                for (CodeIssue issue : issues) {
                    try {
                        boolean fixed = fixIssue(issue, psiFile, document);
                        if (fixed) {
                            fixedCount++;
                        }
                    } catch (Exception e) {
                        LOG.error("Error fixing issue: " + issue.getMessage(), e);
                    }
                }
                
                return fixedCount;
            } catch (Exception e) {
                LOG.error("Error fixing issues in file: " + file.getPath(), e);
                return 0;
            }
        });
    }
    
    /**
     * Fixes a single issue.
     * @param issue The issue to fix
     * @param psiFile The PSI file
     * @param document The document
     * @return Whether the issue was fixed
     */
    private boolean fixIssue(@NotNull CodeIssue issue, @NotNull PsiFile psiFile, @NotNull Document document) {
        switch (issue.getType()) {
            case UNUSED_IMPORT:
                return fixUnusedImport(issue, psiFile, document);
                
            case MISSING_METHOD:
                return fixMissingMethod(issue, psiFile, document);
                
            case MISSING_OVERRIDE:
                return fixMissingOverride(issue, psiFile, document);
                
            default:
                LOG.warn("Unknown issue type: " + issue.getType());
                return false;
        }
    }
    
    /**
     * Fixes an unused import issue.
     * @param issue The issue to fix
     * @param psiFile The PSI file
     * @param document The document
     * @return Whether the issue was fixed
     */
    private boolean fixUnusedImport(@NotNull CodeIssue issue, @NotNull PsiFile psiFile, @NotNull Document document) {
        if (!(issue.getElement() instanceof PsiImportStatement)) {
            return false;
        }
        
        PsiImportStatement importStatement = (PsiImportStatement) issue.getElement();
        
        final int[] startOffset = {importStatement.getTextRange().getStartOffset()};
        final int[] endOffset = {importStatement.getTextRange().getEndOffset()};
        
        // Find the next newline to remove it as well
        String text = document.getText();
        if (endOffset[0] < text.length() && text.charAt(endOffset[0]) == '\n') {
            endOffset[0]++;
        }
        
        Runnable runnable = () -> {
            try {
                document.deleteString(startOffset[0], endOffset[0]);
            } catch (Exception e) {
                LOG.error("Error removing unused import", e);
            }
        };
        
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }, "Remove Unused Import", null);
        });
        
        return true;
    }
    
    /**
     * Fixes a missing method issue.
     * @param issue The issue to fix
     * @param psiFile The PSI file
     * @param document The document
     * @return Whether the issue was fixed
     */
    private boolean fixMissingMethod(@NotNull CodeIssue issue, @NotNull PsiFile psiFile, @NotNull Document document) {
        if (!(issue.getElement() instanceof PsiMethod)) {
            return false;
        }
        
        PsiMethod methodToImplement = (PsiMethod) issue.getElement();
        
        // Find the containing class at the issue offset
        PsiElement elementAtOffset = psiFile.findElementAt(issue.getOffset());
        if (elementAtOffset == null) {
            return false;
        }
        
        PsiClass containingClass = PsiTreeUtil.getParentOfType(elementAtOffset, PsiClass.class);
        if (containingClass == null) {
            return false;
        }
        
        // Generate a method implementation
        String methodDescription = getElementDescription(methodToImplement);
        String classContext = containingClass.getText();
        
        // Build a prompt for the AI service
        String prompt = "Implement the following method for a Java class:\n\n" +
                "Method signature: " + methodDescription + "\n\n" +
                "Class context:\n```java\n" + classContext + "\n```\n\n" +
                "Please provide a complete method implementation with appropriate Javadoc comments.";
        
        try {
            // Get method implementation from AI service
            String implementation = aiServiceManager.generateCode(prompt, "java", null)
                    .get(10, TimeUnit.SECONDS);
            
            if (implementation == null || implementation.trim().isEmpty()) {
                LOG.warn("Failed to generate method implementation");
                return false;
            }
            
            // Clean up the implementation (remove any extra class declarations, etc.)
            implementation = cleanMethodImplementation(implementation);
            
            // Insert the implementation at the right position
            final int[] insertPosition = {issue.getOffset()};
            final String finalImplementation = implementation;
            
            Runnable runnable = () -> {
                try {
                    // Add a newline if needed
                    if (!document.getText(
                            new com.intellij.openapi.util.TextRange(insertPosition[0] - 1, insertPosition[0])
                    ).equals("\n")) {
                        document.insertString(insertPosition[0], "\n");
                        insertPosition[0]++;
                    }
                    
                    document.insertString(insertPosition[0], finalImplementation);
                } catch (Exception e) {
                    LOG.error("Error inserting method implementation", e);
                }
            };
            
            ApplicationManager.getApplication().invokeLater(() -> {
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    ApplicationManager.getApplication().runWriteAction(runnable);
                }, "Add Method Implementation", null);
            });
            
            return true;
        } catch (Exception e) {
            LOG.error("Error generating method implementation", e);
            return false;
        }
    }
    
    /**
     * Fixes a missing @Override annotation issue.
     * @param issue The issue to fix
     * @param psiFile The PSI file
     * @param document The document
     * @return Whether the issue was fixed
     */
    private boolean fixMissingOverride(@NotNull CodeIssue issue, @NotNull PsiFile psiFile, @NotNull Document document) {
        if (!(issue.getElement() instanceof PsiMethod)) {
            return false;
        }
        
        PsiMethod method = (PsiMethod) issue.getElement();
        
        final int[] insertPosition = {method.getTextRange().getStartOffset()};
        
        Runnable runnable = () -> {
            try {
                document.insertString(insertPosition[0], "@Override\n");
            } catch (Exception e) {
                LOG.error("Error adding @Override annotation", e);
            }
        };
        
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }, "Add @Override Annotation", null);
        });
        
        return true;
    }
    
    /**
     * Cleans a method implementation.
     * @param implementation The method implementation to clean
     * @return The cleaned method implementation
     */
    @NotNull
    private String cleanMethodImplementation(@NotNull String implementation) {
        // Extract just the method declaration and body
        // This is a simple implementation and might need enhancement for real-world use
        
        // Remove any class declarations
        implementation = implementation.replaceAll("(?s)\\bclass\\s+\\w+\\s*\\{", "");
        
        // Remove any interface declarations
        implementation = implementation.replaceAll("(?s)\\binterface\\s+\\w+\\s*\\{", "");
        
        // Remove any closing braces at the end that might be from class declarations
        implementation = implementation.replaceAll("\\}\\s*$", "");
        
        // Remove any "public", "protected", or "private" keywords from the beginning
        // of the implementation if there are multiple (we only want one)
        implementation = implementation.replaceAll("(public|protected|private)\\s+(public|protected|private)", "$1");
        
        // Ensure the implementation ends with a newline
        if (!implementation.endsWith("\n")) {
            implementation += "\n";
        }
        
        return implementation.trim();
    }
    
    /**
     * Performs autonomous code generation based on compiler error messages.
     * @param file The file to fix
     * @param errorMessage The compiler error message
     * @return Whether any issues were fixed
     */
    public CompletableFuture<Boolean> fixCompilerError(@NotNull VirtualFile file, @NotNull String errorMessage) {
        LOG.info("Fixing compiler error in file: " + file.getPath());
        LOG.info("Error message: " + errorMessage);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) {
                    LOG.warn("Could not get document for file: " + file.getPath());
                    return false;
                }
                
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for: " + file.getPath());
                    return false;
                }
                
                List<CodeIssue> issues = new ArrayList<>();
                
                // Check for missing methods
                Matcher missingMethodMatcher = MISSING_METHOD_PATTERN.matcher(errorMessage);
                while (missingMethodMatcher.find()) {
                    String methodName = missingMethodMatcher.group(1);
                    LOG.info("Detected missing method: " + methodName);
                    
                    // Find the class that needs the method
                    if (psiFile instanceof PsiJavaFile) {
                        PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
                        if (classes.length > 0) {
                            PsiClass targetClass = classes[0];
                            
                            // Create a code issue for the missing method
                            CodeIssue issue = new CodeIssue(
                                    CodeIssueType.MISSING_METHOD,
                                    targetClass.getTextRange().getEndOffset() - 1,
                                    "Missing method: " + methodName,
                                    createPlaceholderMethod(methodName)
                            );
                            issues.add(issue);
                        }
                    }
                }
                
                // Check for missing @Override annotations
                Matcher missingOverrideMatcher = MISSING_OVERRIDE_PATTERN.matcher(errorMessage);
                while (missingOverrideMatcher.find()) {
                    String methodName = missingOverrideMatcher.group(1);
                    LOG.info("Detected missing @Override annotation: " + methodName);
                    
                    // Find the method that needs the annotation
                    if (psiFile instanceof PsiJavaFile) {
                        PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
                        for (PsiClass psiClass : classes) {
                            for (PsiMethod method : psiClass.getMethods()) {
                                if (method.getName().equals(methodName) && !hasOverrideAnnotation(method)) {
                                    // Create a code issue for the missing annotation
                                    CodeIssue issue = new CodeIssue(
                                            CodeIssueType.MISSING_OVERRIDE,
                                            method.getTextRange().getStartOffset(),
                                            "Missing @Override annotation on method: " + methodName,
                                            method
                                    );
                                    issues.add(issue);
                                }
                            }
                        }
                    }
                }
                
                // Check for unused imports
                Matcher unusedImportMatcher = UNUSED_IMPORT_PATTERN.matcher(errorMessage);
                while (unusedImportMatcher.find()) {
                    String importName = unusedImportMatcher.group(1);
                    LOG.info("Detected unused import: " + importName);
                    
                    // Find the import statement
                    if (psiFile instanceof PsiJavaFile) {
                        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                        for (PsiImportStatement importStatement : javaFile.getImportList().getImportStatements()) {
                            String qualifiedName = importStatement.getQualifiedName();
                            if (qualifiedName != null && qualifiedName.equals(importName)) {
                                // Create a code issue for the unused import
                                CodeIssue issue = new CodeIssue(
                                        CodeIssueType.UNUSED_IMPORT,
                                        importStatement.getTextRange().getStartOffset(),
                                        "Unused import: " + importName,
                                        importStatement
                                );
                                issues.add(issue);
                            }
                        }
                    }
                }
                
                if (issues.isEmpty()) {
                    // No specific issues found, try general AI-based fixing
                    return fixWithGeneralAI(file, errorMessage);
                } else {
                    // Fix the specific issues found
                    int fixedCount = fixIssues(file, issues).get(10, TimeUnit.SECONDS);
                    return fixedCount > 0;
                }
            } catch (Exception e) {
                LOG.error("Error fixing compiler error", e);
                return false;
            }
        });
    }
    
    /**
     * Creates a placeholder method for code generation.
     * @param methodName The method name
     * @return A placeholder method
     */
    @NotNull
    private PsiMethod createPlaceholderMethod(@NotNull String methodName) {
        try {
            PsiElementFactory factory = PsiElementFactory.getInstance(project);
            return factory.createMethodFromText("public void " + methodName + "() {}", null);
        } catch (IncorrectOperationException e) {
            LOG.error("Error creating placeholder method", e);
            throw new RuntimeException("Error creating placeholder method", e);
        }
    }
    
    /**
     * Fixes a file with general AI-based error resolution.
     * @param file The file to fix
     * @param errorMessage The error message
     * @return Whether the file was fixed
     */
    private boolean fixWithGeneralAI(@NotNull VirtualFile file, @NotNull String errorMessage) {
        try {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                LOG.warn("Could not get document for file: " + file.getPath());
                return false;
            }
            
            // Get the current code
            String code = document.getText();
            
            // Use the AI service to fix the code
            String fixedCode = aiServiceManager.fixCode(code, errorMessage, null)
                    .get(30, TimeUnit.SECONDS);
            
            if (fixedCode == null || fixedCode.trim().isEmpty() || fixedCode.equals(code)) {
                LOG.warn("AI service could not fix the code or returned the same code");
                return false;
            }
            
            // Apply the fix
            final String finalFixedCode = fixedCode;
            
            Runnable runnable = () -> {
                try {
                    document.setText(finalFixedCode);
                } catch (Exception e) {
                    LOG.error("Error applying fixed code", e);
                }
            };
            
            ApplicationManager.getApplication().invokeLater(() -> {
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    ApplicationManager.getApplication().runWriteAction(runnable);
                }, "Fix Compiler Error", null);
            });
            
            return true;
        } catch (Exception e) {
            LOG.error("Error fixing with general AI", e);
            return false;
        }
    }
    
    /**
     * Code issue type.
     */
    public enum CodeIssueType {
        UNUSED_IMPORT,
        MISSING_METHOD,
        MISSING_OVERRIDE
    }
    
    /**
     * Code issue.
     */
    public static class CodeIssue {
        private final CodeIssueType type;
        private final int offset;
        private final String message;
        private final PsiElement element;
        
        /**
         * Creates a new CodeIssue.
         * @param type The issue type
         * @param offset The offset
         * @param message The message
         * @param element The element
         */
        public CodeIssue(@NotNull CodeIssueType type, int offset, @NotNull String message, 
                @Nullable PsiElement element) {
            this.type = type;
            this.offset = offset;
            this.message = message;
            this.element = element;
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
         * Gets the offset.
         * @return The offset
         */
        public int getOffset() {
            return offset;
        }
        
        /**
         * Gets the message.
         * @return The message
         */
        @NotNull
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the element.
         * @return The element
         */
        @Nullable
        public PsiElement getElement() {
            return element;
        }
    }
}