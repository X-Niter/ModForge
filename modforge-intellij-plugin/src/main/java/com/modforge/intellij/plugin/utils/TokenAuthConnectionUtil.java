package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Utility for token authentication connections.
 * This class provides utility methods for making authenticated HTTP requests with tokens.
 */
public final class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private TokenAuthConnectionUtil() {
        // Utility class
    }
    
    /**
     * Tests if token authentication works using server URL and token from settings.
     *
     * @return True if authentication works, false otherwise
     */
    public static boolean testTokenAuthentication() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings == null) {
            return false;
        }
        
        return testTokenAuthentication(settings.getServerUrl(), settings.getAccessToken());
    }
    
    /**
     * Tests if token authentication works with the given server URL and token.
     *
     * @param serverUrl The server URL
     * @param token The authentication token
     * @return True if authentication works, false otherwise
     */
    public static boolean testTokenAuthentication(@NotNull String serverUrl, @NotNull String token) {
        try {
            String url = serverUrl;
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "api/auth/verify";
            
            RequestBuilder request = HttpRequests.post(url, "application/json")
                    .accept("application/json")
                    .productNameAsUserAgent()
                    .tuner(connection -> connection.setRequestProperty("Authorization", "Bearer " + token));
            
            String response = request.connect(connection -> {
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                
                // Read response
                Map<String, Object> responseMap = JsonUtil.readMapFromStream(connection.getInputStream());
                if (responseMap == null) {
                    return null;
                }
                
                return (String) responseMap.get("status");
            });
            
            return "success".equals(response);
        } catch (IOException e) {
            LOG.warn("Failed to test token authentication", e);
            return false;
        }
    }
    
    /**
     * Executes a GET request with token authentication.
     *
     * @param serverUrl The server URL
     * @param endpoint The endpoint
     * @param token The authentication token
     * @return The response, or null if an error occurs
     */
    @Nullable
    public static String executeGet(@NotNull String serverUrl, @NotNull String endpoint, @NotNull String token) {
        try {
            String url = serverUrl;
            if (!url.endsWith("/") && !endpoint.startsWith("/")) {
                url += "/";
            }
            url += endpoint;
            
            RequestBuilder request = HttpRequests.get(url)
                    .accept("application/json")
                    .productNameAsUserAgent()
                    .tuner(connection -> connection.setRequestProperty("Authorization", "Bearer " + token));
            
            return request.connect(connection -> {
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    LOG.warn("Failed to execute GET request, response code: " + responseCode);
                    return null;
                }
                
                // Read response
                return new String(connection.getInputStream().readAllBytes());
            });
        } catch (IOException e) {
            LOG.warn("Failed to execute GET request", e);
            return null;
        }
    }
    
    /**
     * Executes a POST request with token authentication.
     *
     * @param serverUrl The server URL
     * @param endpoint The endpoint
     * @param token The authentication token
     * @param body The request body
     * @return The response, or null if an error occurs
     */
    @Nullable
    public static String executePost(@NotNull String serverUrl, @NotNull String endpoint, 
                                   @NotNull String token, @Nullable Object body) {
        try {
            String url = serverUrl;
            if (!url.endsWith("/") && !endpoint.startsWith("/")) {
                url += "/";
            }
            url += endpoint;
            
            RequestBuilder request = HttpRequests.post(url, "application/json")
                    .accept("application/json")
                    .productNameAsUserAgent()
                    .tuner(connection -> connection.setRequestProperty("Authorization", "Bearer " + token));
            
            return request.connect(connection -> {
                // Write request body
                if (body != null) {
                    connection.write(JsonUtil.writeToString(body));
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                    LOG.warn("Failed to execute POST request, response code: " + responseCode);
                    return null;
                }
                
                // Read response
                return new String(connection.getInputStream().readAllBytes());
            });
        } catch (IOException e) {
            LOG.warn("Failed to execute POST request", e);
            return null;
        }
    }
}