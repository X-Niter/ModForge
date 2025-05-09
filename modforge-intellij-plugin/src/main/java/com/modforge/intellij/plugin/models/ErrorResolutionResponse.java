package com.modforge.intellij.plugin.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for error resolution.
 */
public class ErrorResolutionResponse {
    private String fixedCode;
    private String explanation;
    private List<FileChange> additionalFiles = new ArrayList<>();
    private boolean requiresProjectReload;
    private List<String> requiredDependencies = new ArrayList<>();
    private boolean patternMatched;
    private String patternId;
    
    public static class FileChange {
        private String filePath;
        private String originalContent;
        private String newContent;
        private boolean isNew;
        
        public FileChange() {
        }
        
        public FileChange(String filePath, String originalContent, String newContent, boolean isNew) {
            this.filePath = filePath;
            this.originalContent = originalContent;
            this.newContent = newContent;
            this.isNew = isNew;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getOriginalContent() {
            return originalContent;
        }
        
        public void setOriginalContent(String originalContent) {
            this.originalContent = originalContent;
        }
        
        public String getNewContent() {
            return newContent;
        }
        
        public void setNewContent(String newContent) {
            this.newContent = newContent;
        }
        
        public boolean isNew() {
            return isNew;
        }
        
        public void setNew(boolean aNew) {
            isNew = aNew;
        }
    }
    
    public ErrorResolutionResponse() {
    }
    
    public ErrorResolutionResponse(String fixedCode, String explanation) {
        this.fixedCode = fixedCode;
        this.explanation = explanation;
    }
    
    // Getters and setters
    
    public String getFixedCode() {
        return fixedCode;
    }
    
    public void setFixedCode(String fixedCode) {
        this.fixedCode = fixedCode;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public List<FileChange> getAdditionalFiles() {
        return additionalFiles;
    }
    
    public void setAdditionalFiles(List<FileChange> additionalFiles) {
        this.additionalFiles = additionalFiles;
    }
    
    public void addAdditionalFile(String filePath, String originalContent, String newContent, boolean isNew) {
        this.additionalFiles.add(new FileChange(filePath, originalContent, newContent, isNew));
    }
    
    public boolean isRequiresProjectReload() {
        return requiresProjectReload;
    }
    
    public void setRequiresProjectReload(boolean requiresProjectReload) {
        this.requiresProjectReload = requiresProjectReload;
    }
    
    public List<String> getRequiredDependencies() {
        return requiredDependencies;
    }
    
    public void setRequiredDependencies(List<String> requiredDependencies) {
        this.requiredDependencies = requiredDependencies;
    }
    
    public void addRequiredDependency(String dependency) {
        this.requiredDependencies.add(dependency);
    }
    
    public boolean isPatternMatched() {
        return patternMatched;
    }
    
    public void setPatternMatched(boolean patternMatched) {
        this.patternMatched = patternMatched;
    }
    
    public String getPatternId() {
        return patternId;
    }
    
    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }
}