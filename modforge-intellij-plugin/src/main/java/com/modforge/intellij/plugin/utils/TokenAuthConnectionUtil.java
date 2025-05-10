package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for token-based authentication.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    private static final Gson GSON = new Gson();

    /**
     * Response from the authentication server.
     */
    public static class AuthResponse {
        public String token;
        public String username;
        public String message;
    }

    /**
     * Get an authentication token from the server.
     *
     * @param serverUrl The server URL
     * @param username  The username
     * @param password  The password
     * @return A future with the response, or null if failed
     */
    @NotNull
    public static CompletableFuture<AuthResponse> getAuthToken(
            @NotNull String serverUrl, 
            @NotNull String username, 
            @NotNull String password) {
        
        CompletableFuture<AuthResponse> future = new CompletableFuture<>();
        
        try {
            String loginUrl = serverUrl;
            if (!loginUrl.endsWith("/")) {
                loginUrl += "/";
            }
            loginUrl += "api/token";
            
            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("username", username);
            requestBody.addProperty("password", password);
            
            // Set up connection
            URL url = new URL(loginUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Process response
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    AuthResponse authResponse = GSON.fromJson(response.toString(), AuthResponse.class);
                    future.complete(authResponse);
                }
            } else {
                LOG.error("Failed to get auth token: " + statusCode);
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    LOG.error("Error response: " + response.toString());
                } catch (Exception e) {
                    LOG.error("Error reading error stream", e);
                }
                future.complete(null);
            }
        } catch (Exception e) {
            LOG.error("Exception getting auth token", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    /**
     * Verify an authentication token with the server.
     *
     * @param serverUrl The server URL
     * @param token     The token to verify
     * @return A future with the result of the verification (true if valid)
     */
    @NotNull
    public static CompletableFuture<Boolean> verifyToken(@NotNull String serverUrl, @NotNull String token) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            String verifyUrl = serverUrl;
            if (!verifyUrl.endsWith("/")) {
                verifyUrl += "/";
            }
            verifyUrl += "api/auth/verify";
            
            // Set up connection
            URL url = new URL(verifyUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            // Process response
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                future.complete(true);
            } else {
                LOG.error("Failed to verify token: " + statusCode);
                future.complete(false);
            }
        } catch (Exception e) {
            LOG.error("Exception verifying token", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    /**
     * Make an authenticated request to the server.
     *
     * @param serverUrl The server URL
     * @param endpoint  The endpoint to call
     * @param method    The HTTP method to use
     * @param token     The authentication token
     * @param body      The request body, or null for GET/DELETE
     * @return A future with the response, or null if failed
     */
    @NotNull
    public static CompletableFuture<String> makeAuthenticatedRequest(
            @NotNull String serverUrl,
            @NotNull String endpoint,
            @NotNull String method,
            @NotNull String token,
            @Nullable String body) {
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            String fullUrl = serverUrl;
            if (!fullUrl.endsWith("/")) {
                fullUrl += "/";
            }
            fullUrl += endpoint;
            
            // Set up connection
            URL url = new URL(fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            if (body != null && !body.isEmpty()) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Send request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            // Process response
            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    
                    future.complete(response.toString());
                }
            } else {
                LOG.error("Failed to make authenticated request: " + statusCode);
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    LOG.error("Error response: " + response.toString());
                } catch (Exception e) {
                    LOG.error("Error reading error stream", e);
                }
                future.complete(null);
            }
        } catch (Exception e) {
            LOG.error("Exception making authenticated request", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
}