package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.ai.generation.CodeGenerationParameters;
import com.modforge.intellij.plugin.ai.generation.CodeGenerationResponse;
import com.modforge.intellij.plugin.ai.generation.MinecraftCodeGenerator;
import com.modforge.intellij.plugin.models.GeneratedCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Service for autonomous code generation.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);

    private final Project project;
    private final MinecraftCodeGenerator codeGenerator;

    /**
     * Constructor for the service.
     *
     * @param project The project.
     */
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
        this.codeGenerator = new MinecraftCodeGenerator(project);
    }

    /**
     * Gets an instance of the service for the given project.
     *
     * @param project The project.
     * @return The service instance.
     */
    public static AutonomousCodeGenerationService getInstance(@NotNull Project project) {
        return project.getService(AutonomousCodeGenerationService.class);
    }

    /**
     * Generates code based on the given description.
     *
     * @param description The description of the code to generate.
     * @param className   The name of the class to generate.
     * @return A CompletableFuture that completes with the generated code.
     */
    public CompletableFuture<GeneratedCode> generateCode(@NotNull String description, @Nullable String className) {
        LOG.info("Generating code for: " + description);
        
        // Prepare parameters
        CodeGenerationParameters parameters = new CodeGenerationParameters.Builder()
                .withDescription(description)
                .withClassName(className)
                .build();
        
        // Generate code
        return codeGenerator.generateCode(parameters);
    }

    /**
     * Explains the given code.
     *
     * @param code The code to explain.
     * @param context Additional context information (optional).
     * @return A CompletableFuture that completes with the explanation.
     */
    public CompletableFuture<String> explainCode(@NotNull String code, @Nullable String context) {
        LOG.info("Explaining code");
        
        return codeGenerator.explainCode(code, context);
    }

    /**
     * Fixes compilation errors in the given code.
     *
     * @param code The code to fix.
     * @param errors The compilation errors.
     * @return A CompletableFuture that completes with the fixed code.
     */
    public CompletableFuture<CodeGenerationResponse> fixCode(@NotNull String code, @NotNull String errors) {
        LOG.info("Fixing code errors");
        
        return codeGenerator.fixCode(code, errors);
    }

    /**
     * Generates implementation for the given interface.
     *
     * @param interfaceName The name of the interface to implement.
     * @param packageName The package name.
     * @param implementationName The name of the implementation class.
     * @return A CompletableFuture that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> generateImplementation(
            @NotNull String interfaceName,
            @NotNull String packageName,
            @NotNull String implementationName) {
        
        LOG.info("Generating implementation for: " + interfaceName);
        
        return codeGenerator.generateImplementation(interfaceName, packageName, implementationName);
    }

    /**
     * Generates documentation for the given code.
     *
     * @param code The code to document.
     * @return A CompletableFuture that completes with the documented code.
     */
    public CompletableFuture<String> generateDocumentation(@NotNull String code) {
        LOG.info("Generating documentation");
        
        return codeGenerator.generateDocumentation(code);
    }

    /**
     * Optimizes the given code.
     *
     * @param code The code to optimize.
     * @param instructions Additional optimization instructions.
     * @return A CompletableFuture that completes with the optimized code.
     */
    public CompletableFuture<String> optimizeCode(@NotNull String code, @Nullable String instructions) {
        LOG.info("Optimizing code");
        
        return codeGenerator.optimizeCode(code, instructions);
    }
}