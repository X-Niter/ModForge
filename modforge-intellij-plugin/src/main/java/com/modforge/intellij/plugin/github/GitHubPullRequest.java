package com.modforge.intellij.plugin.github;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a GitHub pull request.
 */
public class GitHubPullRequest {
    private final int number;
    private final String title;
    private final String body;
    private final List<String> labels;
    
    /**
     * Create a new GitHub pull request.
     *
     * @param number The pull request number
     * @param title  The pull request title
     * @param body   The pull request body
     * @param labels The pull request labels
     */
    public GitHubPullRequest(int number, @NotNull String title, @NotNull String body, @NotNull List<String> labels) {
        this.number = number;
        this.title = title;
        this.body = body;
        this.labels = labels;
    }
    
    /**
     * Get the pull request number.
     *
     * @return The pull request number
     */
    public int getNumber() {
        return number;
    }
    
    /**
     * Get the pull request title.
     *
     * @return The pull request title
     */
    @NotNull
    public String getTitle() {
        return title;
    }
    
    /**
     * Get the pull request body.
     *
     * @return The pull request body
     */
    @NotNull
    public String getBody() {
        return body;
    }
    
    /**
     * Get the pull request labels.
     *
     * @return The pull request labels
     */
    @NotNull
    public List<String> getLabels() {
        return labels;
    }
}