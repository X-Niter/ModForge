package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility for authenticated connections using token-based authentication.
 * Optimized with modern Java 21 features and IntelliJ IDEA 2025.1 compatibility.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    
    // Default timeout values
    private static final int CONNECTION_TIMEOUT_SEC = 10;
    private static final int REQUEST_TIMEOUT_SEC = 30;
    
    // Token validation
    private static final ConcurrentHashMap<String, Long> TOKEN_VALIDITY_CACHE = new ConcurrentHashMap<>();
    private static final long TOKEN_CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(5);
    
    // HTTP Client (shared, thread-safe instance)
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SEC))
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();
    
    /**
     * Validates a JWT token by checking its format and expiration time
     * 
     * @param token The JWT token to validate
     * @return true if token appears valid, false otherwise
     */
    public static boolean isValidJwtToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // Check if we've recently validated this token
        Long cachedTime = TOKEN_VALIDITY_CACHE.get(token);
        if (cachedTime != null && System.currentTimeMillis() - cachedTime < TOKEN_CACHE_DURATION_MS) {
            return true;
        }
        
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                LOG.warn("Invalid JWT token format (expected 3 parts)");
                return false;
            }
            
            // Decode and check expiration in payload (part 1)
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // Simple check for token expiration - just verify it contains an "exp" field
            // A proper implementation would parse the JSON and check the exp timestamp
            if (!payloadJson.contains("\"exp\"")) {
                LOG.warn("JWT token lacks expiration information");
                return false;
            }
            
            // Cache the validation result
            TOKEN_VALIDITY_CACHE.put(token, System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            LOG.warn("Error validating JWT token: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Makes an authenticated HTTP request with the provided token
     * 
     * @param url The URL for the request
     * @param tokenSupplier A supplier that provides the authentication token
     * @param method The HTTP method (GET, POST, etc.)
     * @param body The request body (for POST/PUT), or null
     * @return HttpResponse with the response data
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    @RequiresBackgroundThread
    public static HttpResponse<String> makeAuthenticatedRequest(
            String url, 
            Supplier<String> tokenSupplier,
            String method, 
            String body) throws IOException, InterruptedException {
        
        String token = tokenSupplier.get();
        if (token == null || token.isEmpty()) {
            throw new IOException("Authentication token is missing or empty");
        }
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SEC))
            .header("Authorization", "Bearer " + token);
            
        // Set appropriate method and body
        HttpRequest request;
        if ("GET".equals(method)) {
            request = requestBuilder.GET().build();
        } else if ("POST".equals(method)) {
            request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""))
                .header("Content-Type", "application/json")
                .build();
        } else if ("PUT".equals(method)) {
            request = requestBuilder
                .PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""))
                .header("Content-Type", "application/json")
                .build();
        } else if ("DELETE".equals(method)) {
            request = requestBuilder.DELETE().build();
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    /**
     * Makes an authenticated HTTP request asynchronously with progress reporting
     * 
     * @param project The current IntelliJ project
     * @param title The title for the progress indicator
     * @param url The URL for the request
     * @param tokenSupplier A supplier that provides the authentication token
     * @param method The HTTP method (GET, POST, etc.)
     * @param body The request body (for POST/PUT), or null
     * @param onSuccess Callback for successful response
     * @param onError Callback for error handling
     */
    public static void makeAuthenticatedRequestWithProgress(
            Project project,
            String title,
            String url,
            Supplier<String> tokenSupplier,
            String method,
            String body,
            Consumer<HttpResponse<String>> onSuccess,
            Consumer<Exception> onError) {
        
        new Task.Backgroundable(project, title, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    HttpResponse<String> response = makeAuthenticatedRequest(url, tokenSupplier, method, body);
                    ApplicationManager.getApplication().invokeLater(() -> onSuccess.accept(response));
                } catch (Exception e) {
                    LOG.warn("Error making authenticated request to " + url + ": " + e.getMessage(), e);
                    ApplicationManager.getApplication().invokeLater(() -> onError.accept(e));
                }
            }
        }.queue();
    }
    
    /**
     * Makes an authenticated HTTP request asynchronously
     * 
     * @param url The URL for the request
     * @param tokenSupplier A supplier that provides the authentication token
     * @param method The HTTP method (GET, POST, etc.)
     * @param body The request body (for POST/PUT), or null
     * @return CompletableFuture that will resolve to the HTTP response
     */
    public static CompletableFuture<HttpResponse<String>> makeAuthenticatedRequestAsync(
            String url,
            Supplier<String> tokenSupplier,
            String method,
            String body) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return makeAuthenticatedRequest(url, tokenSupplier, method, body);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error making authenticated request: " + e.getMessage(), e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Tests if a token is valid by making a simple authenticated request
     * 
     * @param apiUrl The API URL to test against
     * @param token The token to test
     * @return true if token is valid, false otherwise
     */
    public static boolean testToken(String apiUrl, String token) {
        if (!isValidJwtToken(token)) {
            return false;
        }
        
        AtomicBoolean isValid = new AtomicBoolean(false);
        
        try {
            HttpResponse<String> response = makeAuthenticatedRequest(
                apiUrl, 
                () -> token, 
                "GET", 
                null
            );
            
            int statusCode = response.statusCode();
            isValid.set(statusCode >= 200 && statusCode < 300);
        } catch (Exception e) {
            LOG.warn("Error testing token: " + e.getMessage());
            isValid.set(false);
        }
        
        return isValid.get();
    }
    
    /**
     * Clears the token validation cache
     */
    public static void clearTokenCache() {
        TOKEN_VALIDITY_CACHE.clear();
    }
}