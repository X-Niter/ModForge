package com.modforge.intellij.plugin.debug;

import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.debugger.ui.tree.render.ChildrenBuilder;
import com.intellij.debugger.ui.tree.render.ClassRenderer;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced debugger extension for Minecraft-specific objects
 * Provides custom renderers and debugging tools for better Minecraft debugging experience
 */
public class MinecraftDebuggerExtension {
    private static final Logger LOG = Logger.getInstance(MinecraftDebuggerExtension.class);
    
    private final Project project;
    private final Map<String, ClassRenderer> customRenderers = new HashMap<>();
    
    public MinecraftDebuggerExtension(Project project) {
        this.project = project;
        initializeCustomRenderers();
    }
    
    /**
     * Register custom renderers for Minecraft classes
     */
    private void initializeCustomRenderers() {
        // Add custom renderer for Block class
        customRenderers.put("net.minecraft.world.level.block.Block", new MinecraftBlockRenderer());
        
        // Add custom renderer for Entity class
        customRenderers.put("net.minecraft.world.entity.Entity", new MinecraftEntityRenderer());
        
        // Add custom renderer for Item class
        customRenderers.put("net.minecraft.world.item.Item", new MinecraftItemRenderer());
        
        // Add custom renderer for BlockEntity class
        customRenderers.put("net.minecraft.world.level.block.entity.BlockEntity", new MinecraftBlockEntityRenderer());
    }
    
    /**
     * Get a custom renderer for a specific class if available
     * 
     * @param className The fully qualified class name
     * @return A custom renderer if available, null otherwise
     */
    @Nullable
    public ClassRenderer getCustomRenderer(String className) {
        return customRenderers.get(className);
    }
    
    /**
     * Apply all custom renderers to the current debug session
     * 
     * @return true if renderers were applied, false otherwise
     */
    public boolean applyCustomRenderers() {
        XDebugSession currentSession = XDebuggerManager.getInstance(project).getCurrentSession();
        if (currentSession == null) {
            LOG.debug("No active debug session found");
            return false;
        }
        
        XDebugProcess debugProcess = currentSession.getDebugProcess();
        if (!(debugProcess instanceof JavaDebugProcess)) {
            LOG.debug("Debug process is not a JavaDebugProcess");
            return false;
        }
        
        JavaDebugProcess javaDebugProcess = (JavaDebugProcess) debugProcess;
        DebugProcess process = javaDebugProcess.getDebuggerSession().getProcess();
        
        if (process instanceof DebugProcessImpl) {
            final DebugProcessImpl debugProcessImpl = (DebugProcessImpl) process;
            
            DebuggerManagerThreadImpl.assertIsManagerThread();
            
            // Apply all custom renderers
            for (Map.Entry<String, ClassRenderer> entry : customRenderers.entrySet()) {
                try {
                    debugProcessImpl.getNodeManager().setRenderer(entry.getKey(), entry.getValue());
                    LOG.debug("Applied custom renderer for " + entry.getKey());
                } catch (Exception e) {
                    LOG.warn("Failed to apply custom renderer for " + entry.getKey(), e);
                }
            }
            
            return true;
        }
        
        LOG.debug("Debug process is not a DebugProcessImpl");
        return false;
    }
    
    /**
     * Setup conditional breakpoints for catching common Minecraft exceptions
     * 
     * @param runProfile The run profile being debugged
     */
    public void setupMinecraftExceptionBreakpoints(RunProfile runProfile) {
        if (!(runProfile instanceof MinecraftRunConfiguration)) {
            return;
        }
        
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        if (debuggerManager == null) {
            return;
        }
        
        // Add exception breakpoints for common Minecraft issues
        XBreakpointManagerImpl breakpointManager = (XBreakpointManagerImpl) debuggerManager.getBreakpointManager();
        
        // Common Minecraft exceptions to catch
        String[] exceptionClasses = {
            "net.minecraft.ReportedException",
            "net.minecraft.client.renderer.RenderException",
            "net.minecraft.world.level.ChunkLoadException",
            "net.minecraft.world.phys.shapes.VoxelShapeException",
            "net.minecraft.server.network.ServerGamePacketListenerImpl$SuspiciousOperationException",
            "java.lang.OutOfMemoryError"  // Critical to catch for Minecraft performance issues
        };
        
        for (String exceptionClass : exceptionClasses) {
            try {
                // Check if we already have this exception breakpoint
                boolean breakpointExists = breakpointManager.getAllBreakpoints().stream()
                        .filter(XBreakpoint::isEnabled)
                        .anyMatch(bp -> {
                            XBreakpointProperties<?> properties = bp.getProperties();
                            if (properties != null && properties.toString().contains(exceptionClass)) {
                                return true;
                            }
                            return false;
                        });
                
                if (!breakpointExists) {
                    // TODO: Add exception breakpoint programmatically
                    // This requires low-level interaction with the IDE's breakpoint system
                    // Instead, we'll log that the user should add these manually
                    LOG.info("Consider adding exception breakpoint for: " + exceptionClass);
                }
            } catch (Exception e) {
                LOG.warn("Failed to check/add exception breakpoint for " + exceptionClass, e);
            }
        }
    }
    
    /**
     * Check if the current debug session is for a Minecraft run configuration
     * 
     * @return True if debugging a Minecraft run configuration
     */
    public boolean isDebuggingMinecraft() {
        XDebugSession currentSession = XDebuggerManager.getInstance(project).getCurrentSession();
        if (currentSession == null) {
            return false;
        }
        
        RunProfile runProfile = null;
        if (currentSession instanceof XDebugSessionImpl) {
            runProfile = ((XDebugSessionImpl) currentSession).getRunProfile();
        }
        
        return runProfile instanceof MinecraftRunConfiguration;
    }
    
    /**
     * Base class for Minecraft-specific renderers
     */
    private static abstract class MinecraftObjectRenderer extends ClassRenderer {
        @Override
        public void buildChildren(ValueDescriptor descriptor, ChildrenBuilder builder, EvaluationContext evaluationContext) {
            super.buildChildren(descriptor, builder, evaluationContext);
            // Add custom children based on the Minecraft object type
            buildMinecraftChildren(descriptor, builder, evaluationContext);
        }
        
        protected abstract void buildMinecraftChildren(ValueDescriptor descriptor, ChildrenBuilder builder, EvaluationContext evaluationContext);
    }
    
    /**
     * Custom renderer for Block objects
     */
    private static class MinecraftBlockRenderer extends MinecraftObjectRenderer {
        @Override
        public String calcLabel(ValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) {
            // Add registry name to block display
            return "Block: " + super.calcLabel(descriptor, evaluationContext, listener);
        }
        
        @Override
        protected void buildMinecraftChildren(ValueDescriptor descriptor, ChildrenBuilder builder, EvaluationContext evaluationContext) {
            // Add custom children for block objects
            // This would typically add things like block states, material, etc.
        }
    }
    
    /**
     * Custom renderer for Entity objects
     */
    private static class MinecraftEntityRenderer extends MinecraftObjectRenderer {
        @Override
        public String calcLabel(ValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) {
            // Add entity ID and position to display
            return "Entity: " + super.calcLabel(descriptor, evaluationContext, listener);
        }
        
        @Override
        protected void buildMinecraftChildren(ValueDescriptor descriptor, ChildrenBuilder builder, EvaluationContext evaluationContext) {
            // Add custom children for entity objects
            // This would typically add things like position, motion, health, etc.
        }
    }
    
    /**
     * Custom renderer for Item objects
     */
    private static class MinecraftItemRenderer extends MinecraftObjectRenderer {
        @Override
        public String calcLabel(ValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) {
            // Add registry name to item display
            return "Item: " + super.calcLabel(descriptor, evaluationContext, listener);
        }
        
        @Override
        protected void buildMinecraftChildren(ValueDescriptor descriptor, ChildrenBuilder builder, EvaluationContext evaluationContext) {
            // Add custom children for item objects
            // This would typically add things like item properties, creative tab, etc.
        }
    }
    
    /**
     * Custom renderer for BlockEntity objects
     */
    private static class MinecraftBlockEntityRenderer extends MinecraftObjectRenderer {
        @Override
        public String calcLabel(ValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) {
            // Add position to block entity display
            return "BlockEntity: " + super.calcLabel(descriptor, evaluationContext, listener);
        }
        
        @Override
        protected void buildMinecraftChildren(ValueDescriptor descriptor, ChildrenBuilder builder, EvaluationContext evaluationContext) {
            // Add custom children for block entity objects
            // This would typically add things like position, block type, etc.
        }
    }
}