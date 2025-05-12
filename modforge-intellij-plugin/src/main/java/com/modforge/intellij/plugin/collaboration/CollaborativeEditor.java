package com.modforge.intellij.plugin.collaboration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Editor for collaborative editing.
 * This class manages a document and applies operations from remote users.
 */
public class CollaborativeEditor {
    private static final Logger LOG = Logger.getInstance(CollaborativeEditor.class);
    
    private final Project project;
    private final VirtualFile file;
    private final String userId;
    private final Document document;
    private final Map<String, Long> lastOperationTimestamps = new ConcurrentHashMap<>();
    private final AtomicBoolean ignoreLocalChanges = new AtomicBoolean(false);
    
    /**
     * Creates a new CollaborativeEditor.
     * @param project The project
     * @param file The file
     * @param userId The user ID
     */
    public CollaborativeEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull String userId) {
        this.project = project;
        this.file = file;
        this.userId = userId;
        
        // Get document
        Document doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null) {
            throw new IllegalArgumentException("Could not get document for file: " + file.getPath());
        }
        
        this.document = doc;
        
        // Add document listener
        document.addDocumentListener(new DocumentChangeListener());
        
        LOG.info("Created collaborative editor for file: " + file.getPath());
    }
    
    /**
     * Gets the document.
     * @return The document
     */
    @NotNull
    public Document getDocument() {
        return document;
    }
    
    /**
     * Gets the file.
     * @return The file
     */
    @NotNull
    public VirtualFile getFile() {
        return file;
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
     * Applies an operation from a remote user.
     * @param operation The operation to apply
     * @param fromUserId The user ID of the user who created the operation
     */
    public void applyOperation(@NotNull EditorOperation operation, @NotNull String userId) {
        // Ignore own operations
        if (userId.equals(this.userId)) {
            LOG.debug("Ignoring own operation");
            return;
        }
        
        // Check if operation is newer than last received from this user
        Long lastTimestamp = lastOperationTimestamps.get(userId);
        if (lastTimestamp != null && lastTimestamp >= operation.getTimestamp()) {
            LOG.debug("Ignoring older operation from user: " + userId);
            return;
        }
        
        // Update last operation timestamp
        lastOperationTimestamps.put(userId, operation.getTimestamp());
        
        LOG.debug("Applying operation from user: " + userId);
        
        // Apply operation
        ignoreLocalChanges.set(true);
        
        try {
            applyOperationInternal(operation);
        } finally {
            ignoreLocalChanges.set(false);
        }
    }
    
    /**
     * Applies an operation internally.
     * @param operation The operation to apply
     */
    private void applyOperationInternal(@NotNull EditorOperation operation) {
        Runnable runnable = () -> {
            try {
                switch (operation.getType()) {
                    case EditorOperation.TYPE_INSERT:
                        document.insertString(operation.getOffset(), operation.getText());
                        break;
                        
                    case EditorOperation.TYPE_DELETE:
                        document.deleteString(operation.getOffset(), operation.getOffset() + operation.getLength());
                        break;
                        
                    case EditorOperation.TYPE_REPLACE:
                        document.replaceString(
                                operation.getOffset(),
                                operation.getOffset() + operation.getLength(),
                                operation.getText()
                        );
                        break;
                        
                    default:
                        LOG.warn("Unknown operation type: " + operation.getType());
                        break;
                }
            } catch (Exception e) {
                LOG.error("Error applying operation", e);
            }
        };
        
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> ApplicationManager.getApplication().runWriteAction(runnable),
                    "Collaborative Edit",
                    null
            );
        });
    }
    
    /**
     * Gets the caret position.
     * @return The caret position, or -1 if not available
     */
    public int getCaretPosition() {
        Editor editor = getEditor();
        if (editor == null) {
            return -1;
        }
        
        return editor.getCaretModel().getOffset();
    }
    
    /**
     * Gets the editor.
     * @return The editor, or null if not available
     */
    @Nullable
    private Editor getEditor() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        Editor[] editors = fileEditorManager.getEditors(file);
        
        if (editors.length == 0) {
            return null;
        }
        
        return editors[0];
    }
    
    /**
     * Document change listener.
     */
    private class DocumentChangeListener implements DocumentListener {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            // Ignore changes when applying remote operations
            if (ignoreLocalChanges.get()) {
                return;
            }
            
            // Get collaboration service
            CollaborationService collaborationService = CollaborationService.getInstance(project);
            if (!collaborationService.isConnected()) {
                return;
            }
            
            String eventType = event.getType().toString();
            Document changedDocument = event.getDocument();
            
            // Check if this is our document
            if (!changedDocument.equals(document)) {
                return;
            }
            
            // Get operation details
            EditorOperation operation;
            
            if (event.getNewLength() > 0 && event.getOldLength() == 0) {
                // Insert
                operation = EditorOperation.createInsertOperation(
                        event.getOffset(),
                        event.getNewFragment().toString()
                );
            } else if (event.getNewLength() == 0 && event.getOldLength() > 0) {
                // Delete
                operation = EditorOperation.createDeleteOperation(
                        event.getOffset(),
                        event.getOldLength()
                );
            } else {
                // Replace
                operation = EditorOperation.createReplaceOperation(
                        event.getOffset(),
                        event.getOldLength(),
                        event.getNewFragment().toString()
                );
            }
            
            // Send operation to other users
            Map<String, Object> data = new HashMap<>();
            data.put("filePath", file.getPath());
            data.put("operation", operation.toMap());
            
            collaborationService.sendMessage(WebSocketMessage.TYPE_OPERATION, data);
        }
    }
}