package com.modforge.intellij.plugin.debug.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.debug.MinecraftPerformanceMonitor;
import com.modforge.intellij.plugin.debug.MinecraftPerformanceMonitor.MethodPerformanceData;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.Timer; // Explicitly import the Swing Timer to avoid ambiguity with java.util.Timer
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tool window for displaying Minecraft performance data
 * Shows method execution times, memory usage, and performance bottlenecks
 */
public class MinecraftPerformanceToolWindow implements ToolWindowFactory, Disposable {
    private static final Logger LOG = Logger.getInstance(MinecraftPerformanceToolWindow.class);
    
    private Project project;
    private JBTable performanceTable;
    private JBTable memoryTable;
    private DefaultTableModel performanceModel;
    private DefaultTableModel memoryModel;
    private Timer refreshTimer;
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        
        // Create the UI components
        JComponent content = createContent();
        
        // Add the content to the tool window
        Content toolContent = ContentFactory.SERVICE.getInstance().createContent(content, "", false);
        toolWindow.getContentManager().addContent(toolContent);
        
        // Start periodic updates
        startPeriodicUpdates();
    }
    
    /**
     * Create the tool window content
     * 
     * @return The UI component for the tool window
     */
    private JComponent createContent() {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        
        // Create a tabbed pane for different performance views
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Add execution time tab
        tabbedPane.addTab("Method Execution Times", createExecutionTimePanel());
        
        // Add memory usage tab
        tabbedPane.addTab("Memory Usage", createMemoryUsagePanel());
        
        // Add recommendations tab
        tabbedPane.addTab("Performance Recommendations", createRecommendationsPanel());
        
        // Add controls panel at the bottom
        JPanel controlsPanel = createControlsPanel();
        
        // Create main layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        panel.setContent(mainPanel);
        return panel;
    }
    
    /**
     * Create the execution time panel
     * 
     * @return The execution time panel
     */
    private JPanel createExecutionTimePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create the table model with columns
        performanceModel = new DefaultTableModel(
                new Object[]{"Method", "Calls", "Avg Time (ms)", "Max Time (ms)", "Min Time (ms)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Create the table
        performanceTable = new JBTable(performanceModel);
        performanceTable.setShowGrid(false);
        performanceTable.setStriped(true);
        
        // Add custom rendering for time values
        performanceTable.getColumnModel().getColumn(2).setCellRenderer(new TimeRenderer());
        performanceTable.getColumnModel().getColumn(3).setCellRenderer(new TimeRenderer());
        performanceTable.getColumnModel().getColumn(4).setCellRenderer(new TimeRenderer());
        
        // Add to a scroll pane
        JBScrollPane scrollPane = new JBScrollPane(performanceTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the memory usage panel
     * 
     * @return The memory usage panel
     */
    private JPanel createMemoryUsagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create the table model with columns
        memoryModel = new DefaultTableModel(
                new Object[]{"Method", "Memory (MB)", "% of Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Create the table
        memoryTable = new JBTable(memoryModel);
        memoryTable.setShowGrid(false);
        memoryTable.setStriped(true);
        
        // Add custom rendering for memory values
        memoryTable.getColumnModel().getColumn(1).setCellRenderer(new MemoryRenderer());
        memoryTable.getColumnModel().getColumn(2).setCellRenderer(new PercentageRenderer());
        
        // Add to a scroll pane
        JBScrollPane scrollPane = new JBScrollPane(memoryTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the performance recommendations panel
     * 
     * @return The recommendations panel
     */
    private JPanel createRecommendationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create text area for recommendations
        JTextArea recommendationsArea = new JTextArea();
        recommendationsArea.setEditable(false);
        recommendationsArea.setFont(UIUtil.getLabelFont());
        recommendationsArea.setBorder(JBUI.Borders.empty(10));
        recommendationsArea.setText(
                "Performance Recommendations:\n\n" +
                "1. Optimize method execution times highlighted in red\n" +
                "2. Reduce memory allocations for methods using more than 10MB\n" +
                "3. Consider using object pooling for frequently created objects\n" +
                "4. Use efficient data structures for large collections\n" +
                "5. Minimize object creation during game loop\n" +
                "6. Use caching for expensive calculations\n" +
                "7. Avoid synchronization in critical paths\n" +
                "8. Batch similar operations where possible\n" +
                "9. Use primitive types instead of objects when possible\n" +
                "10. Profile different rendering and update strategies\n\n" +
                "Recommendations will update based on actual performance data."
        );
        
        JBScrollPane scrollPane = new JBScrollPane(recommendationsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the controls panel
     * 
     * @return The controls panel
     */
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(JBUI.Borders.empty(5));
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(this::refreshData);
        panel.add(refreshButton);
        
        // Add export button
        JButton exportButton = new JButton("Export Report");
        exportButton.addActionListener(this::exportReport);
        panel.add(exportButton);
        
        // Add reset button
        JButton resetButton = new JButton("Reset Statistics");
        resetButton.addActionListener(this::resetStatistics);
        panel.add(resetButton);
        
        return panel;
    }
    
    /**
     * Start periodic updates of the performance data
     */
    private void startPeriodicUpdates() {
        // Create a timer to refresh data every 5 seconds
        refreshTimer = new Timer(5000, this::refreshData);
        refreshTimer.start();
        LOG.info("Started periodic performance data updates");
    }
    
    /**
     * Refresh the performance data display
     * 
     * @param e The action event (if triggered by a button)
     */
    private void refreshData(ActionEvent e) {
        refreshData();
    }
    
    /**
     * Refresh the performance data display
     */
    private void refreshData() {
        // Get the performance monitor
        MinecraftPerformanceMonitor monitor = project.getService(MinecraftPerformanceMonitor.class);
        if (monitor == null) {
            LOG.warn("Performance monitor service not available");
            return;
        }
        
        try {
            // Update method execution time table
            updateExecutionTimeTable(monitor);
            
            // Update memory usage table
            updateMemoryUsageTable(monitor);
            
            LOG.debug("Performance data refreshed");
        } catch (Exception ex) {
            LOG.error("Error refreshing performance data", ex);
        }
    }
    
    /**
     * Update the execution time table with current data
     * 
     * @param monitor The performance monitor
     */
    private void updateExecutionTimeTable(MinecraftPerformanceMonitor monitor) {
        // Clear the table
        performanceModel.setRowCount(0);
        
        // Get the performance data
        Map<String, MethodPerformanceData> performanceData = monitor.getPerformanceData();
        
        // Add rows for each method
        for (Map.Entry<String, MethodPerformanceData> entry : performanceData.entrySet()) {
            MethodPerformanceData data = entry.getValue();
            
            performanceModel.addRow(new Object[]{
                    entry.getKey(),
                    data.getExecutionCount(),
                    TimeUnit.NANOSECONDS.toMillis(data.getAverageTimeNanos()),
                    TimeUnit.NANOSECONDS.toMillis(data.getMaxTimeNanos()),
                    TimeUnit.NANOSECONDS.toMillis(data.getMinTimeNanos())
            });
        }
        
        // Sort by average time (descending)
        performanceTable.getRowSorter().toggleSortOrder(2);
    }
    
    /**
     * Update the memory usage table with current data
     * 
     * @param monitor The performance monitor
     */
    private void updateMemoryUsageTable(MinecraftPerformanceMonitor monitor) {
        // Clear the table
        memoryModel.setRowCount(0);
        
        // Get the memory allocation data
        Map<String, Long> memoryData = monitor.getMemoryAllocationData();
        
        // Calculate total memory usage
        long totalMemory = memoryData.values().stream().mapToLong(Long::longValue).sum();
        
        // Add rows for each method
        for (Map.Entry<String, Long> entry : memoryData.entrySet()) {
            long memoryBytes = entry.getValue();
            double memoryMb = memoryBytes / (1024.0 * 1024.0);
            double percentage = totalMemory > 0 ? ((double) memoryBytes / totalMemory) * 100.0 : 0.0;
            
            memoryModel.addRow(new Object[]{
                    entry.getKey(),
                    memoryMb,
                    percentage
            });
        }
        
        // Sort by memory usage (descending)
        memoryTable.getRowSorter().toggleSortOrder(1);
    }
    
    /**
     * Export a performance report
     * 
     * @param e The action event
     */
    private void exportReport(ActionEvent e) {
        // Get the performance monitor
        MinecraftPerformanceMonitor monitor = project.getService(MinecraftPerformanceMonitor.class);
        if (monitor == null) {
            LOG.warn("Performance monitor service not available");
            return;
        }
        
        // Generate the report
        String report = monitor.generatePerformanceReport();
        
        // Show the report in a dialog
        JTextArea textArea = new JTextArea(report);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        
        JOptionPane.showMessageDialog(null, scrollPane, "Performance Report", 
                JOptionPane.INFORMATION_MESSAGE);
        
        // TODO: Add option to save to file
    }
    
    /**
     * Reset the performance statistics
     * 
     * @param e The action event
     */
    private void resetStatistics(ActionEvent e) {
        // Reset not directly implemented yet
        // We would need to modify the MinecraftPerformanceMonitor to support this
        
        JOptionPane.showMessageDialog(null, 
                "Statistics reset is not implemented yet. Restart debugging session to reset.", 
                "Reset Statistics", 
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
        LOG.info("Performance tool window disposed");
    }
    
    /**
     * Custom renderer for time values
     */
    private static class TimeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                                                      boolean isSelected, boolean hasFocus, 
                                                      int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof Number) {
                long time = ((Number) value).longValue();
                
                // Color code based on time
                if (time > 50) { // More than 50ms is bad for game performance
                    c.setForeground(Color.RED);
                } else if (time > 16) { // More than 16ms (less than 60 FPS) is concerning
                    c.setForeground(Color.ORANGE);
                } else {
                    c.setForeground(isSelected ? UIUtil.getTableSelectionForeground(true) : 
                                                UIUtil.getTableForeground());
                }
                
                // Format with milliseconds
                setText(time + " ms");
            }
            
            return c;
        }
    }
    
    /**
     * Custom renderer for memory values
     */
    private static class MemoryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                                                      boolean isSelected, boolean hasFocus, 
                                                      int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof Number) {
                double memory = ((Number) value).doubleValue();
                
                // Color code based on memory usage
                if (memory > 100) { // More than 100MB is concerning
                    c.setForeground(Color.RED);
                } else if (memory > 10) { // More than 10MB is notable
                    c.setForeground(Color.ORANGE);
                } else {
                    c.setForeground(isSelected ? UIUtil.getTableSelectionForeground(true) : 
                                                UIUtil.getTableForeground());
                }
                
                // Format with MB
                setText(String.format("%.2f MB", memory));
            }
            
            return c;
        }
    }
    
    /**
     * Custom renderer for percentage values
     */
    private static class PercentageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                                                      boolean isSelected, boolean hasFocus, 
                                                      int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof Number) {
                double percentage = ((Number) value).doubleValue();
                
                // Color code based on percentage
                if (percentage > 50) { // More than 50% is concerning
                    c.setForeground(Color.RED);
                } else if (percentage > 20) { // More than 20% is notable
                    c.setForeground(Color.ORANGE);
                } else {
                    c.setForeground(isSelected ? UIUtil.getTableSelectionForeground(true) : 
                                                UIUtil.getTableForeground());
                }
                
                // Format with percentage
                setText(String.format("%.1f%%", percentage));
            }
            
            return c;
        }
    }
}