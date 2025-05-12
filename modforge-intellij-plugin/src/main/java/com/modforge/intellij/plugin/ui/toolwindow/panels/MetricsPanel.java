package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for displaying usage metrics.
 * This panel shows statistics about API usage, cache hits, and cost estimates.
 */
public class MetricsPanel {
    private static final Logger LOG = Logger.getInstance(MetricsPanel.class);
    
    private final Project project;
    private final JPanel mainPanel;
    private final List<JComponent> metricComponents = new ArrayList<>();
    private final Alarm refreshAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
    private final NumberFormat formatter = new DecimalFormat("#0.000");
    
    // Metrics components
    private JBLabel requestCountLabel;
    private JBLabel cacheHitCountLabel;
    private JBLabel cacheHitRateLabel;
    private JBLabel failureCountLabel;
    private JBLabel tokensUsedLabel;
    private JBLabel estimatedCostLabel;
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     */
    public MetricsPanel(@NotNull Project project) {
        this.project = project;
        
        mainPanel = new JBPanel<>(new BorderLayout());
        
        // Create metrics panel
        JPanel metricsPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        metricsPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add metrics
        requestCountLabel = addMetric(metricsPanel, "Requests:", "0");
        cacheHitCountLabel = addMetric(metricsPanel, "Cache Hits:", "0");
        cacheHitRateLabel = addMetric(metricsPanel, "Cache Hit Rate:", "0%");
        failureCountLabel = addMetric(metricsPanel, "Failures:", "0");
        tokensUsedLabel = addMetric(metricsPanel, "Tokens Used:", "0");
        estimatedCostLabel = addMetric(metricsPanel, "Estimated Cost:", "$0.00");
        
        // Create action toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // Add Refresh action
        actionGroup.add(new AnAction("Refresh", "Refresh metrics", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refreshMetrics();
            }
        });
        
        // Add Reset action
        actionGroup.add(new AnAction("Reset", "Reset metrics", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        mainPanel,
                        "Are you sure you want to reset all metrics?",
                        "Reset Metrics",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    resetMetrics();
                }
            }
        });
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "ModForgeMetrics",
                actionGroup,
                true
        );
        
        // Create toolbar wrapper
        BorderLayoutPanel toolbarWrapper = JBUI.Panels.simplePanel();
        toolbarWrapper.addToLeft(toolbar.getComponent());
        
        // Create heading
        JBLabel headingLabel = new JBLabel("AI Service Metrics");
        headingLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 2));
        headingLabel.setBorder(JBUI.Borders.empty(10, 10, 5, 10));
        
        // Set up main panel
        JPanel centerPanel = new JBPanel<>(new BorderLayout());
        centerPanel.add(headingLabel, BorderLayout.NORTH);
        centerPanel.add(metricsPanel, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(toolbarWrapper, BorderLayout.NORTH);
        
        // Start refresh timer
        startRefreshTimer();
        
        // Initial refresh
        refreshMetrics();
    }
    
    /**
     * Gets the main panel.
     * @return The main panel
     */
    @NotNull
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Adds a metric to the panel.
     * @param panel The panel to add the metric to
     * @param label The metric label
     * @param value The initial value
     * @return The value label
     */
    @NotNull
    private JBLabel addMetric(@NotNull JPanel panel, @NotNull String label, @NotNull String value) {
        JBLabel labelComponent = new JBLabel(label);
        labelComponent.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
        
        JBLabel valueComponent = new JBLabel(value);
        
        panel.add(labelComponent);
        panel.add(valueComponent);
        
        metricComponents.add(labelComponent);
        metricComponents.add(valueComponent);
        
        return valueComponent;
    }
    
    /**
     * Starts the refresh timer.
     */
    private void startRefreshTimer() {
        if (refreshAlarm.isDisposed()) {
            return;
        }
        
        refreshAlarm.cancelAllRequests();
        
        refreshAlarm.addRequest(() -> {
            ApplicationManager.getApplication().invokeLater(this::refreshMetrics);
            startRefreshTimer();
        }, 5000); // Refresh every 5 seconds
    }
    
    /**
     * Refreshes the metrics.
     */
    private void refreshMetrics() {
        try {
            AIServiceManager aiServiceManager = AIServiceManager.getInstance(project);
            
            long requestCount = aiServiceManager.getRequestCount();
            long cacheHitCount = aiServiceManager.getCacheHitCount();
            long failureCount = aiServiceManager.getFailureCount();
            long tokensUsed = aiServiceManager.getTotalTokensUsed();
            double estimatedCost = aiServiceManager.getEstimatedCost();
            
            // Calculate cache hit rate
            double cacheHitRate = requestCount > 0 ?
                    (double) cacheHitCount / (requestCount + cacheHitCount) : 0;
            
            // Update UI
            requestCountLabel.setText(String.valueOf(requestCount));
            cacheHitCountLabel.setText(String.valueOf(cacheHitCount));
            cacheHitRateLabel.setText(String.format("%.1f%%", cacheHitRate * 100));
            failureCountLabel.setText(String.valueOf(failureCount));
            tokensUsedLabel.setText(String.format("%,d", tokensUsed));
            estimatedCostLabel.setText("$" + formatter.format(estimatedCost));
            
            // Highlight cost if high
            if (estimatedCost > 10) {
                estimatedCostLabel.setForeground(JBColor.RED);
            } else if (estimatedCost > 5) {
                estimatedCostLabel.setForeground(JBColor.ORANGE);
            } else {
                estimatedCostLabel.setForeground(UIUtil.getLabelForeground());
            }
        } catch (Exception e) {
            LOG.error("Error refreshing metrics", e);
        }
    }
    
    /**
     * Resets the metrics.
     */
    private void resetMetrics() {
        // Currently not implemented - would require API in AIServiceManager to reset counters
        JOptionPane.showMessageDialog(
                mainPanel,
                "The ability to reset metrics is currently not implemented.",
                "Feature Not Available",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Disposes the panel.
     */
    public void dispose() {
        refreshAlarm.cancelAllRequests();
        refreshAlarm.dispose();
    }
}