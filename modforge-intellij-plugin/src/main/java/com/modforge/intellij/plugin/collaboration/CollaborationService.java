package com.modforge.intellij.plugin.collaboration;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing real-time collaboration with other users.
 */
@Service(Service.Level.PROJECT)
public final class CollaborationService {
    private static final Logger LOG = Logger.getInstance(CollaborationService.class);
    
    private final Project project;
    private String userId;
    private String sessionId;
    private WebSocketClient webSocketClient;
    private boolean connected = false;
    private ScheduledFuture<?> pingTask;
    
    // Maps file paths to editors
    private final Map<String, CollaborativeEditor> editors = new ConcurrentHashMap<>();
    
    // Maps file paths to collaboration status
    private final Map<String, Boolean> collaboratedFiles = new ConcurrentHashMap<>();
    
    // Currently active file
    private VirtualFile activeFile;
    
    /**
     * Creates a new CollaborationService.
     * @param project The project
     */
    public CollaborationService(@NotNull Project project) {
        this.project = project;
        this.userId = UUID.randomUUID().toString(); // Generate a random user ID
        
        LOG.info("CollaborationService initialized for project: " + project.getName());
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
     * Gets the user ID.
     * @return The user ID
     */
    @NotNull
    public String getUserId() {
        return userId;
    }
    
    /**
     * Sets the user ID.
     * @param userId The user ID
     */
    public void setUserId(@NotNull String userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the session ID.
     * @return The session ID
     */
    @Nullable
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Sets the active file.
     * @param file The active file
     */
    public void setActiveFile(@Nullable VirtualFile file) {
        this.activeFile = file;
    }
    
    /**
     * Starts a collaboration session.
     * @param sessionId The session ID
     */
    public void startSession(@NotNull String sessionId) {
        if (connected) {
            LOG.warn("Already connected to a session");
            return;
        }
        
        this.sessionId = sessionId;
        
        try {
            connectToServer();
        } catch (Exception e) {
            LOG.error("Error connecting to server", e);
            
            Notification notification = new Notification(
                    "ModForge Notifications",
                    "Collaboration Error",
                    "Error connecting to collaboration server: " + e.getMessage(),
                    NotificationType.ERROR
            );
            
            notification.addAction(new NotificationAction("Try Again") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    try {
                        connectToServer();
                        notification.expire();
                    } catch (Exception ex) {
                        LOG.error("Error connecting to server", ex);
                        
                        Notification errorNotification = new Notification(
                                "ModForge Notifications",
                                "Collaboration Error",
                                "Error connecting to collaboration server: " + ex.getMessage(),
                                NotificationType.ERROR
                        );
                        
                        Notifications.Bus.notify(errorNotification, project);
                    }
                }
            });
            
            Notifications.Bus.notify(notification, project);
        }
    }
    
    /**
     * Connects to the WebSocket server.
     * @throws URISyntaxException If the server URL is invalid
     */
    private void connectToServer() throws URISyntaxException {
        if (connected) {
            LOG.warn("Already connected to server");
            return;
        }
        
        String serverUrl = ModForgeSettings.getInstance().getCollaborationServerUrl();
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalStateException("Collaboration server URL is not set");
        }
        
        URI serverUri = new URI(serverUrl);
        webSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                LOG.info("Connected to collaboration server");
                connected = true;
                
                // Send join message
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", sessionId);
                data.put("username", ModForgeSettings.getInstance().getUsername());
                
                sendMessage(WebSocketMessage.TYPE_JOIN, data);
                
                // Start ping task
                startPingTask();
                
                // Notify user
                Notification notification = new Notification(
                        "ModForge Notifications",
                        "Collaboration Started",
                        "Connected to collaboration session: " + sessionId,
                        NotificationType.INFORMATION
                );
                
                Notifications.Bus.notify(notification, project);
            }
            
            @Override
            public void onMessage(String message) {
                WebSocketMessage wsMessage = WebSocketMessage.fromJson(message);
                if (wsMessage == null) {
                    LOG.warn("Received invalid message from server");
                    return;
                }
                
                handleMessage(wsMessage);
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                LOG.info("Disconnected from collaboration server: " + reason);
                connected = false;
                
                // Stop ping task
                stopPingTask();
                
                // Clear collaboration state
                synchronized (collaboratedFiles) {
                    collaboratedFiles.clear();
                }
                
                synchronized (editors) {
                    editors.clear();
                }
                
                // Notify user
                if (remote) {
                    Notification notification = new Notification(
                            "ModForge Notifications",
                            "Collaboration Ended",
                            "Disconnected from collaboration session: " + reason,
                            NotificationType.INFORMATION
                    );
                    
                    notification.addAction(new NotificationAction("Reconnect") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            try {
                                connectToServer();
                                notification.expire();
                            } catch (Exception ex) {
                                LOG.error("Error reconnecting to server", ex);
                                
                                Notification errorNotification = new Notification(
                                        "ModForge Notifications",
                                        "Collaboration Error",
                                        "Error reconnecting to collaboration server: " + ex.getMessage(),
                                        NotificationType.ERROR
                                );
                                
                                Notifications.Bus.notify(errorNotification, project);
                            }
                        }
                    });
                    
                    Notifications.Bus.notify(notification, project);
                }
            }
            
            @Override
            public void onError(Exception ex) {
                LOG.error("WebSocket error", ex);
                
                // Notify user
                Notification notification = new Notification(
                        "ModForge Notifications",
                        "Collaboration Error",
                        "WebSocket error: " + ex.getMessage(),
                        NotificationType.ERROR
                );
                
                Notifications.Bus.notify(notification, project);
            }
        };
        
        webSocketClient.connect();
    }
    
    /**
     * Handles a WebSocket message.
     * @param message The message to handle
     */
    private void handleMessage(@NotNull WebSocketMessage message) {
        LOG.debug("Received message: " + message.getType() + " from " + message.getUserId());
        
        switch (message.getType()) {
            case WebSocketMessage.TYPE_PING:
                // Respond with pong
                sendMessage(WebSocketMessage.TYPE_PONG, null);
                break;
                
            case WebSocketMessage.TYPE_JOIN:
                // User joined session
                handleJoinMessage(message);
                break;
                
            case WebSocketMessage.TYPE_LEAVE:
                // User left session
                handleLeaveMessage(message);
                break;
                
            case WebSocketMessage.TYPE_OPERATION:
                // User performed an operation
                handleOperationMessage(message);
                break;
                
            case WebSocketMessage.TYPE_FILE_CONTENT:
                // User sent file content
                handleFileContentMessage(message);
                break;
                
            case WebSocketMessage.TYPE_FILE_SYNC:
                // User requested file sync
                handleFileSyncMessage(message);
                break;
                
            case WebSocketMessage.TYPE_ERROR:
                // Error message from server
                handleErrorMessage(message);
                break;
                
            default:
                LOG.warn("Received unknown message type: " + message.getType());
                break;
        }
    }
    
    /**
     * Handles a join message.
     * @param message The message to handle
     */
    private void handleJoinMessage(@NotNull WebSocketMessage message) {
        String username = (String) message.getData().get("username");
        
        LOG.info("User joined session: " + username + " (" + message.getUserId() + ")");
        
        // Notify user
        Notification notification = new Notification(
                "ModForge Notifications",
                "User Joined",
                username + " joined the collaboration session",
                NotificationType.INFORMATION
        );
        
        Notifications.Bus.notify(notification, project);
        
        // Send currently active file
        if (activeFile != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("filePath", activeFile.getPath());
            data.put("fileName", activeFile.getName());
            
            sendMessage(WebSocketMessage.TYPE_FILE_SYNC, data);
        }
    }
    
    /**
     * Handles a leave message.
     * @param message The message to handle
     */
    private void handleLeaveMessage(@NotNull WebSocketMessage message) {
        String username = (String) message.getData().get("username");
        
        LOG.info("User left session: " + username + " (" + message.getUserId() + ")");
        
        // Notify user
        Notification notification = new Notification(
                "ModForge Notifications",
                "User Left",
                username + " left the collaboration session",
                NotificationType.INFORMATION
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Handles an operation message.
     * @param message The message to handle
     */
    private void handleOperationMessage(@NotNull WebSocketMessage message) {
        String filePath = (String) message.getData().get("filePath");
        if (filePath == null) {
            LOG.warn("Received operation message without file path");
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> operationMap = (Map<String, Object>) message.getData().get("operation");
        if (operationMap == null) {
            LOG.warn("Received operation message without operation");
            return;
        }
        
        // Get the collaborative editor for the file
        CollaborativeEditor editor = editors.get(filePath);
        if (editor == null) {
            LOG.warn("Received operation for unregistered file: " + filePath);
            return;
        }
        
        // Convert operation and apply it
        EditorOperation operation = EditorOperation.fromMap(operationMap);
        editor.applyOperation(operation, message.getUserId());
    }
    
    /**
     * Handles a file content message.
     * @param message The message to handle
     */
    private void handleFileContentMessage(@NotNull WebSocketMessage message) {
        String filePath = (String) message.getData().get("filePath");
        if (filePath == null) {
            LOG.warn("Received file content message without file path");
            return;
        }
        
        String content = (String) message.getData().get("content");
        if (content == null) {
            LOG.warn("Received file content message without content");
            return;
        }
        
        LOG.info("Received file content for: " + filePath);
        
        // Mark file as being collaborated on
        synchronized (collaboratedFiles) {
            collaboratedFiles.put(filePath, true);
        }
        
        // Get the virtual file
        VirtualFile file = findFileByPath(filePath);
        if (file == null) {
            LOG.warn("Could not find file: " + filePath);
            return;
        }
        
        // Open the file if it's not already open
        if (!FileEditorManager.getInstance(project).isFileOpen(file)) {
            FileEditorManager.getInstance(project).openFile(file, true);
        }
        
        // Register editor for collaboration
        registerEditor(file);
        
        // Get the collaborative editor
        CollaborativeEditor editor = editors.get(filePath);
        if (editor == null) {
            LOG.warn("Could not get collaborative editor for file: " + filePath);
            return;
        }
        
        // TODO: Update file content
    }
    
    /**
     * Handles a file sync message.
     * @param message The message to handle
     */
    private void handleFileSyncMessage(@NotNull WebSocketMessage message) {
        String filePath = (String) message.getData().get("filePath");
        if (filePath == null) {
            LOG.warn("Received file sync message without file path");
            return;
        }
        
        String fileName = (String) message.getData().get("fileName");
        if (fileName == null) {
            LOG.warn("Received file sync message without file name");
            return;
        }
        
        LOG.info("Received file sync request for: " + filePath);
        
        // Mark file as being collaborated on
        synchronized (collaboratedFiles) {
            collaboratedFiles.put(filePath, true);
        }
        
        // Get the virtual file
        VirtualFile file = findFileByPath(filePath);
        if (file == null) {
            LOG.warn("Could not find file: " + filePath);
            return;
        }
        
        // If file is open, send its content
        if (FileEditorManager.getInstance(project).isFileOpen(file)) {
            CollaborativeEditor editor = editors.get(filePath);
            if (editor != null) {
                String content = editor.getDocument().getText();
                
                Map<String, Object> data = new HashMap<>();
                data.put("filePath", filePath);
                data.put("fileName", fileName);
                data.put("content", content);
                
                sendMessage(WebSocketMessage.TYPE_FILE_CONTENT, data);
            }
        }
    }
    
    /**
     * Handles an error message.
     * @param message The message to handle
     */
    private void handleErrorMessage(@NotNull WebSocketMessage message) {
        String errorMessage = (String) message.getData().get("message");
        if (errorMessage == null) {
            LOG.warn("Received error message without error message");
            return;
        }
        
        LOG.error("Received error from server: " + errorMessage);
        
        // Notify user
        Notification notification = new Notification(
                "ModForge Notifications",
                "Collaboration Error",
                "Server error: " + errorMessage,
                NotificationType.ERROR
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Finds a file by path.
     * @param path The path to find
     * @return The file, or null if not found
     */
    @Nullable
    private VirtualFile findFileByPath(@NotNull String path) {
        return com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(path);
    }
    
    /**
     * Starts the ping task.
     */
    private void startPingTask() {
        if (pingTask != null && !pingTask.isCancelled() && !pingTask.isDone()) {
            LOG.warn("Ping task already running");
            return;
        }
        
        pingTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleAtFixedRate(
                this::sendPing,
                30, 30, TimeUnit.SECONDS
        );
    }
    
    /**
     * Stops the ping task.
     */
    private void stopPingTask() {
        if (pingTask != null && !pingTask.isCancelled() && !pingTask.isDone()) {
            pingTask.cancel(true);
        }
        
        pingTask = null;
    }
    
    /**
     * Sends a ping message.
     */
    private void sendPing() {
        try {
            if (connected) {
                sendMessage(WebSocketMessage.TYPE_PING, null);
            }
        } catch (Exception e) {
            LOG.error("Error sending ping", e);
        }
    }
    
    /**
     * Sends a message to the server.
     * @param type The message type
     * @param data The message data
     */
    public void sendMessage(@NotNull String type, @Nullable Map<String, Object> data) {
        if (!connected) {
            LOG.warn("Not connected to server");
            return;
        }
        
        WebSocketMessage message = new WebSocketMessage(type, userId, data);
        webSocketClient.send(message.toJson());
    }
    
    /**
     * Broadcasts a message to all connected clients.
     * @param type The message type
     * @param data The message data
     */
    public void broadcastMessage(@NotNull String type, @Nullable Map<String, Object> data) {
        sendMessage(type, data);
    }
    
    /**
     * Registers an editor for collaboration.
     * @param file The file to register
     */
    public void registerEditor(@NotNull VirtualFile file) {
        String filePath = file.getPath();
        
        if (editors.containsKey(filePath)) {
            LOG.debug("Editor already registered for file: " + filePath);
            return;
        }
        
        // Create a new collaborative editor
        CollaborativeEditor editor = new CollaborativeEditor(project, file, userId);
        editors.put(filePath, editor);
        
        LOG.info("Registered editor for file: " + filePath);
    }
    
    /**
     * Unregisters an editor for collaboration.
     * @param file The file to unregister
     */
    public void unregisterEditor(@NotNull VirtualFile file) {
        String filePath = file.getPath();
        
        CollaborativeEditor editor = editors.remove(filePath);
        if (editor != null) {
            LOG.info("Unregistered editor for file: " + filePath);
        }
    }
    
    /**
     * Checks if a file is being collaborated on.
     * @param file The file to check
     * @return Whether the file is being collaborated on
     */
    public boolean isFileCollaborated(@NotNull VirtualFile file) {
        String filePath = file.getPath();
        
        synchronized (collaboratedFiles) {
            return collaboratedFiles.getOrDefault(filePath, false);
        }
    }
    
    /**
     * Ends the collaboration session.
     */
    public void endSession() {
        if (!connected) {
            LOG.warn("Not connected to a session");
            return;
        }
        
        try {
            // Send leave message
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("username", ModForgeSettings.getInstance().getUsername());
            
            sendMessage(WebSocketMessage.TYPE_LEAVE, data);
            
            // Close connection
            webSocketClient.close();
            
            // Reset state
            connected = false;
            sessionId = null;
            
            // Stop ping task
            stopPingTask();
            
            // Clear collaboration state
            synchronized (collaboratedFiles) {
                collaboratedFiles.clear();
            }
            
            synchronized (editors) {
                editors.clear();
            }
            
            LOG.info("Ended collaboration session");
            
            // Notify user
            Notification notification = new Notification(
                    "ModForge Notifications",
                    "Collaboration Ended",
                    "Disconnected from collaboration session",
                    NotificationType.INFORMATION
            );
            
            Notifications.Bus.notify(notification, project);
        } catch (Exception e) {
            LOG.error("Error ending collaboration session", e);
            
            Notification notification = new Notification(
                    "ModForge Notifications",
                    "Collaboration Error",
                    "Error ending collaboration session: " + e.getMessage(),
                    NotificationType.ERROR
            );
            
            Notifications.Bus.notify(notification, project);
        }
    }
    
    /**
     * Checks if connected to a collaboration session.
     * @return Whether connected to a collaboration session
     */
    public boolean isConnected() {
        return connected;
    }
}