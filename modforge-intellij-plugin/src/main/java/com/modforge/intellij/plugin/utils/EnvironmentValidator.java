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
        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String fullVersion = appInfo.getFullVersion();
        String buildString = appInfo.getBuild().asString();
        
        LOG.info("Checking IntelliJ IDEA version: " + fullVersion + " (Build " + buildString + ")");
        
        return fullVersion.contains(MIN_IDE_VERSION) || 
               fullVersion.compareTo(MIN_IDE_VERSION) >= 0;
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
            return false;
        }
        
        try {
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk == null) {
                LOG.warn("Cannot validate JDK version: project SDK is not set");
                
                // Rather than failing immediately, check if any module has Java SDK set
                boolean foundValidModuleSdk = false;
                Module[] modules = ModuleManager.getInstance(project).getModules();
                for (Module module : modules) {
                    Sdk moduleSdk = ModuleRootManager.getInstance(module).getSdk();
                    if (moduleSdk != null && moduleSdk.getSdkType() instanceof JavaSdk) {
                        projectSdk = moduleSdk;
                        foundValidModuleSdk = true;
                        LOG.info("Found valid Java SDK in module: " + module.getName());
                        break;
                    }
                }
                
                if (!foundValidModuleSdk) {
                    return false;
                }
            }
            
            String jdkVersion = projectSdk.getVersionString();
            LOG.info("Checking project JDK version: " + jdkVersion);
            
            // Check if it's a Java SDK
            if (!(projectSdk.getSdkType() instanceof JavaSdk)) {
                LOG.warn("Project SDK is not a Java SDK: " + projectSdk.getSdkType().getName());
                return false;
            }
            
            // Check Java SDK version
            JavaSdk javaSdk = (JavaSdk) projectSdk.getSdkType();
            JavaSdkVersion version = javaSdk.getVersion(projectSdk);
            
            if (version == null) {
                LOG.warn("Cannot determine Java SDK version");
                
                // Fallback to runtime version if SDK version cannot be determined
                String runtimeVersion = System.getProperty("java.version");
                if (runtimeVersion != null && (runtimeVersion.startsWith("21") || compareVersions(runtimeVersion, MIN_JDK_VERSION) >= 0)) {
                    LOG.info("Using runtime Java version: " + runtimeVersion);
                    return true;
                }
                
                return false;
            }
            
            LOG.info("Java SDK version: " + version.getDescription());
            
            return version.isAtLeast(MIN_JAVA_SDK_VERSION) && 
                  (jdkVersion == null || jdkVersion.contains(MIN_JDK_VERSION) || 
                   compareVersions(extractVersion(jdkVersion), MIN_JDK_VERSION) >= 0);
        } catch (Exception e) {
            LOG.warn("Error validating JDK version: " + e.getMessage(), e);
            
            // Fallback to runtime version
            String runtimeVersion = System.getProperty("java.version");
            if (runtimeVersion != null && (runtimeVersion.startsWith("21") || compareVersions(runtimeVersion, MIN_JDK_VERSION) >= 0)) {
                LOG.info("Using runtime Java version: " + runtimeVersion);
                return true;
            }
            
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