package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

/**
 * Utility for server URL operations.
 */
public class ServerUrlUtil {
    private static final Logger LOG = Logger.getInstance(ServerUrlUtil.class);
    
    /**
     * Gets the configured server URL from settings.
     * 
     * @return The server URL, or null if not configured
     */
    public static String getServerUrl() {
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getServerUrl();
            
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.warn("Server URL is not configured");
                return null;
            }
            
            // Ensure the URL ends with a slash
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            
            return serverUrl;
        } catch (Exception e) {
            LOG.error("Failed to get server URL", e);
            return null;
        }
    }
    
    /**
     * Normalizes a URL to ensure it has a trailing slash.
     * 
     * @param url The URL to normalize
     * @return The normalized URL
     */
    public static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        
        if (!url.endsWith("/")) {
            return url + "/";
        }
        
        return url;
    }
    
    /**
     * Gets the configured collaboration server URL from settings.
     * 
     * @return The collaboration server URL, or null if not configured
     */
    public static String getCollaborationServerUrl() {
        try {
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String serverUrl = settings.getCollaborationServerUrl();
            
            if (serverUrl == null || serverUrl.isEmpty()) {
                LOG.warn("Collaboration server URL is not configured");
                return null;
            }
            
            return serverUrl;
        } catch (Exception e) {
            LOG.error("Failed to get collaboration server URL", e);
            return null;
        }
    }
}