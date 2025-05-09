package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that manages AI service interactions.
 * This service is responsible for handling API calls to OpenAI and tracking usage metrics.
 */
@Service
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4-0125-preview";
    
    private final ExecutorService executor;
    private final HttpClient httpClient;
    private final AtomicInteger tokenUsage = new AtomicInteger(0);
    private final AtomicInteger apiCalls = new AtomicInteger(0);
    private final AtomicInteger successfulCalls = new AtomicInteger(0);
    private final AtomicInteger failedCalls = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    
    /**
     * Creates a new AIServiceManager.
     */
    public AIServiceManager() {
        this.executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("AIServiceManager", 4);
        this.httpClient = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        LOG.info("AI service manager created");
    }
    
    /**
     * Gets the AI service manager instance.
     * @return The AI service manager
     */
    public static AIServiceManager getInstance() {
        return ApplicationManager.getApplication().getService(AIServiceManager.class);
    }
    
    /**
     * Sends a chat completion request to OpenAI.
     * @param prompt The prompt
     * @param options The options
     * @return A future that completes with the response text
     */
    public CompletableFuture<String> generateChatCompletion(@NotNull String prompt, @Nullable Map<String, Object> options) {
        // Get API key from settings
        String apiKey = ModForgeSettings.getInstance().getOpenAIApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("OpenAI API key not set. Please configure it in the ModForge settings.")
            );
        }
        
        // Create options map if null
        Map<String, Object> requestOptions = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", requestOptions.getOrDefault("model", DEFAULT_MODEL));
        requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", requestOptions.getOrDefault("systemPrompt", "You are a helpful assistant for Minecraft mod development.")),
                Map.of("role", "user", "content", prompt)
        });
        requestBody.put("max_tokens", requestOptions.getOrDefault("maxTokens", 2048));
        requestBody.put("temperature", requestOptions.getOrDefault("temperature", 0.7));
        
        // Convert to JSON
        String requestBodyJson = toJson(requestBody);
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
        
        // Send request
        return sendRequest(request, 0)
                .thenApply(response -> {
                    // Parse response
                    try {
                        Map<String, Object> responseJson = fromJson(response);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> choice = ((java.util.List<Map<String, Object>>) responseJson.get("choices")).get(0);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        
                        String content = (String) message.get("content");
                        
                        // Track token usage
                        if (responseJson.containsKey("usage")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> usage = (Map<String, Object>) responseJson.get("usage");
                            
                            int totalTokens = ((Number) usage.get("total_tokens")).intValue();
                            tokenUsage.addAndGet(totalTokens);
                        }
                        
                        // Track successful call
                        successfulCalls.incrementAndGet();
                        
                        return content;
                    } catch (Exception e) {
                        // Track failed call
                        failedCalls.incrementAndGet();
                        
                        throw new RuntimeException("Failed to parse OpenAI response", e);
                    }
                });
    }
    
    /**
     * Sends an HTTP request with retry logic.
     * @param request The HTTP request
     * @param retryCount The current retry count
     * @return A future that completes with the response body
     */
    private CompletableFuture<String> sendRequest(HttpRequest request, int retryCount) {
        // Track API call
        apiCalls.incrementAndGet();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    int statusCode = response.statusCode();
                    
                    if (isSuccessful(statusCode)) {
                        return CompletableFuture.completedFuture(response.body());
                    } else if (isRetryable(statusCode) && retryCount < MAX_RETRIES) {
                        // Retry after delay
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                Thread.sleep(RETRY_DELAY_MS * (retryCount + 1));
                                return null;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }, executor).thenCompose(v -> sendRequest(request, retryCount + 1));
                    } else {
                        // Not retryable or max retries reached
                        return CompletableFuture.failedFuture(
                                new IOException("HTTP error " + statusCode + ": " + response.body())
                        );
                    }
                });
    }
    
    /**
     * Checks if a status code indicates a successful response.
     * @param statusCode The status code
     * @return True if the status code indicates a successful response
     */
    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Checks if a status code indicates a retryable error.
     * @param statusCode The status code
     * @return True if the status code indicates a retryable error
     */
    private boolean isRetryable(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_TOO_MANY_REQUESTS || // 429
               statusCode >= 500; // Server errors
    }
    
    /**
     * Records a pattern match.
     * This means the AI service was not needed because a pattern match was found.
     */
    public void recordPatternMatch() {
        patternMatches.incrementAndGet();
    }
    
    /**
     * Gets token usage.
     * @return The token usage
     */
    public int getTokenUsage() {
        return tokenUsage.get();
    }
    
    /**
     * Gets API call count.
     * @return The API call count
     */
    public int getApiCallCount() {
        return apiCalls.get();
    }
    
    /**
     * Gets successful call count.
     * @return The successful call count
     */
    public int getSuccessfulCallCount() {
        return successfulCalls.get();
    }
    
    /**
     * Gets failed call count.
     * @return The failed call count
     */
    public int getFailedCallCount() {
        return failedCalls.get();
    }
    
    /**
     * Gets pattern match count.
     * @return The pattern match count
     */
    public int getPatternMatchCount() {
        return patternMatches.get();
    }
    
    /**
     * Gets the estimated cost savings from pattern matches.
     * @return The estimated cost savings in USD
     */
    public double getEstimatedCostSavings() {
        // Assume average of 1000 tokens per request at $0.01 per 1000 tokens
        return patternMatches.get() * 0.01;
    }
    
    /**
     * Resets usage metrics.
     */
    public void resetMetrics() {
        tokenUsage.set(0);
        apiCalls.set(0);
        successfulCalls.set(0);
        failedCalls.set(0);
        patternMatches.set(0);
    }
    
    /**
     * Converts a map to a JSON string.
     * @param map The map
     * @return The JSON string
     */
    private String toJson(Map<String, Object> map) {
        // Simple JSON serialization
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            appendJsonValue(sb, entry.getValue());
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Appends a JSON value to a string builder.
     * @param sb The string builder
     * @param value The value
     */
    private void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            sb.append(toJson((Map<String, Object>) value));
        } else if (value instanceof Object[]) {
            sb.append("[");
            boolean first = true;
            for (Object item : (Object[]) value) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                appendJsonValue(sb, item);
            }
            sb.append("]");
        } else if (value instanceof java.util.List) {
            sb.append("[");
            boolean first = true;
            for (Object item : (java.util.List<?>) value) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                appendJsonValue(sb, item);
            }
            sb.append("]");
        } else {
            sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }
    
    /**
     * Escapes a string for JSON.
     * @param str The string
     * @return The escaped string
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
    
    /**
     * Parses a JSON string into a map.
     * Note: This is a very simplistic JSON parser that only handles
     * the structures returned by the OpenAI API. For a production system,
     * a proper JSON library should be used.
     * @param json The JSON string
     * @return The parsed map
     */
    private Map<String, Object> fromJson(String json) {
        // For simplicity, we're using gson. In a real implementation, 
        // you would use a proper JSON library like Gson or Jackson.
        // This method is mocked to simplify the example.
        // In a real implementation, replace this with proper JSON parsing.
        
        // Mock implementation for demonstration
        Map<String, Object> result = new HashMap<>();
        
        // Extract content
        if (json.contains("\"choices\"")) {
            java.util.List<Map<String, Object>> choices = new java.util.ArrayList<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            
            // Extract content between "content": " and the next "
            int contentStart = json.indexOf("\"content\":") + 11;
            int contentEnd = json.indexOf("\"", contentStart + 1);
            String content = json.substring(contentStart, contentEnd);
            
            message.put("content", content);
            choice.put("message", message);
            choices.add(choice);
            result.put("choices", choices);
        }
        
        // Extract usage
        if (json.contains("\"total_tokens\"")) {
            Map<String, Object> usage = new HashMap<>();
            
            // Extract total tokens
            int tokensStart = json.indexOf("\"total_tokens\":") + 15;
            int tokensEnd = json.indexOf(",", tokensStart);
            if (tokensEnd == -1) {
                tokensEnd = json.indexOf("}", tokensStart);
            }
            int totalTokens = Integer.parseInt(json.substring(tokensStart, tokensEnd).trim());
            
            usage.put("total_tokens", totalTokens);
            result.put("usage", usage);
        }
        
        return result;
    }
}