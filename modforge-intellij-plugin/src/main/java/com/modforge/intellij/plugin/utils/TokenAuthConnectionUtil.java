package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility for making HTTP requests with token-based authentication.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    private static final int TIMEOUT = 5000; // 5 seconds
    
    /**
     * Make an authenticated GET request.
     * 
     * @param endpoint The endpoint to request (e.g., "/api/user")
     * @return Response as a string, or null if the request failed
     */
    public static String makeAuthenticatedGetRequest(String endpoint) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            LOG.warn("Not authenticated or missing token");
            return null;
        }
        
        String serverUrl = settings.getServerUrl();
        HttpURLConnection connection = null;
        
        try {
            // Normalize the URL
            serverUrl = normalizeServerUrl(serverUrl);
            
            // Make sure the endpoint starts with a slash
            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }
            
            URL url = new URL(serverUrl + endpoint.substring(1));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Set token-based authentication header
            connection.setRequestProperty("Authorization", "Bearer " + settings.getAccessToken());
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                }
                
                return response.toString();
            } else {
                LOG.warn("Request failed with response code: " + responseCode);
                
                // Try to read error stream
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine);
                    }
                } catch (Exception e) {
                    // Ignore error reading error stream
                }
                
                LOG.warn("Error response: " + errorResponse);
                return null;
            }
        } catch (IOException e) {
            LOG.warn("Request failed: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Make an authenticated POST request.
     * 
     * @param endpoint The endpoint to request (e.g., "/api/logout")
     * @param jsonBody JSON body as string, or null for no body
     * @return Response as a string, or null if the request failed
     */
    public static String makeAuthenticatedPostRequest(String endpoint, String jsonBody) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            LOG.warn("Not authenticated or missing token");
            return null;
        }
        
        String serverUrl = settings.getServerUrl();
        HttpURLConnection connection = null;
        
        try {
            // Normalize the URL
            serverUrl = normalizeServerUrl(serverUrl);
            
            // Make sure the endpoint starts with a slash
            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }
            
            URL url = new URL(serverUrl + endpoint.substring(1));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Set token-based authentication header
            connection.setRequestProperty("Authorization", "Bearer " + settings.getAccessToken());
            
            // If we have a body, set content type and write it
            if (jsonBody != null && !jsonBody.isEmpty()) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                }
                
                return response.toString();
            } else {
                LOG.warn("Request failed with response code: " + responseCode);
                
                // Try to read error stream
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine);
                    }
                } catch (Exception e) {
                    // Ignore error reading error stream
                }
                
                LOG.warn("Error response: " + errorResponse);
                return null;
            }
        } catch (IOException e) {
            LOG.warn("Request failed: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Test token-based authentication.
     * 
     * @return Whether token authentication works
     */
    public static boolean testTokenAuthentication() {
        String response = makeAuthenticatedGetRequest("/api/auth/verify");
        return response != null && response.contains("success");
    }
    
    /**
     * Normalize server URL.
     */
    private static String normalizeServerUrl(String serverUrl) {
        // Add trailing slash if needed
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        
        // Remove "api" if it's already in the URL to avoid duplication
        if (serverUrl.endsWith("/api/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 4);
        }
        
        return serverUrl;
    }
}