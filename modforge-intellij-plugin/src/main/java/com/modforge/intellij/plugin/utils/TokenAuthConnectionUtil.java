package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Utility for making authenticated HTTP requests with token auth.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    
    /**
     * Make a GET request with token authentication.
     *
     * @param serverUrl Server URL
     * @param path API path
     * @param token Authentication token
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject get(String serverUrl, String path, String token) {
        try {
            // Create connection
            URL url = new URL(serverUrl + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                return readResponse(connection);
            } else {
                // Request failed
                LOG.info("GET request failed: " + path + ", response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error making GET request", e);
            return null;
        }
    }
    
    /**
     * Make a POST request with token authentication.
     *
     * @param serverUrl Server URL
     * @param path API path
     * @param token Authentication token
     * @param body Request body as JSONObject
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject post(String serverUrl, String path, String token, JSONObject body) {
        try {
            // Create connection
            URL url = new URL(serverUrl + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setDoOutput(true);
            
            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.toJSONString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                // Read response
                return readResponse(connection);
            } else {
                // Request failed
                LOG.info("POST request failed: " + path + ", response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error making POST request", e);
            return null;
        }
    }
    
    /**
     * Make a PUT request with token authentication.
     *
     * @param serverUrl Server URL
     * @param path API path
     * @param token Authentication token
     * @param body Request body as JSONObject
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject put(String serverUrl, String path, String token, JSONObject body) {
        try {
            // Create connection
            URL url = new URL(serverUrl + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setDoOutput(true);
            
            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.toJSONString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                return readResponse(connection);
            } else {
                // Request failed
                LOG.info("PUT request failed: " + path + ", response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error making PUT request", e);
            return null;
        }
    }
    
    /**
     * Make a DELETE request with token authentication.
     *
     * @param serverUrl Server URL
     * @param path API path
     * @param token Authentication token
     * @return true if successful, false otherwise
     */
    public static boolean delete(String serverUrl, String path, String token) {
        try {
            // Create connection
            URL url = new URL(serverUrl + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                // Delete successful
                return true;
            } else {
                // Request failed
                LOG.info("DELETE request failed: " + path + ", response code: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            LOG.error("Error making DELETE request", e);
            return false;
        }
    }
    
    /**
     * Read response body as JSONObject.
     *
     * @param connection HTTP connection
     * @return Response as JSONObject, or null if parsing failed
     */
    private static JSONObject readResponse(HttpURLConnection connection) {
        try {
            // Read response body
            try (InputStream is = connection.getInputStream();
                 Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                
                // Parse JSON
                JSONParser parser = new JSONParser();
                return (JSONObject) parser.parse(response);
            }
        } catch (IOException | ParseException e) {
            LOG.error("Error reading response", e);
            return null;
        }
    }
}