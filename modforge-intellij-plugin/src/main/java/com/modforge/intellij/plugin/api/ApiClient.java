package com.modforge.intellij.plugin.api;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Client for communicating with the ModForge API.
 */
public class ApiClient {
    private static final Logger LOG = Logger.getInstance(ApiClient.class);
    
    private final String baseUrl;
    private String authToken;
    private int connectionTimeout = 30000; // 30 seconds
    private int readTimeout = 60000; // 60 seconds
    
    /**
     * Creates a new API client with the given base URL.
     *
     * @param baseUrl The base URL of the API
     */
    public ApiClient(@NotNull String baseUrl) {
        // Ensure URL ends with a slash
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
    
    /**
     * Sets the authentication token to use for requests.
     *
     * @param authToken The authentication token
     */
    public void setAuthToken(@Nullable String authToken) {
        this.authToken = authToken;
    }
    
    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     */
    public void setConnectionTimeout(int timeoutMs) {
        this.connectionTimeout = timeoutMs;
    }
    
    /**
     * Sets the read timeout in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     */
    public void setReadTimeout(int timeoutMs) {
        this.readTimeout = timeoutMs;
    }
    
    /**
     * Performs a GET request to the specified endpoint.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @return The response body
     * @throws IOException If an I/O error occurs
     */
    public String get(@NotNull String endpoint) throws IOException {
        // Normalize endpoint
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        String urlStr = baseUrl + normalizedEndpoint;
        
        LOG.debug("GET request to " + urlStr);
        
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        
        // Set authentication header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        // Set JSON content type for requests and accepts
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        
        try {
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success - read response
                return readResponse(connection);
            } else {
                // Error - read error stream
                String errorMessage = readErrorResponse(connection);
                LOG.warn("API request failed with code " + responseCode + ": " + errorMessage);
                throw new IOException("API request failed with code " + responseCode + ": " + errorMessage);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Performs a POST request to the specified endpoint with the given body.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @param jsonBody The JSON body to send
     * @return The response body
     * @throws IOException If an I/O error occurs
     */
    public String post(@NotNull String endpoint, @Nullable String jsonBody) throws IOException {
        // Normalize endpoint
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        String urlStr = baseUrl + normalizedEndpoint;
        
        LOG.debug("POST request to " + urlStr);
        
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        
        // Set authentication header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        // Set JSON content type for requests and accepts
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        
        // Enable input/output
        connection.setDoOutput(true);
        
        // Write request body if provided
        if (jsonBody != null && !jsonBody.isEmpty()) {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        try {
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success - read response
                return readResponse(connection);
            } else {
                // Error - read error stream
                String errorMessage = readErrorResponse(connection);
                LOG.warn("API request failed with code " + responseCode + ": " + errorMessage);
                throw new IOException("API request failed with code " + responseCode + ": " + errorMessage);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Performs a PUT request to the specified endpoint with the given body.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @param jsonBody The JSON body to send
     * @return The response body
     * @throws IOException If an I/O error occurs
     */
    public String put(@NotNull String endpoint, @Nullable String jsonBody) throws IOException {
        // Normalize endpoint
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        String urlStr = baseUrl + normalizedEndpoint;
        
        LOG.debug("PUT request to " + urlStr);
        
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        
        // Set authentication header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        // Set JSON content type for requests and accepts
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        
        // Enable input/output
        connection.setDoOutput(true);
        
        // Write request body if provided
        if (jsonBody != null && !jsonBody.isEmpty()) {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        try {
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success - read response
                return readResponse(connection);
            } else {
                // Error - read error stream
                String errorMessage = readErrorResponse(connection);
                LOG.warn("API request failed with code " + responseCode + ": " + errorMessage);
                throw new IOException("API request failed with code " + responseCode + ": " + errorMessage);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Performs a DELETE request to the specified endpoint.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @return The response body
     * @throws IOException If an I/O error occurs
     */
    public String delete(@NotNull String endpoint) throws IOException {
        // Normalize endpoint
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        String urlStr = baseUrl + normalizedEndpoint;
        
        LOG.debug("DELETE request to " + urlStr);
        
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        
        // Set authentication header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        // Set JSON content type for requests and accepts
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        
        try {
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success - read response
                return readResponse(connection);
            } else {
                // Error - read error stream
                String errorMessage = readErrorResponse(connection);
                LOG.warn("API request failed with code " + responseCode + ": " + errorMessage);
                throw new IOException("API request failed with code " + responseCode + ": " + errorMessage);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Reads the response from a connection.
     *
     * @param connection The connection to read from
     * @return The response body
     * @throws IOException If an I/O error occurs
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        return response.toString();
    }
    
    /**
     * Reads the error response from a connection.
     *
     * @param connection The connection to read from
     * @return The error message
     */
    private String readErrorResponse(HttpURLConnection connection) {
        StringBuilder errorMessage = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                errorMessage.append(responseLine.trim());
            }
        } catch (IOException e) {
            LOG.warn("Could not read error stream", e);
            return "Unknown error";
        }
        
        return errorMessage.toString();
    }
    
    /**
     * Normalizes an endpoint by removing leading slash.
     *
     * @param endpoint The endpoint to normalize
     * @return The normalized endpoint
     */
    private String normalizeEndpoint(String endpoint) {
        // Remove leading slash if present
        return endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
    }
}