package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
     * Test token login.
     * @param username Username
     * @param password Password
     * @return Test result
     */
    public static TestResult testTokenLogin(String username, String password) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl.isEmpty()) {
            return new TestResult(false, 0, "Server URL is empty");
        }
        
        HttpURLConnection connection = null;
        
        try {
            // Normalize the URL
            serverUrl = normalizeServerUrl(serverUrl);
            
            URL url = new URL(serverUrl + "api/token");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Create JSON payload
            String jsonPayload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", 
                    username, password);
            
            // Write payload
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }
            
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
            LOG.warn("Failed to test token login: " + e.getMessage());
            return new TestResult(false, 0, "Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Test the complete authentication flow:
     * 1. Login with username/password
     * 2. Extract token
     * 3. Test token with various endpoints
     * 
     * @param username Username
     * @param password Password
     * @return Test results with details about each step
     */
    public static String testCompleteAuthFlow(String username, String password) {
        StringBuilder results = new StringBuilder("Complete Authentication Flow Test\n\n");
        
        // Step 1: Login with username/password to get token
        results.append("Step 1: Login to get token\n");
        TestResult loginResult = testTokenLogin(username, password);
        results.append("Status: ").append(loginResult.isSuccess() ? "Success" : "Failed")
               .append(" (").append(loginResult.getResponseCode()).append(")\n");
        
        if (!loginResult.isSuccess()) {
            results.append("Error: Failed to login and get token\n");
            results.append("Response: ").append(loginResult.getResponse()).append("\n\n");
            return results.toString();
        }
        
        // Extract token from response
        String token = extractToken(loginResult.getResponse());
        if (token == null) {
            results.append("Error: Failed to extract token from response\n");
            results.append("Response: ").append(loginResult.getResponse()).append("\n\n");
            return results.toString();
        }
        
        results.append("Token obtained successfully\n\n");
        
        // Step 2: Test token with authentication verification endpoint
        results.append("Step 2: Test token with /api/auth/verify\n");
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Temporarily set token in settings
        String originalToken = settings.getAccessToken();
        boolean originalAuthState = settings.isAuthenticated();
        
        settings.setAccessToken(token);
        settings.setAuthenticated(true);
        
        TestResult verifyResult = testEndpoint(Endpoint.AUTH_VERIFY);
        results.append("Status: ").append(verifyResult.isSuccess() ? "Success" : "Failed")
               .append(" (").append(verifyResult.getResponseCode()).append(")\n");
        results.append("Response: ").append(verifyResult.getResponse()).append("\n\n");
        
        // Step 3: Test token with user endpoint
        results.append("Step 3: Test token with /api/user\n");
        TestResult userResult = testEndpoint(Endpoint.USER);
        results.append("Status: ").append(userResult.isSuccess() ? "Success" : "Failed")
               .append(" (").append(userResult.getResponseCode()).append(")\n");
        results.append("Response: ").append(userResult.getResponse()).append("\n\n");
        
        // Restore original token
        settings.setAccessToken(originalToken);
        settings.setAuthenticated(originalAuthState);
        
        // Summary
        results.append("Summary:\n");
        results.append("- Login: ").append(loginResult.isSuccess() ? "Success" : "Failed").append("\n");
        results.append("- Token Verification: ").append(verifyResult.isSuccess() ? "Success" : "Failed").append("\n");
        results.append("- User Info: ").append(userResult.isSuccess() ? "Success" : "Failed").append("\n");
        
        if (loginResult.isSuccess() && verifyResult.isSuccess() && userResult.isSuccess()) {
            results.append("\nAuthentication flow test passed successfully!");
        } else {
            results.append("\nAuthentication flow test failed. Check individual steps for details.");
        }
        
        return results.toString();
    }
    
    /**
     * Extract token from JSON response.
     */
    private static String extractToken(String jsonResponse) {
        int tokenStart = jsonResponse.indexOf("\"token\":");
        if (tokenStart == -1) {
            return null;
        }
        
        tokenStart = jsonResponse.indexOf("\"", tokenStart + 8) + 1;
        int tokenEnd = jsonResponse.indexOf("\"", tokenStart);
        
        if (tokenStart == -1 || tokenEnd == -1) {
            return null;
        }
        
        return jsonResponse.substring(tokenStart, tokenEnd);
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