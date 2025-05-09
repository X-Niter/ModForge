package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for creating the ModForge tool window.
 */
public final class ModForgeToolWindowFactory implements ToolWindowFactory {
    private static final String PANEL_AI_ASSIST = "AI Assist";
    private static final String PANEL_METRICS = "Metrics";
    private static final String PANEL_SETTINGS = "Settings";
    
    private AIAssistPanel aiAssistPanel;
    private MetricsPanel metricsPanel;
    private SettingsPanel settingsPanel;
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        createPanels(project);
        
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        JBTabsImpl tabs = new JBTabsImpl(project);
        
        // Create AI Assist tab
        TabInfo aiAssistTab = new TabInfo(aiAssistPanel);
        aiAssistTab.setText(PANEL_AI_ASSIST);
        tabs.addTab(aiAssistTab);
        
        // Create Metrics tab
        TabInfo metricsTab = new TabInfo(metricsPanel);
        metricsTab.setText(PANEL_METRICS);
        tabs.addTab(metricsTab);
        
        // Create Settings tab
        TabInfo settingsTab = new TabInfo(settingsPanel);
        settingsTab.setText(PANEL_SETTINGS);
        tabs.addTab(settingsTab);
        
        Content content = contentFactory.createContent(tabs.getComponent(), "", false);
        contentManager.addContent(content);
    }
    
    /**
     * Creates the panels for the tool window.
     * @param project The project
     */
    private void createPanels(@NotNull Project project) {
        aiAssistPanel = new AIAssistPanel(project);
        metricsPanel = new MetricsPanel(project);
        settingsPanel = new SettingsPanel(project);
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle("ModForge");
        toolWindow.setStripeTitle("ModForge");
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}