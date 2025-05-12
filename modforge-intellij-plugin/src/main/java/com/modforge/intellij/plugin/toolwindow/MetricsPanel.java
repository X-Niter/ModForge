package com.modforge.intellij.plugin.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.ui.MetricsDashboardPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for displaying metrics about ModForge AI usage.
 */
public class MetricsPanel implements Disposable {
    private final Project project;
    private final JBPanel panel;
    private final MetricsDashboardPanel dashboardPanel;
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     */
    public MetricsPanel(Project project) {
        this.project = project;
        this.panel = new JBPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Create dashboard panel
        this.dashboardPanel = new MetricsDashboardPanel(project);
        
        createUI();
        
        // Register for disposal
        Disposer.register(project, this);
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        // Add dashboard panel to the main panel
        panel.add(dashboardPanel, BorderLayout.CENTER);
        
        // Add title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        JLabel titleLabel = new JLabel("ModForge AI Performance Metrics");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);
        
        // Add info text at the bottom
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        JLabel infoLabel = new JLabel(
            "<html><body style='width: 400px'>These metrics show how the AI pattern learning system is optimizing " +
            "performance and reducing API costs by reusing patterns and cached results.</body></html>"
        );
        infoPanel.add(infoLabel);
        panel.add(infoPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Gets the content panel.
     * @return The content panel
     */
    public JComponent getContent() {
        return panel;
    }
    
    /**
     * Disposes the panel.
     */
    @Override
    public void dispose() {
        if (dashboardPanel != null) {
            dashboardPanel.dispose();
        }
    }
}