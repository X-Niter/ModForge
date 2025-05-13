package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import com.modforge.intellij.plugin.model.ModLoaderType;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.JsonUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing AI services.
 * This service provides access to AI APIs for code generation, error fixing, and documentation.
 */
@Service
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    

    /**
     * Gets the instance of AIServiceManager from the AI package.
     * This method avoids conflict with the similarly named method in 
     * com.modforge.intellij.plugin.services.AIServiceManager
     * 
     * @return The AI service manager from the AI package
     */
    public static AIServiceManager getAIInstance() {
        return ApplicationManager.getApplication().getService(AIServiceManager.class);
    }
    
    /**
     * Compatibility method to avoid conflicts between different implementations
     * 
     * @return The AI service manager
     */
    public static AIServiceManager getInstance() {
        return getAIInstance();
    }
    
    /**
     * Generates code based on a prompt.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return The generated code
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String generateCode(@NotNull String prompt, @NotNull String language, @Nullable Map<String, Object> options) throws IOException {
        // Create system prompt for code generation
        String systemPrompt = "You are an expert " + language + " developer. " +
                "You provide well-written, clean, and efficient code. " +
                "Focus on generating code that solves the user's problem. " +
                "Only respond with code without explanation or additional text.";
        
        // Create full prompt
        String fullPrompt = "Write " + language + " code for the following: " + prompt;
        
        // Make API request
        return makeAiRequest(systemPrompt, fullPrompt);
    }
    
    /**
     * Fixes code errors.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return The fixed code
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String fixCode(@NotNull String code, @Nullable String errorMessage, @Nullable Map<String, Object> options) throws IOException {
        // Create system prompt for error fixing
        String systemPrompt = "You are an expert developer. " +
                "You fix code errors precisely without changing functionality. " +
                "Only respond with the corrected code without explanation or additional text.";
        
        // Create full prompt
        StringBuilder fullPrompt = new StringBuilder("Fix the following code:\n\n");
        fullPrompt.append("```\n").append(code).append("\n```\n\n");
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            fullPrompt.append("Error message: ").append(errorMessage).append("\n\n");
        }
        
        fullPrompt.append("Return the complete fixed code.");
        
        // Make API request
        return makeAiRequest(systemPrompt, fullPrompt.toString());
    }
    
    /**
     * Generates documentation for code.
     * @param code The code to document
     * @param options Additional options
     * @return The documented code
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String generateDocumentation(@NotNull String code, @Nullable Map<String, Object> options) throws IOException {
        // Create system prompt for documentation generation
        String systemPrompt = "You are an expert developer. " +
                "You add high-quality documentation to code. " +
                "Add JavaDoc or appropriate documentation comments to classes, methods, and fields. " +
                "Only respond with the documented code without explanation or additional text.";
        
        // Create full prompt
        StringBuilder fullPrompt = new StringBuilder("Add documentation to the following code:\n\n");
        fullPrompt.append("```\n").append(code).append("\n```\n\n");
        fullPrompt.append("Return the complete documented code.");
        
        // Make API request
        return makeAiRequest(systemPrompt, fullPrompt.toString());
    }
    
    /**
     * Explains an error message.
     * @param errorMessage The error message to explain
     * @return The explanation
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String explainError(@NotNull String errorMessage) throws IOException {
        // Create system prompt for error explanation
        String systemPrompt = "You are an expert developer who excels at understanding and fixing code errors. " +
                "You provide clear explanations of error messages and suggest solutions. " +
                "Format your response as follows:\n" +
                "1. Error Analysis: Brief explanation of what the error means\n" +
                "2. Likely Cause: Most common reasons for this error\n" +
                "3. How to Fix: Specific steps to resolve the issue";
        
        // Create full prompt
        StringBuilder fullPrompt = new StringBuilder("Explain the following error and how to fix it:\n\n");
        fullPrompt.append(errorMessage);
        
        // Make API request
        return makeAiRequest(systemPrompt, fullPrompt.toString());
    }
    
    /**
     * Explains code.
     * @param code The code to explain
     * @param options Additional options
     * @return The explanation
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String explainCode(@NotNull String code, @Nullable Map<String, Object> options) throws IOException {
        // Create system prompt for code explanation
        String systemPrompt = "You are an expert developer. " +
                "You explain code clearly and concisely. " +
                "Focus on the purpose, structure, and functionality of the code.";
        
        // Create full prompt
        StringBuilder fullPrompt = new StringBuilder("Explain the following code:\n\n");
        fullPrompt.append("```\n").append(code).append("\n```\n\n");
        fullPrompt.append("Provide a clear explanation of what this code does, how it works, and any notable patterns or techniques used.");
        
        // Make API request
        return makeAiRequest(systemPrompt, fullPrompt.toString());
    }
    
    /**
     * Adds features to code.
     * @param code The code to add features to
     * @param featureDescription The feature description
     * @param options Additional options
     * @return The enhanced code
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String addFeatures(@NotNull String code, @NotNull String featureDescription, @Nullable Map<String, Object> options) throws IOException {
        // Create system prompt for feature addition
        String systemPrompt = "You are an expert developer. " +
                "You add features to existing code while maintaining code style and structure. " +
                "Only respond with the enhanced code without explanation or additional text.";
        
        // Create full prompt
        StringBuilder fullPrompt = new StringBuilder("Add the following feature to this code:\n\n");
        fullPrompt.append("Feature description: ").append(featureDescription).append("\n\n");
        fullPrompt.append("Existing code:\n```\n").append(code).append("\n```\n\n");
        fullPrompt.append("Return the complete enhanced code with the new feature implemented.");
        
        // Make API request
        return makeAiRequest(systemPrompt, fullPrompt.toString());
    }
    
    /**
     * Makes an API request to the AI service.
     * @param systemPrompt The system prompt
     * @param userPrompt The user prompt
     * @return The response
     * @throws IOException If an IO error occurs
     */
    @NotNull
    private String makeAiRequest(@NotNull String systemPrompt, @NotNull String userPrompt) throws IOException {
        // Get settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String apiKey = settings.getOpenAiApiKey();
        String model = settings.getOpenAiModel();
        int maxTokens = settings.getMaxTokens();
        double temperature = settings.getTemperature();
        
        // Validate API key
        if (apiKey.isEmpty()) {
            throw new IOException("OpenAI API key is not configured. Please set it in the settings.");
        }
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // Add system message
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // Add user message
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        // Create request
        RequestBuilder request = HttpRequests.post(OPENAI_API_URL, "application/json")
                .accept("application/json")
                .productNameAsUserAgent()
                .tuner(connection -> connection.setRequestProperty("Authorization", "Bearer " + apiKey));
        
        // Send request
        LOG.info("Sending request to OpenAI API");
        
        String response = request.connect(connection -> {
            // Write request body
            connection.write(JsonUtil.writeToString(requestBody));
            
            // Read response using JsonUtil instead of JsonReader directly
            Map<String, Object> responseMap = JsonUtil.readMapFromStream(connection.getInputStream());
            if (responseMap == null) {
                throw new IOException("Failed to parse response from OpenAI API");
            }
            
            // Parse response
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            
            throw new IOException("Invalid response from OpenAI API");
        });
        
        LOG.info("Received response from OpenAI API");
        
        return response != null ? response.trim() : "";
    }
    
    /**
     * Constructs an OpenAI API URL for a specific endpoint.
     * @param endpoint The endpoint
     * @return The URL
     */
    @NotNull
    private String getOpenAiUrl(@NotNull String endpoint) {
        return "https://api.openai.com/v1/" + endpoint;
    }
    
    /**
     * Executes an AI request with pattern matching optimization.
     * This is a compatibility method that forwards to the service implementation.
     * 
     * @param project The project
     * @param input The input prompt
     * @param category The pattern category
     * @param fallbackFunction The function to call if no pattern match is found
     * @return The AI response or null if there's an error
     */
    @Nullable
    public String executeWithPatternMatching(
            com.intellij.openapi.project.Project project,
            @NotNull String input,
            @NotNull PatternCategory category,
            @NotNull java.util.function.Function<String, String> fallbackFunction
    ) {
        // Forward to the service implementation
        try {
            // Get service manager from services package
            com.modforge.intellij.plugin.services.AIServiceManager serviceManager = 
                com.modforge.intellij.plugin.services.AIServiceManager.getInstance();
            
            return serviceManager.executeWithPatternMatching(
                project,
                input,
                category.toServiceCategory(),
                fallbackFunction
            );
        } catch (Exception e) {
            LOG.error("Error forwarding to pattern matching service", e);
            return null;
        }
    }
    
    /**
     * Uploads patterns to the pattern server
     * This is a compatibility method that forwards to the service implementation
     * 
     * @param patterns The patterns to upload
     * @return True if successful, false otherwise
     */
    public boolean uploadPatterns(@NotNull List<Map<String, Object>> patterns) {
        try {
            // Get service manager from services package
            com.modforge.intellij.plugin.services.AIServiceManager serviceManager = 
                com.modforge.intellij.plugin.services.AIServiceManager.getInstance();
            
            return serviceManager.uploadPatterns(patterns);
        } catch (Exception e) {
            LOG.error("Error uploading patterns", e);
            return false;
        }
    }
    
    /**
     * Downloads the latest patterns from the pattern server
     * This is a compatibility method that forwards to the service implementation
     * 
     * @return The downloaded patterns or an empty list if there's an error
     */
    @NotNull
    public List<Map<String, Object>> downloadLatestPatterns() {
        try {
            // Get service manager from services package
            com.modforge.intellij.plugin.services.AIServiceManager serviceManager = 
                com.modforge.intellij.plugin.services.AIServiceManager.getInstance();
            
            return serviceManager.downloadLatestPatterns();
        } catch (Exception e) {
            LOG.error("Error downloading latest patterns", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generates code based on a code generation request.
     * 
     * @param request The code generation request
     * @return The generated code
     * @throws IOException If an IO error occurs
     */
    @NotNull
    public String generateCode(@NotNull CodeGenerationRequest request) throws IOException {
        // Build system prompt using mod loader and Minecraft version
        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append("You are an expert Minecraft mod developer. ");
        
        if (request.getModLoaderType() != ModLoaderType.UNKNOWN) {
            systemPromptBuilder.append("You specialize in ").append(request.getModLoaderType().getDisplayName())
                              .append(" mods. ");
        }
        
        if (!request.getMinecraftVersion().isEmpty()) {
            systemPromptBuilder.append("You're writing code for Minecraft version ")
                              .append(request.getMinecraftVersion()).append(". ");
        }
        
        systemPromptBuilder.append("You write clean, efficient, and well-documented code. ")
                          .append("Only respond with code without explanation unless explanation was explicitly requested.");
        
        // Build user prompt
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("Generate ").append(request.getLanguage()).append(" code for a ")
                        .append(request.getModLoaderType().getDisplayName())
                        .append(" mod with the following request: ").append(request.getPrompt());
        
        // Add code context if available
        if (!request.getCodeContext().isEmpty()) {
            userPromptBuilder.append("\n\nHere is some context about the existing code:\n```\n")
                            .append(request.getCodeContext()).append("\n```");
        }
        
        if (request.isUsePatternMatching()) {
            // Try pattern matching first
            String result = executeWithPatternMatching(
                null, // No project needed for this
                userPromptBuilder.toString(),
                PatternCategory.CODE_GENERATION,
                prompt -> {
                    try {
                        return makeAiRequest(systemPromptBuilder.toString(), prompt);
                    } catch (IOException e) {
                        LOG.error("Error making AI request", e);
                        return null;
                    }
                }
            );
            
            if (result != null) {
                return result;
            }
        }
        
        // Fall back to direct API call
        return makeAiRequest(systemPromptBuilder.toString(), userPromptBuilder.toString());
    }
}