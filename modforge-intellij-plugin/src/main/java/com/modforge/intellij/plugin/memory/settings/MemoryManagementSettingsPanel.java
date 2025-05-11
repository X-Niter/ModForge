package com.modforge.intellij.plugin.memory.settings;

import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Settings panel for memory management
 */
public class MemoryManagementSettingsPanel extends ConfigurableBase<MemoryManagementSettingsPanel, MemoryManagementSettings.State> {
    private JPanel mainPanel;
    
    // General settings
    private JBCheckBox enableAutomaticOptimizationCheckBox;
    private JBCheckBox showMemoryWidgetCheckBox;
    
    // Thresholds
    private JSlider warningThresholdSlider;
    private JSlider criticalThresholdSlider;
    private JSlider emergencyThresholdSlider;
    private JLabel warningThresholdLabel;
    private JLabel criticalThresholdLabel;
    private JLabel emergencyThresholdLabel;
    
    // Automatic optimization
    private JSpinner optimizationIntervalSpinner;
    private JBCheckBox optimizeOnLowMemoryCheckBox;
    private JBCheckBox optimizeBeforeLongRunningTasksCheckBox;
    
    // Continuous development settings
    private JBCheckBox enableMemoryAwareContinuousServiceCheckBox;
    private JSpinner continuousServiceDefaultIntervalSpinner;
    private JSpinner continuousServiceReducedIntervalSpinner;
    private JSpinner continuousServiceMinimumIntervalSpinner;
    
    public MemoryManagementSettingsPanel() {
        super("com.modforge.intellij.plugin.memory.settings.MemoryManagementSettingsPanel", 
              "Memory Management", 
              "ModForge.MemoryManagement");
    }
    
    @Override
    protected @NotNull MemoryManagementSettings.State getSettings() {
        return MemoryManagementSettings.getInstance().getState();
    }
    
    @Override
    protected Runnable enableSearch(String option) {
        return null; // No search functionality
    }
    
    @Override
    protected @Nullable JComponent createPanel() {
        initializeComponents();
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .setFormLeftIndent(20)
            .addComponent(createGeneralSettingsPanel())
            .addVerticalGap(10)
            .addComponent(createThresholdsPanel())
            .addVerticalGap(10)
            .addComponent(createAutomaticOptimizationPanel())
            .addVerticalGap(10)
            .addComponent(createContinuousServicePanel());
        
        mainPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = builder.getPanel();
        innerPanel.setBorder(JBUI.Borders.empty(10));
        
        mainPanel.add(new JBScrollPane(innerPanel), BorderLayout.CENTER);
        
        loadSettings();
        setupListeners();
        
        return mainPanel;
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // General settings
        enableAutomaticOptimizationCheckBox = new JBCheckBox("Enable automatic memory optimization");
        showMemoryWidgetCheckBox = new JBCheckBox("Show memory usage widget in status bar");
        
        // Thresholds
        warningThresholdSlider = new JSlider(50, 95, 75);
        criticalThresholdSlider = new JSlider(55, 98, 85);
        emergencyThresholdSlider = new JSlider(60, 99, 95);
        
        warningThresholdSlider.setMajorTickSpacing(5);
        warningThresholdSlider.setMinorTickSpacing(1);
        warningThresholdSlider.setPaintTicks(true);
        warningThresholdSlider.setPaintLabels(true);
        
        criticalThresholdSlider.setMajorTickSpacing(5);
        criticalThresholdSlider.setMinorTickSpacing(1);
        criticalThresholdSlider.setPaintTicks(true);
        criticalThresholdSlider.setPaintLabels(true);
        
        emergencyThresholdSlider.setMajorTickSpacing(5);
        emergencyThresholdSlider.setMinorTickSpacing(1);
        emergencyThresholdSlider.setPaintTicks(true);
        emergencyThresholdSlider.setPaintLabels(true);
        
        warningThresholdLabel = new JLabel("Warning threshold: 75%");
        criticalThresholdLabel = new JLabel("Critical threshold: 85%");
        emergencyThresholdLabel = new JLabel("Emergency threshold: 95%");
        
        // Automatic optimization
        optimizationIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 120, 5));
        optimizeOnLowMemoryCheckBox = new JBCheckBox("Optimize memory on low memory conditions");
        optimizeBeforeLongRunningTasksCheckBox = new JBCheckBox("Optimize memory before long-running tasks");
        
        // Continuous development settings
        enableMemoryAwareContinuousServiceCheckBox = new JBCheckBox("Enable memory-aware continuous service");
        continuousServiceDefaultIntervalSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        continuousServiceReducedIntervalSpinner = new JSpinner(new SpinnerNumberModel(15, 5, 120, 5));
        continuousServiceMinimumIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 240, 10));
    }
    
    /**
     * Create the general settings panel
     * 
     * @return The general settings panel
     */
    private JPanel createGeneralSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("General Settings"));
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(enableAutomaticOptimizationCheckBox)
            .addComponent(showMemoryWidgetCheckBox);
        
        panel.add(builder.getPanel(), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create the thresholds panel
     * 
     * @return The thresholds panel
     */
    private JPanel createThresholdsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Memory Thresholds"));
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(warningThresholdLabel)
            .addComponent(warningThresholdSlider)
            .addVerticalGap(10)
            .addComponent(criticalThresholdLabel)
            .addComponent(criticalThresholdSlider)
            .addVerticalGap(10)
            .addComponent(emergencyThresholdLabel)
            .addComponent(emergencyThresholdSlider);
        
        panel.add(builder.getPanel(), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create the automatic optimization panel
     * 
     * @return The automatic optimization panel
     */
    private JPanel createAutomaticOptimizationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Automatic Optimization"));
        
        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        intervalPanel.add(new JLabel("Optimization interval (minutes):"));
        intervalPanel.add(optimizationIntervalSpinner);
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(intervalPanel)
            .addComponent(optimizeOnLowMemoryCheckBox)
            .addComponent(optimizeBeforeLongRunningTasksCheckBox);
        
        panel.add(builder.getPanel(), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create the continuous service panel
     * 
     * @return The continuous service panel
     */
    private JPanel createContinuousServicePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Memory-Aware Continuous Service"));
        
        JPanel defaultIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        defaultIntervalPanel.add(new JLabel("Default interval (minutes):"));
        defaultIntervalPanel.add(continuousServiceDefaultIntervalSpinner);
        
        JPanel reducedIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reducedIntervalPanel.add(new JLabel("Reduced interval (minutes):"));
        reducedIntervalPanel.add(continuousServiceReducedIntervalSpinner);
        
        JPanel minimumIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        minimumIntervalPanel.add(new JLabel("Minimum interval (minutes):"));
        minimumIntervalPanel.add(continuousServiceMinimumIntervalSpinner);
        
        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(enableMemoryAwareContinuousServiceCheckBox)
            .addComponent(defaultIntervalPanel)
            .addComponent(reducedIntervalPanel)
            .addComponent(minimumIntervalPanel);
        
        panel.add(builder.getPanel(), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Set up listeners for UI components
     */
    private void setupListeners() {
        // Update threshold labels when sliders change
        warningThresholdSlider.addChangeListener(e -> 
            warningThresholdLabel.setText("Warning threshold: " + warningThresholdSlider.getValue() + "%"));
        
        criticalThresholdSlider.addChangeListener(e -> 
            criticalThresholdLabel.setText("Critical threshold: " + criticalThresholdSlider.getValue() + "%"));
        
        emergencyThresholdSlider.addChangeListener(e -> 
            emergencyThresholdLabel.setText("Emergency threshold: " + emergencyThresholdSlider.getValue() + "%"));
        
        // Ensure thresholds are in order
        warningThresholdSlider.addChangeListener(e -> {
            if (warningThresholdSlider.getValue() >= criticalThresholdSlider.getValue()) {
                criticalThresholdSlider.setValue(warningThresholdSlider.getValue() + 1);
            }
        });
        
        criticalThresholdSlider.addChangeListener(e -> {
            if (criticalThresholdSlider.getValue() <= warningThresholdSlider.getValue()) {
                warningThresholdSlider.setValue(criticalThresholdSlider.getValue() - 1);
            }
            if (criticalThresholdSlider.getValue() >= emergencyThresholdSlider.getValue()) {
                emergencyThresholdSlider.setValue(criticalThresholdSlider.getValue() + 1);
            }
        });
        
        emergencyThresholdSlider.addChangeListener(e -> {
            if (emergencyThresholdSlider.getValue() <= criticalThresholdSlider.getValue()) {
                criticalThresholdSlider.setValue(emergencyThresholdSlider.getValue() - 1);
            }
        });
        
        // Ensure continuous service intervals are in order
        continuousServiceDefaultIntervalSpinner.addChangeListener(e -> {
            int defaultInterval = (Integer) continuousServiceDefaultIntervalSpinner.getValue();
            int reducedInterval = (Integer) continuousServiceReducedIntervalSpinner.getValue();
            
            if (defaultInterval >= reducedInterval) {
                continuousServiceReducedIntervalSpinner.setValue(defaultInterval + 5);
            }
        });
        
        continuousServiceReducedIntervalSpinner.addChangeListener(e -> {
            int defaultInterval = (Integer) continuousServiceDefaultIntervalSpinner.getValue();
            int reducedInterval = (Integer) continuousServiceReducedIntervalSpinner.getValue();
            int minimumInterval = (Integer) continuousServiceMinimumIntervalSpinner.getValue();
            
            if (reducedInterval <= defaultInterval) {
                continuousServiceDefaultIntervalSpinner.setValue(reducedInterval - 1);
            }
            
            if (reducedInterval >= minimumInterval) {
                continuousServiceMinimumIntervalSpinner.setValue(reducedInterval + 5);
            }
        });
        
        continuousServiceMinimumIntervalSpinner.addChangeListener(e -> {
            int reducedInterval = (Integer) continuousServiceReducedIntervalSpinner.getValue();
            int minimumInterval = (Integer) continuousServiceMinimumIntervalSpinner.getValue();
            
            if (minimumInterval <= reducedInterval) {
                continuousServiceReducedIntervalSpinner.setValue(minimumInterval - 5);
            }
        });
        
        // Enable/disable components based on checkboxes
        enableAutomaticOptimizationCheckBox.addActionListener(e -> {
            boolean enabled = enableAutomaticOptimizationCheckBox.isSelected();
            optimizationIntervalSpinner.setEnabled(enabled);
            optimizeOnLowMemoryCheckBox.setEnabled(enabled);
            optimizeBeforeLongRunningTasksCheckBox.setEnabled(enabled);
        });
        
        enableMemoryAwareContinuousServiceCheckBox.addActionListener(e -> {
            boolean enabled = enableMemoryAwareContinuousServiceCheckBox.isSelected();
            continuousServiceDefaultIntervalSpinner.setEnabled(enabled);
            continuousServiceReducedIntervalSpinner.setEnabled(enabled);
            continuousServiceMinimumIntervalSpinner.setEnabled(enabled);
        });
    }
    
    /**
     * Load settings from the settings class
     */
    private void loadSettings() {
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        
        // General settings
        enableAutomaticOptimizationCheckBox.setSelected(settings.isAutomaticOptimizationEnabled());
        showMemoryWidgetCheckBox.setSelected(settings.isShowMemoryWidget());
        
        // Thresholds
        warningThresholdSlider.setValue(settings.getWarningThresholdPercent());
        criticalThresholdSlider.setValue(settings.getCriticalThresholdPercent());
        emergencyThresholdSlider.setValue(settings.getEmergencyThresholdPercent());
        
        // Update labels
        warningThresholdLabel.setText("Warning threshold: " + settings.getWarningThresholdPercent() + "%");
        criticalThresholdLabel.setText("Critical threshold: " + settings.getCriticalThresholdPercent() + "%");
        emergencyThresholdLabel.setText("Emergency threshold: " + settings.getEmergencyThresholdPercent() + "%");
        
        // Automatic optimization
        optimizationIntervalSpinner.setValue(settings.getOptimizationIntervalMinutes());
        optimizeOnLowMemoryCheckBox.setSelected(settings.isOptimizeOnLowMemory());
        optimizeBeforeLongRunningTasksCheckBox.setSelected(settings.isOptimizeBeforeLongRunningTasks());
        
        // Continuous development settings
        enableMemoryAwareContinuousServiceCheckBox.setSelected(settings.isMemoryAwareContinuousServiceEnabled());
        continuousServiceDefaultIntervalSpinner.setValue(settings.getContinuousServiceDefaultIntervalMinutes());
        continuousServiceReducedIntervalSpinner.setValue(settings.getContinuousServiceReducedIntervalMinutes());
        continuousServiceMinimumIntervalSpinner.setValue(settings.getContinuousServiceMinimumIntervalMinutes());
        
        // Enable/disable components based on checkboxes
        boolean automaticOptimizationEnabled = enableAutomaticOptimizationCheckBox.isSelected();
        optimizationIntervalSpinner.setEnabled(automaticOptimizationEnabled);
        optimizeOnLowMemoryCheckBox.setEnabled(automaticOptimizationEnabled);
        optimizeBeforeLongRunningTasksCheckBox.setEnabled(automaticOptimizationEnabled);
        
        boolean memoryAwareContinuousServiceEnabled = enableMemoryAwareContinuousServiceCheckBox.isSelected();
        continuousServiceDefaultIntervalSpinner.setEnabled(memoryAwareContinuousServiceEnabled);
        continuousServiceReducedIntervalSpinner.setEnabled(memoryAwareContinuousServiceEnabled);
        continuousServiceMinimumIntervalSpinner.setEnabled(memoryAwareContinuousServiceEnabled);
    }
    
    @Override
    public void apply() {
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        
        // General settings
        settings.setAutomaticOptimizationEnabled(enableAutomaticOptimizationCheckBox.isSelected());
        settings.setShowMemoryWidget(showMemoryWidgetCheckBox.isSelected());
        
        // Thresholds
        settings.setWarningThresholdPercent(warningThresholdSlider.getValue());
        settings.setCriticalThresholdPercent(criticalThresholdSlider.getValue());
        settings.setEmergencyThresholdPercent(emergencyThresholdSlider.getValue());
        
        // Automatic optimization
        settings.setOptimizationIntervalMinutes((Integer) optimizationIntervalSpinner.getValue());
        settings.setOptimizeOnLowMemory(optimizeOnLowMemoryCheckBox.isSelected());
        settings.setOptimizeBeforeLongRunningTasks(optimizeBeforeLongRunningTasksCheckBox.isSelected());
        
        // Continuous development settings
        settings.setMemoryAwareContinuousServiceEnabled(enableMemoryAwareContinuousServiceCheckBox.isSelected());
        settings.setContinuousServiceDefaultIntervalMinutes((Integer) continuousServiceDefaultIntervalSpinner.getValue());
        settings.setContinuousServiceReducedIntervalMinutes((Integer) continuousServiceReducedIntervalSpinner.getValue());
        settings.setContinuousServiceMinimumIntervalMinutes((Integer) continuousServiceMinimumIntervalSpinner.getValue());
    }
    
    @Override
    public boolean isModified() {
        MemoryManagementSettings settings = MemoryManagementSettings.getInstance();
        
        return enableAutomaticOptimizationCheckBox.isSelected() != settings.isAutomaticOptimizationEnabled() ||
               showMemoryWidgetCheckBox.isSelected() != settings.isShowMemoryWidget() ||
               warningThresholdSlider.getValue() != settings.getWarningThresholdPercent() ||
               criticalThresholdSlider.getValue() != settings.getCriticalThresholdPercent() ||
               emergencyThresholdSlider.getValue() != settings.getEmergencyThresholdPercent() ||
               (Integer) optimizationIntervalSpinner.getValue() != settings.getOptimizationIntervalMinutes() ||
               optimizeOnLowMemoryCheckBox.isSelected() != settings.isOptimizeOnLowMemory() ||
               optimizeBeforeLongRunningTasksCheckBox.isSelected() != settings.isOptimizeBeforeLongRunningTasks() ||
               enableMemoryAwareContinuousServiceCheckBox.isSelected() != settings.isMemoryAwareContinuousServiceEnabled() ||
               (Integer) continuousServiceDefaultIntervalSpinner.getValue() != settings.getContinuousServiceDefaultIntervalMinutes() ||
               (Integer) continuousServiceReducedIntervalSpinner.getValue() != settings.getContinuousServiceReducedIntervalMinutes() ||
               (Integer) continuousServiceMinimumIntervalSpinner.getValue() != settings.getContinuousServiceMinimumIntervalMinutes();
    }
    
    @Override
    public void reset() {
        loadSettings();
    }
}