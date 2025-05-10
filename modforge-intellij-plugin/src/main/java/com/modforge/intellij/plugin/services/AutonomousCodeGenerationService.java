package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for autonomous code generation using AI.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    private static final Gson GSON = new Gson();
    private static final int MAX_RETRIES = 3;
    private static final int MAX_FILE_PREVIEW_LINES = 20;
    
    private final Project project;
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final AtomicBoolean limitReached = new AtomicBoolean(false);
    
    /**
     * Create an autonomous code generation service.
     *
     * @param project The project
     */
    public AutonomousCodeGenerationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Generate code with a prompt.
     *
     * @param prompt      The prompt for code generation
     * @param contextFile The context file (can be null)
     * @param language    The programming language (e.g., "java", "kotlin")
     * @return A CompletableFuture that will be resolved to the generated code
     */
    public CompletableFuture<String> generateCode(@NotNull String prompt, @Nullable VirtualFile contextFile, @NotNull String language) {
        // Check if daily limit reached
        if (limitReached.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Daily API limit reached"));
        }
        
        // Check if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }
        
        // Create request ID
        String requestId = "generate-" + requestCounter.incrementAndGet();
        
        // Get context code
        String contextCode = getContextCode(contextFile);
        
        // Try pattern recognition first
        PatternRecognitionService patternService = project.getService(PatternRecognitionService.class);
        if (patternService != null && patternService.isEnabled()) {
            String response = patternService.tryMatchPattern("code", contextCode, prompt);
            if (response != null) {
                LOG.info("Code generation request matched pattern");
                return CompletableFuture.completedFuture(response);
            }
        }
        
        // Set up API request
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = authManager.getToken();
        
        String generateUrl = serverUrl.endsWith("/") ? serverUrl + "generate" : serverUrl + "/generate";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("context", contextCode);
        requestBody.put("language", language);
        
        // Execute request
        CompletableFuture<String> future = TokenAuthConnectionUtil.executePost(generateUrl, requestBody, token)
                .thenApply(response -> {
                    try {
                        // Parse response
                        Map<String, Object> responseMap = GSON.fromJson(response, Map.class);
                        String code = (String) responseMap.get("code");
                        String explanation = (String) responseMap.get("explanation");
                        
                        // Store pattern for future use
                        if (patternService != null && patternService.isEnabled() && code != null) {
                            patternService.storePattern("code", contextCode, prompt, code);
                        }
                        
                        // Check if we've reached the daily limit
                        checkDailyLimit(response);
                        
                        // Return code with explanation as a comment
                        return formatCodeWithExplanation(code, explanation, language);
                    } catch (Exception e) {
                        LOG.error("Error parsing code generation response", e);
                        throw new RuntimeException("Error parsing code generation response: " + e.getMessage(), e);
                    } finally {
                        pendingRequests.remove(requestId);
                    }
                })
                .exceptionally(e -> {
                    LOG.error("Error generating code", e);
                    pendingRequests.remove(requestId);
                    throw new RuntimeException("Error generating code: " + e.getMessage(), e);
                });
        
        // Store pending request
        pendingRequests.put(requestId, future);
        
        return future;
    }
    
    /**
     * Fix compilation errors in code.
     *
     * @param code        The code with errors
     * @param errorMessage The error message
     * @param language    The programming language (e.g., "java", "kotlin")
     * @return A CompletableFuture that will be resolved to the fixed code
     */
    public CompletableFuture<String> fixCode(@NotNull String code, @NotNull String errorMessage, @NotNull String language) {
        // Check if daily limit reached
        if (limitReached.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Daily API limit reached"));
        }
        
        // Check if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }
        
        // Create request ID
        String requestId = "fix-" + requestCounter.incrementAndGet();
        
        // Try pattern recognition first
        PatternRecognitionService patternService = project.getService(PatternRecognitionService.class);
        if (patternService != null && patternService.isEnabled()) {
            String response = patternService.tryMatchPattern("fix", code, errorMessage);
            if (response != null) {
                LOG.info("Code fixing request matched pattern");
                return CompletableFuture.completedFuture(response);
            }
        }
        
        // Set up API request
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = authManager.getToken();
        
        String fixUrl = serverUrl.endsWith("/") ? serverUrl + "fix" : serverUrl + "/fix";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("code", code);
        requestBody.put("error", errorMessage);
        requestBody.put("language", language);
        
        // Execute request
        CompletableFuture<String> future = TokenAuthConnectionUtil.executePost(fixUrl, requestBody, token)
                .thenApply(response -> {
                    try {
                        // Parse response
                        Map<String, Object> responseMap = GSON.fromJson(response, Map.class);
                        String fixedCode = (String) responseMap.get("code");
                        String explanation = (String) responseMap.get("explanation");
                        
                        // Store pattern for future use
                        if (patternService != null && patternService.isEnabled() && fixedCode != null) {
                            patternService.storePattern("fix", code, errorMessage, fixedCode);
                        }
                        
                        // Check if we've reached the daily limit
                        checkDailyLimit(response);
                        
                        // Return fixed code with explanation as a comment
                        return formatCodeWithExplanation(fixedCode, explanation, language);
                    } catch (Exception e) {
                        LOG.error("Error parsing code fixing response", e);
                        throw new RuntimeException("Error parsing code fixing response: " + e.getMessage(), e);
                    } finally {
                        pendingRequests.remove(requestId);
                    }
                })
                .exceptionally(e -> {
                    LOG.error("Error fixing code", e);
                    pendingRequests.remove(requestId);
                    throw new RuntimeException("Error fixing code: " + e.getMessage(), e);
                });
        
        // Store pending request
        pendingRequests.put(requestId, future);
        
        return future;
    }
    
    /**
     * Generate documentation for code.
     *
     * @param code     The code to document
     * @param language The programming language (e.g., "java", "kotlin")
     * @return A CompletableFuture that will be resolved to the documented code
     */
    public CompletableFuture<String> generateDocumentation(@NotNull String code, @NotNull String language) {
        // Check if daily limit reached
        if (limitReached.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Daily API limit reached"));
        }
        
        // Check if authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }
        
        // Create request ID
        String requestId = "docs-" + requestCounter.incrementAndGet();
        
        // Try pattern recognition first
        PatternRecognitionService patternService = project.getService(PatternRecognitionService.class);
        if (patternService != null && patternService.isEnabled()) {
            String response = patternService.tryMatchPattern("docs", code, language);
            if (response != null) {
                LOG.info("Documentation request matched pattern");
                return CompletableFuture.completedFuture(response);
            }
        }
        
        // Set up API request
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String token = authManager.getToken();
        
        String docsUrl = serverUrl.endsWith("/") ? serverUrl + "docs" : serverUrl + "/docs";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("code", code);
        requestBody.put("language", language);
        
        // Execute request
        CompletableFuture<String> future = TokenAuthConnectionUtil.executePost(docsUrl, requestBody, token)
                .thenApply(response -> {
                    try {
                        // Parse response
                        Map<String, Object> responseMap = GSON.fromJson(response, Map.class);
                        String documentedCode = (String) responseMap.get("code");
                        
                        // Store pattern for future use
                        if (patternService != null && patternService.isEnabled() && documentedCode != null) {
                            patternService.storePattern("docs", code, language, documentedCode);
                        }
                        
                        // Check if we've reached the daily limit
                        checkDailyLimit(response);
                        
                        return documentedCode;
                    } catch (Exception e) {
                        LOG.error("Error parsing documentation response", e);
                        throw new RuntimeException("Error parsing documentation response: " + e.getMessage(), e);
                    } finally {
                        pendingRequests.remove(requestId);
                    }
                })
                .exceptionally(e -> {
                    LOG.error("Error generating documentation", e);
                    pendingRequests.remove(requestId);
                    throw new RuntimeException("Error generating documentation: " + e.getMessage(), e);
                });
        
        // Store pending request
        pendingRequests.put(requestId, future);
        
        return future;
    }
    
    /**
     * Cancel all pending requests.
     */
    public void cancelAllRequests() {
        LOG.info("Canceling all pending requests (" + pendingRequests.size() + ")");
        pendingRequests.forEach((id, future) -> future.cancel(true));
        pendingRequests.clear();
    }
    
    /**
     * Get the number of pending requests.
     *
     * @return The number of pending requests
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }
    
    /**
     * Check if we've reached the daily API usage limit.
     *
     * @param response The response from the API
     */
    private void checkDailyLimit(String response) {
        try {
            Map<String, Object> responseMap = GSON.fromJson(response, Map.class);
            Map<String, Object> limitInfo = (Map<String, Object>) responseMap.get("limitInfo");
            
            if (limitInfo != null) {
                boolean limitReached = (boolean) limitInfo.getOrDefault("limitReached", false);
                int remaining = ((Number) limitInfo.getOrDefault("remaining", 0)).intValue();
                int limit = ((Number) limitInfo.getOrDefault("limit", 0)).intValue();
                
                if (limitReached) {
                    this.limitReached.set(true);
                    LOG.warn("Daily API limit reached (" + limit + ")");
                    
                    // Notify user
                    Messages.showWarningDialog(
                            project,
                            "You have reached your daily API usage limit of " + limit + " requests. " +
                                    "Requests will be available again tomorrow.",
                            "API Limit Reached"
                    );
                } else if (remaining < 10) {
                    // Warn if approaching limit
                    LOG.warn("Approaching daily API limit (" + remaining + " of " + limit + " remaining)");
                    
                    // Notify user
                    Messages.showWarningDialog(
                            project,
                            "You are approaching your daily API usage limit. " +
                                    "You have " + remaining + " of " + limit + " requests remaining today.",
                            "API Limit Warning"
                    );
                }
            }
        } catch (Exception e) {
            LOG.error("Error checking daily limit", e);
        }
    }
    
    /**
     * Format code with an explanation as a comment.
     *
     * @param code        The code
     * @param explanation The explanation
     * @param language    The programming language
     * @return The formatted code with explanation
     */
    private String formatCodeWithExplanation(String code, String explanation, String language) {
        if (code == null) {
            return "";
        }
        
        if (explanation == null || explanation.isEmpty()) {
            return code;
        }
        
        // Format explanation as a comment based on language
        String commentStart;
        String commentEnd = "";
        String commentLine;
        
        switch (language.toLowerCase()) {
            case "java":
            case "kotlin":
            case "javascript":
            case "typescript":
            case "c":
            case "cpp":
                commentStart = "/**\n";
                commentEnd = " */\n";
                commentLine = " * ";
                break;
                
            case "python":
                commentStart = "'''\n";
                commentEnd = "'''\n";
                commentLine = "";
                break;
                
            case "html":
                commentStart = "<!--\n";
                commentEnd = "-->\n";
                commentLine = "";
                break;
                
            default:
                commentStart = "// ";
                commentLine = "// ";
                break;
        }
        
        // Format explanation
        StringBuilder formattedExplanation = new StringBuilder(commentStart);
        
        try (BufferedReader reader = new BufferedReader(new StringReader(explanation))) {
            String line;
            while ((line = reader.readLine()) != null) {
                formattedExplanation.append(commentLine).append(line).append("\n");
            }
        } catch (Exception e) {
            LOG.error("Error formatting explanation", e);
            // If there's an error, just use the raw explanation
            formattedExplanation.append(commentLine).append(explanation).append("\n");
        }
        
        formattedExplanation.append(commentEnd);
        
        // Return formatted explanation followed by code
        return formattedExplanation + code;
    }
    
    /**
     * Get the content of a file for context.
     *
     * @param contextFile The context file (can be null)
     * @return The content of the file, or an empty string if the file is null or cannot be read
     */
    private String getContextCode(@Nullable VirtualFile contextFile) {
        if (contextFile == null || !contextFile.isValid()) {
            return "";
        }
        
        try {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(contextFile);
            if (psiFile == null) {
                return "";
            }
            
            // Get the content of the file
            String content = psiFile.getText();
            
            // If the content is too large, only use first MAX_FILE_PREVIEW_LINES lines
            if (content.length() > 10000) {
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < MAX_FILE_PREVIEW_LINES) {
                        lines.add(line);
                        lineCount++;
                    }
                }
                
                return String.join("\n", lines) + "\n// ... (file truncated, showing first " + MAX_FILE_PREVIEW_LINES + " lines)";
            }
            
            return content;
        } catch (Exception e) {
            LOG.error("Error getting context code", e);
            return "";
        }
    }
}