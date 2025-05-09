package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an operation on a collaborative editor.
 * Operations are used to synchronize editor content between multiple participants.
 */
public class EditorOperation {
    // Operation types
    public static final String TYPE_INSERT = "insert";
    public static final String TYPE_DELETE = "delete";
    public static final String TYPE_REPLACE = "replace";
    
    /** The operation type. */
    @NotNull
    private final String type;
    
    /** The operation offset. */
    private final int offset;
    
    /** The operation length (for delete and replace operations). */
    private final int length;
    
    /** The operation text (for insert and replace operations). */
    @NotNull
    private final String text;
    
    /** The timestamp when the operation was created. */
    private final long timestamp;
    
    /**
     * Creates a new EditorOperation.
     * @param type The operation type
     * @param offset The operation offset
     * @param length The operation length
     * @param text The operation text
     */
    private EditorOperation(@NotNull String type, int offset, int length, @NotNull String text) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates an insert operation.
     * @param offset The offset at which to insert the text
     * @param text The text to insert
     * @return The insert operation
     */
    @NotNull
    public static EditorOperation createInsertOperation(int offset, @NotNull String text) {
        return new EditorOperation(TYPE_INSERT, offset, 0, text);
    }
    
    /**
     * Creates a delete operation.
     * @param offset The offset at which to delete text
     * @param length The length of the text to delete
     * @return The delete operation
     */
    @NotNull
    public static EditorOperation createDeleteOperation(int offset, int length) {
        return new EditorOperation(TYPE_DELETE, offset, length, "");
    }
    
    /**
     * Creates a replace operation.
     * @param offset The offset at which to replace text
     * @param length The length of the text to replace
     * @param text The replacement text
     * @return The replace operation
     */
    @NotNull
    public static EditorOperation createReplaceOperation(int offset, int length, @NotNull String text) {
        return new EditorOperation(TYPE_REPLACE, offset, length, text);
    }
    
    /**
     * Gets the operation type.
     * @return The operation type
     */
    @NotNull
    public String getType() {
        return type;
    }
    
    /**
     * Gets the operation offset.
     * @return The operation offset
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * Gets the operation length.
     * @return The operation length
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Gets the operation text.
     * @return The operation text
     */
    @NotNull
    public String getText() {
        return text;
    }
    
    /**
     * Gets the operation timestamp.
     * @return The operation timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Converts the operation to a map.
     * @return The operation as a map
     */
    @NotNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("type", type);
        map.put("offset", offset);
        
        if (type.equals(TYPE_DELETE) || type.equals(TYPE_REPLACE)) {
            map.put("length", length);
        }
        
        if (type.equals(TYPE_INSERT) || type.equals(TYPE_REPLACE)) {
            map.put("text", text);
        }
        
        map.put("timestamp", timestamp);
        
        return map;
    }
    
    @Override
    public String toString() {
        return "EditorOperation{" +
                "type='" + type + '\'' +
                ", offset=" + offset +
                ", length=" + length +
                ", text='" + (text.length() > 20 ? text.substring(0, 20) + "..." : text) + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}