package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshotManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component for displaying memory usage trends in a chart
 */
public class MemoryTrendChart extends JPanel {
    private static final Logger LOG = Logger.getInstance(MemoryTrendChart.class);
    
    // Constants for chart dimensions and styles
    private static final int PADDING = 20;
    private static final int AXIS_LABEL_PADDING = 5;
    private static final int POINT_SIZE = 4;
    private static final int HOVER_POINT_SIZE = 8;
    private static final Stroke GRID_STROKE = new BasicStroke(0.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 0, new float[]{1, 2}, 0);
    private static final Stroke DATA_STROKE = new BasicStroke(2.0f);
    private static final Stroke HOVER_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    // Chart colors
    private static final Color AXIS_COLOR = JBColor.border();
    private static final Color GRID_COLOR = new JBColor(new Color(230, 230, 230), new Color(70, 70, 70));
    private static final Color USED_MEMORY_COLOR = new JBColor(new Color(0, 122, 255), new Color(0, 122, 255));
    private static final Color FREE_MEMORY_COLOR = new JBColor(new Color(50, 205, 50), new Color(50, 205, 50));
    private static final Color TOTAL_MEMORY_COLOR = new JBColor(new Color(128, 128, 128), new Color(160, 160, 160));
    private static final Color THRESHOLD_COLOR = new JBColor(new Color(255, 69, 58), new Color(255, 69, 58));
    private static final Color HOVER_INFO_BG = new JBColor(new Color(255, 255, 255, 220), new Color(50, 50, 50, 220));
    private static final Color HOVER_INFO_TEXT = JBColor.foreground();
    
    // Chart data
    private List<MemorySnapshot> snapshots = new ArrayList<>();
    private int minutesBack = 15; // Default to last 15 minutes
    private int chartType = 0; // 0=Used Memory, 1=Memory Usage %, 2=Free Memory, 3=All
    
    // Hover data
    private Point hoverPoint = null;
    private int hoverIndex = -1;
    
    // Cached chart data for performance
    private transient List<Point2D.Double> usedMemoryPoints = new ArrayList<>();
    private transient List<Point2D.Double> freeMemoryPoints = new ArrayList<>();
    private transient List<Point2D.Double> totalMemoryPoints = new ArrayList<>();
    private transient List<Point2D.Double> usagePercentPoints = new ArrayList<>();
    
    private transient double minValue = 0;
    private transient double maxValue = 100;
    
    // Formatter for timestamps
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Constructor
     */
    public MemoryTrendChart() {
        setPreferredSize(new Dimension(600, 300));
        setBorder(JBUI.Borders.empty(10));
        
        // Add mouse listeners for hover effects
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverPoint(e.getPoint());
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverPoint = null;
                hoverIndex = -1;
                repaint();
            }
        });
        
        // Initial data load
        refreshData();
    }
    
    /**
     * Set the time range for the chart
     *
     * @param minutesBack Minutes back to show, or -1 for all data
     */
    public void setTimeRange(int minutesBack) {
        this.minutesBack = minutesBack;
        refreshData();
    }
    
    /**
     * Set the chart type
     *
     * @param chartType Chart type index (0=Used Memory, 1=Memory Usage %, 2=Free Memory, 3=All)
     */
    public void setChartType(int chartType) {
        this.chartType = chartType;
        refreshData();
    }
    
    /**
     * Refresh the chart data
     */
    public void refreshData() {
        MemorySnapshotManager snapshotManager = MemorySnapshotManager.getInstance();
        
        // Get all snapshots or snapshots in range
        if (minutesBack <= 0) {
            // Get all snapshots
            snapshots = snapshotManager.getSnapshots();
        } else {
            // Get snapshots in range
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusMinutes(minutesBack);
            snapshots = snapshotManager.getSnapshotsInRange(startTime, endTime);
        }
        
        // Filter out null snapshots
        snapshots = snapshots.stream()
                .filter(s -> s != null && s.getTimestamp() != null)
                .sorted((s1, s2) -> s1.getTimestamp().compareTo(s2.getTimestamp()))
                .collect(Collectors.toList());
        
        // Recalculate chart data
        calculateChartData();
        
        // Update display
        repaint();
    }
    
    /**
     * Calculate chart data points
     */
    private void calculateChartData() {
        // Clear existing data
        usedMemoryPoints.clear();
        freeMemoryPoints.clear();
        totalMemoryPoints.clear();
        usagePercentPoints.clear();
        
        // If no data, return
        if (snapshots.isEmpty()) {
            return;
        }
        
        // Calculate chart dimensions
        int chartWidth = getWidth() - (2 * PADDING);
        int chartHeight = getHeight() - (2 * PADDING);
        
        if (chartWidth <= 0 || chartHeight <= 0) {
            // Not yet sized, use defaults
            chartWidth = 600 - (2 * PADDING);
            chartHeight = 300 - (2 * PADDING);
        }
        
        // Determine min and max values
        long maxMemory = 0;
        long minMemory = Long.MAX_VALUE;
        double maxPercent = 0;
        
        for (MemorySnapshot snapshot : snapshots) {
            long totalMemory = snapshot.getTotalMemory();
            long usedMemory = snapshot.getUsedMemory();
            long freeMemory = snapshot.getFreeMemory();
            double usagePercent = (double) usedMemory / totalMemory * 100.0;
            
            maxMemory = Math.max(maxMemory, Math.max(totalMemory, Math.max(usedMemory, freeMemory)));
            minMemory = Math.min(minMemory, Math.min(totalMemory, Math.min(usedMemory, freeMemory)));
            maxPercent = Math.max(maxPercent, usagePercent);
        }
        
        // Set minValue and maxValue based on chart type
        if (chartType == 1) {
            // Memory usage %
            minValue = 0;
            maxValue = Math.max(100, maxPercent + 5); // Add some padding
        } else {
            // Memory values (bytes)
            minValue = 0; // Always start at 0 for memory values
            maxValue = maxMemory * 1.05; // Add 5% padding
        }
        
        // Get the time range
        LocalDateTime startTime = snapshots.get(0).getTimestamp();
        LocalDateTime endTime = snapshots.get(snapshots.size() - 1).getTimestamp();
        
        // Calculate points
        for (int i = 0; i < snapshots.size(); i++) {
            MemorySnapshot snapshot = snapshots.get(i);
            
            // Calculate x coordinate based on time
            double x = PADDING + ((double) Duration.between(startTime, snapshot.getTimestamp()).toMillis() / 
                                  Duration.between(startTime, endTime).toMillis()) * chartWidth;
            
            // Calculate y coordinates for different metrics
            long totalMemory = snapshot.getTotalMemory();
            long usedMemory = snapshot.getUsedMemory();
            long freeMemory = snapshot.getFreeMemory();
            double usagePercent = (double) usedMemory / totalMemory * 100.0;
            
            double usedMemoryY = PADDING + chartHeight - ((usedMemory - minValue) / (maxValue - minValue) * chartHeight);
            double freeMemoryY = PADDING + chartHeight - ((freeMemory - minValue) / (maxValue - minValue) * chartHeight);
            double totalMemoryY = PADDING + chartHeight - ((totalMemory - minValue) / (maxValue - minValue) * chartHeight);
            double usagePercentY = PADDING + chartHeight - ((usagePercent - (chartType == 1 ? minValue : 0)) / 
                                                           (chartType == 1 ? maxValue : 100) * chartHeight);
            
            // Add points
            usedMemoryPoints.add(new Point2D.Double(x, usedMemoryY));
            freeMemoryPoints.add(new Point2D.Double(x, freeMemoryY));
            totalMemoryPoints.add(new Point2D.Double(x, totalMemoryY));
            usagePercentPoints.add(new Point2D.Double(x, usagePercentY));
        }
    }
    
    /**
     * Update hover point
     *
     * @param mousePoint Mouse position
     */
    private void updateHoverPoint(Point mousePoint) {
        if (snapshots.isEmpty() || usedMemoryPoints.isEmpty()) {
            hoverPoint = null;
            hoverIndex = -1;
            repaint();
            return;
        }
        
        // Find the closest point
        double minDistance = Double.MAX_VALUE;
        int closestIndex = -1;
        Point2D.Double closestPoint = null;
        
        // Use points based on chart type
        List<Point2D.Double> points;
        switch (chartType) {
            case 0: points = usedMemoryPoints; break;
            case 1: points = usagePercentPoints; break;
            case 2: points = freeMemoryPoints; break;
            case 3: points = usedMemoryPoints; // Default to used memory
            default: points = usedMemoryPoints;
        }
        
        for (int i = 0; i < points.size(); i++) {
            Point2D.Double point = points.get(i);
            double distance = point.distance(mousePoint);
            
            if (distance < minDistance && distance < 20) {
                minDistance = distance;
                closestIndex = i;
                closestPoint = point;
            }
        }
        
        if (closestIndex != -1 && closestPoint != null) {
            hoverPoint = new Point((int) closestPoint.x, (int) closestPoint.y);
            hoverIndex = closestIndex;
        } else {
            hoverPoint = null;
            hoverIndex = -1;
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Get dimensions
        int width = getWidth();
        int height = getHeight();
        int chartWidth = width - (2 * PADDING);
        int chartHeight = height - (2 * PADDING);
        
        // Draw chart background
        g2.setColor(getBackground());
        g2.fillRect(0, 0, width, height);
        
        // Draw grid
        drawGrid(g2, chartWidth, chartHeight);
        
        // Draw axes
        drawAxes(g2, chartWidth, chartHeight);
        
        // Draw data
        if (!snapshots.isEmpty()) {
            if (chartWidth > 0 && chartHeight > 0) {
                // If chart data is empty, recalculate
                if (usedMemoryPoints.isEmpty()) {
                    calculateChartData();
                }
                
                // Draw data lines based on chart type
                switch (chartType) {
                    case 0:
                        // Used memory only
                        drawDataLine(g2, usedMemoryPoints, USED_MEMORY_COLOR);
                        break;
                    case 1:
                        // Memory usage percentage
                        drawDataLine(g2, usagePercentPoints, USED_MEMORY_COLOR);
                        
                        // Draw warning threshold at 75%
                        int warningY = PADDING + chartHeight - (int) ((75.0 - minValue) / (maxValue - minValue) * chartHeight);
                        drawThresholdLine(g2, warningY, chartWidth, "75% (Warning)");
                        
                        // Draw critical threshold at 90%
                        int criticalY = PADDING + chartHeight - (int) ((90.0 - minValue) / (maxValue - minValue) * chartHeight);
                        drawThresholdLine(g2, criticalY, chartWidth, "90% (Critical)");
                        break;
                    case 2:
                        // Free memory only
                        drawDataLine(g2, freeMemoryPoints, FREE_MEMORY_COLOR);
                        break;
                    case 3:
                        // All memory types
                        drawDataLine(g2, totalMemoryPoints, TOTAL_MEMORY_COLOR);
                        drawDataLine(g2, usedMemoryPoints, USED_MEMORY_COLOR);
                        drawDataLine(g2, freeMemoryPoints, FREE_MEMORY_COLOR);
                        break;
                }
                
                // Draw hover info
                if (hoverPoint != null && hoverIndex >= 0 && hoverIndex < snapshots.size()) {
                    drawHoverInfo(g2, snapshots.get(hoverIndex), hoverPoint);
                }
            }
        } else {
            // Draw "No data" message
            g2.setColor(JBColor.foreground());
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            String noDataMessage = "No memory data available";
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(noDataMessage);
            int textHeight = fm.getHeight();
            g2.drawString(noDataMessage, (width - textWidth) / 2, (height - textHeight) / 2 + fm.getAscent());
        }
        
        g2.dispose();
    }
    
    /**
     * Draw a data line
     *
     * @param g2 Graphics context
     * @param points Data points
     * @param color Line color
     */
    private void drawDataLine(Graphics2D g2, List<Point2D.Double> points, Color color) {
        if (points.size() < 2) {
            return;
        }
        
        // Draw line
        g2.setColor(color);
        g2.setStroke(DATA_STROKE);
        
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points.get(0).x, points.get(0).y);
        
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }
        
        g2.draw(path);
        
        // Draw points
        for (Point2D.Double point : points) {
            g2.fillOval((int) point.x - POINT_SIZE / 2, (int) point.y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
        }
    }
    
    /**
     * Draw threshold line
     *
     * @param g2 Graphics context
     * @param y Y coordinate
     * @param width Chart width
     * @param label Threshold label
     */
    private void drawThresholdLine(Graphics2D g2, int y, int width, String label) {
        g2.setColor(THRESHOLD_COLOR);
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));
        g2.drawLine(PADDING, y, PADDING + width, y);
        
        // Draw label
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, PADDING, y - fm.getDescent());
    }
    
    /**
     * Draw grid
     *
     * @param g2 Graphics context
     * @param chartWidth Chart width
     * @param chartHeight Chart height
     */
    private void drawGrid(Graphics2D g2, int chartWidth, int chartHeight) {
        g2.setColor(GRID_COLOR);
        g2.setStroke(GRID_STROKE);
        
        // Draw vertical grid lines
        for (int i = 0; i <= 10; i++) {
            int x = PADDING + (i * chartWidth / 10);
            g2.drawLine(x, PADDING, x, PADDING + chartHeight);
        }
        
        // Draw horizontal grid lines
        for (int i = 0; i <= 10; i++) {
            int y = PADDING + (i * chartHeight / 10);
            g2.drawLine(PADDING, y, PADDING + chartWidth, y);
        }
    }
    
    /**
     * Draw axes
     *
     * @param g2 Graphics context
     * @param chartWidth Chart width
     * @param chartHeight Chart height
     */
    private void drawAxes(Graphics2D g2, int chartWidth, int chartHeight) {
        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.0f));
        
        // Draw axes
        g2.drawLine(PADDING, PADDING, PADDING, PADDING + chartHeight);
        g2.drawLine(PADDING, PADDING + chartHeight, PADDING + chartWidth, PADDING + chartHeight);
        
        // Draw x-axis labels (time)
        if (!snapshots.isEmpty()) {
            // Divide into 5 labels
            for (int i = 0; i <= 5; i++) {
                int index = i * (snapshots.size() - 1) / 5;
                if (index < snapshots.size()) {
                    MemorySnapshot snapshot = snapshots.get(index);
                    String timeLabel = snapshot.getTimestamp().format(TIME_FORMATTER);
                    
                    int x = PADDING + (i * chartWidth / 5);
                    
                    FontMetrics fm = g2.getFontMetrics();
                    int labelWidth = fm.stringWidth(timeLabel);
                    
                    g2.drawString(timeLabel, x - labelWidth / 2, PADDING + chartHeight + AXIS_LABEL_PADDING + fm.getAscent());
                }
            }
        }
        
        // Draw y-axis labels (memory or percentage)
        for (int i = 0; i <= 5; i++) {
            double value = minValue + (i * (maxValue - minValue) / 5);
            String label;
            
            if (chartType == 1) {
                // Memory percentage
                label = String.format("%.1f%%", value);
            } else {
                // Memory size
                label = MemoryUtils.formatMemorySize((long) value);
            }
            
            int y = PADDING + chartHeight - (i * chartHeight / 5);
            
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            
            g2.drawString(label, PADDING - labelWidth - AXIS_LABEL_PADDING, y + fm.getAscent() / 2);
        }
    }
    
    /**
     * Draw hover information
     *
     * @param g2 Graphics context
     * @param snapshot Memory snapshot
     * @param point Hover point
     */
    private void drawHoverInfo(Graphics2D g2, MemorySnapshot snapshot, Point point) {
        // Draw hover point
        g2.setColor(JBColor.RED);
        g2.fillOval(point.x - HOVER_POINT_SIZE / 2, point.y - HOVER_POINT_SIZE / 2, 
                HOVER_POINT_SIZE, HOVER_POINT_SIZE);
        
        // Prepare hover info content
        String timestamp = snapshot.getTimestamp().format(DATE_TIME_FORMATTER);
        String totalMemory = "Total: " + MemoryUtils.formatMemorySize(snapshot.getTotalMemory());
        String usedMemory = "Used: " + MemoryUtils.formatMemorySize(snapshot.getUsedMemory());
        String freeMemory = "Free: " + MemoryUtils.formatMemorySize(snapshot.getFreeMemory());
        String usagePercent = String.format("Usage: %.1f%%", 
                (double) snapshot.getUsedMemory() / snapshot.getTotalMemory() * 100.0);
        
        // Calculate info box dimensions
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        int padding = 8;
        int infoWidth = Math.max(
                Math.max(fm.stringWidth(timestamp), fm.stringWidth(totalMemory)),
                Math.max(
                        Math.max(fm.stringWidth(usedMemory), fm.stringWidth(freeMemory)),
                        fm.stringWidth(usagePercent)
                )
        ) + (2 * padding);
        int infoHeight = (5 * lineHeight) + (2 * padding);
        
        // Calculate info box position
        int infoX = point.x + 15;
        int infoY = point.y - infoHeight / 2;
        
        // Adjust if off screen
        if (infoX + infoWidth > getWidth()) {
            infoX = point.x - infoWidth - 15;
        }
        if (infoY < 0) {
            infoY = 0;
        } else if (infoY + infoHeight > getHeight()) {
            infoY = getHeight() - infoHeight;
        }
        
        // Draw info background
        g2.setColor(HOVER_INFO_BG);
        g2.fillRoundRect(infoX, infoY, infoWidth, infoHeight, 10, 10);
        
        // Draw info border
        g2.setColor(JBColor.border());
        g2.drawRoundRect(infoX, infoY, infoWidth, infoHeight, 10, 10);
        
        // Draw info text
        g2.setColor(HOVER_INFO_TEXT);
        int textY = infoY + padding + fm.getAscent();
        g2.drawString(timestamp, infoX + padding, textY);
        textY += lineHeight;
        g2.drawString(totalMemory, infoX + padding, textY);
        textY += lineHeight;
        g2.drawString(usedMemory, infoX + padding, textY);
        textY += lineHeight;
        g2.drawString(freeMemory, infoX + padding, textY);
        textY += lineHeight;
        g2.drawString(usagePercent, infoX + padding, textY);
    }
}