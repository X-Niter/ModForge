package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.dialogs.CodeGenerationDialog;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Action for generating code using the ModForge AI.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();

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
            // Convert String to VirtualFile if needed
            VirtualFile packageVirtualFile = CompatibilityUtil.getModFileByRelativePath(project,
                    "src/main/java/" + targetPackage.replace('.', '/'));

            AutonomousCodeGenerationService.getInstance(project)
                    .generateModuleCode(description, packageVirtualFile, moduleType)
                    .thenAccept(generatedCode -> {
                        if (generatedCode != null && !generatedCode.isEmpty()) {
                            CompatibilityUtil.runOnUiThread(() -> {
                                // Show the generated code in a dialog
                                showGeneratedCodeDialog(project, generatedCode);

                                // Notify success
                                notificationService.showInfoNotification(
                                        project,
                                        "Code Generated",
                                        "Successfully generated code from description: " + description);
                            });
                        } else {
                            CompatibilityUtil.runOnUiThread(() -> {
                                notificationService.showErrorNotification(
                                        project,
                                        "Code Generation Failed",
                                        "Failed to generate code from description: " + description);
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
        if (project == null)
            return "com.example.mod";

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
        VirtualFile forgeToml = CompatibilityUtil.getModFileByRelativePath(project,
                "src/main/resources/META-INF/mods.toml");
        if (forgeToml != null && forgeToml.exists()) {
            return "forge";
        }

        // Check for Fabric markers
        VirtualFile fabricModJson = CompatibilityUtil.getModFileByRelativePath(project,
                "src/main/resources/fabric.mod.json");
        if (fabricModJson != null && fabricModJson.exists()) {
            return "fabric";
        }

        // Check for Quilt markers
        VirtualFile quiltModJson = CompatibilityUtil.getModFileByRelativePath(project,
                "src/main/resources/quilt.mod.json");
        if (quiltModJson != null && quiltModJson.exists()) {
            return "quilt";
        }

        // Default to Forge
        return "forge";
    }

    /**
     * Shows a dialog with the generated code.
     *
     * @param project       The project.
     * @param generatedCode The generated code.
     */
    private void showGeneratedCodeDialog(@NotNull Project project, @NotNull String generatedCode) {
        try {
            DialogWrapper dialog = new DialogWrapper(project) {
                private JTextArea codeArea;

                {
                    init();
                    setTitle("Generated Code");
                }

                @Override
                protected @NotNull JComponent createCenterPanel() {
                    JBPanel<?> panel = new JBPanel<>(new BorderLayout());
                    panel.setBorder(JBUI.Borders.empty(10));

                    codeArea = new JTextArea(generatedCode);
                    codeArea.setEditable(true);
                    JScrollPane scrollPane = new JScrollPane(codeArea);
                    panel.add(scrollPane, BorderLayout.CENTER);

                    return panel;
                }

                @Override
                protected void doOKAction() {
                    String fileName = "GeneratedCode.java"; // Default filename
                    saveGeneratedCode(project, codeArea.getText(), fileName);
                    super.doOKAction();
                }
            };

            dialog.show();
        } catch (Exception e) {
            LOG.error("Error showing dialog", e);
            ModForgeNotificationService.getInstance().showErrorNotification(
                    project,
                    "Error",
                    "Failed to show code dialog: " + e.getMessage());
        }
    }

    /**
     * Saves the generated code to a file.
     *
     * @param project The project.
     * @param code    The generated code.
     */
    private void saveGeneratedCode(@NotNull Project project, @NotNull String code, String suggestedFileName) {
        // Parse the generated code to determine class name and package
        String className = parseClassName(code);

        // Save dialog
        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
        String fileName;

        if (suggestedFileName != null && !suggestedFileName.trim().isEmpty()) {
            fileName = suggestedFileName;
        } else if (notificationService != null) {
            fileName = notificationService.showInputDialog(
                    project,
                    "Save Generated Code",
                    "Enter file name:",
                    className + ".java");
        } else {
            fileName = CompatibilityUtil.showInputDialogWithProject(
                    project,
                    "Enter file name:",
                    "Save Generated Code",
                    className + ".java");
        }

        if (fileName == null)
            return;

        // Get target directory
        VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
        if (baseDir == null) {
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Error",
                        "Could not find project base directory.");
            } else {
                com.modforge.intellij.plugin.utils.CompatibilityUtil.showErrorDialog(project,
                        "Could not find project base directory.", "Error");
            }
            return;
        }

        try {
            // Find or create the target directory
            String packagePath = "src/main/java/" + parsePackagePath(code);
            VirtualFile targetDir = CompatibilityUtil.computeInWriteAction(() -> {
                VirtualFile dir = baseDir;
                for (String pathComponent : packagePath.split("/")) {
                    if (pathComponent.isEmpty())
                        continue;

                    VirtualFile child = null;
                    try {
                        child = dir.createChildDirectory(this, pathComponent);
                    } catch (IOException e) {
                        LOG.error("Error creating directory: " + e.getMessage(), e);
                        throw new RuntimeException("Error creating directory", e);
                    }
                    if (child == null) {
                        throw new RuntimeException("Failed to create directory");
                    }
                    dir = child;
                }
                return dir;
            });

            // Create the file
            VirtualFile file = null;
            try {
                file = targetDir.createChildData(this, fileName);
            } catch (IOException e) {
                LOG.error("Error creating file: " + e.getMessage(), e);
                throw new RuntimeException("Error creating file", e);
            }

            // Write content
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                // Using CompatibilityUtil for better compatibility with IntelliJ IDEA
                // 2025.1.1.1
                CompatibilityUtil.runWriteAction(() -> {
                    document.setText(code);
                    FileDocumentManager.getInstance().saveDocument(document);
                });

                // Open the file
                CompatibilityUtil.openFileInEditor(project, file, true);

                ModForgeNotificationService.getInstance().showInfoNotification(
                        project,
                        "Code Saved",
                        "Generated code saved to " + fileName);
            }
        } catch (Exception e) {
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Error",
                        "Error saving file: " + e.getMessage());
            } else {
                CompatibilityUtil.showErrorDialog(project, "Error saving file: " + e.getMessage(), "Error");
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