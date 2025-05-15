package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an operation performed in an editor.
 * This class is used for real-time collaboration to sync editor changes between
 * participants.
 */
public class EditorOperation {
    /** The type of operation. */
    @NotNull
    public final OperationType type;

    /** The start offset in the document. */
    public final int offset;

    /** The length of affected text for deletions. */
    public final int length;

    /** The text for insertions. */
    @Nullable
    public final String text;

    /**
     * Enum representing types of editor operations.
     */
    public enum OperationType {
        /** Insert text at a position. */
        INSERT,

        /** Delete text from a position. */
        DELETE,

        /** Replace text at a position. */
        REPLACE
    }

    /**
     * Creates an insert operation.
     * 
     * @param offset The offset to insert at
     * @param text   The text to insert
     * @return The operation
     */
    public static EditorOperation createInsertOperation(int offset, @NotNull String text) {
        return new EditorOperation(OperationType.INSERT, offset, 0, text);
    }

    /**
     * Creates a delete operation.
     * 
     * @param offset The offset to delete from
     * @param length The length of text to delete
     * @return The operation
     */
    public static EditorOperation createDeleteOperation(int offset, int length) {
        return new EditorOperation(OperationType.DELETE, offset, length, null);
    }

    /**
     * Creates a replace operation.
     * 
     * @param offset The offset to replace at
     * @param length The length of text to replace
     * @param text   The replacement text
     * @return The operation
     */
    public static EditorOperation createReplaceOperation(int offset, int length, @NotNull String text) {
        return new EditorOperation(OperationType.REPLACE, offset, length, text);
    }

    /**
     * Creates a new EditorOperation.
     * 
     * @param type   The operation type
     * @param offset The offset
     * @param length The length
     * @param text   The text
     */
    private EditorOperation(@NotNull OperationType type, int offset, int length, @Nullable String text) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.text = text;
    }

    /**
     * Retrieves the timestamp of the operation.
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return System.currentTimeMillis(); // Example implementation
    }

    /**
     * Creates an EditorOperation from a map.
     *
     * @param map The map containing operation data
     * @return The EditorOperation instance
     */
    public static EditorOperation fromMap(@NotNull Map<String, Object> map) {
        OperationType type = OperationType.valueOf((String) map.get("type"));
        int offset = (int) map.get("offset");
        int length = (int) map.getOrDefault("length", 0);
        String text = (String) map.getOrDefault("text", null);
        return new EditorOperation(type, offset, length, text);
    }

    /**
     * Converts the EditorOperation to a map.
     *
     * @return A map representation of the operation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("offset", offset);
        if (length > 0) {
            map.put("length", length);
        }
        if (text != null) {
            map.put("text", text);
        }
        return map;
    }

    /**
     * Gets the type of the operation.
     *
     * @return The operation type
     */
    public OperationType getType() {
        return type;
    }

    /**
     * Gets the offset of the operation.
     *
     * @return The offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the length of the operation.
     *
     * @return The length
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the text of the operation.
     *
     * @return The text, or null if not applicable
     */
    @Nullable
    public String getText() {
        return text;
    }

    /**
     * Constants for operation types.
     */
    public static final String TYPE_DELETE = "DELETE";
    public static final String TYPE_REPLACE = "REPLACE";

    /**
     * Enum constant for insert operation type.
     */
    public static final String TYPE_INSERT = "INSERT";
}