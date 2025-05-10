package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for testing connections to the ModForge server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    
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
     * Test the connection to the server health endpoint synchronously.
     *
     * @param healthEndpoint The health endpoint URL to test
     * @return true if the connection is successful, false otherwise
     */
    private static boolean testHealthEndpoint(String healthEndpoint) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(healthEndpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            LOG.info("Health check response code: " + responseCode);
            
            return responseCode == 200;
        } catch (IOException e) {
            LOG.error("Error testing connection to health endpoint", e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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
     * Test authentication synchronously.
     *
     * @param authEndpoint The authentication endpoint URL to test
     * @param token        The user token
     * @return true if the authentication is successful, false otherwise
     */
    private static boolean testAuthEndpoint(String authEndpoint, String token) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(authEndpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            int responseCode = connection.getResponseCode();
            LOG.info("Auth check response code: " + responseCode);
            
            return responseCode == 200;
        } catch (IOException e) {
            LOG.error("Error testing authentication", e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Shutdown the executor service.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}