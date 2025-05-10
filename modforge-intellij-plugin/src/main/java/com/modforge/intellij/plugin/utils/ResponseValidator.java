package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for validating API responses.
 */
public class ResponseValidator {
    private static final Logger LOG = Logger.getInstance(ResponseValidator.class);
    private static final Gson GSON = new Gson();
    
    /**
     * Validate that the response is valid JSON.
     *
     * @param response The response string
     * @return True if the response is valid JSON, false otherwise
     */
    public static boolean isValidJson(@Nullable String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }
        
        try {
            JsonParser.parseString(response);
            return true;
        } catch (JsonSyntaxException e) {
            LOG.warn("Invalid JSON response: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse a JSON response into a JsonObject.
     *
     * @param response The response string
     * @return The parsed JsonObject, or null if the response is not valid JSON
     */
    @Nullable
    public static JsonObject parseJsonObject(@Nullable String response) {
        if (!isValidJson(response)) {
            return null;
        }
        
        try {
            JsonElement element = JsonParser.parseString(response);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            } else {
                LOG.warn("Response is not a JSON object");
                return null;
            }
        } catch (Exception e) {
            LOG.warn("Error parsing JSON response", e);
            return null;
        }
    }
    
    /**
     * Check if a JSON response has a specific property.
     *
     * @param response The response string
     * @param property The property to check for
     * @return True if the response has the property, false otherwise
     */
    public static boolean hasProperty(@Nullable String response, @NotNull String property) {
        JsonObject jsonObject = parseJsonObject(response);
        return jsonObject != null && jsonObject.has(property);
    }
    
    /**
     * Get a string property from a JSON response.
     *
     * @param response The response string
     * @param property The property to get
     * @return The property value, or null if the property does not exist or is not a string
     */
    @Nullable
    public static String getStringProperty(@Nullable String response, @NotNull String property) {
        JsonObject jsonObject = parseJsonObject(response);
        if (jsonObject != null && jsonObject.has(property) && jsonObject.get(property).isJsonPrimitive()) {
            try {
                return jsonObject.get(property).getAsString();
            } catch (Exception e) {
                LOG.warn("Error getting string property: " + property, e);
            }
        }
        return null;
    }
    
    /**
     * Get a boolean property from a JSON response.
     *
     * @param response The response string
     * @param property The property to get
     * @param defaultValue The default value to return if the property does not exist or is not a boolean
     * @return The property value, or the default value if the property does not exist or is not a boolean
     */
    public static boolean getBooleanProperty(@Nullable String response, @NotNull String property, boolean defaultValue) {
        JsonObject jsonObject = parseJsonObject(response);
        if (jsonObject != null && jsonObject.has(property) && jsonObject.get(property).isJsonPrimitive()) {
            try {
                return jsonObject.get(property).getAsBoolean();
            } catch (Exception e) {
                LOG.warn("Error getting boolean property: " + property, e);
            }
        }
        return defaultValue;
    }
    
    /**
     * Check if a JSON response indicates an error.
     *
     * @param response The response string
     * @return True if the response indicates an error, false otherwise
     */
    public static boolean isErrorResponse(@Nullable String response) {
        JsonObject jsonObject = parseJsonObject(response);
        
        // Common error indicators
        return jsonObject != null && (
            jsonObject.has("error") ||
            jsonObject.has("errorMessage") ||
            jsonObject.has("message") && getBooleanProperty(response, "success", true) == false ||
            getStringProperty(response, "status") != null && getStringProperty(response, "status").equalsIgnoreCase("error")
        );
    }
    
    /**
     * Get the error message from a JSON response.
     *
     * @param response The response string
     * @return The error message, or null if the response does not indicate an error
     */
    @Nullable
    public static String getErrorMessage(@Nullable String response) {
        if (!isErrorResponse(response)) {
            return null;
        }
        
        JsonObject jsonObject = parseJsonObject(response);
        if (jsonObject == null) {
            return null;
        }
        
        // Check common error properties
        String errorMessage = null;
        
        if (jsonObject.has("error") && jsonObject.get("error").isJsonPrimitive()) {
            errorMessage = jsonObject.get("error").getAsString();
        } else if (jsonObject.has("error") && jsonObject.get("error").isJsonObject()) {
            JsonObject errorObject = jsonObject.getAsJsonObject("error");
            if (errorObject.has("message")) {
                errorMessage = errorObject.get("message").getAsString();
            }
        } else if (jsonObject.has("errorMessage")) {
            errorMessage = jsonObject.get("errorMessage").getAsString();
        } else if (jsonObject.has("message")) {
            errorMessage = jsonObject.get("message").getAsString();
        }
        
        return errorMessage;
    }
}