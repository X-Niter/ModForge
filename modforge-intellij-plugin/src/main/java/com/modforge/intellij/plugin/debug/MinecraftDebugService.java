package com.modforge.intellij.plugin.debug;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.XDebuggerManagerImpl;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for enhancing Minecraft debugging experience
 * Provides custom debugger extensions and tools specifically for Minecraft
 */
@Service
public final class MinecraftDebugService {
    private static final Logger LOG = Logger.getInstance(MinecraftDebugService.class);
    
    private final Project project;
    private final MinecraftDebuggerExtension debuggerExtension;
    private final MessageBusConnection connection;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    public MinecraftDebugService(Project project) {
        this.project = project;
        this.debuggerExtension = new MinecraftDebuggerExtension(project);
        
        // Subscribe to debug session events
        MessageBus messageBus = project.getMessageBus();
        connection = messageBus.connect();
        
        // Initialize service on first usage
        initialize();
    }
    
    /**
     * Initialize the service and register all listeners
     */
    private void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return; // Already initialized
        }
        
        LOG.info("Initializing MinecraftDebugService for project: " + project.getName());
        
        // Register listeners for debugging events
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        if (debuggerManager instanceof XDebuggerManagerImpl) {
            XDebuggerManagerImpl debuggerManagerImpl = (XDebuggerManagerImpl) debuggerManager;
            
            // Register a custom debug action handler
            debuggerManagerImpl.registerDebugProcessListener((processHandler, debugProcess) -> {
                if (isMinecraftDebugProcess(debugProcess)) {
                    LOG.info("Minecraft debug process detected, applying custom debugger extensions");
                    enhanceMinecraftDebugProcess(debugProcess);
                }
            });
        }
        
        LOG.info("MinecraftDebugService initialized successfully");
    }
    
    /**
     * Check if a debug process is for a Minecraft run configuration
     * 
     * @param debugProcess The debug process to check
     * @return True if this is a Minecraft debug process, false otherwise
     */
    private boolean isMinecraftDebugProcess(XDebugProcess debugProcess) {
        XDebugSession session = debugProcess.getSession();
        RunProfile runProfile = session.getRunProfile();
        return runProfile instanceof MinecraftRunConfiguration;
    }
    
    /**
     * Enhance a Minecraft debug process with custom debugger extensions
     * 
     * @param debugProcess The debug process to enhance
     */
    private void enhanceMinecraftDebugProcess(XDebugProcess debugProcess) {
        try {
            // Apply custom renderers for Minecraft objects
            boolean renderersApplied = debuggerExtension.applyCustomRenderers();
            LOG.info("Custom renderers applied: " + renderersApplied);
            
            // Set up exception breakpoints for common Minecraft issues
            RunProfile runProfile = debugProcess.getSession().getRunProfile();
            debuggerExtension.setupMinecraftExceptionBreakpoints(runProfile);
            
            // Add performance monitoring if this is a Minecraft client or server
            if (runProfile instanceof MinecraftRunConfiguration) {
                MinecraftRunConfiguration config = (MinecraftRunConfiguration) runProfile;
                switch (config.getRunType()) {
                    case CLIENT:
                        setupClientPerformanceMonitoring(debugProcess);
                        break;
                    case SERVER:
                        setupServerPerformanceMonitoring(debugProcess);
                        break;
                }
            }
        } catch (Exception e) {
            LOG.warn("Error enhancing Minecraft debug process", e);
        }
    }
    
    /**
     * Set up performance monitoring for a Minecraft client debug process
     * 
     * @param debugProcess The debug process to monitor
     */
    private void setupClientPerformanceMonitoring(XDebugProcess debugProcess) {
        // TODO: Implement client performance monitoring
        // This would typically set watchpoints for FPS counter, rendering time, etc.
        LOG.info("Client performance monitoring setup for Minecraft debug process");
    }
    
    /**
     * Set up performance monitoring for a Minecraft server debug process
     * 
     * @param debugProcess The debug process to monitor
     */
    private void setupServerPerformanceMonitoring(XDebugProcess debugProcess) {
        // TODO: Implement server performance monitoring  
        // This would typically set watchpoints for TPS counter, chunk loading time, etc.
        LOG.info("Server performance monitoring setup for Minecraft debug process");
    }
    
    /**
     * Dispose of the service, cleaning up all listeners
     */
    public void dispose() {
        if (initialized.get()) {
            connection.disconnect();
            initialized.set(false);
            LOG.info("MinecraftDebugService disposed for project: " + project.getName());
        }
    }
}