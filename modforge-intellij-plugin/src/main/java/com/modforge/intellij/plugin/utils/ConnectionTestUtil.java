package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for testing connections to the ModForge server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    
    // Connection timeout in milliseconds (10 seconds)
    private static final int CONNECTION_TIMEOUT = 10000;
    
    // Read timeout in milliseconds (30 seconds)
    private static final int READ_TIMEOUT = 30000;
    
    /**
     * Create an HTTP connection to the specified URL with the specified method.
     * @param urlString URL string
     * @param method HTTP method (GET, POST, etc.)
     * @return HttpURLConnection
     */
    public static HttpURLConnection createConnection(String urlString, String method) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method
            connection.setRequestMethod(method);
            
            // Set timeouts
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            // Set common properties
            connection.setUseCaches(false);
            connection.setDoInput(true);
            
            if ("POST".equals(method) || "PUT".equals(method)) {
                connection.setDoOutput(true);
            }
            
            return connection;
        } catch (IOException e) {
            LOG.error("Error creating connection to " + urlString, e);
            return null;
        }
    }
    
    /**
     * Write JSON data to an HTTP connection.
     * @param connection HttpURLConnection
     * @param jsonData JSON data
     * @throws IOException If an I/O error occurs
     */
    public static void writeJsonToConnection(HttpURLConnection connection, JSONObject jsonData) throws IOException {
        if (connection == null || jsonData == null) {
            return;
        }
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonData.toJSONString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            LOG.error("Error writing JSON to connection", e);
            throw e;
        }
    }
    
    /**
     * Read response from an HTTP connection.
     * @param connection HttpURLConnection
     * @return Response string
     */
    public static String readResponseFromConnection(HttpURLConnection connection) {
        if (connection == null) {
            return null;
        }
        
        try {
            StringBuilder response = new StringBuilder();
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return response.toString();
        } catch (IOException e) {
            LOG.error("Error reading response from connection", e);
            
            // Try to read error stream
            try {
                StringBuilder errorResponse = new StringBuilder();
                
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                
                LOG.error("Error response: " + errorResponse);
            } catch (Exception ex) {
                LOG.error("Error reading error stream", ex);
            }
            
            return null;
        }
    }
    
    /**
     * Test connection to the ModForge server.
     * @param serverUrl Server URL
     * @return Whether connection is successful
     */
    public static boolean testConnection(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return false;
        }
        
        try {
            // Call health check API
            String healthCheckUrl = serverUrl + "/api/health";
            HttpURLConnection connection = createConnection(healthCheckUrl, "GET");
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + healthCheckUrl);
                return false;
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            LOG.error("Error testing connection to " + serverUrl, e);
            return false;
        }
    }
    
    /**
     * Test connection to the ModForge server with authentication.
     * @param serverUrl Server URL
     * @param token Access token
     * @return Whether connection is successful
     */
    public static boolean testAuthenticatedConnection(String serverUrl, String token) {
        if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            // Call authenticated API
            String authCheckUrl = serverUrl + "/api/auth/verify";
            HttpURLConnection connection = TokenAuthConnectionUtil.createTokenAuthConnection(authCheckUrl, "GET", token);
            
            if (connection == null) {
                LOG.warn("Failed to create connection to " + authCheckUrl);
                return false;
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            LOG.error("Error testing authenticated connection to " + serverUrl, e);
            return false;
        }
    }
}