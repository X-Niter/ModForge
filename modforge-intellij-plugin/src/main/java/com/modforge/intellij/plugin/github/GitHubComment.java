package com.modforge.intellij.plugin.github;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a GitHub comment.
 */
public class GitHubComment {
    private final int id;
    private final String body;
    
    /**
     * Create a new GitHub comment.
     *
     * @param id   The comment ID
     * @param body The comment body
     */
    public GitHubComment(int id, @NotNull String body) {
        this.id = id;
        this.body = body;
    }
    
    /**
     * Get the comment ID.
     *
     * @return The comment ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the comment body.
     *
     * @return The comment body
     */
    @NotNull
    public String getBody() {
        return body;
    }
}