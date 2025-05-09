package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the ModForge tool window.
 * This factory is responsible for creating the tool window and its contents.
 */
public final class ModForgeToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create panels
        AIAssistPanel aiAssistPanel = new AIAssistPanel(project);
        MetricsPanel metricsPanel = new MetricsPanel(project);
        SettingsPanel settingsPanel = new SettingsPanel(project);
        
        // Create content factory
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // Create contents
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
        
        // Add contents to tool window
        toolWindow.getContentManager().addContent(aiAssistContent);
        toolWindow.getContentManager().addContent(metricsContent);
        toolWindow.getContentManager().addContent(settingsContent);
    }
}