package com.modforge.intellij.plugin.memory.monitoring.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Chart panel for visualizing memory usage trends
 * Displays memory usage over time with various metrics and coloring based on pressure level
 */
public class MemoryTrendChart extends JPanel {
    private static final Logger LOG = Logger.getInstance(MemoryTrendChart.class);
    
    private static final Color BACKGROUND_COLOR = JBColor.background();
    private static final Color GRID_COLOR = new JBColor(new Color(220, 220, 220), new Color(65, 65, 65));
    private static final Color AXIS_COLOR = new JBColor(new Color(120, 120, 120), new Color(140, 140, 140));
    private static final Color TEXT_COLOR = JBColor.foreground();
    
    private static final Color NORMAL_COLOR = new JBColor(new Color(45, 183, 93), new Color(45, 183, 93));
    private static final Color WARNING_COLOR = new JBColor(new Color(255, 170, 0), new Color(255, 170, 0));
    private static final Color CRITICAL_COLOR = new JBColor(new Color(255, 102, 0), new Color(255, 102, 0));
    private static final Color EMERGENCY_COLOR = new JBColor(new Color(232, 17, 35), new Color(232, 17, 35));
    
    private final Project project;
    private List<MemorySnapshot> snapshots = new ArrayList<>();
    private int chartWidth;
    private int chartHeight;
    private int chartMarginLeft = 60;
    private int chartMarginRight = 20;
    private int chartMarginTop = 30;
    private int chartMarginBottom = 50;
    private boolean showHeapMemory = true;
    private boolean showNonHeapMemory = true;
    private boolean showMemoryPools = false;
    private String selectedPoolName = null;
    private int minutesToShow = 30;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryTrendChart(Project project) {
        this.project = project;
        setBackground(BACKGROUND_COLOR);
        setBorder(JBUI.Borders.empty(10));
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateChartDimensions();
                repaint();
            }
        });
        
        updateChartDimensions();
    }
    
    /**
     * Update the chart dimensions based on the panel size
     */
    private void updateChartDimensions() {
        chartWidth = getWidth() - chartMarginLeft - chartMarginRight;
        chartHeight = getHeight() - chartMarginTop - chartMarginBottom;
    }
    
    /**
     * Update the data in the chart
     * 
     * @param snapshots The memory snapshots to display
     */
    public void updateData(List<MemorySnapshot> snapshots) {
        this.snapshots = new ArrayList<>(snapshots);
        repaint();
    }
    
    /**
     * Set whether to show heap memory in the chart
     * 
     * @param show Whether to show heap memory
     */
    public void setShowHeapMemory(boolean show) {
        this.showHeapMemory = show;
        repaint();
    }
    
    /**
     * Set whether to show non-heap memory in the chart
     * 
     * @param show Whether to show non-heap memory
     */
    public void setShowNonHeapMemory(boolean show) {
        this.showNonHeapMemory = show;
        repaint();
    }
    
    /**
     * Set whether to show memory pools in the chart
     * 
     * @param show Whether to show memory pools
     */
    public void setShowMemoryPools(boolean show) {
        this.showMemoryPools = show;
        repaint();
    }
    
    /**
     * Set the selected memory pool to highlight
     * 
     * @param poolName The pool name to highlight
     */
    public void setSelectedPoolName(String poolName) {
        this.selectedPoolName = poolName;
        repaint();
    }
    
    /**
     * Set the number of minutes to show in the chart
     * 
     * @param minutes The number of minutes
     */
    public void setMinutesToShow(int minutes) {
        this.minutesToShow = minutes;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw axes and grid
            drawAxes(g2d);
            
            // Draw data
            if (!snapshots.isEmpty()) {
                drawDataSeries(g2d);
            } else {
                drawNoDataMessage(g2d);
            }
            
        } finally {
            g2d.dispose();
        }
    }
    
    /**
     * Draw the chart axes and grid
     * 
     * @param g2d The graphics context
     */
    private void drawAxes(Graphics2D g2d) {
        // Draw Y axis
        g2d.setColor(AXIS_COLOR);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new Line2D.Double(
                chartMarginLeft, chartMarginTop,
                chartMarginLeft, chartMarginTop + chartHeight
        ));
        
        // Draw X axis
        g2d.draw(new Line2D.Double(
                chartMarginLeft, chartMarginTop + chartHeight,
                chartMarginLeft + chartWidth, chartMarginTop + chartHeight
        ));
        
        // Draw Y axis grid lines and labels
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(JBUI.Fonts.smallFont());
        
        FontMetrics fm = g2d.getFontMetrics();
        int textHeight = fm.getHeight();
        
        for (int i = 0; i <= 10; i++) {
            int y = chartMarginTop + chartHeight - (i * chartHeight / 10);
            
            // Grid line
            g2d.setColor(GRID_COLOR);
            g2d.setStroke(new BasicStroke(0.5f));
            g2d.draw(new Line2D.Double(
                    chartMarginLeft, y,
                    chartMarginLeft + chartWidth, y
            ));
            
            // Label
            g2d.setColor(TEXT_COLOR);
            String label = (i * 10) + "%";
            Rectangle2D bounds = fm.getStringBounds(label, g2d);
            g2d.drawString(label, 
                    (int)(chartMarginLeft - bounds.getWidth() - 5), 
                    y + (textHeight / 2) - 2);
        }
        
        // Draw title
        g2d.setFont(JBUI.Fonts.labelFont());
        g2d.setColor(TEXT_COLOR);
        String title = "Memory Usage Trend";
        Rectangle2D titleBounds = g2d.getFontMetrics().getStringBounds(title, g2d);
        g2d.drawString(title, 
                (int)(chartMarginLeft + (chartWidth - titleBounds.getWidth()) / 2), 
                chartMarginTop - 10);
    }
    
    /**
     * Draw the data series on the chart
     * 
     * @param g2d The graphics context
     */
    private void drawDataSeries(Graphics2D g2d) {
        if (snapshots.isEmpty()) {
            return;
        }
        
        // Calculate time range
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (minutesToShow * 60 * 1000);
        
        // Filter snapshots within the time range
        List<MemorySnapshot> visibleSnapshots = new ArrayList<>();
        for (MemorySnapshot snapshot : snapshots) {
            if (snapshot.getTimestamp() >= startTime) {
                visibleSnapshots.add(snapshot);
            }
        }
        
        if (visibleSnapshots.isEmpty()) {
            drawNoDataMessage(g2d);
            return;
        }
        
        // Find min/max time in visible snapshots
        long minTime = visibleSnapshots.get(0).getTimestamp();
        long maxTime = visibleSnapshots.get(visibleSnapshots.size() - 1).getTimestamp();
        
        // If we have less than 2 points, adjust the range to show something
        if (maxTime - minTime < 60 * 1000) {
            maxTime = minTime + (60 * 1000);
        }
        
        // Draw X axis time labels
        drawTimeLabels(g2d, minTime, maxTime);
        
        // Draw data series
        if (showHeapMemory || showNonHeapMemory) {
            drawMemorySeries(g2d, visibleSnapshots, minTime, maxTime);
        }
        
        if (showMemoryPools) {
            drawMemoryPoolSeries(g2d, visibleSnapshots, minTime, maxTime);
        }
    }
    
    /**
     * Draw time labels on the X axis
     * 
     * @param g2d The graphics context
     * @param minTime The minimum time
     * @param maxTime The maximum time
     */
    private void drawTimeLabels(Graphics2D g2d, long minTime, long maxTime) {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(JBUI.Fonts.smallFont());
        
        long timeRange = maxTime - minTime;
        int numLabels = Math.min(6, visibleWidth() / 80); // Limit labels based on width
        
        for (int i = 0; i <= numLabels; i++) {
            long time = minTime + (i * timeRange / numLabels);
            int x = timeToX(time, minTime, maxTime);
            
            // Vertical grid line
            g2d.setColor(GRID_COLOR);
            g2d.setStroke(new BasicStroke(0.5f));
            g2d.draw(new Line2D.Double(
                    x, chartMarginTop,
                    x, chartMarginTop + chartHeight
            ));
            
            // Label
            g2d.setColor(TEXT_COLOR);
            String label = timeFormat.format(new Date(time));
            Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(label, g2d);
            g2d.drawString(label, 
                    (int)(x - bounds.getWidth() / 2), 
                    chartMarginTop + chartHeight + 20);
        }
    }
    
    /**
     * Draw the total memory usage, heap memory, and non-heap memory series
     * 
     * @param g2d The graphics context
     * @param snapshots The snapshots to draw
     * @param minTime The minimum time
     * @param maxTime The maximum time
     */
    private void drawMemorySeries(Graphics2D g2d, List<MemorySnapshot> snapshots, long minTime, long maxTime) {
        // Draw total memory usage
        Path2D totalPath = new Path2D.Double();
        
        for (int i = 0; i < snapshots.size(); i++) {
            MemorySnapshot snapshot = snapshots.get(i);
            
            int x = timeToX(snapshot.getTimestamp(), minTime, maxTime);
            int y = percentToY(snapshot.getUsagePercentage());
            
            if (i == 0) {
                totalPath.moveTo(x, y);
            } else {
                totalPath.lineTo(x, y);
            }
        }
        
        // Draw filled area for total memory usage
        Path2D totalAreaPath = new Path2D.Double(totalPath);
        totalAreaPath.lineTo(timeToX(snapshots.get(snapshots.size() - 1).getTimestamp(), minTime, maxTime), 
                percentToY(0));
        totalAreaPath.lineTo(timeToX(snapshots.get(0).getTimestamp(), minTime, maxTime), 
                percentToY(0));
        totalAreaPath.closePath();
        
        g2d.setColor(new Color(45, 183, 93, 40));
        g2d.fill(totalAreaPath);
        
        // Draw line for total memory usage
        g2d.setColor(NORMAL_COLOR);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.draw(totalPath);
        
        // Add a dot at each data point with color based on pressure level
        for (MemorySnapshot snapshot : snapshots) {
            int x = timeToX(snapshot.getTimestamp(), minTime, maxTime);
            int y = percentToY(snapshot.getUsagePercentage());
            
            // Determine color based on usage percentage
            Color dotColor;
            if (snapshot.getUsagePercentage() >= 90) {
                dotColor = EMERGENCY_COLOR;
            } else if (snapshot.getUsagePercentage() >= 80) {
                dotColor = CRITICAL_COLOR;
            } else if (snapshot.getUsagePercentage() >= 70) {
                dotColor = WARNING_COLOR;
            } else {
                dotColor = NORMAL_COLOR;
            }
            
            g2d.setColor(dotColor);
            g2d.fillOval(x - 4, y - 4, 8, 8);
            g2d.setColor(JBColor.background());
            g2d.drawOval(x - 4, y - 4, 8, 8);
        }
        
        // Draw heap memory usage if enabled
        if (showHeapMemory) {
            Path2D heapPath = new Path2D.Double();
            
            for (int i = 0; i < snapshots.size(); i++) {
                MemorySnapshot snapshot = snapshots.get(i);
                double heapPercentage = calculateHeapPercentage(snapshot);
                
                int x = timeToX(snapshot.getTimestamp(), minTime, maxTime);
                int y = percentToY(heapPercentage);
                
                if (i == 0) {
                    heapPath.moveTo(x, y);
                } else {
                    heapPath.lineTo(x, y);
                }
            }
            
            g2d.setColor(new JBColor(new Color(0, 120, 255), new Color(30, 144, 255)));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, 
                    new float[]{4, 4}, 0));
            g2d.draw(heapPath);
        }
        
        // Draw non-heap memory usage if enabled
        if (showNonHeapMemory) {
            Path2D nonHeapPath = new Path2D.Double();
            
            for (int i = 0; i < snapshots.size(); i++) {
                MemorySnapshot snapshot = snapshots.get(i);
                double nonHeapPercentage = calculateNonHeapPercentage(snapshot);
                
                int x = timeToX(snapshot.getTimestamp(), minTime, maxTime);
                int y = percentToY(nonHeapPercentage);
                
                if (i == 0) {
                    nonHeapPath.moveTo(x, y);
                } else {
                    nonHeapPath.lineTo(x, y);
                }
            }
            
            g2d.setColor(new JBColor(new Color(255, 99, 132), new Color(255, 99, 132)));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, 
                    new float[]{8, 4}, 0));
            g2d.draw(nonHeapPath);
        }
        
        // Draw legend
        int legendX = chartMarginLeft + 10;
        int legendY = chartMarginTop + 25;
        
        g2d.setFont(JBUI.Fonts.smallFont());
        FontMetrics fm = g2d.getFontMetrics();
        
        // Total memory legend
        g2d.setColor(NORMAL_COLOR);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawLine(legendX, legendY, legendX + 20, legendY);
        g2d.drawString("Total Memory", legendX + 25, legendY + 4);
        
        // Heap memory legend
        if (showHeapMemory) {
            legendY += 20;
            g2d.setColor(new JBColor(new Color(0, 120, 255), new Color(30, 144, 255)));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, 
                    new float[]{4, 4}, 0));
            g2d.drawLine(legendX, legendY, legendX + 20, legendY);
            g2d.drawString("Heap Memory", legendX + 25, legendY + 4);
        }
        
        // Non-heap memory legend
        if (showNonHeapMemory) {
            legendY += 20;
            g2d.setColor(new JBColor(new Color(255, 99, 132), new Color(255, 99, 132)));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, 
                    new float[]{8, 4}, 0));
            g2d.drawLine(legendX, legendY, legendX + 20, legendY);
            g2d.drawString("Non-Heap Memory", legendX + 25, legendY + 4);
        }
    }
    
    /**
     * Draw memory pool series
     * 
     * @param g2d The graphics context
     * @param snapshots The snapshots to draw
     * @param minTime The minimum time
     * @param maxTime The maximum time
     */
    private void drawMemoryPoolSeries(Graphics2D g2d, List<MemorySnapshot> snapshots, long minTime, long maxTime) {
        if (snapshots.isEmpty() || selectedPoolName == null) {
            return;
        }
        
        MemorySnapshot.PoolData firstPoolData = null;
        for (MemorySnapshot snapshot : snapshots) {
            for (MemorySnapshot.PoolData poolData : snapshot.getPoolData()) {
                if (poolData.getName().equals(selectedPoolName)) {
                    firstPoolData = poolData;
                    break;
                }
            }
            if (firstPoolData != null) {
                break;
            }
        }
        
        if (firstPoolData == null) {
            return;
        }
        
        // Draw selected pool
        Path2D poolPath = new Path2D.Double();
        boolean started = false;
        
        for (int i = 0; i < snapshots.size(); i++) {
            MemorySnapshot snapshot = snapshots.get(i);
            
            MemorySnapshot.PoolData poolData = null;
            for (MemorySnapshot.PoolData pd : snapshot.getPoolData()) {
                if (pd.getName().equals(selectedPoolName)) {
                    poolData = pd;
                    break;
                }
            }
            
            if (poolData != null) {
                int x = timeToX(snapshot.getTimestamp(), minTime, maxTime);
                int y = percentToY(poolData.getUsagePercentage());
                
                if (!started) {
                    poolPath.moveTo(x, y);
                    started = true;
                } else {
                    poolPath.lineTo(x, y);
                }
            }
        }
        
        g2d.setColor(new JBColor(new Color(153, 102, 255), new Color(153, 102, 255)));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.draw(poolPath);
        
        // Add pool legend
        int legendX = chartMarginLeft + 10;
        int legendY = chartMarginTop + 85;
        
        g2d.setFont(JBUI.Fonts.smallFont());
        g2d.setColor(new JBColor(new Color(153, 102, 255), new Color(153, 102, 255)));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawLine(legendX, legendY, legendX + 20, legendY);
        g2d.drawString(selectedPoolName + " Pool", legendX + 25, legendY + 4);
    }
    
    /**
     * Draw a message when no data is available
     * 
     * @param g2d The graphics context
     */
    private void drawNoDataMessage(Graphics2D g2d) {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(JBUI.Fonts.mediumFont());
        
        String message = "No memory data available";
        Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(message, g2d);
        
        g2d.drawString(message, 
                (int)(chartMarginLeft + (chartWidth - bounds.getWidth()) / 2), 
                (int)(chartMarginTop + (chartHeight / 2)));
    }
    
    /**
     * Convert a time value to an X coordinate
     * 
     * @param time The time value
     * @param minTime The minimum time
     * @param maxTime The maximum time
     * @return The X coordinate
     */
    private int timeToX(long time, long minTime, long maxTime) {
        double range = maxTime - minTime;
        double position = (time - minTime) / range;
        
        return (int)(chartMarginLeft + (position * chartWidth));
    }
    
    /**
     * Convert a percentage value to a Y coordinate
     * 
     * @param percent The percentage value
     * @return The Y coordinate
     */
    private int percentToY(double percent) {
        return (int)(chartMarginTop + chartHeight - (percent * chartHeight / 100.0));
    }
    
    /**
     * Calculate the heap usage percentage
     * 
     * @param snapshot The memory snapshot
     * @return The heap usage percentage
     */
    private double calculateHeapPercentage(MemorySnapshot snapshot) {
        return (snapshot.getHeapUsed() * 100.0) / snapshot.getMaxMemory();
    }
    
    /**
     * Calculate the non-heap usage percentage
     * 
     * @param snapshot The memory snapshot
     * @return The non-heap usage percentage
     */
    private double calculateNonHeapPercentage(MemorySnapshot snapshot) {
        return (snapshot.getNonHeapUsed() * 100.0) / snapshot.getMaxMemory();
    }
    
    /**
     * Get the visible width of the chart
     * 
     * @return The visible width
     */
    private int visibleWidth() {
        return chartWidth;
    }
}