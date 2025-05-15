package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utility for token authentication connections.
 * This class provides utility methods for making authenticated HTTP requests
 * with tokens.
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
     * @param token     The authentication token
     * @return True if authentication works, false otherwise
     */
    public static boolean testTokenAuthentication(@NotNull String serverUrl, @NotNull String token) {
        try {
            String url = serverUrl;
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "api/auth/verify";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.code() == HttpURLConnection.HTTP_OK;
            }
        } catch (IOException e) {
            LOG.warn("Failed to test token authentication", e);
            return false;
        }
    }

    /**
     * Executes a GET request with token authentication.
     *
     * @param serverUrl The server URL
     * @param endpoint  The endpoint
     * @param token     The authentication token
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

            return HttpRequests.request(url)
                    .accept("application/json")
                    .productNameAsUserAgent()
                    .tuner(connection -> connection.setRequestProperty("Authorization", "Bearer " + token))
                    .connect(request -> {
                        if (request.getConnection() instanceof HttpURLConnection) {
                            HttpURLConnection connection = (HttpURLConnection) request.getConnection();

                            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                // Read response
                                return request.readString();
                            }
                        }
                        return null;
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
     * @param endpoint  The endpoint
     * @param token     The authentication token
     * @param body      The request body
     * @return The response, or null if an error occurs
     */
    @Nullable
    public static String executePost(@NotNull String serverUrl, @NotNull String endpoint,
            @NotNull String token, @Nullable String body) {
        try {
            String url = serverUrl;
            if (!url.endsWith("/") && !endpoint.startsWith("/")) {
                url += "/";
            }
            url += endpoint;

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = body != null
                    ? RequestBody.create(body, MediaType.get("application/json"))
                    : RequestBody.create(new byte[0], MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return response.body() != null ? response.body().string() : null;
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to execute POST request", e);
        }
        return null;
    }

    /**
     * Makes an authenticated GET request.
     *
     * @param url The URL to request
     * @return The response body
     */
    @Nullable
    public static String makeAuthenticatedGetRequest(@NotNull String url) throws IOException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getToken();

        return makeRequest(url, token, null, "GET");
    }

    /**
     * Makes an authenticated POST request.
     *
     * @param url  The URL to request
     * @param body The request body
     * @return The response body
     */
    @Nullable
    public static String makeAuthenticatedPostRequest(@NotNull String url, @Nullable String body) throws IOException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getToken();

        return makeRequest(url, token, body, "POST");
    }

    /**
     * Makes an authenticated PUT request.
     *
     * @param url  The URL to request
     * @param body The request body
     * @return The response body
     */
    @Nullable
    public static String makeAuthenticatedPutRequest(@NotNull String url, @Nullable String body) throws IOException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getToken();

        return makeRequest(url, token, body, "PUT");
    }

    /**
     * Makes an authenticated DELETE request.
     *
     * @param url The URL to request
     * @return The response body
     */
    @Nullable
    public static String makeAuthenticatedDeleteRequest(@NotNull String url) throws IOException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getToken();

        return makeRequest(url, token, null, "DELETE");
    }

    @Nullable
    private static String makeRequest(String url, String token, @Nullable String body, String method)
            throws IOException {
        return HttpRequests.request(url)
                .tuner(connection -> {
                    if (connection instanceof HttpURLConnection) {
                        HttpURLConnection httpConnection = (HttpURLConnection) connection;
                        httpConnection.setRequestMethod(method);
                        httpConnection.setRequestProperty("Authorization", "Bearer " + token);
                        httpConnection.setRequestProperty("Content-Type", "application/json");

                        if (body != null && !body.isEmpty() && (method.equals("POST") || method.equals("PUT"))) {
                            httpConnection.setDoOutput(true);
                            httpConnection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                })
                .accept("application/json")
                .connectTimeout(30000)
                .readTimeout(30000)
                .readString();
    }
}