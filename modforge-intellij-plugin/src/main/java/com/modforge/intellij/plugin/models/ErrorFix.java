package com.modforge.intellij.plugin.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a fix for an error.
 */
public class ErrorFix {
    private String fixedCode;
    private String explanation;
    private String filePath;
    private int line;
    private int column;
    private List<AdditionalFile> additionalFiles;
    private boolean requiresRecompile;
    
    /**
     * Creates a new ErrorFix.
     */
    public ErrorFix() {
        this.additionalFiles = new ArrayList<>();
        this.requiresRecompile = false;
    }
    
    /**
     * Creates a new ErrorFix with the specified parameters.
     * @param fixedCode The fixed code
     * @param explanation The explanation
     * @param filePath The file path
     * @param line The line number
     * @param column The column number
     */
    public ErrorFix(@NotNull String fixedCode, @Nullable String explanation, @NotNull String filePath, int line, int column) {
        this();
        this.fixedCode = fixedCode;
        this.explanation = explanation;
        this.filePath = filePath;
        this.line = line;
        this.column = column;
    }
    
    /**
     * Represents an additional file to be created or updated.
     */
    public static class AdditionalFile {
        private String filePath;
        private String content;
        private boolean isNew;
        
        /**
         * Creates a new AdditionalFile.
         */
        public AdditionalFile() {
        }
        
        /**
         * Creates a new AdditionalFile with the specified parameters.
         * @param filePath The file path
         * @param content The content
         * @param isNew Whether the file is new
         */
        public AdditionalFile(@NotNull String filePath, @NotNull String content, boolean isNew) {
            this.filePath = filePath;
            this.content = content;
            this.isNew = isNew;
        }
        
        /**
         * Gets the file path.
         * @return The file path
         */
        @NotNull
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * Sets the file path.
         * @param filePath The file path
         */
        public void setFilePath(@NotNull String filePath) {
            this.filePath = filePath;
        }
        
        /**
         * Gets the content.
         * @return The content
         */
        @NotNull
        public String getContent() {
            return content;
        }
        
        /**
         * Sets the content.
         * @param content The content
         */
        public void setContent(@NotNull String content) {
            this.content = content;
        }
        
        /**
         * Gets whether the file is new.
         * @return Whether the file is new
         */
        public boolean isNew() {
            return isNew;
        }
        
        /**
         * Sets whether the file is new.
         * @param isNew Whether the file is new
         */
        public void setNew(boolean isNew) {
            this.isNew = isNew;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdditionalFile that = (AdditionalFile) o;
            return isNew == that.isNew && 
                    Objects.equals(filePath, that.filePath) && 
                    Objects.equals(content, that.content);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(filePath, content, isNew);
        }
    }
    
    /**
     * Gets the fixed code.
     * @return The fixed code
     */
    @NotNull
    public String getFixedCode() {
        return fixedCode;
    }
    
    /**
     * Sets the fixed code.
     * @param fixedCode The fixed code
     */
    public void setFixedCode(@NotNull String fixedCode) {
        this.fixedCode = fixedCode;
    }
    
    /**
     * Gets the explanation.
     * @return The explanation
     */
    @Nullable
    public String getExplanation() {
        return explanation;
    }
    
    /**
     * Sets the explanation.
     * @param explanation The explanation
     */
    public void setExplanation(@Nullable String explanation) {
        this.explanation = explanation;
    }
    
    /**
     * Gets the file path.
     * @return The file path
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Sets the file path.
     * @param filePath The file path
     */
    public void setFilePath(@NotNull String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Gets the line number.
     * @return The line number
     */
    public int getLine() {
        return line;
    }
    
    /**
     * Sets the line number.
     * @param line The line number
     */
    public void setLine(int line) {
        this.line = line;
    }
    
    /**
     * Gets the column number.
     * @return The column number
     */
    public int getColumn() {
        return column;
    }
    
    /**
     * Sets the column number.
     * @param column The column number
     */
    public void setColumn(int column) {
        this.column = column;
    }
    
    /**
     * Gets the additional files.
     * @return The additional files
     */
    @NotNull
    public List<AdditionalFile> getAdditionalFiles() {
        return additionalFiles;
    }
    
    /**
     * Sets the additional files.
     * @param additionalFiles The additional files
     */
    public void setAdditionalFiles(@NotNull List<AdditionalFile> additionalFiles) {
        this.additionalFiles = additionalFiles;
    }
    
    /**
     * Adds an additional file.
     * @param filePath The file path
     * @param content The content
     * @param isNew Whether the file is new
     */
    public void addAdditionalFile(@NotNull String filePath, @NotNull String content, boolean isNew) {
        this.additionalFiles.add(new AdditionalFile(filePath, content, isNew));
    }
    
    /**
     * Gets whether the fix requires recompilation.
     * @return Whether the fix requires recompilation
     */
    public boolean isRequiresRecompile() {
        return requiresRecompile;
    }
    
    /**
     * Sets whether the fix requires recompilation.
     * @param requiresRecompile Whether the fix requires recompilation
     */
    public void setRequiresRecompile(boolean requiresRecompile) {
        this.requiresRecompile = requiresRecompile;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorFix errorFix = (ErrorFix) o;
        return line == errorFix.line && 
                column == errorFix.column && 
                requiresRecompile == errorFix.requiresRecompile && 
                Objects.equals(fixedCode, errorFix.fixedCode) && 
                Objects.equals(explanation, errorFix.explanation) && 
                Objects.equals(filePath, errorFix.filePath) && 
                Objects.equals(additionalFiles, errorFix.additionalFiles);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fixedCode, explanation, filePath, line, column, additionalFiles, requiresRecompile);
    }
}