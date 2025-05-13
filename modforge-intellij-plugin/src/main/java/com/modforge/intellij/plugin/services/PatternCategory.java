package com.modforge.intellij.plugin.services;

/**
 * Enum for pattern categories used in the services package.
 * These categories help organize and match AI patterns for different types of requests.
 */
public enum PatternCategory {
    /**
     * Code generation patterns.
     */
    CODE_GENERATION,
    
    /**
     * Code fixing patterns.
     */
    CODE_FIXING,
    
    /**
     * Code enhancement patterns.
     */
    CODE_ENHANCEMENT,
    
    /**
     * Documentation generation patterns.
     */
    DOCUMENTATION,
    
    /**
     * Feature addition patterns.
     */
    FEATURE_ADDITION;
    
    /**
     * Converts this enum to the AI package category enum.
     * This is used for compatibility with the AI package.
     * 
     * @return The AI category
     */
    public com.modforge.intellij.plugin.ai.PatternCategory toAiCategory() {
        switch (this) {
            case CODE_GENERATION:
                return com.modforge.intellij.plugin.ai.PatternCategory.CODE_GENERATION;
            case CODE_FIXING:
                return com.modforge.intellij.plugin.ai.PatternCategory.CODE_FIXING;
            case CODE_ENHANCEMENT:
                return com.modforge.intellij.plugin.ai.PatternCategory.CODE_ENHANCEMENT;
            case DOCUMENTATION:
                return com.modforge.intellij.plugin.ai.PatternCategory.DOCUMENTATION;
            case FEATURE_ADDITION:
                return com.modforge.intellij.plugin.ai.PatternCategory.FEATURE_ADDITION;
            default:
                // Default to code generation
                return com.modforge.intellij.plugin.ai.PatternCategory.CODE_GENERATION;
        }
    }
}