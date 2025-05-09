package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create panels
        AIAssistPanel aiAssistPanel = new AIAssistPanel(project, toolWindow);
        MetricsPanel metricsPanel = new MetricsPanel(project, toolWindow);
        SettingsPanel settingsPanel = new SettingsPanel(project, toolWindow);
        
        // Get content factory
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // Add AI assist panel
        Content aiAssistContent = contentFactory.createContent(
                aiAssistPanel.getContent(),
                "AI Assist",
                false
        );
        toolWindow.getContentManager().addContent(aiAssistContent);
        
        // Add metrics panel
        Content metricsContent = contentFactory.createContent(
                metricsPanel.getContent(),
                "Metrics",
                false
        );
        toolWindow.getContentManager().addContent(metricsContent);
        
        // Add settings panel
        Content settingsContent = contentFactory.createContent(
                settingsPanel.getContent(),
                "Settings",
                false
        );
        toolWindow.getContentManager().addContent(settingsContent);
    }
}