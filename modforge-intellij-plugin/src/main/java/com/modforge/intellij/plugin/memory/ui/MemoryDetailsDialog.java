package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * Dialog for displaying detailed memory information
 */
public class MemoryDetailsDialog extends DialogWrapper {
    private final Project project;
    private final JPanel mainPanel;
    private JProgressBar memoryUsageProgressBar;
    private JLabel totalMemoryLabel;
    private JLabel usedMemoryLabel;
    private JLabel freeMemoryLabel;
    private JLabel maxMemoryLabel;
    private JLabel availableMemoryLabel;
    private JLabel memoryPressureLevelLabel;
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryDetailsDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("Memory Details");
        
        // Create the main panel with tabs
        mainPanel = new JPanel(new BorderLayout());
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("Overview", createOverviewPanel());
        tabbedPane.addTab("Memory Pools", createMemoryPoolsPanel());
        tabbedPane.addTab("Optimization", createOptimizationPanel());
        tabbedPane.addTab("Memory Trend", createMemoryTrendPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        init();
        
        // Start a timer to update the dialog every 2 seconds
        Timer updateTimer = new Timer(2000, e -> updateMemoryInfo());
        updateTimer.setInitialDelay(0);
        updateTimer.start();
        
        // Stop the timer when the dialog is closed
        getDisposable().register(() -> updateTimer.stop());
    }
    
    /**
     * Create the overview panel
     * 
     * @return The overview panel
     */
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = JBUI.insets(5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        
        // Memory usage progress bar
        memoryUsageProgressBar = new JProgressBar(0, 100);
        memoryUsageProgressBar.setStringPainted(true);
        panel.add(memoryUsageProgressBar, c);
        
        // Memory info labels
        c.gridwidth = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Total Memory:"), c);
        
        c.gridx = 1;
        totalMemoryLabel = new JLabel();
        panel.add(totalMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Used Memory:"), c);
        
        c.gridx = 1;
        usedMemoryLabel = new JLabel();
        panel.add(usedMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Free Memory:"), c);
        
        c.gridx = 1;
        freeMemoryLabel = new JLabel();
        panel.add(freeMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Max Memory:"), c);
        
        c.gridx = 1;
        maxMemoryLabel = new JLabel();
        panel.add(maxMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Available Memory:"), c);
        
        c.gridx = 1;
        availableMemoryLabel = new JLabel();
        panel.add(availableMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Memory Pressure Level:"), c);
        
        c.gridx = 1;
        memoryPressureLevelLabel = new JLabel();
        panel.add(memoryPressureLevelLabel, c);
        
        // Update the memory info
        updateMemoryInfo();
        
        return panel;
    }
    
    /**
     * Create the memory pools panel
     * 
     * @return The memory pools panel
     */
    private JPanel createMemoryPoolsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        innerPanel.setBorder(JBUI.Borders.empty(10));
        
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            JPanel poolPanel = new JPanel(new BorderLayout());
            poolPanel.setBorder(BorderFactory.createTitledBorder(pool.getName() + " (" + pool.getType() + ")"));
            
            JPanel infoPanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = JBUI.insets(2);
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            
            MemoryUsage usage = pool.getUsage();
            
            infoPanel.add(new JLabel("Used:"), c);
            c.gridx = 1;
            infoPanel.add(new JLabel(MemoryUtils.formatMemorySize(usage.getUsed())), c);
            
            c.gridx = 0;
            c.gridy++;
            infoPanel.add(new JLabel("Committed:"), c);
            c.gridx = 1;
            infoPanel.add(new JLabel(MemoryUtils.formatMemorySize(usage.getCommitted())), c);
            
            c.gridx = 0;
            c.gridy++;
            infoPanel.add(new JLabel("Max:"), c);
            c.gridx = 1;
            infoPanel.add(new JLabel(usage.getMax() < 0 ? "undefined" : MemoryUtils.formatMemorySize(usage.getMax())), c);
            
            poolPanel.add(infoPanel, BorderLayout.CENTER);
            
            // Add a progress bar if max is defined
            if (usage.getMax() > 0) {
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setValue((int) (usage.getUsed() * 100 / usage.getMax()));
                progressBar.setStringPainted(true);
                progressBar.setString(String.format("%.1f%%", (usage.getUsed() * 100.0 / usage.getMax())));
                poolPanel.add(progressBar, BorderLayout.SOUTH);
            }
            
            innerPanel.add(poolPanel);
        }
        
        panel.add(new JBScrollPane(innerPanel), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the optimization panel
     * 
     * @return The optimization panel
     */
    private JPanel createOptimizationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel innerPanel = new JPanel(new GridBagLayout());
        innerPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.insets = JBUI.insets(5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        
        innerPanel.add(new JLabel("<html><b>Memory Optimization Actions</b></html>"), c);
        
        c.gridy++;
        c.gridwidth = 1;
        
        // Add buttons for different optimization levels
        JButton minimalButton = new JButton("Minimal Optimization");
        minimalButton.addActionListener(e -> performOptimization(MemoryOptimizer.OptimizationLevel.MINIMAL));
        innerPanel.add(minimalButton, c);
        
        c.gridx = 1;
        innerPanel.add(new JLabel("Basic cleanup with minimal impact"), c);
        
        c.gridx = 0;
        c.gridy++;
        JButton conservativeButton = new JButton("Conservative Optimization");
        conservativeButton.addActionListener(e -> performOptimization(MemoryOptimizer.OptimizationLevel.CONSERVATIVE));
        innerPanel.add(conservativeButton, c);
        
        c.gridx = 1;
        innerPanel.add(new JLabel("Standard cleanup with low impact"), c);
        
        c.gridx = 0;
        c.gridy++;
        JButton normalButton = new JButton("Normal Optimization");
        normalButton.addActionListener(e -> performOptimization(MemoryOptimizer.OptimizationLevel.NORMAL));
        innerPanel.add(normalButton, c);
        
        c.gridx = 1;
        innerPanel.add(new JLabel("Balanced cleanup with moderate impact"), c);
        
        c.gridx = 0;
        c.gridy++;
        JButton aggressiveButton = new JButton("Aggressive Optimization");
        aggressiveButton.addActionListener(e -> performOptimization(MemoryOptimizer.OptimizationLevel.AGGRESSIVE));
        innerPanel.add(aggressiveButton, c);
        
        c.gridx = 1;
        innerPanel.add(new JLabel("Deep cleanup with higher impact"), c);
        
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        innerPanel.add(new JSeparator(), c);
        
        c.gridy++;
        JButton gcButton = new JButton("Request Garbage Collection");
        gcButton.addActionListener(e -> {
            MemoryUtils.requestGarbageCollection();
            updateMemoryInfo();
        });
        innerPanel.add(gcButton, c);
        
        panel.add(new JBScrollPane(innerPanel), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the memory trend panel
     * 
     * @return The memory trend panel
     */
    private JPanel createMemoryTrendPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // TODO: Implement memory trend graph
        
        return panel;
    }
    
    /**
     * Update the memory information displayed in the dialog
     */
    private void updateMemoryInfo() {
        double usagePercentage = MemoryUtils.getMemoryUsagePercentage();
        
        memoryUsageProgressBar.setValue((int) usagePercentage);
        memoryUsageProgressBar.setString(String.format("%.1f%%", usagePercentage));
        
        // Set color based on memory pressure level
        switch (MemoryUtils.getMemoryPressureLevel()) {
            case EMERGENCY:
                memoryUsageProgressBar.setForeground(new Color(232, 17, 35));
                break;
            case CRITICAL:
                memoryUsageProgressBar.setForeground(new Color(255, 102, 0));
                break;
            case WARNING:
                memoryUsageProgressBar.setForeground(new Color(255, 170, 0));
                break;
            default:
                memoryUsageProgressBar.setForeground(new Color(45, 183, 93));
                break;
        }
        
        totalMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getTotalMemory()));
        usedMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getUsedMemory()) + 
                                " (" + String.format("%.1f%%", usagePercentage) + ")");
        freeMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getFreeMemory()));
        maxMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getMaxMemory()));
        availableMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getAvailableMemory()));
        memoryPressureLevelLabel.setText(MemoryUtils.getMemoryPressureLevel().toString());
    }
    
    /**
     * Perform a memory optimization
     * 
     * @param level The optimization level
     */
    private void performOptimization(MemoryOptimizer.OptimizationLevel level) {
        if (project != null) {
            MemoryUtils.optimizeMemory(project, level);
            updateMemoryInfo();
        }
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        return mainPanel;
    }
    
    /**
     * Show the memory details dialog
     * 
     * @param project The project
     */
    public static void show(Project project) {
        new MemoryDetailsDialog(project).show();
    }
}