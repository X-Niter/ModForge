package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for working with PSI elements.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class PsiUtils {
    private static final Logger LOG = Logger.getInstance(PsiUtils.class);
    
    /**
     * Private constructor.
     */
    private PsiUtils() {
        // Utility class
    }
    
    /**
     * Gets the element at the caret position.
     *
     * @param editor The editor.
     * @param file   The PSI file.
     * @return The element at the caret position, or null if not found.
     */
    @Nullable
    public static PsiElement getElementAtCaret(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        return file.findElementAt(offset);
    }
    
    /**
     * Gets the PSI class from the caret position.
     *
     * @param editor The editor.
     * @param file   The PSI file.
     * @return The PSI class, or null if not found.
     */
    @Nullable
    public static PsiClass getClassAtCaret(@NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement element = getElementAtCaret(editor, file);
        if (element == null) {
            return null;
        }
        
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }
    
    /**
     * Gets the PSI method from the caret position.
     *
     * @param editor The editor.
     * @param file   The PSI file.
     * @return The PSI method, or null if not found.
     */
    @Nullable
    public static PsiMethod getMethodAtCaret(@NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement element = getElementAtCaret(editor, file);
        if (element == null) {
            return null;
        }
        
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }
    
    /**
     * Gets the PSI class by qualified name.
     *
     * @param project      The project.
     * @param qualifiedName The qualified name.
     * @return The PSI class, or null if not found.
     */
    @Nullable
    public static PsiClass findClass(@NotNull Project project, @NotNull String qualifiedName) {
        return JavaPsiFacade.getInstance(project).findClass(
                qualifiedName,
                GlobalSearchScope.allScope(project)
        );
    }
    
    /**
     * Gets all methods of a class, including inherited ones, with option to include parent methods.
     *
     * @param psiClass          The PSI class.
     * @param includeParentMethods Whether to include parent methods.
     * @return The methods.
     */
    @NotNull
    public static List<PsiMethod> getAllMethods(@NotNull PsiClass psiClass, boolean includeParentMethods) {
        List<PsiMethod> methods = new ArrayList<>(Arrays.asList(psiClass.getMethods()));
        
        if (includeParentMethods) {
            PsiClass[] supers = psiClass.getSupers();
            for (PsiClass superClass : supers) {
                methods.addAll(Arrays.asList(superClass.getMethods()));
            }
        }
        
        return methods;
    }
    
    /**
     * Gets all fields of a class, including inherited ones, with option to include parent fields.
     *
     * @param psiClass         The PSI class.
     * @param includeParentFields Whether to include parent fields.
     * @return The fields.
     */
    @NotNull
    public static List<PsiField> getAllFields(@NotNull PsiClass psiClass, boolean includeParentFields) {
        List<PsiField> fields = new ArrayList<>(Arrays.asList(psiClass.getFields()));
        
        if (includeParentFields) {
            PsiClass[] supers = psiClass.getSupers();
            for (PsiClass superClass : supers) {
                fields.addAll(Arrays.asList(superClass.getFields()));
            }
        }
        
        return fields;
    }
    
    /**
     * Gets abstract methods of a class.
     *
     * @param psiClass The PSI class.
     * @return The abstract methods.
     */
    @NotNull
    public static List<PsiMethod> getAbstractMethods(@NotNull PsiClass psiClass) {
        List<PsiMethod> methods = getAllMethods(psiClass, true);
        
        return methods.stream()
                .filter(method -> method.hasModifierProperty(PsiModifier.ABSTRACT))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the module for a PSI element.
     *
     * @param element The PSI element.
     * @return The module, or null if not found.
     */
    @Nullable
    public static Module getModule(@NotNull PsiElement element) {
        return ModuleUtil.findModuleForPsiElement(element);
    }
    
    /**
     * Gets the JavaDoc comment for a method.
     *
     * @param method The method.
     * @return The JavaDoc comment, or null if not found.
     */
    @Nullable
    public static String getJavaDocComment(@NotNull PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null) {
            return null;
        }
        
        return docComment.getText();
    }
    
    /**
     * Gets the method signature as a string.
     *
     * @param method The method.
     * @return The method signature.
     */
    @NotNull
    public static String getMethodSignature(@NotNull PsiMethod method) {
        StringBuilder signature = new StringBuilder();
        
        // Add modifiers
        PsiModifierList modifiers = method.getModifierList();
        for (String modifier : PsiModifier.MODIFIERS) {
            if (modifiers.hasModifierProperty(modifier)) {
                signature.append(modifier).append(" ");
            }
        }
        
        // Add return type
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            signature.append(returnType.getPresentableText()).append(" ");
        }
        
        // Add method name
        signature.append(method.getName());
        
        // Add parameters
        signature.append("(");
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiParameter parameter = parameters[i];
            signature.append(parameter.getType().getPresentableText())
                    .append(" ")
                    .append(parameter.getName());
            
            if (i < parameters.length - 1) {
                signature.append(", ");
            }
        }
        signature.append(")");
        
        // Add throws
        PsiClassType[] throwsList = method.getThrowsList().getReferencedTypes();
        if (throwsList.length > 0) {
            signature.append(" throws ");
            for (int i = 0; i < throwsList.length; i++) {
                PsiClassType throwsType = throwsList[i];
                signature.append(throwsType.getPresentableText());
                
                if (i < throwsList.length - 1) {
                    signature.append(", ");
                }
            }
        }
        
        return signature.toString();
    }
    
    /**
     * Gets the imports from a file.
     *
     * @param file The file.
     * @return The imports.
     */
    @NotNull
    public static List<String> getImports(@NotNull PsiJavaFile file) {
        List<String> imports = new ArrayList<>();
        
        for (PsiImportStatement importStatement : file.getImportList().getImportStatements()) {
            imports.add(importStatement.getQualifiedName());
        }
        
        return imports;
    }
    
    /**
     * Gets all classes from a file.
     *
     * @param file The file.
     * @return The classes.
     */
    @NotNull
    public static List<PsiClass> getClasses(@NotNull PsiJavaFile file) {
        return Arrays.asList(file.getClasses());
    }
    
    /**
     * Scrolls to a position in the editor.
     *
     * @param editor   The editor.
     * @param line     The line.
     * @param column   The column.
     */
    public static void scrollToPosition(@NotNull Editor editor, int line, int column) {
        LogicalPosition position = new LogicalPosition(line, column);
        editor.getCaretModel().moveToLogicalPosition(position);
        editor.getScrollingModel().scrollTo(position, ScrollType.CENTER);
    }
    
    /**
     * Scrolls to an element in the editor.
     *
     * @param editor   The editor.
     * @param element  The element.
     */
    public static void scrollToElement(@NotNull Editor editor, @NotNull PsiElement element) {
        int offset = element.getTextOffset();
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }
}