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
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class for collaborative editing of a file.
 * Handles operations and transformations for real-time collaboration.
 */
public class CollaborativeEditor {
    private static final Logger LOG = Logger.getInstance(CollaborativeEditor.class);
    
    private final Project project;
    private final VirtualFile file;
    private final String userId;
    private final Document document;
    private final AtomicBoolean isApplyingOperation = new AtomicBoolean(false);
    private final Set<String> operationsSeen = new HashSet<>();
    
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
        this.document = ApplicationManager.getApplication().runReadAction(
                (Computable<Document>) () -> FileDocumentManager.getInstance().getDocument(file)
        );
        
        // Add document listener
        if (document != null) {
            document.addDocumentListener(new DocumentChangeListener());
        }
        
        LOG.info("Created collaborative editor for file: " + file.getPath());
    }
    
    /**
     * Applies an operation from another user.
     * @param operation The operation
     * @param sourceUserId The source user ID
     */
    public void applyOperation(@NotNull EditorOperation operation, @NotNull String sourceUserId) {
        if (document == null) {
            LOG.warn("Cannot apply operation: Document is null");
            return;
        }
        
        // Skip operations from this user
        if (sourceUserId.equals(userId)) {
            LOG.debug("Skipping operation from self");
            return;
        }
        
        // Skip seen operations
        String operationId = sourceUserId + ":" + operation.getTimestamp();
        if (operationsSeen.contains(operationId)) {
            LOG.debug("Skipping already seen operation: " + operationId);
            return;
        }
        
        operationsSeen.add(operationId);
        
        // Apply operation
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                isApplyingOperation.set(true);
                
                ApplicationManager.getApplication().runWriteAction(() -> {
                    CommandProcessor.getInstance().executeCommand(project, () -> {
                        try {
                            switch (operation.getType()) {
                                case EditorOperation.TYPE_INSERT:
                                    document.insertString(operation.getOffset(), operation.getText());
                                    break;
                                case EditorOperation.TYPE_DELETE:
                                    document.deleteString(
                                            operation.getOffset(),
                                            operation.getOffset() + operation.getLength()
                                    );
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
                    }, "Apply Collaborative Edit", null);
                });
            } finally {
                isApplyingOperation.set(false);
            }
        });
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
     * Gets the document.
     * @return The document
     */
    public Document getDocument() {
        return document;
    }
    
    /**
     * Document change listener to detect local changes.
     */
    private class DocumentChangeListener implements DocumentListener {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            // Skip changes from other users
            if (isApplyingOperation.get()) {
                return;
            }
            
            // Convert change to operation
            EditorOperation operation = null;
            
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
            } else if (event.getNewLength() > 0 && event.getOldLength() > 0) {
                // Replace
                operation = EditorOperation.createReplaceOperation(
                        event.getOffset(),
                        event.getOldLength(),
                        event.getNewFragment().toString()
                );
            }
            
            if (operation != null) {
                // Mark as seen
                String operationId = userId + ":" + operation.getTimestamp();
                operationsSeen.add(operationId);
                
                // Broadcast to other users
                CollaborationService collaborationService = CollaborationService.getInstance(project);
                
                Map<String, Object> data = new HashMap<>();
                data.put("filePath", file.getPath());
                data.put("operation", operation.toMap());
                
                collaborationService.broadcastMessage(WebSocketMessage.TYPE_OPERATION, data);
            }
        }
    }
}