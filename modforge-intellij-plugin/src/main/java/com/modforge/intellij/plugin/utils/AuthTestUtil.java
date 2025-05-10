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
 * Utility for authentication testing.
 */
public class AuthTestUtil {
    private static final Logger LOG = Logger.getInstance(AuthTestUtil.class);
    
    /**
     * Test basic connection to the ModForge server.
     * @param serverUrl URL of the ModForge server
     * @return Whether the connection was successful
     */
    public static boolean testConnection(String serverUrl) {
        return ConnectionTestUtil.testConnection(serverUrl);
    }
    
    /**
     * Get access token using username and password.
     * @param serverUrl URL of the ModForge server
     * @param username Username
     * @param password Password
     * @return Access token, or null if authentication failed
     */
    public static String getAccessToken(String serverUrl, String username, String password) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String loginUrl = serverUrl + "api/login";
            
            LOG.info("Authenticating with username to " + loginUrl);
            
            URL url = new URL(loginUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Create login JSON
            JSONObject loginJson = new JSONObject();
            loginJson.put("username", username);
            loginJson.put("password", password);
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = loginJson.toJSONString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200 && responseCode != 201) {
                LOG.error("Authentication failed with response code " + responseCode);
                return null;
            }
            
            // Get user data from response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            // Parse user data
            JSONParser parser = new JSONParser();
            JSONObject userData = (JSONObject) parser.parse(response.toString());
            
            // Get token
            if (userData.containsKey("token")) {
                return (String) userData.get("token");
            } else {
                LOG.error("Response doesn't contain token");
                return null;
            }
        } catch (IOException | ParseException e) {
            LOG.error("Authentication failed", e);
            return null;
        }
    }
    
    /**
     * Verify authentication with token.
     * @param serverUrl URL of the ModForge server
     * @param token Access token
     * @return Whether the token is valid
     */
    public static boolean verifyAuthentication(String serverUrl, String token) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String verifyUrl = serverUrl + "api/auth/verify";
            
            LOG.info("Verifying token at " + verifyUrl);
            
            URL url = new URL(verifyUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            int responseCode = connection.getResponseCode();
            
            return responseCode == 200;
        } catch (IOException e) {
            LOG.error("Token verification failed", e);
            return false;
        }
    }
    
    /**
     * Get authentication status with token.
     * @param serverUrl URL of the ModForge server
     * @param token Access token
     * @return Authentication status as JSON string, or null if failed
     */
    public static String getAuthenticationStatus(String serverUrl, String token) {
        try {
            // Normalize URL
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            // Add API endpoint
            String meUrl = serverUrl + "api/auth/me";
            
            URL url = new URL(meUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                return null;
            }
            
            // Read response
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream())
                .useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            LOG.error("Authentication status check failed", e);
            return null;
        }
    }
}