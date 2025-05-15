package com.modforge.intellij.plugin.github;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a GitHub push operation.
 */
public class PushResult {
    private final boolean success;
    private final String message;
    private final String repositoryUrl;
    
    /**
     * Creates a successful push result.
     * 
     * @param message The success message
     * @param repositoryUrl The repository URL
     * @return The push result
     */
    public static PushResult success(@NotNull String message, @NotNull String repositoryUrl) {
        return new PushResult(true, message, repositoryUrl);
    }
    
    /**
     * Creates a failed push result.
     * 
     * @param message The error message
     * @return The push result
     */
    public static PushResult failure(@NotNull String message) {
        return new PushResult(false, message, null);
    }
    
    /**
     * Creates a new push result.
     * 
     * @param success Whether the push was successful
     * @param message The message
     * @param repositoryUrl The repository URL, or null if the push failed
     */
    PushResult(boolean success, @NotNull String message, @Nullable String repositoryUrl) {
        this.success = success;
        this.message = message;
        this.repositoryUrl = repositoryUrl;
    }
    
    /**
     * Gets whether the push was successful.
     * 
     * @return Whether the push was successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Gets the message.
     * 
     * @return The message
     */
    @NotNull
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the repository URL.
     * 
     * @return The repository URL, or null if the push failed
     */
    @Nullable
    public String getRepositoryUrl() {
        return repositoryUrl;
    }
}