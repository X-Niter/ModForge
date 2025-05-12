package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.services.CodeAnalysisService.AnalysisResult;
import com.modforge.intellij.plugin.services.CodeAnalysisService.ClassInfo;
import com.modforge.intellij.plugin.services.CodeAnalysisService.MethodInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Service for automated code refactoring and improvement.
 * This service analyzes code and suggests improvements based on patterns
 * and best practices.
 */
@Service(Service.Level.PROJECT)
public final class AutomatedRefactoringService {
    private static final Logger LOG = Logger.getInstance(AutomatedRefactoringService.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    private final CodeAnalysisService codeAnalysisService;
    
    /**
     * Creates a new AutomatedRefactoringService.
     * @param project The project
     */
    public AutomatedRefactoringService(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = project.getService(AIServiceManager.class);
        this.codeAnalysisService = project.getService(CodeAnalysisService.class);
        
        LOG.info("AutomatedRefactoringService initialized for project: " + project.getName());
    }
    
    /**
     * Gets the AutomatedRefactoringService instance.
     * @param project The project
     * @return The AutomatedRefactoringService instance
     */
    public static AutomatedRefactoringService getInstance(@NotNull Project project) {
        return project.getService(AutomatedRefactoringService.class);
    }
    
    /**
     * Analyzes and refactors a file.
     * @param file The file to refactor
     * @return A future that completes with the refactoring result
     */
    @NotNull
    public CompletableFuture<RefactoringResult> refactorFile(@NotNull VirtualFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Refactoring file: " + file.getPath());
                
                // Analyze file
                AnalysisResult analysisResult = codeAnalysisService.analyzeFile(file).get(30, TimeUnit.SECONDS);
                
                RefactoringResult result = new RefactoringResult();
                
                // Get PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for: " + file.getPath());
                    return result;
                }
                
                // Check if file is a Java file
                if (!(psiFile instanceof PsiJavaFile)) {
                    LOG.info("File is not a Java file: " + file.getPath());
                    return result;
                }
                
                PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                
                // Detect potential improvements
                List<RefactoringAction> actions = detectRefactoringActions(javaFile, analysisResult);
                result.detectedActions.addAll(actions);
                
                // Apply automatic refactorings
                for (RefactoringAction action : actions) {
                    // Skip actions that don't have auto-fix
                    if (!action.hasAutoFix()) {
                        continue;
                    }
                    
                    try {
                        boolean applied = applyRefactoringAction(action);
                        if (applied) {
                            result.appliedActions.add(action);
                        }
                    } catch (Exception e) {
                        LOG.error("Error applying refactoring action: " + action.getDescription(), e);
                    }
                }
                
                return result;
            } catch (Exception e) {
                LOG.error("Error refactoring file: " + file.getPath(), e);
                return new RefactoringResult();
            }
        });
    }
    
    /**
     * Detects potential refactoring actions.
     * @param javaFile The Java file to analyze
     * @param analysisResult The analysis result
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectRefactoringActions(@NotNull PsiJavaFile javaFile, 
                                                           @NotNull AnalysisResult analysisResult) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        try {
            // Check for unused imports
            actions.addAll(detectUnusedImports(javaFile));
            
            // Check for missing @Override annotations
            actions.addAll(detectMissingOverrideAnnotations(javaFile));
            
            // Check for inconsistent method naming
            actions.addAll(detectInconsistentMethodNaming(javaFile, analysisResult));
            
            // Check for long methods
            actions.addAll(detectLongMethods(javaFile));
            
            // Check for duplicate code
            actions.addAll(detectDuplicateCode(javaFile));
            
            // Check for overly complex methods
            actions.addAll(detectComplexMethods(javaFile));
            
            // Check for potential null pointer exceptions
            actions.addAll(detectPotentialNullPointerExceptions(javaFile));
            
            // Check for missing Javadoc
            actions.addAll(detectMissingJavadoc(javaFile));
        } catch (Exception e) {
            LOG.error("Error detecting refactoring actions", e);
        }
        
        return actions;
    }
    
    /**
     * Detects unused imports.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectUnusedImports(@NotNull PsiJavaFile javaFile) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return actions;
        }
        
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        
        for (PsiImportStatement importStatement : importList.getImportStatements()) {
            PsiJavaCodeReferenceElement reference = importStatement.getImportReference();
            if (reference == null) {
                continue;
            }
            
            boolean isReferenced = ApplicationManager.getApplication().runReadAction(
                    (Computable<Boolean>) () -> {
                        PsiElement target = reference.resolve();
                        return target != null && styleManager.isReferenceUsed(reference, javaFile);
                    });
            
            if (!isReferenced) {
                RefactoringAction action = new RefactoringAction(
                        RefactoringActionType.REMOVE_UNUSED_IMPORT,
                        "Remove unused import: " + reference.getQualifiedName(),
                        importStatement
                );
                
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    /**
     * Detects missing @Override annotations.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectMissingOverrideAnnotations(@NotNull PsiJavaFile javaFile) {
        List<RefactoringAction> actions = new ArrayList<>();
        
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
                    RefactoringAction action = new RefactoringAction(
                            RefactoringActionType.ADD_OVERRIDE_ANNOTATION,
                            "Add @Override annotation to method: " + method.getName(),
                            method
                    );
                    
                    actions.add(action);
                }
            }
        }
        
        return actions;
    }
    
    /**
     * Detects inconsistent method naming.
     * @param javaFile The Java file to analyze
     * @param analysisResult The analysis result
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectInconsistentMethodNaming(@NotNull PsiJavaFile javaFile, 
                                                                 @NotNull AnalysisResult analysisResult) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        // Check each class
        for (ClassInfo classInfo : analysisResult.getClasses()) {
            // Check if method names follow consistent naming convention
            Map<String, List<String>> methodNamePrefixes = new HashMap<>();
            
            for (MethodInfo methodInfo : classInfo.getMethods()) {
                // Skip constructors
                if (methodInfo.isConstructor()) {
                    continue;
                }
                
                // Skip getters and setters
                if (methodInfo.getName().startsWith("get") || 
                        methodInfo.getName().startsWith("set") || 
                        methodInfo.getName().startsWith("is")) {
                    continue;
                }
                
                // Determine method name prefix
                String name = methodInfo.getName();
                String prefix = "";
                
                for (int i = 0; i < name.length(); i++) {
                    char c = name.charAt(i);
                    if (Character.isLowerCase(c)) {
                        prefix += c;
                    } else {
                        break;
                    }
                }
                
                if (!prefix.isEmpty()) {
                    methodNamePrefixes.computeIfAbsent(prefix, k -> new ArrayList<>()).add(name);
                }
            }
            
            // Check for methods with similar functionality but different prefixes
            for (Map.Entry<String, List<String>> entry : methodNamePrefixes.entrySet()) {
                String prefix = entry.getKey();
                List<String> methodNames = entry.getValue();
                
                if (methodNames.size() < 2) {
                    continue;
                }
                
                // Find the class
                PsiClass psiClass = findClass(javaFile, classInfo.getName());
                if (psiClass == null) {
                    continue;
                }
                
                // Find the methods
                PsiMethod[] methods = psiClass.findMethodsByName(methodNames.get(0), false);
                if (methods.length == 0) {
                    continue;
                }
                
                RefactoringAction action = new RefactoringAction(
                        RefactoringActionType.RENAME_METHOD,
                        "Consider consistent naming for methods with prefix '" + prefix + 
                                "': " + String.join(", ", methodNames),
                        methods[0]
                );
                
                // This is a suggestion, so no auto-fix
                action.setHasAutoFix(false);
                
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    /**
     * Detects long methods.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectLongMethods(@NotNull PsiJavaFile javaFile) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        for (PsiClass psiClass : javaFile.getClasses()) {
            for (PsiMethod method : psiClass.getMethods()) {
                // Skip constructors
                if (method.isConstructor()) {
                    continue;
                }
                
                // Check method body
                PsiCodeBlock body = method.getBody();
                if (body == null) {
                    continue;
                }
                
                // Count statements
                PsiStatement[] statements = PsiTreeUtil.getChildrenOfType(body, PsiStatement.class);
                if (statements == null || statements.length < 50) {
                    continue;
                }
                
                RefactoringAction action = new RefactoringAction(
                        RefactoringActionType.EXTRACT_METHOD,
                        "Consider breaking down long method: " + method.getName() + 
                                " (" + statements.length + " statements)",
                        method
                );
                
                // This requires advanced refactoring, so no auto-fix
                action.setHasAutoFix(false);
                
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    /**
     * Detects duplicate code.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectDuplicateCode(@NotNull PsiJavaFile javaFile) {
        // This is a simplified implementation that just checks for identical method bodies
        // A real implementation would use more sophisticated code similarity algorithms
        
        List<RefactoringAction> actions = new ArrayList<>();
        
        for (PsiClass psiClass : javaFile.getClasses()) {
            Map<String, List<PsiMethod>> methodBodies = new HashMap<>();
            
            for (PsiMethod method : psiClass.getMethods()) {
                PsiCodeBlock body = method.getBody();
                if (body == null) {
                    continue;
                }
                
                String bodyText = body.getText();
                methodBodies.computeIfAbsent(bodyText, k -> new ArrayList<>()).add(method);
            }
            
            // Check for methods with identical bodies
            for (Map.Entry<String, List<PsiMethod>> entry : methodBodies.entrySet()) {
                List<PsiMethod> methods = entry.getValue();
                
                if (methods.size() < 2) {
                    continue;
                }
                
                // Create list of method names
                List<String> methodNames = new ArrayList<>(methods.size());
                for (PsiMethod method : methods) {
                    methodNames.add(method.getName());
                }
                
                RefactoringAction action = new RefactoringAction(
                        RefactoringActionType.EXTRACT_DUPLICATE_CODE,
                        "Consider extracting duplicate code in methods: " + 
                                String.join(", ", methodNames),
                        methods.get(0)
                );
                
                // This requires advanced refactoring, so no auto-fix
                action.setHasAutoFix(false);
                
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    /**
     * Detects overly complex methods.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectComplexMethods(@NotNull PsiJavaFile javaFile) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        for (PsiClass psiClass : javaFile.getClasses()) {
            for (PsiMethod method : psiClass.getMethods()) {
                PsiCodeBlock body = method.getBody();
                if (body == null) {
                    continue;
                }
                
                // Count conditional statements and loops
                int complexity = 0;
                
                // Add if statements
                PsiElement[] ifStatements = PsiTreeUtil.findChildrenOfType(
                        body, PsiIfStatement.class).toArray(PsiElement.EMPTY_ARRAY);
                complexity += ifStatements.length;
                
                // Add for loops
                PsiElement[] forStatements = PsiTreeUtil.findChildrenOfType(
                        body, PsiForStatement.class).toArray(PsiElement.EMPTY_ARRAY);
                complexity += forStatements.length;
                
                // Add while loops
                PsiElement[] whileStatements = PsiTreeUtil.findChildrenOfType(
                        body, PsiWhileStatement.class).toArray(PsiElement.EMPTY_ARRAY);
                complexity += whileStatements.length;
                
                // Add do-while loops
                PsiElement[] doWhileStatements = PsiTreeUtil.findChildrenOfType(
                        body, PsiDoWhileStatement.class).toArray(PsiElement.EMPTY_ARRAY);
                complexity += doWhileStatements.length;
                
                // Add switch statements
                PsiElement[] switchStatements = PsiTreeUtil.findChildrenOfType(
                        body, PsiSwitchStatement.class).toArray(PsiElement.EMPTY_ARRAY);
                complexity += switchStatements.length;
                
                // Add try-catch blocks
                PsiElement[] tryStatements = PsiTreeUtil.findChildrenOfType(
                        body, PsiTryStatement.class).toArray(PsiElement.EMPTY_ARRAY);
                complexity += tryStatements.length;
                
                if (complexity > 10) {
                    RefactoringAction action = new RefactoringAction(
                            RefactoringActionType.SIMPLIFY_METHOD,
                            "Consider simplifying complex method: " + method.getName() + 
                                    " (complexity: " + complexity + ")",
                            method
                    );
                    
                    // This requires advanced refactoring, so no auto-fix
                    action.setHasAutoFix(false);
                    
                    actions.add(action);
                }
            }
        }
        
        return actions;
    }
    
    /**
     * Detects potential null pointer exceptions.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectPotentialNullPointerExceptions(@NotNull PsiJavaFile javaFile) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        for (PsiClass psiClass : javaFile.getClasses()) {
            for (PsiMethod method : psiClass.getMethods()) {
                PsiCodeBlock body = method.getBody();
                if (body == null) {
                    continue;
                }
                
                // Find method calls
                PsiMethodCallExpression[] methodCalls = PsiTreeUtil.findChildrenOfType(
                        body, PsiMethodCallExpression.class).toArray(PsiMethodCallExpression.EMPTY_ARRAY);
                
                for (PsiMethodCallExpression methodCall : methodCalls) {
                    PsiReferenceExpression methodExpr = methodCall.getMethodExpression();
                    PsiExpression qualifier = methodExpr.getQualifierExpression();
                    
                    if (qualifier != null) {
                        // Check if qualifier is a variable or method call
                        PsiElement resolved = null;
                        if (qualifier instanceof PsiReferenceExpression) {
                            resolved = ((PsiReferenceExpression) qualifier).resolve();
                        }
                        
                        if (resolved instanceof PsiVariable || 
                                qualifier instanceof PsiMethodCallExpression) {
                            // Check if there's a null check for this qualifier
                            boolean hasNullCheck = hasNullCheck(body, qualifier);
                            
                            if (!hasNullCheck) {
                                RefactoringAction action = new RefactoringAction(
                                        RefactoringActionType.ADD_NULL_CHECK,
                                        "Add null check for: " + qualifier.getText(),
                                        methodCall
                                );
                                
                                actions.add(action);
                            }
                        }
                    }
                }
            }
        }
        
        return actions;
    }
    
    /**
     * Checks if there's a null check for an expression.
     * @param body The code block to search
     * @param expression The expression to check
     * @return Whether there's a null check
     */
    private boolean hasNullCheck(@NotNull PsiCodeBlock body, @NotNull PsiExpression expression) {
        String expressionText = expression.getText();
        
        // Find if statements
        PsiIfStatement[] ifStatements = PsiTreeUtil.findChildrenOfType(
                body, PsiIfStatement.class).toArray(PsiIfStatement.EMPTY_ARRAY);
        
        for (PsiIfStatement ifStatement : ifStatements) {
            PsiExpression condition = ifStatement.getCondition();
            if (condition == null) {
                continue;
            }
            
            String conditionText = condition.getText();
            
            // Check for null checks
            if (conditionText.contains(expressionText + " == null") || 
                    conditionText.contains("null == " + expressionText) || 
                    conditionText.contains(expressionText + " != null") || 
                    conditionText.contains("null != " + expressionText)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Detects missing Javadoc.
     * @param javaFile The Java file to analyze
     * @return A list of refactoring actions
     */
    @NotNull
    private List<RefactoringAction> detectMissingJavadoc(@NotNull PsiJavaFile javaFile) {
        List<RefactoringAction> actions = new ArrayList<>();
        
        for (PsiClass psiClass : javaFile.getClasses()) {
            // Check class Javadoc
            PsiDocComment classDoc = psiClass.getDocComment();
            if (classDoc == null) {
                RefactoringAction action = new RefactoringAction(
                        RefactoringActionType.ADD_JAVADOC,
                        "Add Javadoc for class: " + psiClass.getName(),
                        psiClass
                );
                
                actions.add(action);
            }
            
            // Check method Javadoc
            for (PsiMethod method : psiClass.getMethods()) {
                // Skip constructors and private methods
                if (method.isConstructor() || method.hasModifierProperty(PsiModifier.PRIVATE)) {
                    continue;
                }
                
                PsiDocComment methodDoc = method.getDocComment();
                if (methodDoc == null) {
                    RefactoringAction action = new RefactoringAction(
                            RefactoringActionType.ADD_JAVADOC,
                            "Add Javadoc for method: " + method.getName(),
                            method
                    );
                    
                    actions.add(action);
                }
            }
        }
        
        return actions;
    }
    
    /**
     * Finds a class in a file.
     * @param javaFile The Java file to search
     * @param className The class name to find
     * @return The class, or null if not found
     */
    @Nullable
    private PsiClass findClass(@NotNull PsiJavaFile javaFile, @Nullable String className) {
        if (className == null) {
            return null;
        }
        
        for (PsiClass psiClass : javaFile.getClasses()) {
            if (className.equals(psiClass.getName())) {
                return psiClass;
            }
        }
        
        return null;
    }
    
    /**
     * Applies a refactoring action.
     * @param action The action to apply
     * @return Whether the action was applied successfully
     */
    private boolean applyRefactoringAction(@NotNull RefactoringAction action) {
        try {
            PsiElement element = action.getElement();
            if (element == null) {
                return false;
            }
            
            switch (action.getType()) {
                case REMOVE_UNUSED_IMPORT:
                    return removeUnusedImport(action);
                    
                case ADD_OVERRIDE_ANNOTATION:
                    return addOverrideAnnotation(action);
                    
                case ADD_NULL_CHECK:
                    return addNullCheck(action);
                    
                case ADD_JAVADOC:
                    return addJavadoc(action);
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            LOG.error("Error applying refactoring action: " + action.getDescription(), e);
            return false;
        }
    }
    
    /**
     * Removes an unused import.
     * @param action The action to apply
     * @return Whether the action was applied successfully
     */
    private boolean removeUnusedImport(@NotNull RefactoringAction action) {
        PsiElement element = action.getElement();
        if (!(element instanceof PsiImportStatement)) {
            return false;
        }
        
        PsiImportStatement importStatement = (PsiImportStatement) element;
        
        Runnable runnable = importStatement::delete;
        
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }, "Remove Unused Import", null);
        });
        
        return true;
    }
    
    /**
     * Adds an @Override annotation.
     * @param action The action to apply
     * @return Whether the action was applied successfully
     */
    private boolean addOverrideAnnotation(@NotNull RefactoringAction action) {
        PsiElement element = action.getElement();
        if (!(element instanceof PsiMethod)) {
            return false;
        }
        
        PsiMethod method = (PsiMethod) element;
        
        Runnable runnable = () -> {
            try {
                PsiElementFactory factory = PsiElementFactory.getInstance(project);
                PsiAnnotation annotation = factory.createAnnotationFromText("@Override", method);
                
                PsiModifierList modifierList = method.getModifierList();
                modifierList.addBefore(annotation, modifierList.getFirstChild());
            } catch (IncorrectOperationException e) {
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
     * Adds a null check.
     * @param action The action to apply
     * @return Whether the action was applied successfully
     */
    private boolean addNullCheck(@NotNull RefactoringAction action) {
        PsiElement element = action.getElement();
        if (!(element instanceof PsiMethodCallExpression)) {
            return false;
        }
        
        PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
        PsiReferenceExpression methodExpr = methodCall.getMethodExpression();
        PsiExpression qualifier = methodExpr.getQualifierExpression();
        
        if (qualifier == null) {
            return false;
        }
        
        // Create null check statement
        String qualifierText = qualifier.getText();
        String nullCheckTemplate = "if (" + qualifierText + " == null) {\n    return;\n}\n";
        
        // Find parent statement
        PsiStatement statement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement.class);
        if (statement == null) {
            return false;
        }
        
        Runnable runnable = () -> {
            try {
                PsiElementFactory factory = PsiElementFactory.getInstance(project);
                PsiStatement nullCheck = factory.createStatementFromText(nullCheckTemplate, statement);
                
                statement.getParent().addBefore(nullCheck, statement);
            } catch (IncorrectOperationException e) {
                LOG.error("Error adding null check", e);
            }
        };
        
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }, "Add Null Check", null);
        });
        
        return true;
    }
    
    /**
     * Adds a Javadoc comment.
     * @param action The action to apply
     * @return Whether the action was applied successfully
     */
    private boolean addJavadoc(@NotNull RefactoringAction action) {
        PsiElement element = action.getElement();
        if (!(element instanceof PsiDocCommentOwner)) {
            return false;
        }
        
        PsiDocCommentOwner commentOwner = (PsiDocCommentOwner) element;
        
        // Generate Javadoc
        CompletableFuture<String> javadocFuture;
        
        if (commentOwner instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) commentOwner;
            String className = psiClass.getName();
            
            // Build prompt
            String prompt = "Generate a Javadoc comment for a Java class named '" + className + "'.";
            
            // Generate Javadoc
            javadocFuture = aiServiceManager.generateDocumentation(psiClass.getText(), Map.of("prompt", prompt));
        } else if (commentOwner instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) commentOwner;
            String methodName = method.getName();
            
            // Build prompt
            String prompt = "Generate a Javadoc comment for a Java method named '" + methodName + "'.";
            
            // Generate Javadoc
            javadocFuture = aiServiceManager.generateDocumentation(method.getText(), Map.of("prompt", prompt));
        } else {
            return false;
        }
        
        try {
            // Wait for Javadoc to be generated
            String javadoc = javadocFuture.get(30, TimeUnit.SECONDS);
            
            if (javadoc == null || javadoc.isEmpty()) {
                return false;
            }
            
            // Extract the Javadoc comment from the generated text
            Pattern javadocPattern = Pattern.compile("/\\*\\*\\s*([\\s\\S]*?)\\s*\\*/");
            java.util.regex.Matcher matcher = javadocPattern.matcher(javadoc);
            
            String javadocComment;
            if (matcher.find()) {
                javadocComment = matcher.group(0);
            } else {
                // If no Javadoc comment found, use the entire text
                javadocComment = "/**\n * " + javadoc.replaceAll("\\n", "\n * ") + "\n */";
            }
            
            // Add the Javadoc comment
            String finalJavadocComment = javadocComment;
            
            Runnable runnable = () -> {
                try {
                    PsiElementFactory factory = PsiElementFactory.getInstance(project);
                    PsiComment comment = factory.createCommentFromText(finalJavadocComment, commentOwner);
                    
                    commentOwner.addBefore(comment, commentOwner.getFirstChild());
                } catch (IncorrectOperationException e) {
                    LOG.error("Error adding Javadoc comment", e);
                }
            };
            
            ApplicationManager.getApplication().invokeLater(() -> {
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    ApplicationManager.getApplication().runWriteAction(runnable);
                }, "Add Javadoc", null);
            });
            
            return true;
        } catch (Exception e) {
            LOG.error("Error generating Javadoc", e);
            return false;
        }
    }
    
    /**
     * Refactoring action type.
     */
    public enum RefactoringActionType {
        REMOVE_UNUSED_IMPORT,
        ADD_OVERRIDE_ANNOTATION,
        RENAME_METHOD,
        EXTRACT_METHOD,
        EXTRACT_DUPLICATE_CODE,
        SIMPLIFY_METHOD,
        ADD_NULL_CHECK,
        ADD_JAVADOC
    }
    
    /**
     * Refactoring action.
     */
    public static class RefactoringAction {
        private final RefactoringActionType type;
        private final String description;
        private final PsiElement element;
        private boolean hasAutoFix;
        
        /**
         * Creates a new RefactoringAction.
         * @param type The action type
         * @param description The action description
         * @param element The element to refactor
         */
        public RefactoringAction(@NotNull RefactoringActionType type, @NotNull String description, 
                               @NotNull PsiElement element) {
            this.type = type;
            this.description = description;
            this.element = element;
            
            // By default, all actions have auto-fix
            this.hasAutoFix = true;
        }
        
        /**
         * Gets the action type.
         * @return The action type
         */
        @NotNull
        public RefactoringActionType getType() {
            return type;
        }
        
        /**
         * Gets the action description.
         * @return The action description
         */
        @NotNull
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the element to refactor.
         * @return The element to refactor
         */
        @NotNull
        public PsiElement getElement() {
            return element;
        }
        
        /**
         * Gets whether the action has an auto-fix.
         * @return Whether the action has an auto-fix
         */
        public boolean hasAutoFix() {
            return hasAutoFix;
        }
        
        /**
         * Sets whether the action has an auto-fix.
         * @param hasAutoFix Whether the action has an auto-fix
         */
        public void setHasAutoFix(boolean hasAutoFix) {
            this.hasAutoFix = hasAutoFix;
        }
    }
    
    /**
     * Refactoring result.
     */
    public static class RefactoringResult {
        private final List<RefactoringAction> detectedActions = new ArrayList<>();
        private final List<RefactoringAction> appliedActions = new ArrayList<>();
        
        /**
         * Gets the detected actions.
         * @return The detected actions
         */
        @NotNull
        public List<RefactoringAction> getDetectedActions() {
            return detectedActions;
        }
        
        /**
         * Gets the applied actions.
         * @return The applied actions
         */
        @NotNull
        public List<RefactoringAction> getAppliedActions() {
            return appliedActions;
        }
    }
}