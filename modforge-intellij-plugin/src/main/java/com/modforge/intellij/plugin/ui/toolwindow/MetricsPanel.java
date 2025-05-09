package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Panel for displaying metrics and statistics.
 */
public final class MetricsPanel extends SimpleToolWindowPanel {
    private static final int REFRESH_INTERVAL_MS = 5000; // 5 seconds
    
    private final Project project;
    private final PatternRecognitionService patternRecognitionService;
    private final ContinuousDevelopmentService continuousDevelopmentService;
    
    private JPanel mainPanel;
    private JPanel patternRecognitionPanel;
    private JPanel continuousDevelopmentPanel;
    private JLabel totalRequestsLabel;
    private JLabel patternMatchesLabel;
    private JLabel apiCallsLabel;
    private JLabel estimatedTokensSavedLabel;
    private JLabel estimatedCostSavedLabel;
    private JLabel codeGenPatternCountLabel;
    private JLabel errorFixPatternCountLabel;
    private JLabel docPatternCountLabel;
    private JLabel featurePatternCountLabel;
    private JLabel runningStatusLabel;
    private JLabel checkIntervalLabel;
    private JLabel fixedErrorCountLabel;
    private JLabel addedFeatureCountLabel;
    
    private Timer refreshTimer;
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     */
    public MetricsPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.patternRecognitionService = PatternRecognitionService.getInstance();
        this.continuousDevelopmentService = ContinuousDevelopmentService.getInstance(project);
        
        initializeUI();
        
        setContent(mainPanel);
        
        startRefreshTimer();
    }
    
    /**
     * Initializes the UI.
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(5));
        
        patternRecognitionPanel = createPatternRecognitionPanel();
        continuousDevelopmentPanel = createContinuousDevelopmentPanel();
        
        contentPanel.add(patternRecognitionPanel, BorderLayout.NORTH);
        contentPanel.add(continuousDevelopmentPanel, BorderLayout.CENTER);
        
        mainPanel.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton refreshButton = new JButton("Refresh", AllIcons.Actions.Refresh);
        refreshButton.addActionListener(e -> refreshMetrics());
        
        JButton resetButton = new JButton("Reset Statistics", AllIcons.Actions.GC);
        resetButton.addActionListener(e -> resetStatistics());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(resetButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Creates the pattern recognition metrics panel.
     * @return The panel
     */
    private JPanel createPatternRecognitionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                "Pattern Recognition",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Usage metrics
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(new JBLabel("<html><b>Usage Metrics</b></html>"), c);
        
        c.gridwidth = 1;
        c.gridy++;
        panel.add(new JBLabel("Total Requests:"), c);
        c.gridx = 1;
        totalRequestsLabel = new JBLabel("0");
        panel.add(totalRequestsLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Pattern Matches:"), c);
        c.gridx = 1;
        patternMatchesLabel = new JBLabel("0");
        panel.add(patternMatchesLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("API Calls:"), c);
        c.gridx = 1;
        apiCallsLabel = new JBLabel("0");
        panel.add(apiCallsLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Estimated Tokens Saved:"), c);
        c.gridx = 1;
        estimatedTokensSavedLabel = new JBLabel("0");
        panel.add(estimatedTokensSavedLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Estimated Cost Saved:"), c);
        c.gridx = 1;
        estimatedCostSavedLabel = new JBLabel("$0.00");
        panel.add(estimatedCostSavedLabel, c);
        
        // Pattern counts
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        panel.add(Box.createVerticalStrut(10), c);
        
        c.gridy++;
        panel.add(new JBLabel("<html><b>Pattern Counts</b></html>"), c);
        
        c.gridwidth = 1;
        c.gridy++;
        panel.add(new JBLabel("Code Generation:"), c);
        c.gridx = 1;
        codeGenPatternCountLabel = new JBLabel("0");
        panel.add(codeGenPatternCountLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Error Fix:"), c);
        c.gridx = 1;
        errorFixPatternCountLabel = new JBLabel("0");
        panel.add(errorFixPatternCountLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Documentation:"), c);
        c.gridx = 1;
        docPatternCountLabel = new JBLabel("0");
        panel.add(docPatternCountLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Feature Addition:"), c);
        c.gridx = 1;
        featurePatternCountLabel = new JBLabel("0");
        panel.add(featurePatternCountLabel, c);
        
        return panel;
    }
    
    /**
     * Creates the continuous development metrics panel.
     * @return The panel
     */
    private JPanel createContinuousDevelopmentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                "Continuous Development",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JBLabel("Status:"), c);
        c.gridx = 1;
        runningStatusLabel = new JBLabel("Stopped");
        panel.add(runningStatusLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Check Interval:"), c);
        c.gridx = 1;
        checkIntervalLabel = new JBLabel("60 seconds");
        panel.add(checkIntervalLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Fixed Errors:"), c);
        c.gridx = 1;
        fixedErrorCountLabel = new JBLabel("0");
        panel.add(fixedErrorCountLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JBLabel("Added Features:"), c);
        c.gridx = 1;
        addedFeatureCountLabel = new JBLabel("0");
        panel.add(addedFeatureCountLabel, c);
        
        return panel;
    }
    
    /**
     * Starts the refresh timer.
     */
    private void startRefreshTimer() {
        refreshTimer = new Timer("ModForge-MetricsRefresh", true);
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(() -> refreshMetrics());
            }
        }, 0, REFRESH_INTERVAL_MS);
    }
    
    /**
     * Refreshes the metrics.
     */
    private void refreshMetrics() {
        // Pattern recognition metrics
        Map<String, Object> patternMetrics = patternRecognitionService.getStatistics();
        
        // Usage metrics
        totalRequestsLabel.setText(String.valueOf(patternMetrics.getOrDefault("totalRequests", 0)));
        patternMatchesLabel.setText(String.valueOf(patternMetrics.getOrDefault("patternMatches", 0)));
        apiCallsLabel.setText(String.valueOf(patternMetrics.getOrDefault("apiCalls", 0)));
        estimatedTokensSavedLabel.setText(String.valueOf(patternMetrics.getOrDefault("estimatedTokensSaved", 0)));
        
        double costSaved = (double) patternMetrics.getOrDefault("estimatedCostSaved", 0.0);
        estimatedCostSavedLabel.setText("$" + NumberFormat.getCurrencyInstance().format(costSaved));
        
        // Pattern counts
        @SuppressWarnings("unchecked")
        Map<String, Integer> patternCounts = (Map<String, Integer>) patternMetrics.getOrDefault("patternCounts", new java.util.HashMap<>());
        
        codeGenPatternCountLabel.setText(String.valueOf(patternCounts.getOrDefault("codeGeneration", 0)));
        errorFixPatternCountLabel.setText(String.valueOf(patternCounts.getOrDefault("errorFix", 0)));
        docPatternCountLabel.setText(String.valueOf(patternCounts.getOrDefault("documentation", 0)));
        featurePatternCountLabel.setText(String.valueOf(patternCounts.getOrDefault("featureAddition", 0)));
        
        // Continuous development metrics
        Map<String, Object> continuousMetrics = continuousDevelopmentService.getStatistics();
        
        boolean running = (boolean) continuousMetrics.getOrDefault("running", false);
        runningStatusLabel.setText(running ? "Running" : "Stopped");
        runningStatusLabel.setIcon(running ? AllIcons.Actions.Execute : AllIcons.Actions.Suspend);
        
        long checkIntervalMs = (long) continuousMetrics.getOrDefault("checkIntervalMs", 60000L);
        checkIntervalLabel.setText((checkIntervalMs / 1000) + " seconds");
        
        fixedErrorCountLabel.setText(String.valueOf(continuousMetrics.getOrDefault("fixedErrorCount", 0)));
        addedFeatureCountLabel.setText(String.valueOf(continuousMetrics.getOrDefault("addedFeatureCount", 0)));
    }
    
    /**
     * Resets the statistics.
     */
    private void resetStatistics() {
        patternRecognitionService.resetStatistics();
        continuousDevelopmentService.resetStatistics();
        refreshMetrics();
    }
    
    /**
     * Disposes the panel.
     */
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}