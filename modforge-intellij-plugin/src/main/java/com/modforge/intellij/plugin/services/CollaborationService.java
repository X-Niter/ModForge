package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.collaboration.websocket.ModForgeWebSocketClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for real-time collaboration.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service(Service.Level.PROJECT)
public final class CollaborationService {
    private static final Logger LOG = Logger.getInstance(CollaborationService.class);

    private final Project project;
    private ModForgeWebSocketClient webSocketClient;
    private String currentSessionId;
    private String username;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * Constructor for the service.
     *
     * @param project The project.
     */
    public CollaborationService(Project project) {
        this.project = project;
    }

    /**
     * Checks if the service is connected to a collaboration session.
     *
     * @return True if connected, false otherwise.
     */
    public boolean isConnected() {
        return connected.get() && webSocketClient != null && webSocketClient.isConnected();
    }

    /**
     * Gets the session ID of the current session.
     *
     * @return The session ID, or null if not in a session.
     */
    @Nullable
    public String getSessionId() {
        return isConnected() ? currentSessionId : null;
    }

    /**
     * Starts a new collaboration session.
     *
     * @param username The username to use for the session.
     * @return A CompletableFuture that completes with the session ID.
     */
    public CompletableFuture<String> startSession(@NotNull String username) {
        LOG.info("Starting new collaboration session as: " + username);

        // Clean up any existing session
        cleanupExistingSession();

        // Create a new session ID
        currentSessionId = generateSessionId();
        this.username = username;

        // Connect to the collaboration server
        return connectToServer(currentSessionId, username, true)
                .thenApply(success -> {
                    if (success) {
                        LOG.info("Successfully started session: " + currentSessionId);
                        return currentSessionId;
                    } else {
                        LOG.error("Failed to start session");
                        cleanupExistingSession();
                        throw new RuntimeException("Failed to start collaboration session");
                    }
                });
    }

    /**
     * Joins an existing collaboration session.
     *
     * @param sessionId The ID of the session to join.
     * @param username  The username to use for the session.
     * @return A CompletableFuture that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> joinSession(@NotNull String sessionId, @NotNull String username) {
        LOG.info("Joining session: " + sessionId + " as: " + username);

        // Clean up any existing session
        cleanupExistingSession();

        // Set the session info
        currentSessionId = sessionId;
        this.username = username;

        // Connect to the server
        return connectToServer(sessionId, username, false);
    }

    /**
     * Leaves the current session.
     *
     * @return A CompletableFuture that completes with a boolean indicating success.
     */
    public CompletableFuture<Boolean> leaveSession() {
        LOG.info("Leaving current session");

        if (!isConnected()) {
            LOG.info("Not in a session, nothing to leave");
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (webSocketClient != null) {
                    webSocketClient.close();
                }

                cleanupExistingSession();
                return true;
            } catch (Exception e) {
                LOG.error("Error leaving session", e);
                return false;
            }
        });
    }

    /**
     * Connects to the collaboration server.
     *
     * @param sessionId The session ID.
     * @param username  The username.
     * @param isHost    Whether this client is the host.
     * @return A CompletableFuture that completes with a boolean indicating success.
     */
    private CompletableFuture<Boolean> connectToServer(
            @NotNull String sessionId,
            @NotNull String username,
            boolean isHost) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create a new websocket client
                webSocketClient = new ModForgeWebSocketClient(project, sessionId, username, isHost);

                // Connect to the server
                boolean success = webSocketClient.connect();
                connected.set(success);

                return success;
            } catch (Exception e) {
                LOG.error("Error connecting to collaboration server", e);
                cleanupExistingSession();
                return false;
            }
        });
    }

    /**
     * Cleans up the existing session.
     */
    private void cleanupExistingSession() {
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                LOG.error("Error closing websocket", e);
            } finally {
                webSocketClient = null;
            }
        }

        currentSessionId = null;
        username = null;
        connected.set(false);
    }

    /**
     * Generates a new session ID.
     *
     * @return The session ID.
     */
    private String generateSessionId() {
        // Generate a short, readable session ID
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Gets the current username.
     * 
     * @return The username, or null if not in a session
     */
    @Nullable
    public String getUsername() {
        return isConnected() ? username : null;
    }

    /**
     * Sets the username.
     * 
     * @param username The username
     */
    public void setUsername(@NotNull String username) {
        this.username = username;
    }
}