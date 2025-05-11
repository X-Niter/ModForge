package com.modforge.intellij.plugin.memory.visualization;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.monitoring.MemoryHealthMonitor;
import com.modforge.intellij.plugin.memory.monitoring.MemorySnapshot;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Memory visualization panel
 * Displays memory usage trends over time with predictive analysis
 */
public class MemoryVisualizationPanel extends JBPanel<MemoryVisualizationPanel> {
    private static final Logger LOG = Logger.getInstance(MemoryVisualizationPanel.class);
    private static final int DEFAULT_TIME_RANGE_MINUTES = 30;
    private static final int UPDATE_INTERVAL_MS = 5000;
    private static final int MAX_DATA_POINTS = 1000;
    private static final int POINT_RADIUS = 2;
    private static final DecimalFormat MEMORY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final Project project;
    private final MemoryHealthMonitor healthMonitor;
    private final List<MemorySnapshot> memoryHistory = new CopyOnWriteArrayList<>();
    private final MemoryChartPanel chartPanel;
    private final JBLabel statusLabel;
    private final JComboBox<String> timeRangeComboBox;
    private final JComboBox<String> metricTypeComboBox;
    private final JCheckBox showPredictionCheckBox;
    private final JButton refreshButton;
    private final JButton exportButton;
    
    private Timer updateTimer;
    private int timeRangeMinutes = DEFAULT_TIME_RANGE_MINUTES;
    private boolean showPrediction = true;
    private String selectedMetric = "Used Memory (%)";
    
    /**
     * Create a memory visualization panel
     * 
     * @param project The current project
     */
    public MemoryVisualizationPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.healthMonitor = MemoryHealthMonitor.getInstance();
        
        // Chart panel
        chartPanel = new MemoryChartPanel();
        chartPanel.setBackground(UIUtil.getPanelBackground());
        
        // Controls panel
        JPanel controlsPanel = createControlsPanel();
        
        // Status label
        statusLabel = new JBLabel("Initializing memory visualization...");
        statusLabel.setBorder(JBUI.Borders.empty(5, 10));
        
        // Layout components
        add(controlsPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        
        // Initialize the timer to update data periodically
        initializeUpdateTimer();
        
        // Update the visualization initially
        updateVisualization();
    }
    
    /**
     * Create the controls panel
     * 
     * @return The controls panel
     */
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(JBUI.Borders.empty(5));
        
        // Time range combo box
        panel.add(new JBLabel("Time Range:"));
        timeRangeComboBox = new ComboBox<>(new String[]{"5 min", "15 min", "30 min", "1 hour", "3 hours", "6 hours", "12 hours", "24 hours"});
        timeRangeComboBox.setSelectedIndex(2); // Default: 30 min
        timeRangeComboBox.addActionListener(e -> {
            String selected = (String) timeRangeComboBox.getSelectedItem();
            if (selected != null) {
                if (selected.contains("min")) {
                    timeRangeMinutes = Integer.parseInt(selected.split(" ")[0]);
                } else if (selected.contains("hour")) {
                    int hours = Integer.parseInt(selected.split(" ")[0]);
                    timeRangeMinutes = hours * 60;
                }
                updateVisualization();
            }
        });
        panel.add(timeRangeComboBox);
        
        // Metric type combo box
        panel.add(new JBLabel("  Metric:"));
        metricTypeComboBox = new ComboBox<>(new String[]{"Used Memory (%)", "Used Memory (MB)", "Available Memory (MB)", "Total Memory (MB)"});
        metricTypeComboBox.addActionListener(e -> {
            selectedMetric = (String) metricTypeComboBox.getSelectedItem();
            updateVisualization();
        });
        panel.add(metricTypeComboBox);
        
        // Show prediction checkbox
        showPredictionCheckBox = new JCheckBox("Show Prediction", true);
        showPredictionCheckBox.addActionListener(e -> {
            showPrediction = showPredictionCheckBox.isSelected();
            updateVisualization();
        });
        panel.add(showPredictionCheckBox);
        
        // Refresh button
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> updateVisualization());
        panel.add(refreshButton);
        
        // Export button
        exportButton = new JButton("Export Data");
        exportButton.addActionListener(e -> exportMemoryData());
        panel.add(exportButton);
        
        return panel;
    }
    
    /**
     * Initialize the update timer
     */
    private void initializeUpdateTimer() {
        updateTimer = new Timer(UPDATE_INTERVAL_MS, e -> updateVisualization());
        updateTimer.setRepeats(true);
        updateTimer.start();
    }
    
    /**
     * Update the visualization with current memory data
     */
    private void updateVisualization() {
        try {
            // Get current memory snapshot
            MemorySnapshot currentSnapshot = MemoryManager.getInstance().getCurrentMemorySnapshot();
            
            // Add to history if it's a new snapshot
            if (!memoryHistory.isEmpty()) {
                MemorySnapshot lastSnapshot = memoryHistory.get(memoryHistory.size() - 1);
                if (currentSnapshot.getTimestamp().isAfter(lastSnapshot.getTimestamp())) {
                    memoryHistory.add(currentSnapshot);
                }
            } else {
                memoryHistory.add(currentSnapshot);
            }
            
            // Limit history size
            if (memoryHistory.size() > MAX_DATA_POINTS) {
                memoryHistory.subList(0, memoryHistory.size() - MAX_DATA_POINTS).clear();
            }
            
            // Filter data based on time range
            List<MemorySnapshot> filteredData = filterDataByTimeRange(memoryHistory, timeRangeMinutes);
            
            // Update chart
            chartPanel.updateData(filteredData, showPrediction, selectedMetric);
            
            // Update status label
            updateStatusLabel(currentSnapshot);
            
        } catch (Exception ex) {
            LOG.error("Error updating memory visualization", ex);
            statusLabel.setText("Error updating visualization: " + ex.getMessage());
        }
    }
    
    /**
     * Update the status label with current memory information
     * 
     * @param snapshot The current memory snapshot
     */
    private void updateStatusLabel(MemorySnapshot snapshot) {
        double usedMemoryPercent = snapshot.getUsedMemoryPercent();
        double usedMemoryMB = snapshot.getUsedMemoryMB();
        double availableMemoryMB = snapshot.getAvailableMemoryMB();
        double totalMemoryMB = snapshot.getTotalMemoryMB();
        
        StringBuilder status = new StringBuilder();
        status.append("Current Memory: ")
              .append(MEMORY_FORMAT.format(usedMemoryMB))
              .append(" MB used (")
              .append(MEMORY_FORMAT.format(usedMemoryPercent))
              .append("%), ")
              .append(MEMORY_FORMAT.format(availableMemoryMB))
              .append(" MB available of ")
              .append(MEMORY_FORMAT.format(totalMemoryMB))
              .append(" MB total");
        
        if (healthMonitor != null) {
            MemoryHealthMonitor.MemoryHealthStatus healthStatus = healthMonitor.getCurrentHealthStatus();
            status.append(" | Health Status: ").append(healthStatus.name());
        }
        
        statusLabel.setText(status.toString());
    }
    
    /**
     * Filter data by time range
     * 
     * @param data The full data history
     * @param rangeMinutes The time range in minutes
     * @return The filtered data
     */
    private List<MemorySnapshot> filterDataByTimeRange(List<MemorySnapshot> data, int rangeMinutes) {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(rangeMinutes);
        return data.stream()
                .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoffTime))
                .toList();
    }
    
    /**
     * Export memory data to a CSV file
     */
    private void exportMemoryData() {
        try {
            // Create file chooser and get save location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Memory Data");
            fileChooser.setSelectedFile(new java.io.File("memory_data_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
            
            int result = fileChooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            java.io.File file = fileChooser.getSelectedFile();
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                // Write header
                writer.println("Timestamp,Used Memory (%),Used Memory (MB),Available Memory (MB),Total Memory (MB)");
                
                // Write data
                for (MemorySnapshot snapshot : memoryHistory) {
                    writer.println(
                            snapshot.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "," +
                            snapshot.getUsedMemoryPercent() + "," +
                            snapshot.getUsedMemoryMB() + "," +
                            snapshot.getAvailableMemoryMB() + "," +
                            snapshot.getTotalMemoryMB()
                    );
                }
            }
            
            statusLabel.setText("Memory data exported to " + file.getAbsolutePath());
            
        } catch (Exception ex) {
            LOG.error("Error exporting memory data", ex);
            statusLabel.setText("Error exporting memory data: " + ex.getMessage());
        }
    }
    
    /**
     * Dispose the panel and release resources
     */
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
        memoryHistory.clear();
    }
    
    /**
     * Memory chart panel that draws the chart
     */
    private class MemoryChartPanel extends JPanel {
        private static final int PADDING = 40;
        private static final int X_AXIS_TICK_COUNT = 6;
        private static final int Y_AXIS_TICK_COUNT = 5;
        
        private List<MemorySnapshot> data = Collections.emptyList();
        private boolean showPrediction = true;
        private String metricType = "Used Memory (%)";
        private final Stroke lineStroke = new BasicStroke(2.0f);
        private final Stroke predictionStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f);
        private final Stroke gridStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{1.0f}, 0.0f);
        
        private final Color chartLineColor = new JBColor(new Color(44, 102, 230), new Color(72, 125, 255));
        private final Color predictionLineColor = new JBColor(new Color(255, 100, 100), new Color(255, 120, 120));
        private final Color gridColor = new JBColor(new Color(200, 200, 200), new Color(100, 100, 100));
        private final Color textColor = UIUtil.getLabelForeground();
        private final Color gradientStartColor = new JBColor(new Color(44, 102, 230, 40), new Color(72, 125, 255, 40));
        private final Color gradientEndColor = new JBColor(new Color(44, 102, 230, 5), new Color(72, 125, 255, 5));
        
        /**
         * Create a memory chart panel
         */
        public MemoryChartPanel() {
            setBorder(JBUI.Borders.empty(10));
            setMinimumSize(new Dimension(300, 200));
            setPreferredSize(new Dimension(800, 400));
            
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    repaint();
                }
            });
        }
        
        /**
         * Update the chart data
         * 
         * @param newData The new data
         * @param showPrediction Whether to show prediction
         * @param metricType The metric type to display
         */
        public void updateData(List<MemorySnapshot> newData, boolean showPrediction, String metricType) {
            this.data = new ArrayList<>(newData);
            this.showPrediction = showPrediction;
            this.metricType = metricType;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (data.isEmpty()) {
                drawEmptyState(g);
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int chartWidth = width - 2 * PADDING;
            int chartHeight = height - 2 * PADDING;
            
            // Get min/max values based on metric type
            double[] valueRange = getValueRange();
            double minValue = valueRange[0];
            double maxValue = valueRange[1];
            
            // Draw axes and grid
            drawAxes(g2d, width, height, chartWidth, chartHeight, minValue, maxValue);
            
            // Draw data line
            drawDataLine(g2d, chartWidth, chartHeight, minValue, maxValue);
            
            // Draw prediction line if needed
            if (showPrediction && data.size() >= 2) {
                drawPredictionLine(g2d, chartWidth, chartHeight, minValue, maxValue);
            }
            
            g2d.dispose();
        }
        
        /**
         * Draw the empty state
         * 
         * @param g The graphics context
         */
        private void drawEmptyState(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            String message = "No memory data available yet";
            FontMetrics metrics = g2d.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(message)) / 2;
            int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
            
            g2d.setColor(textColor);
            g2d.drawString(message, x, y);
            
            g2d.dispose();
        }
        
        /**
         * Draw the axes and grid
         * 
         * @param g2d The graphics context
         * @param width The total width
         * @param height The total height
         * @param chartWidth The chart width
         * @param chartHeight The chart height
         * @param minValue The minimum value
         * @param maxValue The maximum value
         */
        private void drawAxes(Graphics2D g2d, int width, int height, int chartWidth, int chartHeight, double minValue, double maxValue) {
            g2d.setColor(textColor);
            
            // Draw X axis
            g2d.drawLine(PADDING, height - PADDING, width - PADDING, height - PADDING);
            
            // Draw Y axis
            g2d.drawLine(PADDING, PADDING, PADDING, height - PADDING);
            
            // Draw X axis ticks and labels
            double xTickStep = (double) chartWidth / (X_AXIS_TICK_COUNT - 1);
            for (int i = 0; i < X_AXIS_TICK_COUNT; i++) {
                int x = PADDING + (int) (i * xTickStep);
                
                // Draw tick
                g2d.drawLine(x, height - PADDING, x, height - PADDING + 5);
                
                // Draw grid line
                g2d.setColor(gridColor);
                g2d.setStroke(gridStroke);
                g2d.drawLine(x, PADDING, x, height - PADDING);
                g2d.setStroke(new BasicStroke());
                g2d.setColor(textColor);
                
                // Draw label
                if (data.size() > 0) {
                    LocalDateTime time;
                    if (i == 0) {
                        time = data.get(0).getTimestamp();
                    } else if (i == X_AXIS_TICK_COUNT - 1) {
                        time = data.get(data.size() - 1).getTimestamp();
                    } else {
                        int index = (int) (i * (data.size() - 1) / (X_AXIS_TICK_COUNT - 1.0));
                        time = data.get(index).getTimestamp();
                    }
                    
                    String label = time.format(TIME_FORMATTER);
                    FontMetrics metrics = g2d.getFontMetrics();
                    int labelWidth = metrics.stringWidth(label);
                    g2d.drawString(label, x - labelWidth / 2, height - PADDING + 20);
                }
            }
            
            // Draw Y axis ticks and labels
            double valueRange = maxValue - minValue;
            double yTickStep = (double) chartHeight / (Y_AXIS_TICK_COUNT - 1);
            
            for (int i = 0; i < Y_AXIS_TICK_COUNT; i++) {
                int y = height - PADDING - (int) (i * yTickStep);
                
                // Draw tick
                g2d.drawLine(PADDING, y, PADDING - 5, y);
                
                // Draw grid line
                g2d.setColor(gridColor);
                g2d.setStroke(gridStroke);
                g2d.drawLine(PADDING, y, width - PADDING, y);
                g2d.setStroke(new BasicStroke());
                g2d.setColor(textColor);
                
                // Draw label
                double value = minValue + (i * valueRange / (Y_AXIS_TICK_COUNT - 1));
                String label = formatValue(value);
                FontMetrics metrics = g2d.getFontMetrics();
                int labelWidth = metrics.stringWidth(label);
                g2d.drawString(label, PADDING - 10 - labelWidth, y + metrics.getAscent() / 2);
            }
            
            // Draw Y axis title
            String yAxisTitle = metricType;
            g2d.translate(15, height / 2);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString(yAxisTitle, 0, 0);
            g2d.rotate(Math.PI / 2);
            g2d.translate(-15, -height / 2);
        }
        
        /**
         * Draw the data line
         * 
         * @param g2d The graphics context
         * @param chartWidth The chart width
         * @param chartHeight The chart height
         * @param minValue The minimum value
         * @param maxValue The maximum value
         */
        private void drawDataLine(Graphics2D g2d, int chartWidth, int chartHeight, double minValue, double maxValue) {
            if (data.isEmpty()) {
                return;
            }
            
            // Create path for the line
            Path2D path = new Path2D.Double();
            Path2D fillPath = new Path2D.Double();
            
            // Draw line
            boolean firstPoint = true;
            double valueRange = maxValue - minValue;
            
            for (int i = 0; i < data.size(); i++) {
                MemorySnapshot snapshot = data.get(i);
                double value = getValueFromSnapshot(snapshot);
                
                // Calculate point coordinates
                double xRatio = (double) i / (data.size() - 1);
                double yRatio = (value - minValue) / valueRange;
                
                int x = PADDING + (int) (xRatio * chartWidth);
                int y = getHeight() - PADDING - (int) (yRatio * chartHeight);
                
                if (firstPoint) {
                    path.moveTo(x, y);
                    fillPath.moveTo(x, getHeight() - PADDING);
                    fillPath.lineTo(x, y);
                    firstPoint = false;
                } else {
                    path.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
            }
            
            // Complete the fill path
            fillPath.lineTo(PADDING + chartWidth, getHeight() - PADDING);
            fillPath.lineTo(PADDING, getHeight() - PADDING);
            fillPath.closePath();
            
            // Draw gradient fill
            GradientPaint gradient = new GradientPaint(
                    0, PADDING, gradientStartColor,
                    0, getHeight() - PADDING, gradientEndColor);
            g2d.setPaint(gradient);
            g2d.fill(fillPath);
            
            // Draw line
            g2d.setColor(chartLineColor);
            g2d.setStroke(lineStroke);
            g2d.draw(path);
            
            // Draw data points
            for (int i = 0; i < data.size(); i++) {
                MemorySnapshot snapshot = data.get(i);
                double value = getValueFromSnapshot(snapshot);
                
                // Calculate point coordinates
                double xRatio = (double) i / (data.size() - 1);
                double yRatio = (value - minValue) / valueRange;
                
                int x = PADDING + (int) (xRatio * chartWidth);
                int y = getHeight() - PADDING - (int) (yRatio * chartHeight);
                
                g2d.setColor(chartLineColor);
                g2d.fillOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
                g2d.setColor(UIUtil.getPanelBackground());
                g2d.drawOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
            }
        }
        
        /**
         * Draw the prediction line
         * 
         * @param g2d The graphics context
         * @param chartWidth The chart width
         * @param chartHeight The chart height
         * @param minValue The minimum value
         * @param maxValue The maximum value
         */
        private void drawPredictionLine(Graphics2D g2d, int chartWidth, int chartHeight, double minValue, double maxValue) {
            if (data.size() < 2) {
                return;
            }
            
            try {
                // Calculate linear regression
                double[] coefficients = calculateLinearRegression();
                double slope = coefficients[0];
                double intercept = coefficients[1];
                
                // Create prediction path
                Path2D path = new Path2D.Double();
                
                // Start from the last data point
                MemorySnapshot lastSnapshot = data.get(data.size() - 1);
                double lastValue = getValueFromSnapshot(lastSnapshot);
                double valueRange = maxValue - minValue;
                
                // Calculate point coordinates for last data point
                double xRatio = 1.0;
                double yRatio = (lastValue - minValue) / valueRange;
                
                int x = PADDING + (int) (xRatio * chartWidth);
                int y = getHeight() - PADDING - (int) (yRatio * chartHeight);
                
                path.moveTo(x, y);
                
                // Add prediction points (for next 30% of the time range)
                int predictionPoints = 10;
                double timeDiff = 0;
                
                // Calculate average time difference between snapshots
                if (data.size() >= 2) {
                    LocalDateTime firstTime = data.get(0).getTimestamp();
                    LocalDateTime lastTime = data.get(data.size() - 1).getTimestamp();
                    timeDiff = java.time.Duration.between(firstTime, lastTime).getSeconds() / (double) (data.size() - 1);
                }
                
                // Draw prediction line
                for (int i = 1; i <= predictionPoints; i++) {
                    // Prediction value based on linear regression
                    double timeOffset = data.size() - 1 + i * (chartWidth * 0.3 / predictionPoints) / (chartWidth / (data.size() - 1));
                    double predictedValue = slope * timeOffset + intercept;
                    
                    // Ensure prediction stays within reasonable bounds
                    predictedValue = Math.max(minValue, Math.min(maxValue, predictedValue));
                    
                    // Calculate point coordinates
                    xRatio = 1.0 + (i * 0.3 / predictionPoints);
                    xRatio = Math.min(xRatio, 1.3); // Limit to 30% extra
                    
                    yRatio = (predictedValue - minValue) / valueRange;
                    
                    x = PADDING + (int) (xRatio * chartWidth);
                    y = getHeight() - PADDING - (int) (yRatio * chartHeight);
                    
                    path.lineTo(x, y);
                }
                
                // Draw prediction line
                g2d.setColor(predictionLineColor);
                g2d.setStroke(predictionStroke);
                g2d.draw(path);
                
            } catch (Exception ex) {
                LOG.warn("Failed to draw prediction line", ex);
            }
        }
        
        /**
         * Calculate linear regression coefficients
         * 
         * @return The slope and intercept
         */
        private double[] calculateLinearRegression() {
            // Simple linear regression: y = mx + b
            int n = data.size();
            double sumX = 0;
            double sumY = 0;
            double sumXY = 0;
            double sumXX = 0;
            
            for (int i = 0; i < n; i++) {
                double x = i;
                double y = getValueFromSnapshot(data.get(i));
                
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumXX += x * x;
            }
            
            double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            
            return new double[]{slope, intercept};
        }
        
        /**
         * Get the value range based on the selected metric
         * 
         * @return The minimum and maximum values
         */
        private double[] getValueRange() {
            if (data.isEmpty()) {
                return new double[]{0, 100};
            }
            
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            
            for (MemorySnapshot snapshot : data) {
                double value = getValueFromSnapshot(snapshot);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            
            // Add some padding to the range
            double padding = (max - min) * 0.1;
            min = Math.max(0, min - padding);
            max = max + padding;
            
            // Special case for percentages
            if (metricType.contains("%")) {
                min = Math.max(0, min);
                max = Math.min(100, max);
                
                // If the range is too small, expand it a bit
                if (max - min < 10) {
                    min = Math.max(0, min - 5);
                    max = Math.min(100, max + 5);
                }
            }
            
            // If min and max are the same, add some padding
            if (Math.abs(max - min) < 0.001) {
                min = Math.max(0, min - 1);
                max = max + 1;
            }
            
            return new double[]{min, max};
        }
        
        /**
         * Get the value from a snapshot based on the selected metric
         * 
         * @param snapshot The memory snapshot
         * @return The value
         */
        private double getValueFromSnapshot(MemorySnapshot snapshot) {
            return switch (metricType) {
                case "Used Memory (%)" -> snapshot.getUsedMemoryPercent();
                case "Used Memory (MB)" -> snapshot.getUsedMemoryMB();
                case "Available Memory (MB)" -> snapshot.getAvailableMemoryMB();
                case "Total Memory (MB)" -> snapshot.getTotalMemoryMB();
                default -> snapshot.getUsedMemoryPercent();
            };
        }
        
        /**
         * Format a value based on the selected metric
         * 
         * @param value The value
         * @return The formatted value
         */
        private String formatValue(double value) {
            if (metricType.contains("%")) {
                return MEMORY_FORMAT.format(value) + "%";
            } else {
                return MEMORY_FORMAT.format(value) + " MB";
            }
        }
    }
}