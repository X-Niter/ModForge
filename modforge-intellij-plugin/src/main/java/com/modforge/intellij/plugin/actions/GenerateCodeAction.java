package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.ui.CodeGenerationDialog;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Action for generating code using the ModForge AI.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class GenerateCodeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        AutonomousCodeGenerationService codeGenerationService = AutonomousCodeGenerationService.getInstance(project);
        
        // Determine package and module type
        String targetPackage = getTargetPackage(e);
        String moduleType = determineModuleType(project);
        
        // Create and show dialog
        CodeGenerationDialog dialog = new CodeGenerationDialog(project, targetPackage, moduleType);
        if (dialog.showAndGet()) {
            // Get user input
            String description = dialog.getDescription();
            targetPackage = dialog.getTargetPackage();
            moduleType = dialog.getModuleType();
            
            // Generate code
            codeGenerationService.generateCode(description, targetPackage, moduleType)
                    .thenAccept(generatedCode -> {
                        if (generatedCode != null && !generatedCode.isEmpty()) {
                            CompatibilityUtil.executeOnUiThread(() -> {
                                // Show the generated code in a dialog
                                showGeneratedCodeDialog(project, generatedCode, description);
                                
                                // Notify success
                                notificationService.showInfo(
                                        "Code Generated",
                                        "Successfully generated code from description: " + description
                                );
                            });
                        } else {
                            CompatibilityUtil.executeOnUiThread(() -> {
                                notificationService.showError(
                                        "Code Generation Failed",
                                        "Failed to generate code from description: " + description
                                );
                            });
                        }
                    });
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable in projects
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
    
    /**
     * Determines the target package based on the current file.
     *
     * @param e The action event.
     * @return The target package name.
     */
    private String getTargetPackage(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return "com.example.mod";
        
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile instanceof PsiJavaFile) {
            return ((PsiJavaFile) psiFile).getPackageName();
        }
        
        // Default package
        return "com.example.mod";
    }
    
    /**
     * Determines the module type (Forge, Fabric, etc.) for the project.
     *
     * @param project The project.
     * @return The module type.
     */
    private String determineModuleType(@NotNull Project project) {
        // Check for Forge markers
        VirtualFile forgeToml = CompatibilityUtil.getModFileByRelativePath(project, "src/main/resources/META-INF/mods.toml");
        if (forgeToml != null && forgeToml.exists()) {
            return "forge";
        }
        
        // Check for Fabric markers
        VirtualFile fabricModJson = CompatibilityUtil.getModFileByRelativePath(project, "src/main/resources/fabric.mod.json");
        if (fabricModJson != null && fabricModJson.exists()) {
            return "fabric";
        }
        
        // Check for Quilt markers
        VirtualFile quiltModJson = CompatibilityUtil.getModFileByRelativePath(project, "src/main/resources/quilt.mod.json");
        if (quiltModJson != null && quiltModJson.exists()) {
            return "quilt";
        }
        
        // Default to Forge
        return "forge";
    }
    
    /**
     * Shows a dialog with the generated code.
     *
     * @param project The project.
     * @param generatedCode The generated code.
     * @param description The description used to generate the code.
     */
    private void showGeneratedCodeDialog(@NotNull Project project, @NotNull String generatedCode, @NotNull String description) {
        DialogWrapper dialog = new DialogWrapper(project, false) {
            private JTextArea codeArea;
            
            {
                init();
                setTitle("Generated Code for: " + description);
            }
            
            @Override
            protected JComponent createCenterPanel() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBorder(JBUI.Borders.empty(10));
                
                codeArea = new JTextArea(generatedCode);
                codeArea.setEditable(true);
                codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                
                JScrollPane scrollPane = new JScrollPane(codeArea);
                scrollPane.setPreferredSize(new Dimension(800, 600));
                
                panel.add(scrollPane, BorderLayout.CENTER);
                
                return panel;
            }
            
            @Override
            protected JComponent createSouthPanel() {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton saveButton = new JButton("Save to File...");
                saveButton.addActionListener(e -> {
                    saveGeneratedCode(project, codeArea.getText());
                    close(0);
                });
                
                JButton copyButton = new JButton("Copy to Clipboard");
                copyButton.addActionListener(e -> {
                    codeArea.selectAll();
                    codeArea.copy();
                });
                
                panel.add(copyButton);
                panel.add(saveButton);
                panel.add(super.createSouthPanel());
                
                return panel;
            }
        };
        
        dialog.show();
    }
    
    /**
     * Saves the generated code to a file.
     *
     * @param project The project.
     * @param code The generated code.
     */
    private void saveGeneratedCode(@NotNull Project project, @NotNull String code) {
        // Parse the generated code to determine class name and package
        String className = parseClassName(code);
        
        // Save dialog
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        String fileName;
        
        if (notificationService != null) {
            fileName = notificationService.showInputDialog(
                    project,
                    "Save Generated Code",
                    "Enter file name:",
                    className + ".java"
            );
        } else {
            fileName = Messages.showInputDialog(
                    project,
                    "Enter file name:",
                    "Save Generated Code",
                    null,
                    className + ".java",
                    null
            );
        }
        
        if (fileName == null) return;
        
        // Get target directory
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Error",
                        "Could not find project base directory."
                );
            } else {
                Messages.showErrorDialog(project, "Could not find project base directory.", "Error");
            }
            return;
        }
        
        try {
            // Find or create the target directory
            String packagePath = "src/main/java/" + parsePackagePath(code);
            VirtualFile targetDir = CompatibilityUtil.computeInWriteAction(() -> {
                VirtualFile dir = baseDir;
                for (String pathComponent : packagePath.split("/")) {
                    if (pathComponent.isEmpty()) continue;
                    
                    VirtualFile child = dir.findChild(pathComponent);
                    if (child == null) {
                        child = dir.createChildDirectory(this, pathComponent);
                    }
                    dir = child;
                }
                return dir;
            });
            
            // Create the file
            VirtualFile file = CompatibilityUtil.computeInWriteAction(() -> 
                targetDir.createChildData(this, fileName)
            );
            
            // Write content
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    document.setText(code);
                    FileDocumentManager.getInstance().saveDocument(document);
                });
                
                // Open the file
                CompatibilityUtil.openFileInEditor(project, file, true);
                
                ModForgeNotificationService.getInstance().showInfo(
                        "Code Saved",
                        "Generated code saved to " + fileName
                );
            }
        } catch (Exception e) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Error",
                        "Error saving file: " + e.getMessage()
                );
            } else {
                Messages.showErrorDialog(project, "Error saving file: " + e.getMessage(), "Error");
            }
        }
    }
    
    /**
     * Parses the class name from the generated code.
     *
     * @param code The generated code.
     * @return The parsed class name, or a default.
     */
    private String parseClassName(@NotNull String code) {
        // Simple parsing for class/interface/enum name
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("class ") || line.contains("interface ") || line.contains("enum ")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("class") || parts[i].equals("interface") || parts[i].equals("enum")) {
                        String name = parts[i + 1];
                        // Remove any generics or implements/extends
                        int genericStart = name.indexOf('<');
                        int implementsStart = name.indexOf("implements");
                        int extendsStart = name.indexOf("extends");
                        
                        if (genericStart > 0) {
                            name = name.substring(0, genericStart);
                        } else if (implementsStart > 0) {
                            name = name.substring(0, implementsStart);
                        } else if (extendsStart > 0) {
                            name = name.substring(0, extendsStart);
                        }
                        
                        return name.trim();
                    }
                }
            }
        }
        
        return "GeneratedClass";
    }
    
    /**
     * Parses the package path from the generated code.
     *
     * @param code The generated code.
     * @return The parsed package path.
     */
    private String parsePackagePath(@NotNull String code) {
        // Extract package declaration
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ")) {
                String packageName = line.substring("package ".length(), line.indexOf(';'));
                return packageName.replace('.', '/');
            }
        }
        
        return "com/example/mod";
    }
}