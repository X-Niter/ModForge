package com.modforge.intellij.plugin.models;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a problem or error in the code.
 * This class provides a compatibility layer for different IntelliJ versions.
 */
public class Problem {
    private final String description;
    private final VirtualFile file;
    private final PsiElement psiElement;
    private final int line;
    private final int column;
    private final String severity;
    
    /**
     * Creates a new problem.
     *
     * @param description The problem description
     * @param file The file containing the problem
     * @param psiElement The PSI element associated with the problem
     * @param line The line number
     * @param column The column number
     * @param severity The severity level
     */
    public Problem(
            @NotNull String description,
            @Nullable VirtualFile file,
            @Nullable PsiElement psiElement,
            int line,
            int column,
            @NotNull String severity) {
        this.description = description;
        this.file = file;
        this.psiElement = psiElement;
        this.line = line;
        this.column = column;
        this.severity = severity;
    }
    
    /**
     * Gets the problem description.
     *
     * @return The description
     */
    @NotNull
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the file containing the problem.
     *
     * @return The file, or null if not available
     */
    @Nullable
    public VirtualFile getFile() {
        return file;
    }
    
    /**
     * Gets the PSI element associated with the problem.
     *
     * @return The PSI element, or null if not available
     */
    @Nullable
    public PsiElement getPsiElement() {
        return psiElement;
    }
    
    /**
     * Gets the line number.
     *
     * @return The line number
     */
    public int getLine() {
        return line;
    }
    
    /**
     * Gets the column number.
     *
     * @return The column number
     */
    public int getColumn() {
        return column;
    }
    
    /**
     * Gets the severity level.
     *
     * @return The severity level
     */
    @NotNull
    public String getSeverity() {
        return severity;
    }
    
    /**
     * Creates a builder for a problem.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for problems.
     */
    public static class Builder {
        private String description = "Unknown problem";
        private VirtualFile file = null;
        private PsiElement psiElement = null;
        private int line = -1;
        private int column = -1;
        private String severity = "ERROR";
        
        /**
         * Sets the description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the file.
         *
         * @param file The file
         * @return This builder
         */
        public Builder setFile(VirtualFile file) {
            this.file = file;
            return this;
        }
        
        /**
         * Sets the PSI element.
         *
         * @param psiElement The PSI element
         * @return This builder
         */
        public Builder setPsiElement(PsiElement psiElement) {
            this.psiElement = psiElement;
            return this;
        }
        
        /**
         * Sets the line number.
         *
         * @param line The line number
         * @return This builder
         */
        public Builder setLine(int line) {
            this.line = line;
            return this;
        }
        
        /**
         * Sets the column number.
         *
         * @param column The column number
         * @return This builder
         */
        public Builder setColumn(int column) {
            this.column = column;
            return this;
        }
        
        /**
         * Sets the severity level.
         *
         * @param severity The severity level
         * @return This builder
         */
        public Builder setSeverity(String severity) {
            this.severity = severity;
            return this;
        }
        
        /**
         * Builds the problem.
         *
         * @return A new problem
         */
        public Problem build() {
            return new Problem(description, file, psiElement, line, column, severity);
        }
    }
}