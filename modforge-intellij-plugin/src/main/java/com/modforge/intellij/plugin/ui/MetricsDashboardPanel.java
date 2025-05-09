package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.services.AIServiceManager;
import com.modforge.intellij.plugin.services.PatternCachingService;
import com.modforge.intellij.plugin.services.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Map;

/**
 * Dashboard panel for displaying AI performance metrics.
 */
public class MetricsDashboardPanel extends JBPanel<MetricsDashboardPanel> {
    private static final Logger LOG = Logger.getInstance(MetricsDashboardPanel.class);
    
    // Refresh interval in milliseconds
    private static final int REFRESH_INTERVAL = 5000;
    
    // Cost constants
    private static final double COST_PER_1K_TOKENS = 0.002; // $0.002 per 1K tokens for GPT-4
    
    // UI components
    private JBPanel<JBPanel> metricsPanel;
    private JBPanel<JBPanel> costSavingsPanel;
    private JBPanel<JBPanel> patternStatsPanel;
    private JLabel totalRequestsLabel;
    private JLabel patternMatchesLabel;
    private JLabel apiCallsLabel;
    private JLabel patternMatchRateLabel;
    private JLabel estimatedTokensSavedLabel;
    private JLabel estimatedCostSavedLabel;
    private JLabel estimatedTotalCostLabel;
    private JLabel cacheHitRatioLabel;
    private JProgressBar efficiencyProgressBar;
    
    // Services
    private final Project project;
    private final AIServiceManager aiServiceManager;
    private final PatternCachingService patternCachingService;
    private final PatternRecognitionService patternRecognitionService;
    
    // Timer for refreshing metrics
    private Timer refreshTimer;
    
    /**
     * Creates a new MetricsDashboardPanel.
     * @param project The project
     */
    public MetricsDashboardPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.aiServiceManager = AIServiceManager.getInstance();
        this.patternCachingService = PatternCachingService.getInstance(project);
        this.patternRecognitionService = PatternRecognitionService.getInstance(project);
        
        createUI();
        startRefreshTimer();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        // Create main panel
        JBPanel<JBPanel> mainPanel = new JBPanel<>(new GridBagLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.insets(5);
        
        // Create metrics panel
        c.gridx = 0;
        c.gridy = 0;
        metricsPanel = createMetricsPanel();
        mainPanel.add(metricsPanel, c);
        
        // Create cost savings panel
        c.gridy = 1;
        costSavingsPanel = createCostSavingsPanel();
        mainPanel.add(costSavingsPanel, c);
        
        // Create pattern stats panel
        c.gridy = 2;
        patternStatsPanel = createPatternStatsPanel();
        mainPanel.add(patternStatsPanel, c);
        
        // Create efficiency graph panel
        c.gridy = 3;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        JBPanel<JBPanel> efficiencyPanel = createEfficiencyPanel();
        mainPanel.add(efficiencyPanel, c);
        
        // Add main panel to scroll pane
        JBScrollPane scrollPane = new JBScrollPane(mainPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);
        
        // Add toolbar
        JBPanel<JBPanel> toolbarPanel = createToolbarPanel();
        add(toolbarPanel, BorderLayout.NORTH);
        
        // Initial refresh
        refreshMetrics();
    }
    
    /**
     * Creates the metrics panel.
     * @return The metrics panel
     */
    @NotNull
    private JBPanel<JBPanel> createMetricsPanel() {
        JBPanel<JBPanel> panel = new JBPanel<>(new GridLayout(4, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Usage Metrics"));
        
        panel.add(new JBLabel("Total Requests:", SwingConstants.RIGHT));
        totalRequestsLabel = new JBLabel("0");
        panel.add(totalRequestsLabel);
        
        panel.add(new JBLabel("Pattern Matches:", SwingConstants.RIGHT));
        patternMatchesLabel = new JBLabel("0");
        panel.add(patternMatchesLabel);
        
        panel.add(new JBLabel("API Calls:", SwingConstants.RIGHT));
        apiCallsLabel = new JBLabel("0");
        panel.add(apiCallsLabel);
        
        panel.add(new JBLabel("Pattern Match Rate:", SwingConstants.RIGHT));
        patternMatchRateLabel = new JBLabel("0%");
        panel.add(patternMatchRateLabel);
        
        return panel;
    }
    
    /**
     * Creates the cost savings panel.
     * @return The cost savings panel
     */
    @NotNull
    private JBPanel<JBPanel> createCostSavingsPanel() {
        JBPanel<JBPanel> panel = new JBPanel<>(new GridLayout(3, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Cost Savings"));
        
        panel.add(new JBLabel("Estimated Tokens Saved:", SwingConstants.RIGHT));
        estimatedTokensSavedLabel = new JBLabel("0");
        panel.add(estimatedTokensSavedLabel);
        
        panel.add(new JBLabel("Estimated Cost Saved:", SwingConstants.RIGHT));
        estimatedCostSavedLabel = new JBLabel("$0.00");
        panel.add(estimatedCostSavedLabel);
        
        panel.add(new JBLabel("Estimated Total Cost:", SwingConstants.RIGHT));
        estimatedTotalCostLabel = new JBLabel("$0.00");
        panel.add(estimatedTotalCostLabel);
        
        return panel;
    }
    
    /**
     * Creates the pattern stats panel.
     * @return The pattern stats panel
     */
    @NotNull
    private JBPanel<JBPanel> createPatternStatsPanel() {
        JBPanel<JBPanel> panel = new JBPanel<>(new GridLayout(1, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Pattern Statistics"));
        
        panel.add(new JBLabel("Cache Hit Ratio:", SwingConstants.RIGHT));
        cacheHitRatioLabel = new JBLabel("0%");
        panel.add(cacheHitRatioLabel);
        
        return panel;
    }
    
    /**
     * Creates the efficiency panel.
     * @return The efficiency panel
     */
    @NotNull
    private JBPanel<JBPanel> createEfficiencyPanel() {
        JBPanel<JBPanel> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "System Efficiency"));
        
        efficiencyProgressBar = new JProgressBar(0, 100);
        efficiencyProgressBar.setValue(0);
        efficiencyProgressBar.setStringPainted(true);
        efficiencyProgressBar.setString("0%");
        panel.add(efficiencyProgressBar, BorderLayout.NORTH);
        
        JBLabel efficiencyDescriptionLabel = new JBLabel(
                "<html><body style='width: 300px'>" +
                "System efficiency measures how well the pattern recognition and caching systems " +
                "are reducing API calls and improving performance. Higher values indicate better efficiency " +
                "and greater cost savings.</body></html>"
        );
        efficiencyDescriptionLabel.setBorder(JBUI.Borders.empty(10));
        panel.add(efficiencyDescriptionLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the toolbar panel.
     * @return The toolbar panel
     */
    @NotNull
    private JBPanel<JBPanel> createToolbarPanel() {
        JBPanel<JBPanel> panel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(JBUI.Borders.empty(5));
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshMetrics());
        panel.add(refreshButton);
        
        JButton resetButton = new JButton("Reset Metrics");
        resetButton.addActionListener(e -> resetMetrics());
        panel.add(resetButton);
        
        return panel;
    }
    
    /**
     * Starts the refresh timer.
     */
    private void startRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        
        refreshTimer = new Timer(REFRESH_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshMetrics();
            }
        });
        refreshTimer.start();
    }
    
    /**
     * Stops the refresh timer.
     */
    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }
    
    /**
     * Refreshes the metrics.
     */
    private void refreshMetrics() {
        try {
            // Get metrics from services
            Map<String, Integer> usageMetrics = aiServiceManager.getUsageMetrics();
            Map<String, Integer> cacheMetrics = patternCachingService.getCacheStatistics();
            Map<String, Integer> patternMetrics = patternRecognitionService.getMetrics();
            
            // Update usage metrics
            int totalRequests = usageMetrics.getOrDefault("totalRequests", 0);
            int patternMatches = usageMetrics.getOrDefault("patternMatches", 0);
            int apiCalls = usageMetrics.getOrDefault("apiCalls", 0);
            int patternMatchRate = totalRequests > 0 ? (patternMatches * 100) / totalRequests : 0;
            
            totalRequestsLabel.setText(String.valueOf(totalRequests));
            patternMatchesLabel.setText(String.valueOf(patternMatches));
            apiCallsLabel.setText(String.valueOf(apiCalls));
            patternMatchRateLabel.setText(patternMatchRate + "%");
            
            // Update cost savings
            int estimatedTokensSaved = usageMetrics.getOrDefault("estimatedTokensSaved", 0);
            double estimatedCostSaved = estimatedTokensSaved * COST_PER_1K_TOKENS / 1000;
            double estimatedTotalCost = (apiCalls > 0) ? (apiCalls * 1000 * COST_PER_1K_TOKENS / 1000) : 0;
            
            estimatedTokensSavedLabel.setText(NumberFormat.getIntegerInstance().format(estimatedTokensSaved));
            estimatedCostSavedLabel.setText("$" + NumberFormat.getCurrencyInstance().format(estimatedCostSaved)
                    .substring(1));
            estimatedTotalCostLabel.setText("$" + NumberFormat.getCurrencyInstance().format(estimatedTotalCost)
                    .substring(1));
            
            // Update pattern stats
            int cacheHitRatio = cacheMetrics.getOrDefault("hitRatio", 0);
            cacheHitRatioLabel.setText(cacheHitRatio + "%");
            
            // Update efficiency
            int efficiency = calculateSystemEfficiency(usageMetrics, cacheMetrics, patternMetrics);
            efficiencyProgressBar.setValue(efficiency);
            efficiencyProgressBar.setString(efficiency + "%");
            
            // Set progress bar color based on efficiency
            if (efficiency < 30) {
                efficiencyProgressBar.setForeground(JBColor.RED);
            } else if (efficiency < 70) {
                efficiencyProgressBar.setForeground(JBColor.ORANGE);
            } else {
                efficiencyProgressBar.setForeground(JBColor.GREEN);
            }
            
            LOG.debug("Refreshed metrics dashboard");
        } catch (Exception e) {
            LOG.error("Error refreshing metrics", e);
        }
    }
    
    /**
     * Calculates the system efficiency.
     * @param usageMetrics The usage metrics
     * @param cacheMetrics The cache metrics
     * @param patternMetrics The pattern metrics
     * @return The system efficiency (0-100)
     */
    private int calculateSystemEfficiency(
            Map<String, Integer> usageMetrics,
            Map<String, Integer> cacheMetrics,
            Map<String, Integer> patternMetrics
    ) {
        // Factors to consider for efficiency
        int totalRequests = usageMetrics.getOrDefault("totalRequests", 0);
        int patternMatches = usageMetrics.getOrDefault("patternMatches", 0);
        int cacheHitRatio = cacheMetrics.getOrDefault("hitRatio", 0);
        int patternMatchCount = patternMetrics.getOrDefault("patternMatchCount", 0);
        
        // If no requests, return 0
        if (totalRequests == 0) {
            return 0;
        }
        
        // Calculate efficiency
        int apiEfficiency = Math.min(100, (patternMatches * 100) / Math.max(1, totalRequests));
        int weightedEfficiency = (apiEfficiency * 60 + cacheHitRatio * 40) / 100;
        
        return weightedEfficiency;
    }
    
    /**
     * Resets the metrics.
     */
    private void resetMetrics() {
        int response = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to reset all metrics?",
                "Reset Metrics",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        
        if (response == JOptionPane.YES_OPTION) {
            // Reset metrics in services
            // Note: actual implementation would call reset methods on service
            
            // Refresh display
            refreshMetrics();
            
            LOG.info("Reset metrics dashboard");
        }
    }
    
    /**
     * Disposes the panel.
     */
    public void dispose() {
        stopRefreshTimer();
    }
}