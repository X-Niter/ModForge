package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.collaboration.websocket.WebSocketClient;
import com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessage;
import com.modforge.intellij.plugin.collaboration.websocket.WebSocketMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service for real-time collaboration features.
 * Allows multiple developers to work on the same mod simultaneously.
 */
@Service(Service.Level.PROJECT)
public final class CollaborationService {
    private static final Logger LOG = Logger.getInstance(CollaborationService.class);
    
    // The project
    private final Project project;
    
    // Executor for background tasks
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.Collaboration", 4);
    
    // WebSocket client
    private final WebSocketClient webSocketClient = new WebSocketClient();
    
    // Session management
    private String sessionId;
    private String userId;
    private String username;
    private boolean isHost;
    private boolean isConnected;
    
    // WebSocket server URL
    private static final String WEBSOCKET_SERVER_URL = "wss://modforge.io/ws/collaboration";
    
    // Maps file paths to their collaborative editors
    private final Map<String, CollaborativeEditor> collaborativeEditors = new ConcurrentHashMap<>();
    
    // List of participants in the session
    private final List<Participant> participants = new CopyOnWriteArrayList<>();
    
    // Listeners
    private final List<CollaborationListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Creates a new CollaborationService.
     * @param project The project
     */
    public CollaborationService(@NotNull Project project) {
        this.project = project;
        
        // Initialize WebSocket handler
        CollaborationWebSocketHandler handler = new CollaborationWebSocketHandler(this);
        webSocketClient.addListener(handler);
    }
    
    /**
     * Adds a participant to the session.
     * @param participant The participant to add
     */
    public void addParticipant(@NotNull Participant participant) {
        // Check if participant already exists
        if (participants.contains(participant)) {
            return;
        }
        
        // Add participant
        participants.add(participant);
        
        // Notify listeners
        notifyParticipantJoined(participant);
    }
    
    /**
     * Removes a participant from the session.
     * @param userId The user ID of the participant to remove
     */
    public void removeParticipant(@NotNull String userId) {
        // Find participant
        Participant participant = participants.stream()
                .filter(p -> p.userId.equals(userId))
                .findFirst()
                .orElse(null);
        
        if (participant != null) {
            // Remove participant
            participants.remove(participant);
            
            // Notify listeners
            notifyParticipantLeft(participant);
        }
    }
    
    /**
     * Gets the CollaborationService instance.
     * @param project The project
     * @return The CollaborationService instance
     */
    public static CollaborationService getInstance(@NotNull Project project) {
        return project.getService(CollaborationService.class);
    }
    
    /**
     * Starts a new collaboration session.
     * @param username The username
     * @return The session ID
     */
    @NotNull
    public CompletableFuture<String> startSession(@NotNull String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.username = username;
                this.userId = UUID.randomUUID().toString();
                this.sessionId = UUID.randomUUID().toString();
                this.isHost = true;
                
                // Connect to WebSocket server
                boolean connected = webSocketClient.connect(
                        WEBSOCKET_SERVER_URL,
                        sessionId,
                        userId,
                        username
                );
                
                if (!connected) {
                    throw new IllegalStateException("Failed to connect to WebSocket server");
                }
                
                this.isConnected = true;
                
                // Add self as a participant
                Participant self = new Participant(userId, username, true);
                participants.add(self);
                
                // Send join message
                Map<String, Object> joinData = new HashMap<>();
                joinData.put("sessionId", sessionId);
                joinData.put("userId", userId);
                joinData.put("username", username);
                joinData.put("isHost", true);
                
                webSocketClient.sendMessage(WebSocketMessage.TYPE_JOIN, joinData);
                
                // Notify listeners
                notifySessionStarted(sessionId);
                
                LOG.info("Started collaboration session: " + sessionId);
                return sessionId;
            } catch (Exception e) {
                LOG.error("Error starting collaboration session", e);
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Joins an existing collaboration session.
     * @param sessionId The session ID
     * @param username The username
     * @return Whether joining was successful
     */
    @NotNull
    public CompletableFuture<Boolean> joinSession(@NotNull String sessionId, @NotNull String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.sessionId = sessionId;
                this.username = username;
                this.userId = UUID.randomUUID().toString();
                this.isHost = false;
                
                // Connect to WebSocket server
                boolean connected = webSocketClient.connect(
                        WEBSOCKET_SERVER_URL,
                        sessionId,
                        userId,
                        username
                );
                
                if (!connected) {
                    throw new IllegalStateException("Failed to connect to WebSocket server");
                }
                
                this.isConnected = true;
                
                // Add self as a participant
                Participant self = new Participant(userId, username, false);
                participants.add(self);
                
                // Send join message to notify other participants
                Map<String, Object> joinData = new HashMap<>();
                joinData.put("sessionId", sessionId);
                joinData.put("userId", userId);
                joinData.put("username", username);
                joinData.put("isHost", false);
                
                webSocketClient.sendMessage(WebSocketMessage.TYPE_JOIN, joinData);
                
                // Notify listeners
                notifySessionJoined(sessionId);
                
                LOG.info("Joined collaboration session: " + sessionId);
                return true;
            } catch (Exception e) {
                LOG.error("Error joining collaboration session", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Leaves the current collaboration session.
     * @return Whether leaving was successful
     */
    @NotNull
    public CompletableFuture<Boolean> leaveSession() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected) {
                    return false;
                }
                
                String sessionIdToLeave = sessionId;
                
                // Send leave message to notify other participants
                Map<String, Object> leaveData = new HashMap<>();
                leaveData.put("sessionId", sessionId);
                leaveData.put("userId", userId);
                
                webSocketClient.sendMessage(WebSocketMessage.TYPE_LEAVE, leaveData);
                
                // Disconnect from WebSocket server
                webSocketClient.disconnect();
                
                // Clear session data
                clearSessionData();
                
                // Notify listeners
                notifySessionLeft(sessionIdToLeave);
                
                LOG.info("Left collaboration session: " + sessionIdToLeave);
                return true;
            } catch (Exception e) {
                LOG.error("Error leaving collaboration session", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Clears session data.
     */
    private void clearSessionData() {
        sessionId = null;
        isConnected = false;
        collaborativeEditors.clear();
        participants.clear();
    }
    
    /**
     * Gets the current collaboration session ID.
     * @return The session ID
     */
    @Nullable
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets whether this client is the host.
     * @return Whether this client is the host
     */
    public boolean isHost() {
        return isHost;
    }
    
    /**
     * Gets whether this client is connected to a session.
     * @return Whether this client is connected
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Gets the current username.
     * @return The username
     */
    @Nullable
    public String getUsername() {
        return username;
    }
    
    /**
     * Gets the current user ID.
     * @return The user ID
     */
    @Nullable
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the list of participants.
     * @return The participants
     */
    @NotNull
    public List<Participant> getParticipants() {
        return new ArrayList<>(participants);
    }
    
    /**
     * Adds a collaboration listener.
     * @param listener The listener
     */
    public void addListener(@NotNull CollaborationListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a collaboration listener.
     * @param listener The listener
     */
    public void removeListener(@NotNull CollaborationListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Gets a collaborative editor for a file.
     * @param file The file
     * @return The collaborative editor
     */
    @NotNull
    public CollaborativeEditor getCollaborativeEditor(@NotNull VirtualFile file) {
        String filePath = file.getPath();
        
        // Create a new collaborative editor if one doesn't exist
        return collaborativeEditors.computeIfAbsent(filePath, path -> {
            CollaborativeEditor editor = new CollaborativeEditor(project, file, userId);
            
            // TODO: Synchronize editor state with other participants
            
            return editor;
        });
    }
    
    /**
     * Applies an operation to a file.
     * @param filePath The file path
     * @param operation The operation
     * @param sourceUserId The user ID that generated the operation
     */
    public void applyOperation(@NotNull String filePath, @NotNull EditorOperation operation, @NotNull String sourceUserId) {
        // Find the collaborative editor
        CollaborativeEditor editor = collaborativeEditors.get(filePath);
        
        if (editor != null) {
            // Apply the operation
            editor.applyOperation(operation, sourceUserId);
        }
    }
    
    /**
     * Broadcasts a message to all participants.
     * @param type The message type
     * @param data The message data
     */
    public void broadcastMessage(@NotNull String type, @NotNull Map<String, Object> data) {
        if (!isConnected) {
            LOG.warn("Cannot broadcast message: Not connected to a session");
            return;
        }
        
        // Add session and user information to the message
        Map<String, Object> messageData = new HashMap<>(data);
        messageData.put("sessionId", sessionId);
        messageData.put("userId", userId);
        
        // Send the message through WebSocket
        boolean sent = webSocketClient.sendMessage(type, messageData);
        
        if (sent) {
            LOG.info("Broadcasted message: " + type);
            
            // Log the recipients
            for (Participant participant : participants) {
                if (!participant.userId.equals(userId)) {
                    LOG.info("  To: " + participant.username);
                }
            }
        } else {
            LOG.error("Failed to broadcast message: " + type);
        }
    }
    
    /**
     * Sends a direct message to a specific participant.
     * @param recipientUserId The recipient user ID
     * @param type The message type
     * @param data The message data
     */
    public void sendDirectMessage(@NotNull String recipientUserId, @NotNull String type, @NotNull Map<String, Object> data) {
        if (!isConnected) {
            LOG.warn("Cannot send direct message: Not connected to a session");
            return;
        }
        
        // Find the recipient
        Optional<Participant> recipient = participants.stream()
                .filter(p -> p.userId.equals(recipientUserId))
                .findFirst();
        
        if (recipient.isPresent()) {
            // Add session, user, and recipient information to the message
            Map<String, Object> messageData = new HashMap<>(data);
            messageData.put("sessionId", sessionId);
            messageData.put("userId", userId);
            messageData.put("recipientId", recipientUserId);
            
            // Send the message through WebSocket
            boolean sent = webSocketClient.sendMessage(type, messageData);
            
            if (sent) {
                LOG.info("Sent direct message to " + recipient.get().username + ": " + type);
            } else {
                LOG.error("Failed to send direct message to " + recipient.get().username + ": " + type);
            }
        } else {
            LOG.warn("Recipient not found: " + recipientUserId);
        }
    }
    
    /**
     * Notifies listeners that a session has started.
     * @param sessionId The session ID
     */
    private void notifySessionStarted(@NotNull String sessionId) {
        for (CollaborationListener listener : listeners) {
            try {
                listener.onSessionStarted(sessionId);
            } catch (Exception e) {
                LOG.error("Error notifying listener of session start", e);
            }
        }
    }
    
    /**
     * Notifies listeners that a session has been joined.
     * @param sessionId The session ID
     */
    private void notifySessionJoined(@NotNull String sessionId) {
        for (CollaborationListener listener : listeners) {
            try {
                listener.onSessionJoined(sessionId);
            } catch (Exception e) {
                LOG.error("Error notifying listener of session join", e);
            }
        }
    }
    
    /**
     * Notifies listeners that a session has been left.
     * @param sessionId The session ID
     */
    private void notifySessionLeft(@NotNull String sessionId) {
        for (CollaborationListener listener : listeners) {
            try {
                listener.onSessionLeft(sessionId);
            } catch (Exception e) {
                LOG.error("Error notifying listener of session leave", e);
            }
        }
    }
    
    /**
     * Notifies listeners that a participant has joined.
     * @param participant The participant
     */
    private void notifyParticipantJoined(@NotNull Participant participant) {
        for (CollaborationListener listener : listeners) {
            try {
                listener.onParticipantJoined(participant);
            } catch (Exception e) {
                LOG.error("Error notifying listener of participant join", e);
            }
        }
    }
    
    /**
     * Notifies listeners that a participant has left.
     * @param participant The participant
     */
    private void notifyParticipantLeft(@NotNull Participant participant) {
        for (CollaborationListener listener : listeners) {
            try {
                listener.onParticipantLeft(participant);
            } catch (Exception e) {
                LOG.error("Error notifying listener of participant leave", e);
            }
        }
    }
    
    /**
     * Disposes the service.
     */
    public void dispose() {
        // Leave the session if connected
        if (isConnected) {
            leaveSession();
        }
        
        // Shutdown the executor
        executor.shutdownNow();
    }
}