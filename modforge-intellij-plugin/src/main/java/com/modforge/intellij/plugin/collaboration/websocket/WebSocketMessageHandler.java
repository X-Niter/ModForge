package com.modforge.intellij.plugin.collaboration.websocket;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for incoming WebSocket messages.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class WebSocketMessageHandler {
    private static final Logger LOG = Logger.getInstance(WebSocketMessageHandler.class);
    
    /**
     * Handles an incoming WebSocket message.
     *
     * @param message The message to handle.
     * @param project The project.
     */
    public void handleMessage(@NotNull WebSocketMessage message, @NotNull Project project) {
        LOG.info("Handling message of type: " + message.getType() + " from: " + message.getSender());
        
        switch (message.getType()) {
            case CONNECT -> handleConnect(message, project);
            case DISCONNECT -> handleDisconnect(message, project);
            case CHAT -> handleChat(message, project);
            case CODE_CHANGE -> handleCodeChange(message, project);
            case CURSOR_MOVE -> handleCursorMove(message, project);
            case FILE_SELECT -> handleFileSelect(message, project);
            case GENERATE_CODE -> handleGenerateCode(message, project);
            case ANALYZE_CODE -> handleAnalyzeCode(message, project);
            case REVIEW_CODE -> handleReviewCode(message, project);
            case ERROR -> handleError(message, project);
            case SYSTEM -> handleSystem(message, project);
            case SYNC -> handleSync(message, project);
            default -> LOG.warn("Unhandled message type: " + message.getType());
        }
    }
    
    /**
     * Handles a CONNECT message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleConnect(WebSocketMessage message, Project project) {
        showNotification(project, "User Connected", message.getSender() + " joined the session", false);
    }
    
    /**
     * Handles a DISCONNECT message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleDisconnect(WebSocketMessage message, Project project) {
        showNotification(project, "User Disconnected", message.getSender() + " left the session", false);
    }
    
    /**
     * Handles a CHAT message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleChat(WebSocketMessage message, Project project) {
        showNotification(project, "Chat Message", message.getSender() + ": " + message.getContent(), false);
    }
    
    /**
     * Handles a CODE_CHANGE message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleCodeChange(WebSocketMessage message, Project project) {
        // Parse the content and apply the changes
        // This would need to be implemented based on the specific format of code change messages
        LOG.info("Code change received from: " + message.getSender());
    }
    
    /**
     * Handles a CURSOR_MOVE message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleCursorMove(WebSocketMessage message, Project project) {
        // Update the cursor position for the user
        // This would need to be implemented based on the specific format of cursor move messages
        LOG.debug("Cursor move received from: " + message.getSender());
    }
    
    /**
     * Handles a FILE_SELECT message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleFileSelect(WebSocketMessage message, Project project) {
        showNotification(project, "File Selection", message.getSender() + " opened: " + message.getContent(), false);
    }
    
    /**
     * Handles a GENERATE_CODE message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleGenerateCode(WebSocketMessage message, Project project) {
        showNotification(project, "Code Generation", message.getSender() + " requested code generation", true);
    }
    
    /**
     * Handles an ANALYZE_CODE message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleAnalyzeCode(WebSocketMessage message, Project project) {
        showNotification(project, "Code Analysis", message.getSender() + " requested code analysis", true);
    }
    
    /**
     * Handles a REVIEW_CODE message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleReviewCode(WebSocketMessage message, Project project) {
        showNotification(project, "Code Review", message.getSender() + " requested code review", true);
    }
    
    /**
     * Handles an ERROR message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleError(WebSocketMessage message, Project project) {
        showNotification(project, "Error", "Error from " + message.getSender() + ": " + message.getContent(), true);
    }
    
    /**
     * Handles a SYSTEM message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleSystem(WebSocketMessage message, Project project) {
        showNotification(project, "System Message", message.getContent(), false);
    }
    
    /**
     * Handles a SYNC message.
     *
     * @param message The message.
     * @param project The project.
     */
    private void handleSync(WebSocketMessage message, Project project) {
        // Handle synchronization messages
        // This would need to be implemented based on the specific format of sync messages
        LOG.info("Sync message received from: " + message.getSender());
    }
    
    /**
     * Shows a notification for a message.
     *
     * @param project The project.
     * @param title The notification title.
     * @param content The notification content.
     * @param important Whether the notification is important.
     */
    private void showNotification(Project project, String title, String content, boolean important) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            
            if (important) {
                notificationService.showWarningNotification(project, title, content);
            } else {
                notificationService.showInfoNotification(project, title, content);
            }
        });
    }
}