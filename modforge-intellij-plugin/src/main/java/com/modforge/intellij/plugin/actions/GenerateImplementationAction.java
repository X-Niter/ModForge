package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Action for generating an implementation of an interface or abstract class.
 */
public class GenerateImplementationAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (project == null || editor == null || psiFile == null) {
            return;
        }
        
        // Find the class at cursor position
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        
        if (psiClass == null) {
            Messages.showErrorDialog(project, "No class found at cursor position", "Error");
            return;
        }
        
        // Check if the class is an interface or abstract class
        if (!psiClass.isInterface() && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            Messages.showErrorDialog(project, "The selected class is not an interface or abstract class", "Error");
            return;
        }
        
        // Show dialog to get implementation details
        ImplementationDialog dialog = new ImplementationDialog(project, psiClass);
        if (!dialog.showAndGet()) {
            return;
        }
        
        // Get input values
        String implementationName = dialog.getImplementationName();
        String packageName = dialog.getPackageName();
        
        // Get service
        AutonomousCodeGenerationService codeGenerationService = 
                AutonomousCodeGenerationService.getInstance(project);
        
        // Generate implementation
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Implementation") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                
                try {
                    // Set progress text
                    indicator.setText("Generating implementation...");
                    indicator.setFraction(0.2);
                    
                    // Generate implementation
                    String targetClassName = packageName + "." + implementationName;
                    boolean success = codeGenerationService.generateImplementation(
                            targetClassName,
                            psiClass.getQualifiedName(),
                            packageName
                    ).get();
                    
                    // Update progress
                    indicator.setText("Improving implementation with AI...");
                    indicator.setFraction(0.8);
                    
                    // Show result
                    if (success) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoDialog(
                                    project,
                                    "Successfully generated implementation: " + targetClassName,
                                    "Implementation Generated"
                            );
                        });
                    } else {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showErrorDialog(
                                    project,
                                    "Failed to generate implementation",
                                    "Error"
                            );
                        });
                    }
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Error generating implementation: " + ex.getMessage(),
                                "Error"
                        );
                    });
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        e.getPresentation().setEnabled(project != null && editor != null && psiFile != null);
    }
    
    /**
     * Dialog for getting implementation details.
     */
    private static class ImplementationDialog extends DialogWrapper {
        private final JBTextField implementationNameField;
        private final JBTextField packageNameField;
        private final PsiClass sourceClass;
        
        protected ImplementationDialog(@Nullable Project project, @NotNull PsiClass sourceClass) {
            super(project);
            this.sourceClass = sourceClass;
            
            // Initialize fields
            String suggestedName = suggestImplementationName(sourceClass.getName());
            implementationNameField = new JBTextField(suggestedName);
            
            String suggestedPackage = suggestPackageName(sourceClass.getQualifiedName());
            packageNameField = new JBTextField(suggestedPackage);
            
            setTitle("Generate Implementation");
            init();
        }
        
        /**
         * Suggests an implementation name based on the interface name.
         * @param interfaceName The interface name
         * @return The suggested implementation name
         */
        private String suggestImplementationName(String interfaceName) {
            if (interfaceName == null) {
                return "Implementation";
            }
            
            if (interfaceName.startsWith("I") && Character.isUpperCase(interfaceName.charAt(1))) {
                return interfaceName.substring(1) + "Impl";
            } else {
                return interfaceName + "Impl";
            }
        }
        
        /**
         * Suggests a package name based on the interface's package.
         * @param qualifiedName The qualified name of the interface
         * @return The suggested package name
         */
        private String suggestPackageName(String qualifiedName) {
            if (qualifiedName == null) {
                return "com.example.impl";
            }
            
            int lastDot = qualifiedName.lastIndexOf('.');
            if (lastDot != -1) {
                String packageName = qualifiedName.substring(0, lastDot);
                return packageName + ".impl";
            } else {
                return "impl";
            }
        }
        
        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = FormBuilder.createFormBuilder()
                    .addLabeledComponent("Implementation name:", implementationNameField)
                    .addLabeledComponent("Package name:", packageNameField)
                    .addComponentFillVertically(new JPanel(), 0)
                    .getPanel();
            
            panel.setPreferredSize(new Dimension(400, 100));
            return panel;
        }
        
        @Override
        protected void doOKAction() {
            if (StringUtil.isEmpty(implementationNameField.getText())) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Implementation name cannot be empty",
                        "Error"
                );
                return;
            }
            
            if (StringUtil.isEmpty(packageNameField.getText())) {
                Messages.showErrorDialog(
                        getContentPanel(),
                        "Package name cannot be empty",
                        "Error"
                );
                return;
            }
            
            super.doOKAction();
        }
        
        /**
         * Gets the implementation name.
         * @return The implementation name
         */
        public String getImplementationName() {
            return implementationNameField.getText();
        }
        
        /**
         * Gets the package name.
         * @return The package name
         */
        public String getPackageName() {
            return packageNameField.getText();
        }
    }
}