package com.modforge.intellij.plugin.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;

/**
 * Utility class for validating the runtime environment for the ModForge plugin.
 * This ensures compatibility with IntelliJ IDEA 2025.1 and Java 21.
 */
public class EnvironmentValidator {
    private static final Logger LOG = Logger.getInstance(EnvironmentValidator.class);
    
    private static final String MIN_IDE_VERSION = "2025.1";
    private static final String MIN_JDK_VERSION = "21.0.6";
    private static final JavaSdkVersion MIN_JAVA_SDK_VERSION = JavaSdkVersion.JDK_21;

    /**
     * Validates the runtime environment and displays warnings for issues.
     * 
     * @param project The current project
     * @return true if the environment is fully compatible, false otherwise
     */
    public static boolean validateEnvironment(Project project) {
        boolean isIdeCompatible = validateIdeVersion();
        boolean isJdkCompatible = validateJdkVersion(project);
        
        if (!isIdeCompatible || !isJdkCompatible) {
            showEnvironmentWarning(project, isIdeCompatible, isJdkCompatible);
            return false;
        }
        
        LOG.info("Environment validation passed successfully");
        return true;
    }
    
    /**
     * Validates the IntelliJ IDEA version.
     * 
     * @return true if the IDE version is compatible, false otherwise
     */
    private static boolean validateIdeVersion() {
        try {
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            String fullVersion = appInfo.getFullVersion();
            String buildString = appInfo.getBuild().asString();
            int buildNumber = extractBuildNumber(buildString);
            
            LOG.info("Checking IntelliJ IDEA version: " + fullVersion + " (Build " + buildString + ")");
            
            // First check by exact version match (most reliable)
            if (fullVersion.contains(MIN_IDE_VERSION)) {
                LOG.info("IDE version validated by exact version match: " + fullVersion);
                return true;
            }
            
            // Check by build number (2025.1 starts at build 251.*)
            if (buildNumber >= 251) {
                LOG.info("IDE version validated by build number: " + buildNumber);
                return true;
            }
            
            // Try numeric comparison as a last resort
            if (fullVersion.compareTo(MIN_IDE_VERSION) >= 0) {
                LOG.info("IDE version validated by version comparison: " + fullVersion + " >= " + MIN_IDE_VERSION);
                return true;
            }
            
            LOG.warn("IDE version validation failed: " + fullVersion + " (build " + buildString + ")");
            return false;
        } catch (Exception e) {
            LOG.warn("Error validating IDE version: " + e.getMessage(), e);
            
            // Fallback to class compatibility check as a last resort
            try {
                // Check for presence of key 2025.1 classes
                Class.forName("com.intellij.platform.workspace.jps.JpsProjectKindIndicator");
                LOG.info("IDE version validated by 2025.1 class compatibility");
                return true;
            } catch (ClassNotFoundException ignored) {
                // Class not found, likely not 2025.1
            }
            
            return false;
        }
    }
    
    /**
     * Extract the numeric build number from the build string
     * 
     * @param buildString The build string (e.g., "IC-251.23774.435")
     * @return The numeric build number (e.g., 251)
     */
    private static int extractBuildNumber(String buildString) {
        if (buildString == null || buildString.isEmpty()) {
            return 0;
        }
        
        try {
            // Extract the first number segment (typically the year/version number)
            String[] parts = buildString.split("\\.");
            if (parts.length > 0) {
                // Handle format like "IC-251.23774.435"
                String firstPart = parts[0].replaceAll("[^0-9]", "");
                if (!firstPart.isEmpty()) {
                    return Integer.parseInt(firstPart);
                }
            }
            return 0;
        } catch (Exception e) {
            LOG.warn("Error extracting build number from: " + buildString, e);
            return 0;
        }
    }
    
    /**
     * Validates the JDK version used by the project.
     * 
     * @param project The current project
     * @return true if the JDK version is compatible, false otherwise
     */
    private static boolean validateJdkVersion(Project project) {
        if (project == null) {
            LOG.warn("Cannot validate JDK version: project is null");
            return checkRuntimeJavaVersion();  // Fallback to runtime check
        }
        
        try {
            // Start with checking the project SDK
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            
            // If project SDK is not set, try to find a valid SDK in the modules
            if (projectSdk == null) {
                LOG.info("Project SDK is not set, checking module SDKs...");
                projectSdk = findValidModuleSdk(project);
            }
            
            // If we have a valid SDK, check its version
            if (projectSdk != null) {
                if (validateSdkVersion(projectSdk)) {
                    return true;
                }
            } else {
                LOG.warn("No valid SDK found in project or modules");
            }
            
            // If SDK validation fails or no SDK is found, try runtime
            return checkRuntimeJavaVersion();
            
        } catch (Exception e) {
            LOG.warn("Error during JDK validation: " + e.getMessage(), e);
            return checkRuntimeJavaVersion();  // Fallback to runtime check
        }
    }
    
    /**
     * Find a valid Java SDK in any of the project's modules
     * 
     * @param project The current project
     * @return A valid Java SDK or null if none is found
     */
    private static Sdk findValidModuleSdk(Project project) {
        try {
            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
                try {
                    Sdk moduleSdk = ModuleRootManager.getInstance(module).getSdk();
                    if (moduleSdk != null && moduleSdk.getSdkType() instanceof JavaSdk) {
                        LOG.info("Found Java SDK in module: " + module.getName());
                        return moduleSdk;
                    }
                } catch (Exception e) {
                    LOG.warn("Error checking SDK for module " + module.getName() + ": " + e.getMessage());
                    // Continue with next module
                }
            }
        } catch (Exception e) {
            LOG.warn("Error while scanning for module SDKs: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Validate a specific SDK's version against our requirements
     * 
     * @param sdk The SDK to validate
     * @return true if the SDK version is compatible, false otherwise
     */
    private static boolean validateSdkVersion(Sdk sdk) {
        if (sdk == null) {
            return false;
        }
        
        String jdkVersion = sdk.getVersionString();
        LOG.info("Checking JDK version: " + jdkVersion);
        
        // Check if it's a Java SDK
        if (!(sdk.getSdkType() instanceof JavaSdk)) {
            LOG.warn("SDK is not a Java SDK: " + sdk.getSdkType().getName());
            return false;
        }
        
        // Check Java SDK version using JavaSdk's API
        JavaSdk javaSdk = (JavaSdk) sdk.getSdkType();
        JavaSdkVersion version = javaSdk.getVersion(sdk);
        
        if (version == null) {
            LOG.warn("Cannot determine Java SDK version through JavaSdk API");
            
            // Try the version string directly
            if (jdkVersion != null) {
                String extractedVersion = extractVersion(jdkVersion);
                LOG.info("Checking extracted version: " + extractedVersion);
                
                if (extractedVersion.startsWith("21") || compareVersions(extractedVersion, MIN_JDK_VERSION) >= 0) {
                    LOG.info("SDK validated through version string: " + extractedVersion);
                    return true;
                }
            }
            
            return false;
        }
        
        LOG.info("Java SDK version through API: " + version.getDescription());
        
        // Primary check using JavaSdk API
        if (version.isAtLeast(MIN_JAVA_SDK_VERSION)) {
            LOG.info("SDK validated through JavaSdk API: " + version.getDescription());
            return true;
        }
        
        // Secondary check using version string (for edge cases)
        if (jdkVersion != null) {
            if (jdkVersion.contains(MIN_JDK_VERSION) || 
                jdkVersion.contains("21.0") ||
                compareVersions(extractVersion(jdkVersion), MIN_JDK_VERSION) >= 0) {
                
                LOG.info("SDK validated through version string: " + jdkVersion);
                return true;
            }
        }
        
        LOG.warn("SDK version is not compatible: " + jdkVersion);
        return false;
    }
    
    /**
     * Check the runtime Java version as a fallback
     * 
     * @return true if the runtime Java version is compatible, false otherwise
     */
    private static boolean checkRuntimeJavaVersion() {
        try {
            // Get runtime Java version
            String runtimeVersion = System.getProperty("java.version");
            String runtimeName = System.getProperty("java.vm.name");
            String runtimeVendor = System.getProperty("java.vendor");
            
            LOG.info("Checking runtime Java version: " + runtimeVersion + 
                     " (" + runtimeName + " from " + runtimeVendor + ")");
            
            if (runtimeVersion == null) {
                LOG.warn("Cannot determine runtime Java version");
                return false;
            }
            
            // Multiple ways to check for Java 21 compatibility
            boolean isCompatible = false;
            
            // Check for Java 21
            if (runtimeVersion.startsWith("21") || runtimeVersion.startsWith("21.")) {
                LOG.info("Runtime is Java 21: " + runtimeVersion);
                isCompatible = true;
            }
            // Check specific minimum version
            else if (compareVersions(runtimeVersion, MIN_JDK_VERSION) >= 0) {
                LOG.info("Runtime Java version is >= " + MIN_JDK_VERSION + ": " + runtimeVersion);
                isCompatible = true;
            }
            // Check for common JDK version patterns
            else if (runtimeVersion.matches("1\\.21\\..*")) {
                LOG.info("Runtime uses legacy Java version format for Java 21: " + runtimeVersion);
                isCompatible = true;
            }
            
            // Also try to check for Java 21 features as a last resort
            if (!isCompatible) {
                try {
                    // Check for a Java 21 API feature (virtual threads)
                    Class<?> threadClass = Thread.class;
                    if (threadClass.getMethod("startVirtualThread", Runnable.class) != null) {
                        LOG.info("Runtime supports Java 21 feature (virtual threads)");
                        isCompatible = true;
                    }
                } catch (NoSuchMethodException e) {
                    // Not Java 21, continue with other checks
                }
            }
            
            return isCompatible;
        } catch (Exception e) {
            LOG.warn("Error checking runtime Java version: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Shows a warning notification for environment issues.
     * 
     * @param project The current project
     * @param isIdeCompatible Whether the IDE version is compatible
     * @param isJdkCompatible Whether the JDK version is compatible
     */
    private static void showEnvironmentWarning(Project project, boolean isIdeCompatible, boolean isJdkCompatible) {
        StringBuilder message = new StringBuilder("ModForge Plugin Environment Warning:<br>");
        
        if (!isIdeCompatible) {
            message.append("• IntelliJ IDEA 2025.1 or newer is required<br>");
        }
        
        if (!isJdkCompatible) {
            message.append("• JDK 21.0.6 or newer is required<br>");
        }
        
        message.append("<br>Some features may not work correctly. Please update to compatible versions.");
        
        Notification notification = new Notification(
            "ModForge Notifications",
            "ModForge Plugin Environment Warning",
            message.toString(),
            NotificationType.WARNING
        );
        
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Extracts the version number from a version string.
     * 
     * @param versionString The version string (e.g., "JDK 21.0.6")
     * @return The extracted version number (e.g., "21.0.6")
     */
    private static String extractVersion(String versionString) {
        if (versionString == null) {
            return "";
        }
        
        // Extract version number (assuming format like "JDK 21.0.6")
        String[] parts = versionString.split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : versionString;
    }
    
    /**
     * Compares two version strings.
     * 
     * @param version1 The first version
     * @param version2 The second version
     * @return Negative if version1 < version2, zero if equal, positive if version1 > version2
     */
    private static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int v1 = (i < parts1.length) ? parseInt(parts1[i]) : 0;
            int v2 = (i < parts2.length) ? parseInt(parts2[i]) : 0;
            
            if (v1 != v2) {
                return v1 - v2;
            }
        }
        
        return 0;
    }
    
    /**
     * Parses a string to an integer, handling non-numeric parts.
     * 
     * @param str The string to parse
     * @return The parsed integer or 0 if parsing fails
     */
    private static int parseInt(String str) {
        try {
            // Remove any non-numeric suffix
            String numericPart = str.replaceAll("[^0-9].*$", "");
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}