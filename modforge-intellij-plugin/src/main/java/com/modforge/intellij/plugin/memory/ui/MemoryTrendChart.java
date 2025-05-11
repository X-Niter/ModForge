package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshotManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Component that displays a chart of memory usage over time.
 */
public class MemoryTrendChart extends JPanel {
    private static final Logger LOG = Logger.getInstance(MemoryTrendChart.class);
    
    private static final Color[] CHART_COLORS = {
            new Color(45, 183, 93),      // Green for Used Memory
            new Color(0, 120, 215),      // Blue for Free Memory
            new Color(122, 117, 199),    // Purple for Total Memory
            new Color(255, 141, 0)       // Orange for Max Memory
    };
    
    private static final Color BACKGROUND_COLOR = new JBColor(new Color(245, 245, 245), new Color(60, 63, 65));
    private static final Color GRID_COLOR = new JBColor(new Color(220, 220, 220), new Color(80, 83, 85));
    private static final Color TEXT_COLOR = new JBColor(new Color(60, 60, 60), new Color(187, 187, 187));
    private static final Color HOVER_LINE_COLOR = new JBColor(new Color(180, 180, 180), new Color(120, 123, 125));
    
    private static final int PADDING = 50;
    private static final int X_AXIS_LABEL_HEIGHT = 25;
    private static final int Y_AXIS_LABEL_WIDTH = 60;
    private static final int HOVER_TOLERANCE = 5;
    
    private final List<MemorySnapshot> displaySnapshots = new ArrayList<>();
    private int chartType = 0; // 0=Used Memory, 1=Memory Usage %, 2=Free Memory, 3=All Types
    private int timeRangeMinutes = 15; // How far back to show (in minutes)
    private boolean showGrid = true;
    private boolean showLabels = true;
    private Point hoverPoint = null;
    private MemorySnapshot hoverSnapshot = null;
    
    /**
     * Constructor
     */
    public MemoryTrendChart() {
        setBackground(BACKGROUND_COLOR);
        setBorder(JBUI.Borders.empty(5));
        setPreferredSize(new Dimension(800, 400));
        
        // Add resize listener
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaint();
            }
        });
        
        // Add mouse listeners for hover information
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverPoint(e.getPoint());
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverPoint = null;
                hoverSnapshot = null;
                repaint();
            }
        });
        
        // Initial data load
        refreshData();
        
        // Start a timer to update the chart data
        Timer updateTimer = new Timer(10000, e -> refreshData()); // Update every 10 seconds
        updateTimer.setInitialDelay(10000);
        updateTimer.start();
    }
    
    /**
     * Set the chart type
     *
     * @param type The chart type (0=Used Memory, 1=Memory Usage %, 2=Free Memory, 3=All Types)
     */
    public void setChartType(int type) {
        if (type >= 0 && type <= 3) {
            this.chartType = type;
            repaint();
        }
    }
    
    /**
     * Set the time range to display
     *
     * @param minutes Minutes of history to show (-1 for all data)
     */
    public void setTimeRange(int minutes) {
        this.timeRangeMinutes = minutes;
        refreshData();
    }
    
    /**
     * Set whether to show the grid
     *
     * @param show True to show the grid, false to hide it
     */
    public void setShowGrid(boolean show) {
        this.showGrid = show;
        repaint();
    }
    
    /**
     * Set whether to show labels
     *
     * @param show True to show labels, false to hide them
     */
    public void setShowLabels(boolean show) {
        this.showLabels = show;
        repaint();
    }
    
    /**
     * Refresh data from the memory snapshot manager
     */
    public void refreshData() {
        try {
            // Get memory snapshot manager
            MemorySnapshotManager snapshotManager = MemorySnapshotManager.getInstance();
            if (snapshotManager == null) {
                LOG.warn("Memory snapshot manager not available");
                return;
            }
            
            // Get filtered snapshots
            synchronized (displaySnapshots) {
                displaySnapshots.clear();
                
                List<MemorySnapshot> snapshots = snapshotManager.getSnapshots();
                if (snapshots == null || snapshots.isEmpty()) {
                    LOG.info("No memory snapshots available");
                    return;
                }
                
                if (timeRangeMinutes > 0) {
                    // Filter by time range
                    LocalDateTime cutoffTime = LocalDateTime.now().minus(timeRangeMinutes, ChronoUnit.MINUTES);
                    for (MemorySnapshot snapshot : snapshots) {
                        if (snapshot.getTimestamp().isAfter(cutoffTime)) {
                            displaySnapshots.add(snapshot);
                        }
                    }
                } else {
                    // Show all snapshots
                    displaySnapshots.addAll(snapshots);
                }
                
                // If we have no data in the time range, show at least the latest snapshot
                if (displaySnapshots.isEmpty() && !snapshots.isEmpty()) {
                    displaySnapshots.add(snapshots.get(snapshots.size() - 1));
                }
            }
            
            repaint();
        } catch (Exception e) {
            LOG.error("Error refreshing memory trend data", e);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Get chart area
        Rectangle2D chartArea = new Rectangle2D.Double(
                PADDING + Y_AXIS_LABEL_WIDTH,
                PADDING,
                width - (2 * PADDING) - Y_AXIS_LABEL_WIDTH,
                height - (2 * PADDING) - X_AXIS_LABEL_HEIGHT
        );
        
        // Draw chart
        synchronized (displaySnapshots) {
            if (displaySnapshots.isEmpty()) {
                drawNoDataMessage(g2d, chartArea);
                return;
            }
            
            drawChart(g2d, chartArea);
        }
        
        g2d.dispose();
    }
    
    /**
     * Draw a message when no data is available
     *
     * @param g2d       The graphics context
     * @param chartArea The chart area
     */
    private void drawNoDataMessage(Graphics2D g2d, Rectangle2D chartArea) {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 14f));
        
        String message = "No memory trend data available";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(message);
        int textHeight = fm.getHeight();
        
        g2d.drawString(
                message,
                (int) (chartArea.getCenterX() - (textWidth / 2)),
                (int) (chartArea.getCenterY() - (textHeight / 2) + fm.getAscent())
        );
    }
    
    /**
     * Draw the chart with available data
     *
     * @param g2d       The graphics context
     * @param chartArea The chart area
     */
    private void drawChart(Graphics2D g2d, Rectangle2D chartArea) {
        // Determine value ranges
        long maxValue = determineMaxValue();
        long minValue = 0; // Always start from zero for memory
        
        // Draw grid and labels
        if (showGrid) {
            drawGrid(g2d, chartArea, minValue, maxValue);
        }
        
        if (showLabels) {
            drawLabels(g2d, chartArea, minValue, maxValue);
        }
        
        // Draw data lines based on chart type
        if (chartType == 3) {
            // Draw all memory types
            drawDataLine(g2d, chartArea, 0, minValue, maxValue, CHART_COLORS[0]);  // Used
            drawDataLine(g2d, chartArea, 2, minValue, maxValue, CHART_COLORS[1]);  // Free
            drawDataLine(g2d, chartArea, 1, minValue, maxValue, CHART_COLORS[2]);  // Total
        } else {
            // Draw selected memory type
            drawDataLine(g2d, chartArea, chartType, minValue, maxValue, CHART_COLORS[chartType]);
        }
        
        // Draw hover information
        if (hoverPoint != null && hoverSnapshot != null) {
            drawHoverInfo(g2d, chartArea, hoverPoint, hoverSnapshot, minValue, maxValue);
        }
    }
    
    /**
     * Determine the maximum value for the y-axis based on the chart type
     *
     * @return The maximum value
     */
    private long determineMaxValue() {
        long maxValue = 0;
        
        for (MemorySnapshot snapshot : displaySnapshots) {
            switch (chartType) {
                case 0: // Used Memory
                    maxValue = Math.max(maxValue, snapshot.getUsedMemory());
                    break;
                case 1: // Memory Usage %
                    maxValue = 100; // Fixed scale for percentage
                    break;
                case 2: // Free Memory
                    maxValue = Math.max(maxValue, snapshot.getFreeMemory());
                    break;
                case 3: // All Types
                    maxValue = Math.max(maxValue, snapshot.getTotalMemory());
                    break;
            }
        }
        
        // For memory values, round up to a nice number
        if (chartType != 1) { // Not percentage
            maxValue = ((maxValue / 1_000_000_000) + 1) * 1_000_000_000; // Round up to next GB
        }
        
        return maxValue;
    }
    
    /**
     * Draw the grid lines
     *
     * @param g2d       The graphics context
     * @param chartArea The chart area
     * @param minValue  The minimum value
     * @param maxValue  The maximum value
     */
    private void drawGrid(Graphics2D g2d, Rectangle2D chartArea, long minValue, long maxValue) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                10.0f, new float[]{5.0f}, 0.0f));
        
        // Horizontal grid lines (y-axis)
        int numYLines = 5;
        for (int i = 0; i <= numYLines; i++) {
            double y = chartArea.getMaxY() - (i * (chartArea.getHeight() / numYLines));
            g2d.draw(new Line2D.Double(chartArea.getMinX(), y, chartArea.getMaxX(), y));
        }
        
        // Vertical grid lines (x-axis)
        if (displaySnapshots.size() > 1) {
            LocalDateTime firstTime = displaySnapshots.get(0).getTimestamp();
            LocalDateTime lastTime = displaySnapshots.get(displaySnapshots.size() - 1).getTimestamp();
            
            // Calculate grid intervals based on time range
            long minutes = ChronoUnit.MINUTES.between(firstTime, lastTime);
            int numXLines = Math.min(10, (int) Math.max(2, minutes));
            
            for (int i = 0; i <= numXLines; i++) {
                double ratio = (double) i / numXLines;
                double x = chartArea.getMinX() + (ratio * chartArea.getWidth());
                g2d.draw(new Line2D.Double(x, chartArea.getMinY(), x, chartArea.getMaxY()));
            }
        }
        
        // Reset stroke
        g2d.setStroke(new BasicStroke(1.0f));
    }
    
    /**
     * Draw the axis labels
     *
     * @param g2d       The graphics context
     * @param chartArea The chart area
     * @param minValue  The minimum value
     * @param maxValue  The maximum value
     */
    private void drawLabels(Graphics2D g2d, Rectangle2D chartArea, long minValue, long maxValue) {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(g2d.getFont().deriveFont(10f));
        FontMetrics fm = g2d.getFontMetrics();
        
        // Y-axis labels
        int numYLabels = 5;
        for (int i = 0; i <= numYLabels; i++) {
            double y = chartArea.getMaxY() - (i * (chartArea.getHeight() / numYLabels));
            double value = minValue + (i * ((maxValue - minValue) / numYLabels));
            
            String label;
            if (chartType == 1) { // Percentage
                label = String.format("%.0f%%", value);
            } else {
                label = MemoryUtils.formatMemorySize((long) value);
            }
            
            g2d.drawString(label, (int) (chartArea.getMinX() - fm.stringWidth(label) - 5), (int) (y + fm.getAscent() / 2));
        }
        
        // X-axis labels (time)
        if (displaySnapshots.size() > 1) {
            LocalDateTime firstTime = displaySnapshots.get(0).getTimestamp();
            LocalDateTime lastTime = displaySnapshots.get(displaySnapshots.size() - 1).getTimestamp();
            
            SimpleDateFormat timeFormat;
            if (ChronoUnit.HOURS.between(firstTime, lastTime) > 12) {
                timeFormat = new SimpleDateFormat("MM/dd HH:mm");
            } else {
                timeFormat = new SimpleDateFormat("HH:mm:ss");
            }
            
            int numXLabels = Math.min(5, displaySnapshots.size());
            for (int i = 0; i <= numXLabels; i++) {
                double ratio = (double) i / numXLabels;
                double x = chartArea.getMinX() + (ratio * chartArea.getWidth());
                
                // Interpolate the time
                LocalDateTime labelTime = firstTime.plus(
                        (long) (ChronoUnit.SECONDS.between(firstTime, lastTime) * ratio),
                        ChronoUnit.SECONDS
                );
                
                Date date = Date.from(labelTime.atZone(ZoneId.systemDefault()).toInstant());
                String label = timeFormat.format(date);
                
                g2d.drawString(
                        label,
                        (int) (x - (fm.stringWidth(label) / 2)),
                        (int) (chartArea.getMaxY() + fm.getHeight() + 5)
                );
            }
        }
        
        // Chart title based on type
        String chartTitle = getChartTitle();
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 12f));
        fm = g2d.getFontMetrics();
        g2d.drawString(
                chartTitle,
                (int) (chartArea.getCenterX() - (fm.stringWidth(chartTitle) / 2)),
                (int) (chartArea.getMinY() - 10)
        );
    }
    
    /**
     * Get the chart title based on the chart type
     *
     * @return The chart title
     */
    private String getChartTitle() {
        switch (chartType) {
            case 0:
                return "Used Memory Over Time";
            case 1:
                return "Memory Usage Percentage Over Time";
            case 2:
                return "Free Memory Over Time";
            case 3:
                return "Memory Metrics Over Time";
            default:
                return "Memory Trend";
        }
    }
    
    /**
     * Draw a data line for the specified memory metric
     *
     * @param g2d       The graphics context
     * @param chartArea The chart area
     * @param dataType  The data type (0=Used, 1=Total, 2=Free)
     * @param minValue  The minimum value
     * @param maxValue  The maximum value
     * @param color     The line color
     */
    private void drawDataLine(Graphics2D g2d, Rectangle2D chartArea, int dataType, long minValue, long maxValue, Color color) {
        if (displaySnapshots.size() < 2) {
            return; // Need at least two points for a line
        }
        
        // Calculate the path
        Path2D path = new Path2D.Double();
        boolean started = false;
        
        LocalDateTime firstTime = displaySnapshots.get(0).getTimestamp();
        LocalDateTime lastTime = displaySnapshots.get(displaySnapshots.size() - 1).getTimestamp();
        long totalTimeSpan = ChronoUnit.SECONDS.between(firstTime, lastTime);
        
        for (MemorySnapshot snapshot : displaySnapshots) {
            double x = calculateXPosition(snapshot.getTimestamp(), firstTime, lastTime, chartArea);
            double y = calculateYPosition(dataType, snapshot, minValue, maxValue, chartArea);
            
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        
        // Draw the line
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.draw(path);
        
        // Draw data points
        for (MemorySnapshot snapshot : displaySnapshots) {
            double x = calculateXPosition(snapshot.getTimestamp(), firstTime, lastTime, chartArea);
            double y = calculateYPosition(dataType, snapshot, minValue, maxValue, chartArea);
            
            g2d.setColor(color);
            g2d.fillOval((int) (x - 3), (int) (y - 3), 6, 6);
            g2d.setColor(color.darker());
            g2d.drawOval((int) (x - 3), (int) (y - 3), 6, 6);
        }
        
        // Reset stroke
        g2d.setStroke(new BasicStroke(1.0f));
        
        // Add legend if showing all types
        if (chartType == 3) {
            String label;
            switch (dataType) {
                case 0:
                    label = "Used Memory";
                    break;
                case 1:
                    label = "Total Memory";
                    break;
                case 2:
                    label = "Free Memory";
                    break;
                default:
                    label = "Unknown";
            }
            
            g2d.setFont(g2d.getFont().deriveFont(10f));
            FontMetrics fm = g2d.getFontMetrics();
            
            // Position legend in top-right corner with some offset
            int legendX = (int) chartArea.getMaxX() - fm.stringWidth(label) - 30;
            int legendY = (int) chartArea.getMinY() + 15 + (dataType * 15);
            
            // Draw legend color box
            g2d.setColor(color);
            g2d.fillRect(legendX - 15, legendY - 10, 10, 10);
            g2d.setColor(color.darker());
            g2d.drawRect(legendX - 15, legendY - 10, 10, 10);
            
            // Draw legend text
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(label, legendX, legendY);
        }
    }
    
    /**
     * Calculate the x position for a timestamp
     *
     * @param timestamp The timestamp
     * @param firstTime The first timestamp in the data
     * @param lastTime  The last timestamp in the data
     * @param chartArea The chart area
     * @return The x position
     */
    private double calculateXPosition(LocalDateTime timestamp, LocalDateTime firstTime, LocalDateTime lastTime, Rectangle2D chartArea) {
        long totalTimeSpan = ChronoUnit.SECONDS.between(firstTime, lastTime);
        
        if (totalTimeSpan == 0) {
            return chartArea.getCenterX(); // Special case for single point or all same time
        }
        
        long secondsFromStart = ChronoUnit.SECONDS.between(firstTime, timestamp);
        double ratio = (double) secondsFromStart / totalTimeSpan;
        
        return chartArea.getMinX() + (ratio * chartArea.getWidth());
    }
    
    /**
     * Calculate the y position for a memory value
     *
     * @param dataType  The data type (0=Used, 1=Total, 2=Free)
     * @param snapshot  The memory snapshot
     * @param minValue  The minimum value
     * @param maxValue  The maximum value
     * @param chartArea The chart area
     * @return The y position
     */
    private double calculateYPosition(int dataType, MemorySnapshot snapshot, long minValue, long maxValue, Rectangle2D chartArea) {
        double value;
        
        switch (dataType) {
            case 0: // Used Memory
                if (chartType == 1) { // Percentage
                    value = (double) snapshot.getUsedMemory() * 100 / snapshot.getTotalMemory();
                } else {
                    value = snapshot.getUsedMemory();
                }
                break;
            case 1: // Total Memory
                value = snapshot.getTotalMemory();
                break;
            case 2: // Free Memory
                value = snapshot.getFreeMemory();
                break;
            default:
                value = 0;
        }
        
        // Convert value to y position (inverted since y-axis goes down)
        double valueRange = maxValue - minValue;
        double ratio = (value - minValue) / valueRange;
        
        return chartArea.getMaxY() - (ratio * chartArea.getHeight());
    }
    
    /**
     * Draw hover information
     *
     * @param g2d       The graphics context
     * @param chartArea The chart area
     * @param point     The hover point
     * @param snapshot  The snapshot at the hover point
     * @param minValue  The minimum value
     * @param maxValue  The maximum value
     */
    private void drawHoverInfo(Graphics2D g2d, Rectangle2D chartArea, Point point, MemorySnapshot snapshot, long minValue, long maxValue) {
        // Draw vertical line at hover point
        g2d.setColor(HOVER_LINE_COLOR);
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
        g2d.draw(new Line2D.Double(point.x, chartArea.getMinY(), point.x, chartArea.getMaxY()));
        
        // Create info box
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = Date.from(snapshot.getTimestamp().atZone(ZoneId.systemDefault()).toInstant());
        String timeStr = timeFormat.format(date);
        
        List<String> infoLines = new ArrayList<>();
        infoLines.add("Time: " + timeStr);
        infoLines.add("Used: " + MemoryUtils.formatMemorySize(snapshot.getUsedMemory()) + 
                String.format(" (%.1f%%)", (double) snapshot.getUsedMemory() * 100 / snapshot.getTotalMemory()));
        infoLines.add("Free: " + MemoryUtils.formatMemorySize(snapshot.getFreeMemory()));
        infoLines.add("Total: " + MemoryUtils.formatMemorySize(snapshot.getTotalMemory()));
        infoLines.add("Max: " + MemoryUtils.formatMemorySize(snapshot.getMaxMemory()));
        
        // Calculate box size
        g2d.setFont(g2d.getFont().deriveFont(11f));
        FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getHeight();
        int boxWidth = 0;
        for (String line : infoLines) {
            boxWidth = Math.max(boxWidth, fm.stringWidth(line));
        }
        int boxHeight = lineHeight * infoLines.size() + 10;
        
        // Position box near the cursor but keep it in bounds
        int boxX = point.x + 10;
        if (boxX + boxWidth + 10 > chartArea.getMaxX()) {
            boxX = point.x - boxWidth - 10;
        }
        
        int boxY = point.y - boxHeight / 2;
        if (boxY < chartArea.getMinY()) {
            boxY = (int) chartArea.getMinY();
        } else if (boxY + boxHeight > chartArea.getMaxY()) {
            boxY = (int) chartArea.getMaxY() - boxHeight;
        }
        
        // Draw box background with semi-transparency
        g2d.setColor(new Color(245, 245, 245, 220));
        g2d.fillRoundRect(boxX, boxY, boxWidth + 20, boxHeight, 6, 6);
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRoundRect(boxX, boxY, boxWidth + 20, boxHeight, 6, 6);
        
        // Draw info text
        g2d.setColor(new Color(50, 50, 50));
        for (int i = 0; i < infoLines.size(); i++) {
            g2d.drawString(infoLines.get(i), boxX + 10, boxY + (i + 1) * lineHeight);
        }
    }
    
    /**
     * Update the hover point and find the nearest snapshot
     *
     * @param point The mouse point
     */
    private void updateHoverPoint(Point point) {
        synchronized (displaySnapshots) {
            if (displaySnapshots.isEmpty()) {
                hoverPoint = null;
                hoverSnapshot = null;
                return;
            }
            
            // Get chart area
            Rectangle2D chartArea = new Rectangle2D.Double(
                    PADDING + Y_AXIS_LABEL_WIDTH,
                    PADDING,
                    getWidth() - (2 * PADDING) - Y_AXIS_LABEL_WIDTH,
                    getHeight() - (2 * PADDING) - X_AXIS_LABEL_HEIGHT
            );
            
            // Check if point is in chart area
            if (!chartArea.contains(point)) {
                hoverPoint = null;
                hoverSnapshot = null;
                return;
            }
            
            // Find the nearest snapshot
            LocalDateTime firstTime = displaySnapshots.get(0).getTimestamp();
            LocalDateTime lastTime = displaySnapshots.get(displaySnapshots.size() - 1).getTimestamp();
            
            double nearestDistance = Double.MAX_VALUE;
            MemorySnapshot nearestSnapshot = null;
            Point nearestPoint = null;
            
            for (MemorySnapshot snapshot : displaySnapshots) {
                double x = calculateXPosition(snapshot.getTimestamp(), firstTime, lastTime, chartArea);
                double distance = Math.abs(x - point.x);
                
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestSnapshot = snapshot;
                    
                    // Calculate y position based on chart type
                    double y;
                    if (chartType == 3) { // All types - use used memory for hover
                        y = calculateYPosition(0, snapshot, 0, determineMaxValue(), chartArea);
                    } else {
                        y = calculateYPosition(chartType == 2 ? 2 : chartType, snapshot, 0, determineMaxValue(), chartArea);
                    }
                    
                    nearestPoint = new Point((int) x, (int) y);
                }
            }
            
            // Only update if we found a snapshot within tolerance
            if (nearestDistance <= HOVER_TOLERANCE) {
                hoverPoint = nearestPoint;
                hoverSnapshot = nearestSnapshot;
                repaint();
            } else if (hoverPoint != null) {
                // Clear hover if we moved away
                hoverPoint = null;
                hoverSnapshot = null;
                repaint();
            }
        }
    }
}