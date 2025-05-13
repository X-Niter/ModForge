package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for analyzing code structure and dependencies.
 * This service provides static analysis capabilities that are used by other services
 * to understand the code and make intelligent suggestions.
 */
@Service(Service.Level.PROJECT)
public final class CodeAnalysisService {
    private static final Logger LOG = Logger.getInstance(CodeAnalysisService.class);
    
    private final Project project;
    
    // Cache of analyzed files
    private final Map<String, AnalysisResult> analysisCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new CodeAnalysisService.
     * @param project The project
     */
    public CodeAnalysisService(@NotNull Project project) {
        this.project = project;
        
        // Schedule periodic cache cleanup
        CompatibilityUtil.getCompatibleAppScheduledExecutorService().scheduleWithFixedDelay(
                this::cleanupCache, 30, 30, TimeUnit.MINUTES);
        
        LOG.info("CodeAnalysisService initialized");
    }
    
    /**
     * Gets the CodeAnalysisService instance.
     * @param project The project
     * @return The CodeAnalysisService instance
     */
    public static CodeAnalysisService getInstance(@NotNull Project project) {
        return project.getService(CodeAnalysisService.class);
    }
    
    /**
     * Analyzes a file's structure and dependencies.
     * @param file The file to analyze
     * @return A future that completes with the analysis result
     */
    @NotNull
    public CompletableFuture<AnalysisResult> analyzeFile(@NotNull VirtualFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache
                String filePath = file.getPath();
                AnalysisResult cachedResult = analysisCache.get(filePath);
                
                if (cachedResult != null && cachedResult.timestamp > file.getTimeStamp()) {
                    return cachedResult;
                }
                
                // Get PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    return new AnalysisResult(filePath, file.getTimeStamp());
                }
                
                // Create analysis result
                AnalysisResult result = new AnalysisResult(filePath, file.getTimeStamp());
                
                // Analyze file
                if (psiFile instanceof PsiJavaFile) {
                    analyzeJavaFile((PsiJavaFile) psiFile, result);
                }
                
                // Cache result
                analysisCache.put(filePath, result);
                
                return result;
            } catch (Exception e) {
                LOG.error("Error analyzing file: " + file.getPath(), e);
                return new AnalysisResult(file.getPath(), file.getTimeStamp());
            }
        }, CompatibilityUtil.getCompatibleAppExecutorService());
    }
    
    /**
     * Analyzes a Java file.
     * @param javaFile The Java file to analyze
     * @param result The analysis result to update
     */
    private void analyzeJavaFile(@NotNull PsiJavaFile javaFile, @NotNull AnalysisResult result) {
        // Get package name
        result.packageName = javaFile.getPackageName();
        
        // Analyze imports
        for (PsiImportStatement importStatement : javaFile.getImportList().getImportStatements()) {
            String importName = importStatement.getQualifiedName();
            if (importName != null) {
                result.imports.add(importName);
            }
        }
        
        // Analyze classes
        for (PsiClass psiClass : javaFile.getClasses()) {
            ClassInfo classInfo = analyzeClass(psiClass);
            result.classes.add(classInfo);
        }
    }
    
    /**
     * Analyzes a class.
     * @param psiClass The class to analyze
     * @return The class info
     */
    @NotNull
    private ClassInfo analyzeClass(@NotNull PsiClass psiClass) {
        ClassInfo classInfo = new ClassInfo();
        
        // Get class name
        classInfo.name = psiClass.getName();
        classInfo.qualifiedName = psiClass.getQualifiedName();
        
        // Get class type
        if (psiClass.isInterface()) {
            classInfo.type = "interface";
        } else if (psiClass.isEnum()) {
            classInfo.type = "enum";
        } else if (psiClass.isAnnotationType()) {
            classInfo.type = "annotation";
        } else {
            classInfo.type = "class";
        }
        
        // Check if class is abstract
        classInfo.isAbstract = psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
        
        // Get super class
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null && !superClass.getQualifiedName().equals("java.lang.Object")) {
            classInfo.superClass = superClass.getQualifiedName();
        }
        
        // Get interfaces
        for (PsiClass anInterface : psiClass.getInterfaces()) {
            String interfaceName = anInterface.getQualifiedName();
            if (interfaceName != null) {
                classInfo.interfaces.add(interfaceName);
            }
        }
        
        // Analyze fields
        for (PsiField field : psiClass.getFields()) {
            FieldInfo fieldInfo = analyzeField(field);
            classInfo.fields.add(fieldInfo);
        }
        
        // Analyze methods
        for (PsiMethod method : psiClass.getMethods()) {
            MethodInfo methodInfo = analyzeMethod(method);
            classInfo.methods.add(methodInfo);
        }
        
        // Analyze inner classes
        for (PsiClass innerClass : psiClass.getInnerClasses()) {
            ClassInfo innerClassInfo = analyzeClass(innerClass);
            classInfo.innerClasses.add(innerClassInfo);
        }
        
        return classInfo;
    }
    
    /**
     * Analyzes a field.
     * @param field The field to analyze
     * @return The field info
     */
    @NotNull
    private FieldInfo analyzeField(@NotNull PsiField field) {
        FieldInfo fieldInfo = new FieldInfo();
        
        // Get field name
        fieldInfo.name = field.getName();
        
        // Get field type
        fieldInfo.type = field.getType().getCanonicalText();
        
        // Get modifiers
        fieldInfo.isStatic = field.hasModifierProperty(PsiModifier.STATIC);
        fieldInfo.isFinal = field.hasModifierProperty(PsiModifier.FINAL);
        fieldInfo.visibility = getVisibility(field);
        
        return fieldInfo;
    }
    
    /**
     * Analyzes a method.
     * @param method The method to analyze
     * @return The method info
     */
    @NotNull
    private MethodInfo analyzeMethod(@NotNull PsiMethod method) {
        MethodInfo methodInfo = new MethodInfo();
        
        // Get method name
        methodInfo.name = method.getName();
        
        // Get return type
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            methodInfo.returnType = returnType.getCanonicalText();
        } else {
            methodInfo.returnType = "void";
        }
        
        // Get parameters
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            ParameterInfo parameterInfo = new ParameterInfo();
            parameterInfo.name = parameter.getName();
            parameterInfo.type = parameter.getType().getCanonicalText();
            
            methodInfo.parameters.add(parameterInfo);
        }
        
        // Get throws list
        for (PsiClassType exceptionType : method.getThrowsList().getReferencedTypes()) {
            methodInfo.exceptions.add(exceptionType.getCanonicalText());
        }
        
        // Get modifiers
        methodInfo.isStatic = method.hasModifierProperty(PsiModifier.STATIC);
        methodInfo.isAbstract = method.hasModifierProperty(PsiModifier.ABSTRACT);
        methodInfo.isFinal = method.hasModifierProperty(PsiModifier.FINAL);
        methodInfo.visibility = getVisibility(method);
        
        // Check if method is a constructor
        methodInfo.isConstructor = method.isConstructor();
        
        // Check if method is overriding
        methodInfo.isOverride = isOverridingMethod(method);
        
        return methodInfo;
    }
    
    /**
     * Gets the visibility of a member.
     * @param member The member
     * @return The visibility
     */
    @NotNull
    private String getVisibility(@NotNull PsiMember member) {
        if (member.hasModifierProperty(PsiModifier.PUBLIC)) {
            return "public";
        } else if (member.hasModifierProperty(PsiModifier.PROTECTED)) {
            return "protected";
        } else if (member.hasModifierProperty(PsiModifier.PRIVATE)) {
            return "private";
        } else {
            return "package-private";
        }
    }
    
    /**
     * Checks if a method is overriding a method from a superclass or interface.
     * @param method The method to check
     * @return Whether the method is overriding
     */
    private boolean isOverridingMethod(@NotNull PsiMethod method) {
        for (PsiAnnotation annotation : method.getAnnotations()) {
            if (annotation.getQualifiedName().equals("java.lang.Override")) {
                return true;
            }
        }
        
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        
        PsiMethod[] superMethods = method.findSuperMethods();
        return superMethods.length > 0;
    }
    
    /**
     * Finds references to a class.
     * @param qualifiedName The class's qualified name
     * @return A list of references
     */
    @NotNull
    public CompletableFuture<List<PsiElement>> findReferences(@NotNull String qualifiedName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PsiElement> references = new ArrayList<>();
                
                // Find class
                JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                PsiClass psiClass = psiFacade.findClass(qualifiedName, GlobalSearchScope.projectScope(project));
                
                if (psiClass != null) {
                    // Find references to the class
                    ReferencesSearch.search(psiClass, GlobalSearchScope.projectScope(project))
                            .forEach(reference -> references.add(reference.getElement()));
                }
                
                return references;
            } catch (Exception e) {
                LOG.error("Error finding references to class: " + qualifiedName, e);
                return new ArrayList<>();
            }
        }, CompatibilityUtil.getCompatibleAppExecutorService());
    }
    
    /**
     * Finds references to a method.
     * @param qualifiedClassName The class's qualified name
     * @param methodName The method name
     * @param parameterTypes The method parameter types
     * @return A list of references
     */
    @NotNull
    public CompletableFuture<List<PsiElement>> findMethodReferences(@NotNull String qualifiedClassName, 
                                                                  @NotNull String methodName, 
                                                                  @NotNull String[] parameterTypes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PsiElement> references = new ArrayList<>();
                
                // Find class
                JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                PsiClass psiClass = psiFacade.findClass(qualifiedClassName, GlobalSearchScope.projectScope(project));
                
                if (psiClass != null) {
                    // Find method
                    PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
                    
                    for (PsiMethod method : methods) {
                        PsiParameter[] parameters = method.getParameterList().getParameters();
                        
                        if (parameters.length != parameterTypes.length) {
                            continue;
                        }
                        
                        boolean match = true;
                        for (int i = 0; i < parameters.length; i++) {
                            String parameterType = parameters[i].getType().getCanonicalText();
                            if (!parameterType.equals(parameterTypes[i])) {
                                match = false;
                                break;
                            }
                        }
                        
                        if (match) {
                            // Find references to the method
                            ReferencesSearch.search(method, GlobalSearchScope.projectScope(project))
                                    .forEach(reference -> references.add(reference.getElement()));
                        }
                    }
                }
                
                return references;
            } catch (Exception e) {
                LOG.error("Error finding references to method: " + qualifiedClassName + "." + methodName, e);
                return new ArrayList<>();
            }
        }, CompatibilityUtil.getCompatibleAppExecutorService());
    }
    
    /**
     * Cleans up the analysis cache.
     */
    private void cleanupCache() {
        // Remove entries older than 1 hour
        long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        
        analysisCache.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoff);
    }
    
    /**
     * Analysis result.
     */
    public static class AnalysisResult {
        private final String filePath;
        private final long timestamp;
        private String packageName;
        private final List<String> imports = new ArrayList<>();
        private final List<ClassInfo> classes = new ArrayList<>();
        
        /**
         * Creates a new AnalysisResult.
         * @param filePath The file path
         * @param timestamp The timestamp
         */
        public AnalysisResult(@NotNull String filePath, long timestamp) {
            this.filePath = filePath;
            this.timestamp = timestamp;
        }
        
        /**
         * Gets the file path.
         * @return The file path
         */
        @NotNull
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * Gets the timestamp.
         * @return The timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Gets the package name.
         * @return The package name
         */
        @Nullable
        public String getPackageName() {
            return packageName;
        }
        
        /**
         * Gets the imports.
         * @return The imports
         */
        @NotNull
        public List<String> getImports() {
            return imports;
        }
        
        /**
         * Gets the classes.
         * @return The classes
         */
        @NotNull
        public List<ClassInfo> getClasses() {
            return classes;
        }
    }
    
    /**
     * Class info.
     */
    public static class ClassInfo {
        private String name;
        private String qualifiedName;
        private String type;
        private boolean isAbstract;
        private String superClass;
        private final List<String> interfaces = new ArrayList<>();
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<MethodInfo> methods = new ArrayList<>();
        private final List<ClassInfo> innerClasses = new ArrayList<>();
        
        /**
         * Gets the class name.
         * @return The class name
         */
        @Nullable
        public String getName() {
            return name;
        }
        
        /**
         * Gets the qualified name.
         * @return The qualified name
         */
        @Nullable
        public String getQualifiedName() {
            return qualifiedName;
        }
        
        /**
         * Gets the class type.
         * @return The class type
         */
        @Nullable
        public String getType() {
            return type;
        }
        
        /**
         * Gets whether the class is abstract.
         * @return Whether the class is abstract
         */
        public boolean isAbstract() {
            return isAbstract;
        }
        
        /**
         * Gets the super class.
         * @return The super class
         */
        @Nullable
        public String getSuperClass() {
            return superClass;
        }
        
        /**
         * Gets the interfaces.
         * @return The interfaces
         */
        @NotNull
        public List<String> getInterfaces() {
            return interfaces;
        }
        
        /**
         * Gets the fields.
         * @return The fields
         */
        @NotNull
        public List<FieldInfo> getFields() {
            return fields;
        }
        
        /**
         * Gets the methods.
         * @return The methods
         */
        @NotNull
        public List<MethodInfo> getMethods() {
            return methods;
        }
        
        /**
         * Gets the inner classes.
         * @return The inner classes
         */
        @NotNull
        public List<ClassInfo> getInnerClasses() {
            return innerClasses;
        }
    }
    
    /**
     * Field info.
     */
    public static class FieldInfo {
        private String name;
        private String type;
        private boolean isStatic;
        private boolean isFinal;
        private String visibility;
        
        /**
         * Gets the field name.
         * @return The field name
         */
        @Nullable
        public String getName() {
            return name;
        }
        
        /**
         * Gets the field type.
         * @return The field type
         */
        @Nullable
        public String getType() {
            return type;
        }
        
        /**
         * Gets whether the field is static.
         * @return Whether the field is static
         */
        public boolean isStatic() {
            return isStatic;
        }
        
        /**
         * Gets whether the field is final.
         * @return Whether the field is final
         */
        public boolean isFinal() {
            return isFinal;
        }
        
        /**
         * Gets the field visibility.
         * @return The field visibility
         */
        @Nullable
        public String getVisibility() {
            return visibility;
        }
    }
    
    /**
     * Method info.
     */
    public static class MethodInfo {
        private String name;
        private String returnType;
        private final List<ParameterInfo> parameters = new ArrayList<>();
        private final List<String> exceptions = new ArrayList<>();
        private boolean isStatic;
        private boolean isAbstract;
        private boolean isFinal;
        private String visibility;
        private boolean isConstructor;
        private boolean isOverride;
        
        /**
         * Gets the method name.
         * @return The method name
         */
        @Nullable
        public String getName() {
            return name;
        }
        
        /**
         * Gets the return type.
         * @return The return type
         */
        @Nullable
        public String getReturnType() {
            return returnType;
        }
        
        /**
         * Gets the parameters.
         * @return The parameters
         */
        @NotNull
        public List<ParameterInfo> getParameters() {
            return parameters;
        }
        
        /**
         * Gets the exceptions.
         * @return The exceptions
         */
        @NotNull
        public List<String> getExceptions() {
            return exceptions;
        }
        
        /**
         * Gets whether the method is static.
         * @return Whether the method is static
         */
        public boolean isStatic() {
            return isStatic;
        }
        
        /**
         * Gets whether the method is abstract.
         * @return Whether the method is abstract
         */
        public boolean isAbstract() {
            return isAbstract;
        }
        
        /**
         * Gets whether the method is final.
         * @return Whether the method is final
         */
        public boolean isFinal() {
            return isFinal;
        }
        
        /**
         * Gets the method visibility.
         * @return The method visibility
         */
        @Nullable
        public String getVisibility() {
            return visibility;
        }
        
        /**
         * Gets whether the method is a constructor.
         * @return Whether the method is a constructor
         */
        public boolean isConstructor() {
            return isConstructor;
        }
        
        /**
         * Gets whether the method is overriding.
         * @return Whether the method is overriding
         */
        public boolean isOverride() {
            return isOverride;
        }
    }
    
    /**
     * Parameter info.
     */
    public static class ParameterInfo {
        private String name;
        private String type;
        
        /**
         * Gets the parameter name.
         * @return The parameter name
         */
        @Nullable
        public String getName() {
            return name;
        }
        
        /**
         * Gets the parameter type.
         * @return The parameter type
         */
        @Nullable
        public String getType() {
            return type;
        }
    }
}