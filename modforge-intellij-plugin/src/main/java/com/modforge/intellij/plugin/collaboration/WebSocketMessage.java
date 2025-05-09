package com.modforge.intellij.plugin.collaboration;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Message for WebSocket communication.
 */
public class WebSocketMessage {
    private static final Logger LOG = Logger.getInstance(WebSocketMessage.class);
    private static final Gson GSON = new Gson();
    
    // Message types
    public static final String TYPE_PING = "ping";
    public static final String TYPE_PONG = "pong";
    public static final String TYPE_JOIN = "join";
    public static final String TYPE_LEAVE = "leave";
    public static final String TYPE_OPERATION = "operation";
    public static final String TYPE_FILE_CONTENT = "file_content";
    public static final String TYPE_FILE_SYNC = "file_sync";
    public static final String TYPE_ERROR = "error";
    
    private final String type;
    private final String userId;
    private final Map<String, Object> data;
    
    /**
     * Creates a new WebSocketMessage.
     * @param type The message type
     * @param userId The user ID
     * @param data The message data
     */
    public WebSocketMessage(@NotNull String type, @NotNull String userId, @Nullable Map<String, Object> data) {
        this.type = type;
        this.userId = userId;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }
    
    /**
     * Gets the message type.
     * @return The message type
     */
    @NotNull
    public String getType() {
        return type;
    }
    
    /**
     * Gets the user ID.
     * @return The user ID
     */
    @NotNull
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the message data.
     * @return The message data
     */
    @NotNull
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Converts the message to JSON.
     * @return The JSON representation of the message
     */
    @NotNull
    public String toJson() {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("userId", userId);
        message.put("data", data);
        
        return GSON.toJson(message);
    }
    
    /**
     * Parses a WebSocketMessage from JSON.
     * @param json The JSON to parse
     * @return The WebSocketMessage, or null if parsing failed
     */
    @Nullable
    public static WebSocketMessage fromJson(@NotNull String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = GSON.fromJson(json, Map.class);
            
            String type = (String) message.get("type");
            String userId = (String) message.get("userId");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            
            return new WebSocketMessage(type, userId, data);
        } catch (Exception e) {
            LOG.error("Error parsing WebSocketMessage from JSON", e);
            return null;
        }
    }
}