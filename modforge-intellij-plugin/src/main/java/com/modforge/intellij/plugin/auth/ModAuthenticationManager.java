package com.modforge.intellij.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.AuthTestUtil;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.json.simple.JSONObject;

/**
 * Manager for authentication with ModForge server.
 */
public class ModAuthenticationManager {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    
    /**
     * Get instance.
     * @return Instance
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }
    
    /**
     * Check if user is authenticated.
     * @return Whether the user is authenticated
     */
    public boolean isAuthenticated() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return settings.isAuthenticated() && !settings.getAccessToken().isEmpty();
    }
    
    /**
     * Verify authentication with token.
     * @return Whether authentication is valid
     */
    public boolean verifyAuthentication() {
        if (!isAuthenticated()) {
            return false;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        return AuthTestUtil.verifyAuthentication(settings.getServerUrl(), settings.getAccessToken());
    }
    
    /**
     * Login with username and password.
     * @param username Username
     * @param password Password
     * @return Whether login was successful
     */
    public boolean login(String username, String password) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = AuthTestUtil.getAccessToken(settings.getServerUrl(), username, password);
        
        if (token == null) {
            return false;
        }
        
        settings.setUsername(username);
        settings.setPassword(password);
        settings.setAccessToken(token);
        settings.setAuthenticated(true);
        
        return true;
    }
    
    /**
     * Logout.
     */
    public void logout() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        if (!settings.isRememberCredentials()) {
            settings.setUsername("");
            settings.setPassword("");
        }
        
        settings.setAccessToken("");
        settings.setAuthenticated(false);
    }
    
    /**
     * Get user data.
     * @return User data as JSONObject, or null if not authenticated
     */
    public JSONObject getUserData() {
        if (!isAuthenticated()) {
            return null;
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        JSONObject response = TokenAuthConnectionUtil.get(settings.getServerUrl(), "/api/user", settings.getAccessToken());
        
        if (response == null) {
            LOG.warn("Failed to get user data");
            return null;
        }
        
        return response;
    }
    
    /**
     * Get user ID.
     * @return User ID, or null if not authenticated
     */
    public String getUserId() {
        JSONObject userData = getUserData();
        
        if (userData == null) {
            return null;
        }
        
        try {
            if (userData.containsKey("id")) {
                Object idObj = userData.get("id");
                return idObj != null ? idObj.toString() : null;
            }
        } catch (Exception e) {
            LOG.error("Error getting user ID", e);
        }
        
        return null;
    }
    
    /**
     * Get username.
     * @return Username, or null if not authenticated
     */
    public String getUsername() {
        JSONObject userData = getUserData();
        
        if (userData == null) {
            return null;
        }
        
        try {
            if (userData.containsKey("username")) {
                Object usernameObj = userData.get("username");
                return usernameObj instanceof String ? (String) usernameObj : null;
            }
        } catch (Exception e) {
            LOG.error("Error getting username", e);
        }
        
        return null;
    }
}