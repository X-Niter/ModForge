package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to generate code using AI.
 */
public class GenerateCodeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
        
        // Get programming language from file
        String fileExtension = psiFile.getFileType().getDefaultExtension();
        String language = getLanguageFromExtension(fileExtension);
        
        // Ask for prompt
        String prompt = Messages.showInputDialog(
                project,
                "Enter what you want to generate:",
                "Generate Code",
                Messages.getQuestionIcon()
        );
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return;
        }
        
        // Get code generation service
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        try {
            // Generate code
            codeGenService.generateCode(prompt, language, null)
                    .thenAccept(generatedCode -> {
                        if (generatedCode == null || generatedCode.isEmpty()) {
                            Messages.showErrorDialog(
                                    project,
                                    "Failed to generate code.",
                                    "Generate Code"
                            );
                            return;
                        }
                        
                        // Insert code at cursor position
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            int offset = editor.getCaretModel().getOffset();
                            editor.getDocument().insertString(offset, generatedCode);
                            editor.getCaretModel().moveToOffset(offset + generatedCode.length());
                            editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                        });
                    })
                    .exceptionally(ex -> {
                        Messages.showErrorDialog(
                                project,
                                "Error generating code: " + ex.getMessage(),
                                "Generate Code"
                        );
                        return null;
                    });
        } catch (Exception ex) {
            Messages.showErrorDialog(
                    project,
                    "Error generating code: " + ex.getMessage(),
                    "Generate Code"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable only if we have a project, editor, and file
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        e.getPresentation().setEnabledAndVisible(
                project != null && editor != null && psiFile != null
        );
    }
    
    /**
     * Determines the programming language from a file extension.
     * @param extension The file extension
     * @return The programming language
     */
    private String getLanguageFromExtension(String extension) {
        if (extension == null) {
            return "text";
        }
        
        switch (extension.toLowerCase()) {
            case "java":
                return "java";
            case "kt":
                return "kotlin";
            case "js":
            case "jsx":
            case "ts":
            case "tsx":
                return "javascript";
            case "py":
                return "python";
            case "rb":
                return "ruby";
            case "go":
                return "go";
            case "c":
            case "cpp":
            case "h":
            case "hpp":
                return "cpp";
            case "cs":
                return "csharp";
            case "php":
                return "php";
            case "rs":
                return "rust";
            case "swift":
                return "swift";
            case "m":
                return "objective-c";
            case "scala":
                return "scala";
            case "groovy":
                return "groovy";
            case "rb":
                return "ruby";
            default:
                return "text";
        }
    }
}