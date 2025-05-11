package com.modforge.intellij.plugin.collaboration.websocket;

/**
 * Enum for WebSocket message types.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public enum WebSocketMessageType {
    /**
     * Message sent when a user connects to a session.
     */
    CONNECT,
    
    /**
     * Message sent when a user disconnects from a session.
     */
    DISCONNECT,
    
    /**
     * Message containing a chat message.
     */
    CHAT,
    
    /**
     * Message containing code changes.
     */
    CODE_CHANGE,
    
    /**
     * Message containing cursor movement.
     */
    CURSOR_MOVE,
    
    /**
     * Message containing file selection.
     */
    FILE_SELECT,
    
    /**
     * Message to trigger a code generation.
     */
    GENERATE_CODE,
    
    /**
     * Message to trigger a code analysis.
     */
    ANALYZE_CODE,
    
    /**
     * Message to trigger a code review.
     */
    REVIEW_CODE,
    
    /**
     * Message containing error information.
     */
    ERROR,
    
    /**
     * Message containing system information.
     */
    SYSTEM,
    
    /**
     * Message containing synchronization information.
     */
    SYNC
}