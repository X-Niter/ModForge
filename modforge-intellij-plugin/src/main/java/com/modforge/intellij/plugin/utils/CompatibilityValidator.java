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
        try {
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            if (appInfo == null) {
                LOG.error("Unable to retrieve ApplicationInfo - running in a test environment?");
                // In test environments, we should allow the plugin to work
                return true;
            }
            
            String buildNumber = appInfo.getBuild().asString();
            String fullVersion = appInfo.getFullVersion();
            String apiVersion = appInfo.getApiVersion();
            
            LOG.info("Checking compatibility with IntelliJ IDEA " + fullVersion + 
                    " (Build #" + buildNumber + ", API: " + apiVersion + ")");
            
            // Direct check for 2025.1
            if (fullVersion != null && fullVersion.contains("2025.1")) {
                LOG.info("IntelliJ IDEA 2025.1 detected via version string");
                return true;
            }
            
            // Parse build number (format: IC-251.23774.435)
            String[] parts = buildNumber.split("\\.");
            if (parts.length < 2) {
                LOG.warn("Unexpected build number format: " + buildNumber);
                
                // Last resort - look for "251" in the build string
                if (buildNumber != null && (buildNumber.contains("251") || buildNumber.contains("IC-251"))) {
                    LOG.info("IntelliJ IDEA build 251 detected via partial match");
                    return true;
                }
                
                return false;
            }
            
            // Extract major and minor version numbers
            String majorStr = parts[0];
            if (majorStr.contains("-")) {
                majorStr = majorStr.split("-")[1];
            }
            
            try {
                int majorVersion = Integer.parseInt(majorStr);
                int minorVersion = Integer.parseInt(parts[1]);
                
                boolean isCompatible = (majorVersion > MIN_BUILD_NUMBER || 
                                       (majorVersion == MIN_BUILD_NUMBER && minorVersion >= MIN_BUILD_MINOR)) &&
                                       (majorVersion < MAX_BUILD_NUMBER || 
                                       (majorVersion == MAX_BUILD_NUMBER && minorVersion <= MAX_BUILD_MINOR));
                
                if (isCompatible) {
                    LOG.info("IntelliJ IDEA version is compatible (build range check)");
                } else {
                    LOG.warn("IntelliJ IDEA version is not officially compatible with this plugin (requires build " + 
                           MIN_BUILD_NUMBER + "." + MIN_BUILD_MINOR + " to " + MAX_BUILD_NUMBER + "." + MAX_BUILD_MINOR + ")");

                    // Special handling for minor compatibility outside range but still likely compatible
                    if (majorVersion == MIN_BUILD_NUMBER && minorVersion < MIN_BUILD_MINOR && minorVersion >= MIN_BUILD_MINOR - 100) {
                        LOG.info("IntelliJ IDEA version is slightly below minimum but may still be compatible, allowing with warning");
                        return true;
                    }
                    if (majorVersion == MAX_BUILD_NUMBER && minorVersion > MAX_BUILD_MINOR && minorVersion <= MAX_BUILD_MINOR + 1000) {
                        LOG.info("IntelliJ IDEA version is slightly above maximum but may still be compatible, allowing with warning");
                        return true;
                    }
                }
                
                return isCompatible;
            } catch (NumberFormatException e) {
                LOG.warn("Error parsing build number components: " + buildNumber, e);
                
                // Fall back to API version check which might be more reliable
                if (apiVersion != null) {
                    try {
                        // API version format is typically a simple number like "2025.1"
                        double apiVer = Double.parseDouble(apiVersion);
                        boolean apiCompatible = apiVer >= 2025.1;
                        
                        if (apiCompatible) {
                            LOG.info("IntelliJ IDEA API version is compatible: " + apiVersion);
                            return true;
                        } else {
                            LOG.warn("IntelliJ IDEA API version is not compatible: " + apiVersion);
                        }
                    } catch (NumberFormatException apiEx) {
                        LOG.warn("Error parsing API version: " + apiVersion, apiEx);
                    }
                }
                
                // Last resort check for 2025.1 in the build string
                if (buildNumber != null && buildNumber.contains("2025.1")) {
                    LOG.info("IntelliJ IDEA 2025.1 detected via build string content");
                    return true;
                }
                
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error checking IntelliJ IDEA compatibility: " + e.getMessage(), e);
            // Special case for headless environments or unit tests
            String isHeadless = System.getProperty("java.awt.headless");
            if ("true".equals(isHeadless)) {
                LOG.info("Running in headless mode, skipping IDE version check");
                return true;
            }
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
        String javaHome = System.getProperty("java.home");
        String javaVmVersion = System.getProperty("java.vm.version");
        
        LOG.info("Checking compatibility with Java runtime " + javaVersion);
        LOG.info("Java home: " + javaHome);
        LOG.info("Java VM version: " + javaVmVersion);
        
        try {
            if (javaVersion == null || javaVersion.isEmpty()) {
                LOG.warn("Java version property is missing or empty");
                // Fall back to runtime version
                String runtimeVersion = Runtime.version().toString();
                LOG.info("Using Runtime.version(): " + runtimeVersion);
                javaVersion = runtimeVersion;
            }
            
            // First, check for explicit Java 21 match
            if (javaVersion.startsWith("21") || javaVersion.contains("21.")) {
                LOG.info("Java 21 detected via direct version match");
                return true;
            }
            
            // Check Java VM version which might be more accurate in some environments
            if (javaVmVersion != null && (javaVmVersion.startsWith("21") || javaVmVersion.contains("21."))) {
                LOG.info("Java 21 detected via VM version");
                return true;
            }
            
            // The plugin requires Java 21 or newer
            if (javaVersion.startsWith("1.")) {
                // Legacy versioning scheme (1.8 for Java 8)
                try {
                    int majorVersion = Integer.parseInt(javaVersion.substring(2, 3));
                    boolean isCompatible = majorVersion >= 21; // This will always be false for legacy scheme
                    if (!isCompatible) {
                        LOG.warn("Java version is not compatible with this plugin (requires Java 21 or newer)");
                    }
                    return isCompatible;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    LOG.warn("Error parsing legacy Java version: " + javaVersion, e);
                    // Try fallback to VM version
                    if (javaVmVersion != null && javaVmVersion.contains("21")) {
                        LOG.info("Java 21 compatibility determined from VM version fallback");
                        return true;
                    }
                    return false;
                }
            } else {
                // Modern versioning scheme (e.g., 21.0.6+9-b895.109)
                try {
                    // Parse version more carefully to handle various formats
                    String versionToParse = javaVersion;
                    
                    // Remove build and patch information if present
                    if (versionToParse.contains("+")) {
                        versionToParse = versionToParse.substring(0, versionToParse.indexOf("+"));
                    }
                    
                    int majorVersion;
                    int dotIndex = versionToParse.indexOf('.');
                    if (dotIndex != -1) {
                        majorVersion = Integer.parseInt(versionToParse.substring(0, dotIndex));
                    } else {
                        majorVersion = Integer.parseInt(versionToParse);
                    }
                    
                    boolean isCompatible = majorVersion >= 21;
                    
                    if (isCompatible) {
                        LOG.info("Java version is compatible: " + majorVersion);
                    } else {
                        LOG.warn("Java version is not compatible with this plugin (requires Java 21 or newer)");
                    }
                    
                    return isCompatible;
                } catch (NumberFormatException e) {
                    LOG.warn("Error parsing Java version: " + javaVersion, e);
                    
                    // Last resort: check if string contains "21"
                    if (javaVersion.contains("21")) {
                        LOG.info("Java 21 compatibility assumed from version string content");
                        return true;
                    }
                    
                    return false;
                }
            }
        } catch (Exception e) {
            LOG.error("Error checking Java compatibility: " + e.getMessage(), e);
            // Last resort emergency fallback
            try {
                // Try to create a virtual thread as proof of Java 21 capabilities
                Thread vt = Thread.ofVirtual().start(() -> {});
                vt.join(100); // Wait briefly
                LOG.info("Java 21 feature (virtual threads) detected, assuming compatibility");
                return true;
            } catch (Throwable t) {
                LOG.error("Virtual thread test failed, Java 21 features not available", t);
                return false;
            }
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