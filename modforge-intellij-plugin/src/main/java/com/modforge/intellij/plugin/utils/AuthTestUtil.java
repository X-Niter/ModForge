package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

/**
 * Utility for testing authentication with ModForge server.
 */
public class AuthTestUtil {
    private static final Logger LOG = Logger.getInstance(AuthTestUtil.class);
    
    /**
     * Test authentication with username and password.
     *
     * @param serverUrl Server URL
     * @param username Username
     * @param password Password
     * @return Authentication result (true if successful, false otherwise)
     */
    public static boolean testAuthentication(String serverUrl, String username, String password) {
        try {
            // Create connection to login API
            URL url = new URL(serverUrl + "/api/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Create request body
            String requestBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
            
            // Send request
            connection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Authentication successful
                LOG.info("Authentication successful for user: " + username);
                return true;
            } else {
                // Authentication failed
                LOG.info("Authentication failed for user: " + username + ", response code: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            LOG.error("Error testing authentication", e);
            return false;
        }
    }
    
    /**
     * Verify authentication with token.
     *
     * @param serverUrl Server URL
     * @param token Authentication token
     * @return Authentication result (true if successful, false otherwise)
     */
    public static boolean verifyAuthentication(String serverUrl, String token) {
        try {
            // Create connection to user API
            URL url = new URL(serverUrl + "/api/user");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
                    String response = scanner.useDelimiter("\\A").next();
                    
                    // Parse response
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(response);
                    
                    // Check if response contains user ID and username
                    if (json.containsKey("id") && json.containsKey("username")) {
                        LOG.info("Authentication verified for token");
                        return true;
                    } else {
                        LOG.info("Authentication failed: invalid response: " + response);
                        return false;
                    }
                } catch (ParseException e) {
                    LOG.error("Error parsing response", e);
                    return false;
                }
            } else {
                // Authentication failed
                LOG.info("Authentication failed for token, response code: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            LOG.error("Error verifying authentication", e);
            return false;
        }
    }
    
    /**
     * Login and get access token.
     *
     * @param serverUrl Server URL
     * @param username Username
     * @param password Password
     * @return Access token, or null if login failed
     */
    public static String getAccessToken(String serverUrl, String username, String password) {
        try {
            // Create connection to login API
            URL url = new URL(serverUrl + "/api/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Create request body
            String requestBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
            
            // Send request
            connection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
                    String response = scanner.useDelimiter("\\A").next();
                    
                    // Parse response
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(response);
                    
                    // Extract access token
                    if (json.containsKey("token")) {
                        String token = (String) json.get("token");
                        LOG.info("Got access token for user: " + username);
                        return token;
                    } else if (json.containsKey("accessToken")) {
                        String token = (String) json.get("accessToken");
                        LOG.info("Got access token for user: " + username);
                        return token;
                    } else {
                        LOG.info("Login successful but no token in response: " + response);
                        return null;
                    }
                } catch (ParseException e) {
                    LOG.error("Error parsing response", e);
                    return null;
                }
            } else {
                // Login failed
                LOG.info("Login failed for user: " + username + ", response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            LOG.error("Error getting access token", e);
            return null;
        }
    }
}