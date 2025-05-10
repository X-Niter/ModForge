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

import java.util.concurrent.ExecutorService;

/**
 * Service for pattern recognition.
 * Uses AI-based pattern recognition to optimize API usage.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    private final Project project;
    private final ExecutorService executor;
    
    /**
     * Construct the pattern recognition service.
     *
     * @param project The project
     */
    public PatternRecognitionService(@NotNull Project project) {
        this.project = project;
        this.executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("ModForge Pattern Recognition", 1);
    }
    
    /**
     * Check if pattern recognition is enabled.
     *
     * @return Whether pattern recognition is enabled
     */
    public boolean isEnabled() {
        return ModForgeSettings.getInstance().isPatternRecognition();
    }
    
    /**
     * Try to recognize a pattern for code generation.
     *
     * @param prompt The prompt
     * @return The generated code, or null if no pattern is recognized
     */
    @Nullable
    public String recognizeCodeGenerationPattern(@NotNull String prompt) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            // Call API to recognize pattern
            String requestBody = String.format("{\"prompt\":\"%s\",\"usePattern\":true}", 
                    prompt.replace("\"", "\\\"").replace("\n", "\\n"));
            String response = TokenAuthConnectionUtil.executePost("/api/code/generate", requestBody);
            
            if (response != null && !response.isEmpty()) {
                // Try to extract patternMatched flag from response
                boolean patternMatched = response.contains("\"patternMatched\":true");
                
                if (patternMatched) {
                    // Try to extract code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            LOG.error("Error recognizing code generation pattern", e);
            return null;
        }
    }
    
    /**
     * Try to recognize a pattern for error fixing.
     *
     * @param code   The code with errors
     * @param errors The error messages
     * @return The fixed code, or null if no pattern is recognized
     */
    @Nullable
    public String recognizeErrorFixingPattern(@NotNull String code, @NotNull String errors) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            // Call API to recognize pattern
            String requestBody = String.format("{\"code\":\"%s\",\"errors\":\"%s\",\"usePattern\":true}", 
                    code.replace("\"", "\\\"").replace("\n", "\\n"),
                    errors.replace("\"", "\\\"").replace("\n", "\\n"));
            String response = TokenAuthConnectionUtil.executePost("/api/code/fix", requestBody);
            
            if (response != null && !response.isEmpty()) {
                // Try to extract patternMatched flag from response
                boolean patternMatched = response.contains("\"patternMatched\":true");
                
                if (patternMatched) {
                    // Try to extract code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            LOG.error("Error recognizing error fixing pattern", e);
            return null;
        }
    }
    
    /**
     * Try to recognize a pattern for feature addition.
     *
     * @param code        The code to add features to
     * @param description The description of the features to add
     * @return The updated code, or null if no pattern is recognized
     */
    @Nullable
    public String recognizeFeatureAdditionPattern(@NotNull String code, @NotNull String description) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            // Check if authenticated
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            // Call API to recognize pattern
            String requestBody = String.format("{\"code\":\"%s\",\"description\":\"%s\",\"usePattern\":true}", 
                    code.replace("\"", "\\\"").replace("\n", "\\n"),
                    description.replace("\"", "\\\"").replace("\n", "\\n"));
            String response = TokenAuthConnectionUtil.executePost("/api/code/add-features", requestBody);
            
            if (response != null && !response.isEmpty()) {
                // Try to extract patternMatched flag from response
                boolean patternMatched = response.contains("\"patternMatched\":true");
                
                if (patternMatched) {
                    // Try to extract code from response
                    if (response.contains("\"code\":")) {
                        return response.split("\"code\":")[1].split("\"")[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            LOG.error("Error recognizing feature addition pattern", e);
            return null;
        }
    }
}