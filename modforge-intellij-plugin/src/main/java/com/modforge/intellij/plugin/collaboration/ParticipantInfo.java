package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;

/**
 * Information about a collaboration session participant.
 */
public class ParticipantInfo {
    private final String userId;
    private final String username;
    private long lastActiveTimestamp;
    private boolean active;

    /**
     * Creates a new participant info object.
     * 
     * @param userId   The user ID
     * @param username The username
     */
    public ParticipantInfo(@NotNull String userId, @NotNull String username) {
        this.userId = userId;
        this.username = username;
        this.lastActiveTimestamp = System.currentTimeMillis();
        this.active = true;
    }

    /**
     * Gets the user ID.
     * 
     * @return The user ID
     */
    @NotNull
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the username.
     * 
     * @return The username
     */
    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     * Gets the last active timestamp.
     * 
     * @return The last active timestamp in milliseconds
     */
    public long getLastActiveTimestamp() {
        return lastActiveTimestamp;
    }

    /**
     * Sets the last active timestamp to the current time.
     */
    public void updateActivity() {
        this.lastActiveTimestamp = System.currentTimeMillis();
        this.active = true;
    }

    /**
     * Sets the active status.
     * 
     * @param active The active status
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Gets whether the participant is active.
     * 
     * @return True if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Checks if the participant is the host.
     * 
     * @return True if the participant is the host, false otherwise
     */
    public boolean isHost() {
        // Placeholder implementation
        return false;
    }
}