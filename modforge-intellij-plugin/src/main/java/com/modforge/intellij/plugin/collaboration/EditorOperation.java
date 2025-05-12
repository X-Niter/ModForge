package com.modforge.intellij.plugin.collaboration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an operation performed in an editor.
 * This class is used for real-time collaboration to sync editor changes between participants.
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
     * @param text The text to insert
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
     * @param text The replacement text
     * @return The operation
     */
    public static EditorOperation createReplaceOperation(int offset, int length, @NotNull String text) {
        return new EditorOperation(OperationType.REPLACE, offset, length, text);
    }
    
    /**
     * Creates a new EditorOperation.
     * 
     * @param type The operation type
     * @param offset The offset
     * @param length The length
     * @param text The text
     */
    private EditorOperation(@NotNull OperationType type, int offset, int length, @Nullable String text) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.text = text;
    }
}