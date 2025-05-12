package com.modforge.intellij.plugin.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.ai.pattern.PatternLearningSystem;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Map;

/**
 * Settings panel for pattern recognition system
 * Allows configuring the pattern learning system and viewing statistics
 */
public class PatternRecognitionSettingsPanel implements Configurable {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionSettingsPanel.class);
    
    private final Project project;
    private final PatternRecognitionService patternService;
    private final PatternLearningSystem patternLearningSystem;
    
    // UI components
    private JPanel mainPanel;
    private JBCheckBox enablePatternRecognitionCheckbox;
    private JBTextField maxPatternsField;
    private JBTextField minConfidenceThresholdField;
    private JBTextField minSuccessesField;
    private JTable statisticsTable;
    private DefaultTableModel statisticsTableModel;
    
    /**
     * Create settings panel
     * 
     * @param project The current project
     */
    public PatternRecognitionSettingsPanel(@NotNull Project project) {
        this.project = project;
        this.patternService = project.getService(PatternRecognitionService.class);
        this.patternLearningSystem = PatternLearningSystem.getInstance(project);
        
        createUI();
    }
    
    /**
     * Create the UI components
     */
    private void createUI() {
        // Main settings panel
        JPanel settingsPanel = createSettingsPanel();
        
        // Statistics panel
        JPanel statisticsPanel = createStatisticsPanel();
        
        // Create main panel with both sections
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(settingsPanel, BorderLayout.NORTH);
        mainPanel.add(statisticsPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create the settings panel for configuring pattern recognition
     * 
     * @return The settings panel
     */
    private JPanel createSettingsPanel() {
        enablePatternRecognitionCheckbox = new JBCheckBox("Enable pattern recognition for AI prompts");
        
        // Configuration fields with labels
        maxPatternsField = new JBTextField(5);
        minConfidenceThresholdField = new JBTextField(5);
        minSuccessesField = new JBTextField(5);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton resetStatisticsButton = new JButton("Reset Statistics");
        resetStatisticsButton.addActionListener(e -> resetStatistics());
        
        JButton viewPatternsButton = new JButton("View Patterns");
        viewPatternsButton.addActionListener(e -> viewPatterns());
        
        buttonsPanel.add(resetStatisticsButton);
        buttonsPanel.add(viewPatternsButton);
        
        // Create form
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(enablePatternRecognitionCheckbox)
                .addSeparator()
                .addLabeledComponent("Maximum patterns to store:", maxPatternsField)
                .addLabeledComponent("Minimum confidence threshold (0.0-1.0):", minConfidenceThresholdField)
                .addLabeledComponent("Minimum successful matches for reliability:", minSuccessesField)
                .addComponent(buttonsPanel)
                .addSeparator()
                .getPanel();
        
        // Add border
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Pattern Recognition Settings", 
                TitledBorder.LEFT, TitledBorder.TOP));
        
        return panel;
    }
    
    /**
     * Create the statistics panel for viewing pattern learning statistics
     * 
     * @return The statistics panel
     */
    private JPanel createStatisticsPanel() {
        // Create table for statistics
        statisticsTableModel = new DefaultTableModel(
                new Object[]{"Metric", "Value"}, 0);
        
        statisticsTable = new JTable(statisticsTableModel);
        statisticsTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        statisticsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        statisticsTable.setShowGrid(false);
        statisticsTable.setEnabled(false);
        
        JBScrollPane scrollPane = new JBScrollPane(statisticsTable);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add border
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Pattern Recognition Statistics", 
                TitledBorder.LEFT, TitledBorder.TOP));
        
        return panel;
    }
    
    /**
     * Reset pattern recognition statistics
     */
    private void resetStatistics() {
        int result = Messages.showYesNoDialog(
                "Are you sure you want to reset all pattern recognition statistics?",
                "Reset Statistics",
                Messages.getQuestionIcon());
        
        if (result == CompatibilityUtil.DIALOG_YES) {
            patternService.resetStatistics();
            updateStatisticsTable();
        }
    }
    
    /**
     * View patterns in a dialog
     */
    private void viewPatterns() {
        // This would require implementing a patterns browser dialog
        Messages.showInfoMessage(
                "Pattern browser not implemented yet. " +
                "Please check the logs for pattern information.",
                "View Patterns");
    }
    
    /**
     * Update the statistics table with current data
     */
    private void updateStatisticsTable() {
        // Clear the table
        statisticsTableModel.setRowCount(0);
        
        // Get the statistics
        Map<String, Object> stats = patternService.getStatistics();
        
        // Add rows for each statistic
        addStatisticRow("Enabled", patternService.isEnabled() ? "Yes" : "No");
        addStatisticRow("Total Patterns", stats.get("totalPatterns"));
        addStatisticRow("Reliable Patterns", stats.get("reliablePatterns"));
        addStatisticRow("Total Requests", stats.get("totalRequests"));
        addStatisticRow("Pattern Matches", stats.get("patternMatches"));
        addStatisticRow("API Calls", stats.get("apiCalls"));
        
        // Format hit rate as percentage
        float hitRate = (float) stats.get("hitRate");
        addStatisticRow("Hit Rate", String.format("%.1f%%", hitRate * 100));
        
        // Format tokens saved
        int tokensSaved = (int) stats.get("estimatedTokensSaved");
        addStatisticRow("Estimated Tokens Saved", NumberFormat.getInstance().format(tokensSaved));
        
        // Format cost saved as dollars
        int costSavedCents = (int) stats.get("estimatedCostSavedCents");
        addStatisticRow("Estimated Cost Saved", String.format("$%.2f", costSavedCents / 100.0));
    }
    
    /**
     * Add a row to the statistics table
     * 
     * @param metric The metric name
     * @param value The metric value
     */
    private void addStatisticRow(String metric, Object value) {
        statisticsTableModel.addRow(new Object[]{metric, value});
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Pattern Recognition";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        boolean enabledModified = enablePatternRecognitionCheckbox.isSelected() != patternService.isEnabled();
        
        boolean maxPatternsModified = false;
        boolean minConfidenceModified = false;
        boolean minSuccessesModified = false;
        
        try {
            int maxPatterns = Integer.parseInt(maxPatternsField.getText());
            maxPatternsModified = maxPatterns != patternLearningSystem.getMaxPatterns();
            
            float minConfidence = Float.parseFloat(minConfidenceThresholdField.getText());
            minConfidenceModified = Math.abs(minConfidence - patternLearningSystem.getMinConfidenceThreshold()) > 0.001;
            
            int minSuccesses = Integer.parseInt(minSuccessesField.getText());
            minSuccessesModified = minSuccesses != patternLearningSystem.getMinSuccessfulMatchesForPattern();
        } catch (NumberFormatException e) {
            // If parsing fails, consider modified to trigger validation on apply
            return true;
        }
        
        return enabledModified || maxPatternsModified || minConfidenceModified || minSuccessesModified;
    }
    
    @Override
    public void apply() {
        // Validate input before applying
        try {
            int maxPatterns = Integer.parseInt(maxPatternsField.getText());
            if (maxPatterns < 10 || maxPatterns > 10000) {
                throw new NumberFormatException("Max patterns must be between 10 and 10000");
            }
            
            float minConfidence = Float.parseFloat(minConfidenceThresholdField.getText());
            if (minConfidence < 0 || minConfidence > 1) {
                throw new NumberFormatException("Confidence threshold must be between 0.0 and 1.0");
            }
            
            int minSuccesses = Integer.parseInt(minSuccessesField.getText());
            if (minSuccesses < 1 || minSuccesses > 100) {
                throw new NumberFormatException("Minimum successes must be between 1 and 100");
            }
            
            // Apply changes
            patternService.setEnabled(enablePatternRecognitionCheckbox.isSelected());
            patternLearningSystem.setMaxPatterns(maxPatterns);
            patternLearningSystem.setMinConfidenceThreshold(minConfidence);
            patternLearningSystem.setMinSuccessfulMatchesForPattern(minSuccesses);
            
            LOG.info("Applied pattern recognition settings changes");
        } catch (NumberFormatException e) {
            Messages.showErrorDialog(
                    "Invalid input: " + e.getMessage(),
                    "Settings Error");
        }
    }
    
    @Override
    public void reset() {
        enablePatternRecognitionCheckbox.setSelected(patternService.isEnabled());
        maxPatternsField.setText(String.valueOf(patternLearningSystem.getMaxPatterns()));
        minConfidenceThresholdField.setText(String.format("%.2f", patternLearningSystem.getMinConfidenceThreshold()));
        minSuccessesField.setText(String.valueOf(patternLearningSystem.getMinSuccessfulMatchesForPattern()));
        
        updateStatisticsTable();
    }
}