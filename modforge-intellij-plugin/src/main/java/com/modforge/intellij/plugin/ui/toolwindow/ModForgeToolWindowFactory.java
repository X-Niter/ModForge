package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for the ModForge tool window.
 * This class creates the ModForge tool window with its tabs.
 */
public final class ModForgeToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowFactory.class);
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("Creating ModForge tool window content");
        
        // Create AI assist panel
        AIAssistPanel aiAssistPanel = new AIAssistPanel(project);
        
        // Create metrics panel
        MetricsPanel metricsPanel = new MetricsPanel(project);
        
        // Create settings panel
        SettingsPanel settingsPanel = new SettingsPanel(project);
        
        // Create content factory
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // Create content for each panel
        Content aiAssistContent = contentFactory.createContent(
                aiAssistPanel.getContent(),
                "AI Assist",
                false
        );
        
        Content metricsContent = contentFactory.createContent(
                metricsPanel.getContent(),
                "Metrics",
                false
        );
        
        Content settingsContent = contentFactory.createContent(
                settingsPanel.getContent(),
                "Settings",
                false
        );
        
        // Add content to tool window
        toolWindow.getContentManager().addContent(aiAssistContent);
        toolWindow.getContentManager().addContent(metricsContent);
        toolWindow.getContentManager().addContent(settingsContent);
        
        // Set up tool window dispose actions
        toolWindow.getContentManager().addContentManagerListener(new ToolWindowCleanupListener(metricsPanel));
    }
}