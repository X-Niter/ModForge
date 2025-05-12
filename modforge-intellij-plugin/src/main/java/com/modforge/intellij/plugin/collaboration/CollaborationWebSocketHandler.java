package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
// Using fully qualified name for websocket.WebSocketMessage to avoid conflicts
import com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Handler for WebSocket messages in the collaboration system.
 */
public class CollaborationWebSocketHandler implements WebSocketMessageListener {
    private static final Logger LOG = Logger.getInstance(CollaborationWebSocketHandler.class);
    
    // The collaboration service
    private final CollaborationService collaborationService;
    
    // Executor for background tasks
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.CollaborationWebSocketHandler", 2);
    
    /**
     * Creates a new CollaborationWebSocketHandler.
     * @param collaborationService The collaboration service
     */
    public CollaborationWebSocketHandler(@NotNull CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }
    
    @Override
    public void onMessageReceived(@NotNull com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage message) {
        executor.submit(() -> {
            try {
                handleMessage(message);
            } catch (Exception e) {
                LOG.error("Error handling WebSocket message: " + message, e);
            }
        });
    }
    
    @Override
    public void onConnected() {
        LOG.info("Connected to WebSocket server");
    }
    
    @Override
    public void onDisconnected() {
        LOG.info("Disconnected from WebSocket server");
    }
    
    @Override
    public void onError(@NotNull String message, @Nullable Exception exception) {
        LOG.error("WebSocket error: " + message, exception);
    }
    
    /**
     * Handles a WebSocket message.
     * @param message The message
     */
    private void handleMessage(@NotNull com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage message) {
        com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessageType type = message.getType();
        // Get data map from message content
        Map<String, Object> data = new HashMap<>();
        // Parse JSON content if needed
        String content = message.getContent();
        
        switch (type) {
            case CONNECT:
                handleJoinMessage(data);
                break;
            case DISCONNECT:
                handleLeaveMessage(data);
                break;
            case CODE_CHANGE:
                handleOperationMessage(data);
                break;
            case SYSTEM:
                // Check content for participant_joined or participant_left
                if (content.contains("participant_joined")) {
                    handleParticipantJoinedMessage(data);
                } else if (content.contains("participant_left")) {
                    handleParticipantLeftMessage(data);
                }
                break;
            case ERROR:
                handleErrorMessage(data);
                break;
            default:
                LOG.warn("Unknown WebSocket message type: " + type);
                break;
        }
    }
    
    /**
     * Handles a join message.
     * @param data The message data
     */
    private void handleJoinMessage(@NotNull Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        String userId = (String) data.get("userId");
        String username = (String) data.get("username");
        
        if (sessionId == null || userId == null || username == null) {
            LOG.warn("Invalid join message: " + data);
            return;
        }
        
        LOG.info("User joined session: " + username + " (" + userId + ")");
        
        // Add participant
        Participant participant = new Participant(userId, username, false);
        collaborationService.addParticipant(participant);
    }
    
    /**
     * Handles a leave message.
     * @param data The message data
     */
    private void handleLeaveMessage(@NotNull Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        String userId = (String) data.get("userId");
        
        if (sessionId == null || userId == null) {
            LOG.warn("Invalid leave message: " + data);
            return;
        }
        
        LOG.info("User left session: " + userId);
        
        // Remove participant
        collaborationService.removeParticipant(userId);
    }
    
    /**
     * Handles an operation message.
     * @param data The message data
     */
    private void handleOperationMessage(@NotNull Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        String userId = (String) data.get("userId");
        Map<String, Object> operationData = (Map<String, Object>) data.get("operation");
        
        if (sessionId == null || userId == null || operationData == null) {
            LOG.warn("Invalid operation message: " + data);
            return;
        }
        
        LOG.info("Received operation from user: " + userId);
        
        // Get operation details
        String filePath = (String) operationData.get("filePath");
        String type = (String) operationData.get("type");
        
        if (filePath == null || type == null) {
            LOG.warn("Invalid operation data: " + operationData);
            return;
        }
        
        // Create operation
        EditorOperation operation = null;
        
        switch (type) {
            case EditorOperation.TYPE_INSERT:
                operation = createInsertOperation(operationData);
                break;
            case EditorOperation.TYPE_DELETE:
                operation = createDeleteOperation(operationData);
                break;
            case EditorOperation.TYPE_REPLACE:
                operation = createReplaceOperation(operationData);
                break;
            default:
                LOG.warn("Unknown operation type: " + type);
                break;
        }
        
        if (operation != null) {
            // Apply operation
            collaborationService.applyOperation(filePath, operation, userId);
        }
    }
    
    /**
     * Creates an insert operation from operation data.
     * @param operationData The operation data
     * @return The insert operation
     */
    @Nullable
    private EditorOperation createInsertOperation(@NotNull Map<String, Object> operationData) {
        Integer offset = (Integer) operationData.get("offset");
        String text = (String) operationData.get("text");
        
        if (offset == null || text == null) {
            LOG.warn("Invalid insert operation data: " + operationData);
            return null;
        }
        
        return EditorOperation.createInsertOperation(offset, text);
    }
    
    /**
     * Creates a delete operation from operation data.
     * @param operationData The operation data
     * @return The delete operation
     */
    @Nullable
    private EditorOperation createDeleteOperation(@NotNull Map<String, Object> operationData) {
        Integer offset = (Integer) operationData.get("offset");
        Integer length = (Integer) operationData.get("length");
        
        if (offset == null || length == null) {
            LOG.warn("Invalid delete operation data: " + operationData);
            return null;
        }
        
        return EditorOperation.createDeleteOperation(offset, length);
    }
    
    /**
     * Creates a replace operation from operation data.
     * @param operationData The operation data
     * @return The replace operation
     */
    @Nullable
    private EditorOperation createReplaceOperation(@NotNull Map<String, Object> operationData) {
        Integer offset = (Integer) operationData.get("offset");
        Integer length = (Integer) operationData.get("length");
        String text = (String) operationData.get("text");
        
        if (offset == null || length == null || text == null) {
            LOG.warn("Invalid replace operation data: " + operationData);
            return null;
        }
        
        return EditorOperation.createReplaceOperation(offset, length, text);
    }
    
    /**
     * Handles a participant joined message.
     * @param data The message data
     */
    private void handleParticipantJoinedMessage(@NotNull Map<String, Object> data) {
        String userId = (String) data.get("userId");
        String username = (String) data.get("username");
        Boolean isHost = (Boolean) data.get("isHost");
        
        if (userId == null || username == null || isHost == null) {
            LOG.warn("Invalid participant joined message: " + data);
            return;
        }
        
        LOG.info("Participant joined: " + username + " (" + userId + ")");
        
        // Add participant
        Participant participant = new Participant(userId, username, isHost);
        collaborationService.addParticipant(participant);
    }
    
    /**
     * Handles a participant left message.
     * @param data The message data
     */
    private void handleParticipantLeftMessage(@NotNull Map<String, Object> data) {
        String userId = (String) data.get("userId");
        
        if (userId == null) {
            LOG.warn("Invalid participant left message: " + data);
            return;
        }
        
        LOG.info("Participant left: " + userId);
        
        // Remove participant
        collaborationService.removeParticipant(userId);
    }
    
    /**
     * Handles an error message.
     * @param data The message data
     */
    private void handleErrorMessage(@NotNull Map<String, Object> data) {
        String errorMessage = (String) data.get("message");
        
        if (errorMessage == null) {
            LOG.warn("Invalid error message: " + data);
            return;
        }
        
        LOG.error("WebSocket error: " + errorMessage);
    }
}