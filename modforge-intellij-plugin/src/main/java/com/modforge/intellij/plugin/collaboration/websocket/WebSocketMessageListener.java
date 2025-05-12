package com.modforge.intellij.plugin.collaboration.websocket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listener for WebSocket messages.
 */
public interface WebSocketMessageListener {
    /**
     * Called when a WebSocket message is received.
     * @param message The message
     */
    void onMessageReceived(@NotNull com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage message);
    
    /**
     * Called when a WebSocket connection is established.
     */
    void onConnected();
    
    /**
     * Called when a WebSocket connection is closed.
     */
    void onDisconnected();
    
    /**
     * Called when a WebSocket error occurs.
     * @param message The error message
     * @param exception The exception
     */
    void onError(@NotNull String message, @Nullable Exception exception);
}