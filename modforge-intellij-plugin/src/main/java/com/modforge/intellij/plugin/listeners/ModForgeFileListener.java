package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Listener for file change events.
 * Monitors changes to mod files and triggers continuous development actions.
 */
public class ModForgeFileListener implements VirtualFileListener, BulkFileListener {
    private static final Logger LOG = Logger.getInstance(ModForgeFileListener.class);
    private final Project project;
    private final MessageBusConnection connection;
    
    /**
     * Creates a new ModForgeFileListener.
     * @param project The project
     */
    public ModForgeFileListener(Project project) {
        this.project = project;
        
        // Register with virtual file manager
        VirtualFileManager.getInstance().addVirtualFileListener(this);
        
        // Register with message bus
        connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
        
        LOG.info("ModForge file listener registered for project: " + project.getName());
    }
    
    /**
     * Disposes the listener.
     */
    public void dispose() {
        // Unregister from virtual file manager
        VirtualFileManager.getInstance().removeVirtualFileListener(this);
        
        // Unregister from message bus
        connection.disconnect();
        
        LOG.info("ModForge file listener unregistered for project: " + project.getName());
    }

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        handleFileChange(event.getFile());
    }
    
    /**
     * Handles file changes.
     * @param file The changed file
     */
    private void handleFileChange(VirtualFile file) {
        // Check if continuous development is enabled
        if (!ModForgeSettings.getInstance().isEnableContinuousDevelopment()) {
            return;
        }
        
        // Check if file is a mod file
        if (!isModFile(file)) {
            return;
        }
        
        LOG.info("Mod file changed: " + file.getName());
        
        // Notify continuous development service
        ContinuousDevelopmentService.getInstance(project).fileChanged(file);
    }
    
    /**
     * Checks if a file is a mod file.
     * @param file The file
     * @return True if the file is a mod file
     */
    private boolean isModFile(VirtualFile file) {
        // Check if file is in a mod directory
        String path = file.getPath();
        
        // Check common mod patterns
        return (path.contains("/src/main/java/") || 
                path.contains("/src/main/kotlin/") ||
                path.contains("/src/main/resources/") ||
                path.contains("/src/client/java/") ||
                path.contains("/src/client/kotlin/") ||
                path.contains("/src/client/resources/") ||
                path.contains("/src/server/java/") ||
                path.contains("/src/server/kotlin/") ||
                path.contains("/src/server/resources/")) &&
               (path.contains("/forge/") || 
                path.contains("/fabric/") ||
                path.contains("/quilt/") ||
                path.contains("/common/") ||
                path.contains("/architectury/"));
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        // Process all events
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            
            if (file != null) {
                handleFileChange(file);
            }
        }
    }
}