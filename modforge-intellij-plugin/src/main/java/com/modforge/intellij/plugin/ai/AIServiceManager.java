package com.modforge.intellij.plugin.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for interacting with AI APIs.
 * This service provides methods for generating code, fixing code, and other AI-powered features.
 */
@Service(Service.Level.PROJECT)
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4";
    private static final int DEFAULT_MAX_TOKENS = 2000;
    private static final int REQUEST_TIMEOUT_SECONDS = 60;
    
    private final Project project;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final PatternRecognitionService patternRecognitionService;
    
    // Statistics
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalTokensUsed = new AtomicLong(0);
    private final AtomicLong totalApiCalls = new AtomicLong(0);
    
    /**
     * Creates a new AIServiceManager.
     * @param project The project
     */
    public AIServiceManager(@NotNull Project project) {
        this.project = project;
        
        // Create HTTP client
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        
        this.gson = new Gson();
        this.patternRecognitionService = project.getService(PatternRecognitionService.class);
        
        LOG.info("AIServiceManager initialized for project: " + project.getName());
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
     * Generates code.
     * @param prompt The prompt
     * @param language The programming language
     * @param options Additional options
     * @return A future that completes with the generated code
     */
    @NotNull
    public CompletableFuture<String> generateCode(@NotNull String prompt, @NotNull String language,
                                                @Nullable Map<String, Object> options) {
        String patternType = "code_generation";
        Map<String, Object> context = new HashMap<>();
        context.put("language", language);
        
        if (options != null) {
            context.putAll(options);
        }
        
        // Check for matching pattern
        if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
            PatternRecognitionService.RecognizedPattern pattern = 
                    patternRecognitionService.findMatchingPattern(patternType, prompt, context);
            
            if (pattern != null) {
                LOG.info("Using cached code generation response for prompt: " + truncateForLogging(prompt));
                cacheHitCount.incrementAndGet();
                return CompletableFuture.completedFuture(pattern.getResponse());
            }
        }
        
        // Create messages
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", String.format(
                "You are an expert %s programmer. Generate well-structured, idiomatic %s code in response to the user's request. " +
                "Focus on generating clean, maintainable code with appropriate error handling. " +
                "Include necessary imports and comments where appropriate. " +
                "Output ONLY the code without any surrounding markdown or explanation.",
                language, language));
        messages.add(systemMessage);
        
        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);
        
        // Create request
        return sendOpenAiRequest(messages, DEFAULT_MAX_TOKENS)
                .thenApply(response -> {
                    // Store pattern
                    if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
                        patternRecognitionService.addPattern(patternType, prompt, response, context);
                    }
                    
                    return response;
                });
    }
    
    /**
     * Fixes code.
     * @param code The code to fix
     * @param errorMessage The error message
     * @param options Additional options
     * @return A future that completes with the fixed code
     */
    @NotNull
    public CompletableFuture<String> fixCode(@NotNull String code, @Nullable String errorMessage,
                                           @Nullable Map<String, Object> options) {
        String patternType = "code_fix";
        Map<String, Object> context = new HashMap<>();
        
        if (errorMessage != null) {
            context.put("errorMessage", errorMessage);
        }
        
        if (options != null) {
            context.putAll(options);
        }
        
        // Check for matching pattern
        if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
            PatternRecognitionService.RecognizedPattern pattern = 
                    patternRecognitionService.findMatchingPattern(patternType, code, context);
            
            if (pattern != null) {
                LOG.info("Using cached code fix response for code: " + truncateForLogging(code));
                cacheHitCount.incrementAndGet();
                return CompletableFuture.completedFuture(pattern.getResponse());
            }
        }
        
        // Create messages
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", 
                "You are an expert programmer specializing in code review and bug fixing. " +
                "Fix the code provided by the user. Only provide the corrected code without explanations. " +
                "If you see compilation errors, logic errors, or potential bugs, fix them. " +
                "If the code is already correct, return it as is.");
        messages.add(systemMessage);
        
        // User message
        StringBuilder userContent = new StringBuilder();
        userContent.append("Fix the following code:");
        userContent.append("\n\n```\n").append(code).append("\n```\n\n");
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            userContent.append("Error message: ").append(errorMessage).append("\n\n");
        }
        
        userContent.append("Return only the fixed code without explanations or markdown formatting.");
        
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userContent.toString());
        messages.add(userMessage);
        
        // Create request
        return sendOpenAiRequest(messages, DEFAULT_MAX_TOKENS)
                .thenApply(response -> {
                    // Clean up response
                    String cleanResponse = cleanupCodeResponse(response);
                    
                    // Store pattern
                    if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
                        patternRecognitionService.addPattern(patternType, code, cleanResponse, context);
                    }
                    
                    return cleanResponse;
                });
    }
    
    /**
     * Explains code.
     * @param code The code to explain
     * @param options Additional options
     * @return A future that completes with the explanation
     */
    @NotNull
    public CompletableFuture<String> explainCode(@NotNull String code, @Nullable Map<String, Object> options) {
        String patternType = "code_explanation";
        Map<String, Object> context = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Check for matching pattern
        if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
            PatternRecognitionService.RecognizedPattern pattern = 
                    patternRecognitionService.findMatchingPattern(patternType, code, context);
            
            if (pattern != null) {
                LOG.info("Using cached code explanation response for code: " + truncateForLogging(code));
                cacheHitCount.incrementAndGet();
                return CompletableFuture.completedFuture(pattern.getResponse());
            }
        }
        
        // Create messages
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", 
                "You are an expert programmer specializing in code explanation. " +
                "Explain the code provided by the user in clear, concise terms. " +
                "Break down complex parts, explain the algorithm, and highlight important aspects. " +
                "Keep your explanation well-structured and easy to understand.");
        messages.add(systemMessage);
        
        // User message
        StringBuilder userContent = new StringBuilder();
        userContent.append("Explain the following code:");
        userContent.append("\n\n```\n").append(code).append("\n```\n\n");
        
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userContent.toString());
        messages.add(userMessage);
        
        // Create request
        return sendOpenAiRequest(messages, DEFAULT_MAX_TOKENS)
                .thenApply(response -> {
                    // Store pattern
                    if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
                        patternRecognitionService.addPattern(patternType, code, response, context);
                    }
                    
                    return response;
                });
    }
    
    /**
     * Generates documentation.
     * @param code The code to document
     * @param options Additional options
     * @return A future that completes with the documentation
     */
    @NotNull
    public CompletableFuture<String> generateDocumentation(@NotNull String code, @Nullable Map<String, Object> options) {
        String patternType = "documentation_generation";
        Map<String, Object> context = options != null ? new HashMap<>(options) : new HashMap<>();
        
        // Check for matching pattern
        if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
            PatternRecognitionService.RecognizedPattern pattern = 
                    patternRecognitionService.findMatchingPattern(patternType, code, context);
            
            if (pattern != null) {
                LOG.info("Using cached documentation response for code: " + truncateForLogging(code));
                cacheHitCount.incrementAndGet();
                return CompletableFuture.completedFuture(pattern.getResponse());
            }
        }
        
        // Create messages
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", 
                "You are an expert programmer specializing in code documentation. " +
                "Generate comprehensive documentation for the code provided by the user. " +
                "Include class, method, and parameter documentation in the appropriate format for the language. " +
                "For Java code, use JavaDoc format. For JavaScript, use JSDoc. For Python, use docstrings. " +
                "Focus on explaining what each component does, any parameters, return values, and exceptions." +
                "Return only the documented code, not explanations about it.");
        messages.add(systemMessage);
        
        // User message
        StringBuilder userContent = new StringBuilder();
        userContent.append("Document the following code:");
        userContent.append("\n\n```\n").append(code).append("\n```\n\n");
        userContent.append("Return the code with added documentation.");
        
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userContent.toString());
        messages.add(userMessage);
        
        // Create request
        return sendOpenAiRequest(messages, DEFAULT_MAX_TOKENS)
                .thenApply(response -> {
                    // Clean up response
                    String cleanResponse = cleanupCodeResponse(response);
                    
                    // Store pattern
                    if (ModForgeSettings.getInstance().isUsePatternRecognition()) {
                        patternRecognitionService.addPattern(patternType, code, cleanResponse, context);
                    }
                    
                    return cleanResponse;
                });
    }
    
    /**
     * Sends a request to the OpenAI API.
     * @param messages The messages
     * @param maxTokens The maximum number of tokens
     * @return A future that completes with the response
     */
    @NotNull
    private CompletableFuture<String> sendOpenAiRequest(@NotNull JsonArray messages, int maxTokens) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String apiKey = settings.getOpenAiApiKey();
        String model = settings.getOpenAiModel();
        
        if (StringUtil.isEmpty(apiKey)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("OpenAI API key is not set. Please configure it in the settings."));
        }
        
        if (StringUtil.isEmpty(model)) {
            model = DEFAULT_MODEL;
        }
        
        // Track request
        requestCount.incrementAndGet();
        totalApiCalls.incrementAndGet();
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("temperature", 0.2); // Lower temperature for more deterministic outputs
        requestBody.addProperty("top_p", 1);
        
        // Create request
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        gson.toJson(requestBody)))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();
        
        // Create future
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Send request
        String finalModel = model;
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                LOG.error("OpenAI API request failed", e);
                failureCount.incrementAndGet();
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                
                try {
                    if (!response.isSuccessful() || responseBody == null) {
                        String errorBody = responseBody != null ? responseBody.string() : "Empty response";
                        LOG.error("OpenAI API request failed with status: " + response.code() + ", body: " + errorBody);
                        failureCount.incrementAndGet();
                        future.completeExceptionally(new IOException(
                                "OpenAI API request failed with status: " + response.code() + ", body: " + errorBody));
                        return;
                    }
                    
                    // Parse response
                    String responseString = responseBody.string();
                    JsonObject jsonResponse = gson.fromJson(responseString, JsonObject.class);
                    
                    // Extract content
                    String content = extractContentFromResponse(jsonResponse);
                    
                    // Update token usage
                    if (jsonResponse.has("usage")) {
                        JsonObject usage = jsonResponse.getAsJsonObject("usage");
                        if (usage.has("total_tokens")) {
                            totalTokensUsed.addAndGet(usage.get("total_tokens").getAsInt());
                        }
                    }
                    
                    future.complete(content);
                } catch (Exception e) {
                    LOG.error("Error parsing OpenAI API response", e);
                    failureCount.incrementAndGet();
                    future.completeExceptionally(e);
                } finally {
                    if (responseBody != null) {
                        responseBody.close();
                    }
                    response.close();
                }
            }
        });
        
        return future;
    }
    
    /**
     * Extracts content from a response.
     * @param jsonResponse The JSON response
     * @return The content
     */
    @NotNull
    private String extractContentFromResponse(@NotNull JsonObject jsonResponse) {
        try {
            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                
                if (choice.has("message") && choice.getAsJsonObject("message").has("content")) {
                    return choice.getAsJsonObject("message").get("content").getAsString();
                }
            }
        } catch (Exception e) {
            LOG.error("Error extracting content from response", e);
        }
        
        return "";
    }
    
    /**
     * Cleans up a code response by removing code blocks and extra whitespace.
     * @param response The response
     * @return The cleaned up response
     */
    @NotNull
    private String cleanupCodeResponse(@NotNull String response) {
        // Remove code blocks
        String cleaned = response;
        
        // Remove ```language
        cleaned = cleaned.replaceAll("```\\w*\\s*", "");
        
        // Remove remaining ``` markers
        cleaned = cleaned.replace("```", "");
        
        // Trim whitespace
        cleaned = cleaned.trim();
        
        return cleaned;
    }
    
    /**
     * Truncates a string for logging.
     * @param str The string
     * @return The truncated string
     */
    @NotNull
    private String truncateForLogging(@NotNull String str) {
        return str.length() <= 100 ? str : str.substring(0, 97) + "...";
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
     * Gets the estimated cost.
     * @return The estimated cost
     */
    public double getEstimatedCost() {
        // Simplified cost calculation based on gpt-4 pricing ($0.03 per 1K tokens)
        // For a more accurate calculation, we would need to track input and output tokens separately
        // and use the appropriate pricing for each model
        return totalTokensUsed.get() * 0.03 / 1000.0;
    }
    
    /**
     * Disposes the service.
     */
    public void dispose() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}