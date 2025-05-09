package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.models.*;
import com.modforge.intellij.plugin.utils.ApiRequestUtil;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Manages AI services and API interactions between the IDE plugin and the web platform.
 * Handles API requests, pattern sync, and remote code generation.
 */
@Service
public final class AIServiceManager {
    private static final Logger LOG = Logger.getInstance(AIServiceManager.class);
    
    private final Gson gson;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    
    // API endpoints
    private String apiBaseUrl = "https://modforge.ai/api"; // Can be configured in settings
    private String patternsEndpoint = "/patterns";
    private String codeGenerationEndpoint = "/generate-code";
    private String errorResolutionEndpoint = "/resolve-error";
    private String testModEndpoint = "/test-mod";
    
    // Service status
    private boolean isConnected = false;
    private boolean useRemoteServices = true;
    
    public AIServiceManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        executorService = Executors.newCachedThreadPool();
        
        // Check connection to web platform on startup
        checkConnection();
    }
    
    /**
     * Checks the connection to the web platform.
     */
    public void checkConnection() {
        try {
            Request request = new Request.Builder()
                    .url(apiBaseUrl + "/health")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                isConnected = response.isSuccessful();
                LOG.info("ModForge AI web platform connection status: " + (isConnected ? "Connected" : "Disconnected"));
            }
        } catch (Exception e) {
            isConnected = false;
            LOG.warn("Could not connect to ModForge AI web platform", e);
        }
    }
    
    /**
     * Returns whether the plugin is connected to the web platform.
     */
    public boolean isConnectedToWebPlatform() {
        return isConnected;
    }
    
    /**
     * Sets whether to use remote services for computation-intensive tasks.
     */
    public void setUseRemoteServices(boolean useRemote) {
        this.useRemoteServices = useRemote;
    }
    
    /**
     * Uploads patterns to the web platform.
     * @param patterns The patterns to upload
     * @return True if successful, false otherwise
     */
    public boolean uploadPatterns(List<Pattern> patterns) {
        if (!isConnected || patterns.isEmpty()) {
            return false;
        }
        
        try {
            String json = gson.toJson(patterns);
            RequestBody body = RequestBody.create(json, ApiRequestUtil.JSON);
            
            Request request = new Request.Builder()
                    .url(apiBaseUrl + patternsEndpoint)
                    .post(body)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                if (success) {
                    LOG.info("Successfully uploaded " + patterns.size() + " patterns to web platform");
                } else {
                    LOG.warn("Failed to upload patterns. Status: " + response.code());
                }
                return success;
            }
        } catch (Exception e) {
            LOG.error("Error uploading patterns", e);
            return false;
        }
    }
    
    /**
     * Downloads the latest patterns from the web platform.
     * @param lastSyncTimestamp The timestamp of the last sync
     * @return The downloaded patterns
     */
    public List<Pattern> downloadLatestPatterns(long lastSyncTimestamp) {
        if (!isConnected) {
            return new ArrayList<>();
        }
        
        try {
            HttpUrl url = HttpUrl.parse(apiBaseUrl + patternsEndpoint).newBuilder()
                    .addQueryParameter("since", String.valueOf(lastSyncTimestamp))
                    .build();
            
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    Type patternListType = new TypeToken<ArrayList<Pattern>>(){}.getType();
                    List<Pattern> patterns = gson.fromJson(response.body().string(), patternListType);
                    LOG.info("Downloaded " + patterns.size() + " patterns from web platform");
                    return patterns;
                } else {
                    LOG.warn("Failed to download patterns. Status: " + response.code());
                    return new ArrayList<>();
                }
            }
        } catch (Exception e) {
            LOG.error("Error downloading patterns", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generates code using the web platform or local services.
     * @param request The code generation request
     * @return The generated code response, or null if generation failed
     */
    @Nullable
    public CodeGenerationResponse generateCode(@NotNull CodeGenerationRequest request) {
        // If using remote services and connected to web platform, use remote code generation
        if (useRemoteServices && isConnected) {
            return generateCodeRemotely(request);
        } else {
            // Otherwise, use local code generation
            return generateCodeLocally(request);
        }
    }
    
    /**
     * Generates code using the remote web platform.
     * @param request The code generation request
     * @return The generated code response, or null if generation failed
     */
    @Nullable
    private CodeGenerationResponse generateCodeRemotely(@NotNull CodeGenerationRequest request) {
        try {
            String json = gson.toJson(request);
            RequestBody body = RequestBody.create(json, ApiRequestUtil.JSON);
            
            Request httpRequest = new Request.Builder()
                    .url(apiBaseUrl + codeGenerationEndpoint)
                    .post(body)
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return gson.fromJson(response.body().string(), CodeGenerationResponse.class);
                } else {
                    LOG.warn("Failed to generate code remotely. Status: " + response.code());
                    return null;
                }
            }
        } catch (Exception e) {
            LOG.error("Error generating code remotely", e);
            return null;
        }
    }
    
    /**
     * Generates code using local services.
     * @param request The code generation request
     * @return The generated code response, or null if generation failed
     */
    @Nullable
    private CodeGenerationResponse generateCodeLocally(@NotNull CodeGenerationRequest request) {
        // This would be implemented with local OpenAI API calls
        // For now, we'll just return a placeholder
        LOG.info("Local code generation not yet implemented. Falling back to remote generation");
        return generateCodeRemotely(request);
    }
    
    /**
     * Resolves an error using the web platform or local services.
     * @param request The error resolution request
     * @return The error resolution response, or null if resolution failed
     */
    @Nullable
    public ErrorResolutionResponse resolveError(@NotNull ErrorResolutionRequest request) {
        // If using remote services and connected to web platform, use remote error resolution
        if (useRemoteServices && isConnected) {
            return resolveErrorRemotely(request);
        } else {
            // Otherwise, use local error resolution
            return resolveErrorLocally(request);
        }
    }
    
    /**
     * Resolves an error using the remote web platform.
     * @param request The error resolution request
     * @return The error resolution response, or null if resolution failed
     */
    @Nullable
    private ErrorResolutionResponse resolveErrorRemotely(@NotNull ErrorResolutionRequest request) {
        try {
            String json = gson.toJson(request);
            RequestBody body = RequestBody.create(json, ApiRequestUtil.JSON);
            
            Request httpRequest = new Request.Builder()
                    .url(apiBaseUrl + errorResolutionEndpoint)
                    .post(body)
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return gson.fromJson(response.body().string(), ErrorResolutionResponse.class);
                } else {
                    LOG.warn("Failed to resolve error remotely. Status: " + response.code());
                    return null;
                }
            }
        } catch (Exception e) {
            LOG.error("Error resolving error remotely", e);
            return null;
        }
    }
    
    /**
     * Resolves an error using local services.
     * @param request The error resolution request
     * @return The error resolution response, or null if resolution failed
     */
    @Nullable
    private ErrorResolutionResponse resolveErrorLocally(@NotNull ErrorResolutionRequest request) {
        // This would be implemented with local pattern matching and OpenAI API calls
        // For now, we'll just return a placeholder
        LOG.info("Local error resolution not yet implemented. Falling back to remote resolution");
        return resolveErrorRemotely(request);
    }
    
    /**
     * Tests a mod on the remote testing infrastructure.
     * @param request The test request
     * @return The test response, or null if testing failed
     */
    @Nullable
    public ModTestResponse testMod(@NotNull ModTestRequest request) {
        if (!isConnected) {
            LOG.warn("Not connected to web platform, unable to test mod remotely");
            return null;
        }
        
        try {
            String json = gson.toJson(request);
            RequestBody body = RequestBody.create(json, ApiRequestUtil.JSON);
            
            Request httpRequest = new Request.Builder()
                    .url(apiBaseUrl + testModEndpoint)
                    .post(body)
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return gson.fromJson(response.body().string(), ModTestResponse.class);
                } else {
                    LOG.warn("Failed to test mod. Status: " + response.code());
                    return null;
                }
            }
        } catch (Exception e) {
            LOG.error("Error testing mod", e);
            return null;
        }
    }
    
    /**
     * Sets the API base URL.
     * @param url The new base URL
     */
    public void setApiBaseUrl(String url) {
        this.apiBaseUrl = url;
        // Check connection with new URL
        checkConnection();
    }
}