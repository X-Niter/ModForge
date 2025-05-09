package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an operation on a collaborative editor.
 */
public class EditorOperation {
    // Operation types
    public static final String TYPE_INSERT = "insert";
    public static final String TYPE_DELETE = "delete";
    public static final String TYPE_REPLACE = "replace";
    public static final String TYPE_CURSOR_MOVE = "cursor_move";
    public static final String TYPE_SELECTION_CHANGE = "selection_change";
    
    // Operation ID for ordering and conflict resolution
    @NotNull
    private final String id;
    
    // The user who generated the operation
    @NotNull
    private final String userId;
    
    // The file path this operation applies to
    @NotNull
    private final String filePath;
    
    // Operation type (insert, delete, replace, etc.)
    @NotNull
    private final String type;
    
    // Operation position (offset in the document)
    private final int offset;
    
    // The text to insert, delete, or replace
    @NotNull
    private final String text;
    
    // For replacements, the length of text to replace
    private final int length;
    
    // Timestamp when the operation was created
    private final long timestamp;
    
    /**
     * Creates a new EditorOperation.
     * @param userId The user ID
     * @param filePath The file path
     * @param type The operation type
     * @param offset The offset
     * @param text The text
     * @param length The length
     */
    public EditorOperation(
            @NotNull String userId,
            @NotNull String filePath,
            @NotNull String type,
            int offset,
            @NotNull String text,
            int length
    ) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.filePath = filePath;
        this.type = type;
        this.offset = offset;
        this.text = text;
        this.length = length;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a cursor move operation.
     * @param userId The user ID
     * @param filePath The file path
     * @param offset The offset
     * @return The operation
     */
    @NotNull
    public static EditorOperation createCursorMove(
            @NotNull String userId,
            @NotNull String filePath,
            int offset
    ) {
        return new EditorOperation(userId, filePath, TYPE_CURSOR_MOVE, offset, "", 0);
    }
    
    /**
     * Creates a selection change operation.
     * @param userId The user ID
     * @param filePath The file path
     * @param offset The offset
     * @param length The length
     * @return The operation
     */
    @NotNull
    public static EditorOperation createSelectionChange(
            @NotNull String userId,
            @NotNull String filePath,
            int offset,
            int length
    ) {
        return new EditorOperation(userId, filePath, TYPE_SELECTION_CHANGE, offset, "", length);
    }
    
    /**
     * Creates an insert operation.
     * @param userId The user ID
     * @param filePath The file path
     * @param offset The offset
     * @param text The text to insert
     * @return The operation
     */
    @NotNull
    public static EditorOperation createInsert(
            @NotNull String userId,
            @NotNull String filePath,
            int offset,
            @NotNull String text
    ) {
        return new EditorOperation(userId, filePath, TYPE_INSERT, offset, text, 0);
    }
    
    /**
     * Creates a delete operation.
     * @param userId The user ID
     * @param filePath The file path
     * @param offset The offset
     * @param length The length of text to delete
     * @return The operation
     */
    @NotNull
    public static EditorOperation createDelete(
            @NotNull String userId,
            @NotNull String filePath,
            int offset,
            int length
    ) {
        return new EditorOperation(userId, filePath, TYPE_DELETE, offset, "", length);
    }
    
    /**
     * Creates a replace operation.
     * @param userId The user ID
     * @param filePath The file path
     * @param offset The offset
     * @param text The text to insert
     * @param length The length of text to replace
     * @return The operation
     */
    @NotNull
    public static EditorOperation createReplace(
            @NotNull String userId,
            @NotNull String filePath,
            int offset,
            @NotNull String text,
            int length
    ) {
        return new EditorOperation(userId, filePath, TYPE_REPLACE, offset, text, length);
    }
    
    /**
     * Gets the operation ID.
     * @return The ID
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Gets the user ID.
     * @return The user ID
     */
    @NotNull
    public String getUserId() {
        return userId;
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
     * Gets the operation type.
     * @return The type
     */
    @NotNull
    public String getType() {
        return type;
    }
    
    /**
     * Gets the offset.
     * @return The offset
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * Gets the text.
     * @return The text
     */
    @NotNull
    public String getText() {
        return text;
    }
    
    /**
     * Gets the length.
     * @return The length
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Gets the timestamp.
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format(
                "EditorOperation{type=%s, offset=%d, text='%s', length=%d}",
                type, offset, text, length
        );
    }
}