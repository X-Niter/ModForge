package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility for testing connections to ModForge server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    
    /**
     * Test connection to server.
     *
     * @param serverUrl Server URL
     * @return Connection result (true if successful, false otherwise)
     */
    public static boolean testConnection(String serverUrl) {
        try {
            // Create a task to test the connection
            Callable<Boolean> connectionTask = () -> {
                try {
                    // Create connection to health API
                    URL url = new URL(serverUrl + "/api/health");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    
                    // Set request method and timeout
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                    connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
                    
                    // Check response
                    int responseCode = connection.getResponseCode();
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Connection successful
                        LOG.info("Connection successful to: " + serverUrl);
                        return true;
                    } else {
                        // Connection failed
                        LOG.info("Connection failed to: " + serverUrl + ", response code: " + responseCode);
                        return false;
                    }
                } catch (IOException e) {
                    LOG.error("Error testing connection", e);
                    return false;
                }
            };
            
            // Execute the task with a timeout
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(connectionTask);
            
            try {
                // Wait for the task to complete with timeout
                boolean result = future.get(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                return result;
            } catch (TimeoutException e) {
                // Connection timeout
                LOG.error("Connection timeout to: " + serverUrl);
                future.cancel(true);
                return false;
            } catch (Exception e) {
                // Other error
                LOG.error("Error testing connection", e);
                return false;
            } finally {
                // Shutdown executor
                executor.shutdownNow();
            }
        } catch (Exception e) {
            LOG.error("Error testing connection", e);
            return false;
        }
    }
    
    /**
     * Test connection to server with custom timeout.
     *
     * @param serverUrl Server URL
     * @param timeoutMs Timeout in milliseconds
     * @return Connection result (true if successful, false otherwise)
     */
    public static boolean testConnection(String serverUrl, int timeoutMs) {
        try {
            // Create a task to test the connection
            Callable<Boolean> connectionTask = () -> {
                try {
                    // Create connection to health API
                    URL url = new URL(serverUrl + "/api/health");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    
                    // Set request method and timeout
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(timeoutMs);
                    connection.setReadTimeout(timeoutMs);
                    
                    // Check response
                    int responseCode = connection.getResponseCode();
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Connection successful
                        LOG.info("Connection successful to: " + serverUrl);
                        return true;
                    } else {
                        // Connection failed
                        LOG.info("Connection failed to: " + serverUrl + ", response code: " + responseCode);
                        return false;
                    }
                } catch (IOException e) {
                    LOG.error("Error testing connection", e);
                    return false;
                }
            };
            
            // Execute the task with a timeout
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(connectionTask);
            
            try {
                // Wait for the task to complete with timeout
                boolean result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                return result;
            } catch (TimeoutException e) {
                // Connection timeout
                LOG.error("Connection timeout to: " + serverUrl);
                future.cancel(true);
                return false;
            } catch (Exception e) {
                // Other error
                LOG.error("Error testing connection", e);
                return false;
            } finally {
                // Shutdown executor
                executor.shutdownNow();
            }
        } catch (Exception e) {
            LOG.error("Error testing connection", e);
            return false;
        }
    }
}