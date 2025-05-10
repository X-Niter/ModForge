package com.modforge.intellij.plugin.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for ModForge authentication.
 */
@Service
@State(
        name = "com.modforge.intellij.plugin.auth.ModAuthenticationManager",
        storages = {@Storage("ModForgeAuth.xml")}
)
public final class ModAuthenticationManager implements PersistentStateComponent<ModAuthenticationManager> {
    private static final Logger LOG = Logger.getInstance(ModAuthenticationManager.class);
    private static final Gson GSON = new Gson();
    private static final Type USER_MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    
    private String token = "";
    private Map<String, Object> user = new HashMap<>();
    private boolean authenticated = false;
    
    /**
     * Get the instance of ModAuthenticationManager.
     *
     * @return The instance
     */
    public static ModAuthenticationManager getInstance() {
        return ApplicationManager.getApplication().getService(ModAuthenticationManager.class);
    }
    
    /**
     * Login to ModForge with username and password.
     *
     * @param username The username
     * @param password The password
     * @return A CompletableFuture that will be resolved to true if the login is successful, false otherwise
     */
    public CompletableFuture<Boolean> login(String username, String password) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String loginUrl = serverUrl.endsWith("/") ? serverUrl + "login" : serverUrl + "/login";
        
        return TokenAuthConnectionUtil.executeLogin(loginUrl, username, password)
                .thenApply(response -> {
                    if (response != null) {
                        Object tokenObj = response.get("token");
                        if (tokenObj != null) {
                            token = tokenObj.toString();
                            user = response;
                            user.remove("token"); // Don't store token in user info
                            authenticated = true;
                            return true;
                        }
                    }
                    
                    LOG.error("Login failed: response did not contain token");
                    return false;
                })
                .exceptionally(e -> {
                    LOG.error("Error during login", e);
                    return false;
                });
    }
    
    /**
     * Login to ModForge with a token.
     *
     * @param authToken The authentication token
     * @return A CompletableFuture that will be resolved to true if the login is successful, false otherwise
     */
    public CompletableFuture<Boolean> loginWithToken(String authToken) {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        String userUrl = serverUrl.endsWith("/") ? serverUrl + "user" : serverUrl + "/user";
        
        return TokenAuthConnectionUtil.executeGet(userUrl, authToken)
                .thenApply(response -> {
                    if (response != null && !response.isEmpty()) {
                        try {
                            Map<String, Object> userMap = GSON.fromJson(response, USER_MAP_TYPE);
                            if (userMap != null && userMap.containsKey("id")) {
                                token = authToken;
                                user = userMap;
                                authenticated = true;
                                return true;
                            }
                        } catch (Exception e) {
                            LOG.error("Error parsing user data", e);
                        }
                    }
                    
                    LOG.error("Login with token failed: could not retrieve user information");
                    return false;
                })
                .exceptionally(e -> {
                    LOG.error("Error during login with token", e);
                    return false;
                });
    }
    
    /**
     * Logout from ModForge.
     */
    public void logout() {
        token = "";
        user = new HashMap<>();
        authenticated = false;
    }
    
    /**
     * Verify if the current token is valid.
     *
     * @return A CompletableFuture that will be resolved to true if the token is valid, false otherwise
     */
    public CompletableFuture<Boolean> verifyToken() {
        if (token.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String serverUrl = settings.getServerUrl();
        
        return ConnectionTestUtil.testAuthentication(serverUrl, token)
                .thenApply(valid -> {
                    if (!valid) {
                        // Token is invalid, logout
                        logout();
                    }
                    
                    return valid;
                })
                .exceptionally(e -> {
                    LOG.error("Error verifying token", e);
                    return false;
                });
    }
    
    /**
     * Check if the user is authenticated.
     *
     * @return True if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return authenticated && !token.isEmpty();
    }
    
    /**
     * Get the authentication token.
     *
     * @return The token
     */
    @NotNull
    public String getToken() {
        return token;
    }
    
    /**
     * Get the authenticated user information.
     *
     * @return The user information
     */
    @NotNull
    public Map<String, Object> getUser() {
        return new HashMap<>(user);
    }
    
    /**
     * Get the user ID.
     *
     * @return The user ID, or -1 if not authenticated
     */
    public int getUserId() {
        if (!isAuthenticated() || !user.containsKey("id")) {
            return -1;
        }
        
        try {
            Object idObj = user.get("id");
            if (idObj instanceof Number) {
                return ((Number) idObj).intValue();
            } else if (idObj instanceof String) {
                return Integer.parseInt((String) idObj);
            }
        } catch (Exception e) {
            LOG.error("Error getting user ID", e);
        }
        
        return -1;
    }
    
    /**
     * Get the username.
     *
     * @return The username, or an empty string if not authenticated
     */
    @NotNull
    public String getUsername() {
        if (!isAuthenticated() || !user.containsKey("username")) {
            return "";
        }
        
        Object usernameObj = user.get("username");
        return usernameObj != null ? usernameObj.toString() : "";
    }
    
    @Nullable
    @Override
    public ModAuthenticationManager getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull ModAuthenticationManager state) {
        XmlSerializerUtil.copyBean(state, this);
        
        // Verify token on load
        if (authenticated && !token.isEmpty()) {
            verifyToken()
                    .thenAccept(valid -> {
                        if (!valid) {
                            LOG.info("Token verification failed on load, logging out");
                        } else {
                            LOG.info("Token verified on load");
                        }
                    });
        }
    }
}