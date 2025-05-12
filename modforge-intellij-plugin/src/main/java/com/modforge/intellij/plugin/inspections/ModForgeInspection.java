package com.modforge.intellij.plugin.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Custom inspection for ModForge-specific code issues.
 * This inspection identifies common Minecraft modding issues and offers quick fixes.
 */
public class ModForgeInspection extends AbstractBaseJavaLocalInspectionTool {
    @NotNull
    @Override
    public String getShortName() {
        return "ModForgeCodeAnalysis";
    }
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "ModForge Code Analysis";
    }
    
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "ModForge";
    }
    
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                super.visitMethod(method);
                
                // Check methods that might be involved in Minecraft mod registration
                if (method.getName().equals("registerBlocks") || 
                        method.getName().equals("registerItems") ||
                        method.getName().equals("init") ||
                        method.getName().equals("setup") ||
                        method.getName().equals("onInitialize")) {
                    
                    // Check for potential threading issues in registration methods
                    PsiCodeBlock body = method.getBody();
                    if (body != null) {
                        PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class).stream()
                                .filter(call -> {
                                    PsiReferenceExpression methodExpression = call.getMethodExpression();
                                    String methodName = methodExpression.getReferenceName();
                                    return "execute".equals(methodName) || 
                                           "submit".equals(methodName) ||
                                           "supplyAsync".equals(methodName) ||
                                           "runAsync".equals(methodName);
                                })
                                .forEach(call -> {
                                    holder.registerProblem(
                                            call,
                                            "Avoid using threading in registration methods as it can cause race conditions",
                                            new ModForgeThreadingQuickFix()
                                    );
                                });
                    }
                }
                
                // Check for direct item registry access without using DeferredRegister or Registry
                checkForDirectRegistryAccess(method, holder);
            }
            
            @Override
            public void visitClass(@NotNull PsiClass aClass) {
                super.visitClass(aClass);
                
                // Check mod main class
                PsiAnnotation[] annotations = aClass.getAnnotations();
                for (PsiAnnotation annotation : annotations) {
                    String qualifiedName = annotation.getQualifiedName();
                    if (qualifiedName != null && (
                            qualifiedName.equals("net.minecraftforge.fml.common.Mod") ||
                            qualifiedName.equals("net.fabricmc.api.ModInitializer"))) {
                        
                        // Mod class found, check for common issues
                        checkModClass(aClass, holder);
                    }
                }
            }
            
            @Override
            public void visitField(@NotNull PsiField field) {
                super.visitField(field);
                
                // Check static fields that might be part of a registry
                if (field.hasModifierProperty(PsiModifier.STATIC)) {
                    PsiType type = field.getType();
                    String typeName = type.getPresentableText();
                    
                    if (typeName.contains("DeferredRegister") || 
                            typeName.contains("Registry") ||
                            typeName.contains("RegistryObject")) {
                        
                        // Check if initialized correctly
                        PsiExpression initializer = field.getInitializer();
                        if (initializer == null) {
                            holder.registerProblem(
                                    field.getNameIdentifier(),
                                    "Registry field should be initialized",
                                    new ModForgeRegistryInitQuickFix()
                            );
                        }
                    }
                }
            }
        };
    }
    
    /**
     * Checks for direct registry access.
     * @param method The method to check
     * @param holder The problems holder
     */
    private void checkForDirectRegistryAccess(@NotNull PsiMethod method, @NotNull ProblemsHolder holder) {
        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return;
        }
        
        PsiTreeUtil.findChildrenOfType(body, PsiReferenceExpression.class).stream()
                .filter(ref -> {
                    String name = ref.getReferenceName();
                    return "ITEMS".equals(name) || 
                           "BLOCKS".equals(name) ||
                           "BIOMES".equals(name) ||
                           "FEATURES".equals(name);
                })
                .forEach(ref -> {
                    // Check if part of a qualified name like Registry.ITEMS
                    PsiElement parent = ref.getParent();
                    if (parent instanceof PsiReferenceExpression) {
                        PsiReferenceExpression parentRef = (PsiReferenceExpression) parent;
                        String qualifier = parentRef.getQualifiedName();
                        
                        if (qualifier != null && (
                                qualifier.contains("Registry") ||
                                qualifier.contains("Registries"))) {
                            
                            // Check if part of a register call
                            PsiElement grandParent = parent.getParent();
                            if (grandParent instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression call = (PsiMethodCallExpression) grandParent;
                                String methodName = call.getMethodExpression().getReferenceName();
                                
                                if ("register".equals(methodName) || "registerObject".equals(methodName)) {
                                    holder.registerProblem(
                                            call,
                                            "Consider using DeferredRegister instead of direct Registry access",
                                            new ModForgeDeferredRegisterQuickFix()
                                    );
                                }
                            }
                        }
                    }
                });
    }
    
    /**
     * Checks the mod class for common issues.
     * @param aClass The class to check
     * @param holder The problems holder
     */
    private void checkModClass(@NotNull PsiClass aClass, @NotNull ProblemsHolder holder) {
        // Check for missing logger
        boolean hasLogger = false;
        for (PsiField field : aClass.getFields()) {
            PsiType type = field.getType();
            String typeName = type.getPresentableText();
            
            if (typeName.contains("Logger") || typeName.contains("Log4j") || typeName.contains("Slf4j")) {
                hasLogger = true;
                break;
            }
        }
        
        if (!hasLogger) {
            holder.registerProblem(
                    aClass.getNameIdentifier(),
                    "Mod class should have a logger for proper error reporting",
                    new ModForgeAddLoggerQuickFix()
            );
        }
    }
    
    /**
     * Quick fix for threading issues.
     */
    private static class ModForgeThreadingQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Remove threading from registration method";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "ModForge Fixes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // This fix would need to be implemented in a real plugin
        }
    }
    
    /**
     * Quick fix for registry initialization.
     */
    private static class ModForgeRegistryInitQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Initialize registry field";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "ModForge Fixes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // This fix would need to be implemented in a real plugin
        }
    }
    
    /**
     * Quick fix for direct registry access.
     */
    private static class ModForgeDeferredRegisterQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Convert to DeferredRegister";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "ModForge Fixes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // This fix would need to be implemented in a real plugin
        }
    }
    
    /**
     * Quick fix to add a logger.
     */
    private static class ModForgeAddLoggerQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add logger field";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "ModForge Fixes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // This fix would need to be implemented in a real plugin
        }
    }
}