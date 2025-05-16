package com.modforge.intellij.plugin.debug;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebugSession;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Stubbed Minecraft debugger extension (no-op for current IDE).
 */
public class MinecraftDebuggerExtension implements XDebugSessionListener {
    private final XDebugSession session;

    public MinecraftDebuggerExtension(@NotNull XDebugSession session) {
        this.session = session;
        session.addSessionListener(this);
    }

    private void applyCustomRenderers() {
        // implement renderer setup here if needed
    }

    private void setupMinecraftExceptionBreakpoints(@NotNull RunProfile runProfile) {
        if (runProfile instanceof MinecraftRunConfiguration) {
            // add breakpoints or custom logic
        }
    }

    private boolean isDebuggingMinecraft() {
        RunProfile profile = session.getRunProfile();
        return profile instanceof MinecraftRunConfiguration;
    }

    @Override
    public void sessionInitialized() {
        applyCustomRenderers();
        setupMinecraftExceptionBreakpoints(session.getRunProfile());
    }

    @Override public void sessionPaused() {}
    @Override public void sessionResumed() {}
    @Override public void sessionStopped() {}
    @Override public void stackFrameChanged() {}
}