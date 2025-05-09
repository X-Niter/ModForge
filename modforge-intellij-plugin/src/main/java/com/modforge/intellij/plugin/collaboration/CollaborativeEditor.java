package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides collaborative editing capabilities for a file.
 */
public class CollaborativeEditor implements DocumentListener {
    private static final Logger LOG = Logger.getInstance(CollaborativeEditor.class);
    
    // The project
    private final Project project;
    
    // The file being edited
    private final VirtualFile file;
    
    // The document
    private final Document document;
    
    // User ID of the local user
    private final String userId;
    
    // Reference to the collaboration service
    private final CollaborationService collaborationService;
    
    // Set of operation IDs that have been processed
    private final Set<String> processedOperationIds = ConcurrentHashMap.newKeySet();
    
    // Map of participant cursors and selections
    private final Map<String, ParticipantCursor> participantCursors = new ConcurrentHashMap<>();
    
    /**
     * Represents a participant's cursor and selection.
     */
    private static class ParticipantCursor {
        int offset;
        int selectionStart;
        int selectionEnd;
        
        ParticipantCursor(int offset, int selectionStart, int selectionEnd) {
            this.offset = offset;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
        }
    }
    
    /**
     * Creates a new CollaborativeEditor.
     * @param project The project
     * @param file The file
     * @param userId The user ID
     */
    public CollaborativeEditor(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull String userId
    ) {
        this.project = project;
        this.file = file;
        this.userId = userId;
        this.collaborationService = CollaborationService.getInstance(project);
        
        // Get the document for the file
        this.document = FileDocumentManager.getInstance().getDocument(file);
        
        if (document != null) {
            // Register document listener
            document.addDocumentListener(this);
            
            // Register editor listeners for cursor and selection changes
            registerEditorListeners();
        } else {
            LOG.error("Could not get document for file: " + file.getPath());
        }
    }
    
    /**
     * Registers editor listeners.
     */
    private void registerEditorListeners() {
        // TODO: Register listeners for cursor and selection changes
    }
    
    /**
     * Gets the current editors for the file.
     * @return The editors
     */
    @NotNull
    private Editor[] getEditors() {
        if (document != null) {
            return EditorFactory.getInstance().getEditors(document, project);
        }
        return new Editor[0];
    }
    
    /**
     * Applies an operation to the document.
     * @param operation The operation
     * @param sourceUserId The user ID that generated the operation
     */
    public void applyOperation(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        // Check if the operation has already been processed
        if (processedOperationIds.contains(operation.getId())) {
            return;
        }
        
        // Add to processed operations
        processedOperationIds.add(operation.getId());
        
        // Handle different operation types
        switch (operation.getType()) {
            case EditorOperation.TYPE_CURSOR_MOVE:
                handleCursorMove(operation, sourceUserId);
                break;
                
            case EditorOperation.TYPE_SELECTION_CHANGE:
                handleSelectionChange(operation, sourceUserId);
                break;
                
            case EditorOperation.TYPE_INSERT:
                handleInsert(operation, sourceUserId);
                break;
                
            case EditorOperation.TYPE_DELETE:
                handleDelete(operation, sourceUserId);
                break;
                
            case EditorOperation.TYPE_REPLACE:
                handleReplace(operation, sourceUserId);
                break;
                
            default:
                LOG.warn("Unknown operation type: " + operation.getType());
        }
    }
    
    /**
     * Handles a cursor move operation.
     * @param operation The operation
     * @param sourceUserId The user ID
     */
    private void handleCursorMove(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        // Update participant cursor
        ParticipantCursor cursor = participantCursors.computeIfAbsent(sourceUserId, id -> new ParticipantCursor(0, 0, 0));
        cursor.offset = operation.getOffset();
        
        // TODO: Update cursor visualization in the editor
        LOG.info("Cursor moved for user " + sourceUserId + " to offset " + operation.getOffset());
    }
    
    /**
     * Handles a selection change operation.
     * @param operation The operation
     * @param sourceUserId The user ID
     */
    private void handleSelectionChange(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        // Update participant cursor
        ParticipantCursor cursor = participantCursors.computeIfAbsent(sourceUserId, id -> new ParticipantCursor(0, 0, 0));
        cursor.offset = operation.getOffset();
        cursor.selectionStart = operation.getOffset();
        cursor.selectionEnd = operation.getOffset() + operation.getLength();
        
        // TODO: Update selection visualization in the editor
        LOG.info("Selection changed for user " + sourceUserId + " to " + cursor.selectionStart + "-" + cursor.selectionEnd);
    }
    
    /**
     * Handles an insert operation.
     * @param operation The operation
     * @param sourceUserId The user ID
     */
    private void handleInsert(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        if (document == null) return;
        
        // Apply the insert operation
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    try {
                        document.insertString(operation.getOffset(), operation.getText());
                        LOG.info("Inserted text from user " + sourceUserId + " at offset " + operation.getOffset());
                    } catch (Exception e) {
                        LOG.error("Error applying insert operation", e);
                    }
                }, "Collaborative Edit", null);
            });
        });
    }
    
    /**
     * Handles a delete operation.
     * @param operation The operation
     * @param sourceUserId The user ID
     */
    private void handleDelete(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        if (document == null) return;
        
        // Apply the delete operation
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    try {
                        document.deleteString(operation.getOffset(), operation.getOffset() + operation.getLength());
                        LOG.info("Deleted text from user " + sourceUserId + " at offset " + operation.getOffset());
                    } catch (Exception e) {
                        LOG.error("Error applying delete operation", e);
                    }
                }, "Collaborative Edit", null);
            });
        });
    }
    
    /**
     * Handles a replace operation.
     * @param operation The operation
     * @param sourceUserId The user ID
     */
    private void handleReplace(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        if (document == null) return;
        
        // Apply the replace operation
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    try {
                        document.replaceString(
                                operation.getOffset(),
                                operation.getOffset() + operation.getLength(),
                                operation.getText()
                        );
                        LOG.info("Replaced text from user " + sourceUserId + " at offset " + operation.getOffset());
                    } catch (Exception e) {
                        LOG.error("Error applying replace operation", e);
                    }
                }, "Collaborative Edit", null);
            });
        });
    }
    
    // DocumentListener methods
    
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        // Skip events generated by this editor
        if (processedOperationIds.contains("local:" + event.getOffset() + ":" + event.getOldLength() + ":" + event.getNewLength())) {
            return;
        }
        
        // Create and broadcast the appropriate operation
        EditorOperation operation;
        String text = event.getNewFragment().toString();
        int offset = event.getOffset();
        
        if (event.getOldLength() == 0 && event.getNewLength() > 0) {
            // Insert operation
            operation = EditorOperation.createInsert(userId, file.getPath(), offset, text);
        } else if (event.getOldLength() > 0 && event.getNewLength() == 0) {
            // Delete operation
            operation = EditorOperation.createDelete(userId, file.getPath(), offset, event.getOldLength());
        } else {
            // Replace operation
            operation = EditorOperation.createReplace(userId, file.getPath(), offset, text, event.getOldLength());
        }
        
        // Broadcast the operation
        Map<String, Object> data = new HashMap<>();
        data.put("operation", operation);
        collaborationService.broadcastMessage("operation", data);
        
        // Mark the operation as processed
        processedOperationIds.add(operation.getId());
        processedOperationIds.add("local:" + offset + ":" + event.getOldLength() + ":" + event.getNewLength());
    }
    
    /**
     * Disposes the collaborative editor.
     */
    public void dispose() {
        if (document != null) {
            document.removeDocumentListener(this);
        }
    }
}