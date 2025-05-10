package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for testing connections to the server.
 */
public class ConnectionTestUtil {
    private static final Logger LOG = Logger.getInstance(ConnectionTestUtil.class);
    
    private ConnectionTestUtil() {
        // Utility class, no instantiation
    }
    
    /**
     * Tests the connection to the server.
     * @param serverUrl The server URL
     * @return Connection status: "OK" if connection is successful, error message otherwise
     */
    @NotNull
    public static String testServerConnection(@NotNull String serverUrl) {
        try {
            String url = serverUrl + "/api/health";
            String response = ApiRequestUtil.get(url);
            
            if (response == null) {
                return "Failed to connect to server. Please check the URL and try again.";
            }
            
            JsonObject healthResponse = JsonParser.parseString(response).getAsJsonObject();
            String status = healthResponse.get("status").getAsString();
            
            if ("healthy".equals(status)) {
                return "OK";
            } else {
                String message = healthResponse.has("message") 
                    ? healthResponse.get("message").getAsString() 
                    : "Unknown error";
                return "Server health check failed: " + message;
            }
        } catch (Exception e) {
            LOG.error("Error testing server connection", e);
            return "Error testing connection: " + e.getMessage();
        }
    }
    
    /**
     * Checks if OpenAI API is available on the server.
     * @param serverUrl The server URL
     * @return OpenAI API status: true if available, false otherwise
     */
    public static boolean isOpenAiApiAvailable(@NotNull String serverUrl) {
        try {
            String url = serverUrl + "/api/health";
            String response = ApiRequestUtil.get(url);
            
            if (response == null) {
                return false;
            }
            
            JsonObject healthResponse = JsonParser.parseString(response).getAsJsonObject();
            
            // Check if AI section exists and has status
            if (healthResponse.has("ai") && healthResponse.getAsJsonObject("ai").has("status")) {
                String aiStatus = healthResponse.getAsJsonObject("ai").get("status").getAsString();
                return "available".equals(aiStatus);
            }
            
            return false;
        } catch (Exception e) {
            LOG.error("Error checking OpenAI API availability", e);
            return false;
        }
    }
}