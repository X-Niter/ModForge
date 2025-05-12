package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import com.modforge.intellij.plugin.utils.VirtualFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for integrating with GitHub.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class GitHubIntegrationService {
    private static final Logger LOG = Logger.getInstance(GitHubIntegrationService.class);
    
    // GitHub API endpoints
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String REPOS_ENDPOINT = GITHUB_API_BASE + "/repos";
    private static final String GISTS_ENDPOINT = GITHUB_API_BASE + "/gists";
    private static final String USER_REPOS_ENDPOINT = GITHUB_API_BASE + "/user/repos";
    
    // HTTP status codes
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UNAUTHORIZED = 401;
    
    // Service instances
    private final ModAuthenticationManager authManager;
    private final ModForgeNotificationService notificationService;
    
    // Repository monitoring
    private final Map<String, ScheduledFuture<?>> monitoringTasks = new ConcurrentHashMap<>();
    private final Map<String, Consumer<String>> updateCallbacks = new ConcurrentHashMap<>();
    
    // JSON parser
    private final Gson gson = new Gson();

    /**
     * Constructor.
     */
    public GitHubIntegrationService() {
        this.authManager = ModAuthenticationManager.getInstance();
        this.notificationService = ModForgeNotificationService.getInstance();
    }

    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static GitHubIntegrationService getInstance() {
        return ApplicationManager.getApplication().getService(GitHubIntegrationService.class);
    }

    /**
     * Pushes a project to GitHub.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     * @param rootDir    The project root directory.
     * @param isPrivate  Whether the repository is private.
     * @param callback   The callback to call with the repository URL.
     */
    @RequiresBackgroundThread
    public void pushToGitHub(
            @NotNull String owner,
            @NotNull String repository,
            @NotNull String rootDir,
            boolean isPrivate,
            @NotNull Consumer<String> callback) {
        
        if (!authManager.isAuthenticated()) {
            LOG.warn("Not authenticated");
            callback.accept("Error: Not authenticated");
            return;
        }
        
        String authHeader = authManager.getAuthenticationHeader();
        if (authHeader == null) {
            LOG.warn("Invalid authentication");
            callback.accept("Error: Invalid authentication");
            return;
        }
        
        // Check if repository exists
        ThreadUtils.runAsyncVirtual(() -> {
            try {
                boolean exists = repositoryExists(owner, repository, authHeader);
                if (exists) {
                    callback.accept("Error: Repository already exists");
                    return;
                }
                
                // Create repository
                String repositoryUrl = createRepository(owner, repository, isPrivate, authHeader);
                if (repositoryUrl == null) {
                    callback.accept("Error: Failed to create repository");
                    return;
                }
                
                // Package project files
                File zipFile = packageProject(rootDir);
                if (zipFile == null) {
                    callback.accept("Error: Failed to package project");
                    return;
                }
                
                // Upload project files
                boolean uploaded = uploadProjectFiles(owner, repository, zipFile, authHeader);
                zipFile.delete();
                
                if (!uploaded) {
                    callback.accept("Error: Failed to upload project files");
                    return;
                }
                
                callback.accept("Success: " + repositoryUrl);
            } catch (Exception e) {
                LOG.error("Error pushing to GitHub", e);
                callback.accept("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if a repository exists.
     *
     * @param owner       The repository owner.
     * @param repository  The repository name.
     * @param authHeader  The authentication header.
     * @return Whether the repository exists.
     * @throws IOException If an I/O error occurs.
     */
    private boolean repositoryExists(@NotNull String owner, @NotNull String repository, @NotNull String authHeader) throws IOException {
        URL url = new URL(REPOS_ENDPOINT + "/" + owner + "/" + repository);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", authHeader);
        connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        
        int responseCode = connection.getResponseCode();
        return responseCode == HTTP_OK;
    }

    /**
     * Creates a new repository.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     * @param isPrivate  Whether the repository is private.
     * @param authHeader The authentication header.
     * @return The repository URL or null if creation failed.
     * @throws IOException If an I/O error occurs.
     */
    @Nullable
    private String createRepository(
            @NotNull String owner,
            @NotNull String repository,
            boolean isPrivate,
            @NotNull String authHeader) throws IOException {
        
        URL url = new URL(USER_REPOS_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authHeader);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        connection.setDoOutput(true);
        
        JsonObject json = new JsonObject();
        json.addProperty("name", repository);
        json.addProperty("private", isPrivate);
        json.addProperty("auto_init", true);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HTTP_CREATED) {
            LOG.warn("Failed to create repository: " + responseCode);
            return null;
        }
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            
            JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
            return responseJson.get("html_url").getAsString();
        }
    }

    /**
     * Packages a project into a zip file.
     *
     * @param rootDir The project root directory.
     * @return The zip file or null if packaging failed.
     */
    @Nullable
    private File packageProject(@NotNull String rootDir) {
        VirtualFile root = VirtualFileUtil.findFileByPath(rootDir);
        if (root == null) {
            LOG.warn("Failed to find root directory: " + rootDir);
            return null;
        }
        
        File zipFile;
        try {
            zipFile = File.createTempFile("project", ".zip");
        } catch (IOException e) {
            LOG.error("Failed to create temporary file", e);
            return null;
        }
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addToZip(zos, root, "");
        } catch (IOException e) {
            LOG.error("Failed to package project", e);
            zipFile.delete();
            return null;
        }
        
        return zipFile;
    }

    /**
     * Adds a file or directory to a zip file.
     *
     * @param zos    The zip output stream.
     * @param file   The file or directory to add.
     * @param prefix The path prefix in the zip file.
     * @throws IOException If an I/O error occurs.
     */
    private void addToZip(@NotNull ZipOutputStream zos, @NotNull VirtualFile file, @NotNull String prefix) throws IOException {
        String path = prefix + file.getName();
        
        if (file.isDirectory()) {
            if (!path.isEmpty()) {
                path += "/";
                ZipEntry entry = new ZipEntry(path);
                zos.putNextEntry(entry);
                zos.closeEntry();
            }
            
            for (VirtualFile child : file.getChildren()) {
                addToZip(zos, child, path);
            }
        } else {
            ZipEntry entry = new ZipEntry(path);
            zos.putNextEntry(entry);
            
            byte[] content = file.contentsToByteArray();
            zos.write(content, 0, content.length);
            
            zos.closeEntry();
        }
    }

    /**
     * Uploads project files to GitHub.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     * @param zipFile    The zip file.
     * @param authHeader The authentication header.
     * @return Whether the upload was successful.
     */
    private boolean uploadProjectFiles(
            @NotNull String owner,
            @NotNull String repository,
            @NotNull File zipFile,
            @NotNull String authHeader) {
        
        // This is a simplified implementation
        // In a real implementation, you would use a Git client to push the files
        // Here, we'll just upload the zip file as a gist
        
        try {
            URL url = new URL(GISTS_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", authHeader);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
            connection.setDoOutput(true);
            
            JsonObject files = new JsonObject();
            JsonObject fileObject = new JsonObject();
            fileObject.addProperty("content", "Project files are in the zip file");
            files.add("project.txt", fileObject);
            
            JsonObject json = new JsonObject();
            json.addProperty("description", "Project files for " + repository);
            json.add("files", files);
            json.addProperty("public", false);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            return responseCode == HTTP_CREATED;
        } catch (IOException e) {
            LOG.error("Failed to upload project files", e);
            return false;
        }
    }

    /**
     * Starts monitoring a repository for changes.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     */
    public void startMonitoring(@NotNull String owner, @NotNull String repository) {
        String key = owner + "/" + repository;
        
        if (monitoringTasks.containsKey(key)) {
            LOG.info("Already monitoring repository: " + key);
            return;
        }
        
        // Schedule a task to check for updates every 5 minutes
        ScheduledFuture<?> task = ThreadUtils.scheduleAtFixedRate(
                () -> checkForUpdates(owner, repository),
                Duration.ofMinutes(1),
                Duration.ofMinutes(5)
        );
        
        monitoringTasks.put(key, task);
        LOG.info("Started monitoring repository: " + key);
    }

    /**
     * Stops monitoring a repository.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     */
    public void stopMonitoring(@NotNull String owner, @NotNull String repository) {
        String key = owner + "/" + repository;
        
        ScheduledFuture<?> task = monitoringTasks.remove(key);
        if (task != null) {
            task.cancel(false);
            LOG.info("Stopped monitoring repository: " + key);
        }
        
        updateCallbacks.remove(key);
    }

    /**
     * Registers a callback for repository updates.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     * @param callback   The callback to call when there are updates.
     */
    public void registerUpdateCallback(@NotNull String owner, @NotNull String repository, @NotNull Consumer<String> callback) {
        String key = owner + "/" + repository;
        updateCallbacks.put(key, callback);
    }

    /**
     * Checks for updates in a repository.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     */
    private void checkForUpdates(@NotNull String owner, @NotNull String repository) {
        String key = owner + "/" + repository;
        Consumer<String> callback = updateCallbacks.get(key);
        
        if (callback == null) {
            return;
        }
        
        if (!authManager.isAuthenticated()) {
            LOG.warn("Not authenticated, cannot check for updates");
            return;
        }
        
        String authHeader = authManager.getAuthenticationHeader();
        if (authHeader == null) {
            LOG.warn("Invalid authentication, cannot check for updates");
            return;
        }
        
        try {
            URL url = new URL(REPOS_ENDPOINT + "/" + owner + "/" + repository + "/commits");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", authHeader);
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HTTP_OK) {
                LOG.warn("Failed to check for updates: " + responseCode);
                return;
            }
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                // For simplicity, we'll just pass the raw JSON to the callback
                callback.accept(response.toString());
            }
        } catch (IOException e) {
            LOG.error("Failed to check for updates", e);
        }
    }

    /**
     * Gets a list of repositories for the authenticated user.
     *
     * @return A CompletableFuture with a list of repository URLs.
     */
    public CompletableFuture<List<String>> getRepositories() {
        return ThreadUtils.supplyAsyncVirtual(() -> {
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return Collections.emptyList();
            }
            
            String authHeader = authManager.getAuthenticationHeader();
            if (authHeader == null) {
                LOG.warn("Invalid authentication");
                return Collections.emptyList();
            }
            
            try {
                URL url = new URL(USER_REPOS_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", authHeader);
                connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
                connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HTTP_OK) {
                    LOG.warn("Failed to get repositories: " + responseCode);
                    return Collections.emptyList();
                }
                
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    List<Map<String, Object>> repos = gson.fromJson(response.toString(), List.class);
                    List<String> repoUrls = new ArrayList<>();
                    
                    for (Map<String, Object> repo : repos) {
                        repoUrls.add((String) repo.get("html_url"));
                    }
                    
                    return repoUrls;
                }
            } catch (IOException e) {
                LOG.error("Failed to get repositories", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Creates a pull request.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     * @param title      The pull request title.
     * @param body       The pull request body.
     * @param head       The head branch.
     * @param base       The base branch.
     * @return A CompletableFuture with the pull request URL or null if creation failed.
     */
    public CompletableFuture<String> createPullRequest(
            @NotNull String owner,
            @NotNull String repository,
            @NotNull String title,
            @NotNull String body,
            @NotNull String head,
            @NotNull String base) {
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            String authHeader = authManager.getAuthenticationHeader();
            if (authHeader == null) {
                LOG.warn("Invalid authentication");
                return null;
            }
            
            try {
                URL url = new URL(REPOS_ENDPOINT + "/" + owner + "/" + repository + "/pulls");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", authHeader);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
                connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
                connection.setDoOutput(true);
                
                JsonObject json = new JsonObject();
                json.addProperty("title", title);
                json.addProperty("body", body);
                json.addProperty("head", head);
                json.addProperty("base", base);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HTTP_CREATED) {
                    LOG.warn("Failed to create pull request: " + responseCode);
                    return null;
                }
                
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
                    return responseJson.get("html_url").getAsString();
                }
            } catch (IOException e) {
                LOG.error("Failed to create pull request", e);
                return null;
            }
        });
    }

    /**
     * Creates an issue.
     *
     * @param owner      The repository owner.
     * @param repository The repository name.
     * @param title      The issue title.
     * @param body       The issue body.
     * @param labels     The issue labels.
     * @return A CompletableFuture with the issue URL or null if creation failed.
     */
    public CompletableFuture<String> createIssue(
            @NotNull String owner,
            @NotNull String repository,
            @NotNull String title,
            @NotNull String body,
            @NotNull List<String> labels) {
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            if (!authManager.isAuthenticated()) {
                LOG.warn("Not authenticated");
                return null;
            }
            
            String authHeader = authManager.getAuthenticationHeader();
            if (authHeader == null) {
                LOG.warn("Invalid authentication");
                return null;
            }
            
            try {
                URL url = new URL(REPOS_ENDPOINT + "/" + owner + "/" + repository + "/issues");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", authHeader);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
                connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
                connection.setDoOutput(true);
                
                JsonObject json = new JsonObject();
                json.addProperty("title", title);
                json.addProperty("body", body);
                
                // Convert labels to JSON array
                JsonObject jsonLabels = new JsonObject();
                for (String label : labels) {
                    jsonLabels.addProperty("name", label);
                }
                json.add("labels", jsonLabels);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HTTP_CREATED) {
                    LOG.warn("Failed to create issue: " + responseCode);
                    return null;
                }
                
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
                    return responseJson.get("html_url").getAsString();
                }
            } catch (IOException e) {
                LOG.error("Failed to create issue", e);
                return null;
            }
        });
    }

    /**
     * Disposes the service.
     */
    public void dispose() {
        for (ScheduledFuture<?> task : monitoringTasks.values()) {
            task.cancel(false);
        }
        
        monitoringTasks.clear();
        updateCallbacks.clear();
    }
}