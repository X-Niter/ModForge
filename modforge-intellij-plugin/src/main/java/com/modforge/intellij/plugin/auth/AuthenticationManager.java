package com.modforge.intellij.plugin.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ApiRequestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing authentication with the ModForge server.
 */
@Service(Service.Level.APP)
public final class AuthenticationManager {
    private static final Logger LOG = Logger.getInstance(AuthenticationManager.class);
    private static final Gson GSON = new Gson();
    
    private boolean isAuthenticating = false;
    
    /**
     * Gets the instance of this service.
     * @return The service instance
     */
    public static AuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(AuthenticationManager.class);
    }
    
    /**
     * Authenticates with the ModForge server using the credentials in the settings.
     * @return Whether authentication was successful
     */
    public boolean authenticate() {
        // Prevent multiple authentication attempts at the same time
        if (isAuthenticating) {
            return false;
        }
        
        try {
            isAuthenticating = true;
            
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String username = settings.getUsername();
            String password = settings.getPassword();
            
            if (username.isEmpty() || password.isEmpty()) {
                LOG.warn("Cannot authenticate: username or password is empty");
                settings.setAuthenticated(false);
                return false;
            }
            
            // Create login request
            Map<String, String> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("password", password);
            
            String loginUrl = settings.getServerUrl() + "/api/login";
            
            // Make login request
            String responseStr = ApiRequestUtil.post(loginUrl, GSON.toJson(loginData));
            
            if (responseStr == null) {
                LOG.warn("Failed to authenticate: null response");
                settings.setAuthenticated(false);
                return false;
            }
            
            // Check authentication response
            return processAuthResponse(responseStr, settings);
        } catch (Exception e) {
            LOG.error("Error during authentication", e);
            ModForgeSettings.getInstance().setAuthenticated(false);
            return false;
        } finally {
            isAuthenticating = false;
        }
    }
    
    /**
     * Verifies the current authentication status with the server.
     * @return Whether the current authentication is valid
     */
    public boolean verifyAuthentication() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated()) {
            return false;
        }
        
        String url = settings.getServerUrl() + "/api/auth/me";
        String responseStr = ApiRequestUtil.get(url);
        
        if (responseStr == null) {
            settings.setAuthenticated(false);
            return false;
        }
        
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseStr).getAsJsonObject();
            boolean success = jsonResponse.get("success").getAsBoolean();
            
            settings.setAuthenticated(success);
            return success;
        } catch (Exception e) {
            LOG.error("Error parsing authentication verification response", e);
            settings.setAuthenticated(false);
            return false;
        }
    }
    
    /**
     * Logs out from the ModForge server.
     * @return Whether logout was successful
     */
    public boolean logout() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isAuthenticated()) {
            return true;
        }
        
        String url = settings.getServerUrl() + "/api/logout";
        
        try {
            ApiRequestUtil.post(url, "{}");
            
            // Reset authentication status
            settings.setSessionToken("");
            settings.setAuthenticated(false);
            
            return true;
        } catch (Exception e) {
            LOG.error("Error during logout", e);
            return false;
        }
    }
    
    /**
     * Process the authentication response from the server.
     * @param responseStr The response from the server
     * @param settings The settings to update
     * @return Whether authentication was successful
     */
    private boolean processAuthResponse(@NotNull String responseStr, @NotNull ModForgeSettings settings) {
        try {
            // Parse response
            JsonObject userObject = JsonParser.parseString(responseStr).getAsJsonObject();
            
            // Check if response contains user data
            if (userObject.has("id") && userObject.has("username")) {
                settings.setAuthenticated(true);
                LOG.info("Successfully authenticated as " + userObject.get("username").getAsString());
                return true;
            } else {
                LOG.warn("Failed to authenticate: invalid response format");
                settings.setAuthenticated(false);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error parsing authentication response", e);
            settings.setAuthenticated(false);
            return false;
        }
    }
}