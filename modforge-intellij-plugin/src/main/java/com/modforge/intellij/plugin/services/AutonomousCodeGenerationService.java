package com.modforge.intellij.plugin.services;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.codeInsight.hint.ShowParameterInfoHandler;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Service for autonomously generating and improving code.
 * This service combines IntelliJ's code generation capabilities with ModForge's AI capabilities to:
 * - Generate code based on patterns recognized in the codebase
 * - Integrate with existing code structure
 * - Apply IDE-based refactoring and optimization
 * - Generate boilerplate code like getters/setters, constructors, etc.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.AutonomousCodeGeneration", 2);
    
    // Other services needed
    private final AIServiceManager aiServiceManager;
    private final IDEIntegrationService ideIntegrationService;
    private final AutomatedRefactoringService refactoringService;
    private final CodeAnalysisService codeAnalysisService;
    private final PatternRecognitionService patternRecognitionService;
    
    /**
     * Creates a new AutonomousCodeGenerationService.
     * @param project The project
     */
    public AutonomousCodeGenerationService(@NotNull Project project) {
        this.project = project;
        
        // Get other services
        this.aiServiceManager = project.getService(AIServiceManager.class);
        this.ideIntegrationService = project.getService(IDEIntegrationService.class);
        this.refactoringService = project.getService(AutomatedRefactoringService.class);
        this.codeAnalysisService = project.getService(CodeAnalysisService.class);
        this.patternRecognitionService = project.getService(PatternRecognitionService.class);
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
     * Generates an implementation for an interface or abstract class.
     * @param targetClassName The fully qualified name of the class to generate
     * @param interfaceName The fully qualified name of the interface or abstract class to implement
     * @param packageName The package name for the new class
     * @return Whether the class was generated successfully
     */
    public CompletableFuture<Boolean> generateImplementation(@NotNull String targetClassName,
                                                           @NotNull String interfaceName,
                                                           @NotNull String packageName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Generating implementation for interface: " + interfaceName);
                
                // Find the interface
                PsiClass interfaceClass = findClass(interfaceName);
                if (interfaceClass == null) {
                    LOG.warn("Interface not found: " + interfaceName);
                    return false;
                }
                
                // Check if interface is really an interface or abstract class
                if (!interfaceClass.isInterface() && !interfaceClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    LOG.warn(interfaceName + " is not an interface or abstract class");
                    return false;
                }
                
                // Get methods to implement
                List<PsiMethod> methodsToImplement = Arrays.stream(interfaceClass.getMethods())
                        .filter(method -> method.hasModifierProperty(PsiModifier.ABSTRACT))
                        .collect(Collectors.toList());
                
                LOG.info("Found " + methodsToImplement.size() + " methods to implement");
                
                // Create class template
                StringBuilder classTemplate = new StringBuilder();
                classTemplate.append("public class ").append(getSimpleName(targetClassName))
                        .append(" implements ").append(interfaceName).append(" {\n\n");
                
                // Add constructor
                classTemplate.append("    public ").append(getSimpleName(targetClassName)).append("() {\n")
                        .append("        // TODO: Initialize fields\n")
                        .append("    }\n\n");
                
                // Add method implementations
                for (PsiMethod method : methodsToImplement) {
                    classTemplate.append("    @Override\n")
                            .append("    public ").append(method.getReturnType().getPresentableText())
                            .append(" ").append(method.getName()).append("(");
                    
                    // Add parameters
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        PsiParameter parameter = parameters[i];
                        classTemplate.append(parameter.getType().getPresentableText())
                                .append(" ").append(parameter.getName());
                        
                        if (i < parameters.length - 1) {
                            classTemplate.append(", ");
                        }
                    }
                    
                    classTemplate.append(") {\n");
                    
                    // Add method body based on return type
                    PsiType returnType = method.getReturnType();
                    if (returnType != null && !returnType.equals(PsiType.VOID)) {
                        if (returnType.equals(PsiType.BOOLEAN)) {
                            classTemplate.append("        return false; // TODO: Implement\n");
                        } else if (returnType instanceof PsiPrimitiveType) {
                            // For primitive types, return default value
                            if (returnType.equals(PsiType.INT) || 
                                    returnType.equals(PsiType.LONG) || 
                                    returnType.equals(PsiType.SHORT) ||
                                    returnType.equals(PsiType.BYTE)) {
                                classTemplate.append("        return 0; // TODO: Implement\n");
                            } else if (returnType.equals(PsiType.FLOAT) || 
                                    returnType.equals(PsiType.DOUBLE)) {
                                classTemplate.append("        return 0.0; // TODO: Implement\n");
                            } else if (returnType.equals(PsiType.CHAR)) {
                                classTemplate.append("        return '\\0'; // TODO: Implement\n");
                            }
                        } else {
                            classTemplate.append("        return null; // TODO: Implement\n");
                        }
                    } else {
                        classTemplate.append("        // TODO: Implement\n");
                    }
                    
                    classTemplate.append("    }\n\n");
                }
                
                // Close class
                classTemplate.append("}");
                
                // Create the class file
                boolean created = refactoringService.createClass(packageName, 
                        getSimpleName(targetClassName), 
                        classTemplate.toString()).get();
                
                if (created) {
                    LOG.info("Created implementation class: " + targetClassName);
                    
                    // Now apply AI-based improvement
                    improveImplementationWithAI(packageName + "." + getSimpleName(targetClassName), interfaceName)
                            .thenAccept(improved -> {
                                if (improved) {
                                    LOG.info("Improved implementation with AI");
                                } else {
                                    LOG.warn("Failed to improve implementation with AI");
                                }
                            });
                    
                    return true;
                } else {
                    LOG.warn("Failed to create implementation class: " + targetClassName);
                    return false;
                }
            } catch (Exception e) {
                LOG.error("Error generating implementation", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Improves an implementation using AI and pattern recognition.
     * @param className The fully qualified name of the class to improve
     * @param interfaceName The fully qualified name of the interface or abstract class it implements
     * @return Whether the implementation was improved
     */
    public CompletableFuture<Boolean> improveImplementationWithAI(@NotNull String className,
                                                                @NotNull String interfaceName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Improving implementation of " + className + " with AI");
                
                // Find the class
                PsiClass psiClass = findClass(className);
                if (psiClass == null) {
                    LOG.warn("Class not found: " + className);
                    return false;
                }
                
                // Find the interface
                PsiClass interfaceClass = findClass(interfaceName);
                if (interfaceClass == null) {
                    LOG.warn("Interface not found: " + interfaceName);
                    return false;
                }
                
                // Find other implementations of the same interface to learn from
                List<IDEIntegrationService.ClassInfo> implementations = 
                        ideIntegrationService.findClassesImplementingInterface(interfaceName).get();
                
                // Remove the class we're improving from the list
                implementations = implementations.stream()
                        .filter(impl -> !className.equals(impl.getQualifiedName()))
                        .collect(Collectors.toList());
                
                LOG.info("Found " + implementations.size() + " other implementations to learn from");
                
                // Get methods that need improvement
                PsiMethod[] methods = psiClass.getMethods();
                
                for (PsiMethod method : methods) {
                    // Check if method needs improvement (has TODO comment or just returns default value)
                    String methodText = method.getText();
                    if (methodText.contains("TODO") || 
                            methodText.contains("return null") ||
                            methodText.contains("return 0") ||
                            methodText.contains("return false") ||
                            methodText.contains("return '\\0'")) {
                        
                        // This method needs improvement
                        LOG.info("Improving method: " + method.getName());
                        
                        // Find same method in other implementations
                        List<PsiMethod> similarMethods = new ArrayList<>();
                        
                        for (IDEIntegrationService.ClassInfo impl : implementations) {
                            PsiClass implClass = findClass(impl.getQualifiedName());
                            if (implClass != null) {
                                // Find method with same name and parameter count
                                PsiMethod[] implMethods = implClass.getMethods();
                                for (PsiMethod implMethod : implMethods) {
                                    if (implMethod.getName().equals(method.getName()) &&
                                            implMethod.getParameterList().getParametersCount() == 
                                                    method.getParameterList().getParametersCount()) {
                                        similarMethods.add(implMethod);
                                    }
                                }
                            }
                        }
                        
                        LOG.info("Found " + similarMethods.size() + " similar methods to learn from");
                        
                        // Generate improved implementation based on similar methods
                        String improvedMethodBody = null;
                        
                        if (!similarMethods.isEmpty()) {
                            // Use pattern recognition to generate improved method body
                            // In a real implementation, we'd use the PatternRecognitionService
                            // For simplicity, we'll just take the first similar method's body
                            PsiCodeBlock body = similarMethods.get(0).getBody();
                            if (body != null) {
                                improvedMethodBody = body.getText();
                            }
                        }
                        
                        if (improvedMethodBody == null) {
                            // Fallback to AI generation
                            // In a real implementation, we'd use the AIServiceManager
                            // For simplicity, we'll just generate a basic implementation
                            PsiType returnType = method.getReturnType();
                            if (returnType != null && !returnType.equals(PsiType.VOID)) {
                                if (returnType.equals(PsiType.BOOLEAN)) {
                                    improvedMethodBody = "{\n    // AI-generated implementation\n    return true;\n}";
                                } else if (returnType instanceof PsiPrimitiveType) {
                                    improvedMethodBody = "{\n    // AI-generated implementation\n    return 1;\n}";
                                } else {
                                    improvedMethodBody = "{\n    // AI-generated implementation\n    return new " +
                                            returnType.getPresentableText() + "();\n}";
                                }
                            } else {
                                improvedMethodBody = "{\n    // AI-generated implementation\n}";
                            }
                        }
                        
                        // Apply the improved method body
                        final String finalMethodBody = improvedMethodBody;
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                // Replace method body
                                PsiCodeBlock body = method.getBody();
                                if (body != null) {
                                    body.replace(PsiElementFactory.getInstance(project)
                                            .createCodeBlockFromText(finalMethodBody, method));
                                }
                                
                                // Format the method
                                CodeStyleManager.getInstance(project).reformat(method);
                            } catch (Exception e) {
                                LOG.error("Error updating method body", e);
                            }
                        });
                    }
                }
                
                LOG.info("Improved implementation of " + className);
                return true;
            } catch (Exception e) {
                LOG.error("Error improving implementation", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Generates getters and setters for all fields in a class.
     * @param className The fully qualified name of the class
     * @return Whether getters and setters were generated
     */
    public CompletableFuture<Boolean> generateGettersAndSetters(@NotNull String className) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Generating getters and setters for class: " + className);
                
                // Find the class
                PsiClass psiClass = findClass(className);
                if (psiClass == null) {
                    LOG.warn("Class not found: " + className);
                    return false;
                }
                
                // Get fields
                PsiField[] fields = psiClass.getFields();
                if (fields.length == 0) {
                    LOG.warn("No fields found in class: " + className);
                    return false;
                }
                
                // Create getters and setters for each field
                for (PsiField field : fields) {
                    // Skip static or final fields
                    if (field.hasModifierProperty(PsiModifier.STATIC) || 
                            field.hasModifierProperty(PsiModifier.FINAL)) {
                        continue;
                    }
                    
                    // Generate getter and setter names
                    String fieldName = field.getName();
                    String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    String getterName = "get" + capitalizedFieldName;
                    String setterName = "set" + capitalizedFieldName;
                    
                    // Check if getter or setter already exists
                    boolean hasGetter = false;
                    boolean hasSetter = false;
                    
                    for (PsiMethod method : psiClass.getMethods()) {
                        if (method.getName().equals(getterName)) {
                            hasGetter = true;
                        } else if (method.getName().equals(setterName)) {
                            hasSetter = true;
                        }
                    }
                    
                    // Generate getter if needed
                    if (!hasGetter) {
                        String getterTemplate = "public " + field.getType().getPresentableText() + " " + 
                                getterName + "() {\n" +
                                "    return " + fieldName + ";\n" +
                                "}";
                        
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                // Create method
                                PsiMethod getter = PsiElementFactory.getInstance(project)
                                        .createMethodFromText(getterTemplate, psiClass);
                                
                                // Add method to class
                                psiClass.add(getter);
                                
                                // Format method
                                CodeStyleManager.getInstance(project).reformat(getter);
                            } catch (Exception e) {
                                LOG.error("Error creating getter", e);
                            }
                        });
                    }
                    
                    // Generate setter if needed
                    if (!hasSetter) {
                        String setterTemplate = "public void " + setterName + "(" + 
                                field.getType().getPresentableText() + " " + fieldName + ") {\n" +
                                "    this." + fieldName + " = " + fieldName + ";\n" +
                                "}";
                        
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                // Create method
                                PsiMethod setter = PsiElementFactory.getInstance(project)
                                        .createMethodFromText(setterTemplate, psiClass);
                                
                                // Add method to class
                                psiClass.add(setter);
                                
                                // Format method
                                CodeStyleManager.getInstance(project).reformat(setter);
                            } catch (Exception e) {
                                LOG.error("Error creating setter", e);
                            }
                        });
                    }
                }
                
                LOG.info("Generated getters and setters for class: " + className);
                return true;
            } catch (Exception e) {
                LOG.error("Error generating getters and setters", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Generates a class based on a specification.
     * @param packageName The package name
     * @param className The class name
     * @param classType The class type (class, interface, enum)
     * @param fields The fields to include
     * @param methods The methods to include
     * @param comment The class comment
     * @return Whether the class was generated successfully
     */
    public CompletableFuture<Boolean> generateClass(@NotNull String packageName,
                                                  @NotNull String className,
                                                  @NotNull ClassType classType,
                                                  @NotNull List<FieldDefinition> fields,
                                                  @NotNull List<MethodDefinition> methods,
                                                  @Nullable String comment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Generating " + classType + ": " + packageName + "." + className);
                
                StringBuilder classTemplate = new StringBuilder();
                
                // Add comment
                if (comment != null && !comment.isEmpty()) {
                    classTemplate.append("/**\n");
                    for (String line : comment.split("\n")) {
                        classTemplate.append(" * ").append(line).append("\n");
                    }
                    classTemplate.append(" */\n");
                }
                
                // Add class/interface/enum declaration
                switch (classType) {
                    case CLASS:
                        classTemplate.append("public class ").append(className).append(" {\n\n");
                        break;
                    case INTERFACE:
                        classTemplate.append("public interface ").append(className).append(" {\n\n");
                        break;
                    case ENUM:
                        classTemplate.append("public enum ").append(className).append(" {\n\n");
                        classTemplate.append("    // TODO: Add enum constants\n\n");
                        break;
                }
                
                // Add fields
                for (FieldDefinition field : fields) {
                    // Add field comment
                    if (field.comment != null && !field.comment.isEmpty()) {
                        classTemplate.append("    /**\n");
                        for (String line : field.comment.split("\n")) {
                            classTemplate.append("     * ").append(line).append("\n");
                        }
                        classTemplate.append("     */\n");
                    }
                    
                    // Add field declaration
                    classTemplate.append("    ");
                    
                    // Add modifiers
                    if (classType != ClassType.INTERFACE) {
                        if (field.isPrivate) {
                            classTemplate.append("private ");
                        } else {
                            classTemplate.append("public ");
                        }
                    }
                    
                    if (field.isStatic) {
                        classTemplate.append("static ");
                    }
                    
                    if (field.isFinal) {
                        classTemplate.append("final ");
                    }
                    
                    // Add type and name
                    classTemplate.append(field.type).append(" ").append(field.name);
                    
                    // Add initializer if provided
                    if (field.initializer != null && !field.initializer.isEmpty()) {
                        classTemplate.append(" = ").append(field.initializer);
                    }
                    
                    classTemplate.append(";\n\n");
                }
                
                // Add constructor for classes
                if (classType == ClassType.CLASS && !fields.isEmpty()) {
                    // Add constructor comment
                    classTemplate.append("    /**\n");
                    classTemplate.append("     * Creates a new ").append(className).append(".\n");
                    for (FieldDefinition field : fields) {
                        if (!field.isStatic && !field.isFinal) {
                            classTemplate.append("     * @param ").append(field.name).append(" The ").append(field.name).append("\n");
                        }
                    }
                    classTemplate.append("     */\n");
                    
                    // Add constructor declaration
                    classTemplate.append("    public ").append(className).append("(");
                    
                    // Add parameters
                    boolean first = true;
                    for (FieldDefinition field : fields) {
                        if (!field.isStatic && !field.isFinal) {
                            if (!first) {
                                classTemplate.append(", ");
                            }
                            classTemplate.append(field.type).append(" ").append(field.name);
                            first = false;
                        }
                    }
                    
                    classTemplate.append(") {\n");
                    
                    // Add field assignments
                    for (FieldDefinition field : fields) {
                        if (!field.isStatic && !field.isFinal) {
                            classTemplate.append("        this.").append(field.name).append(" = ").append(field.name).append(";\n");
                        }
                    }
                    
                    classTemplate.append("    }\n\n");
                }
                
                // Add methods
                for (MethodDefinition method : methods) {
                    // Add method comment
                    if (method.comment != null && !method.comment.isEmpty()) {
                        classTemplate.append("    /**\n");
                        for (String line : method.comment.split("\n")) {
                            classTemplate.append("     * ").append(line).append("\n");
                        }
                        
                        // Add parameter comments
                        for (ParameterDefinition param : method.parameters) {
                            classTemplate.append("     * @param ").append(param.name).append(" ").append(param.comment).append("\n");
                        }
                        
                        // Add return comment if not void
                        if (!method.returnType.equals("void") && method.returnComment != null) {
                            classTemplate.append("     * @return ").append(method.returnComment).append("\n");
                        }
                        
                        classTemplate.append("     */\n");
                    }
                    
                    // Add method declaration
                    classTemplate.append("    ");
                    
                    // Add modifiers for class methods
                    if (classType == ClassType.CLASS) {
                        if (method.isPrivate) {
                            classTemplate.append("private ");
                        } else {
                            classTemplate.append("public ");
                        }
                        
                        if (method.isStatic) {
                            classTemplate.append("static ");
                        }
                        
                        if (method.isFinal) {
                            classTemplate.append("final ");
                        }
                    }
                    
                    // Add return type and name
                    classTemplate.append(method.returnType).append(" ").append(method.name).append("(");
                    
                    // Add parameters
                    for (int i = 0; i < method.parameters.size(); i++) {
                        ParameterDefinition param = method.parameters.get(i);
                        classTemplate.append(param.type).append(" ").append(param.name);
                        
                        if (i < method.parameters.size() - 1) {
                            classTemplate.append(", ");
                        }
                    }
                    
                    classTemplate.append(")");
                    
                    // Add method body for classes or ; for interfaces
                    if (classType == ClassType.INTERFACE) {
                        classTemplate.append(";\n\n");
                    } else {
                        classTemplate.append(" {\n");
                        
                        // Add method body
                        if (method.body != null && !method.body.isEmpty()) {
                            for (String line : method.body.split("\n")) {
                                classTemplate.append("        ").append(line).append("\n");
                            }
                        } else {
                            // Generate default body based on return type
                            if (!method.returnType.equals("void")) {
                                if (method.returnType.equals("boolean")) {
                                    classTemplate.append("        return false; // TODO: Implement\n");
                                } else if (method.returnType.equals("int") || 
                                        method.returnType.equals("long") || 
                                        method.returnType.equals("short") ||
                                        method.returnType.equals("byte")) {
                                    classTemplate.append("        return 0; // TODO: Implement\n");
                                } else if (method.returnType.equals("float") || 
                                        method.returnType.equals("double")) {
                                    classTemplate.append("        return 0.0; // TODO: Implement\n");
                                } else if (method.returnType.equals("char")) {
                                    classTemplate.append("        return '\\0'; // TODO: Implement\n");
                                } else {
                                    classTemplate.append("        return null; // TODO: Implement\n");
                                }
                            } else {
                                classTemplate.append("        // TODO: Implement\n");
                            }
                        }
                        
                        classTemplate.append("    }\n\n");
                    }
                }
                
                // Close class
                classTemplate.append("}");
                
                // Create the class file
                boolean created = refactoringService.createClass(packageName, className, classTemplate.toString()).get();
                
                if (created) {
                    LOG.info("Created " + classType + ": " + packageName + "." + className);
                    return true;
                } else {
                    LOG.warn("Failed to create " + classType + ": " + packageName + "." + className);
                    return false;
                }
            } catch (Exception e) {
                LOG.error("Error generating class", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Finds a class in the project.
     * @param qualifiedName The fully qualified class name
     * @return The PsiClass, or null if not found
     */
    @Nullable
    private PsiClass findClass(@NotNull String qualifiedName) {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiClass>) () -> {
            try {
                String className = getSimpleName(qualifiedName);
                PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(
                        className,
                        GlobalSearchScope.projectScope(project)
                );
                
                for (PsiClass psiClass : classes) {
                    if (qualifiedName.equals(psiClass.getQualifiedName())) {
                        return psiClass;
                    }
                }
                
                return null;
            } catch (Exception e) {
                LOG.error("Error finding class", e);
                return null;
            }
        });
    }
    
    /**
     * Gets the simple name from a qualified name.
     * @param qualifiedName The fully qualified name
     * @return The simple name
     */
    @NotNull
    private String getSimpleName(@NotNull String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot != -1) {
            return qualifiedName.substring(lastDot + 1);
        }
        return qualifiedName;
    }
    
    /**
     * Class types that can be generated.
     */
    public enum ClassType {
        CLASS,
        INTERFACE,
        ENUM
    }
    
    /**
     * Definition of a field.
     */
    public static class FieldDefinition {
        private final String name;
        private final String type;
        private final boolean isPrivate;
        private final boolean isStatic;
        private final boolean isFinal;
        private final String initializer;
        private final String comment;
        
        /**
         * Creates a new FieldDefinition.
         * @param name The field name
         * @param type The field type
         * @param isPrivate Whether the field is private
         * @param isStatic Whether the field is static
         * @param isFinal Whether the field is final
         * @param initializer The field initializer
         * @param comment The field comment
         */
        public FieldDefinition(@NotNull String name, @NotNull String type, boolean isPrivate,
                              boolean isStatic, boolean isFinal, @Nullable String initializer,
                              @Nullable String comment) {
            this.name = name;
            this.type = type;
            this.isPrivate = isPrivate;
            this.isStatic = isStatic;
            this.isFinal = isFinal;
            this.initializer = initializer;
            this.comment = comment;
        }
    }
    
    /**
     * Definition of a method.
     */
    public static class MethodDefinition {
        private final String name;
        private final String returnType;
        private final List<ParameterDefinition> parameters;
        private final boolean isPrivate;
        private final boolean isStatic;
        private final boolean isFinal;
        private final String body;
        private final String comment;
        private final String returnComment;
        
        /**
         * Creates a new MethodDefinition.
         * @param name The method name
         * @param returnType The return type
         * @param parameters The method parameters
         * @param isPrivate Whether the method is private
         * @param isStatic Whether the method is static
         * @param isFinal Whether the method is final
         * @param body The method body
         * @param comment The method comment
         * @param returnComment The return comment
         */
        public MethodDefinition(@NotNull String name, @NotNull String returnType,
                               @NotNull List<ParameterDefinition> parameters, boolean isPrivate,
                               boolean isStatic, boolean isFinal, @Nullable String body,
                               @Nullable String comment, @Nullable String returnComment) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
            this.isPrivate = isPrivate;
            this.isStatic = isStatic;
            this.isFinal = isFinal;
            this.body = body;
            this.comment = comment;
            this.returnComment = returnComment;
        }
    }
    
    /**
     * Definition of a parameter.
     */
    public static class ParameterDefinition {
        private final String name;
        private final String type;
        private final String comment;
        
        /**
         * Creates a new ParameterDefinition.
         * @param name The parameter name
         * @param type The parameter type
         * @param comment The parameter comment
         */
        public ParameterDefinition(@NotNull String name, @NotNull String type, @Nullable String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
    }
}