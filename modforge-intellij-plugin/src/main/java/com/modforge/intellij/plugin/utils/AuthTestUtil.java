package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for testing authentication.
 */
public class AuthTestUtil {
    private static final Logger LOG = Logger.getInstance(AuthTestUtil.class);
    
    /**
     * Test the complete authentication flow.
     * 
     * @param username Username to test
     * @param password Password to test
     * @return Results of the test as a multi-line string
     */
    public static String testCompleteAuthFlow(String username, String password) {
        List<String> results = new ArrayList<>();
        
        results.add("=== Complete Authentication Flow Test ===");
        results.add("");
        
        // Step 1: Test server connectivity
        results.add("Step 1: Testing server connectivity...");
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        if (serverUrl == null || serverUrl.isEmpty()) {
            results.add("ERROR: Server URL is not configured in settings.");
            return String.join("\n", results);
        }
        
        results.add("Server URL: " + serverUrl);
        
        boolean connectionResult = ConnectionTestUtil.testConnection(serverUrl);
        if (connectionResult) {
            results.add("✅ Server connection successful");
        } else {
            results.add("❌ Server connection failed");
            results.add("");
            results.add("Test aborted due to server connection failure.");
            return String.join("\n", results);
        }
        
        results.add("");
        
        // Step 2: Test username/password authentication
        results.add("Step 2: Testing username/password authentication...");
        results.add("Username: " + username);
        results.add("Password: " + "********");
        
        AuthenticationManager authManager = new AuthenticationManager();
        authManager.setCredentials(username, password);
        
        boolean authResult = authManager.authenticate();
        if (authResult) {
            results.add("✅ Authentication successful");
            results.add("🔑 Got access token: " + 
                    (settings.getAccessToken().isEmpty() ? "NONE" : settings.getAccessToken().substring(0, 10) + "..."));
        } else {
            results.add("❌ Authentication failed");
            results.add("");
            results.add("Test aborted due to authentication failure.");
            return String.join("\n", results);
        }
        
        results.add("");
        
        // Step 3: Test token verification
        results.add("Step 3: Testing token verification...");
        
        boolean verifyResult = TokenAuthConnectionUtil.testTokenAuthentication();
        if (verifyResult) {
            results.add("✅ Token verification successful");
        } else {
            results.add("❌ Token verification failed");
            results.add("");
            results.add("Test aborted due to token verification failure.");
            return String.join("\n", results);
        }
        
        results.add("");
        
        // Step 4: Test specific authenticated endpoints
        results.add("Step 4: Testing authenticated API calls...");
        
        String userResponse = TokenAuthConnectionUtil.makeAuthenticatedGetRequest("/api/user");
        if (userResponse != null) {
            results.add("✅ User API call successful");
            results.add("   Response: " + (userResponse.length() > 100 ? userResponse.substring(0, 100) + "..." : userResponse));
        } else {
            results.add("❌ User API call failed");
        }
        
        String meResponse = TokenAuthConnectionUtil.makeAuthenticatedGetRequest("/api/auth/me");
        if (meResponse != null) {
            results.add("✅ Auth/me API call successful");
            results.add("   Response: " + (meResponse.length() > 100 ? meResponse.substring(0, 100) + "..." : meResponse));
        } else {
            results.add("❌ Auth/me API call failed");
        }
        
        results.add("");
        
        // Step 5: Test logout
        results.add("Step 5: Testing logout...");
        
        boolean logoutResult = authManager.logout();
        if (logoutResult) {
            results.add("✅ Logout successful");
        } else {
            results.add("❌ Logout failed");
        }
        
        results.add("");
        results.add("=== Test Complete ===");
        
        LOG.info("Completed authentication flow test with " + (authResult && verifyResult ? "SUCCESS" : "FAILURES"));
        
        return String.join("\n", results);
    }
    
    /**
     * Test token-based authentication.
     * 
     * @return Results of the test as a multi-line string
     */
    public static String testTokenAuthentication() {
        List<String> results = new ArrayList<>();
        
        results.add("=== Token Authentication Test ===");
        results.add("");
        
        // Step 1: Check if we have a token
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String token = settings.getAccessToken();
        
        if (token == null || token.isEmpty()) {
            results.add("❌ No access token available. Please log in first.");
            return String.join("\n", results);
        }
        
        results.add("Token: " + token.substring(0, Math.min(10, token.length())) + "...");
        results.add("");
        
        // Step 2: Test token verification
        results.add("Step 2: Testing token verification...");
        
        boolean verifyResult = TokenAuthConnectionUtil.testTokenAuthentication();
        if (verifyResult) {
            results.add("✅ Token verification successful");
        } else {
            results.add("❌ Token verification failed");
            results.add("");
            results.add("Test aborted due to token verification failure.");
            return String.join("\n", results);
        }
        
        results.add("");
        
        // Step 3: Test specific authenticated endpoints
        results.add("Step 3: Testing authenticated API calls...");
        
        String userResponse = TokenAuthConnectionUtil.makeAuthenticatedGetRequest("/api/user");
        if (userResponse != null) {
            results.add("✅ User API call successful");
            results.add("   Response: " + (userResponse.length() > 100 ? userResponse.substring(0, 100) + "..." : userResponse));
        } else {
            results.add("❌ User API call failed");
        }
        
        String meResponse = TokenAuthConnectionUtil.makeAuthenticatedGetRequest("/api/auth/me");
        if (meResponse != null) {
            results.add("✅ Auth/me API call successful");
            results.add("   Response: " + (meResponse.length() > 100 ? meResponse.substring(0, 100) + "..." : meResponse));
        } else {
            results.add("❌ Auth/me API call failed");
        }
        
        results.add("");
        results.add("=== Test Complete ===");
        
        LOG.info("Completed token authentication test with " + (verifyResult ? "SUCCESS" : "FAILURES"));
        
        return String.join("\n", results);
    }
}