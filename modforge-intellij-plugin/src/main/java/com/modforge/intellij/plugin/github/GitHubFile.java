package com.modforge.intellij.plugin.github;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a file changed in a GitHub pull request.
 */
public class GitHubFile {
    private final String filename;
    private final String status;
    private final int additions;
    private final int deletions;
    private final int changes;
    
    /**
     * Create a new GitHub file.
     *
     * @param filename  The file name
     * @param status    The file status (added, modified, removed)
     * @param additions The number of additions
     * @param deletions The number of deletions
     * @param changes   The number of changes
     */
    public GitHubFile(@NotNull String filename, @NotNull String status, int additions, int deletions, int changes) {
        this.filename = filename;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
    }
    
    /**
     * Get the file name.
     *
     * @return The file name
     */
    @NotNull
    public String getFilename() {
        return filename;
    }
    
    /**
     * Get the file status.
     *
     * @return The file status
     */
    @NotNull
    public String getStatus() {
        return status;
    }
    
    /**
     * Get the number of additions.
     *
     * @return The number of additions
     */
    public int getAdditions() {
        return additions;
    }
    
    /**
     * Get the number of deletions.
     *
     * @return The number of deletions
     */
    public int getDeletions() {
        return deletions;
    }
    
    /**
     * Get the number of changes.
     *
     * @return The number of changes
     */
    public int getChanges() {
        return changes;
    }
}