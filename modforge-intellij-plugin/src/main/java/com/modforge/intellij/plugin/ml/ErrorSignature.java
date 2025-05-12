package com.modforge.intellij.plugin.ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a signature for an error.
 * Used for pattern matching to find similar errors.
 */
public class ErrorSignature {
    private String errorMessage;
    private String filePath;
    private int line;
    private int column;
    private String fileContent;
    private String errorType;
    private String errorContext;
    private Set<String> keywords = new HashSet<>();
    
    // TODO: Implement machine learning-based error similarity detection for more accurate pattern matching
    
    private static final Gson GSON = new GsonBuilder().create();
    
    // Regular expressions for normalizing error messages
    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile("\\b(?:line|at)\\s+\\d+\\b");
    private static final Pattern COLUMN_PATTERN = Pattern.compile("\\bcolumn\\s+\\d+\\b");
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("\\b[a-zA-Z]:[/\\\\][^\\s:;,]*\\b|\\b/[^\\s:;,]*\\b");
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("\\b[a-z]+(\\.[a-z]+)+\\b");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\b[A-Z][a-zA-Z0-9_$]*\\b");
    
    // Error type patterns
    private static final Map<String, Pattern> ERROR_TYPE_PATTERNS = new HashMap<>();
    static {
        // Java compilation errors
        ERROR_TYPE_PATTERNS.put("syntax_error", Pattern.compile("(?i)\\b(syntax error|unexpected token|illegal start of expression)\\b"));
        ERROR_TYPE_PATTERNS.put("unresolved_symbol", Pattern.compile("(?i)\\b(cannot find symbol|cannot resolve symbol|symbol not found)\\b"));
        ERROR_TYPE_PATTERNS.put("type_mismatch", Pattern.compile("(?i)\\b(incompatible types|inconvertible types|bad type|required type|cannot be applied to)\\b"));
        ERROR_TYPE_PATTERNS.put("missing_method", Pattern.compile("(?i)\\b(method .* not found|no suitable method|cannot find method)\\b"));
        ERROR_TYPE_PATTERNS.put("missing_class", Pattern.compile("(?i)\\b(class .* not found|cannot find class|no such class)\\b"));
        ERROR_TYPE_PATTERNS.put("access_control", Pattern.compile("(?i)\\b(is not accessible|has private access|protected access|not visible)\\b"));
        ERROR_TYPE_PATTERNS.put("duplicate_declaration", Pattern.compile("(?i)\\b(duplicate|variable .* already defined|class .* already exists)\\b"));
        ERROR_TYPE_PATTERNS.put("unchecked_exception", Pattern.compile("(?i)\\b(unreported exception|unhandled exception|must be caught or declared)\\b"));
        
        // Minecraft-specific errors
        ERROR_TYPE_PATTERNS.put("mixin_error", Pattern.compile("(?i)\\b(mixin|mixin application|failed to apply mixin)\\b"));
        ERROR_TYPE_PATTERNS.put("forge_registry", Pattern.compile("(?i)\\b(forge registry|registry.*object|register.*item|register.*block)\\b"));
        ERROR_TYPE_PATTERNS.put("mod_loading", Pattern.compile("(?i)\\b(mod loading|loading error|initializing mod|constructing mod)\\b"));
        ERROR_TYPE_PATTERNS.put("missing_annotation", Pattern.compile("(?i)\\b(missing.*annotation|requires.*annotation|@.*expected)\\b"));
        ERROR_TYPE_PATTERNS.put("client_server_side", Pattern.compile("(?i)\\b(client side|server side|dedicated server|side.*only)\\b"));
        ERROR_TYPE_PATTERNS.put("obfuscation", Pattern.compile("(?i)\\b(obfuscation|srg name|mapped name|remapped|unmapped)\\b"));
    }
    
    /**
     * Creates a new ErrorSignature.
     */
    public ErrorSignature() {
    }
    
    /**
     * Creates a new ErrorSignature with the specified parameters.
     * @param errorMessage The error message
     * @param filePath The file path
     * @param line The line number
     * @param column The column number
     * @param fileContent The file content
     */
    public ErrorSignature(String errorMessage, String filePath, int line, int column, String fileContent) {
        this.errorMessage = errorMessage;
        this.filePath = filePath;
        this.line = line;
        this.column = column;
        this.fileContent = fileContent;
        
        // Derive error type and context
        this.errorType = deriveErrorType(errorMessage);
        this.errorContext = extractErrorContext(fileContent, line, column);
        this.keywords = extractKeywords(errorMessage);
    }
    
    /**
     * Extracts the error context from the file content.
     * @param fileContent The file content
     * @param line The line number
     * @param column The column number
     * @return The error context
     */
    private String extractErrorContext(String fileContent, int line, int column) {
        if (fileContent == null || fileContent.isEmpty()) {
            return "";
        }
        
        try {
            String[] lines = fileContent.split("\\r?\\n");
            
            // Adjust for 0-based indexing
            int lineIndex = line - 1;
            if (lineIndex < 0 || lineIndex >= lines.length) {
                return "";
            }
            
            // Get 3 lines before and after the error line
            int startLine = Math.max(0, lineIndex - 3);
            int endLine = Math.min(lines.length - 1, lineIndex + 3);
            
            StringBuilder context = new StringBuilder();
            for (int i = startLine; i <= endLine; i++) {
                if (i == lineIndex) {
                    // Mark the error line
                    context.append("> ");
                } else {
                    context.append("  ");
                }
                context.append(lines[i]).append("\n");
            }
            
            return context.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Derives the error type from the error message.
     * @param errorMessage The error message
     * @return The error type
     */
    private String deriveErrorType(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "unknown";
        }
        
        // Check against known error type patterns
        for (Map.Entry<String, Pattern> entry : ERROR_TYPE_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(errorMessage).find()) {
                return entry.getKey();
            }
        }
        
        // Check for common error keywords
        if (errorMessage.matches("(?i).*\\bnull\\b.*")) {
            return "null_pointer";
        } else if (errorMessage.matches("(?i).*\\bindexoutofbounds\\b.*")) {
            return "array_index";
        } else if (errorMessage.matches("(?i).*\\billegalargument\\b.*")) {
            return "illegal_argument";
        } else if (errorMessage.matches("(?i).*\\bclasscast\\b.*")) {
            return "class_cast";
        }
        
        return "unknown";
    }
    
    /**
     * Extracts keywords from the error message.
     * @param errorMessage The error message
     * @return The keywords
     */
    private Set<String> extractKeywords(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> keywords = new HashSet<>();
        
        // Extract class names
        Matcher classMatcher = CLASS_NAME_PATTERN.matcher(errorMessage);
        while (classMatcher.find()) {
            keywords.add(classMatcher.group());
        }
        
        // Extract package names
        Matcher packageMatcher = PACKAGE_NAME_PATTERN.matcher(errorMessage);
        while (packageMatcher.find()) {
            keywords.add(packageMatcher.group());
        }
        
        // Add the error type as a keyword
        keywords.add(errorType);
        
        // Add common words from the error message, filtering out stopping words
        String[] words = errorMessage.split("\\s+");
        for (String word : words) {
            word = word.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (word.length() > 3 && !isStopWord(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
    
    /**
     * Checks if the specified word is a stop word.
     * @param word The word to check
     * @return Whether the word is a stop word
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "the", "and", "for", "with", "this", "that", "from", "not", "have", "has",
                "error", "exception", "caused", "found", "line", "column", "file", "code", "java"));
        return stopWords.contains(word);
    }
    
    /**
     * Normalizes the error message for better matching.
     * @param errorMessage The error message
     * @return The normalized error message
     */
    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "";
        }
        
        String normalized = errorMessage;
        
        // Replace line numbers
        normalized = LINE_NUMBER_PATTERN.matcher(normalized).replaceAll("line XXX");
        
        // Replace column numbers
        normalized = COLUMN_PATTERN.matcher(normalized).replaceAll("column XXX");
        
        // Replace file paths
        normalized = FILE_PATH_PATTERN.matcher(normalized).replaceAll("PATH");
        
        // Make lowercase
        normalized = normalized.toLowerCase();
        
        return normalized;
    }
    
    /**
     * Calculates the similarity between this error signature and another.
     * @param other The other error signature
     * @return The similarity, between 0 and 1
     */
    public double calculateSimilarity(ErrorSignature other) {
        // Weight factors for different components
        double messageWeight = 0.5;
        double typeWeight = 0.3;
        double contextWeight = 0.2;
        
        // Calculate similarity for each component
        double messageSimilarity = calculateMessageSimilarity(other);
        double typeSimilarity = errorType.equals(other.errorType) ? 1.0 : 0.0;
        double contextSimilarity = calculateContextSimilarity(other);
        
        // Calculate weighted average
        return (messageSimilarity * messageWeight) +
               (typeSimilarity * typeWeight) +
               (contextSimilarity * contextWeight);
    }
    
    /**
     * Calculates the similarity between error messages.
     * @param other The other error signature
     * @return The similarity, between 0 and 1
     */
    private double calculateMessageSimilarity(ErrorSignature other) {
        // Normalize both messages
        String normalizedThis = normalizeErrorMessage(this.errorMessage);
        String normalizedOther = normalizeErrorMessage(other.errorMessage);
        
        if (normalizedThis.isEmpty() || normalizedOther.isEmpty()) {
            return 0.0;
        }
        
        // Return Levenshtein similarity
        return 1.0 - (double) StringUtils.getLevenshteinDistance(normalizedThis, normalizedOther) / 
                Math.max(normalizedThis.length(), normalizedOther.length());
    }
    
    /**
     * Calculates the similarity between error contexts.
     * @param other The other error signature
     * @return The similarity, between 0 and 1
     */
    private double calculateContextSimilarity(ErrorSignature other) {
        if (this.errorContext == null || other.errorContext == null ||
            this.errorContext.isEmpty() || other.errorContext.isEmpty()) {
            return 0.0;
        }
        
        // Simple comparison based on line content
        String[] thisLines = this.errorContext.split("\\r?\\n");
        String[] otherLines = other.errorContext.split("\\r?\\n");
        
        int matchingLines = 0;
        for (String thisLine : thisLines) {
            for (String otherLine : otherLines) {
                if (thisLine.trim().equals(otherLine.trim())) {
                    matchingLines++;
                    break;
                }
            }
        }
        
        return (double) matchingLines / Math.max(thisLines.length, otherLines.length);
    }
    
    /**
     * Calculates the similarity between keyword sets.
     * @param other The other error signature
     * @return The similarity, between 0 and 1
     */
    private double calculateKeywordSimilarity(ErrorSignature other) {
        if (this.keywords.isEmpty() || other.keywords.isEmpty()) {
            return 0.0;
        }
        
        // Calculate Jaccard similarity
        Set<String> intersection = new HashSet<>(this.keywords);
        intersection.retainAll(other.keywords);
        
        Set<String> union = new HashSet<>(this.keywords);
        union.addAll(other.keywords);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Serializes this error signature to JSON.
     * @return The serialized error signature
     */
    public String serialize() {
        return GSON.toJson(this);
    }
    
    /**
     * Deserializes an error signature from JSON.
     * @param json The JSON string
     * @return The deserialized error signature or null if there was an error
     */
    @Nullable
    public static ErrorSignature deserialize(@NotNull String json) {
        try {
            return GSON.fromJson(json, ErrorSignature.class);
        } catch (Exception e) {
            // Log error and return null
            com.intellij.openapi.diagnostic.Logger.getInstance(ErrorSignature.class)
                .warn("Failed to deserialize error signature: " + e.getMessage());
            return null;
        }
    }
    
    // Getters and setters
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public int getLine() {
        return line;
    }
    
    public void setLine(int line) {
        this.line = line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public void setColumn(int column) {
        this.column = column;
    }
    
    public String getFileContent() {
        return fileContent;
    }
    
    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public String getErrorContext() {
        return errorContext;
    }
    
    public void setErrorContext(String errorContext) {
        this.errorContext = errorContext;
    }
    
    public Set<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }
}