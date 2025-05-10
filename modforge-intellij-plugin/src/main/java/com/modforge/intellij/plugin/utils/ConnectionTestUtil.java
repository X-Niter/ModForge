package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for testing connections to the ModForge server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    
    /**
     * Test connection to the ModForge server.
     * @param serverUrl URL of the ModForge server
     * @return Whether the connection was successful
     */
    public static boolean testConnection(String serverUrl) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String healthUrl = serverUrl + "api/health";
            
            LOG.info("Testing connection to " + healthUrl);
            
            URL url = new URL(healthUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            LOG.info("Response code: " + responseCode);
            
            return responseCode == 200;
        } catch (IOException e) {
            LOG.error("Connection test failed", e);
            return false;
        }
    }
    
    /**
     * Check if server is available.
     * @param serverUrl URL of the ModForge server
     * @return Whether the server is available
     */
    public static boolean isServerAvailable(String serverUrl) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(2000);
            
            int responseCode = connection.getResponseCode();
            
            return responseCode < 400;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get health status from server.
     * @param serverUrl URL of the ModForge server
     * @return Health status as JSON string, or null if failed
     */
    public static String getHealthStatus(String serverUrl) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String healthUrl = serverUrl + "api/health";
            
            URL url = new URL(healthUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                return null;
            }
            
            // Read response
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream())
                .useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            LOG.error("Health check failed", e);
            return null;
        }
    }
}