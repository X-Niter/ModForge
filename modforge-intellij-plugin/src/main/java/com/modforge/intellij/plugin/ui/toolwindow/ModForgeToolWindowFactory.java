package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String TOOL_WINDOW_ID = "ModForge";
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // Create AI Assist tab
        AIAssistPanel aiAssistPanel = new AIAssistPanel(project);
        Content aiAssistContent = contentFactory.createContent(aiAssistPanel, "AI Assist", false);
        toolWindow.getContentManager().addContent(aiAssistContent);
        
        // Create Metrics tab
        MetricsPanel metricsPanel = new MetricsPanel(project);
        Content metricsContent = contentFactory.createContent(metricsPanel, "Metrics", false);
        toolWindow.getContentManager().addContent(metricsContent);
        
        // Create Settings tab
        SettingsPanel settingsPanel = new SettingsPanel(project);
        Content settingsContent = contentFactory.createContent(settingsPanel, "Settings", false);
        toolWindow.getContentManager().addContent(settingsContent);
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("ModForge");
        toolWindow.setTitle("ModForge");
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}