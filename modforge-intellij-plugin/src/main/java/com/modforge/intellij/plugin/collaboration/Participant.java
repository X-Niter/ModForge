package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a participant in a collaboration session.
 */
public class Participant {
    /** The participant's user ID. */
    @NotNull
    public final String userId;
    
    /** The participant's username. */
    @NotNull
    public final String username;
    
    /** Whether the participant is the host. */
    public final boolean isHost;
    
    /**
     * Creates a new Participant.
     * @param userId The user ID
     * @param username The username
     * @param isHost Whether the participant is the host
     */
    public Participant(@NotNull String userId, @NotNull String username, boolean isHost) {
        this.userId = userId;
        this.username = username;
        this.isHost = isHost;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return username + (isHost ? " (Host)" : "");
    }
}