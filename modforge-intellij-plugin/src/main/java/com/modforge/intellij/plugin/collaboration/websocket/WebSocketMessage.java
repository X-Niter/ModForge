package com.modforge.intellij.plugin.collaboration.websocket;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Message for WebSocket communication.
 */
public class WebSocketMessage {
    // Common message types
    public static final String TYPE_JOIN = "join";
    public static final String TYPE_LEAVE = "leave";
    public static final String TYPE_OPERATION = "operation";
    public static final String TYPE_PARTICIPANT_JOINED = "participant_joined";
    public static final String TYPE_PARTICIPANT_LEFT = "participant_left";
    public static final String TYPE_ERROR = "error";
    
    /** The message type. */
    @NotNull
    private final String type;
    
    /** The message data. */
    @NotNull
    private final Map<String, Object> data;
    
    /** The timestamp when the message was created. */
    private final long timestamp;
    
    /**
     * Creates a new WebSocketMessage.
     * @param type The message type
     * @param data The message data
     */
    public WebSocketMessage(@NotNull String type, @NotNull Map<String, Object> data) {
        this.type = type;
        this.data = new HashMap<>(data);
        this.timestamp = System.currentTimeMillis();
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
     * Gets the message data.
     * @return The message data
     */
    @NotNull
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Gets the timestamp.
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Creates a join message.
     * @param sessionId The session ID
     * @param userId The user ID
     * @param username The username
     * @return The message
     */
    @NotNull
    public static WebSocketMessage createJoinMessage(
            @NotNull String sessionId,
            @NotNull String userId,
            @NotNull String username
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("userId", userId);
        data.put("username", username);
        
        return new WebSocketMessage(TYPE_JOIN, data);
    }
    
    /**
     * Creates a leave message.
     * @param sessionId The session ID
     * @param userId The user ID
     * @return The message
     */
    @NotNull
    public static WebSocketMessage createLeaveMessage(
            @NotNull String sessionId,
            @NotNull String userId
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("userId", userId);
        
        return new WebSocketMessage(TYPE_LEAVE, data);
    }
    
    /**
     * Creates an operation message.
     * @param sessionId The session ID
     * @param userId The user ID
     * @param operationData The operation data
     * @return The message
     */
    @NotNull
    public static WebSocketMessage createOperationMessage(
            @NotNull String sessionId,
            @NotNull String userId,
            @NotNull Map<String, Object> operationData
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("userId", userId);
        data.put("operation", operationData);
        
        return new WebSocketMessage(TYPE_OPERATION, data);
    }
    
    /**
     * Creates an error message.
     * @param errorMessage The error message
     * @return The message
     */
    @NotNull
    public static WebSocketMessage createErrorMessage(@NotNull String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", errorMessage);
        
        return new WebSocketMessage(TYPE_ERROR, data);
    }
    
    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "type='" + type + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}