package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.JsonReader;
import com.intellij.util.io.JsonUtil;
import com.intellij.util.io.RequestBuilder;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
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
     * Gets the instance of the service.
     * @return The AI service manager
     */
    public static AIServiceManager getInstance() {
        return ApplicationManager.getApplication().getService(AIServiceManager.class);
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
            
            // Read response
            JsonReader reader = JsonReader.fromInputStream(connection.getInputStream());
            Map<String, Object> responseMap = reader.readObject();
            
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
}