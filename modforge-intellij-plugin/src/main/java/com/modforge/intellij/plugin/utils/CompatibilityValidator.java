package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;

/**
 * Utility class for validating plugin compatibility with IntelliJ IDEA.
 * Specifically designed for IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) compatibility.
 */
public class CompatibilityValidator {
    private static final Logger LOG = Logger.getInstance(CompatibilityValidator.class);
    
    // Minimum compatible build
    private static final int MIN_BUILD_NUMBER = 251;
    private static final int MIN_BUILD_MINOR = 23774;
    
    // Maximum verified build
    private static final int MAX_BUILD_NUMBER = 252;
    private static final int MAX_BUILD_MINOR = 99999;
    
    /**
     * Validate if the current IntelliJ IDEA version is compatible with this plugin.
     *
     * @return true if compatible, false otherwise
     */
    public static boolean isCompatibleIdeVersion() {
        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String buildNumber = appInfo.getBuild().asString();
        
        LOG.info("Checking compatibility with IntelliJ IDEA " + appInfo.getFullVersion() + 
                " (Build #" + buildNumber + ")");
        
        try {
            // Parse build number (format: IC-251.23774.435)
            String[] parts = buildNumber.split("\\.");
            if (parts.length < 2) {
                LOG.warn("Unexpected build number format: " + buildNumber);
                return false;
            }
            
            // Extract major and minor version numbers
            String majorStr = parts[0];
            if (majorStr.contains("-")) {
                majorStr = majorStr.split("-")[1];
            }
            
            int majorVersion = Integer.parseInt(majorStr);
            int minorVersion = Integer.parseInt(parts[1]);
            
            boolean isCompatible = (majorVersion > MIN_BUILD_NUMBER || 
                                   (majorVersion == MIN_BUILD_NUMBER && minorVersion >= MIN_BUILD_MINOR)) &&
                                   (majorVersion < MAX_BUILD_NUMBER || 
                                   (majorVersion == MAX_BUILD_NUMBER && minorVersion <= MAX_BUILD_MINOR));
            
            if (isCompatible) {
                LOG.info("IntelliJ IDEA version is compatible");
            } else {
                LOG.warn("IntelliJ IDEA version is not officially compatible with this plugin (requires build " + 
                       MIN_BUILD_NUMBER + "." + MIN_BUILD_MINOR + " to " + MAX_BUILD_NUMBER + "." + MAX_BUILD_MINOR + ")");
            }
            
            return isCompatible;
        } catch (Exception e) {
            LOG.error("Error checking IntelliJ IDEA compatibility: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate if the current Java runtime is compatible with this plugin.
     * 
     * @return true if compatible, false otherwise
     */
    public static boolean isCompatibleJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        LOG.info("Checking compatibility with Java runtime " + javaVersion);
        
        try {
            // The plugin requires Java 21 or newer
            if (javaVersion.startsWith("1.")) {
                // Legacy versioning scheme (1.8 for Java 8)
                int majorVersion = Integer.parseInt(javaVersion.substring(2, 3));
                return majorVersion >= 21;
            } else {
                // New versioning scheme
                int majorVersion;
                int dotIndex = javaVersion.indexOf('.');
                if (dotIndex != -1) {
                    majorVersion = Integer.parseInt(javaVersion.substring(0, dotIndex));
                } else {
                    majorVersion = Integer.parseInt(javaVersion);
                }
                
                boolean isCompatible = majorVersion >= 21;
                
                if (isCompatible) {
                    LOG.info("Java version is compatible");
                } else {
                    LOG.warn("Java version is not compatible with this plugin (requires Java 21 or newer)");
                }
                
                return isCompatible;
            }
        } catch (Exception e) {
            LOG.error("Error checking Java compatibility: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if all compatibility requirements are met.
     * 
     * @return true if all requirements are met, false otherwise
     */
    public static boolean checkAllCompatibility() {
        return isCompatibleIdeVersion() && isCompatibleJavaVersion();
    }
}