package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Operation for collaborative editing.
 */
public class EditorOperation {
    // Operation types
    public static final String TYPE_INSERT = "insert";
    public static final String TYPE_DELETE = "delete";
    public static final String TYPE_REPLACE = "replace";
    
    private final String type;
    private final int offset;
    private final int length;
    private final String text;
    private final long timestamp;
    
    /**
     * Creates a new EditorOperation.
     * @param type The operation type
     * @param offset The operation offset
     * @param length The operation length
     * @param text The operation text
     */
    public EditorOperation(@NotNull String type, int offset, int length, @NotNull String text) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a new EditorOperation.
     * @param type The operation type
     * @param offset The operation offset
     * @param length The operation length
     * @param text The operation text
     * @param timestamp The operation timestamp
     */
    public EditorOperation(@NotNull String type, int offset, int length, @NotNull String text, long timestamp) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.text = text;
        this.timestamp = timestamp;
    }
    
    /**
     * Creates an insert operation.
     * @param offset The offset to insert at
     * @param text The text to insert
     * @return The insert operation
     */
    @NotNull
    public static EditorOperation createInsertOperation(int offset, @NotNull String text) {
        return new EditorOperation(TYPE_INSERT, offset, 0, text);
    }
    
    /**
     * Creates a delete operation.
     * @param offset The offset to delete from
     * @param length The number of characters to delete
     * @return The delete operation
     */
    @NotNull
    public static EditorOperation createDeleteOperation(int offset, int length) {
        return new EditorOperation(TYPE_DELETE, offset, length, "");
    }
    
    /**
     * Creates a replace operation.
     * @param offset The offset to replace at
     * @param length The number of characters to replace
     * @param text The text to replace with
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
     * @return The map representation of the operation
     */
    @NotNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("offset", offset);
        map.put("length", length);
        map.put("text", text);
        map.put("timestamp", timestamp);
        
        return map;
    }
    
    /**
     * Creates an operation from a map.
     * @param map The map to create the operation from
     * @return The operation
     */
    @NotNull
    public static EditorOperation fromMap(@NotNull Map<String, Object> map) {
        String type = (String) map.get("type");
        int offset = ((Number) map.get("offset")).intValue();
        int length = ((Number) map.get("length")).intValue();
        String text = (String) map.get("text");
        long timestamp = ((Number) map.get("timestamp")).longValue();
        
        return new EditorOperation(type, offset, length, text, timestamp);
    }
}