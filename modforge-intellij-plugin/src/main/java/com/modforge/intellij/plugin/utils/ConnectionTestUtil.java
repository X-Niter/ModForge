package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Utility for testing server connections.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    
    /**
     * Test if a server is available.
     * @param serverUrl Server URL to test
     * @return Whether the server is available
     */
    public static boolean testConnection(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            LOG.warn("Cannot test connection: server URL is empty");
            return false;
        }
        
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Try to connect to health endpoint
            String healthUrl = serverUrl + "api/health";
            LOG.info("Testing connection to " + healthUrl);
            
            URL url = new URL(healthUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(CONNECTION_TIMEOUT);
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                LOG.info("Connection successful: " + responseCode);
                return true;
            } else {
                LOG.warn("Connection test failed with response code: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Connection test failed", e);
            return false;
        }
    }
    
    /**
     * Test if a URL is valid (can be connected to).
     * @param urlString URL string to test
     * @return Whether the URL is valid and can be connected to
     */
    public static boolean isValidUrl(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return false;
        }
        
        URLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.connect();
            return true;
        } catch (IOException e) {
            LOG.debug("URL validation failed for: " + urlString, e);
            return false;
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }
}