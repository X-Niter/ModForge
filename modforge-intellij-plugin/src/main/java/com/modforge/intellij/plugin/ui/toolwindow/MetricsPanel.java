package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Panel for displaying metrics.
 */
public class MetricsPanel implements Disposable {
    private final Project project;
    private final ToolWindow toolWindow;
    private final SimpleToolWindowPanel panel;
    
    private JPanel statsPanel;
    private JLabel apiCallsLabel;
    private JLabel patternMatchesLabel;
    private JLabel tokensSavedLabel;
    private JLabel costSavedLabel;
    private JLabel successRateLabel;
    
    private Timer refreshTimer;
    private static final int REFRESH_INTERVAL = 30000; // 30 seconds
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     * @param toolWindow The tool window
     */
    public MetricsPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        
        // Create panel
        panel = new SimpleToolWindowPanel(true, true);
        
        // Init UI
        initUI();
        
        // Start refresh timer
        startRefreshTimer();
    }
    
    /**
     * Initializes the UI.
     */
    private void initUI() {
        // Create components
        JButton refreshButton = new JButton("Refresh");
        
        statsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        statsPanel.setBorder(JBUI.Borders.empty(10));
        
        apiCallsLabel = new JLabel("API Calls: 0");
        patternMatchesLabel = new JLabel("Pattern Matches: 0");
        tokensSavedLabel = new JLabel("Tokens Saved: 0");
        costSavedLabel = new JLabel("Cost Saved: $0.00");
        successRateLabel = new JLabel("Success Rate: 0%");
        
        statsPanel.add(new JBLabel("<html><b>Pattern Recognition Metrics</b></html>"));
        statsPanel.add(patternMatchesLabel);
        statsPanel.add(apiCallsLabel);
        statsPanel.add(tokensSavedLabel);
        statsPanel.add(costSavedLabel);
        statsPanel.add(successRateLabel);
        
        // Add separator
        statsPanel.add(Box.createVerticalStrut(20));
        statsPanel.add(new JBLabel("<html><b>Pattern Recognition Efficiency</b></html>"));
        
        // Add statistics for different pattern types
        statsPanel.add(new JLabel("Code Generation Patterns: 0"));
        statsPanel.add(new JLabel("Error Fixing Patterns: 0"));
        statsPanel.add(new JLabel("Documentation Patterns: 0"));
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JBScrollPane(statsPanel), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(refreshButton);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Add padding
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add to panel
        panel.setContent(mainPanel);
        
        // Add action listeners
        refreshButton.addActionListener(this::onRefreshClicked);
        
        // Initial refresh
        refreshMetrics();
    }
    
    /**
     * Starts the refresh timer.
     */
    private void startRefreshTimer() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> refreshMetrics());
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
    }
    
    /**
     * Called when the refresh button is clicked.
     * @param e The action event
     */
    private void onRefreshClicked(ActionEvent e) {
        refreshMetrics();
    }
    
    /**
     * Refreshes the metrics.
     */
    private void refreshMetrics() {
        // Get metrics from services
        AIServiceManager aiManager = AIServiceManager.getInstance();
        PatternRecognitionService patternService = PatternRecognitionService.getInstance();
        
        Map<String, Object> metrics = patternService.getMetrics();
        
        // Update UI
        int apiCalls = ((Number) metrics.getOrDefault("apiCalls", 0)).intValue();
        int patternMatches = ((Number) metrics.getOrDefault("patternMatches", 0)).intValue();
        int tokensSaved = ((Number) metrics.getOrDefault("tokensSaved", 0)).intValue();
        double costSaved = ((Number) metrics.getOrDefault("costSaved", 0.0)).doubleValue();
        
        apiCallsLabel.setText("API Calls: " + apiCalls);
        patternMatchesLabel.setText("Pattern Matches: " + patternMatches);
        tokensSavedLabel.setText("Tokens Saved: " + tokensSaved);
        costSavedLabel.setText(String.format("Cost Saved: $%.2f", costSaved));
        
        // Calculate success rate
        int totalRequests = apiCalls + patternMatches;
        double successRate = totalRequests > 0 ? (double) patternMatches / totalRequests * 100.0 : 0.0;
        successRateLabel.setText(String.format("Success Rate: %.1f%%", successRate));
        
        // Update pattern type counts
        Map<String, Integer> patternCounts = (Map<String, Integer>) metrics.getOrDefault("patternCounts", Map.of());
        
        int codePatterns = patternCounts.getOrDefault("code", 0);
        int errorPatterns = patternCounts.getOrDefault("error", 0);
        int docPatterns = patternCounts.getOrDefault("documentation", 0);
        
        // Remove existing pattern type labels
        for (int i = statsPanel.getComponentCount() - 1; i >= 0; i--) {
            Component component = statsPanel.getComponent(i);
            if (component instanceof JLabel) {
                String text = ((JLabel) component).getText();
                if (text.startsWith("Code Generation Patterns") ||
                        text.startsWith("Error Fixing Patterns") ||
                        text.startsWith("Documentation Patterns")) {
                    statsPanel.remove(i);
                }
            }
        }
        
        // Add updated pattern type labels
        statsPanel.add(new JLabel("Code Generation Patterns: " + codePatterns));
        statsPanel.add(new JLabel("Error Fixing Patterns: " + errorPatterns));
        statsPanel.add(new JLabel("Documentation Patterns: " + docPatterns));
        
        // Revalidate
        statsPanel.revalidate();
        statsPanel.repaint();
    }
    
    /**
     * Gets the content.
     * @return The content
     */
    public JComponent getContent() {
        return panel;
    }
    
    @Override
    public void dispose() {
        // Cancel timer
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}