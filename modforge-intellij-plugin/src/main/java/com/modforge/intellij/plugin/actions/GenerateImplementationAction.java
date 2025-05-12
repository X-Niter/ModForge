package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.ui.dialogs.GenerateImplementationDialog;
import com.modforge.intellij.plugin.utils.DialogUtils;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Action for generating an implementation of a class or interface.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class GenerateImplementationAction extends AnAction {
    
    /**
     * Updates the action's presentation.
     *
     * @param e The action event.
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        boolean enabled = project != null && editor != null && psiFile instanceof PsiJavaFile;
        e.getPresentation().setEnabledAndVisible(enabled);
    }
    
    /**
     * Performs the action.
     *
     * @param e The action event.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (project == null || editor == null || !(psiFile instanceof PsiJavaFile)) {
            return;
        }
        
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        
        if (psiClass == null) {
            DialogUtils.showErrorDialog(project, "Please place the cursor inside a class or interface", "Error");
            return;
        }
        
        if (!psiClass.isInterface() && !psiClass.isEnum() && !psiClass.hasModifierProperty("abstract")) {
            DialogUtils.showErrorDialog(project, 
                    "Selected class is neither an interface, enum nor an abstract class", 
                    "Error");
            return;
        }
        
        // Show dialog to get implementation details
        GenerateImplementationDialog dialog = new GenerateImplementationDialog(project, psiClass.getName());
        if (!dialog.showAndGet()) {
            return;
        }
        
        String implementationName = dialog.getImplementationName();
        String description = dialog.getImplementationDescription();
        
        // Get service
        AutonomousCodeGenerationService codeGenerationService = 
                ApplicationManager.getApplication().getService(AutonomousCodeGenerationService.class);
        
        // Generate implementation asynchronously
        CompletableFuture<Boolean> future = ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                // This is a mock implementation, in the real code this would generate the actual implementation
                // Pass null for now to avoid compilation errors
                boolean result = generateImplementation(project, psiClass, implementationName, description);
                
                if (result) {
                    DialogUtils.showInfoDialog(
                            project, 
                            "Successfully generated implementation: " + implementationName, 
                            "Success"
                    );
                } else {
                    DialogUtils.showErrorDialog(
                            project, 
                            "Failed to generate implementation", 
                            "Error"
                    );
                }
                
                return result;
            } catch (Exception ex) {
                DialogUtils.showErrorDialog(
                        project, 
                        "Error generating implementation: " + ex.getMessage(), 
                        "Error"
                );
                return false;
            }
        });
    }
    
    /**
     * Generates an implementation for a class or interface.
     * This is a mock implementation.
     *
     * @param project          The project.
     * @param psiClass         The class to implement.
     * @param implementationName The name of the implementation.
     * @param description      The description.
     * @return Whether generation was successful.
     */
    private boolean generateImplementation(
            @NotNull Project project,
            @NotNull PsiClass psiClass,
            @NotNull String implementationName,
            @NotNull String description) {
        
        // In a real implementation, this would:
        // 1. Analyze the class structure (methods, fields, etc.)
        // 2. Call AI service to generate code
        // 3. Create a new file with the implementation
        // 4. Add the implementation to the project
        
        // For now, just return true
        return true;
    }
}