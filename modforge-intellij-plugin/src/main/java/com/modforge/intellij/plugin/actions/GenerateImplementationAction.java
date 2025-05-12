package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.ui.dialogs.GenerateImplementationDialog;
import com.modforge.intellij.plugin.utils.PsiUtils;
import com.modforge.intellij.plugin.utils.VirtualFileUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * Action for generating implementations of interfaces or abstract classes.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class GenerateImplementationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateImplementationAction.class);
    
    /**
     * Performs the action.
     *
     * @param e The action event.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) {
            return;
        }
        
        PsiElement elementAtCaret = PsiUtils.getElementAtCaret(editor, psiFile);
        if (elementAtCaret == null) {
            return;
        }
        
        // Find class or interface at caret
        PsiClass psiClass = PsiTreeUtil.getParentOfType(elementAtCaret, PsiClass.class);
        if (psiClass == null) {
            ModForgeNotificationService.getInstance().showWarningNotification(
                    project,
                    "Generate Implementation",
                    "No class or interface found at cursor position."
            );
            return;
        }
        
        // Check if class is an interface or abstract class
        if (!psiClass.isInterface() && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            ModForgeNotificationService.getInstance().showWarningNotification(
                    project,
                    "Generate Implementation",
                    "Selected class is not an interface or abstract class."
            );
            return;
        }
        
        // Show implementation dialog
        GenerateImplementationDialog dialog = new GenerateImplementationDialog(project);
        String className = psiClass.getName();
        String packageName = ((PsiJavaFile) psiFile).getPackageName();
        
        // Create a suggested output path
        VirtualFile baseDirVf = CompatibilityUtil.getProjectBaseDir(project);
        String baseDir = baseDirVf != null ? baseDirVf.getPath() : "";
        String relativePath = VirtualFileUtil.getRelativePath(psiFile.getVirtualFile(), project);
        String directoryPath = relativePath != null && Paths.get(relativePath).getParent() != null ? 
                              Paths.get(relativePath).getParent().toString() : "";
        String implName = className + "Impl";
        String outputPath = Paths.get(baseDir, directoryPath, implName + ".java").toString();
        
        // Set dialog fields
        if (dialog.getComponent() != null) {
            dialog.getComponent().setText(
                    "Generate implementation for " + className + " in package " + packageName + "\n\n" +
                    "Additional requirements (optional):"
            );
            
            dialog.getOutputPathField().setText(outputPath);
        }
        
        if (dialog.showAndGet()) {
            // Dialog was confirmed, generate implementation
            String prompt = "Generate a complete implementation for the " + 
                    (psiClass.isInterface() ? "interface" : "abstract class") + 
                    " " + className + " in package " + packageName + ".\n\n" +
                    "Requirements:\n" + dialog.getPromptText();
            
            AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance();
            
            // Generate implementation
            CompletableFuture<Boolean> future = service.generateImplementation(
                    project,
                    psiClass,
                    dialog.getOutputPath(),
                    prompt
            );
            
            // Use .whenComplete to avoid unwrapping CompletableFuture result
            future.whenComplete((success, error) -> {
                if (error != null) {
                    LOG.error("Error generating implementation", error);
                    ModForgeNotificationService.getInstance().showErrorNotification(
                            project,
                            "Generation Failed",
                            "Failed to generate implementation for " + className + ": " + error.getMessage()
                    );
                } else if (success != null && success) { // Fixed boolean dereferencing issue here
                    ModForgeNotificationService.getInstance().showInfoNotification(
                            project,
                            "Implementation Generated",
                            "Successfully generated implementation for " + className
                    );
                } else {
                    ModForgeNotificationService.getInstance().showErrorNotification(
                            project,
                            "Generation Failed",
                            "Failed to generate implementation for " + className
                    );
                }
            });
        }
    }
    
    /**
     * Updates the action presentation.
     *
     * @param e The action event.
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (!(psiFile instanceof PsiJavaFile)) {
            e.getPresentation().setEnabled(false);
            return;
        }
        
        // Enable action for Java files
        e.getPresentation().setEnabled(true);
    }
    
    /**
     * Gets the action update thread.
     *
     * @return The action update thread.
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}