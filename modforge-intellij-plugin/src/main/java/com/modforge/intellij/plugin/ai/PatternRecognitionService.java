package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for recognizing and applying patterns in code generation and error resolution.
 * This helps reduce API usage by learning from previous requests and responses.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private final Project project;
    private final Map<String, String> codeGenerationPatterns = new ConcurrentHashMap<>();
    private final Map<String, String> errorFixingPatterns = new ConcurrentHashMap<>();
    
    /**
     * Create a new pattern recognition service.
     *
     * @param project The project
     */
    public PatternRecognitionService(Project project) {
        this.project = project;
        
        // Start a background task to load patterns from the server
        AppExecutorUtil.getAppScheduledExecutorService().schedule(
                this::loadPatterns,
                5,
                TimeUnit.SECONDS
        );
    }
    
    /**
     * Recognize a pattern for code generation.
     *
     * @param prompt The prompt for code generation
     * @return The generated code, or null if no pattern was recognized
     */
    @Nullable
    public String recognizeCodeGenerationPattern(@NotNull String prompt) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isEnablePatternRecognition()) {
            LOG.info("Pattern recognition is disabled");
            return null;
        }
        
        // Check if there's a pattern match
        for (Map.Entry<String, String> entry : codeGenerationPatterns.entrySet()) {
            if (isPromptSimilar(prompt, entry.getKey())) {
                LOG.info("Found code generation pattern match");
                return entry.getValue();
            }
        }
        
        LOG.info("No code generation pattern match found");
        return null;
    }
    
    /**
     * Recognize a pattern for error fixing.
     *
     * @param code    The code to fix
     * @param errors  The error messages
     * @return The fixed code, or null if no pattern was recognized
     */
    @Nullable
    public String recognizeErrorFixingPattern(@NotNull String code, @NotNull String errors) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isEnablePatternRecognition()) {
            LOG.info("Pattern recognition is disabled");
            return null;
        }
        
        // Create a key combining code and errors
        String key = code + "\n\nERRORS:\n" + errors;
        
        // Check if there's a direct pattern match
        if (errorFixingPatterns.containsKey(key)) {
            LOG.info("Found exact error fixing pattern match");
            return errorFixingPatterns.get(key);
        }
        
        // Check for similar patterns
        for (Map.Entry<String, String> entry : errorFixingPatterns.entrySet()) {
            if (isErrorProblemSimilar(code, errors, entry.getKey())) {
                LOG.info("Found similar error fixing pattern match");
                return entry.getValue();
            }
        }
        
        LOG.info("No error fixing pattern match found");
        return null;
    }
    
    /**
     * Store a code generation pattern.
     *
     * @param prompt        The prompt
     * @param generatedCode The generated code
     */
    public void storeCodeGenerationPattern(@NotNull String prompt, @NotNull String generatedCode) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isEnablePatternRecognition()) {
            return;
        }
        
        // Store the pattern locally
        codeGenerationPatterns.put(prompt, generatedCode);
        
        // Send the pattern to the server
        sendPatternToServer("code-generation", prompt, generatedCode);
    }
    
    /**
     * Store an error fixing pattern.
     *
     * @param code        The original code
     * @param errors      The error messages
     * @param fixedCode   The fixed code
     */
    public void storeErrorFixingPattern(
            @NotNull String code,
            @NotNull String errors,
            @NotNull String fixedCode) {
        // Check if pattern recognition is enabled
        if (!ModForgeSettings.getInstance().isEnablePatternRecognition()) {
            return;
        }
        
        // Create a key combining code and errors
        String key = code + "\n\nERRORS:\n" + errors;
        
        // Store the pattern locally
        errorFixingPatterns.put(key, fixedCode);
        
        // Send the pattern to the server
        sendPatternToServer("error-fixing", key, fixedCode);
    }
    
    /**
     * Load patterns from the server.
     */
    private void loadPatterns() {
        try {
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.info("Not authenticated, skipping pattern loading");
                return;
            }
            
            // Get the server URL
            String serverUrl = ModForgeSettings.getInstance().getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set");
                return;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null");
                return;
            }
            
            // Load code generation patterns
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/patterns/code-generation",
                    "GET",
                    token,
                    null
            ).thenAccept(response -> {
                if (response != null) {
                    LOG.info("Loaded code generation patterns");
                    
                    // Parse and store patterns
                    // This is a simplified implementation
                    // In a real implementation, you would parse the JSON response
                    
                    // For now, just log that we got the patterns
                    LOG.info("Received patterns: " + response);
                } else {
                    LOG.error("Failed to load code generation patterns");
                }
            });
            
            // Load error fixing patterns
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/patterns/error-fixing",
                    "GET",
                    token,
                    null
            ).thenAccept(response -> {
                if (response != null) {
                    LOG.info("Loaded error fixing patterns");
                    
                    // Parse and store patterns
                    // This is a simplified implementation
                    // In a real implementation, you would parse the JSON response
                    
                    // For now, just log that we got the patterns
                    LOG.info("Received patterns: " + response);
                } else {
                    LOG.error("Failed to load error fixing patterns");
                }
            });
        } catch (Exception e) {
            LOG.error("Error loading patterns", e);
        }
    }
    
    /**
     * Send a pattern to the server.
     *
     * @param patternType The pattern type
     * @param input       The input
     * @param output      The output
     */
    private void sendPatternToServer(
            @NotNull String patternType,
            @NotNull String input,
            @NotNull String output) {
        try {
            // Check if we're authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.info("Not authenticated, skipping pattern sending");
                return;
            }
            
            // Get the server URL
            String serverUrl = ModForgeSettings.getInstance().getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.error("Server URL is not set");
                return;
            }
            
            // Get the authentication token
            String token = authManager.getAuthToken();
            if (token == null) {
                LOG.error("Auth token is null");
                return;
            }
            
            // Create request body
            // This is a simplified implementation
            // In a real implementation, you would use a JSON library to create the body
            String body = "{"
                    + "\"patternType\":\"" + patternType + "\","
                    + "\"input\":\"" + input.replace("\"", "\\\"").replace("\n", "\\n") + "\","
                    + "\"output\":\"" + output.replace("\"", "\\\"").replace("\n", "\\n") + "\""
                    + "}";
            
            // Send the pattern
            TokenAuthConnectionUtil.makeAuthenticatedRequest(
                    serverUrl,
                    "api/patterns",
                    "POST",
                    token,
                    body
            ).thenAccept(response -> {
                if (response != null) {
                    LOG.info("Pattern sent to server");
                } else {
                    LOG.error("Failed to send pattern to server");
                }
            });
        } catch (Exception e) {
            LOG.error("Error sending pattern to server", e);
        }
    }
    
    /**
     * Check if two prompts are similar.
     *
     * @param prompt1 The first prompt
     * @param prompt2 The second prompt
     * @return True if similar, false otherwise
     */
    private boolean isPromptSimilar(@NotNull String prompt1, @NotNull String prompt2) {
        // This is a very simplified implementation
        // In a real implementation, you would use more sophisticated text similarity algorithms
        
        // For now, just do a simple check
        return prompt1.toLowerCase().contains(prompt2.toLowerCase())
                || prompt2.toLowerCase().contains(prompt1.toLowerCase());
    }
    
    /**
     * Check if two error problems are similar.
     *
     * @param code1   The first code
     * @param errors1 The first errors
     * @param key2    The second key (code + errors)
     * @return True if similar, false otherwise
     */
    private boolean isErrorProblemSimilar(
            @NotNull String code1,
            @NotNull String errors1,
            @NotNull String key2) {
        // This is a very simplified implementation
        // In a real implementation, you would use more sophisticated text similarity algorithms
        
        // Extract code and errors from key2
        String[] parts = key2.split("\n\nERRORS:\n");
        if (parts.length != 2) {
            return false;
        }
        
        String code2 = parts[0];
        String errors2 = parts[1];
        
        // Compare codes
        if (!code1.equals(code2)) {
            return false;
        }
        
        // Compare error messages
        return errors1.contains(errors2) || errors2.contains(errors1);
    }
}