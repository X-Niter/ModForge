package com.modforge.intellij.plugin.memory.monitoring.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.monitoring.MemoryHealthMonitor;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot;
import com.modforge.intellij.plugin.memory.recovery.MemoryRecoveryManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

/**
 * Panel for monitoring memory usage and displaying memory statistics
 * Contains the memory trend chart and memory usage statistics
 */
public class MemoryMonitorPanel extends JBPanel<MemoryMonitorPanel> implements Disposable {
    private static final Logger LOG = Logger.getInstance(MemoryMonitorPanel.class);
    
    private static final Color NORMAL_COLOR = new JBColor(new Color(45, 183, 93), new Color(45, 183, 93));
    private static final Color WARNING_COLOR = new JBColor(new Color(255, 170, 0), new Color(255, 170, 0));
    private static final Color CRITICAL_COLOR = new JBColor(new Color(255, 102, 0), new Color(255, 102, 0));
    private static final Color EMERGENCY_COLOR = new JBColor(new Color(232, 17, 35), new Color(232, 17, 35));
    
    private final Project project;
    private final MemoryTrendChart memoryTrendChart;
    private final JPanel statsPanel;
    private final JLabel totalMemoryLabel;
    private final JLabel usedMemoryLabel;
    private final JLabel freeMemoryLabel;
    private final JLabel maxMemoryLabel;
    private final JLabel heapMemoryLabel;
    private final JLabel nonHeapMemoryLabel;
    private final JLabel usagePercentageLabel;
    private final JLabel memoryPressureLabel;
    private final JProgressBar memoryUsageBar;
    private final ComboBox<String> timeRangeComboBox;
    private final ComboBox<String> poolComboBox;
    private final JCheckBox showHeapCheckBox;
    private final JCheckBox showNonHeapCheckBox;
    private final JCheckBox showPoolsCheckBox;
    private final JButton optimizeButton;
    private final JButton refreshButton;
    
    private final Alarm updateAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    private final List<MemoryPoolMXBean> memoryPoolMXBeans;
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryMonitorPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(10));
        
        // Create the memory trend chart
        memoryTrendChart = new MemoryTrendChart(project);
        memoryTrendChart.setPreferredSize(new Dimension(800, 400));
        
        // Create the stats panel
        statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        
        statsPanel.add(new JBLabel("Total Memory:"), c);
        c.gridx = 1;
        totalMemoryLabel = new JLabel("Calculating...");
        statsPanel.add(totalMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Used Memory:"), c);
        c.gridx = 1;
        usedMemoryLabel = new JLabel("Calculating...");
        statsPanel.add(usedMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Free Memory:"), c);
        c.gridx = 1;
        freeMemoryLabel = new JLabel("Calculating...");
        statsPanel.add(freeMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Max Memory:"), c);
        c.gridx = 1;
        maxMemoryLabel = new JLabel("Calculating...");
        statsPanel.add(maxMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Heap Memory:"), c);
        c.gridx = 1;
        heapMemoryLabel = new JLabel("Calculating...");
        statsPanel.add(heapMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Non-Heap Memory:"), c);
        c.gridx = 1;
        nonHeapMemoryLabel = new JLabel("Calculating...");
        statsPanel.add(nonHeapMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Usage Percentage:"), c);
        c.gridx = 1;
        usagePercentageLabel = new JLabel("Calculating...");
        statsPanel.add(usagePercentageLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        statsPanel.add(new JBLabel("Memory Pressure:"), c);
        c.gridx = 1;
        memoryPressureLabel = new JLabel("Calculating...");
        statsPanel.add(memoryPressureLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        memoryUsageBar = new JProgressBar(0, 100);
        memoryUsageBar.setStringPainted(true);
        statsPanel.add(memoryUsageBar, c);
        
        // Create the control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Chart Controls"));
        
        // Time range combo box
        controlPanel.add(new JLabel("Time Range:"));
        timeRangeComboBox = new ComboBox<>(new String[]{"5 minutes", "15 minutes", "30 minutes", "1 hour"});
        timeRangeComboBox.setSelectedItem("30 minutes");
        timeRangeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateTimeRange();
            }
        });
        controlPanel.add(timeRangeComboBox);
        
        // Memory pool combo box
        memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        String[] poolNames = new String[memoryPoolMXBeans.size() + 1];
        poolNames[0] = "None";
        for (int i = 0; i < memoryPoolMXBeans.size(); i++) {
            poolNames[i + 1] = memoryPoolMXBeans.get(i).getName();
        }
        
        controlPanel.add(new JLabel("Memory Pool:"));
        poolComboBox = new ComboBox<>(poolNames);
        poolComboBox.setSelectedItem("None");
        poolComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateSelectedPool();
            }
        });
        controlPanel.add(poolComboBox);
        
        // Show heap and non-heap checkboxes
        showHeapCheckBox = new JCheckBox("Show Heap", true);
        showHeapCheckBox.addItemListener(e -> 
                memoryTrendChart.setShowHeapMemory(showHeapCheckBox.isSelected()));
        controlPanel.add(showHeapCheckBox);
        
        showNonHeapCheckBox = new JCheckBox("Show Non-Heap", true);
        showNonHeapCheckBox.addItemListener(e -> 
                memoryTrendChart.setShowNonHeapMemory(showNonHeapCheckBox.isSelected()));
        controlPanel.add(showNonHeapCheckBox);
        
        showPoolsCheckBox = new JCheckBox("Show Pools", false);
        showPoolsCheckBox.addItemListener(e -> {
            boolean selected = showPoolsCheckBox.isSelected();
            memoryTrendChart.setShowMemoryPools(selected);
            poolComboBox.setEnabled(selected);
        });
        controlPanel.add(showPoolsCheckBox);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        optimizeButton = new JButton("Optimize Memory");
        optimizeButton.addActionListener(e -> optimizeMemory());
        buttonPanel.add(optimizeButton);
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> updateMemoryData());
        buttonPanel.add(refreshButton);
        
        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(memoryTrendChart, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Memory Statistics"));
        rightPanel.add(statsPanel, BorderLayout.NORTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        add(new JBScrollPane(mainPanel), BorderLayout.CENTER);
        
        // Initial update
        updateMemoryData();
        
        // Start periodic updates
        scheduleUpdate();
        
        // Start the memory health monitor if it's not already running
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            MemoryHealthMonitor healthMonitor = MemoryHealthMonitor.getInstance();
            if (healthMonitor != null) {
                healthMonitor.start();
            }
        });
    }
    
    /**
     * Update the time range for the chart
     */
    private void updateTimeRange() {
        String selectedRange = (String) timeRangeComboBox.getSelectedItem();
        int minutes = 30; // Default
        
        if (selectedRange != null) {
            if (selectedRange.startsWith("5")) {
                minutes = 5;
            } else if (selectedRange.startsWith("15")) {
                minutes = 15;
            } else if (selectedRange.startsWith("30")) {
                minutes = 30;
            } else if (selectedRange.startsWith("1")) {
                minutes = 60;
            }
        }
        
        memoryTrendChart.setMinutesToShow(minutes);
    }
    
    /**
     * Update the selected memory pool
     */
    private void updateSelectedPool() {
        String selectedPool = (String) poolComboBox.getSelectedItem();
        
        if ("None".equals(selectedPool)) {
            memoryTrendChart.setSelectedPoolName(null);
        } else {
            memoryTrendChart.setSelectedPoolName(selectedPool);
        }
    }
    
    /**
     * Update memory data
     */
    private void updateMemoryData() {
        // Update memory statistics
        MemorySnapshot snapshot = takeMemorySnapshot();
        updateMemoryStatistics(snapshot);
        
        // Update memory trend chart
        MemoryHealthMonitor healthMonitor = MemoryHealthMonitor.getInstance();
        if (healthMonitor != null) {
            int minutes = 30;
            String selectedRange = (String) timeRangeComboBox.getSelectedItem();
            
            if (selectedRange != null) {
                if (selectedRange.startsWith("5")) {
                    minutes = 5;
                } else if (selectedRange.startsWith("15")) {
                    minutes = 15;
                } else if (selectedRange.startsWith("30")) {
                    minutes = 30;
                } else if (selectedRange.startsWith("1")) {
                    minutes = 60;
                }
            }
            
            List<MemorySnapshot> history = healthMonitor.getMemoryHistory(minutes);
            memoryTrendChart.updateData(history);
        }
    }
    
    /**
     * Take a memory snapshot
     * 
     * @return The memory snapshot
     */
    private MemorySnapshot takeMemorySnapshot() {
        return new MemorySnapshot(
                System.currentTimeMillis(),
                MemoryUtils.getTotalMemory(),
                MemoryUtils.getFreeMemory(),
                MemoryUtils.getMaxMemory(),
                MemoryUtils.getUsedMemory(),
                MemoryUtils.getMemoryUsagePercentage(),
                ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(),
                ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()
        );
    }
    
    /**
     * Update memory statistics
     * 
     * @param snapshot The memory snapshot
     */
    private void updateMemoryStatistics(MemorySnapshot snapshot) {
        totalMemoryLabel.setText(snapshot.getFormattedTotalMemory());
        usedMemoryLabel.setText(snapshot.getFormattedUsedMemory());
        freeMemoryLabel.setText(snapshot.getFormattedFreeMemory());
        maxMemoryLabel.setText(snapshot.getFormattedMaxMemory());
        heapMemoryLabel.setText(snapshot.getFormattedHeapUsed());
        nonHeapMemoryLabel.setText(snapshot.getFormattedNonHeapUsed());
        usagePercentageLabel.setText(snapshot.getFormattedUsagePercentage());
        
        // Update pressure level
        MemoryUtils.MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();
        memoryPressureLabel.setText(pressureLevel.toString());
        
        // Set pressure level color
        switch (pressureLevel) {
            case EMERGENCY:
                memoryPressureLabel.setForeground(EMERGENCY_COLOR);
                break;
            case CRITICAL:
                memoryPressureLabel.setForeground(CRITICAL_COLOR);
                break;
            case WARNING:
                memoryPressureLabel.setForeground(WARNING_COLOR);
                break;
            default:
                memoryPressureLabel.setForeground(NORMAL_COLOR);
                break;
        }
        
        // Update progress bar
        int usagePercent = (int) snapshot.getUsagePercentage();
        memoryUsageBar.setValue(usagePercent);
        
        // Set progress bar color
        if (usagePercent >= 90) {
            memoryUsageBar.setForeground(EMERGENCY_COLOR);
        } else if (usagePercent >= 80) {
            memoryUsageBar.setForeground(CRITICAL_COLOR);
        } else if (usagePercent >= 70) {
            memoryUsageBar.setForeground(WARNING_COLOR);
        } else {
            memoryUsageBar.setForeground(NORMAL_COLOR);
        }
    }
    
    /**
     * Optimize memory
     */
    private void optimizeMemory() {
        MemoryRecoveryManager recoveryManager = MemoryRecoveryManager.getInstance();
        if (recoveryManager != null) {
            optimizeButton.setEnabled(false);
            optimizeButton.setText("Optimizing...");
            
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // Get appropriate level based on current pressure
                    MemoryRecoveryManager.RecoveryPriority priority;
                    MemoryUtils.MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();
                    
                    switch (pressureLevel) {
                        case EMERGENCY:
                            priority = MemoryRecoveryManager.RecoveryPriority.CRITICAL;
                            break;
                        case CRITICAL:
                            priority = MemoryRecoveryManager.RecoveryPriority.HIGH;
                            break;
                        default:
                            priority = MemoryRecoveryManager.RecoveryPriority.MEDIUM;
                            break;
                    }
                    
                    recoveryManager.performRecovery(priority);
                    
                    // Allow some time for recovery to complete
                    Thread.sleep(1000);
                    
                    // Update data
                    SwingUtilities.invokeLater(this::updateMemoryData);
                } catch (Exception e) {
                    LOG.error("Error optimizing memory", e);
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        optimizeButton.setEnabled(true);
                        optimizeButton.setText("Optimize Memory");
                    });
                }
            });
        }
    }
    
    /**
     * Schedule regular updates
     */
    private void scheduleUpdate() {
        updateAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                if (project.isDisposed() || !project.isOpen()) {
                    return;
                }
                
                SwingUtilities.invokeLater(() -> updateMemoryData());
                
                // Schedule the next update
                updateAlarm.addRequest(this, 10000); // Update every 10 seconds
            }
        }, 10000);
    }
    
    @Override
    public void dispose() {
        updateAlarm.cancelAllRequests();
    }
}