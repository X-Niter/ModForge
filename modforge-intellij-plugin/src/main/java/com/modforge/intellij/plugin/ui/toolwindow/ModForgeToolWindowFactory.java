package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.collaboration.ui.CollaborationPanel;
import com.modforge.intellij.plugin.ui.toolwindow.panels.*;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        // Create panels
        ModGenerationPanel modGenerationPanel = new ModGenerationPanel(project);
        Content modGenerationContent = contentFactory.createContent(
                modGenerationPanel.getContent(), "Mod Generation", false);
        toolWindow.getContentManager().addContent(modGenerationContent);

        AIAssistPanel aiAssistPanel = new AIAssistPanel(project);
        Content aiAssistContent = contentFactory.createContent(
                aiAssistPanel.getContent(), "AI Assistant", false);
        toolWindow.getContentManager().addContent(aiAssistContent);

        CollaborationPanel collaborationPanel = new CollaborationPanel(project);
        Content collaborationContent = contentFactory.createContent(
                collaborationPanel.getContent(), "Collaboration", false);
        toolWindow.getContentManager().addContent(collaborationContent);

        MetricsPanel metricsPanel = new MetricsPanel(project);
        Content metricsContent = contentFactory.createContent(
                metricsPanel.getContent(), "Metrics", false);
        toolWindow.getContentManager().addContent(metricsContent);

        SettingsPanel settingsPanel = new SettingsPanel(project);
        Content settingsContent = contentFactory.createContent(
                settingsPanel.getContent(), "Settings", false);
        toolWindow.getContentManager().addContent(settingsContent);

        // Set the default tab
        toolWindow.getContentManager().setSelectedContent(modGenerationContent);
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