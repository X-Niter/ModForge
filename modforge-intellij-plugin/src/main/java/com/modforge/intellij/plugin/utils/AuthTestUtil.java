package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility for testing authentication endpoints with token-based authentication.
 */
public class AuthTestUtil {
    private static final Logger LOG = Logger.getInstance(AuthTestUtil.class);
    private static final int TIMEOUT = 5000; // 5 seconds
    
    public static enum Endpoint {
        USER("/api/user"),
        AUTH_ME("/api/auth/me"),
        AUTH_VERIFY("/api/auth/verify");
        
        private final String path;
        
        Endpoint(String path) {
            this.path = path;
        }
        
        public String getPath() {
            return path;
        }
    }
    
    /**
     * Test authentication by calling various endpoints with token-based authentication.
     * @param endpoint The endpoint to test.
     * @return The result of the test including response code and body.
     */
    public static TestResult testEndpoint(Endpoint endpoint) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated() || settings.getAccessToken().isEmpty()) {
            return new TestResult(false, 401, "Not authenticated");
        }
        
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl.isEmpty()) {
            return new TestResult(false, 0, "Server URL is empty");
        }
        
        HttpURLConnection connection = null;
        
        try {
            // Normalize the URL (add trailing slash if needed)
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Remove "api" if it's already in the URL to avoid duplication
            if (serverUrl.endsWith("/api/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 4);
            }
            
            URL url = new URL(serverUrl + endpoint.getPath().substring(1));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Set token-based authentication header
            connection.setRequestProperty("Authorization", "Bearer " + settings.getAccessToken());
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 
                                    ? connection.getErrorStream() 
                                    : connection.getInputStream(), 
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            return new TestResult(
                    responseCode >= 200 && responseCode < 300,
                    responseCode,
                    response.toString()
            );
            
        } catch (IOException e) {
            LOG.warn("Failed to test authentication endpoint: " + e.getMessage());
            return new TestResult(false, 0, "Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Result of testing an authentication endpoint.
     */
    public static class TestResult {
        private final boolean success;
        private final int responseCode;
        private final String response;
        
        public TestResult(boolean success, int responseCode, String response) {
            this.success = success;
            this.responseCode = responseCode;
            this.response = response;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public int getResponseCode() {
            return responseCode;
        }
        
        public String getResponse() {
            return response;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "TestResult{success=%s, responseCode=%d, response='%s'}",
                    success,
                    responseCode,
                    response
            );
        }
    }
}