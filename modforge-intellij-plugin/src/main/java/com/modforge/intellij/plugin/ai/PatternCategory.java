package com.modforge.intellij.plugin.ai;

import org.jetbrains.annotations.NotNull;

/**
 * Pattern categories used for AI pattern matching.
 * This is a compatibility enum for the AIServiceManager in the ai package.
 */
public enum PatternCategory {
    CODE_GENERATION,
    ERROR_RESOLUTION,
    FEATURE_SUGGESTION,
    CLASS_STRUCTURE,
    METHOD_IMPLEMENTATION,
    ARCHITECTURY_LOADER;
    
    /**
     * Convert to the service package's PatternCategory enum.
     * @return The corresponding PatternCategory in the services package
     */
    @NotNull
    public com.modforge.intellij.plugin.services.PatternRecognitionService.PatternCategory toServiceCategory() {
        return com.modforge.intellij.plugin.services.PatternRecognitionService.PatternCategory.valueOf(this.name());
    }
    
    /**
     * Create from the service package's PatternCategory enum.
     * @param category The category from the services package
     * @return The corresponding PatternCategory in this package
     */
    @NotNull
    public static PatternCategory fromServiceCategory(
            @NotNull com.modforge.intellij.plugin.services.PatternRecognitionService.PatternCategory category) {
        return PatternCategory.valueOf(category.name());
    }
}