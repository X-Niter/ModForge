package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for testing connection to the ModForge server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    
    /**
     * Test connection to the ModForge server.
     *
     * @param serverUrl The server URL to test
     * @return Whether the connection was successful
     */
    public static boolean testConnection(@NotNull String serverUrl) {
        try {
            // Ensure serverUrl ends with /
            if (!serverUrl.endsWith("/")) {
                serverUrl = serverUrl + "/";
            }
            
            // Create health check URL
            URL url = new URL(serverUrl + "api/health");
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            
            boolean success = responseCode == 200;
            
            if (!success) {
                LOG.warn("Connection test failed with response code: " + responseCode);
            }
            
            return success;
        } catch (IOException e) {
            LOG.error("Error testing connection", e);
            return false;
        }
    }
    
    /**
     * Get server version from the ModForge server.
     *
     * @param serverUrl The server URL
     * @return The server version, or null if the request fails
     */
    public static String getServerVersion(@NotNull String serverUrl) {
        try {
            // Ensure serverUrl ends with /
            if (!serverUrl.endsWith("/")) {
                serverUrl = serverUrl + "/";
            }
            
            // Create health check URL
            URL url = new URL(serverUrl + "api/health");
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                String response = TokenAuthConnectionUtil.readResponse(connection);
                
                if (response != null && !response.isEmpty()) {
                    // Try to find version in response (simple approach - might need improvement)
                    if (response.contains("\"version\":")) {
                        return response.split("\"version\":")[1].split("\"")[1];
                    }
                }
            }
            
            LOG.warn("Failed to get server version with response code: " + responseCode);
            return null;
        } catch (IOException e) {
            LOG.error("Error getting server version", e);
            return null;
        }
    }
}