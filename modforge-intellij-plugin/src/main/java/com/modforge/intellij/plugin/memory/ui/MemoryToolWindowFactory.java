package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory for creating the memory monitoring tool window
 */
public class MemoryToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(MemoryToolWindowFactory.class);
    private Timer updateTimer;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create tabs for different memory information
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Overview", createOverviewPanel(project));
        tabbedPane.addTab("Memory Pools", createMemoryPoolsPanel());
        tabbedPane.addTab("Optimization", createOptimizationPanel(project));
        tabbedPane.addTab("History", createHistoryPanel());
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
        
        // Begin updating the memory information periodically
        startUpdating(project, panel);
        
        // Stop updating when the tool window is disposed
        toolWindow.getContentManager().addContentManagerListener(new MemoryToolWindowDisposer(this));
    }
    
    private JPanel createOverviewPanel(Project project) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Create memory usage overview
        JPanel overviewPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = JBUI.insets(5);
        
        // Memory usage gauge
        JProgressBar memoryUsageBar = new JProgressBar(0, 100);
        memoryUsageBar.setStringPainted(true);
        memoryUsageBar.setString("Calculating...");
        memoryUsageBar.putClientProperty("MemoryUsageBar", true);
        overviewPanel.add(memoryUsageBar, c);
        
        // Memory stats
        c.gridwidth = 1;
        c.gridy++;
        overviewPanel.add(new JLabel("Total Memory:"), c);
        c.gridx = 1;
        JLabel totalMemoryLabel = new JLabel("Calculating...");
        totalMemoryLabel.putClientProperty("TotalMemoryLabel", true);
        overviewPanel.add(totalMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        overviewPanel.add(new JLabel("Used Memory:"), c);
        c.gridx = 1;
        JLabel usedMemoryLabel = new JLabel("Calculating...");
        usedMemoryLabel.putClientProperty("UsedMemoryLabel", true);
        overviewPanel.add(usedMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        overviewPanel.add(new JLabel("Free Memory:"), c);
        c.gridx = 1;
        JLabel freeMemoryLabel = new JLabel("Calculating...");
        freeMemoryLabel.putClientProperty("FreeMemoryLabel", true);
        overviewPanel.add(freeMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        overviewPanel.add(new JLabel("Max Memory:"), c);
        c.gridx = 1;
        JLabel maxMemoryLabel = new JLabel("Calculating...");
        maxMemoryLabel.putClientProperty("MaxMemoryLabel", true);
        overviewPanel.add(maxMemoryLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        overviewPanel.add(new JLabel("Memory Pressure:"), c);
        c.gridx = 1;
        JLabel pressureLevelLabel = new JLabel("Calculating...");
        pressureLevelLabel.putClientProperty("PressureLevelLabel", true);
        overviewPanel.add(pressureLevelLabel, c);
        
        // Add a separator
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        overviewPanel.add(new JSeparator(), c);
        
        // Add optimization buttons
        c.gridy++;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton optimizeButton = new JButton("Optimize Memory");
        optimizeButton.addActionListener(e -> {
            MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.NORMAL);
            updateMemoryOverview(panel);
        });
        buttonPanel.add(optimizeButton);
        
        JButton gcButton = new JButton("Request GC");
        gcButton.addActionListener(e -> {
            MemoryUtils.requestGarbageCollection();
            updateMemoryOverview(panel);
        });
        buttonPanel.add(gcButton);
        
        overviewPanel.add(buttonPanel, c);
        
        panel.add(overviewPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JPanel createMemoryPoolsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel poolsPanel = new JPanel();
        poolsPanel.setLayout(new BoxLayout(poolsPanel, BoxLayout.Y_AXIS));
        
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            String poolName = pool.getName();
            String poolType = pool.getType().toString();
            
            JPanel poolPanel = new JPanel(new BorderLayout());
            poolPanel.setBorder(BorderFactory.createTitledBorder(poolName + " (" + poolType + ")"));
            
            JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
            infoPanel.setBorder(JBUI.Borders.empty(5));
            
            infoPanel.add(new JLabel("Used:"));
            JLabel usedLabel = new JLabel("Calculating...");
            usedLabel.putClientProperty("PoolUsed:" + poolName, true);
            infoPanel.add(usedLabel);
            
            infoPanel.add(new JLabel("Committed:"));
            JLabel committedLabel = new JLabel("Calculating...");
            committedLabel.putClientProperty("PoolCommitted:" + poolName, true);
            infoPanel.add(committedLabel);
            
            infoPanel.add(new JLabel("Max:"));
            JLabel maxLabel = new JLabel("Calculating...");
            maxLabel.putClientProperty("PoolMax:" + poolName, true);
            infoPanel.add(maxLabel);
            
            poolPanel.add(infoPanel, BorderLayout.CENTER);
            
            JProgressBar poolUsageBar = new JProgressBar(0, 100);
            poolUsageBar.setStringPainted(true);
            poolUsageBar.setString("Calculating...");
            poolUsageBar.putClientProperty("PoolUsageBar:" + poolName, true);
            poolPanel.add(poolUsageBar, BorderLayout.SOUTH);
            
            poolsPanel.add(poolPanel);
            poolsPanel.add(Box.createVerticalStrut(10));
        }
        
        panel.add(new JBScrollPane(poolsPanel), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createOptimizationPanel(Project project) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(new JLabel("<html><h2>Memory Optimization</h2></html>"));
        innerPanel.add(titlePanel);
        
        // Add description
        JPanel descPanel = new JPanel(new BorderLayout());
        JTextArea descArea = new JTextArea(
                "Memory optimization helps reduce memory usage and improve performance. " +
                "Different optimization levels have different impacts on performance and functionality.\n\n" +
                "Choose the appropriate level based on your needs:"
        );
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(panel.getBackground());
        descPanel.add(descArea, BorderLayout.CENTER);
        innerPanel.add(descPanel);
        innerPanel.add(Box.createVerticalStrut(10));
        
        // Add optimization buttons
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 10));
        
        // Minimal optimization
        JPanel minimalPanel = new JPanel(new BorderLayout());
        JButton minimalButton = new JButton("Minimal Optimization");
        minimalButton.addActionListener(e -> 
            MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.MINIMAL));
        minimalPanel.add(minimalButton, BorderLayout.WEST);
        JLabel minimalLabel = new JLabel(" - Basic cleanup with minimal impact on performance");
        minimalPanel.add(minimalLabel, BorderLayout.CENTER);
        buttonPanel.add(minimalPanel);
        
        // Conservative optimization
        JPanel conservativePanel = new JPanel(new BorderLayout());
        JButton conservativeButton = new JButton("Conservative Optimization");
        conservativeButton.addActionListener(e -> 
            MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.CONSERVATIVE));
        conservativePanel.add(conservativeButton, BorderLayout.WEST);
        JLabel conservativeLabel = new JLabel(" - Standard cleanup with low impact on performance");
        conservativePanel.add(conservativeLabel, BorderLayout.CENTER);
        buttonPanel.add(conservativePanel);
        
        // Normal optimization
        JPanel normalPanel = new JPanel(new BorderLayout());
        JButton normalButton = new JButton("Normal Optimization");
        normalButton.addActionListener(e -> 
            MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.NORMAL));
        normalPanel.add(normalButton, BorderLayout.WEST);
        JLabel normalLabel = new JLabel(" - Balanced cleanup with moderate impact on performance");
        normalPanel.add(normalLabel, BorderLayout.CENTER);
        buttonPanel.add(normalPanel);
        
        // Aggressive optimization
        JPanel aggressivePanel = new JPanel(new BorderLayout());
        JButton aggressiveButton = new JButton("Aggressive Optimization");
        aggressiveButton.addActionListener(e -> 
            MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.AGGRESSIVE));
        aggressivePanel.add(aggressiveButton, BorderLayout.WEST);
        JLabel aggressiveLabel = new JLabel(" - Deep cleanup with higher impact on performance");
        aggressivePanel.add(aggressiveLabel, BorderLayout.CENTER);
        buttonPanel.add(aggressivePanel);
        
        innerPanel.add(buttonPanel);
        innerPanel.add(Box.createVerticalStrut(20));
        
        // Add garbage collection button
        JPanel gcPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton gcButton = new JButton("Request Garbage Collection");
        gcButton.addActionListener(e -> MemoryUtils.requestGarbageCollection());
        gcPanel.add(gcButton);
        innerPanel.add(gcPanel);
        
        // Add optimization status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Optimization Status: "));
        JLabel statusLabel = new JLabel("Idle");
        statusLabel.putClientProperty("OptimizationStatusLabel", true);
        statusPanel.add(statusLabel);
        innerPanel.add(statusPanel);
        
        panel.add(new JBScrollPane(innerPanel), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JPanel innerPanel = new JPanel(new BorderLayout());
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(new JLabel("<html><h2>Memory History</h2></html>"));
        innerPanel.add(titlePanel, BorderLayout.NORTH);
        
        // This will be filled in later when we implement memory history tracking
        JTextArea historyArea = new JTextArea(
                "Memory history tracking will be implemented in a future update.\n\n" +
                "This feature will track memory usage over time and display trends."
        );
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        historyArea.setBackground(panel.getBackground());
        
        innerPanel.add(new JBScrollPane(historyArea), BorderLayout.CENTER);
        
        panel.add(innerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void startUpdating(Project project, JPanel panel) {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        
        updateTimer = new Timer("MemoryMonitorUpdateTimer", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isDisposed.get()) {
                    cancel();
                    return;
                }
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed.get()) {
                        updateMemoryOverview(panel);
                        updateMemoryPools(panel);
                        updateOptimizationStatus(panel, project);
                    }
                });
            }
        }, 0, 1000);
    }
    
    private void updateMemoryOverview(JPanel panel) {
        // Update memory usage bar
        JProgressBar memoryUsageBar = findComponent(panel, "MemoryUsageBar");
        if (memoryUsageBar != null) {
            double usagePercentage = MemoryUtils.getMemoryUsagePercentage();
            memoryUsageBar.setValue((int) usagePercentage);
            memoryUsageBar.setString(String.format("%.1f%%", usagePercentage));
            
            // Set color based on memory pressure level
            switch (MemoryUtils.getMemoryPressureLevel()) {
                case EMERGENCY:
                    memoryUsageBar.setForeground(new Color(232, 17, 35));
                    break;
                case CRITICAL:
                    memoryUsageBar.setForeground(new Color(255, 102, 0));
                    break;
                case WARNING:
                    memoryUsageBar.setForeground(new Color(255, 170, 0));
                    break;
                default:
                    memoryUsageBar.setForeground(new Color(45, 183, 93));
                    break;
            }
        }
        
        // Update memory labels
        JLabel totalMemoryLabel = findComponent(panel, "TotalMemoryLabel");
        if (totalMemoryLabel != null) {
            totalMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getTotalMemory()));
        }
        
        JLabel usedMemoryLabel = findComponent(panel, "UsedMemoryLabel");
        if (usedMemoryLabel != null) {
            usedMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getUsedMemory()) + 
                                   " (" + String.format("%.1f%%", MemoryUtils.getMemoryUsagePercentage()) + ")");
        }
        
        JLabel freeMemoryLabel = findComponent(panel, "FreeMemoryLabel");
        if (freeMemoryLabel != null) {
            freeMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getFreeMemory()));
        }
        
        JLabel maxMemoryLabel = findComponent(panel, "MaxMemoryLabel");
        if (maxMemoryLabel != null) {
            maxMemoryLabel.setText(MemoryUtils.formatMemorySize(MemoryUtils.getMaxMemory()));
        }
        
        JLabel pressureLevelLabel = findComponent(panel, "PressureLevelLabel");
        if (pressureLevelLabel != null) {
            pressureLevelLabel.setText(MemoryUtils.getMemoryPressureLevel().toString());
            
            // Set color based on memory pressure level
            switch (MemoryUtils.getMemoryPressureLevel()) {
                case EMERGENCY:
                    pressureLevelLabel.setForeground(new Color(232, 17, 35));
                    break;
                case CRITICAL:
                    pressureLevelLabel.setForeground(new Color(255, 102, 0));
                    break;
                case WARNING:
                    pressureLevelLabel.setForeground(new Color(255, 170, 0));
                    break;
                default:
                    pressureLevelLabel.setForeground(new Color(45, 183, 93));
                    break;
            }
        }
    }
    
    private void updateMemoryPools(JPanel panel) {
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            String poolName = pool.getName();
            MemoryUsage usage = pool.getUsage();
            
            JLabel usedLabel = findComponent(panel, "PoolUsed:" + poolName);
            if (usedLabel != null) {
                usedLabel.setText(MemoryUtils.formatMemorySize(usage.getUsed()));
            }
            
            JLabel committedLabel = findComponent(panel, "PoolCommitted:" + poolName);
            if (committedLabel != null) {
                committedLabel.setText(MemoryUtils.formatMemorySize(usage.getCommitted()));
            }
            
            JLabel maxLabel = findComponent(panel, "PoolMax:" + poolName);
            if (maxLabel != null) {
                maxLabel.setText(usage.getMax() < 0 ? "undefined" : MemoryUtils.formatMemorySize(usage.getMax()));
            }
            
            JProgressBar usageBar = findComponent(panel, "PoolUsageBar:" + poolName);
            if (usageBar != null) {
                if (usage.getMax() > 0) {
                    double usagePercentage = (usage.getUsed() * 100.0) / usage.getMax();
                    usageBar.setValue((int) usagePercentage);
                    usageBar.setString(String.format("%.1f%%", usagePercentage));
                    
                    // Set color based on usage percentage
                    if (usagePercentage >= 90) {
                        usageBar.setForeground(new Color(232, 17, 35));
                    } else if (usagePercentage >= 70) {
                        usageBar.setForeground(new Color(255, 102, 0));
                    } else if (usagePercentage >= 50) {
                        usageBar.setForeground(new Color(255, 170, 0));
                    } else {
                        usageBar.setForeground(new Color(45, 183, 93));
                    }
                } else {
                    usageBar.setValue(0);
                    usageBar.setString("N/A");
                }
            }
        }
    }
    
    private void updateOptimizationStatus(JPanel panel, Project project) {
        JLabel statusLabel = findComponent(panel, "OptimizationStatusLabel");
        if (statusLabel != null) {
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null && memoryManager.isOptimizing()) {
                statusLabel.setText("Optimizing...");
                statusLabel.setForeground(new Color(0, 120, 215));
            } else {
                statusLabel.setText("Idle");
                statusLabel.setForeground(UIManager.getColor("Label.foreground"));
            }
        }
    }
    
    /**
     * Find a component with the given client property
     * 
     * @param container The container to search in
     * @param clientPropertyKey The client property key to look for
     * @return The component with the given client property, or null if not found
     */
    @SuppressWarnings("unchecked")
    private <T extends JComponent> T findComponent(Container container, String clientPropertyKey) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent) {
                JComponent jComponent = (JComponent) component;
                Object property = jComponent.getClientProperty(clientPropertyKey);
                if (property != null && (Boolean) property) {
                    return (T) jComponent;
                }
            }
            
            if (component instanceof Container) {
                T result = findComponent((Container) component, clientPropertyKey);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    void disposeComponent() {
        isDisposed.set(true);
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
    
    /**
     * Disposer for the memory tool window
     */
    private static class MemoryToolWindowDisposer extends com.intellij.ui.content.ContentManagerAdapter {
        private final MemoryToolWindowFactory factory;
        
        MemoryToolWindowDisposer(MemoryToolWindowFactory factory) {
            this.factory = factory;
        }
        
        @Override
        public void contentRemoved(@NotNull com.intellij.ui.content.ContentManagerEvent event) {
            factory.disposeComponent();
        }
    }
}