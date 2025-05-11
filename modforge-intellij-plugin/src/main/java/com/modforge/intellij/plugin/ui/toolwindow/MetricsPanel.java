package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Timer; // Explicitly using java.util.Timer for background tasks
import java.util.TimerTask;

/**
 * Panel for displaying AI usage metrics in the tool window.
 */
public class MetricsPanel extends SimpleToolWindowPanel {
    private final Project project;
    private final PatternRecognitionService patternRecognitionService;
    
    private JPanel mainPanel;
    private JTable metricsTable;
    private DefaultTableModel tableModel;
    private JProgressBar patternMatchRateProgressBar;
    private JLabel costSavedLabel;
    private JLabel tokensSavedLabel;
    private JLabel totalRequestsLabel;
    private JLabel patternMatchesLabel;
    private JLabel apiCallsLabel;
    
    private Timer refreshTimer;
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     */
    public MetricsPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.patternRecognitionService = PatternRecognitionService.getInstance(project);
        
        initializeUI();
        startRefreshTimer();
        
        setContent(mainPanel);
    }
    
    /**
     * Initializes the UI.
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel overviewPanel = createOverviewPanel();
        JPanel patternPanel = createPatternPanel();
        JPanel tablePanel = createTablePanel();
        
        contentPanel.add(overviewPanel, BorderLayout.NORTH);
        contentPanel.add(patternPanel, BorderLayout.CENTER);
        contentPanel.add(tablePanel, BorderLayout.SOUTH);
        
        mainPanel.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);
        
        updateMetrics();
    }
    
    /**
     * Creates the overview panel.
     * @return The overview panel
     */
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 0, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtil.getBorderColor()),
                "Usage Overview",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        totalRequestsLabel = new JBLabel("Total Requests: 0");
        patternMatchesLabel = new JBLabel("Pattern Matches: 0");
        apiCallsLabel = new JBLabel("API Calls: 0");
        tokensSavedLabel = new JBLabel("Estimated Tokens Saved: 0");
        costSavedLabel = new JBLabel("Estimated Cost Saved: $0.00");
        
        panel.add(totalRequestsLabel);
        panel.add(patternMatchesLabel);
        panel.add(apiCallsLabel);
        panel.add(tokensSavedLabel);
        panel.add(costSavedLabel);
        
        return panel;
    }
    
    /**
     * Creates the pattern matching rate panel.
     * @return The pattern matching rate panel
     */
    private JPanel createPatternPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtil.getBorderColor()),
                "Pattern Matching Rate",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        patternMatchRateProgressBar = new JProgressBar(0, 100);
        patternMatchRateProgressBar.setStringPainted(true);
        patternMatchRateProgressBar.setValue(0);
        patternMatchRateProgressBar.setString("0%");
        
        panel.add(new JBLabel("Percentage of requests served by pattern matching:"), BorderLayout.NORTH);
        panel.add(patternMatchRateProgressBar, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the table panel.
     * @return The table panel
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtil.getBorderColor()),
                "Pattern Statistics",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        String[] columnNames = { "Pattern Type", "Count", "Success Rate", "Avg. Tokens Saved" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        metricsTable = new JTable(tableModel);
        metricsTable.setFillsViewportHeight(true);
        
        // Center align all columns except the first
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        for (int i = 1; i < metricsTable.getColumnCount(); i++) {
            metricsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        panel.add(new JBScrollPane(metricsTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Starts the refresh timer.
     */
    private void startRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        
        refreshTimer = new Timer("MetricsPanelRefresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateMetrics());
            }
        }, 0, 5000); // Update every 5 seconds
    }
    
    /**
     * Updates the metrics.
     */
    private void updateMetrics() {
        Map<String, Object> metrics = patternRecognitionService.getUsageMetrics();
        
        // Update overview labels
        int totalRequests = (int) metrics.getOrDefault("totalRequests", 0);
        int patternMatches = (int) metrics.getOrDefault("patternMatches", 0);
        int apiCalls = (int) metrics.getOrDefault("apiCalls", 0);
        int tokensSaved = (int) metrics.getOrDefault("estimatedTokensSaved", 0);
        double costSaved = (double) metrics.getOrDefault("estimatedCostSaved", 0.0);
        
        totalRequestsLabel.setText("Total Requests: " + totalRequests);
        patternMatchesLabel.setText("Pattern Matches: " + patternMatches);
        apiCallsLabel.setText("API Calls: " + apiCalls);
        tokensSavedLabel.setText("Estimated Tokens Saved: " + NumberFormat.getIntegerInstance().format(tokensSaved));
        costSavedLabel.setText("Estimated Cost Saved: $" + String.format("%.2f", costSaved));
        
        // Update pattern match rate progress bar
        int matchRate = totalRequests > 0 ? (patternMatches * 100 / totalRequests) : 0;
        patternMatchRateProgressBar.setValue(matchRate);
        patternMatchRateProgressBar.setString(matchRate + "%");
        
        // Set color based on rate
        if (matchRate >= 70) {
            patternMatchRateProgressBar.setForeground(new JBColor(new Color(0, 150, 0), new Color(0, 150, 0)));
        } else if (matchRate >= 40) {
            patternMatchRateProgressBar.setForeground(new JBColor(new Color(200, 150, 0), new Color(200, 150, 0)));
        } else {
            patternMatchRateProgressBar.setForeground(new JBColor(new Color(200, 0, 0), new Color(200, 0, 0)));
        }
        
        // Update table data
        tableModel.setRowCount(0);
        
        Map<String, Map<String, Object>> patternStats = 
                (Map<String, Map<String, Object>>) metrics.getOrDefault("patternStats", Map.of());
        
        for (Map.Entry<String, Map<String, Object>> entry : patternStats.entrySet()) {
            String patternType = entry.getKey();
            Map<String, Object> stats = entry.getValue();
            
            int count = (int) stats.getOrDefault("count", 0);
            double successRate = (double) stats.getOrDefault("successRate", 0.0);
            int avgTokensSaved = (int) stats.getOrDefault("avgTokensSaved", 0);
            
            Object[] row = {
                    patternType,
                    count,
                    String.format("%.1f%%", successRate * 100),
                    avgTokensSaved
            };
            
            tableModel.addRow(row);
        }
    }
    
    /**
     * Disposes the panel.
     */
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        
        super.dispose();
    }
}