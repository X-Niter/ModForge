package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter class for DocumentListener that provides consistent behavior across different IntelliJ versions.
 * Use this instead of directly implementing DocumentListener to ensure compatibility with
 * IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).
 */
public abstract class DocumentListenerAdapter implements DocumentListener {
    
    /**
     * Called before the document is changed.
     *
     * @param event the event containing the change information
     */
    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        // Default empty implementation
    }
    
    /**
     * Called after the document has been changed.
     *
     * @param event the event containing the change information
     */
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        // Default empty implementation
    }
    
    /**
     * Helper method to add this listener to a document.
     *
     * @param document the document to add the listener to
     */
    public void addTo(@NotNull Document document) {
        document.addDocumentListener(this);
    }
    
    /**
     * Helper method to remove this listener from a document.
     *
     * @param document the document to remove the listener from
     */
    public void removeFrom(@NotNull Document document) {
        document.removeDocumentListener(this);
    }
}