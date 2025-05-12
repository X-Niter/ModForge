package com.modforge.intellij.plugin.collaboration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Message for WebSocket communication.
 */
public class WebSocketMessage {
    // Message types
    public static final String TYPE_PING = "ping";
    public static final String TYPE_PONG = "pong";
    public static final String TYPE_JOIN = "join";
    public static final String TYPE_LEAVE = "leave";
    public static final String TYPE_OPERATION = "operation";
    public static final String TYPE_FILE_CONTENT = "file_content";
    public static final String TYPE_FILE_SYNC = "file_sync";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_PARTICIPANT_JOINED = "participant_joined";
    public static final String TYPE_PARTICIPANT_LEFT = "participant_left";
    
    private final String type;
    private final String userId;
    private final Map<String, Object> data;
    
    private static final Gson GSON = new GsonBuilder().create();
    
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
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("userId", userId);
        map.put("data", data);
        
        return GSON.toJson(map);
    }
    
    /**
     * Creates a message from JSON.
     * @param json The JSON to create the message from
     * @return The message, or null if the JSON is invalid
     */
    @Nullable
    public static WebSocketMessage fromJson(@NotNull String json) {
        try {
            // Use specific type parameter for GSON.fromJson
            @SuppressWarnings("unchecked")
            Map<String, Object> map = GSON.fromJson(json, HashMap.class);
            
            if (map == null) {
                return null;
            }
            
            Object typeObj = map.get("type");
            Object userIdObj = map.get("userId");
            Object dataObj = map.get("data");
            
            // Verify objects are of the expected types
            if (!(typeObj instanceof String) || !(userIdObj instanceof String)) {
                return null;
            }
            
            String type = (String) typeObj;
            String userId = (String) userIdObj;
            
            // Safely handle the data map
            Map<String, Object> data = null;
            if (dataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tempData = (Map<String, Object>) dataObj;
                data = tempData;
            }
            
            return new WebSocketMessage(type, userId, data);
        } catch (Exception e) {
            com.intellij.openapi.diagnostic.Logger.getInstance(WebSocketMessage.class)
                .warn("Failed to parse WebSocket message: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a ping message.
     * @param userId The user ID
     * @return The ping message
     */
    @NotNull
    public static WebSocketMessage createPingMessage(@NotNull String userId) {
        return new WebSocketMessage(TYPE_PING, userId, null);
    }
    
    /**
     * Creates a pong message.
     * @param userId The user ID
     * @return The pong message
     */
    @NotNull
    public static WebSocketMessage createPongMessage(@NotNull String userId) {
        return new WebSocketMessage(TYPE_PONG, userId, null);
    }
    
    /**
     * Creates a join message.
     * @param userId The user ID
     * @param sessionId The session ID
     * @param username The username
     * @return The join message
     */
    @NotNull
    public static WebSocketMessage createJoinMessage(@NotNull String userId, @NotNull String sessionId, 
                                                   @NotNull String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("username", username);
        
        return new WebSocketMessage(TYPE_JOIN, userId, data);
    }
    
    /**
     * Creates a leave message.
     * @param userId The user ID
     * @param sessionId The session ID
     * @param username The username
     * @return The leave message
     */
    @NotNull
    public static WebSocketMessage createLeaveMessage(@NotNull String userId, @NotNull String sessionId, 
                                                    @NotNull String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("username", username);
        
        return new WebSocketMessage(TYPE_LEAVE, userId, data);
    }
    
    /**
     * Creates an operation message.
     * @param userId The user ID
     * @param filePath The file path
     * @param operation The operation
     * @return The operation message
     */
    @NotNull
    public static WebSocketMessage createOperationMessage(@NotNull String userId, @NotNull String filePath, 
                                                        @NotNull EditorOperation operation) {
        Map<String, Object> data = new HashMap<>();
        data.put("filePath", filePath);
        data.put("operation", operation.toMap());
        
        return new WebSocketMessage(TYPE_OPERATION, userId, data);
    }
    
    /**
     * Creates a file content message.
     * @param userId The user ID
     * @param filePath The file path
     * @param fileName The file name
     * @param content The file content
     * @return The file content message
     */
    @NotNull
    public static WebSocketMessage createFileContentMessage(@NotNull String userId, @NotNull String filePath, 
                                                          @NotNull String fileName, @NotNull String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("filePath", filePath);
        data.put("fileName", fileName);
        data.put("content", content);
        
        return new WebSocketMessage(TYPE_FILE_CONTENT, userId, data);
    }
    
    /**
     * Creates a file sync message.
     * @param userId The user ID
     * @param filePath The file path
     * @param fileName The file name
     * @return The file sync message
     */
    @NotNull
    public static WebSocketMessage createFileSyncMessage(@NotNull String userId, @NotNull String filePath, 
                                                       @NotNull String fileName) {
        Map<String, Object> data = new HashMap<>();
        data.put("filePath", filePath);
        data.put("fileName", fileName);
        
        return new WebSocketMessage(TYPE_FILE_SYNC, userId, data);
    }
    
    /**
     * Creates an error message.
     * @param userId The user ID
     * @param message The error message
     * @return The error message
     */
    @NotNull
    public static WebSocketMessage createErrorMessage(@NotNull String userId, @NotNull String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        
        return new WebSocketMessage(TYPE_ERROR, userId, data);
    }
}