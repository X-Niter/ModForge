package com.modforge.intellij.plugin.crossloader.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService;
import org.jetbrains.annotations.NotNull;

/**
 * Intention action to convert selected code to platform-specific implementation
 * using Architectury's ExpectPlatform annotation.
 */
public class ConvertToPlatformSpecificIntention extends PsiElementBaseIntentionAction implements IntentionAction {
    
    /**
     * Gets the text to display in the intention popup.
     * @return The text
     */
    @NotNull
    @Override
    public String getText() {
        return "Convert to platform-specific implementation";
    }
    
    /**
     * Gets the family name for this intention.
     * @return The family name
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return "ModForge AI";
    }
    
    /**
     * Checks if this intention is available at the current offset.
     * @param project The project
     * @param editor The editor
     * @param element The element at the current offset
     * @return Whether this intention is available
     */
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // Check if we're in a Java file
        if (!(element.getContainingFile() instanceof PsiJavaFile)) {
            return false;
        }
        
        // Check if Architectury is available
        ArchitecturyService architecturyService = ArchitecturyService.getInstance(project);
        if (!architecturyService.isArchitecturyAvailable()) {
            return false;
        }
        
        // Check if we're in a method
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return false;
        }
        
        // Check if the method is not already annotated with @ExpectPlatform
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getQualifiedName() != null && 
                annotation.getQualifiedName().equals("dev.architectury.injectables.annotations.ExpectPlatform")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Invokes the intention action.
     * @param project The project
     * @param editor The editor
     * @param element The element at the current offset
     * @throws IncorrectOperationException If an error occurs
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        // Find the method containing this element
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return;
        }
        
        // Get the factory to create elements
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        
        // Add the ExpectPlatform annotation
        PsiAnnotation annotation = factory.createAnnotationFromText("@dev.architectury.injectables.annotations.ExpectPlatform", method);
        PsiModifierList modifierList = method.getModifierList();
        modifierList.addAfter(annotation, null);
        
        // Convert the method body to throw an AssertionError
        if (!method.isConstructor() && !modifierList.hasModifierProperty(PsiModifier.ABSTRACT)) {
            // Create new method body that throws AssertionError
            PsiCodeBlock newBody = factory.createCodeBlockFromText(
                    "{\n    // This code is eliminated by the platform-specific implementation\n    throw new AssertionError();\n}", method);
            
            // Replace the existing method body
            PsiCodeBlock body = method.getBody();
            if (body != null) {
                body.replace(newBody);
            }
        }
    }
}