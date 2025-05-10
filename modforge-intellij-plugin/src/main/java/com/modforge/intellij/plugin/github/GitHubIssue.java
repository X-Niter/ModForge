package com.modforge.intellij.plugin.github;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a GitHub issue.
 */
public class GitHubIssue {
    private final int number;
    private final String title;
    private final String body;
    private final List<String> labels;
    
    /**
     * Create a new GitHub issue.
     *
     * @param number The issue number
     * @param title  The issue title
     * @param body   The issue body
     * @param labels The issue labels
     */
    public GitHubIssue(int number, @NotNull String title, @NotNull String body, @NotNull List<String> labels) {
        this.number = number;
        this.title = title;
        this.body = body;
        this.labels = labels;
    }
    
    /**
     * Get the issue number.
     *
     * @return The issue number
     */
    public int getNumber() {
        return number;
    }
    
    /**
     * Get the issue title.
     *
     * @return The issue title
     */
    @NotNull
    public String getTitle() {
        return title;
    }
    
    /**
     * Get the issue body.
     *
     * @return The issue body
     */
    @NotNull
    public String getBody() {
        return body;
    }
    
    /**
     * Get the issue labels.
     *
     * @return The issue labels
     */
    @NotNull
    public List<String> getLabels() {
        return labels;
    }
}