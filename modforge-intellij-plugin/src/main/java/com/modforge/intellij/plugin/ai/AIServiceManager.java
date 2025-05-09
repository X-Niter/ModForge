package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service that manages AI API requests.
 * This service is responsible for handling requests to the OpenAI API.
 */
@Service(Service.Level.APP)
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    
    private final HttpClient httpClient;
    private final Executor executor;
    
    /**
     * Creates a new AIServiceManager.
     */
    public AIServiceManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
        
        this.executor = Executors.newCachedThreadPool();
        
        LOG.info("AI service manager created");
    }
    
    /**
     * Gets the AI service manager.
     * @return The AI service manager
     */
    public static AIServiceManager getInstance() {
        return ApplicationManager.getApplication().getService(AIServiceManager.class);
    }
    
    /**
     * Generates a chat completion using the OpenAI API.
     * @param prompt The prompt
     * @param options Additional options
     * @return A future that completes with the response
     */
    public CompletableFuture<String> generateChatCompletion(@NotNull String prompt, @Nullable Map<String, Object> options) {
        LOG.info("Generating chat completion for prompt: " + prompt);
        
        // Create options if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        
        // Set model
        String model = (String) requestOptions.getOrDefault("model", DEFAULT_MODEL);
        requestBody.put("model", model);
        
        // Set messages
        Object systemPrompt = requestOptions.get("systemPrompt");
        
        if (systemPrompt != null) {
            requestBody.put("messages", new Object[]{
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt)
            });
        } else {
            requestBody.put("messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
            });
        }
        
        // Set temperature
        Object temperature = requestOptions.getOrDefault("temperature", 0.7);
        requestBody.put("temperature", temperature);
        
        // Set max tokens
        Object maxTokens = requestOptions.getOrDefault("maxTokens", 2048);
        requestBody.put("max_tokens", maxTokens);
        
        // Convert request body to JSON
        String requestBodyJson = toJson(requestBody);
        
        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
        
        // Send request asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // Check if response is successful
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // Parse response
                    String responseBody = response.body();
                    
                    // Extract content
                    return extractContentFromResponse(responseBody);
                } else {
                    LOG.error("Error generating chat completion: " + response.statusCode() + " " + response.body());
                    throw new IOException("Error generating chat completion: " + response.statusCode() + " " + response.body());
                }
            } catch (Exception e) {
                LOG.error("Error generating chat completion", e);
                throw new RuntimeException("Error generating chat completion", e);
            }
        }, executor);
    }
    
    /**
     * Gets the API key from settings.
     * @return The API key
     */
    @NotNull
    private String getApiKey() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String apiKey = settings.getOpenAiApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not set. Please set it in the ModForge settings.");
        }
        
        return apiKey;
    }
    
    /**
     * Converts an object to JSON.
     * @param object The object
     * @return The JSON
     */
    @NotNull
    private String toJson(@NotNull Object object) {
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            
            StringBuilder json = new StringBuilder();
            json.append("{");
            
            boolean first = true;
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                
                json.append("\"").append(entry.getKey()).append("\":");
                json.append(objectToJson(entry.getValue()));
                
                first = false;
            }
            
            json.append("}");
            
            return json.toString();
        } else {
            return objectToJson(object).toString();
        }
    }
    
    /**
     * Converts an object to a JSON value.
     * @param object The object
     * @return The JSON value
     */
    @NotNull
    private Object objectToJson(@Nullable Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof String) {
            return "\"" + ((String) object).replace("\"", "\\\"") + "\"";
        } else if (object instanceof Number || object instanceof Boolean) {
            return object.toString();
        } else if (object instanceof Map) {
            return toJson(object);
        } else if (object instanceof Object[]) {
            StringBuilder json = new StringBuilder();
            json.append("[");
            
            Object[] array = (Object[]) object;
            boolean first = true;
            
            for (Object item : array) {
                if (!first) {
                    json.append(",");
                }
                
                json.append(objectToJson(item));
                
                first = false;
            }
            
            json.append("]");
            
            return json.toString();
        } else {
            return "\"" + object.toString() + "\"";
        }
    }
    
    /**
     * Extracts content from OpenAI API response.
     * @param responseBody The response body
     * @return The content
     */
    @NotNull
    private String extractContentFromResponse(@NotNull String responseBody) {
        // Simple JSON extraction for OpenAI API response
        // In a real implementation, use a proper JSON library
        
        // Find choices array
        int choicesStart = responseBody.indexOf("\"choices\"");
        
        if (choicesStart == -1) {
            throw new IllegalArgumentException("Invalid response: no choices found");
        }
        
        // Find first choice
        int messageStart = responseBody.indexOf("\"message\"", choicesStart);
        
        if (messageStart == -1) {
            throw new IllegalArgumentException("Invalid response: no message found");
        }
        
        // Find content
        int contentStart = responseBody.indexOf("\"content\"", messageStart);
        
        if (contentStart == -1) {
            throw new IllegalArgumentException("Invalid response: no content found");
        }
        
        // Find content value
        int valueStart = responseBody.indexOf(":", contentStart) + 1;
        
        if (valueStart == 0) {
            throw new IllegalArgumentException("Invalid response: no content value found");
        }
        
        // Find content value start (after quotes)
        int contentValueStart = responseBody.indexOf("\"", valueStart) + 1;
        
        if (contentValueStart == 0) {
            throw new IllegalArgumentException("Invalid response: no content value start found");
        }
        
        // Find content value end (before quotes)
        int contentValueEnd = responseBody.indexOf("\"", contentValueStart);
        
        if (contentValueEnd == -1) {
            throw new IllegalArgumentException("Invalid response: no content value end found");
        }
        
        // Extract content value
        String content = responseBody.substring(contentValueStart, contentValueEnd);
        
        // Unescape JSON
        content = content.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
        
        return content;
    }
}