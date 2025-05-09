package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for IDE integration.
 * Handles auto-detection of IDE installations and plugin auto-installation.
 */
public class IDEIntegrationUtil {
    private static final Logger LOG = Logger.getInstance(IDEIntegrationUtil.class);
    
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    
    // Common IntelliJ installation paths by OS
    private static final List<String> WINDOWS_INTELLIJ_PATHS = Arrays.asList(
            "C:\\Program Files\\JetBrains",
            "C:\\Program Files (x86)\\JetBrains"
    );
    
    private static final List<String> MAC_INTELLIJ_PATHS = Arrays.asList(
            "/Applications",
            System.getProperty("user.home") + "/Applications"
    );
    
    private static final List<String> LINUX_INTELLIJ_PATHS = Arrays.asList(
            "/opt/JetBrains",
            System.getProperty("user.home") + "/.local/share/JetBrains"
    );
    
    /**
     * Gets the URL for downloading the latest version of the plugin.
     * @param apiUrl The API URL
     * @return The download URL
     */
    @Nullable
    public static String getPluginDownloadUrl(String apiUrl) {
        try {
            String downloadInfoUrl = apiUrl.replace("/api", "/download/plugin/info");
            String response = ApiRequestUtil.get(downloadInfoUrl);
            if (response != null) {
                // Parse the response as JSON
                com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(response, com.google.gson.JsonObject.class);
                return jsonObject.get("downloadUrl").getAsString();
            }
        } catch (Exception e) {
            LOG.error("Error getting plugin download URL", e);
        }
        return null;
    }
    
    /**
     * Downloads and installs the plugin to the specified IDE installation.
     * @param downloadUrl The download URL
     * @param installPath The installation path
     * @param listener A listener for installation progress
     * @return A CompletableFuture that completes when the installation is done
     */
    public static CompletableFuture<Boolean> downloadAndInstallPlugin(
            @NotNull String downloadUrl,
            @NotNull String installPath,
            @NotNull InstallProgressListener listener
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Downloading plugin from " + downloadUrl);
                listener.onProgress("Downloading plugin...", 0);
                
                // Create temp file for download
                Path tempFile = Files.createTempFile("modforge-plugin", ".zip");
                
                // Download the file
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int fileSize = connection.getContentLength();
                
                try (InputStream in = connection.getInputStream();
                     ReadableByteChannel rbc = Channels.newChannel(in);
                     FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                    
                    // Download in chunks to track progress
                    byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                    int bytesRead;
                    long totalBytesRead = 0;
                    
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        // Update progress
                        if (fileSize > 0) {
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            listener.onProgress("Downloading plugin...", progress);
                        }
                    }
                }
                
                // Install the plugin
                listener.onProgress("Installing plugin...", 50);
                
                // Create plugins directory if it doesn't exist
                File pluginsDir = new File(installPath);
                if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
                    throw new IOException("Failed to create plugins directory: " + pluginsDir.getAbsolutePath());
                }
                
                // Extract the plugin to the plugins directory
                extractZip(tempFile.toString(), pluginsDir.getAbsolutePath());
                
                // Delete the temp file
                Files.delete(tempFile);
                
                listener.onProgress("Installation complete!", 100);
                LOG.info("Plugin installation complete at " + installPath);
                
                return true;
            } catch (Exception e) {
                LOG.error("Error installing plugin", e);
                listener.onError("Error installing plugin: " + e.getMessage());
                return false;
            }
        }, EXECUTOR);
    }
    
    /**
     * Gets the IDE installation path.
     * @return The IDE installation path, or null if not found
     */
    @Nullable
    public static String getPluginInstallPath() {
        // First, check the current IDE if running inside one
        String idePath = getIDEPluginsPathFromCurrentInstance();
        if (idePath != null) {
            return idePath;
        }
        
        // If not, try to find the IDE installation path
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return findIDEInstallationPath(WINDOWS_INTELLIJ_PATHS);
        } else if (os.contains("mac")) {
            return findIDEInstallationPath(MAC_INTELLIJ_PATHS);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return findIDEInstallationPath(LINUX_INTELLIJ_PATHS);
        }
        
        return null;
    }
    
    /**
     * Gets the plugins path from the current IDE instance.
     * @return The plugins path, or null if not running inside an IDE
     */
    @Nullable
    private static String getIDEPluginsPathFromCurrentInstance() {
        try {
            // Check if we're running inside IntelliJ
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            if (appInfo != null) {
                // Get the config directory
                String configDir = System.getProperty("idea.config.path");
                if (configDir != null) {
                    return configDir + File.separator + "plugins";
                }
            }
        } catch (Exception e) {
            // Not running inside IntelliJ
            LOG.debug("Not running inside IntelliJ", e);
        }
        
        return null;
    }
    
    /**
     * Finds the IDE installation path.
     * @param searchPaths The paths to search
     * @return The IDE installation path, or null if not found
     */
    @Nullable
    private static String findIDEInstallationPath(List<String> searchPaths) {
        for (String basePath : searchPaths) {
            File baseDir = new File(basePath);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                continue;
            }
            
            // Look for IntelliJ IDEA directories
            File[] subDirs = baseDir.listFiles(File::isDirectory);
            if (subDirs == null) continue;
            
            for (File subDir : subDirs) {
                if (subDir.getName().contains("IntelliJ IDEA")) {
                    File pluginsDir = new File(subDir, "plugins");
                    if (pluginsDir.exists() || pluginsDir.mkdirs()) {
                        return pluginsDir.getAbsolutePath();
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extracts a ZIP file.
     * @param zipFilePath The path to the ZIP file
     * @param destDir The destination directory
     * @throws IOException If an I/O error occurs
     */
    private static void extractZip(String zipFilePath, String destDir) throws IOException {
        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            destDirFile.mkdirs();
        }
        
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            // Create a buffer for reading the file content
            byte[] buffer = new byte[4096];
            
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // Create parent directories if they don't exist
                    File parent = new File(filePath).getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    // Extract the file
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    // Create the directory
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
    
    /**
     * Checks if there is a new version of the plugin available.
     * @param apiUrl The API URL
     * @return Whether there is a new version available
     */
    public static boolean isNewVersionAvailable(String apiUrl) {
        try {
            String versionInfoUrl = apiUrl.replace("/api", "/version/plugin/info");
            String response = ApiRequestUtil.get(versionInfoUrl);
            if (response != null) {
                // Parse the response as JSON
                com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(response, com.google.gson.JsonObject.class);
                
                String latestVersion = jsonObject.get("version").getAsString();
                String currentVersion = getPluginVersion();
                
                return compareVersions(latestVersion, currentVersion) > 0;
            }
        } catch (Exception e) {
            LOG.error("Error checking for new version", e);
        }
        
        return false;
    }
    
    /**
     * Gets the current plugin version.
     * @return The current plugin version
     */
    @NotNull
    private static String getPluginVersion() {
        try {
            // Read the version from the plugin.xml
            return "1.0.0"; // In a real implementation, this would read from plugin.xml
        } catch (Exception e) {
            LOG.error("Error getting plugin version", e);
            return "0.0.0";
        }
    }
    
    /**
     * Compares two version strings.
     * @param v1 The first version
     * @param v2 The second version
     * @return 1 if v1 is greater, -1 if v2 is greater, 0 if equal
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        
        return 0;
    }
    
    /**
     * Interface for receiving installation progress updates.
     */
    public interface InstallProgressListener {
        void onProgress(String message, int percentage);
        void onError(String errorMessage);
    }
}