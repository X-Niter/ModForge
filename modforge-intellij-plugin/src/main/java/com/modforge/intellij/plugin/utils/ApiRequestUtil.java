package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for making API requests.
 */
public class ApiRequestUtil {
    private static final Logger LOG = Logger.getInstance(ApiRequestUtil.class);
    
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    
    /**
     * Makes a GET request to the specified URL.
     * @param url The URL to make the request to
     * @return The response body, or null if the request failed
     */
    public static String get(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    LOG.warn("Failed to make GET request to " + url + ". Status: " + response.code());
                    return null;
                }
            }
        } catch (IOException e) {
            LOG.error("Error making GET request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a POST request to the specified URL with the specified JSON body.
     * @param url The URL to make the request to
     * @param json The JSON body
     * @return The response body, or null if the request failed
     */
    public static String post(String url, String json) {
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(json, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    LOG.warn("Failed to make POST request to " + url + ". Status: " + response.code());
                    return null;
                }
            }
        } catch (IOException e) {
            LOG.error("Error making POST request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a PUT request to the specified URL with the specified JSON body.
     * @param url The URL to make the request to
     * @param json The JSON body
     * @return The response body, or null if the request failed
     */
    public static String put(String url, String json) {
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(json, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();
            
            try (Response response = CLIENT.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    LOG.warn("Failed to make PUT request to " + url + ". Status: " + response.code());
                    return null;
                }
            }
        } catch (IOException e) {
            LOG.error("Error making PUT request to " + url, e);
            return null;
        }
    }
    
    /**
     * Makes a DELETE request to the specified URL.
     * @param url The URL to make the request to
     * @return True if the request was successful, false otherwise
     */
    public static boolean delete(String url) {
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