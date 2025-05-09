package com.modforge.intellij.plugin.ai;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing AI operations.
 * This service provides high-level methods for generating code, fixing code,
 * and generating documentation using AI services.
 */
@Service(Service.Level.PROJECT)
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4";
    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 0.7;
    
    private final Project project;
    
    // Cache for recent responses
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalTokensUsed = new AtomicLong(0);
    
    /**
     * Creates a new AIServiceManager.
     * @param project The project
     */
    public AIServiceManager(@NotNull Project project) {
        this.project = project;
        
        // Schedule cache cleanup
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                this::cleanupCache, 30, 30, TimeUnit.MINUTES);
        
        LOG.info("AIServiceManager initialized");
    }
    
    /**
     * Gets the AIServiceManager instance.
     * @param project The project
     * @return The AIServiceManager instance
     */
    public static AIServiceManager getInstance(@NotNull Project project) {
        return project.getService(AIServiceManager.class);
    }
    
    /**
     * Generates code based on a prompt.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return A future that completes with the generated code
     */
    @NotNull
    public CompletableFuture<String> generateCode(@NotNull String prompt, @NotNull String language, 
                                               @Nullable Map<String, Object> options) {
        // Build messages
        String systemPrompt = "You are an expert " + language + " developer. " +
                "Generate clean, well-commented code based on the user's request. " +
                "Provide only the code without explanations, unless specifically asked for explanations.";
        
        if (options != null && !options.isEmpty()) {
            systemPrompt += "\n\nAdditional context:\n";
            
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                systemPrompt += "- " + entry.getKey() + ": " + entry.getValue() + "\n";
            }
        }
        
        return sendRequest(systemPrompt, prompt, "generate_code", options);
    }
    
    /**
     * Fixes code based on a prompt and code.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return A future that completes with the fixed code
     */
    @NotNull
    public CompletableFuture<String> fixCode(@NotNull String code, @Nullable String errorMessage, 
                                          @Nullable Map<String, Object> options) {
        // Build messages
        String systemPrompt = "You are an expert programmer. Fix the code provided by the user.";
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            systemPrompt += " The code is producing the following error:\n\n" + errorMessage;
        }
        
        String userPrompt = "Please fix the following code:\n\n```\n" + code + "\n```";
        
        return sendRequest(systemPrompt, userPrompt, "fix_code", options);
    }
    
    /**
     * Generates documentation for code.
     * @param code The code to document
     * @param options Additional options
     * @return A future that completes with the documentation
     */
    @NotNull
    public CompletableFuture<String> generateDocumentation(@NotNull String code, 
                                                        @Nullable Map<String, Object> options) {
        // Build messages
        String systemPrompt = "You are an expert technical writer. " +
                "Generate comprehensive documentation for the code provided by the user.";
        
        if (options != null && options.containsKey("prompt")) {
            systemPrompt += " " + options.get("prompt");
        }
        
        String userPrompt = "Please generate documentation for the following code:\n\n```\n" + code + "\n```";
        
        return sendRequest(systemPrompt, userPrompt, "generate_documentation", options);
    }
    
    /**
     * Explains code.
     * @param code The code to explain
     * @param options Additional options
     * @return A future that completes with the explanation
     */
    @NotNull
    public CompletableFuture<String> explainCode(@NotNull String code, @Nullable Map<String, Object> options) {
        // Build messages
        String systemPrompt = "You are an expert programmer. " +
                "Explain the code provided by the user in a clear and concise manner.";
        
        String userPrompt = "Please explain the following code:\n\n```\n" + code + "\n```";
        
        return sendRequest(systemPrompt, userPrompt, "explain_code", options);
    }
    
    /**
     * Sends a request to the OpenAI API.
     * @param systemPrompt The system prompt
     * @param userPrompt The user prompt
     * @param requestType The request type for caching
     * @param options Additional options
     * @return A future that completes with the response
     */
    @NotNull
    private CompletableFuture<String> sendRequest(@NotNull String systemPrompt, @NotNull String userPrompt, 
                                               @NotNull String requestType, @Nullable Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache
            String cacheKey = generateCacheKey(systemPrompt, userPrompt, requestType);
            
            CachedResponse cachedResponse = responseCache.get(cacheKey);
            if (cachedResponse != null && !cachedResponse.isExpired()) {
                // Cache hit
                cacheHitCount.incrementAndGet();
                LOG.info("Cache hit for request type: " + requestType);
                
                return cachedResponse.getResponse();
            }
            
            // Increment request count
            requestCount.incrementAndGet();
            
            try {
                // Get API key
                String apiKey = ModForgeSettings.getInstance().getOpenAiApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalStateException("OpenAI API key is not set");
                }
                
                // Build request
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setDoOutput(true);
                
                // Build messages
                String requestBody = "{" +
                        "\"model\": \"" + MODEL + "\"," +
                        "\"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"" + escapeJson(systemPrompt) + "\"}," +
                        "{\"role\": \"user\", \"content\": \"" + escapeJson(userPrompt) + "\"}" +
                        "]," +
                        "\"max_tokens\": " + MAX_TOKENS + "," +
                        "\"temperature\": " + TEMPERATURE +
                        "}";
                
                // Send request
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                    writer.write(requestBody);
                    writer.flush();
                }
                
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                // Parse response
                String responseText = parseResponse(response.toString());
                
                // Increment token count (estimation)
                int estimatedTokens = estimateTokenCount(systemPrompt) + estimateTokenCount(userPrompt) + 
                        estimateTokenCount(responseText);
                totalTokensUsed.addAndGet(estimatedTokens);
                
                // Cache response
                responseCache.put(cacheKey, new CachedResponse(responseText));
                
                return responseText;
            } catch (Exception e) {
                // Increment failure count
                failureCount.incrementAndGet();
                
                LOG.error("Error sending request", e);
                
                // Notify user
                Notification notification = new Notification(
                        "ModForge Notifications",
                        "AI Service Error",
                        "Error sending request to AI service: " + e.getMessage(),
                        NotificationType.ERROR
                );
                
                Notifications.Bus.notify(notification, project);
                
                throw new RuntimeException("Error sending request to AI service", e);
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Parses the OpenAI API response.
     * @param response The response to parse
     * @return The parsed response
     * @throws IOException If there is an error parsing the response
     */
    @NotNull
    private String parseResponse(@NotNull String response) throws IOException {
        // Check for error response
        if (response.contains("\"error\":")) {
            String errorMessage = extractJsonField(response, "message");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new IOException("API error: " + errorMessage);
            }
            
            throw new IOException("Unknown API error");
        }
        
        // Extract choice content
        String content = extractJsonField(
                extractJsonField(extractJsonField(response, "choices"), "message"), "content");
        
        return content != null ? content : "";
    }
    
    /**
     * Extracts a field from a JSON string.
     * @param json The JSON string
     * @param field The field to extract
     * @return The extracted field, or null if not found
     */
    @Nullable
    private String extractJsonField(@Nullable String json, @NotNull String field) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        String fieldPattern = "\"" + field + "\"\\s*:\\s*";
        
        // Check if field exists
        int fieldStart = json.indexOf(fieldPattern);
        if (fieldStart < 0) {
            return null;
        }
        
        // Find start of value
        int valueStart = json.indexOf(":", fieldStart) + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        // Check if value is object or array
        char startChar = json.charAt(valueStart);
        
        if (startChar == '{') {
            // Object
            int braceCount = 1;
            int i = valueStart + 1;
            
            while (i < json.length() && braceCount > 0) {
                char c = json.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
                i++;
            }
            
            return json.substring(valueStart, i);
        } else if (startChar == '[') {
            // Array
            int bracketCount = 1;
            int i = valueStart + 1;
            
            while (i < json.length() && bracketCount > 0) {
                char c = json.charAt(i);
                if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                }
                i++;
            }
            
            return json.substring(valueStart, i);
        } else if (startChar == '"') {
            // String
            int i = valueStart + 1;
            
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '"' && json.charAt(i - 1) != '\\') {
                    break;
                }
                i++;
            }
            
            return json.substring(valueStart + 1, i).replaceAll("\\\\\"", "\"").replaceAll("\\\\n", "\n");
        } else {
            // Number, boolean, or null
            int i = valueStart;
            
            while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}' && json.charAt(i) != ']') {
                i++;
            }
            
            return json.substring(valueStart, i).trim();
        }
    }
    
    /**
     * Escapes a string for use in JSON.
     * @param string The string to escape
     * @return The escaped string
     */
    @NotNull
    private String escapeJson(@NotNull String string) {
        return string.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Estimates the number of tokens in a string.
     * @param string The string to estimate
     * @return The estimated number of tokens
     */
    private int estimateTokenCount(@NotNull String string) {
        // Rough estimation: 1 token ≈ 4 characters
        return string.length() / 4;
    }
    
    /**
     * Generates a cache key.
     * @param systemPrompt The system prompt
     * @param userPrompt The user prompt
     * @param requestType The request type
     * @return The cache key
     */
    @NotNull
    private String generateCacheKey(@NotNull String systemPrompt, @NotNull String userPrompt, 
                                  @NotNull String requestType) {
        return requestType + ":" + Objects.hash(systemPrompt, userPrompt);
    }
    
    /**
     * Cleans up the response cache.
     */
    private void cleanupCache() {
        LOG.info("Cleaning up response cache");
        
        // Remove expired entries
        responseCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Gets the request count.
     * @return The request count
     */
    public long getRequestCount() {
        return requestCount.get();
    }
    
    /**
     * Gets the cache hit count.
     * @return The cache hit count
     */
    public long getCacheHitCount() {
        return cacheHitCount.get();
    }
    
    /**
     * Gets the failure count.
     * @return The failure count
     */
    public long getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Gets the total tokens used.
     * @return The total tokens used
     */
    public long getTotalTokensUsed() {
        return totalTokensUsed.get();
    }
    
    /**
     * Gets the estimated cost in USD.
     * @return The estimated cost
     */
    public double getEstimatedCost() {
        // GPT-4 pricing: $0.03 per 1K tokens
        return totalTokensUsed.get() * 0.03 / 1000.0;
    }
    
    /**
     * Cached API response.
     */
    private static class CachedResponse {
        private final String response;
        private final long timestamp;
        private static final long EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1);
        
        /**
         * Creates a new CachedResponse.
         * @param response The response
         */
        public CachedResponse(@NotNull String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Gets the response.
         * @return The response
         */
        @NotNull
        public String getResponse() {
            return response;
        }
        
        /**
         * Checks if the response is expired.
         * @return Whether the response is expired
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRATION_TIME;
        }
    }
}