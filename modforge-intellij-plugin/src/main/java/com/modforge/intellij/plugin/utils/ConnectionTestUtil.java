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
            // Sanitize URL 
            if (url == null || url.trim().isEmpty()) {
                LOG.warn("Invalid URL: null or empty");
                return false;
            }
            
            // Ensure URL has proper scheme
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                LOG.warn("URL missing scheme, assuming https: " + url);
                url = "https://" + url;
            }
            
            // Create client with virtual thread executor and connection timeout
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .executor(virtualThreadExecutor)
                .version(HttpClient.Version.HTTP_2) // Prefer HTTP/2 for better performance
                .followRedirects(HttpClient.Redirect.NORMAL) // Follow redirects
                .build();
            
            // Build request with appropriate headers and timeout    
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .header("User-Agent", "ModForge-IntelliJ-Plugin/2.1.0")
                .header("Accept", "*/*")
                .GET()
                .build();
            
            // Send request with empty response body handler for efficiency
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            
            watch.stop();
            int statusCode = response.statusCode();
            
            // Calculate and log performance metrics
            long elapsed = watch.getTime(TimeUnit.MILLISECONDS);
            if (elapsed > 5000) {
                LOG.warn("Connection test to " + url + " completed in " + elapsed + "ms (slow) with status: " + statusCode);
            } else {
                LOG.info("Connection test to " + url + " completed in " + elapsed + "ms with status: " + statusCode);
            }
            
            // Accept all 2xx responses as success
            if (statusCode >= 200 && statusCode < 300) {
                return true;
            }
            
            // Special handling for specific status codes
            if (statusCode == 301 || statusCode == 302 || statusCode == 307 || statusCode == 308) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location != null) {
                    LOG.info("Redirect detected to: " + location);
                    // Following redirects should be handled by the client, but log it anyway
                }
                // Consider redirects as successful connections
                return true;
            }
            
            // Consider 401/403 as "successful" connections for test purposes
            // since they indicate the server is responding
            if (statusCode == 401 || statusCode == 403) {
                LOG.info("Authentication required but connection successful");
                return true;
            }
            
            // For all other status codes, log but consider as failure
            LOG.warn("Connection test received unexpected status: " + statusCode);
            return false;
        } catch (IllegalArgumentException e) {
            watch.stop();
            LOG.warn("Invalid URL format: " + url + " - " + e.getMessage());
            return false;
        } catch (IOException e) {
            watch.stop();
            LOG.warn("Connection test to " + url + " failed after " + watch.getTime(TimeUnit.MILLISECONDS) + 
                    "ms with I/O error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            
            // Log more detailed connection error information
            if (e.getCause() != null) {
                LOG.info("Root cause: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            
            return false;
        } catch (InterruptedException e) {
            watch.stop();
            Thread.currentThread().interrupt(); // Restore interrupted status
            LOG.warn("Connection test interrupted after " + watch.getTime(TimeUnit.MILLISECONDS) + "ms");
            return false;
        } catch (Exception e) {
            watch.stop();
            LOG.warn("Unexpected error during connection test to " + url + ": " + 
                    e.getClass().getName() + " - " + e.getMessage(), e);
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
        // Record start time for overall retry timing
        StopWatch totalWatch = new StopWatch();
        totalWatch.start();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (testConnection(url)) {
                    if (attempt > 1) {
                        LOG.info("Connection succeeded on retry attempt " + attempt + 
                                " after " + totalWatch.getTime(TimeUnit.MILLISECONDS) + "ms");
                    }
                    totalWatch.stop();
                    return true;
                }
                
                if (attempt < MAX_RETRIES) {
                    // Exponential backoff with jitter: base * (1.5^attempt) + random jitter
                    long baseDelay = 1000; // 1 second base
                    double multiplier = Math.pow(1.5, attempt - 1);
                    long calculatedDelay = (long)(baseDelay * multiplier);
                    
                    // Add jitter (±20% of calculated delay)
                    long jitter = (long)(calculatedDelay * 0.2 * (Math.random() * 2 - 1));
                    long totalDelay = Math.max(500, calculatedDelay + jitter); // At least 500ms
                    
                    LOG.info("Retrying connection in " + totalDelay + "ms (attempt " + attempt + 
                             " of " + MAX_RETRIES + ", base delay: " + calculatedDelay + 
                             "ms, jitter: " + jitter + "ms)");
                    
                    try {
                        Thread.sleep(totalDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.warn("Connection retry interrupted after " + 
                                totalWatch.getTime(TimeUnit.MILLISECONDS) + "ms");
                        totalWatch.stop();
                        return false;
                    }
                }
            } catch (Exception e) {
                // This shouldn't normally happen as testConnection() catches its exceptions
                // But we add this for extra protection
                lastException = e;
                LOG.warn("Unexpected error during retry attempt " + attempt + ": " + e.getMessage(), e);
                
                // Continue to next retry
                if (attempt < MAX_RETRIES) {
                    try {
                        // Simple delay before next attempt
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOG.warn("Connection retry interrupted");
                        totalWatch.stop();
                        return false;
                    }
                }
            }
        }
        
        totalWatch.stop();
        LOG.warn("Connection test failed after " + MAX_RETRIES + " attempts (" + 
                totalWatch.getTime(TimeUnit.MILLISECONDS) + "ms total)" + 
                (lastException != null ? ". Last error: " + lastException.getMessage() : ""));
        
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
            // Validate URL
            if (url == null || url.trim().isEmpty()) {
                LOG.warn("Invalid URL for authenticated connection: null or empty");
                return false;
            }
            
            // Ensure URL has proper scheme
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                LOG.warn("URL missing scheme for authenticated connection, assuming https: " + url);
                url = "https://" + url;
            }
            
            // Get authentication header safely
            String authHeader = null;
            try {
                authHeader = authHeaderSupplier.get();
            } catch (Exception e) {
                LOG.warn("Error retrieving authentication header: " + e.getMessage(), e);
                return false;
            }
            
            if (authHeader == null || authHeader.isEmpty()) {
                LOG.warn("Authentication header is empty or null");
                return false;
            }
            
            // Mask token in logs for security (show only first few and last few characters)
            String logSafeAuthHeader = authHeader;
            if (authHeader.length() > 10) {
                int visibleChars = 4;
                if (authHeader.startsWith("Bearer ")) {
                    // For Bearer tokens, mask the token part only
                    String token = authHeader.substring(7);
                    if (token.length() > visibleChars * 2) {
                        logSafeAuthHeader = "Bearer " + token.substring(0, visibleChars) + 
                                "..." + token.substring(token.length() - visibleChars);
                    }
                } else if (authHeader.startsWith("token ")) {
                    // For GitHub-style tokens
                    String token = authHeader.substring(6);
                    if (token.length() > visibleChars * 2) {
                        logSafeAuthHeader = "token " + token.substring(0, visibleChars) + 
                                "..." + token.substring(token.length() - visibleChars);
                    }
                } else {
                    // For other auth header formats
                    logSafeAuthHeader = authHeader.substring(0, visibleChars) + 
                            "..." + authHeader.substring(authHeader.length() - visibleChars);
                }
            }
            LOG.info("Using authentication header: " + logSafeAuthHeader);
            
            // Build HTTP client with modern features
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .executor(virtualThreadExecutor)
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
                
            // Build request with timeout and auth header
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .header("Authorization", authHeader)
                .header("User-Agent", "ModForge-IntelliJ-Plugin/2.1.0")
                .header("Accept", "*/*")
                .GET()
                .build();
            
            // Send request with discarding body handler for efficiency
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            
            watch.stop();
            int statusCode = response.statusCode();
            
            // Calculate and log performance metrics
            long elapsed = watch.getTime(TimeUnit.MILLISECONDS);
            if (elapsed > 5000) {
                LOG.warn("Authenticated connection test to " + url + " completed in " + 
                        elapsed + "ms (slow) with status: " + statusCode);
            } else {
                LOG.info("Authenticated connection test to " + url + " completed in " + 
                        elapsed + "ms with status: " + statusCode);
            }
            
            // Check for successful status codes: 2xx or specific others that indicate the server responded
            boolean isSuccessful = statusCode >= 200 && statusCode < 300;
            
            // Special handling for GitHub API rate limiting
            if (statusCode == 429) {
                String rateLimit = response.headers().firstValue("X-RateLimit-Limit").orElse(null);
                String rateLimitRemaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);
                String rateLimitReset = response.headers().firstValue("X-RateLimit-Reset").orElse(null);
                
                LOG.warn("GitHub API rate limit reached: " + rateLimit + " (remaining: " + 
                        rateLimitRemaining + ", reset: " + rateLimitReset + ")");
                
                // Rate limiting is a problem but indicates the connection works
                return true;
            }
            
            // For authentication failures, connection was established but credentials failed
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                LOG.warn("Authentication failed with status " + statusCode + " but connection was established");
                return false;
            }
            
            // For redirection responses
            if (statusCode >= 300 && statusCode < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                LOG.info("Authenticated request redirected to: " + location);
                // Redirection indicates the server is responding
                return true;
            }
            
            // For other status codes
            if (!isSuccessful) {
                LOG.warn("Authenticated connection test received unexpected status: " + statusCode);
                // Consider as success if we got any response, even an error
                return true;
            }
            
            // Normal success path
            return true;
        } catch (IllegalArgumentException e) {
            watch.stop();
            LOG.warn("Invalid URL format for authenticated connection: " + url + " - " + e.getMessage());
            return false;
        } catch (IOException e) {
            watch.stop();
            LOG.warn("Authenticated connection test to " + url + " failed after " + 
                    watch.getTime(TimeUnit.MILLISECONDS) + "ms with I/O error: " + 
                    e.getClass().getSimpleName() + " - " + e.getMessage());
            
            // Log more detailed error information
            if (e.getCause() != null) {
                LOG.info("Root cause: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            
            return false;
        } catch (InterruptedException e) {
            watch.stop();
            Thread.currentThread().interrupt(); // Restore interrupted status
            LOG.warn("Authenticated connection test interrupted after " + watch.getTime(TimeUnit.MILLISECONDS) + "ms");
            return false;
        } catch (Exception e) {
            watch.stop();
            LOG.warn("Unexpected error during authenticated connection test to " + url + ": " + 
                    e.getClass().getName() + " - " + e.getMessage(), e);
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