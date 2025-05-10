package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility for making token-authenticated HTTP requests to the ModForge server.
 */
public class TokenAuthConnectionUtil {
    private static final Logger LOG = Logger.getInstance(TokenAuthConnectionUtil.class);
    
    /**
     * Make a GET request to the ModForge server with token authentication.
     * @param serverUrl Base URL of the ModForge server
     * @param endpoint API endpoint
     * @param token Authentication token
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject get(String serverUrl, String endpoint, String token) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Remove leading slash from endpoint if present
            if (endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            
            // Create URL
            String url = serverUrl + endpoint;
            
            LOG.info("Making GET request to " + url);
            
            return makeRequest("GET", url, token, null);
        } catch (Exception e) {
            LOG.error("GET request failed", e);
            return null;
        }
    }
    
    /**
     * Make a POST request to the ModForge server with token authentication.
     * @param serverUrl Base URL of the ModForge server
     * @param endpoint API endpoint
     * @param token Authentication token
     * @param data Data to send in request body as JSONObject
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject post(String serverUrl, String endpoint, String token, JSONObject data) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Remove leading slash from endpoint if present
            if (endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            
            // Create URL
            String url = serverUrl + endpoint;
            
            LOG.info("Making POST request to " + url);
            
            return makeRequest("POST", url, token, data);
        } catch (Exception e) {
            LOG.error("POST request failed", e);
            return null;
        }
    }
    
    /**
     * Make a PUT request to the ModForge server with token authentication.
     * @param serverUrl Base URL of the ModForge server
     * @param endpoint API endpoint
     * @param token Authentication token
     * @param data Data to send in request body as JSONObject
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject put(String serverUrl, String endpoint, String token, JSONObject data) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Remove leading slash from endpoint if present
            if (endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            
            // Create URL
            String url = serverUrl + endpoint;
            
            LOG.info("Making PUT request to " + url);
            
            return makeRequest("PUT", url, token, data);
        } catch (Exception e) {
            LOG.error("PUT request failed", e);
            return null;
        }
    }
    
    /**
     * Make a DELETE request to the ModForge server with token authentication.
     * @param serverUrl Base URL of the ModForge server
     * @param endpoint API endpoint
     * @param token Authentication token
     * @return Response as JSONObject, or null if request failed
     */
    public static JSONObject delete(String serverUrl, String endpoint, String token) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Remove leading slash from endpoint if present
            if (endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            
            // Create URL
            String url = serverUrl + endpoint;
            
            LOG.info("Making DELETE request to " + url);
            
            return makeRequest("DELETE", url, token, null);
        } catch (Exception e) {
            LOG.error("DELETE request failed", e);
            return null;
        }
    }
    
    /**
     * Make an HTTP request with token authentication.
     * @param method HTTP method (GET, POST, PUT, DELETE)
     * @param urlString URL to make request to
     * @param token Authentication token
     * @param data Data to send in request body as JSONObject (for POST and PUT)
     * @return Response as JSONObject, or null if request failed
     * @throws IOException If connection fails
     * @throws ParseException If JSON parsing fails
     */
    private static JSONObject makeRequest(String method, String urlString, String token, JSONObject data) 
            throws IOException, ParseException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        
        // Add token if provided
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        
        // Add request body for POST and PUT
        if ((method.equals("POST") || method.equals("PUT")) && data != null) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = data.toJSONString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        int responseCode = connection.getResponseCode();
        
        if (responseCode >= 400) {
            LOG.error(method + " request failed with response code " + responseCode);
            return null;
        }
        
        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // Parse response
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(response.toString());
    }
}