package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for testing connections to the ModForge server.
 * Compatible with IntelliJ IDEA 2025.1 and Java 21.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final int MAX_RETRIES = 2;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    
    /**
     * Test the connection to the server asynchronously.
     *
     * @param serverUrl The server URL to test
     * @return A CompletableFuture that will be resolved to true if the connection is successful, false otherwise
     */
    public static CompletableFuture<Boolean> testConnection(String serverUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String healthEndpoint = serverUrl.endsWith("/") ? serverUrl + "health" : serverUrl + "/health";
                return testHealthEndpoint(healthEndpoint);
            } catch (Exception e) {
                LOG.error("Error testing connection", e);
                return false;
            }
        }, EXECUTOR);
    }
    
    /**
     * Test the connection to the server health endpoint synchronously with retries.
     *
     * @param healthEndpoint The health endpoint URL to test
     * @return true if the connection is successful, false otherwise
     */
    private static boolean testHealthEndpoint(String healthEndpoint) {
        for (int retry = 0; retry <= MAX_RETRIES; retry++) {
            if (retry > 0) {
                LOG.info("Retrying health check (" + retry + "/" + MAX_RETRIES + ")");
                try {
                    TimeUnit.MILLISECONDS.sleep(500 * retry); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Interrupted while waiting to retry", e);
                    return false;
                }
            }
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(healthEndpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                
                int responseCode = connection.getResponseCode();
                LOG.info("Health check response code: " + responseCode);
                
                if (responseCode == 200) {
                    return true;
                }
                
                // Only retry for server errors or timeouts
                if (responseCode < 500) {
                    return false;
                }
            } catch (IOException e) {
                if (retry >= MAX_RETRIES) {
                    LOG.error("Error testing connection to health endpoint", e);
                } else {
                    LOG.warn("Error testing connection to health endpoint, will retry", e);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Test user authentication asynchronously.
     *
     * @param serverUrl The server URL
     * @param token     The user token
     * @return A CompletableFuture that will be resolved to true if the authentication is successful, false otherwise
     */
    public static CompletableFuture<Boolean> testAuthentication(String serverUrl, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String authEndpoint = serverUrl.endsWith("/") ? serverUrl + "auth/verify" : serverUrl + "/auth/verify";
                return testAuthEndpoint(authEndpoint, token);
            } catch (Exception e) {
                LOG.error("Error testing authentication", e);
                return false;
            }
        }, EXECUTOR);
    }
    
    /**
     * Test authentication synchronously with retries.
     *
     * @param authEndpoint The authentication endpoint URL to test
     * @param token        The user token
     * @return true if the authentication is successful, false otherwise
     */
    private static boolean testAuthEndpoint(String authEndpoint, String token) {
        for (int retry = 0; retry <= MAX_RETRIES; retry++) {
            if (retry > 0) {
                LOG.info("Retrying auth check (" + retry + "/" + MAX_RETRIES + ")");
                try {
                    TimeUnit.MILLISECONDS.sleep(500 * retry); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Interrupted while waiting to retry", e);
                    return false;
                }
            }
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(authEndpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Accept", "application/json");
                
                int responseCode = connection.getResponseCode();
                LOG.info("Auth check response code: " + responseCode);
                
                if (responseCode == 200) {
                    return true;
                }
                
                // 401 Unauthorized - token is invalid, no need to retry
                if (responseCode == 401) {
                    LOG.info("Invalid token, authentication failed");
                    return false;
                }
                
                // Only retry for server errors or timeouts
                if (responseCode < 500) {
                    return false;
                }
            } catch (IOException e) {
                if (retry >= MAX_RETRIES) {
                    LOG.error("Error testing authentication", e);
                } else {
                    LOG.warn("Error testing authentication, will retry", e);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Shutdown the executor service gracefully.
     * This method should be called when the plugin is being unloaded.
     */
    public static void shutdown() {
        try {
            LOG.info("Shutting down connection test executor service");
            EXECUTOR.shutdown();
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warn("Executor did not terminate in the specified time, forcing shutdown");
                EXECUTOR.shutdownNow();
                if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.error("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while shutting down executor", e);
            EXECUTOR.shutdownNow();
        }
    }
}