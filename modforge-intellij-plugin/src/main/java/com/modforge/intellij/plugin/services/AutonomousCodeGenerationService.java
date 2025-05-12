package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import com.modforge.intellij.plugin.utils.VirtualFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service for autonomous code generation in the ModForge plugin.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final ModForgeSettings settings;
    private final ModForgeNotificationService notificationService;
    
    private final Map<String, AtomicInteger> requestCounts = new HashMap<>();
    private final AtomicBoolean isGenerating = new AtomicBoolean(false);

    /**
     * Creates a new instance of the autonomous code generation service.
     *
     * @param project The project.
     */
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance(project);
        LOG.info("AutonomousCodeGenerationService initialized for project: " + project.getName());
    }

    /**
     * Gets the instance of the autonomous code generation service for the specified project.
     *
     * @param project The project.
     * @return The autonomous code generation service.
     */
    public static AutonomousCodeGenerationService getInstance(@NotNull Project project) {
        return project.getService(AutonomousCodeGenerationService.class);
    }

    /**
     * Generates code based on a prompt.
     *
     * @param prompt The prompt.
     * @param contextFile The context file.
     * @param language The programming language.
     * @return A future with the generated code.
     */
    @NotNull
    public CompletableFuture<String> generateCode(
            @NotNull String prompt,
            @Nullable String contextFile,
            @NotNull String language) {
        
        LOG.info("Generating code with prompt: " + prompt);
        
        CompletableFuture<String> result = new CompletableFuture<>();
        
        if (isGenerating.getAndSet(true)) {
            LOG.info("Already generating code, queueing request");
            ThreadUtils.runWithDelay(() -> {
                generateCode(prompt, contextFile, language).thenAccept(result::complete);
            }, 1, TimeUnit.SECONDS);
            return result;
        }
        
        try {
            ProgressManager.getInstance().run(new Task.Backgroundable(
                    project,
                    "Generating Code",
                    true) {
                
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setIndeterminate(false);
                        indicator.setText("Analyzing prompt...");
                        indicator.setFraction(0.1);
                        
                        // Track request count for this prompt type
                        String promptType = getPromptType(prompt);
                        requestCounts.computeIfAbsent(promptType, k -> new AtomicInteger(0))
                                .incrementAndGet();
                        
                        // Use pattern recognition if enabled
                        boolean usePatterns = settings.isPatternRecognition();
                        
                        indicator.setText("Generating code...");
                        indicator.setFraction(0.3);
                        
                        // Mock code generation (in real implementation, call API)
                        String generatedCode = generateMockCode(prompt, language);
                        
                        indicator.setText("Formatting code...");
                        indicator.setFraction(0.7);
                        
                        // Format the code
                        String formattedCode = formatCode(generatedCode, language);
                        
                        indicator.setText("Analyzing generated code...");
                        indicator.setFraction(0.9);
                        
                        // Validate the code
                        boolean isValid = validateCode(formattedCode, language);
                        
                        indicator.setText("Code generation complete");
                        indicator.setFraction(1.0);
                        
                        if (isValid) {
                            LOG.info("Successfully generated code");
                            CompatibilityUtil.executeOnUiThread(() -> {
                                notificationService.showInfo("Code Generation", "Successfully generated code");
                            });
                            result.complete(formattedCode);
                        } else {
                            LOG.warn("Generated code is invalid");
                            CompatibilityUtil.executeOnUiThread(() -> {
                                notificationService.showWarning("Code Generation", "Generated code might have issues");
                            });
                            result.complete(formattedCode);
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to generate code", e);
                        CompatibilityUtil.executeOnUiThread(() -> {
                            notificationService.showError("Code Generation Failed", e.getMessage());
                        });
                        result.completeExceptionally(e);
                    } finally {
                        isGenerating.set(false);
                    }
                }
                
                @Override
                public void onCancel() {
                    LOG.info("Code generation cancelled");
                    isGenerating.set(false);
                    result.cancel(true);
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to start code generation task", e);
            isGenerating.set(false);
            result.completeExceptionally(e);
        }
        
        return result;
    }

    /**
     * Fixes code with errors.
     *
     * @param code The code to fix.
     * @param errorMessage The error message.
     * @param language The programming language.
     * @return A future with the fixed code.
     */
    @NotNull
    public CompletableFuture<String> fixCode(
            @NotNull String code,
            @NotNull String errorMessage,
            @NotNull String language) {
        
        LOG.info("Fixing code with error: " + errorMessage);
        
        CompletableFuture<String> result = new CompletableFuture<>();
        
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Fixing Code",
                true) {
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setText("Analyzing error...");
                    indicator.setFraction(0.1);
                    
                    // Use pattern recognition if enabled
                    boolean usePatterns = settings.isPatternRecognition();
                    
                    indicator.setText("Fixing code...");
                    indicator.setFraction(0.5);
                    
                    // Mock code fixing (in real implementation, call API)
                    String fixedCode = fixMockCode(code, errorMessage, language);
                    
                    indicator.setText("Code fixing complete");
                    indicator.setFraction(1.0);
                    
                    LOG.info("Successfully fixed code");
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showInfo("Code Fixed", "Successfully fixed code error");
                    });
                    result.complete(fixedCode);
                } catch (Exception e) {
                    LOG.error("Failed to fix code", e);
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showError("Code Fix Failed", e.getMessage());
                    });
                    result.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancel() {
                LOG.info("Code fixing cancelled");
                result.cancel(true);
            }
        });
        
        return result;
    }

    /**
     * Enhances code with additional features.
     *
     * @param code The code to enhance.
     * @param instructions The enhancement instructions.
     * @param language The programming language.
     * @return A future with the enhanced code.
     */
    @NotNull
    public CompletableFuture<String> enhanceCode(
            @NotNull String code,
            @NotNull String instructions,
            @NotNull String language) {
        
        LOG.info("Enhancing code with instructions: " + instructions);
        
        CompletableFuture<String> result = new CompletableFuture<>();
        
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Enhancing Code",
                true) {
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setText("Analyzing code...");
                    indicator.setFraction(0.1);
                    
                    // Use pattern recognition if enabled
                    boolean usePatterns = settings.isPatternRecognition();
                    
                    indicator.setText("Enhancing code...");
                    indicator.setFraction(0.5);
                    
                    // Mock code enhancement (in real implementation, call API)
                    String enhancedCode = enhanceMockCode(code, instructions, language);
                    
                    indicator.setText("Code enhancement complete");
                    indicator.setFraction(1.0);
                    
                    LOG.info("Successfully enhanced code");
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showInfo("Code Enhanced", "Successfully enhanced code");
                    });
                    result.complete(enhancedCode);
                } catch (Exception e) {
                    LOG.error("Failed to enhance code", e);
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showError("Code Enhancement Failed", e.getMessage());
                    });
                    result.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancel() {
                LOG.info("Code enhancement cancelled");
                result.cancel(true);
            }
        });
        
        return result;
    }

    /**
     * Explains code.
     *
     * @param code The code to explain.
     * @param language The programming language.
     * @return A future with the explanation.
     */
    @NotNull
    public CompletableFuture<String> explainCode(
            @NotNull String code,
            @Nullable String language) {
        
        LOG.info("Explaining code");
        
        CompletableFuture<String> result = new CompletableFuture<>();
        
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Explaining Code",
                true) {
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setText("Analyzing code...");
                    indicator.setFraction(0.1);
                    
                    // Use pattern recognition if enabled
                    boolean usePatterns = settings.isPatternRecognition();
                    
                    indicator.setText("Generating explanation...");
                    indicator.setFraction(0.5);
                    
                    // Mock code explanation (in real implementation, call API)
                    String explanation = explainMockCode(code, language);
                    
                    indicator.setText("Code explanation complete");
                    indicator.setFraction(1.0);
                    
                    LOG.info("Successfully explained code");
                    result.complete(explanation);
                } catch (Exception e) {
                    LOG.error("Failed to explain code", e);
                    result.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancel() {
                LOG.info("Code explanation cancelled");
                result.cancel(true);
            }
        });
        
        return result;
    }

    /**
     * Generates documentation for code.
     *
     * @param code The code to document.
     * @param language The programming language.
     * @return A future with the documented code.
     */
    @NotNull
    public CompletableFuture<String> generateDocumentation(
            @NotNull String code,
            @Nullable String language) {
        
        LOG.info("Generating documentation for code");
        
        CompletableFuture<String> result = new CompletableFuture<>();
        
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Documenting Code",
                true) {
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setText("Analyzing code...");
                    indicator.setFraction(0.1);
                    
                    // Use pattern recognition if enabled
                    boolean usePatterns = settings.isPatternRecognition();
                    
                    indicator.setText("Generating documentation...");
                    indicator.setFraction(0.5);
                    
                    // Mock code documentation (in real implementation, call API)
                    String documentedCode = documentMockCode(code, language);
                    
                    indicator.setText("Documentation generation complete");
                    indicator.setFraction(1.0);
                    
                    LOG.info("Successfully documented code");
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showInfo("Documentation Generated", "Successfully added documentation to code");
                    });
                    result.complete(documentedCode);
                } catch (Exception e) {
                    LOG.error("Failed to document code", e);
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showError("Documentation Generation Failed", e.getMessage());
                    });
                    result.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancel() {
                LOG.info("Documentation generation cancelled");
                result.cancel(true);
            }
        });
        
        return result;
    }

    /**
     * Generates an implementation for an interface or abstract class.
     *
     * @param interfaceCode The interface or abstract class code.
     * @param className The name of the class to generate.
     * @param packageName The package name.
     * @return Whether the implementation was generated successfully.
     */
    public boolean generateImplementation(
            @NotNull String interfaceCode,
            @NotNull String className,
            @NotNull String packageName) {
        
        LOG.info("Generating implementation for: " + className);
        
        try {
            String projectPath = project.getBasePath();
            if (projectPath == null) {
                LOG.error("Project base path is null");
                return false;
            }
            
            // Create the package directory
            String packagePath = packageName.replace('.', '/');
            String fullPath = projectPath + "/src/main/java/" + packagePath;
            
            PsiDirectory directory = VirtualFileUtil.createDirectoryRecursively(
                    project,
                    projectPath,
                    "src/main/java/" + packagePath);
            
            if (directory == null) {
                LOG.error("Failed to create directory: " + fullPath);
                return false;
            }
            
            // Generate the implementation code
            String implementationCode = generateImplementationCode(interfaceCode, className, packageName);
            
            // Create the file
            String fileName = className + ".java";
            PsiFile file = VirtualFileUtil.createFile(directory, fileName, implementationCode);
            
            if (file == null) {
                LOG.error("Failed to create file: " + fileName);
                return false;
            }
            
            // Open the file in the editor
            CompatibilityUtil.executeOnUiThread(() -> {
                FileEditorManager.getInstance(project).openFile(file.getVirtualFile(), true);
                notificationService.showInfo("Implementation Generated", "Created " + className + " in " + packageName);
            });
            
            return true;
        } catch (Exception e) {
            LOG.error("Failed to generate implementation", e);
            CompatibilityUtil.executeOnUiThread(() -> {
                notificationService.showError("Implementation Generation Failed", e.getMessage());
            });
            return false;
        }
    }

    /**
     * Adds features to code.
     *
     * @param code The code to add features to.
     * @param featureDescription The feature description.
     * @param language The programming language.
     * @return A future with the code with features added.
     */
    @NotNull
    public CompletableFuture<String> addFeatures(
            @NotNull String code,
            @NotNull String featureDescription,
            @NotNull String language) {
        
        LOG.info("Adding features to code: " + featureDescription);
        
        CompletableFuture<String> result = new CompletableFuture<>();
        
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Adding Features",
                true) {
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setText("Analyzing code...");
                    indicator.setFraction(0.1);
                    
                    // Use pattern recognition if enabled
                    boolean usePatterns = settings.isPatternRecognition();
                    
                    indicator.setText("Adding features...");
                    indicator.setFraction(0.5);
                    
                    // Mock feature addition (in real implementation, call API)
                    String codeWithFeatures = addMockFeatures(code, featureDescription, language);
                    
                    indicator.setText("Feature addition complete");
                    indicator.setFraction(1.0);
                    
                    LOG.info("Successfully added features to code");
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showInfo("Features Added", "Successfully added features to code");
                    });
                    result.complete(codeWithFeatures);
                } catch (Exception e) {
                    LOG.error("Failed to add features to code", e);
                    CompatibilityUtil.executeOnUiThread(() -> {
                        notificationService.showError("Feature Addition Failed", e.getMessage());
                    });
                    result.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancel() {
                LOG.info("Feature addition cancelled");
                result.cancel(true);
            }
        });
        
        return result;
    }

    /**
     * Gets the prompt type from a prompt.
     *
     * @param prompt The prompt.
     * @return The prompt type.
     */
    @NotNull
    private String getPromptType(@NotNull String prompt) {
        // Simple heuristic to categorize prompts
        if (prompt.toLowerCase().contains("bug") || prompt.toLowerCase().contains("fix")) {
            return "bug_fix";
        } else if (prompt.toLowerCase().contains("feature") || prompt.toLowerCase().contains("add")) {
            return "feature_request";
        } else if (prompt.toLowerCase().contains("optimize") || prompt.toLowerCase().contains("performance")) {
            return "optimization";
        } else if (prompt.toLowerCase().contains("refactor") || prompt.toLowerCase().contains("clean")) {
            return "refactoring";
        } else {
            return "general";
        }
    }

    /**
     * Formats code.
     *
     * @param code The code to format.
     * @param language The programming language.
     * @return The formatted code.
     */
    @NotNull
    private String formatCode(@NotNull String code, @NotNull String language) {
        // In a real implementation, use a language-specific formatter
        return code;
    }

    /**
     * Validates code.
     *
     * @param code The code to validate.
     * @param language The programming language.
     * @return Whether the code is valid.
     */
    private boolean validateCode(@NotNull String code, @NotNull String language) {
        // In a real implementation, use a language-specific validator
        return true;
    }

    /**
     * Generates mock code for testing.
     *
     * @param prompt The prompt.
     * @param language The programming language.
     * @return The generated code.
     */
    @NotNull
    private String generateMockCode(@NotNull String prompt, @NotNull String language) {
        // In a real implementation, call the AI API to generate code
        if (language.equalsIgnoreCase("java")) {
            return "/**\n * Generated code based on: " + prompt + "\n */\npublic class GeneratedClass {\n    \n    public static void main(String[] args) {\n        System.out.println(\"Hello, world!\");\n    }\n}";
        } else if (language.equalsIgnoreCase("python")) {
            return "# Generated code based on: " + prompt + "\n\ndef main():\n    print(\"Hello, world!\")\n\nif __name__ == \"__main__\":\n    main()";
        } else {
            return "// Generated code based on: " + prompt + "\n\nconsole.log(\"Hello, world!\");";
        }
    }

    /**
     * Fixes mock code for testing.
     *
     * @param code The code to fix.
     * @param errorMessage The error message.
     * @param language The programming language.
     * @return The fixed code.
     */
    @NotNull
    private String fixMockCode(@NotNull String code, @NotNull String errorMessage, @NotNull String language) {
        // In a real implementation, call the AI API to fix code
        return "/**\n * Fixed code\n * Original error: " + errorMessage + "\n */\n" + code.replace("System.out.println", "System.out.print");
    }

    /**
     * Enhances mock code for testing.
     *
     * @param code The code to enhance.
     * @param instructions The enhancement instructions.
     * @param language The programming language.
     * @return The enhanced code.
     */
    @NotNull
    private String enhanceMockCode(@NotNull String code, @NotNull String instructions, @NotNull String language) {
        // In a real implementation, call the AI API to enhance code
        return "/**\n * Enhanced code based on: " + instructions + "\n */\n" + code;
    }

    /**
     * Explains mock code for testing.
     *
     * @param code The code to explain.
     * @param language The programming language.
     * @return The explanation.
     */
    @NotNull
    private String explainMockCode(@NotNull String code, @Nullable String language) {
        // In a real implementation, call the AI API to explain code
        return "# Code Explanation\n\nThis code defines a simple program that prints \"Hello, world!\" to the console.\n\n## Details\n\n- It contains a main method which is the entry point of the program\n- It uses standard output to display the message";
    }

    /**
     * Documents mock code for testing.
     *
     * @param code The code to document.
     * @param language The programming language.
     * @return The documented code.
     */
    @NotNull
    private String documentMockCode(@NotNull String code, @Nullable String language) {
        // In a real implementation, call the AI API to document code
        if (code.contains("class")) {
            return code.replace("public class", "/**\n * This class demonstrates a simple Java program.\n */\npublic class");
        } else {
            return "/**\n * This is an auto-generated documentation.\n */\n" + code;
        }
    }

    /**
     * Generates mock implementation code for testing.
     *
     * @param interfaceCode The interface or abstract class code.
     * @param className The name of the class to generate.
     * @param packageName The package name.
     * @return The implementation code.
     */
    @NotNull
    private String generateImplementationCode(
            @NotNull String interfaceCode,
            @NotNull String className,
            @NotNull String packageName) {
        
        // In a real implementation, call the AI API to generate implementation
        return "package " + packageName + ";\n\n/**\n * Implementation of the interface.\n */\npublic class " + className + " implements SomeInterface {\n    \n    @Override\n    public void someMethod() {\n        // Implementation\n    }\n}";
    }

    /**
     * Adds mock features to code for testing.
     *
     * @param code The code to add features to.
     * @param featureDescription The feature description.
     * @param language The programming language.
     * @return The code with features added.
     */
    @NotNull
    private String addMockFeatures(@NotNull String code, @NotNull String featureDescription, @NotNull String language) {
        // In a real implementation, call the AI API to add features
        return "/**\n * With added features: " + featureDescription + "\n */\n" + code;
    }
}