package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.modforge.intellij.plugin.ui.toolwindow.panels.AIAssistPanel;
import com.modforge.intellij.plugin.ui.toolwindow.panels.MetricsPanel;
import com.modforge.intellij.plugin.ui.toolwindow.panels.SettingsPanel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory, DumbAware {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowFactory.class);
    
    // Keep track of panels so we can dispose them when the project is closed
    private final Map<Project, AIAssistPanel> aiAssistPanels = new HashMap<>();
    private final Map<Project, MetricsPanel> metricsPanels = new HashMap<>();
    private final Map<Project, SettingsPanel> settingsPanels = new HashMap<>();
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("Creating ModForge tool window for project: " + project.getName());
        
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // Create AI Assist panel
        AIAssistPanel aiAssistPanel = new AIAssistPanel(project);
        aiAssistPanels.put(project, aiAssistPanel);
        
        Content aiAssistContent = contentFactory.createContent(
                aiAssistPanel.getContent(), 
                "AI Assist", 
                false);
        contentManager.addContent(aiAssistContent);
        
        // Create Metrics panel
        MetricsPanel metricsPanel = new MetricsPanel(project);
        metricsPanels.put(project, metricsPanel);
        
        Content metricsContent = contentFactory.createContent(
                metricsPanel.getContent(),
                "Metrics",
                false);
        contentManager.addContent(metricsContent);
        
        // Create Settings panel
        SettingsPanel settingsPanel = new SettingsPanel(project);
        settingsPanels.put(project, settingsPanel);
        
        Content settingsContent = contentFactory.createContent(
                settingsPanel.getContent(),
                "Settings",
                false);
        contentManager.addContent(settingsContent);
        
        // Set default content
        contentManager.setSelectedContent(aiAssistContent);
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        LOG.info("Initializing ModForge tool window");
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
    
    @Override
    public boolean isDoNotActivateOnStart() {
        return false;
    }
    
    /**
     * Disposes the panels for a project.
     * @param project The project
     */
    public void disposeProject(@NotNull Project project) {
        LOG.info("Disposing ModForge tool window for project: " + project.getName());
        
        // Dispose AI Assist panel
        AIAssistPanel aiAssistPanel = aiAssistPanels.remove(project);
        if (aiAssistPanel != null) {
            aiAssistPanel.dispose();
        }
        
        // Metrics panel is Disposable, so it will be disposed automatically
        metricsPanels.remove(project);
    }
}