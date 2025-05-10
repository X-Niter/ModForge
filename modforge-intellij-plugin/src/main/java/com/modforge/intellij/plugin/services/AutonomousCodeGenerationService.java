package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for generating code autonomously using AI.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    private static final Gson GSON = new Gson();
    
    private final Project project;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    
    /**
     * Create a new autonomous code generation service.
     *
     * @param project The project
     */
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
    }
    
    /**
     * Generate code from a prompt.
     *
     * @param prompt The prompt
     * @return A future with the generated code
     */
    @NotNull
    public CompletableFuture<String> generateCode(@NotNull String prompt) {
        // Create a future to return
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.error("Not authenticated");
                future.complete(null);
                return future;
            }
            
            // Check if we've hit the daily request limit
            ModForgeSettings settings = ModForgeSettings.getInstance();
            int maxRequests = settings.getMaxApiRequestsPerDay();
            int currentRequests = requestCount.get();
            
            if (currentRequests >= maxRequests) {
                LOG.error("Daily request limit reached");
                future.complete(null);
                return future;
            }
            
            // Get the server URL
            String serverUrl = settings.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set");
                future.complete(null);
                return future;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null");
                future.complete(null);
                return future;
            }
            
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("prompt", prompt);
            
            // Send the request
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/ai/generate-code",
                    "POST",
                    token,
                    requestBody.toString()
            ).thenAccept(response -> {
                if (response != null) {
                    // Parse the response
                    try {
                        JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
                        String code = jsonResponse.get("code").getAsString();
                        
                        // Store the pattern
                        PatternRecognitionService patternService = project.getService(PatternRecognitionService.class);
                        patternService.storeCodeGenerationPattern(prompt, code);
                        
                        // Increment the request count
                        requestCount.incrementAndGet();
                        
                        // Complete the future
                        future.complete(code);
                    } catch (Exception e) {
                        LOG.error("Error parsing response", e);
                        future.complete(null);
                    }
                } else {
                    LOG.error("Failed to generate code");
                    future.complete(null);
                }
            }).exceptionally(ex -> {
                LOG.error("Exception generating code", ex);
                future.complete(null);
                return null;
            });
        } catch (Exception e) {
            LOG.error("Error in generate code", e);
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * Fix code with errors.
     *
     * @param code   The code with errors
     * @param errors The error messages
     * @return A future with the fixed code
     */
    @NotNull
    public CompletableFuture<String> fixCode(@NotNull String code, @NotNull String errors) {
        // Create a future to return
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.error("Not authenticated");
                future.complete(null);
                return future;
            }
            
            // Check if we've hit the daily request limit
            ModForgeSettings settings = ModForgeSettings.getInstance();
            int maxRequests = settings.getMaxApiRequestsPerDay();
            int currentRequests = requestCount.get();
            
            if (currentRequests >= maxRequests) {
                LOG.error("Daily request limit reached");
                future.complete(null);
                return future;
            }
            
            // Get the server URL
            String serverUrl = settings.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set");
                future.complete(null);
                return future;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null");
                future.complete(null);
                return future;
            }
            
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("code", code);
            requestBody.addProperty("errors", errors);
            
            // Send the request
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/ai/fix-code",
                    "POST",
                    token,
                    requestBody.toString()
            ).thenAccept(response -> {
                if (response != null) {
                    // Parse the response
                    try {
                        JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
                        String fixedCode = jsonResponse.get("code").getAsString();
                        
                        // Store the pattern
                        PatternRecognitionService patternService = project.getService(PatternRecognitionService.class);
                        patternService.storeErrorFixingPattern(code, errors, fixedCode);
                        
                        // Increment the request count
                        requestCount.incrementAndGet();
                        
                        // Complete the future
                        future.complete(fixedCode);
                    } catch (Exception e) {
                        LOG.error("Error parsing response", e);
                        future.complete(null);
                    }
                } else {
                    LOG.error("Failed to fix code");
                    future.complete(null);
                }
            }).exceptionally(ex -> {
                LOG.error("Exception fixing code", ex);
                future.complete(null);
                return null;
            });
        } catch (Exception e) {
            LOG.error("Error in fix code", e);
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * Get the current request count.
     *
     * @return The current request count
     */
    public int getRequestCount() {
        return requestCount.get();
    }
    
    /**
     * Reset the request count.
     */
    public void resetRequestCount() {
        requestCount.set(0);
    }
}