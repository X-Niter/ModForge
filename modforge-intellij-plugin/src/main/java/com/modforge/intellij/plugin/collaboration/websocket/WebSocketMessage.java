package com.modforge.intellij.plugin.collaboration.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Message model for WebSocket communication.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class WebSocketMessage {
    private static final Gson GSON = new GsonBuilder().create();
    
    private final WebSocketMessageType type;
    private final String sender;
    private final String content;
    private final long timestamp;
    
    /**
     * Constructor for creating a new message.
     *
     * @param type The message type.
     * @param sender The sender's username.
     * @param content The message content.
     */
    public WebSocketMessage(@NotNull WebSocketMessageType type, @NotNull String sender, @NotNull String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the message type.
     *
     * @return The message type.
     */
    public WebSocketMessageType getType() {
        return type;
    }
    
    /**
     * Gets the sender's username.
     *
     * @return The sender.
     */
    public String getSender() {
        return sender;
    }
    
    /**
     * Gets the message content.
     *
     * @return The content.
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Gets the message timestamp.
     *
     * @return The timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Converts the message to JSON.
     *
     * @return The JSON string.
     */
    public String toJson() {
        return GSON.toJson(this);
    }
    
    /**
     * Creates a message from JSON.
     *
     * @param json The JSON string.
     * @return The message.
     */
    public static WebSocketMessage fromJson(String json) {
        return GSON.fromJson(json, WebSocketMessage.class);
    }
}