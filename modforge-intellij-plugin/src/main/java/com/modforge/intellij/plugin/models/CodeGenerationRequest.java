package com.modforge.intellij.plugin.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for code generation.
 */
public class CodeGenerationRequest {
    private String prompt;
    private ModLoaderType modLoader;
    private String minecraftVersion;
    private String language = "java"; // Default is Java
    private List<String> existingFileContents = new ArrayList<>();
    private String projectContext;
    private boolean usePatternLearning = true;
    
    public CodeGenerationRequest() {
    }
    
    public CodeGenerationRequest(String prompt, ModLoaderType modLoader, String minecraftVersion) {
        this.prompt = prompt;
        this.modLoader = modLoader;
        this.minecraftVersion = minecraftVersion;
    }
    
    // Getters and setters
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
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
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public List<String> getExistingFileContents() {
        return existingFileContents;
    }
    
    public void setExistingFileContents(List<String> existingFileContents) {
        this.existingFileContents = existingFileContents;
    }
    
    public void addExistingFileContent(String filePath, String content) {
        this.existingFileContents.add(filePath + ":" + content);
    }
    
    public String getProjectContext() {
        return projectContext;
    }
    
    public void setProjectContext(String projectContext) {
        this.projectContext = projectContext;
    }
    
    public boolean isUsePatternLearning() {
        return usePatternLearning;
    }
    
    public void setUsePatternLearning(boolean usePatternLearning) {
        this.usePatternLearning = usePatternLearning;
    }
}