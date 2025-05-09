package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;

/**
 * Listener for collaboration events.
 */
public interface CollaborationListener {
    /**
     * Called when a session is started.
     * @param sessionId The session ID
     */
    void onSessionStarted(@NotNull String sessionId);
    
    /**
     * Called when a session is joined.
     * @param sessionId The session ID
     */
    void onSessionJoined(@NotNull String sessionId);
    
    /**
     * Called when a session is left.
     * @param sessionId The session ID
     */
    void onSessionLeft(@NotNull String sessionId);
    
    /**
     * Called when a participant joins the session.
     * @param participant The participant
     */
    void onParticipantJoined(@NotNull Participant participant);
    
    /**
     * Called when a participant leaves the session.
     * @param participant The participant
     */
    void onParticipantLeft(@NotNull Participant participant);
    
    /**
     * Called when an operation is received from another participant.
     * @param operation The operation
     * @param participant The participant who sent the operation
     */
    void onOperationReceived(@NotNull EditorOperation operation, @NotNull Participant participant);
}