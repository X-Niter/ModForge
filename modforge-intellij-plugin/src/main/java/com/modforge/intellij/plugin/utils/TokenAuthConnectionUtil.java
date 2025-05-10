package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for token-based authenticated HTTP connections.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);

    /**
     * Create an authenticated HTTP connection to the ModForge server.
     *
     * @param endpoint The API endpoint (without server URL)
     * @param method   The HTTP method
     * @return The connection, or null if authentication fails
     */
    @Nullable
    public static HttpURLConnection createAuthenticatedConnection(@NotNull String endpoint, @NotNull String method) {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        if (!authManager.isAuthenticated()) {
            LOG.warn("Not authenticated");
            return null;
        }
        
        String authHeader = authManager.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("Auth header is null");
            return null;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl.isEmpty()) {
            LOG.warn("Server URL is empty");
            return null;
        }
        
        try {
            // Ensure endpoint starts with /
            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }
            
            // Create URL
            URL url = new URL(serverUrl + endpoint);
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", authHeader);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            // Set output flag for non-GET methods
            if (!method.equals("GET")) {
                connection.setDoOutput(true);
            }
            
            return connection;
        } catch (IOException e) {
            LOG.error("Error creating authenticated connection", e);
            return null;
        }
    }
    
    /**
     * Execute a GET request to the ModForge server.
     *
     * @param endpoint The API endpoint (without server URL)
     * @return The response body, or null if the request fails
     */
    @Nullable
    public static String executeGet(@NotNull String endpoint) {
        HttpURLConnection connection = createAuthenticatedConnection(endpoint, "GET");
        
        if (connection == null) {
            return null;
        }
        
        try {
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                return readResponse(connection);
            } else {
                LOG.warn("GET request failed with response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error executing GET request", e);
            return null;
        }
    }
    
    /**
     * Execute a POST request to the ModForge server.
     *
     * @param endpoint The API endpoint (without server URL)
     * @param jsonBody The JSON request body
     * @return The response body, or null if the request fails
     */
    @Nullable
    public static String executePost(@NotNull String endpoint, @NotNull String jsonBody) {
        HttpURLConnection connection = createAuthenticatedConnection(endpoint, "POST");
        
        if (connection == null) {
            return null;
        }
        
        try {
            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200 || responseCode == 201) {
                return readResponse(connection);
            } else {
                LOG.warn("POST request failed with response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error executing POST request", e);
            return null;
        }
    }
    
    /**
     * Execute a POST request to the ModForge server with form parameters.
     *
     * @param endpoint   The API endpoint (without server URL)
     * @param formParams The form parameters
     * @return The response body, or null if the request fails
     */
    @Nullable
    public static String executeFormPost(@NotNull String endpoint, @NotNull Map<String, String> formParams) {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        if (!authManager.isAuthenticated()) {
            LOG.warn("Not authenticated");
            return null;
        }
        
        String authHeader = authManager.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("Auth header is null");
            return null;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl.isEmpty()) {
            LOG.warn("Server URL is empty");
            return null;
        }
        
        try {
            // Ensure endpoint starts with /
            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }
            
            // Create URL
            URL url = new URL(serverUrl + endpoint);
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", authHeader);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            
            // Build form data
            StringBuilder formData = new StringBuilder();
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (formData.length() > 0) {
                    formData.append("&");
                }
                formData.append(entry.getKey())
                        .append("=")
                        .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            
            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = formData.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200 || responseCode == 201) {
                return readResponse(connection);
            } else {
                LOG.warn("Form POST request failed with response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error executing form POST request", e);
            return null;
        }
    }
    
    /**
     * Read the response body from an HTTP connection.
     *
     * @param connection The connection
     * @return The response body, or null if reading fails
     */
    @Nullable
    public static String readResponse(@NotNull HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            LOG.error("Error reading response", e);
            return null;
        }
    }
}