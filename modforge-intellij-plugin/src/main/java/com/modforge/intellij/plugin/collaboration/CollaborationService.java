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
                this.isConnected = true;
                
                // Add self as a participant
                Participant self = new Participant(userId, username, true);
                participants.add(self);
                
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
                this.isConnected = true;
                
                // Add self as a participant
                Participant self = new Participant(userId, username, false);
                participants.add(self);
                
                // TODO: Connect to session host and get participant list
                
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
                
                // If we're the host, terminate the session
                if (isHost) {
                    // Notify all participants that the session is ending
                    for (Participant participant : participants) {
                        if (!participant.userId.equals(userId)) {
                            // TODO: Notify participant that the session is ending
                        }
                    }
                } else {
                    // Notify the host that we're leaving
                    // TODO: Notify host that we're leaving
                }
                
                // Clear session data
                clearSessionData();
                
                // Notify listeners
                notifySessionLeft(sessionId);
                
                LOG.info("Left collaboration session: " + sessionId);
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
        // TODO: Send message to all participants
        LOG.info("Broadcasting message: " + type);
        
        // For now, just log the message
        for (Participant participant : participants) {
            if (!participant.userId.equals(userId)) {
                LOG.info("  To: " + participant.username);
            }
        }
    }
    
    /**
     * Sends a direct message to a specific participant.
     * @param recipientUserId The recipient user ID
     * @param type The message type
     * @param data The message data
     */
    public void sendDirectMessage(@NotNull String recipientUserId, @NotNull String type, @NotNull Map<String, Object> data) {
        // Find the recipient
        Optional<Participant> recipient = participants.stream()
                .filter(p -> p.userId.equals(recipientUserId))
                .findFirst();
        
        if (recipient.isPresent()) {
            // TODO: Send message to the recipient
            LOG.info("Sending direct message to " + recipient.get().username + ": " + type);
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