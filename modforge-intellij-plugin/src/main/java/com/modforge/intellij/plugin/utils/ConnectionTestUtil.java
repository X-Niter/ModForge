package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class for testing network connections with modern Java 21 features.
 * Optimized for IntelliJ IDEA 2025.1 compatibility.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    
    // Connection timeout in seconds
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    
    // Maximum number of retries
    private static final int MAX_RETRIES = 3;
    
    // Executor service for virtual threads (Java 21 feature)
    private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * Test a connection to the specified URL
     * 
     * @param url The URL to test
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection(String url) {
        LOG.info("Testing connection to " + url);
        StopWatch watch = new StopWatch();
        watch.start();
        
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .executor(virtualThreadExecutor)
                .build();
                
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .GET()
                .build();
                
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            
            watch.stop();
            int statusCode = response.statusCode();
            LOG.info("Connection test to " + url + " completed in " + watch.getTime(TimeUnit.MILLISECONDS) + "ms with status: " + statusCode);
            
            return statusCode >= 200 && statusCode < 300;
        } catch (IOException | InterruptedException e) {
            watch.stop();
            LOG.warn("Connection test to " + url + " failed after " + watch.getTime(TimeUnit.MILLISECONDS) + "ms: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test a connection with retry logic using exponential backoff
     * 
     * @param url The URL to test
     * @return true if connection succeeds within retry attempts, false otherwise
     */
    public static boolean testConnectionWithRetry(String url) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            if (testConnection(url)) {
                if (attempt > 1) {
                    LOG.info("Connection succeeded on retry attempt " + attempt);
                }
                return true;
            }
            
            if (attempt < MAX_RETRIES) {
                // Exponential backoff: 1s, 2s, 4s, etc.
                long delayMs = (long) Math.pow(2, attempt - 1) * 1000;
                LOG.info("Retrying connection in " + delayMs + "ms (attempt " + attempt + " of " + MAX_RETRIES + ")");
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Connection retry interrupted");
                    return false;
                }
            }
        }
        
        LOG.warn("Connection test failed after " + MAX_RETRIES + " attempts");
        return false;
    }
    
    /**
     * Asynchronously test a connection using virtual threads
     * 
     * @param url The URL to test
     * @return CompletableFuture that resolves to connection test result
     */
    public static CompletableFuture<Boolean> testConnectionAsync(String url) {
        return CompletableFuture.supplyAsync(() -> testConnection(url), virtualThreadExecutor);
    }
    
    /**
     * Test if a connection has proper authentication
     * 
     * @param url The URL to test
     * @param authHeaderSupplier Supplier that provides the authentication header
     * @return true if authenticated connection succeeds, false otherwise
     */
    public static boolean testAuthenticatedConnection(String url, Supplier<String> authHeaderSupplier) {
        LOG.info("Testing authenticated connection to " + url);
        StopWatch watch = new StopWatch();
        watch.start();
        
        try {
            String authHeader = authHeaderSupplier.get();
            if (authHeader == null || authHeader.isEmpty()) {
                LOG.warn("Authentication header is empty or null");
                return false;
            }
            
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .executor(virtualThreadExecutor)
                .build();
                
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .header("Authorization", authHeader)
                .GET()
                .build();
                
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            
            watch.stop();
            int statusCode = response.statusCode();
            LOG.info("Authenticated connection test to " + url + " completed in " + watch.getTime(TimeUnit.MILLISECONDS) + 
                    "ms with status: " + statusCode);
            
            return statusCode != HttpURLConnection.HTTP_UNAUTHORIZED 
                && statusCode != HttpURLConnection.HTTP_FORBIDDEN;
        } catch (IOException | InterruptedException e) {
            watch.stop();
            LOG.warn("Authenticated connection test to " + url + " failed after " + 
                    watch.getTime(TimeUnit.MILLISECONDS) + "ms: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current health status of connection utilities
     * 
     * @return A string representation of the connection utilities health
     */
    public static String getConnectionHealthStatus() {
        return "Virtual Thread Executor: " + (virtualThreadExecutor.isShutdown() ? "SHUTDOWN" : "ACTIVE");
    }
    
    /**
     * Clean up resources when plugin is shutting down
     */
    public static void cleanup() {
        LOG.info("Cleaning up connection test utilities");
        virtualThreadExecutor.shutdownNow();
    }
}