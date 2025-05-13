package com.modforge.intellij.plugin.models;

import com.modforge.intellij.plugin.model.ModLoaderType;
import java.util.UUID;

/**
 * Represents a learned pattern in the ModForge AI system.
 * Patterns can be for code generation, error resolution, or other AI-powered features.
 */
public class Pattern {
    public enum PatternType {
        CODE_GENERATION,
        ERROR_RESOLUTION,
        FEATURE_SUGGESTION,
        DOCUMENTATION,
        IDEA_EXPANSION
    }
    
    private String id;
    private PatternType type;
    private String context;  // E.g., code snippet, error message, etc.
    private String solution; // E.g., generated code, fix, etc.
    private String description;
    private ModLoaderType modLoader;
    private String minecraftVersion;
    private int successCount;
    private int failureCount;
    private double confidence;
    private long createdTimestamp;
    private long lastModifiedTimestamp;
    private boolean isDirty; // Whether this pattern has been modified locally and needs to be synced
    
    public Pattern() {
        this.id = UUID.randomUUID().toString();
        this.createdTimestamp = System.currentTimeMillis();
        this.lastModifiedTimestamp = createdTimestamp;
        this.successCount = 0;
        this.failureCount = 0;
        this.confidence = 0.5; // Start with neutral confidence
        this.isDirty = true;   // New patterns are dirty by default
    }
    
    public Pattern(PatternType type, String context, String solution, ModLoaderType modLoader, String minecraftVersion) {
        this();
        this.type = type;
        this.context = context;
        this.solution = solution;
        this.modLoader = modLoader;
        this.minecraftVersion = minecraftVersion;
    }
    
    /**
     * Records a successful use of this pattern.
     */
    public void recordSuccess() {
        successCount++;
        updateConfidence();
        markDirty();
    }
    
    /**
     * Records a failed use of this pattern.
     */
    public void recordFailure() {
        failureCount++;
        updateConfidence();
        markDirty();
    }
    
    /**
     * Updates the confidence score based on success and failure counts.
     */
    private void updateConfidence() {
        if (successCount + failureCount == 0) {
            confidence = 0.5; // Default confidence
        } else {
            confidence = (double) successCount / (successCount + failureCount);
        }
    }
    
    /**
     * Marks this pattern as dirty, meaning it needs to be synced with the server.
     */
    public void markDirty() {
        isDirty = true;
        lastModifiedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Marks this pattern as clean, meaning it has been synced with the server.
     */
    public void markClean() {
        isDirty = false;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public PatternType getType() {
        return type;
    }
    
    public void setType(PatternType type) {
        this.type = type;
        markDirty();
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
        markDirty();
    }
    
    public String getSolution() {
        return solution;
    }
    
    public void setSolution(String solution) {
        this.solution = solution;
        markDirty();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        markDirty();
    }
    
    public ModLoaderType getModLoader() {
        return modLoader;
    }
    
    public void setModLoader(ModLoaderType modLoader) {
        this.modLoader = modLoader;
        markDirty();
    }
    
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
        markDirty();
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
        updateConfidence();
        markDirty();
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
        updateConfidence();
        markDirty();
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }
    
    public boolean isDirty() {
        return isDirty;
    }
}