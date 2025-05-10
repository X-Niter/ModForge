package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for creating authenticated connections to the ModForge server.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    
    // Connection timeout in milliseconds (10 seconds)
    private static final int CONNECTION_TIMEOUT = 10000;
    
    // Read timeout in milliseconds (30 seconds)
    private static final int READ_TIMEOUT = 30000;
    
    /**
     * Create an authenticated HTTP connection to the specified URL with the specified method.
     * @param urlString URL string
     * @param method HTTP method (GET, POST, etc.)
     * @param token Authentication token
     * @return HttpURLConnection
     */
    public static HttpURLConnection createTokenAuthConnection(String urlString, String method, String token) {
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
            
            // Set authorization header
            if (token != null && !token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            
            return connection;
        } catch (IOException e) {
            LOG.error("Error creating authenticated connection to " + urlString, e);
            return null;
        }
    }
}