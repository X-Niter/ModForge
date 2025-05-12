package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility for authentication testing.
 */
public class AuthTestUtil {
    private static final Logger LOG = Logger.getInstance(AuthTestUtil.class);
    
    /**
     * API Endpoint for testing.
     */
    public enum Endpoint {
        USER("/api/user", "GET"),
        PROFILE("/api/profile", "GET"),
        PROJECTS("/api/projects", "GET"),
        LOGOUT("/api/logout", "POST");
        
        private final String path;
        private final String method;
        
        Endpoint(String path, String method) {
            this.path = path;
            this.method = method;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getMethod() {
            return method;
        }
    }
    
    /**
     * Test result for an endpoint.
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
    }
    
    /**
     * Tests a specific API endpoint.
     * 
     * @param endpoint The endpoint to test
     * @return Test result
     */
    public static TestResult testEndpoint(Endpoint endpoint) {
        try {
            String serverUrl = ServerUrlUtil.getServerUrl();
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            String apiUrl = serverUrl + endpoint.getPath().replaceFirst("^/", "");
            LOG.info("Testing endpoint: " + apiUrl);
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(endpoint.getMethod());
            
            // Add authentication token if available
            String accessToken = getStoredAccessToken();
            if (accessToken != null && !accessToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            
            int responseCode = conn.getResponseCode();
            
            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return new TestResult(responseCode >= 200 && responseCode < 300, 
                responseCode, response.toString());
                
        } catch (Exception e) {
            LOG.error("Error testing endpoint " + endpoint.getPath(), e);
            return new TestResult(false, 0, e.getMessage());
        }
    }
    
    /**
     * Gets the stored access token from settings.
     * 
     * @return The access token or null
     */
    private static String getStoredAccessToken() {
        try {
            return ModForgeSettings.getInstance().getAccessToken();
        } catch (Exception e) {
            LOG.error("Failed to get access token", e);
            return null;
        }
    }
    
    /**
     * Tests the complete auth flow.
     * 
     * @param username The username
     * @param password The password
     * @return Results of the test
     */
    public static String testCompleteAuthFlow(String username, String password) {
        StringBuilder results = new StringBuilder("Authentication Flow Test Results:\n\n");
        
        try {
            String serverUrl = ServerUrlUtil.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                return "Error: Server URL is not configured.";
            }
            
            // Test basic connection
            boolean connected = testConnection(serverUrl);
            results.append("Connection to ").append(serverUrl).append(": ")
                   .append(connected ? "Successful" : "Failed").append("\n\n");
            
            if (!connected) {
                return results.toString();
            }
            
            // Try to authenticate
            String token = getAccessToken(serverUrl, username, password);
            results.append("Authentication: ").append(token != null ? "Successful" : "Failed").append("\n");
            
            if (token == null) {
                return results.toString();
            }
            
            // Store the token for subsequent tests
            ModForgeSettings.getInstance().setAccessToken(token);
            
            // Test API endpoints
            results.append("\nEndpoint Tests:\n");
            for (Endpoint endpoint : Endpoint.values()) {
                TestResult result = testEndpoint(endpoint);
                results.append(endpoint.getPath())
                       .append(" - ")
                       .append(result.isSuccess() ? "Success" : "Failed")
                       .append(" (").append(result.getResponseCode()).append(")")
                       .append("\n");
            }
            
            return results.toString();
        } catch (Exception e) {
            LOG.error("Error testing auth flow", e);
            results.append("\nError: ").append(e.getMessage());
            return results.toString();
        }
    }
    
    /**
     * Test basic connection to the ModForge server.
     * @param serverUrl URL of the ModForge server
     * @return Whether the connection was successful
     */
    public static boolean testConnection(String serverUrl) {
        return ConnectionTestUtil.testConnection(serverUrl);
    }
    
    /**
     * Get access token using username and password.
     * @param serverUrl URL of the ModForge server
     * @param username Username
     * @param password Password
     * @return Access token, or null if authentication failed
     */
    public static String getAccessToken(String serverUrl, String username, String password) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String loginUrl = serverUrl + "api/login";
            
            LOG.info("Authenticating with username to " + loginUrl);
            
            URL url = new URL(loginUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Create login JSON
            JSONObject loginJson = new JSONObject();
            loginJson.put("username", username);
            loginJson.put("password", password);
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = loginJson.toJSONString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200 && responseCode != 201) {
                LOG.error("Authentication failed with response code " + responseCode);
                return null;
            }
            
            // Get user data from response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            // Parse user data
            JSONParser parser = new JSONParser();
            JSONObject userData = (JSONObject) parser.parse(response.toString());
            
            // Get token
            if (userData.containsKey("token")) {
                return (String) userData.get("token");
            } else {
                LOG.error("Response doesn't contain token");
                return null;
            }
        } catch (IOException | ParseException e) {
            LOG.error("Authentication failed", e);
            return null;
        }
    }
    
    /**
     * Verify authentication with token.
     * @param serverUrl URL of the ModForge server
     * @param token Access token
     * @return Whether the token is valid
     */
    public static boolean verifyAuthentication(String serverUrl, String token) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String verifyUrl = serverUrl + "api/auth/verify";
            
            LOG.info("Verifying token at " + verifyUrl);
            
            URL url = new URL(verifyUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            int responseCode = connection.getResponseCode();
            
            return responseCode == 200;
        } catch (IOException e) {
            LOG.error("Token verification failed", e);
            return false;
        }
    }
    
    /**
     * Get authentication status with token.
     * @param serverUrl URL of the ModForge server
     * @param token Access token
     * @return Authentication status as JSON string, or null if failed
     */
    public static String getAuthenticationStatus(String serverUrl, String token) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String meUrl = serverUrl + "api/auth/me";
            
            URL url = new URL(meUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                return null;
            }
            
            // Read response
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream())
                .useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            LOG.error("Authentication status check failed", e);
            return null;
        }
    }
}