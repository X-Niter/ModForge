package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for testing connection to ModForge server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final String HEALTH_CHECK_ENDPOINT = "/api/health";
    private static final int TIMEOUT = 5000; // 5 seconds
    
    /**
     * Tests connection to the ModForge server.
     * @param serverUrl Base URL of the server
     * @return Whether the connection was successful
     */
    public static boolean testConnection(String serverUrl) {
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
            
            URL url = new URL(serverUrl + HEALTH_CHECK_ENDPOINT.substring(1));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            // 200 OK means server is up and running
            return responseCode == 200;
        } catch (IOException e) {
            LOG.warn("Failed to connect to ModForge server: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Tests the authentication endpoint.
     * @param serverUrl Base URL of the server
     * @param username Username
     * @param password Password
     * @return Whether authentication was successful
     */
    public static boolean testAuthentication(String serverUrl, String username, String password) {
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
            
            URL url = new URL(serverUrl + "api/auth/verify");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Create JSON payload
            String jsonPayload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
            
            // Write payload
            connection.getOutputStream().write(jsonPayload.getBytes("UTF-8"));
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            // 200 OK means authentication was successful
            return responseCode == 200;
        } catch (IOException e) {
            LOG.warn("Failed to authenticate with ModForge server: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}