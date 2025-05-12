package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import com.modforge.intellij.plugin.utils.VirtualFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for autonomous code generation using AI.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    // Server API endpoints
    private static final String DEFAULT_API_ENDPOINT = "https://modforge.ai/api";
    private static final String CODE_GENERATION_ENDPOINT = "/generate";
    private static final String CODE_FIX_ENDPOINT = "/fix";
    private static final String CODE_ENHANCE_ENDPOINT = "/enhance";
    private static final String CODE_EXPLAIN_ENDPOINT = "/explain";
    private static final String CODE_DOCUMENT_ENDPOINT = "/document";
    private static final String CODE_IMPLEMENT_ENDPOINT = "/implement";
    private static final String MOD_CODE_ENDPOINT = "/minecraft/generate";
    private static final String MOD_FIX_ENDPOINT = "/minecraft/fix";
    private static final String ADD_FEATURES_ENDPOINT = "/minecraft/add-features";
    
    // HTTP parameters
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_SERVER_ERROR = 500;
    
    // Service instances
    private final ModForgeSettings settings;
    private final ModForgeNotificationService notificationService;
    private final Project project;
    
    // JSON parser
    private final Gson gson = new Gson();
    
    // Request timeout
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Constructor.
     *
     * @param project The project.
     */
    public AutonomousCodeGenerationService(Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance();
    }

    /**
     * Gets the instance of the service.
     *
     * @param project The project.
     * @return The service instance.
     */
    public static AutonomousCodeGenerationService getInstance(@NotNull Project project) {
        return project.getService(AutonomousCodeGenerationService.class);
    }

    /**
     * Generates code from a prompt.
     *
     * @param prompt   The prompt.
     * @param file     The file to add context, can be null.
     * @param language The language to generate.
     * @return A CompletableFuture with the generated code.
     */
    public CompletableFuture<String> generateCode(@NotNull String prompt, @Nullable VirtualFile file, @NotNull String language) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("prompt", prompt);
        requestData.put("language", language);
        
        if (file != null) {
            String fileContent = VirtualFileUtil.readFile(file);
            if (fileContent != null) {
                requestData.put("context", fileContent);
            }
        }
        
        return sendRequest(CODE_GENERATION_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.get("code").getAsString();
                });
    }

    /**
     * Fixes code with errors.
     *
     * @param code         The code to fix.
     * @param errorMessage The error message.
     * @param language     The language.
     * @return A CompletableFuture with the fixed code.
     */
    public CompletableFuture<String> fixCode(@NotNull String code, @NotNull String errorMessage, @NotNull String language) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("code", code);
        requestData.put("error", errorMessage);
        requestData.put("language", language);
        
        return sendRequest(CODE_FIX_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.get("fixedCode").getAsString();
                });
    }

    /**
     * Enhances code.
     *
     * @param code        The code to enhance.
     * @param instruction The enhancement instruction.
     * @param language    The language.
     * @return A CompletableFuture with the enhanced code.
     */
    public CompletableFuture<String> enhanceCode(@NotNull String code, @NotNull String instruction, @NotNull String language) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("code", code);
        requestData.put("instruction", instruction);
        requestData.put("language", language);
        
        return sendRequest(CODE_ENHANCE_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.get("enhancedCode").getAsString();
                });
    }

    /**
     * Explains code.
     *
     * @param code     The code to explain.
     * @param language The language.
     * @return A CompletableFuture with the explanation.
     */
    public CompletableFuture<String> explainCode(@NotNull String code, @Nullable String language) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("code", code);
        
        if (language != null) {
            requestData.put("language", language);
        }
        
        return sendRequest(CODE_EXPLAIN_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.get("explanation").getAsString();
                });
    }

    /**
     * Generates documentation for code.
     *
     * @param code     The code to document.
     * @param language The language.
     * @return A CompletableFuture with the documented code.
     */
    public CompletableFuture<String> generateDocumentation(@NotNull String code, @Nullable String language) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("code", code);
        
        if (language != null) {
            requestData.put("language", language);
        }
        
        return sendRequest(CODE_DOCUMENT_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.get("documentedCode").getAsString();
                });
    }

    /**
     * Generates an implementation from a prompt.
     *
     * @param prompt      The prompt.
     * @param filePath    The file path to generate.
     * @param language    The language to generate.
     * @return Whether the implementation was generated successfully.
     */
    public boolean generateImplementation(@NotNull String prompt, @NotNull String filePath, @NotNull String language) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("prompt", prompt);
        requestData.put("filePath", filePath);
        requestData.put("language", language);
        
        try {
            String response = sendRequest(CODE_IMPLEMENT_ENDPOINT, requestData).get(60, TimeUnit.SECONDS);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            
            if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                String code = jsonResponse.get("code").getAsString();
                
                // Create the file with the generated code
                int lastSlashIndex = filePath.lastIndexOf('/');
                String directoryPath = filePath.substring(0, lastSlashIndex);
                String fileName = filePath.substring(lastSlashIndex + 1);
                
                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    LOG.error("Project base directory is null");
                    return false;
                }
                
                VirtualFile directory = VirtualFileUtil.createDirectoryStructure(baseDir, directoryPath);
                if (directory == null) {
                    LOG.error("Failed to create directory: " + directoryPath);
                    return false;
                }
                
                VirtualFile file = VirtualFileUtil.createFile(directory, fileName, code);
                return file != null;
            }
            
            return false;
        } catch (Exception e) {
            LOG.error("Failed to generate implementation", e);
            return false;
        }
    }

    /**
     * Generates Minecraft mod code.
     *
     * @param modName      The mod name.
     * @param modId        The mod ID.
     * @param description  The mod description.
     * @param modLoader    The mod loader.
     * @param mcVersion    The Minecraft version.
     * @param features     The mod features.
     * @return A CompletableFuture with the result message.
     */
    public CompletableFuture<String> generateModCode(
            @NotNull String modName,
            @NotNull String modId,
            @NotNull String description,
            @NotNull String modLoader,
            @NotNull String mcVersion,
            @NotNull String features) {
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("modName", modName);
        requestData.put("modId", modId);
        requestData.put("description", description);
        requestData.put("modLoader", modLoader);
        requestData.put("mcVersion", mcVersion);
        requestData.put("features", features);
        
        return sendRequest(MOD_CODE_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        return "Successfully generated mod: " + modName;
                    } else {
                        String errorMessage = "Unknown error";
                        if (jsonResponse.has("error")) {
                            errorMessage = jsonResponse.get("error").getAsString();
                        }
                        return "Failed to generate mod: " + errorMessage;
                    }
                });
    }

    /**
     * Fixes a Minecraft mod with errors.
     *
     * @param modId       The mod ID.
     * @param errorLog    The error log.
     * @param modLoader   The mod loader.
     * @param mcVersion   The Minecraft version.
     * @return A CompletableFuture with the result message.
     */
    public CompletableFuture<String> fixModCode(
            @NotNull String modId,
            @NotNull String errorLog,
            @NotNull String modLoader,
            @NotNull String mcVersion) {
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("modId", modId);
        requestData.put("errorLog", errorLog);
        requestData.put("modLoader", modLoader);
        requestData.put("mcVersion", mcVersion);
        
        return sendRequest(MOD_FIX_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        return "Successfully fixed mod: " + modId;
                    } else {
                        String errorMessage = "Unknown error";
                        if (jsonResponse.has("error")) {
                            errorMessage = jsonResponse.get("error").getAsString();
                        }
                        return "Failed to fix mod: " + errorMessage;
                    }
                });
    }

    /**
     * Adds features to a Minecraft mod.
     *
     * @param modId     The mod ID.
     * @param features  The features to add.
     * @param modLoader The mod loader.
     * @param mcVersion The Minecraft version.
     * @return A CompletableFuture with the result message.
     */
    public CompletableFuture<String> addFeatures(
            @NotNull String modId,
            @NotNull String features,
            @NotNull String modLoader,
            @NotNull String mcVersion) {
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("modId", modId);
        requestData.put("features", features);
        requestData.put("modLoader", modLoader);
        requestData.put("mcVersion", mcVersion);
        
        return sendRequest(ADD_FEATURES_ENDPOINT, requestData)
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        return "Successfully added features to mod: " + modId;
                    } else {
                        String errorMessage = "Unknown error";
                        if (jsonResponse.has("error")) {
                            errorMessage = jsonResponse.get("error").getAsString();
                        }
                        return "Failed to add features: " + errorMessage;
                    }
                });
    }

    /**
     * Sends a request to the API.
     *
     * @param endpoint   The API endpoint.
     * @param requestData The request data.
     * @return A CompletableFuture with the response.
     */
    @RequiresBackgroundThread
    private CompletableFuture<String> sendRequest(@NotNull String endpoint, @NotNull Map<String, Object> requestData) {
        String apiUrl = settings.getServerUrl() + endpoint;
        boolean usePatternRecognition = settings.isPatternRecognition();
        
        requestData.put("usePatternRecognition", usePatternRecognition);
        
        String authToken = settings.getAccessToken();
        if (authToken != null && !authToken.isEmpty()) {
            requestData.put("token", authToken);
        }
        
        Duration timeout = Duration.ofSeconds(settings.getRequestTimeout());
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout((int) timeout.toMillis());
                connection.setReadTimeout((int) timeout.toMillis());
                connection.setDoOutput(true);
                
                String requestBody = gson.toJson(requestData);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        return response.toString();
                    }
                } else {
                    String errorMessage;
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        errorMessage = response.toString();
                    } catch (Exception e) {
                        errorMessage = "Unknown error";
                    }
                    
                    throw new IOException("API request failed with status " + responseCode + ": " + errorMessage);
                }
            } catch (Exception e) {
                LOG.error("Failed to send request to API", e);
                throw new RuntimeException("Failed to send request to API: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Tests the API connection.
     *
     * @return A CompletableFuture with whether the connection was successful.
     */
    public CompletableFuture<Boolean> testConnection() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("test", true);
        
        CompletableFuture<String> responseFuture = sendRequest("/ping", requestData);
        
        return responseFuture
                .thenApply(response -> {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean();
                })
                .exceptionally(e -> {
                    LOG.error("API connection test failed", e);
                    return false;
                });
    }

    /**
     * Gets the API endpoint URL.
     *
     * @return The API endpoint URL.
     */
    @NotNull
    public String getApiEndpoint() {
        return settings.getServerUrl();
    }

    /**
     * Sets the API endpoint URL.
     *
     * @param apiEndpoint The API endpoint URL.
     */
    public void setApiEndpoint(@NotNull String apiEndpoint) {
        settings.setServerUrl(apiEndpoint);
    }

    /**
     * Gets the request timeout.
     *
     * @return The request timeout.
     */
    @NotNull
    public Duration getRequestTimeout() {
        return Duration.ofSeconds(settings.getRequestTimeout());
    }

    /**
     * Sets the request timeout.
     *
     * @param timeout The request timeout.
     */
    public void setRequestTimeout(@NotNull Duration timeout) {
        settings.setRequestTimeout((int) timeout.getSeconds());
    }

    /**
     * Checks if the API endpoint is valid.
     *
     * @param apiEndpoint The API endpoint URL.
     * @return Whether the API endpoint is valid.
     */
    public static boolean isValidApiEndpoint(@NotNull String apiEndpoint) {
        return apiEndpoint.startsWith("http://") || apiEndpoint.startsWith("https://");
    }

    /**
     * Resets the API endpoint to the default.
     */
    public void resetApiEndpoint() {
        settings.setServerUrl(DEFAULT_API_ENDPOINT);
    }
}