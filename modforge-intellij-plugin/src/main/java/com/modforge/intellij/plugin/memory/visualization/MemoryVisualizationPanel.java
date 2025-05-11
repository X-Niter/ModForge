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
            // Check if we have a valid memory manager
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager == null) {
                LOG.warn("Memory manager is not available, skipping visualization update");
                return;
            }
            
            // Get current memory snapshot
            MemorySnapshot currentSnapshot = memoryManager.getCurrentMemorySnapshot();
            if (currentSnapshot == null) {
                LOG.warn("Current memory snapshot is null, skipping visualization update");
                return;
            }
            
            // Thread-safe update of memory history using synchronization
            synchronized (memoryHistory) {
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
                
                // Make a thread-safe copy of the current state for filtering and display
                final List<MemorySnapshot> memoryCopy = new ArrayList<>(memoryHistory);
                final int currentTimeRange = timeRangeMinutes;
                final boolean currentShowPrediction = showPrediction;
                final String currentMetric = selectedMetric;
                
                // Filter data based on time range
                List<MemorySnapshot> filteredData = filterDataByTimeRange(memoryCopy, currentTimeRange);
                
                // Update chart in a thread-safe manner
                chartPanel.updateData(filteredData, currentShowPrediction, currentMetric);
                
                // Update status label in the EDT
                final MemorySnapshot finalSnapshot = currentSnapshot;
                SwingUtilities.invokeLater(() -> {
                    try {
                        updateStatusLabel(finalSnapshot);
                    } catch (Exception ex) {
                        LOG.error("Error updating status label", ex);
                    }
                });
            }
        } catch (Exception ex) {
            LOG.error("Error updating memory visualization", ex);
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error updating visualization: " + ex.getMessage());
            });
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
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Ensure range is positive
            int sanitizedRange = Math.max(1, rangeMinutes);
            
            // Get current time and calculate cutoff
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoffTime = now.minusMinutes(sanitizedRange);
            
            // Filter data
            return data.stream()
                    .filter(snapshot -> snapshot != null && snapshot.getTimestamp() != null)
                    .filter(snapshot -> {
                        try {
                            return snapshot.getTimestamp().isAfter(cutoffTime);
                        } catch (Exception ex) {
                            LOG.debug("Error comparing timestamps in filter", ex);
                            return false;
                        }
                    })
                    .toList();
                    
        } catch (Exception ex) {
            LOG.error("Error filtering data by time range", ex);
            return Collections.emptyList();
        }
    }
    
    /**
     * Export memory data to a CSV file
     */
    private void exportMemoryData() {
        // Check if we have data to export
        if (memoryHistory == null || memoryHistory.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("No memory data available to export");
            });
            return;
        }
        
        try {
            // Take a thread-safe snapshot of the data
            final List<MemorySnapshot> dataToExport;
            synchronized (memoryHistory) {
                dataToExport = new ArrayList<>(memoryHistory);
            }
            
            // Create file chooser and get save location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Memory Data");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            
            // Set default filename with timestamp
            String defaultFilename = "memory_data_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            fileChooser.setSelectedFile(new java.io.File(defaultFilename));
            
            // Show save dialog
            int result = fileChooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                LOG.info("User canceled memory data export");
                return;
            }
            
            // Get selected file
            java.io.File file = fileChooser.getSelectedFile();
            
            // Add .csv extension if missing
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new java.io.File(file.getAbsolutePath() + ".csv");
            }
            
            // Check if file already exists
            if (file.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                        "File already exists. Overwrite?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                        
                if (overwrite != JOptionPane.YES_OPTION) {
                    LOG.info("User canceled overwriting existing file: " + file.getAbsolutePath());
                    return;
                }
            }
            
            // Write data to file
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                // Write header
                writer.println("Timestamp,Used Memory (%),Used Memory (MB),Available Memory (MB),Total Memory (MB)");
                
                // Create date-time formatter for consistent output
                DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                
                // Write data
                for (MemorySnapshot snapshot : dataToExport) {
                    if (snapshot != null && snapshot.getTimestamp() != null) {
                        try {
                            writer.println(
                                    snapshot.getTimestamp().format(dtf) + "," +
                                    String.format("%.2f", snapshot.getUsedMemoryPercent()) + "," +
                                    String.format("%.2f", snapshot.getUsedMemoryMB()) + "," +
                                    String.format("%.2f", snapshot.getAvailableMemoryMB()) + "," +
                                    String.format("%.2f", snapshot.getTotalMemoryMB())
                            );
                        } catch (Exception ex) {
                            LOG.warn("Error writing snapshot to CSV: " + ex.getMessage());
                            // Continue with next snapshot
                        }
                    }
                }
            }
            
            final String finalPath = file.getAbsolutePath();
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Memory data exported to " + finalPath);
                
                // Show success notification
                JOptionPane.showMessageDialog(this,
                        "Memory data successfully exported to:\n" + finalPath,
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            });
            
            LOG.info("Memory data exported to " + file.getAbsolutePath() + 
                    " (" + dataToExport.size() + " records)");
            
        } catch (Exception ex) {
            LOG.error("Error exporting memory data", ex);
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error exporting memory data: " + ex.getMessage());
                
                // Show error notification
                JOptionPane.showMessageDialog(this,
                        "Failed to export memory data:\n" + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    /**
     * Dispose the panel and release resources
     */
    public void dispose() {
        try {
            if (updateTimer != null) {
                updateTimer.stop();
                updateTimer = null;
            }
            
            // Clear data structures
            if (memoryHistory != null) {
                memoryHistory.clear();
            }
            
            // Remove listeners from components
            if (timeRangeComboBox != null) {
                for (ActionListener listener : timeRangeComboBox.getActionListeners()) {
                    timeRangeComboBox.removeActionListener(listener);
                }
            }
            
            if (metricTypeComboBox != null) {
                for (ActionListener listener : metricTypeComboBox.getActionListeners()) {
                    metricTypeComboBox.removeActionListener(listener);
                }
            }
            
            if (showPredictionCheckBox != null) {
                for (ActionListener listener : showPredictionCheckBox.getActionListeners()) {
                    showPredictionCheckBox.removeActionListener(listener);
                }
            }
            
            if (refreshButton != null) {
                for (ActionListener listener : refreshButton.getActionListeners()) {
                    refreshButton.removeActionListener(listener);
                }
            }
            
            if (exportButton != null) {
                for (ActionListener listener : exportButton.getActionListeners()) {
                    exportButton.removeActionListener(listener);
                }
            }
        } catch (Exception ex) {
            LOG.error("Error disposing memory visualization panel", ex);
        }
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
            // Thread-safe update by using a local copy
            if (newData != null) {
                final List<MemorySnapshot> dataCopy = new ArrayList<>(newData);
                
                // Use SwingUtilities.invokeLater for thread safety when updating the UI
                SwingUtilities.invokeLater(() -> {
                    this.data = dataCopy;
                    this.showPrediction = showPrediction;
                    this.metricType = metricType != null ? metricType : "Used Memory (%)";
                    repaint();
                });
            } else {
                LOG.warn("Attempted to update chart with null data");
            }
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
            try {
                // Validate we have enough data points
                if (data == null || data.size() < 2) {
                    LOG.debug("Not enough data points for linear regression");
                    return new double[]{0, 0}; // Default no-change prediction
                }
                
                // Simple linear regression: y = mx + b
                int n = data.size();
                double sumX = 0;
                double sumY = 0;
                double sumXY = 0;
                double sumXX = 0;
                int validPoints = 0;
                
                for (int i = 0; i < n; i++) {
                    try {
                        MemorySnapshot snapshot = data.get(i);
                        if (snapshot == null) {
                            continue;
                        }
                        
                        double x = i;
                        double y = getValueFromSnapshot(snapshot);
                        
                        // Skip extreme outliers that would skew the regression
                        if (Double.isNaN(y) || Double.isInfinite(y)) {
                            continue;
                        }
                        
                        sumX += x;
                        sumY += y;
                        sumXY += x * y;
                        sumXX += x * x;
                        validPoints++;
                    } catch (Exception ex) {
                        LOG.debug("Error processing data point for regression", ex);
                        // Skip this data point
                    }
                }
                
                // Need at least 2 valid points for regression
                if (validPoints < 2) {
                    LOG.debug("Not enough valid data points for linear regression");
                    return new double[]{0, 0}; // Default no-change prediction
                }
                
                // Calculate regression coefficients with division by zero protection
                double denominator = (validPoints * sumXX - sumX * sumX);
                double slope = (Math.abs(denominator) < 0.0001) ? 
                               0 : // Avoid division by zero
                               (validPoints * sumXY - sumX * sumY) / denominator;
                               
                double intercept = (validPoints == 0) ? 
                                   0 : // Avoid division by zero 
                                   (sumY - slope * sumX) / validPoints;
                
                // Apply reasonable limits to avoid extreme projections
                // Limit maximum change rate to 5% per data point
                double maxSlope = 5.0;
                if (Math.abs(slope) > maxSlope) {
                    slope = Math.signum(slope) * maxSlope;
                    LOG.debug("Limiting excessive slope in regression");
                }
                
                return new double[]{slope, intercept};
                
            } catch (Exception ex) {
                LOG.error("Error calculating linear regression", ex);
                return new double[]{0, 0}; // Default no-change prediction in case of error
            }
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
            if (snapshot == null) {
                LOG.warn("Attempted to get value from null snapshot");
                return 0.0; // Default value
            }
            
            try {
                return switch (metricType) {
                    case "Used Memory (%)" -> {
                        double value = snapshot.getUsedMemoryPercent();
                        // Clamp to reasonable range
                        yield Math.max(0.0, Math.min(100.0, value));
                    }
                    case "Used Memory (MB)" -> {
                        double value = snapshot.getUsedMemoryMB();
                        // Ensure non-negative
                        yield Math.max(0.0, value);
                    }
                    case "Available Memory (MB)" -> {
                        double value = snapshot.getAvailableMemoryMB();
                        // Ensure non-negative
                        yield Math.max(0.0, value);
                    }
                    case "Total Memory (MB)" -> {
                        double value = snapshot.getTotalMemoryMB();
                        // Ensure non-negative
                        yield Math.max(0.0, value);
                    }
                    default -> {
                        LOG.debug("Unknown metric type: " + metricType + ", defaulting to Used Memory (%)");
                        // Default to used memory percent as fallback
                        double value = snapshot.getUsedMemoryPercent();
                        // Clamp to reasonable range
                        yield Math.max(0.0, Math.min(100.0, value));
                    }
                };
            } catch (Exception ex) {
                LOG.warn("Error getting value from snapshot for metric: " + metricType, ex);
                return 0.0; // Default value in case of error
            }
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