package com.modforge.intellij.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

/**
 * Service for AI pattern recognition to reduce API costs.
 */
public class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    private boolean isEnabled = true;
    
    /**
     * Constructor.
     */
    public PatternRecognitionService() {
        LOG.info("PatternRecognitionService created");
        
        // Check if pattern learning is enabled in settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        isEnabled = settings.isUsePatternLearning();
    }
    
    /**
     * Get the service instance.
     * @return The service instance
     */
    public static PatternRecognitionService getInstance() {
        return ApplicationManager.getApplication().getService(PatternRecognitionService.class);
    }
    
    /**
     * Check if pattern recognition is enabled.
     * @return Whether pattern recognition is enabled
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * Enable or disable pattern recognition.
     * @param enabled Whether to enable pattern recognition
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        
        // Update settings
        ModForgeSettings settings = ModForgeSettings.getInstance();
        settings.setUsePatternLearning(enabled);
        
        LOG.info("Pattern recognition " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Toggle pattern recognition.
     * @return Whether pattern recognition is enabled after toggling
     */
    public boolean toggle() {
        setEnabled(!isEnabled);
        return isEnabled;
    }
    
    /**
     * Find a pattern for a code generation prompt.
     * @param prompt The prompt
     * @return The pattern, or null if no pattern found
     */
    public String findPattern(String prompt) {
        if (!isEnabled) {
            return null;
        }
        
        // TODO: Implement pattern recognition
        
        return null;
    }
    
    /**
     * Save a new pattern.
     * @param prompt The prompt
     * @param code The generated code
     */
    public void savePattern(String prompt, String code) {
        if (!isEnabled) {
            return;
        }
        
        // TODO: Implement pattern saving
    }
}