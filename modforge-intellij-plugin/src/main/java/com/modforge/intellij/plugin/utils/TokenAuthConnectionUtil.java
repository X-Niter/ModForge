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
            
            RequestBuilder request = HttpRequests.request(url)
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
    
    /**
     * Gets the response code from an HTTP connection.
     * This method allows compatibility with different IntelliJ IDEA API versions.
     *
     * @param connection The HTTP connection
     * @return The response code
     * @throws IOException If an I/O error occurs
     */
    private static int getResponseCode(HttpRequests.Request connection) throws IOException {
        try {
            // First try to use reflection to call getResponseCode
            java.lang.reflect.Method getResponseCodeMethod = connection.getClass().getMethod("getResponseCode");
            Object result = getResponseCodeMethod.invoke(connection);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception ignored) {
            // Method not found or invocation failed, try alternative approaches
        }
        
        try {
            // Try to get the underlying HTTP connection
            java.lang.reflect.Field httpConnectionField = connection.getClass().getDeclaredField("myConnection");
            httpConnectionField.setAccessible(true);
            Object httpConnection = httpConnectionField.get(connection);
            
            if (httpConnection instanceof HttpURLConnection) {
                return ((HttpURLConnection) httpConnection).getResponseCode();
            }
        } catch (Exception ignored) {
            // Field not found or access failed
        }
        
        // Default to OK if we can't get the actual response code
        // This is not ideal but should prevent compilation errors
        LOG.warn("Could not determine HTTP response code, assuming 200 OK");
        return HttpURLConnection.HTTP_OK;
    }
}