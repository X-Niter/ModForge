package com.modforge.intellij.plugin.github;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for GitHub API.
 */
public class GitHubApiClient {
    private static final Logger LOG = Logger.getInstance(GitHubApiClient.class);
    private static final String API_URL = "https://api.github.com";
    private static final Gson GSON = new Gson();
    
    private final String token;
    
    /**
     * Create a new GitHub API client.
     *
     * @param token The GitHub token
     */
    public GitHubApiClient(@NotNull String token) {
        this.token = token;
    }
    
    /**
     * Check if a repository exists.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @return Whether the repository exists
     * @throws IOException If an error occurs
     */
    public boolean repositoryExists(@NotNull String owner, @NotNull String repository) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository;
        
        try {
            String response = executeRequest("GET", url, null);
            return true;
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                return false;
            }
            throw e;
        }
    }
    
    /**
     * Create a repository.
     *
     * @param name        The repository name
     * @param description The repository description
     * @param isPrivate   Whether the repository is private
     * @return The repository URL
     * @throws IOException If an error occurs
     */
    public String createRepository(@NotNull String name, @NotNull String description, boolean isPrivate) throws IOException {
        String url = API_URL + "/user/repos";
        
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("private", isPrivate);
        json.addProperty("auto_init", true);
        
        String response = executeRequest("POST", url, json.toString());
        JsonObject repo = GSON.fromJson(response, JsonObject.class);
        
        return repo.get("html_url").getAsString();
    }
    
    /**
     * Create or update a file in a repository.
     *
     * @param owner       The repository owner
     * @param repository  The repository name
     * @param path        The file path
     * @param message     The commit message
     * @param content     The file content
     * @param branch      The branch name
     * @throws IOException If an error occurs
     */
    public void createOrUpdateFile(
            @NotNull String owner,
            @NotNull String repository,
            @NotNull String path,
            @NotNull String message,
            @NotNull String content,
            @NotNull String branch
    ) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/contents/" + path;
        
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        json.addProperty("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        json.addProperty("branch", branch);
        
        // Check if file exists
        try {
            String fileJson = executeRequest("GET", url, null);
            JsonObject fileObject = GSON.fromJson(fileJson, JsonObject.class);
            
            if (fileObject.has("sha")) {
                json.addProperty("sha", fileObject.get("sha").getAsString());
            }
        } catch (IOException e) {
            // File doesn't exist, which is fine for creation
            if (!(e instanceof FileNotFoundException)) {
                throw e;
            }
        }
        
        executeRequest("PUT", url, json.toString());
    }
    
    /**
     * Get open issues for a repository.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @return The open issues
     * @throws IOException If an error occurs
     */
    public List<GitHubIssue> getOpenIssues(@NotNull String owner, @NotNull String repository) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/issues?state=open";
        
        String response = executeRequest("GET", url, null);
        JsonArray issuesArray = GSON.fromJson(response, JsonArray.class);
        
        List<GitHubIssue> issues = new ArrayList<>();
        for (JsonElement element : issuesArray) {
            JsonObject issueObj = element.getAsJsonObject();
            
            // Skip pull requests, which are also returned by the issues endpoint
            if (issueObj.has("pull_request")) {
                continue;
            }
            
            // Get issue details
            int number = issueObj.get("number").getAsInt();
            String title = issueObj.get("title").getAsString();
            String body = issueObj.has("body") && !issueObj.get("body").isJsonNull() ? 
                    issueObj.get("body").getAsString() : "";
            
            // Get labels
            List<String> labels = new ArrayList<>();
            if (issueObj.has("labels")) {
                JsonArray labelsArray = issueObj.getAsJsonArray("labels");
                for (JsonElement labelElement : labelsArray) {
                    JsonObject labelObj = labelElement.getAsJsonObject();
                    labels.add(labelObj.get("name").getAsString());
                }
            }
            
            issues.add(new GitHubIssue(number, title, body, labels));
        }
        
        return issues;
    }
    
    /**
     * Get open pull requests for a repository.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @return The open pull requests
     * @throws IOException If an error occurs
     */
    public List<GitHubPullRequest> getOpenPullRequests(@NotNull String owner, @NotNull String repository) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/pulls?state=open";
        
        String response = executeRequest("GET", url, null);
        JsonArray prsArray = GSON.fromJson(response, JsonArray.class);
        
        List<GitHubPullRequest> pullRequests = new ArrayList<>();
        for (JsonElement element : prsArray) {
            JsonObject prObj = element.getAsJsonObject();
            
            // Get PR details
            int number = prObj.get("number").getAsInt();
            String title = prObj.get("title").getAsString();
            String body = prObj.has("body") && !prObj.get("body").isJsonNull() ? 
                    prObj.get("body").getAsString() : "";
            
            // Get labels (requires a separate API call for pulls)
            List<String> labels = getPullRequestLabels(owner, repository, number);
            
            pullRequests.add(new GitHubPullRequest(number, title, body, labels));
        }
        
        return pullRequests;
    }
    
    /**
     * Get labels for a pull request.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @param number     The pull request number
     * @return The labels
     * @throws IOException If an error occurs
     */
    private List<String> getPullRequestLabels(@NotNull String owner, @NotNull String repository, int number) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/issues/" + number + "/labels";
        
        try {
            String response = executeRequest("GET", url, null);
            JsonArray labelsArray = GSON.fromJson(response, JsonArray.class);
            
            List<String> labels = new ArrayList<>();
            for (JsonElement element : labelsArray) {
                JsonObject labelObj = element.getAsJsonObject();
                labels.add(labelObj.get("name").getAsString());
            }
            
            return labels;
        } catch (IOException e) {
            LOG.warn("Error getting PR labels: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get files changed in a pull request.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @param number     The pull request number
     * @return The files
     * @throws IOException If an error occurs
     */
    public List<GitHubFile> getPullRequestFiles(@NotNull String owner, @NotNull String repository, int number) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/pulls/" + number + "/files";
        
        String response = executeRequest("GET", url, null);
        JsonArray filesArray = GSON.fromJson(response, JsonArray.class);
        
        List<GitHubFile> files = new ArrayList<>();
        for (JsonElement element : filesArray) {
            JsonObject fileObj = element.getAsJsonObject();
            
            String filename = fileObj.get("filename").getAsString();
            String status = fileObj.get("status").getAsString();
            int additions = fileObj.get("additions").getAsInt();
            int deletions = fileObj.get("deletions").getAsInt();
            int changes = fileObj.get("changes").getAsInt();
            
            files.add(new GitHubFile(filename, status, additions, deletions, changes));
        }
        
        return files;
    }
    
    /**
     * Get comments for an issue.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @param number     The issue number
     * @return The comments
     * @throws IOException If an error occurs
     */
    public List<GitHubComment> getIssueComments(@NotNull String owner, @NotNull String repository, int number) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/issues/" + number + "/comments";
        
        String response = executeRequest("GET", url, null);
        JsonArray commentsArray = GSON.fromJson(response, JsonArray.class);
        
        List<GitHubComment> comments = new ArrayList<>();
        for (JsonElement element : commentsArray) {
            JsonObject commentObj = element.getAsJsonObject();
            
            int id = commentObj.get("id").getAsInt();
            String body = commentObj.get("body").getAsString();
            
            comments.add(new GitHubComment(id, body));
        }
        
        return comments;
    }
    
    /**
     * Get comments for a pull request.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @param number     The pull request number
     * @return The comments
     * @throws IOException If an error occurs
     */
    public List<GitHubComment> getPullRequestComments(@NotNull String owner, @NotNull String repository, int number) throws IOException {
        // PR comments are actually issue comments in the GitHub API
        return getIssueComments(owner, repository, number);
    }
    
    /**
     * Create a comment on an issue.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @param number     The issue number
     * @param body       The comment body
     * @throws IOException If an error occurs
     */
    public void createIssueComment(@NotNull String owner, @NotNull String repository, int number, @NotNull String body) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repository + "/issues/" + number + "/comments";
        
        JsonObject json = new JsonObject();
        json.addProperty("body", body);
        
        executeRequest("POST", url, json.toString());
    }
    
    /**
     * Create a comment on a pull request.
     *
     * @param owner      The repository owner
     * @param repository The repository name
     * @param number     The pull request number
     * @param body       The comment body
     * @throws IOException If an error occurs
     */
    public void createPullRequestComment(@NotNull String owner, @NotNull String repository, int number, @NotNull String body) throws IOException {
        // PR comments are actually issue comments in the GitHub API
        createIssueComment(owner, repository, number, body);
    }
    
    /**
     * Execute a request to the GitHub API.
     *
     * @param method The HTTP method
     * @param urlStr The URL
     * @param body   The request body, or null if none
     * @return The response body
     * @throws IOException If an error occurs
     */
    private String executeRequest(@NotNull String method, @NotNull String urlStr, @Nullable String body) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "token " + token);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setDoInput(true);
            
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            int status = connection.getResponseCode();
            
            if (status >= 200 && status < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            } else {
                String errorMessage;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    errorMessage = reader.lines().collect(Collectors.joining("\n"));
                }
                
                if (status == 404) {
                    throw new FileNotFoundException("Resource not found: " + urlStr);
                } else {
                    throw new IOException("HTTP error " + status + ": " + errorMessage);
                }
            }
        } finally {
            connection.disconnect();
        }
    }
}