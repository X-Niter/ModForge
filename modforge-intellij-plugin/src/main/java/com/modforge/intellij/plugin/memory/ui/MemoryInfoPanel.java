package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.memory.MemoryListener;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemorySnapshot;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Panel to display detailed memory information
 */
public class MemoryInfoPanel extends SimpleToolWindowPanel implements MemoryListener, Disposable {
    private static final Logger LOG = Logger.getInstance(MemoryInfoPanel.class);
    private static final DecimalFormat FORMAT = new DecimalFormat("#0.0");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    private final Project project;
    private ScheduledFuture<?> updateTask;
    
    // UI components
    private JPanel statsPanel;
    private JPanel chartPanel;
    private JPanel actionsPanel;
    
    private JLabel heapUsedLabel;
    private JLabel heapMaxLabel;
    private JLabel heapPercentLabel;
    private JLabel nonHeapUsedLabel;
    private JLabel gcCountLabel;
    private JProgressBar memoryBar;
    
    /**
     * Create a new memory info panel
     * 
     * @param project The current project
     */
    public MemoryInfoPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        
        setupUI();
        
        // Register as memory listener
        MemoryManager.getInstance().addListener(this);
        
        // Schedule periodic updates
        updateTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            this::updateStats, 1, 2, TimeUnit.SECONDS
        );
        
        // Initial update
        updateStats();
    }
    
    /**
     * Set up the UI components
     */
    private void setupUI() {
        // Stats panel
        statsPanel = new JBPanel<>(new GridLayout(3, 2, 10, 10));
        statsPanel.setBorder(JBUI.Borders.empty(10));
        
        heapUsedLabel = new JBLabel("Used Heap: N/A");
        heapMaxLabel = new JBLabel("Max Heap: N/A");
        heapPercentLabel = new JBLabel("Usage: N/A");
        nonHeapUsedLabel = new JBLabel("Non-Heap: N/A");
        gcCountLabel = new JBLabel("GC Count: N/A");
        
        statsPanel.add(heapUsedLabel);
        statsPanel.add(heapMaxLabel);
        statsPanel.add(heapPercentLabel);
        statsPanel.add(nonHeapUsedLabel);
        statsPanel.add(gcCountLabel);
        
        // Memory bar
        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setString("Memory Usage");
        
        // Chart panel (simple placeholder - in a real app, use a charting library)
        chartPanel = new JBPanel<>(new BorderLayout());
        chartPanel.setBorder(JBUI.Borders.empty(10));
        chartPanel.add(new JBLabel("Memory Usage Chart"), BorderLayout.NORTH);
        chartPanel.add(new MemoryChartPanel(), BorderLayout.CENTER);
        
        // Actions panel
        actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        actionsPanel.setBorder(JBUI.Borders.empty(10));
        
        JButton gcButton = new JButton("Run GC");
        gcButton.addActionListener(e -> {
            System.gc();
            updateStats();
        });
        
        actionsPanel.add(gcButton);
        
        // Main panel layout
        JPanel mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(statsPanel, BorderLayout.NORTH);
        mainPanel.add(memoryBar, BorderLayout.CENTER);
        mainPanel.add(chartPanel, BorderLayout.SOUTH);
        
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.add(actionsPanel, BorderLayout.NORTH);
        
        setContent(mainPanel);
        
        // Add some padding
        mainPanel.setBorder(JBUI.Borders.empty(10));
    }
    
    /**
     * Update memory statistics
     */
    private void updateStats() {
        try {
            MemorySnapshot snapshot = MemoryManager.getInstance().getCurrentSnapshot();
            
            // Update labels
            SwingUtilities.invokeLater(() -> {
                heapUsedLabel.setText(String.format("Used Heap: %.1f MB", snapshot.getUsedHeapMB()));
                heapMaxLabel.setText(String.format("Max Heap: %.1f MB", snapshot.getMaxHeapMB()));
                heapPercentLabel.setText(String.format("Usage: %.1f%%", snapshot.getUsagePercentage()));
                nonHeapUsedLabel.setText(String.format("Non-Heap: %.1f MB", snapshot.getUsedNonHeapMB()));
                
                // Update progress bar
                int percentage = (int) Math.round(snapshot.getUsagePercentage());
                memoryBar.setValue(percentage);
                memoryBar.setString(String.format("Memory Usage: %d%%", percentage));
                
                // Set color based on usage level
                if (percentage >= 85) {
                    memoryBar.setForeground(JBColor.RED);
                } else if (percentage >= 75) {
                    memoryBar.setForeground(JBColor.ORANGE);
                } else {
                    memoryBar.setForeground(JBColor.GREEN);
                }
                
                // Update chart
                chartPanel.repaint();
            });
        } catch (Exception e) {
            LOG.error("Error updating memory stats", e);
        }
    }
    
    @Override
    public void dispose() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        MemoryManager.getInstance().removeListener(this);
    }
    
    @Override
    public void onWarningMemoryPressure(MemorySnapshot snapshot) {
        updateStats();
    }
    
    @Override
    public void onCriticalMemoryPressure(MemorySnapshot snapshot) {
        updateStats();
    }
    
    @Override
    public void onEmergencyMemoryPressure(MemorySnapshot snapshot) {
        updateStats();
    }
    
    @Override
    public void onNormalMemory(MemorySnapshot snapshot) {
        updateStats();
    }
    
    /**
     * Simple panel for displaying a memory usage chart
     */
    private class MemoryChartPanel extends JBPanel<MemoryChartPanel> {
        public MemoryChartPanel() {
            setPreferredSize(new Dimension(500, 150));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Get memory history
            List<MemorySnapshot> history = MemoryManager.getInstance().getMemoryHistory();
            if (history.isEmpty()) {
                return;
            }
            
            // Calculate max values
            long maxHeap = 0;
            for (MemorySnapshot snapshot : history) {
                maxHeap = Math.max(maxHeap, snapshot.getMaxHeap());
            }
            
            if (maxHeap <= 0) {
                return;
            }
            
            // Setup graphics
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background
            g2d.setColor(UIUtil.getPanelBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Draw chart
            int width = getWidth();
            int height = getHeight();
            int usedHeight = height - 20; // Leave space for labels
            
            // Draw axis
            g2d.setColor(JBColor.GRAY);
            g2d.drawLine(30, 10, 30, height - 10);
            g2d.drawLine(30, height - 10, width - 10, height - 10);
            
            // Draw memory line
            if (history.size() > 1) {
                int xStep = (width - 50) / (history.size() - 1);
                int[] xPoints = new int[history.size()];
                int[] yPoints = new int[history.size()];
                
                for (int i = 0; i < history.size(); i++) {
                    MemorySnapshot snapshot = history.get(i);
                    xPoints[i] = 30 + i * xStep;
                    
                    // Calculate y position (inverted because graphics coordinate system is inverted)
                    double ratio = (double) snapshot.getUsedHeap() / maxHeap;
                    yPoints[i] = height - 10 - (int) (usedHeight * ratio);
                }
                
                // Draw line
                g2d.setColor(JBColor.BLUE);
                g2d.setStroke(new BasicStroke(2.0f));
                
                for (int i = 1; i < history.size(); i++) {
                    g2d.drawLine(xPoints[i-1], yPoints[i-1], xPoints[i], yPoints[i]);
                }
                
                // Draw points
                g2d.setColor(JBColor.RED);
                for (int i = 0; i < history.size(); i++) {
                    g2d.fillOval(xPoints[i] - 3, yPoints[i] - 3, 6, 6);
                }
                
                // Draw labels
                g2d.setColor(JBColor.DARK_GRAY);
                g2d.setFont(UIUtil.getFont(UIUtil.FontSize.SMALL, g2d.getFont()));
                
                // Y axis label
                g2d.drawString("MB", 5, 20);
                
                // X axis label (time)
                g2d.drawString("Time", width - 30, height - 5);
                
                // Draw max memory label
                g2d.drawString(FORMAT.format(maxHeap / (1024.0 * 1024.0)), 5, 15);
                
                // Draw latest time
                if (!history.isEmpty()) {
                    String timeStr = TIME_FORMAT.format(new Date(history.get(history.size() - 1).getTimestamp()));
                    g2d.drawString(timeStr, width - 60, height - 5);
                }
            }
        }
    }
}