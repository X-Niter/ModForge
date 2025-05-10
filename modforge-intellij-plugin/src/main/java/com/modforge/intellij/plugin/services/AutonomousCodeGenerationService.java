package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Service for autonomous code generation.
 * Provides methods for generating, enhancing, and fixing code using AI.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    private final Project project;
    private final ExecutorService executor;
    
    /**
     * Construct the autonomous code generation service.
     *
     * @param project The project
     */
    public AutonomousCodeGenerationService(@NotNull Project project) {
        this.project = project;
        this.executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("ModForge Code Generation", 2);
    }
    
    /**
     * Generate code from a prompt.
     *
     * @param prompt The prompt
     * @return A future that completes with the generated code, or null if generation fails
     */
    @NotNull
    public CompletableFuture<String> generateCode(@NotNull String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if authenticated
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                if (!authManager.isAuthenticated()) {
                    LOG.warn("Not authenticated");
                    return null;
                }
                
                // Call API to generate code
                String requestBody = String.format("{\"prompt\":\"%s\"}", 
                        prompt.replace("\"", "\\\"").replace("\n", "\\n"));
                String response = TokenAuthConnectionUtil.executePost("/api/code/generate", requestBody);
                
                if (response != null && !response.isEmpty()) {
                    // Try to extract code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
                
                LOG.warn("Code generation failed");
                return null;
            } catch (Exception e) {
                LOG.error("Error generating code", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * Fix code errors.
     *
     * @param code   The code with errors
     * @param errors The error messages
     * @return A future that completes with the fixed code, or null if fixing fails
     */
    @NotNull
    public CompletableFuture<String> fixCode(@NotNull String code, @NotNull String errors) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if authenticated
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                if (!authManager.isAuthenticated()) {
                    LOG.warn("Not authenticated");
                    return null;
                }
                
                // Call API to fix code
                String requestBody = String.format("{\"code\":\"%s\",\"errors\":\"%s\"}", 
                        code.replace("\"", "\\\"").replace("\n", "\\n"),
                        errors.replace("\"", "\\\"").replace("\n", "\\n"));
                String response = TokenAuthConnectionUtil.executePost("/api/code/fix", requestBody);
                
                if (response != null && !response.isEmpty()) {
                    // Try to extract fixed code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
                
                LOG.warn("Code fixing failed");
                return null;
            } catch (Exception e) {
                LOG.error("Error fixing code", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * Enhance code with improvements.
     *
     * @param code The code to enhance
     * @return A future that completes with the enhanced code, or null if enhancement fails
     */
    @NotNull
    public CompletableFuture<String> enhanceCode(@NotNull String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if authenticated
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                if (!authManager.isAuthenticated()) {
                    LOG.warn("Not authenticated");
                    return null;
                }
                
                // Call API to enhance code
                String requestBody = String.format("{\"code\":\"%s\"}", 
                        code.replace("\"", "\\\"").replace("\n", "\\n"));
                String response = TokenAuthConnectionUtil.executePost("/api/code/enhance", requestBody);
                
                if (response != null && !response.isEmpty()) {
                    // Try to extract enhanced code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
                
                LOG.warn("Code enhancement failed");
                return null;
            } catch (Exception e) {
                LOG.error("Error enhancing code", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * Add features to code.
     *
     * @param code        The code to add features to
     * @param description The description of the features to add
     * @return A future that completes with the updated code, or null if feature addition fails
     */
    @NotNull
    public CompletableFuture<String> addFeatures(@NotNull String code, @NotNull String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if authenticated
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                if (!authManager.isAuthenticated()) {
                    LOG.warn("Not authenticated");
                    return null;
                }
                
                // Call API to add features
                String requestBody = String.format("{\"code\":\"%s\",\"description\":\"%s\"}", 
                        code.replace("\"", "\\\"").replace("\n", "\\n"),
                        description.replace("\"", "\\\"").replace("\n", "\\n"));
                String response = TokenAuthConnectionUtil.executePost("/api/code/add-features", requestBody);
                
                if (response != null && !response.isEmpty()) {
                    // Try to extract updated code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
                
                LOG.warn("Feature addition failed");
                return null;
            } catch (Exception e) {
                LOG.error("Error adding features", e);
                return null;
            }
        }, executor);
    }
    
    /**
     * Generate documentation for code.
     *
     * @param code The code to generate documentation for
     * @return A future that completes with the documentation, or null if documentation generation fails
     */
    @NotNull
    public CompletableFuture<String> generateDocumentation(@NotNull String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if authenticated
                ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
                if (!authManager.isAuthenticated()) {
                    LOG.warn("Not authenticated");
                    return null;
                }
                
                // Call API to generate documentation
                String requestBody = String.format("{\"code\":\"%s\"}", 
                        code.replace("\"", "\\\"").replace("\n", "\\n"));
                String response = TokenAuthConnectionUtil.executePost("/api/code/document", requestBody);
                
                if (response != null && !response.isEmpty()) {
                    // Try to extract documentation from response
                    if (response.contains("\"documentation\":")) {
                        return response.split("\"documentation\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
                
                LOG.warn("Documentation generation failed");
                return null;
            } catch (Exception e) {
                LOG.error("Error generating documentation", e);
                return null;
            }
        }, executor);
    }
}