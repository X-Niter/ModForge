package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for testing connections to ModForge servers.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 5000; // 5 seconds

    /**
     * Test a connection to a ModForge server.
     *
     * @param serverUrl The server URL to test
     * @return A future with the result of the test (true if successful)
     */
    @NotNull
    public static CompletableFuture<Boolean> testConnection(@NotNull String serverUrl) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Run the test asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Build URL for health check endpoint
                String healthUrl = serverUrl;
                if (!healthUrl.endsWith("/")) {
                    healthUrl += "/";
                }
                healthUrl += "api/health";
                
                // Open connection
                URL url = new URL(healthUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                
                // Get response
                int responseCode = connection.getResponseCode();
                
                // Check if response is successful
                boolean success = responseCode >= 200 && responseCode < 300;
                
                if (success) {
                    LOG.info("Connection test successful: " + serverUrl);
                } else {
                    LOG.error("Connection test failed: " + serverUrl + ", response code: " + responseCode);
                }
                
                future.complete(success);
            } catch (IOException e) {
                LOG.error("Connection test exception: " + e.getMessage(), e);
                future.complete(false);
            } catch (Exception e) {
                LOG.error("Unexpected error in connection test", e);
                future.complete(false);
            }
        });
        
        // Add a timeout to the future
        return future.orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    LOG.error("Connection test timed out or failed", ex);
                    return false;
                });
    }
    
    /**
     * Check if a ModForge server has a specific feature.
     *
     * @param serverUrl The server URL to check
     * @param feature   The feature to check for (e.g., "pattern-recognition")
     * @return A future with the result of the check (true if feature is supported)
     */
    @NotNull
    public static CompletableFuture<Boolean> checkFeatureSupport(
            @NotNull String serverUrl, 
            @NotNull String feature) {
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Run the check asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Build URL for feature check endpoint
                String featureUrl = serverUrl;
                if (!featureUrl.endsWith("/")) {
                    featureUrl += "/";
                }
                featureUrl += "api/features/" + feature;
                
                // Open connection
                URL url = new URL(featureUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                
                // Get response
                int responseCode = connection.getResponseCode();
                
                // Check if response is successful
                boolean supported = responseCode == HttpURLConnection.HTTP_OK;
                
                if (supported) {
                    LOG.info("Feature supported: " + feature);
                } else {
                    LOG.info("Feature not supported: " + feature);
                }
                
                future.complete(supported);
            } catch (IOException e) {
                LOG.error("Feature check exception: " + e.getMessage(), e);
                future.complete(false);
            } catch (Exception e) {
                LOG.error("Unexpected error in feature check", e);
                future.complete(false);
            }
        });
        
        // Add a timeout to the future
        return future.orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    LOG.error("Feature check timed out or failed", ex);
                    return false;
                });
    }
}