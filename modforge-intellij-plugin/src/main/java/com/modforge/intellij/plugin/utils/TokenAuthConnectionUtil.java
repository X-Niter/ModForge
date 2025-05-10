package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for making HTTP requests with token authentication.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Gson GSON = new Gson();
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000; // 1 second delay between retries
    
    /**
     * Execute a GET request asynchronously.
     *
     * @param url   The URL to send the request to
     * @param token The authentication token
     * @return A CompletableFuture that will be resolved to the response body
     */
    public static CompletableFuture<String> executeGet(String url, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeRequest(url, "GET", null, token);
            } catch (IOException e) {
                LOG.error("Error executing GET request", e);
                throw new RuntimeException("Error executing GET request", e);
            }
        }, EXECUTOR);
    }
    
    /**
     * Execute a POST request asynchronously.
     *
     * @param url         The URL to send the request to
     * @param requestBody The request body
     * @param token       The authentication token
     * @return A CompletableFuture that will be resolved to the response body
     */
    public static CompletableFuture<String> executePost(String url, Object requestBody, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = GSON.toJson(requestBody);
                return executeRequest(url, "POST", jsonBody, token);
            } catch (IOException e) {
                LOG.error("Error executing POST request", e);
                throw new RuntimeException("Error executing POST request", e);
            }
        }, EXECUTOR);
    }
    
    /**
     * Execute a PUT request asynchronously.
     *
     * @param url         The URL to send the request to
     * @param requestBody The request body
     * @param token       The authentication token
     * @return A CompletableFuture that will be resolved to the response body
     */
    public static CompletableFuture<String> executePut(String url, Object requestBody, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = GSON.toJson(requestBody);
                return executeRequest(url, "PUT", jsonBody, token);
            } catch (IOException e) {
                LOG.error("Error executing PUT request", e);
                throw new RuntimeException("Error executing PUT request", e);
            }
        }, EXECUTOR);
    }
    
    /**
     * Execute a DELETE request asynchronously.
     *
     * @param url   The URL to send the request to
     * @param token The authentication token
     * @return A CompletableFuture that will be resolved to the response body
     */
    public static CompletableFuture<String> executeDelete(String url, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeRequest(url, "DELETE", null, token);
            } catch (IOException e) {
                LOG.error("Error executing DELETE request", e);
                throw new RuntimeException("Error executing DELETE request", e);
            }
        }, EXECUTOR);
    }
    
    /**
     * Execute a PATCH request asynchronously.
     *
     * @param url         The URL to send the request to
     * @param requestBody The request body
     * @param token       The authentication token
     * @return A CompletableFuture that will be resolved to the response body
     */
    public static CompletableFuture<String> executePatch(String url, Object requestBody, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = GSON.toJson(requestBody);
                return executeRequest(url, "PATCH", jsonBody, token);
            } catch (IOException e) {
                LOG.error("Error executing PATCH request", e);
                throw new RuntimeException("Error executing PATCH request", e);
            }
        }, EXECUTOR);
    }
    
    /**
     * Execute a login request asynchronously.
     *
     * @param url      The URL to send the request to
     * @param username The username
     * @param password The password
     * @return A CompletableFuture that will be resolved to a map containing the token and user information
     */
    public static CompletableFuture<Map<String, Object>> executeLogin(String url, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("username", username);
                requestBody.addProperty("password", password);
                
                String response = executeRequest(url, "POST", requestBody.toString(), null);
                
                // Parse the response as a map
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = GSON.fromJson(response, Map.class);
                
                return responseMap;
            } catch (IOException e) {
                LOG.error("Error executing login request", e);
                throw new RuntimeException("Error executing login request", e);
            }
        }, EXECUTOR);
    }
    
    /**
     * Execute an HTTP request synchronously.
     *
     * @param urlString   The URL to send the request to
     * @param method      The HTTP method
     * @param requestBody The request body (can be null for GET and DELETE requests)
     * @param token       The authentication token (can be null for unauthenticated requests)
     * @return The response body
     * @throws IOException If an I/O error occurs
     */
    private static String executeRequest(String urlString, String method, String requestBody, String token) throws IOException {
        IOException lastException = null;
        
        // Implement retry logic
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            if (retry > 0) {
                LOG.info("Retrying request (" + retry + "/" + MAX_RETRIES + ") to: " + urlString);
                // Wait before retrying
                try {
                    Thread.sleep(RETRY_DELAY_MS * retry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                
                // Set common headers
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                
                // Set token if provided
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
                
                // Set connection properties
                connection.setConnectTimeout(10000); // 10 seconds
                connection.setReadTimeout(30000); // 30 seconds
                
                // Send request body for methods that support it
                if (requestBody != null && !requestBody.isEmpty() && 
                        ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                    connection.setDoOutput(true);
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }
                
                // Check response code
                int responseCode = connection.getResponseCode();
                LOG.info("Response code: " + responseCode + " for " + method + " " + urlString);
                
                // Read response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(
                                responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                                StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // If the response code indicates an error
                    if (responseCode >= 400) {
                        String errorMsg = "HTTP error code: " + responseCode + ", Response: " + response;
                        
                        // Only retry if it's a server error (5xx) or specific client errors
                        if (responseCode >= 500 || responseCode == 429) {
                            lastException = new IOException(errorMsg);
                            continue; // Retry
                        } else {
                            // Don't retry client errors except for 429 (Too Many Requests)
                            throw new IOException(errorMsg);
                        }
                    }
                    
                    // Success
                    return response.toString();
                }
            } catch (IOException e) {
                lastException = e;
                
                // Check if this is a connection-related error that might be worth retrying
                if (e instanceof java.net.ConnectException || 
                    e instanceof java.net.SocketTimeoutException ||
                    e instanceof java.net.UnknownHostException) {
                    LOG.warn("Retryable network error: " + e.getMessage());
                    continue; // Retry
                }
                
                // For other types of errors, don't retry
                throw e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        // If we get here, all retries failed
        if (lastException != null) {
            LOG.error("All retries failed for request to: " + urlString, lastException);
            throw lastException;
        }
        
        // Should never reach here, but just in case
        throw new IOException("Unknown error executing request to: " + urlString);
    }
    
    /**
     * Shutdown the executor service.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}