package com.modforge.intellij.plugin.api;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Client for communicating with the ModForge API.
 * Enhanced with retry logic, circuit breaker, and error tracking
 * for production reliability.
 */
public class ApiClient {
    private static final Logger LOG = Logger.getInstance(ApiClient.class);

    private final String baseUrl;
    private String authToken;
    private int connectionTimeout = 30000; // 30 seconds
    private int readTimeout = 60000; // 60 seconds

    // Retry settings
    private int maxRetries = 3;
    private long initialRetryDelayMs = 1000;
    private double retryBackoffMultiplier = 2.0;

    // Circuit breaker settings
    private int failureThreshold = 5;
    private long resetTimeoutMs = 60000; // 1 minute
    private final ConcurrentHashMap<String, FailureStats> failureStats = new ConcurrentHashMap<>();

    private static class FailureStats {
        final AtomicInteger failureCount = new AtomicInteger(0);
        volatile Instant lastFailure = Instant.now();

        void recordFailure() {
            failureCount.incrementAndGet();
            lastFailure = Instant.now();
        }

        void reset() {
            failureCount.set(0);
        }

        boolean exceedsThreshold(int threshold) {
            return failureCount.get() >= threshold;
        }

        boolean shouldReset(long resetTimeoutMs) {
            return Instant.now().isAfter(lastFailure.plusMillis(resetTimeoutMs));
        }
    }

    /**
     * Creates a new API client with the given base URL.
     *
     * @param baseUrl The base URL of the API
     */
    public ApiClient(@NotNull String baseUrl) {
        // Ensure URL ends with a slash
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /**
     * Sets the authentication token to use for requests.
     *
     * @param authToken The authentication token
     */
    public void setAuthToken(@Nullable String authToken) {
        this.authToken = authToken;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     */
    public void setConnectionTimeout(int timeoutMs) {
        this.connectionTimeout = timeoutMs;
    }

    /**
     * Sets the read timeout in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     */
    public void setReadTimeout(int timeoutMs) {
        this.readTimeout = timeoutMs;
    }

    /**
     * Sets the maximum number of retries for failed requests.
     *
     * @param maxRetries Maximum number of retries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
    }

    /**
     * Sets the initial retry delay in milliseconds.
     *
     * @param initialRetryDelayMs Initial delay in milliseconds
     */
    public void setInitialRetryDelayMs(long initialRetryDelayMs) {
        this.initialRetryDelayMs = Math.max(100, initialRetryDelayMs);
    }

    /**
     * Sets the backoff multiplier for retries.
     *
     * @param retryBackoffMultiplier Multiplier for each retry delay
     */
    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = Math.max(1.0, retryBackoffMultiplier);
    }

    /**
     * Sets the failure threshold for the circuit breaker.
     *
     * @param failureThreshold Number of failures before circuit breaker opens
     */
    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = Math.max(1, failureThreshold);
    }

    /**
     * Sets the reset timeout for the circuit breaker in milliseconds.
     *
     * @param resetTimeoutMs Timeout in milliseconds
     */
    public void setResetTimeoutMs(long resetTimeoutMs) {
        this.resetTimeoutMs = Math.max(1000, resetTimeoutMs);
    }

    /**
     * Performs a GET request to the specified endpoint with retry logic and circuit
     * breaker.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @return The response body
     * @throws IOException If an I/O error occurs after all retries
     */
    public String get(@NotNull String endpoint) throws IOException {
        return executeWithRetry("GET", endpoint, null);
    }

    /**
     * Checks if a status code is retryable.
     * Retryable status codes are 408, 429, 500, 502, 503, 504.
     *
     * @param statusCode HTTP status code
     * @return True if the status code is retryable
     */
    private boolean isRetryableStatusCode(int statusCode) {
        return statusCode == 408 || // Request Timeout
                statusCode == 429 || // Too Many Requests
                statusCode == 500 || // Internal Server Error
                statusCode == 502 || // Bad Gateway
                statusCode == 503 || // Service Unavailable
                statusCode == 504; // Gateway Timeout
    }

    /**
     * Checks if an exception is retryable.
     *
     * @param e The exception to check
     * @return True if the exception is retryable
     */
    private boolean isRetryableException(IOException e) {
        // Socket timeouts are already handled separately
        if (e instanceof SocketTimeoutException) {
            return true;
        }

        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // Common network errors that are usually transient
        return message.contains("Connection reset") ||
                message.contains("Connection refused") ||
                message.contains("broken pipe") ||
                message.contains("Connection closed") ||
                message.contains("Unexpected end of file") ||
                message.contains("Connection timed out");
    }

    /**
     * Checks if the circuit breaker is open for an endpoint.
     * Throws an exception if the circuit is open.
     *
     * @param endpoint The normalized endpoint
     * @throws IOException If the circuit breaker is open
     */
    private void checkCircuitBreaker(String endpoint) throws IOException {
        FailureStats stats = failureStats.get(endpoint);

        if (stats != null) {
            // If reset timeout has passed, try to reset the circuit breaker
            if (stats.shouldReset(resetTimeoutMs)) {
                LOG.info("Circuit breaker reset timeout reached for endpoint: " + endpoint);
                stats.reset();
            } else if (stats.exceedsThreshold(failureThreshold)) {
                LOG.warn("Circuit breaker open for endpoint: " + endpoint);
                throw new IOException("Circuit breaker open for endpoint: " + endpoint);
            }
        }
    }

    /**
     * Records a failure for an endpoint.
     *
     * @param endpoint The normalized endpoint
     */
    private void recordFailure(String endpoint) {
        failureStats.computeIfAbsent(endpoint, k -> new FailureStats()).recordFailure();
    }

    /**
     * Resets failure stats for an endpoint on success.
     *
     * @param endpoint The normalized endpoint
     */
    private void resetFailureStats(String endpoint) {
        FailureStats stats = failureStats.get(endpoint);
        if (stats != null) {
            stats.reset();
        }
    }

    /**
     * Executes an HTTP request with retry logic.
     * 
     * @param method   HTTP method (GET, POST, etc.)
     * @param endpoint API endpoint
     * @param jsonBody Optional JSON body (for POST/PUT requests)
     * @return Response body
     * @throws IOException If an error occurs after all retries
     */
    private String executeWithRetry(@NotNull String method, @NotNull String endpoint, @Nullable String jsonBody)
            throws IOException {
        // Normalize endpoint
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        String urlStr = baseUrl + normalizedEndpoint;

        // Check circuit breaker
        checkCircuitBreaker(normalizedEndpoint);

        IOException lastException = null;
        int retryCount = 0;
        long delay = initialRetryDelayMs;

        while (retryCount <= maxRetries) {
            if (retryCount > 0) {
                LOG.info("Retry " + retryCount + " for " + method + " request to " + urlStr);

                try {
                    Thread.sleep(delay);
                    delay = (long) (delay * retryBackoffMultiplier);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted during retry", e);
                }
            }

            LOG.debug(method + " request to " + urlStr + (retryCount > 0 ? " (retry " + retryCount + ")" : ""));

            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(connectionTimeout);
                connection.setReadTimeout(readTimeout);

                // Set authentication header if token is available
                if (authToken != null && !authToken.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + authToken);
                }

                // Set JSON content type for requests and accepts
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");

                // For POST and PUT, enable output and write body
                if (("POST".equals(method) || "PUT".equals(method)) && jsonBody != null) {
                    connection.setDoOutput(true);
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(connection.getURL().toString())
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    int responseCode = response.code();

                    if (responseCode >= 200 && responseCode < 300) {
                        // Success - read response
                        String responseBody = response.body() != null ? response.body().string() : "";
                        // Reset failure stats on success
                        resetFailureStats(normalizedEndpoint);
                        return responseBody;
                    } else if (isRetryableStatusCode(responseCode)) {
                        // Retryable error
                        String errorMessage = response.body() != null ? response.body().string() : "";
                        lastException = new IOException(
                                "API request failed with retryable code " + responseCode + ": " + errorMessage);
                        LOG.warn("API request failed with retryable code " + responseCode + ": " + errorMessage +
                                (retryCount < maxRetries ? " - will retry" : " - retry limit reached"));

                        // Record failure but continue with retry
                        recordFailure(normalizedEndpoint);
                    } else {
                        // Non-retryable error
                        String errorMessage = response.body() != null ? response.body().string() : "";
                        throw new IOException(
                                "API request failed with non-retryable code " + responseCode + ": " + errorMessage);
                    }
                }
            } catch (SocketTimeoutException e) {
                // Always retry timeouts
                lastException = e;
                LOG.warn("Timeout during " + method + " request to " + urlStr +
                        (retryCount < maxRetries ? " - will retry" : " - retry limit reached"), e);

                // Timeout should be retried but still counts as a failure
                recordFailure(normalizedEndpoint);
            } catch (IOException e) {
                if (isRetryableException(e)) {
                    lastException = e;
                    LOG.warn("Retryable error during " + method + " request to " + urlStr +
                            (retryCount < maxRetries ? " - will retry" : " - retry limit reached"), e);

                    // Record failure but continue with retry
                    recordFailure(normalizedEndpoint);
                } else {
                    // Non-retryable exception
                    recordFailure(normalizedEndpoint);
                    throw e;
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            retryCount++;
        }

        // If we get here, we've exhausted retries
        throw lastException != null ? lastException
                : new IOException(method + " request to " + urlStr + " failed after " + maxRetries + " retries");
    }

    /**
     * Performs a POST request to the specified endpoint with the given body.
     * Enhanced with retry logic and circuit breaker.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @param jsonBody The JSON body to send
     * @return The response body
     * @throws IOException If an I/O error occurs after all retries
     */
    public String post(@NotNull String endpoint, @Nullable String jsonBody) throws IOException {
        return executeWithRetry("POST", endpoint, jsonBody);
    }

    /**
     * Performs a PUT request to the specified endpoint with the given body.
     * Enhanced with retry logic and circuit breaker.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @param jsonBody The JSON body to send
     * @return The response body
     * @throws IOException If an I/O error occurs after all retries
     */
    public String put(@NotNull String endpoint, @Nullable String jsonBody) throws IOException {
        return executeWithRetry("PUT", endpoint, jsonBody);
    }

    /**
     * Performs a DELETE request to the specified endpoint.
     * Enhanced with retry logic and circuit breaker.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @return The response body
     * @throws IOException If an I/O error occurs after all retries
     */
    public String delete(@NotNull String endpoint) throws IOException {
        return executeWithRetry("DELETE", endpoint, null);
    }

    /**
     * Performs a PATCH request to the specified endpoint with the given body.
     * Enhanced with retry logic and circuit breaker.
     *
     * @param endpoint The API endpoint (without the base URL)
     * @param jsonBody The JSON body to send
     * @return The response body
     * @throws IOException If an I/O error occurs after all retries
     */
    public String patch(@NotNull String endpoint, @Nullable String jsonBody) throws IOException {
        return executeWithRetry("PATCH", endpoint, jsonBody);
    }

    /**
     * Normalizes an endpoint by removing leading slash.
     *
     * @param endpoint The endpoint to normalize
     * @return The normalized endpoint
     */
    private String normalizeEndpoint(String endpoint) {
        // Remove leading slash if present
        return endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
    }
}