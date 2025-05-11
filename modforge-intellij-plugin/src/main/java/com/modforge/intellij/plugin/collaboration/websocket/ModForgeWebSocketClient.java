package com.modforge.intellij.plugin.collaboration.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client for real-time collaboration.
 */
public class ModForgeWebSocketClient {
    private static final Logger LOG = Logger.getInstance(ModForgeWebSocketClient.class);
    
    // WebSocket connection
    private InternalWebSocketClient client;
    
    // Gson for JSON serialization/deserialization
    private final Gson gson = new GsonBuilder().create();
    
    // Listeners
    private final CopyOnWriteArrayList<WebSocketMessageListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Creates a new ModForgeWebSocketClient.
     */
    public ModForgeWebSocketClient() {
    }
    
    /**
     * Connects to a WebSocket server.
     * @param serverUrl The server URL
     * @param sessionId The session ID
     * @param userId The user ID
     * @param username The username
     * @return Whether the connection was successful
     */
    public boolean connect(@NotNull String serverUrl, @NotNull String sessionId, @NotNull String userId, @NotNull String username) {
        try {
            // Create WebSocket URI
            URI uri = new URI(serverUrl + "?sessionId=" + sessionId + "&userId=" + userId + "&username=" + username);
            
            // Create and connect WebSocket client
            client = new InternalWebSocketClient(uri);
            client.connectBlocking(5, TimeUnit.SECONDS);
            
            LOG.info("Connected to WebSocket server: " + serverUrl);
            return client.isOpen();
        } catch (URISyntaxException e) {
            LOG.error("Invalid WebSocket server URL: " + serverUrl, e);
            return false;
        } catch (InterruptedException e) {
            LOG.error("WebSocket connection interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            LOG.error("Error connecting to WebSocket server", e);
            return false;
        }
    }
    
    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        if (client != null && client.isOpen()) {
            try {
                client.closeBlocking();
                LOG.info("Disconnected from WebSocket server");
            } catch (InterruptedException e) {
                LOG.error("WebSocket disconnection interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Sends a message to the WebSocket server.
     * @param type The message type
     * @param data The message data
     * @return Whether the message was sent
     */
    public boolean sendMessage(@NotNull String type, @NotNull Map<String, Object> data) {
        if (client == null || !client.isOpen()) {
            LOG.warn("Cannot send message: WebSocket not connected");
            return false;
        }
        
        try {
            // Create message
            WebSocketMessage message = new WebSocketMessage(type, data);
            
            // Serialize message to JSON
            String json = gson.toJson(message);
            
            // Send message
            client.send(json);
            return true;
        } catch (Exception e) {
            LOG.error("Error sending WebSocket message", e);
            return false;
        }
    }
    
    /**
     * Adds a WebSocket message listener.
     * @param listener The listener
     */
    public void addListener(@NotNull WebSocketMessageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a WebSocket message listener.
     * @param listener The listener
     */
    public void removeListener(@NotNull WebSocketMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies listeners of a received message.
     * @param message The message
     */
    private void notifyMessageReceived(@NotNull WebSocketMessage message) {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                LOG.error("Error notifying WebSocket message listener", e);
            }
        }
    }
    
    /**
     * Notifies listeners of a WebSocket connection.
     */
    private void notifyConnected() {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onConnected();
            } catch (Exception e) {
                LOG.error("Error notifying WebSocket connection listener", e);
            }
        }
    }
    
    /**
     * Notifies listeners of a WebSocket disconnection.
     */
    private void notifyDisconnected() {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onDisconnected();
            } catch (Exception e) {
                LOG.error("Error notifying WebSocket disconnection listener", e);
            }
        }
    }
    
    /**
     * Notifies listeners of a WebSocket error.
     * @param message The error message
     * @param exception The exception
     */
    private void notifyError(@NotNull String message, @Nullable Exception exception) {
        for (WebSocketMessageListener listener : listeners) {
            try {
                listener.onError(message, exception);
            } catch (Exception e) {
                LOG.error("Error notifying WebSocket error listener", e);
            }
        }
    }
    
    /**
     * Internal WebSocket client implementation.
     */
    private class InternalWebSocketClient extends org.java_websocket.client.WebSocketClient {
        /**
         * Creates a new InternalWebSocketClient.
         * @param serverUri The server URI
         */
        InternalWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            LOG.info("WebSocket connection opened");
            notifyConnected();
        }
        
        @Override
        public void onMessage(String message) {
            try {
                // Parse message from JSON
                WebSocketMessage webSocketMessage = gson.fromJson(message, WebSocketMessage.class);
                
                // Notify listeners
                notifyMessageReceived(webSocketMessage);
            } catch (Exception e) {
                LOG.error("Error parsing WebSocket message: " + message, e);
                notifyError("Error parsing WebSocket message", e);
            }
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            LOG.info("WebSocket connection closed: " + reason);
            notifyDisconnected();
        }
        
        @Override
        public void onError(Exception ex) {
            LOG.error("WebSocket error", ex);
            notifyError("WebSocket error", ex);
        }
    }
}