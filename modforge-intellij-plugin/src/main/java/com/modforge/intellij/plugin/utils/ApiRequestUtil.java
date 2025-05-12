package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utility for making API requests.
 */
public class ApiRequestUtil {
    private static final Logger LOG = Logger.getInstance(ApiRequestUtil.class);
    
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    
    private ApiRequestUtil() {
        // Utility class, no instantiation
    }
    
    /**
     * Makes a GET request to the specified URL.
     * @param url The URL
     * @return The response body, or null if the request failed
     */
    @Nullable
    public static String get(@NotNull String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOG.warn("GET request to " + url + " failed with status code " + response.code());
                    return null;
                }
                
                if (response.body() == null) {
                    LOG.warn("GET request to " + url + " returned null body");
                    return null;
                }
                
                return response.body().string();
            }
        } catch (IOException e) {
            LOG.error("Error making GET request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a GET request to the specified URL with the specified token.
     * @param url The URL
     * @param token The authentication token
     * @return The response body, or null if the request failed
     */
    @Nullable
    public static String get(@NotNull String url, @NotNull String token) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + token)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOG.warn("GET request to " + url + " failed with status code " + response.code());
                    return null;
                }
                
                if (response.body() == null) {
                    LOG.warn("GET request to " + url + " returned null body");
                    return null;
                }
                
                return response.body().string();
            }
        } catch (IOException e) {
            LOG.error("Error making GET request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a POST request to the specified URL with the specified JSON body.
     * @param url The URL
     * @param json The JSON body
     * @return The response body, or null if the request failed
     */
    @Nullable
    public static String post(@NotNull String url, @NotNull String json) {
        try {
            RequestBody body = RequestBody.create(json, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOG.warn("POST request to " + url + " failed with status code " + response.code());
                    return null;
                }
                
                if (response.body() == null) {
                    LOG.warn("POST request to " + url + " returned null body");
                    return null;
                }
                
                return response.body().string();
            }
        } catch (IOException e) {
            LOG.error("Error making POST request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a POST request to the specified URL with the specified JSON body and token.
     * @param url The URL
     * @param json The JSON body
     * @param token The authentication token
     * @return The response body, or null if the request failed
     */
    @Nullable
    public static String post(@NotNull String url, @NotNull String json, @NotNull String token) {
        try {
            RequestBody body = RequestBody.create(json, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + token)
                    .post(body)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOG.warn("POST request to " + url + " failed with status code " + response.code());
                    return null;
                }
                
                if (response.body() == null) {
                    LOG.warn("POST request to " + url + " returned null body");
                    return null;
                }
                
                return response.body().string();
            }
        } catch (IOException e) {
            LOG.error("Error making POST request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a PUT request to the specified URL with the specified JSON body.
     * @param url The URL
     * @param json The JSON body
     * @return The response body, or null if the request failed
     */
    @Nullable
    public static String put(@NotNull String url, @NotNull String json) {
        try {
            RequestBody body = RequestBody.create(json, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOG.warn("PUT request to " + url + " failed with status code " + response.code());
                    return null;
                }
                
                if (response.body() == null) {
                    LOG.warn("PUT request to " + url + " returned null body");
                    return null;
                }
                
                return response.body().string();
            }
        } catch (IOException e) {
            LOG.error("Error making PUT request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a DELETE request to the specified URL.
     * @param url The URL
     * @return Whether the request was successful
     */
    public static boolean delete(@NotNull String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            LOG.error("Error making DELETE request to " + url, e);
            return false;
        }
    }
}