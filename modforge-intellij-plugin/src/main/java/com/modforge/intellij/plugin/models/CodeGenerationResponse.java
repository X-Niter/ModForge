package com.modforge.intellij.plugin.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for code generation.
 */
public class CodeGenerationResponse {
    private String code;
    private String explanation;
    private String suggestedFileName;
    private boolean patternMatched;
    private String patternId;
    private List<FileChange> additionalFiles = new ArrayList<>();
    private List<String> requiredDependencies = new ArrayList<>();
    
    public static class FileChange {
        private String filePath;
        private String content;
        private boolean isNew;
        
        public FileChange() {
        }
        
        public FileChange(String filePath, String content, boolean isNew) {
            this.filePath = filePath;
            this.content = content;
            this.isNew = isNew;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public boolean isNew() {
            return isNew;
        }
        
        public void setNew(boolean aNew) {
            isNew = aNew;
        }
    }
    
    public CodeGenerationResponse() {
    }
    
    public CodeGenerationResponse(String code, String explanation) {
        this.code = code;
        this.explanation = explanation;
    }
    
    // Getters and setters
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public String getSuggestedFileName() {
        return suggestedFileName;
    }
    
    public void setSuggestedFileName(String suggestedFileName) {
        this.suggestedFileName = suggestedFileName;
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
    
    public List<FileChange> getAdditionalFiles() {
        return additionalFiles;
    }
    
    public void setAdditionalFiles(List<FileChange> additionalFiles) {
        this.additionalFiles = additionalFiles;
    }
    
    public void addAdditionalFile(String filePath, String content, boolean isNew) {
        this.additionalFiles.add(new FileChange(filePath, content, isNew));
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
}