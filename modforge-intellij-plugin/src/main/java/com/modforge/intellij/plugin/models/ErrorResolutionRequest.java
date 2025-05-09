package com.modforge.intellij.plugin.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Request model for error resolution.
 */
public class ErrorResolutionRequest {
    private String errorMessage;
    private String errorStackTrace;
    private String errorType;
    private String fileContent;
    private String filePath;
    private Integer lineNumber;
    private Integer columnNumber;
    private ModLoaderType modLoader;
    private String minecraftVersion;
    private List<Map<String, String>> projectContext = new ArrayList<>();
    private boolean usePatternLearning = true;
    
    public ErrorResolutionRequest() {
    }
    
    public ErrorResolutionRequest(String errorMessage, String fileContent, String filePath) {
        this.errorMessage = errorMessage;
        this.fileContent = fileContent;
        this.filePath = filePath;
    }
    
    // Getters and setters
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorStackTrace() {
        return errorStackTrace;
    }
    
    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public String getFileContent() {
        return fileContent;
    }
    
    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Integer getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public Integer getColumnNumber() {
        return columnNumber;
    }
    
    public void setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber;
    }
    
    public ModLoaderType getModLoader() {
        return modLoader;
    }
    
    public void setModLoader(ModLoaderType modLoader) {
        this.modLoader = modLoader;
    }
    
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }
    
    public List<Map<String, String>> getProjectContext() {
        return projectContext;
    }
    
    public void setProjectContext(List<Map<String, String>> projectContext) {
        this.projectContext = projectContext;
    }
    
    public void addContextFile(String path, String content) {
        Map<String, String> file = Map.of("path", path, "content", content);
        this.projectContext.add(file);
    }
    
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }
    
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }
}