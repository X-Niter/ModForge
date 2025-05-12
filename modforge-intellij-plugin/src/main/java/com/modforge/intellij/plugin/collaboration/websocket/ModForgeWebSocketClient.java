package com.modforge.intellij.plugin.collaboration.websocket;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket client for ModForge real-time collaboration.
 * Uses Java 21 HttpClient with WebSocket support for compatibility with IntelliJ IDEA 2025.1.1.1
 */
public class ModForgeWebSocketClient {
    private static final Logger LOG = Logger.getInstance(ModForgeWebSocketClient.class);
    
    private final Project project;
    private final String sessionId;
    private final String username;
    private final boolean isHost;
    private WebSocket webSocket;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final WebSocketMessageHandler messageHandler = new WebSocketMessageHandler();

    /**
     * Constructor for the client.
     *
     * @param project The project.
     * @param sessionId The session ID.
     * @param username The username.
     * @param isHost Whether this client is the host.
     */
    public ModForgeWebSocketClient(@NotNull Project project, @NotNull String sessionId, @NotNull String username, boolean isHost) {
        this.project = project;
        this.sessionId = sessionId;
        this.username = username;
        this.isHost = isHost;
    }

    /**
     * Connects to the WebSocket server.
     *
     * @return True if connected successfully, false otherwise.
     */
    public boolean connect() {
        try {
            String serverUrl = ModForgeSettings.getInstance().getServerUrl();
            URI uri = new URI(serverUrl.replace("http", "ws") + "/ws/collaboration/" + sessionId);
            
            LOG.info("Connecting to WebSocket server: " + uri);
            
            // Create the WebSocket client using Java 21 HttpClient 
            HttpClient client = HttpClient.newHttpClient();
            
            CompletableFuture<WebSocket> webSocketFuture = client.newWebSocketBuilder()
                .header("Username", username)
                .header("Session-ID", sessionId)
                .header("Is-Host", String.valueOf(isHost))
                .buildAsync(uri, new WebSocketListener());
            
            // Wait for the connection to complete
            webSocket = webSocketFuture.join();
            connected.set(true);
            
            // Send initial connection message
            sendMessage(new WebSocketMessage(
                WebSocketMessageType.CONNECT, 
                username, 
                "Joined session"
            ));
            
            return true;
        } catch (Exception e) {
            LOG.error("Error connecting to WebSocket server", e);
            return false;
        }
    }

    /**
     * Closes the WebSocket connection.
     */
    public void close() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnected");
            } catch (Exception e) {
                LOG.error("Error closing WebSocket", e);
            } finally {
                connected.set(false);
            }
        }
    }

    /**
     * Checks if the WebSocket is connected.
     *
     * @return True if connected, false otherwise.
     */
    public boolean isConnected() {
        return connected.get() && webSocket != null;
    }

    /**
     * Sends a message to the WebSocket server.
     *
     * @param message The message to send.
     */
    public void sendMessage(com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage message) {
        if (!isConnected()) {
            LOG.warn("Cannot send message, not connected");
            return;
        }
        
        try {
            String json = message.toJson();
            webSocket.sendText(json, true);
        } catch (Exception e) {
            LOG.error("Error sending message", e);
        }
    }

    /**
     * WebSocket listener implementation using Java 21 WebSocket.Listener.
     */
    private class WebSocketListener implements WebSocket.Listener {
        private final StringBuilder messageBuffer = new StringBuilder();
        
        @Override
        public void onOpen(WebSocket webSocket) {
            LOG.info("WebSocket connection opened");
            WebSocket.Listener.super.onOpen(webSocket);
        }
        
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            
            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                
                try {
                    com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage parsedMessage = com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage.fromJson(message);
                    messageHandler.handleMessage(parsedMessage, project);
                } catch (Exception e) {
                    LOG.error("Error handling message", e);
                }
            }
            
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
        
        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // Not implemented for this client
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOG.info("WebSocket connection closed: " + statusCode + " - " + reason);
            connected.set(false);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
        
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOG.error("WebSocket error", error);
            connected.set(false);
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}