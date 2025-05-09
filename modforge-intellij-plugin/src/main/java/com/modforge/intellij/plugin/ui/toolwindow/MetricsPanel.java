package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Panel for the Metrics tab in the tool window.
 * This panel displays usage statistics and metrics.
 */
public final class MetricsPanel {
    private static final Logger LOG = Logger.getInstance(MetricsPanel.class);
    private static final long UPDATE_INTERVAL_MS = 5000; // 5 seconds
    
    private final Project project;
    private final PatternRecognitionService patternRecognitionService;
    private final ContinuousDevelopmentService continuousDevelopmentService;
    
    private JPanel mainPanel;
    private JPanel patternMetricsPanel;
    private JPanel continuousDevMetricsPanel;
    
    private JBLabel patternCountLabel;
    private JBLabel apiSavingsLabel;
    private JBLabel successRateLabel;
    private JBLabel fixedErrorCountLabel;
    private JBLabel addedFeatureCountLabel;
    private JBLabel continuousDevStatusLabel;
    
    private Timer refreshTimer;
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     */
    public MetricsPanel(@NotNull Project project) {
        this.project = project;
        this.patternRecognitionService = PatternRecognitionService.getInstance();
        this.continuousDevelopmentService = ContinuousDevelopmentService.getInstance(project);
        
        createUI();
        startRefreshTimer();
    }
    
    /**
     * Gets the panel content.
     * @return The panel content
     */
    @NotNull
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Creates the UI for the panel.
     */
    private void createUI() {
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create pattern metrics panel
        patternMetricsPanel = createPatternMetricsPanel();
        
        // Create continuous development metrics panel
        continuousDevMetricsPanel = createContinuousDevMetricsPanel();
        
        // Create content panel
        JPanel contentPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Pattern Recognition Metrics", UIUtil.ComponentStyle.LARGE))
                .addComponentFillVertically(patternMetricsPanel, 0)
                .addVerticalGap(20)
                .addComponent(new JBLabel("Continuous Development Metrics", UIUtil.ComponentStyle.LARGE))
                .addComponentFillVertically(continuousDevMetricsPanel, 0)
                .addVerticalGap(10)
                .getPanel();
        
        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        // Add scroll pane to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh metrics
        refreshMetrics();
    }
    
    /**
     * Creates the pattern metrics panel.
     * @return The pattern metrics panel
     */
    @NotNull
    private JPanel createPatternMetricsPanel() {
        // Create labels
        patternCountLabel = new JBLabel("Total patterns: 0");
        patternCountLabel.setBorder(JBUI.Borders.empty(5, 0));
        
        apiSavingsLabel = new JBLabel("Estimated API cost savings: $0.00");
        apiSavingsLabel.setBorder(JBUI.Borders.empty(5, 0));
        
        successRateLabel = new JBLabel("Pattern match success rate: 0%");
        successRateLabel.setBorder(JBUI.Borders.empty(5, 0));
        
        // Create panel
        return FormBuilder.createFormBuilder()
                .addComponent(patternCountLabel)
                .addComponent(apiSavingsLabel)
                .addComponent(successRateLabel)
                .addVerticalGap(10)
                .getPanel();
    }
    
    /**
     * Creates the continuous development metrics panel.
     * @return The continuous development metrics panel
     */
    @NotNull
    private JPanel createContinuousDevMetricsPanel() {
        // Create labels
        continuousDevStatusLabel = new JBLabel("Status: Not running");
        continuousDevStatusLabel.setBorder(JBUI.Borders.empty(5, 0));
        
        fixedErrorCountLabel = new JBLabel("Errors fixed: 0");
        fixedErrorCountLabel.setBorder(JBUI.Borders.empty(5, 0));
        
        addedFeatureCountLabel = new JBLabel("Features added: 0");
        addedFeatureCountLabel.setBorder(JBUI.Borders.empty(5, 0));
        
        // Create panel
        return FormBuilder.createFormBuilder()
                .addComponent(continuousDevStatusLabel)
                .addComponent(fixedErrorCountLabel)
                .addComponent(addedFeatureCountLabel)
                .addVerticalGap(10)
                .getPanel();
    }
    
    /**
     * Starts the refresh timer.
     */
    private void startRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        
        refreshTimer = new Timer("ModForge-MetricsRefresh");
        
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> refreshMetrics());
            }
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS);
    }
    
    /**
     * Stops the refresh timer.
     */
    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
    
    /**
     * Refreshes the metrics.
     */
    private void refreshMetrics() {
        try {
            // Get pattern statistics
            Map<String, Object> patternStats = patternRecognitionService.getStatistics();
            
            // Update pattern metrics
            int totalPatternCount = (int) patternStats.getOrDefault("totalPatternCount", 0);
            patternCountLabel.setText("Total patterns: " + totalPatternCount);
            
            double estimatedCostSaved = (double) patternStats.getOrDefault("estimatedCostSaved", 0.0);
            NumberFormat currencyFormat = new DecimalFormat("$#,##0.00");
            apiSavingsLabel.setText("Estimated API cost savings: " + currencyFormat.format(estimatedCostSaved));
            
            double successRate = (double) patternStats.getOrDefault("successRate", 0.0);
            NumberFormat percentFormat = new DecimalFormat("0.0%");
            successRateLabel.setText("Pattern match success rate: " + percentFormat.format(successRate));
            
            // Get continuous development statistics
            Map<String, Object> continuousDevStats = continuousDevelopmentService.getStatistics();
            
            // Update continuous development metrics
            boolean running = (boolean) continuousDevStats.getOrDefault("running", false);
            continuousDevStatusLabel.setText("Status: " + (running ? "Running" : "Not running"));
            
            int fixedErrorCount = (int) continuousDevStats.getOrDefault("fixedErrorCount", 0);
            fixedErrorCountLabel.setText("Errors fixed: " + fixedErrorCount);
            
            int addedFeatureCount = (int) continuousDevStats.getOrDefault("addedFeatureCount", 0);
            addedFeatureCountLabel.setText("Features added: " + addedFeatureCount);
        } catch (Exception e) {
            LOG.error("Error refreshing metrics", e);
        }
    }
}