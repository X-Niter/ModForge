package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for testing connection to the ModForge server.
 * @deprecated Use TokenAuthConnectionUtil for authenticated requests. This only does basic connection testing.
 */
@Deprecated
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final int TIMEOUT = 5000;
    
    /**
     * Test connection to server.
     * @param serverUrl Server URL
     * @return Whether connection was successful
     */
    public static boolean testConnection(String serverUrl) {
        try {
            // Normalize URL
            String normalizedUrl = serverUrl;
            if (!normalizedUrl.endsWith("/")) {
                normalizedUrl += "/";
            }
            
            // Remove "api" if it's already in the URL to avoid duplication
            if (normalizedUrl.endsWith("api/")) {
                normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 4);
            }
            
            // Use health endpoint which doesn't require auth
            URL url = new URL(normalizedUrl + "api/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            int responseCode = connection.getResponseCode();
            boolean connected = responseCode >= 200 && responseCode < 300;
            
            LOG.info("Connection test to " + url + " returned " + responseCode);
            
            connection.disconnect();
            return connected;
        } catch (Exception e) {
            LOG.warn("Connection test failed: " + e.getMessage(), e);
            return false;
        }
    }
}