package com.modforge.intellij.plugin.ai;

import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for interacting with AI services.
 * This service is responsible for making requests to various AI providers.
 */
@Service(Service.Level.APP)
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    
    private static final int TIMEOUT_SECONDS = 60;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final PatternRecognitionService patternRecognitionService;
    
    /**
     * Creates a new AIServiceManager.
     */
    public AIServiceManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        
        gson = new Gson();
        patternRecognitionService = ApplicationManager.getApplication().getService(PatternRecognitionService.class);
        
        LOG.info("AI service manager initialized");
    }
    
    /**
     * Gets the AI service manager instance.
     * @return The AI service manager
     */
    public static AIServiceManager getInstance() {
        return ApplicationManager.getApplication().getService(AIServiceManager.class);
    }
    
    /**
     * Generates a chat completion.
     * @param prompt The prompt
     * @param options The options
     * @return A future that completes with the generated text
     */
    @NotNull
    public CompletableFuture<String> generateChatCompletion(@NotNull String prompt, @Nullable Map<String, Object> options) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Get options
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Get current API key and model
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String apiKey = settings.getApiKey();
        String model = settings.getAiModel();
        
        if (apiKey == null || apiKey.isEmpty()) {
            future.completeExceptionally(new IllegalStateException("API key not set"));
            return future;
        }
        
        // Record API call for metrics
        patternRecognitionService.recordApiCall();
        
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                String systemPrompt = (String) requestOptions.getOrDefault("systemPrompt", 
                        "You are a helpful AI assistant for Minecraft mod development.");
                
                Double temperature = (Double) requestOptions.getOrDefault("temperature", 0.7);
                Integer maxTokens = (Integer) requestOptions.getOrDefault("maxTokens", 1024);
                
                // Determine API endpoint and request format based on model
                if (model.startsWith("gpt")) {
                    // OpenAI format
                    String response = generateOpenAICompletion(prompt, systemPrompt, model, temperature, maxTokens, apiKey);
                    future.complete(response);
                } else if (model.startsWith("claude")) {
                    // Anthropic format
                    String response = generateAnthropicCompletion(prompt, systemPrompt, model, temperature, maxTokens, apiKey);
                    future.complete(response);
                } else {
                    future.completeExceptionally(new IllegalArgumentException("Unsupported model: " + model));
                }
            } catch (Exception e) {
                LOG.error("Error generating completion", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Generates a completion using OpenAI.
     * @param prompt The prompt
     * @param systemPrompt The system prompt
     * @param model The model
     * @param temperature The temperature
     * @param maxTokens The maximum number of tokens
     * @param apiKey The API key
     * @return The generated text
     * @throws IOException If an error occurs
     */
    @NotNull
    private String generateOpenAICompletion(
            @NotNull String prompt,
            @NotNull String systemPrompt,
            @NotNull String model,
            double temperature,
            int maxTokens,
            @NotNull String apiKey) throws IOException {
        
        // Create request body
        Map<String, Object> message1 = new HashMap<>();
        message1.put("role", "system");
        message1.put("content", systemPrompt);
        
        Map<String, Object> message2 = new HashMap<>();
        message2.put("role", "user");
        message2.put("content", prompt);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", new Object[]{message1, message2});
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        // Create request
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(requestBody), JSON_MEDIA_TYPE))
                .build();
        
        // Execute request
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + ", message: " + response.message());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }
            
            String json = responseBody.string();
            Map<String, Object> responseMap = gson.fromJson(json, Map.class);
            
            // Parse response
            if (responseMap.containsKey("choices") && responseMap.get("choices") instanceof java.util.List) {
                java.util.List choices = (java.util.List) responseMap.get("choices");
                if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                    Map choice = (Map) choices.get(0);
                    if (choice.containsKey("message") && choice.get("message") instanceof Map) {
                        Map message = (Map) choice.get("message");
                        if (message.containsKey("content")) {
                            return (String) message.get("content");
                        }
                    }
                }
            }
            
            throw new IOException("Failed to parse response: " + json);
        }
    }
    
    /**
     * Generates a completion using Anthropic.
     * @param prompt The prompt
     * @param systemPrompt The system prompt
     * @param model The model
     * @param temperature The temperature
     * @param maxTokens The maximum number of tokens
     * @param apiKey The API key
     * @return The generated text
     * @throws IOException If an error occurs
     */
    @NotNull
    private String generateAnthropicCompletion(
            @NotNull String prompt,
            @NotNull String systemPrompt,
            @NotNull String model,
            double temperature,
            int maxTokens,
            @NotNull String apiKey) throws IOException {
        
        // Create request body
        Map<String, Object> message1 = new HashMap<>();
        message1.put("role", "user");
        message1.put("content", prompt);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", new Object[]{message1});
        requestBody.put("system", systemPrompt);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        // Create request
        Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(requestBody), JSON_MEDIA_TYPE))
                .build();
        
        // Execute request
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + ", message: " + response.message());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }
            
            String json = responseBody.string();
            Map<String, Object> responseMap = gson.fromJson(json, Map.class);
            
            // Parse response
            if (responseMap.containsKey("content") && responseMap.get("content") instanceof java.util.List) {
                java.util.List content = (java.util.List) responseMap.get("content");
                if (!content.isEmpty() && content.get(0) instanceof Map) {
                    Map contentItem = (Map) content.get(0);
                    if (contentItem.containsKey("text")) {
                        return (String) contentItem.get("text");
                    }
                }
            }
            
            throw new IOException("Failed to parse response: " + json);
        }
    }
}