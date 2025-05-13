package com.modforge.intellij.plugin.ai;

/**
 * Enum for pattern categories.
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
     * Converts this enum to the service category enum.
     * This is used for compatibility with the services package.
     * 
     * @return The service category
     */
    public com.modforge.intellij.plugin.services.PatternCategory toServiceCategory() {
        switch (this) {
            case CODE_GENERATION:
                return com.modforge.intellij.plugin.services.PatternCategory.CODE_GENERATION;
            case CODE_FIXING:
                return com.modforge.intellij.plugin.services.PatternCategory.CODE_FIXING;
            case CODE_ENHANCEMENT:
                return com.modforge.intellij.plugin.services.PatternCategory.CODE_ENHANCEMENT;
            case DOCUMENTATION:
                return com.modforge.intellij.plugin.services.PatternCategory.DOCUMENTATION;
            case FEATURE_ADDITION:
                return com.modforge.intellij.plugin.services.PatternCategory.FEATURE_ADDITION;
            default:
                // Default to code generation
                return com.modforge.intellij.plugin.services.PatternCategory.CODE_GENERATION;
        }
    }
}