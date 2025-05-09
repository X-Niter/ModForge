package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.services.AIServiceManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Panel for displaying metrics about AI usage and pattern learning performance.
 */
public class MetricsPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(MetricsPanel.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    
    private JPanel mainPanel;
    private Timer refreshTimer;
    
    // UI components
    private UsageMetricsPanel usageMetricsPanel;
    private PatternLearningPanel patternLearningPanel;
    private PerformanceMetricsPanel performanceMetricsPanel;
    
    /**
     * Creates a new MetricsPanel.
     * @param project The project
     */
    public MetricsPanel(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = project.getService(AIServiceManager.class);
        
        createUI();
        
        // Start refresh timer
        refreshTimer = new Timer(5000, e -> refreshData());
        refreshTimer.start();
        
        // Initial data refresh
        refreshData();
        
        // Register for disposal
        Disposer.register(project, this);
    }

    /**
     * Creates the UI.
     */
    private void createUI() {
        mainPanel = new JBPanel<>(new BorderLayout());
        
        // Create tabbed pane
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Create panels
        usageMetricsPanel = new UsageMetricsPanel();
        patternLearningPanel = new PatternLearningPanel();
        performanceMetricsPanel = new PerformanceMetricsPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Usage Metrics", usageMetricsPanel);
        tabbedPane.addTab("Pattern Learning", patternLearningPanel);
        tabbedPane.addTab("Performance", performanceMetricsPanel);
        
        // Add tabbed pane to main panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshData());
        
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Refreshes the data displayed in the panels.
     */
    private void refreshData() {
        SwingUtilities.invokeLater(() -> {
            try {
                // In a real implementation, we'd get this data from the AIServiceManager
                // For now, we'll just use sample data
                
                // Usage metrics
                Map<String, Integer> apiCalls = new HashMap<>();
                apiCalls.put("Code Generation", 125);
                apiCalls.put("Code Improvement", 89);
                apiCalls.put("Pattern Learning", 43);
                apiCalls.put("Error Resolution", 67);
                
                usageMetricsPanel.updateData(apiCalls, 324, 178);
                
                // Pattern learning metrics
                Map<String, Double> patternEfficiency = new HashMap<>();
                patternEfficiency.put("Code Generation", 0.68);
                patternEfficiency.put("Code Improvement", 0.42);
                patternEfficiency.put("Error Resolution", 0.57);
                
                patternLearningPanel.updateData(patternEfficiency, 146, 324);
                
                // Performance metrics
                List<Double> responseTimesMs = Arrays.asList(850.0, 920.0, 760.0, 1120.0, 890.0, 820.0, 930.0);
                List<Double> tokensPerRequest = Arrays.asList(320.0, 450.0, 380.0, 290.0, 510.0, 420.0, 370.0);
                
                performanceMetricsPanel.updateData(responseTimesMs, tokensPerRequest);
                
            } catch (Exception e) {
                LOG.error("Error refreshing metrics data", e);
            }
        });
    }
    
    /**
     * Gets the content component.
     * @return The content component
     */
    public JComponent getContent() {
        return mainPanel;
    }
    
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }
    
    /**
     * Panel for displaying API usage metrics.
     */
    private static class UsageMetricsPanel extends JBPanel<UsageMetricsPanel> {
        private PieChartPanel pieChart;
        private JBLabel apiCallsLabel;
        private JBLabel patternMatchesLabel;
        private JBLabel tokensSavedLabel;
        private JBLabel costSavedLabel;
        
        /**
         * Creates a new UsageMetricsPanel.
         */
        public UsageMetricsPanel() {
            super(new BorderLayout());
            createUI();
        }
        
        /**
         * Creates the UI.
         */
        private void createUI() {
            // Create pie chart
            pieChart = new PieChartPanel();
            
            // Create labels panel
            JPanel labelsPanel = new JBPanel<>();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
            labelsPanel.setBorder(JBUI.Borders.empty(10));
            
            apiCallsLabel = new JBLabel("Total API Calls: 0");
            patternMatchesLabel = new JBLabel("Pattern Matches: 0");
            tokensSavedLabel = new JBLabel("Tokens Saved: 0");
            costSavedLabel = new JBLabel("Estimated Cost Saved: $0.00");
            
            JPanel apiCallsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            apiCallsPanel.add(apiCallsLabel);
            labelsPanel.add(apiCallsPanel);
            
            JPanel patternMatchesPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            patternMatchesPanel.add(patternMatchesLabel);
            labelsPanel.add(patternMatchesPanel);
            
            JPanel tokensSavedPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            tokensSavedPanel.add(tokensSavedLabel);
            labelsPanel.add(tokensSavedPanel);
            
            JPanel costSavedPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            costSavedPanel.add(costSavedLabel);
            labelsPanel.add(costSavedPanel);
            
            // Add components to panel
            add(pieChart, BorderLayout.CENTER);
            add(labelsPanel, BorderLayout.SOUTH);
        }
        
        /**
         * Updates the data displayed in the panel.
         * @param apiCalls The API calls by type
         * @param totalTokensSaved The total tokens saved
         * @param patternMatches The number of pattern matches
         */
        public void updateData(Map<String, Integer> apiCalls, int totalTokensSaved, int patternMatches) {
            pieChart.updateData(apiCalls);
            
            int totalApiCalls = apiCalls.values().stream().mapToInt(Integer::intValue).sum();
            apiCallsLabel.setText("Total API Calls: " + totalApiCalls);
            patternMatchesLabel.setText("Pattern Matches: " + patternMatches);
            tokensSavedLabel.setText("Tokens Saved: " + NumberFormat.getIntegerInstance().format(totalTokensSaved));
            
            // Assume $0.002 per 1000 tokens
            double costSaved = totalTokensSaved * 0.002 / 1000.0;
            costSavedLabel.setText("Estimated Cost Saved: $" + String.format("%.2f", costSaved));
        }
    }
    
    /**
     * Panel for displaying pattern learning metrics.
     */
    private static class PatternLearningPanel extends JBPanel<PatternLearningPanel> {
        private BarChartPanel barChart;
        private JBLabel patternsLearnedLabel;
        private JBLabel patternMatchesLabel;
        private JBLabel patternEfficiencyLabel;
        
        /**
         * Creates a new PatternLearningPanel.
         */
        public PatternLearningPanel() {
            super(new BorderLayout());
            createUI();
        }
        
        /**
         * Creates the UI.
         */
        private void createUI() {
            // Create bar chart
            barChart = new BarChartPanel();
            
            // Create labels panel
            JPanel labelsPanel = new JBPanel<>();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
            labelsPanel.setBorder(JBUI.Borders.empty(10));
            
            patternsLearnedLabel = new JBLabel("Total Patterns Learned: 0");
            patternMatchesLabel = new JBLabel("Total Pattern Matches: 0");
            patternEfficiencyLabel = new JBLabel("Overall Pattern Efficiency: 0%");
            
            JPanel patternsLearnedPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            patternsLearnedPanel.add(patternsLearnedLabel);
            labelsPanel.add(patternsLearnedPanel);
            
            JPanel patternMatchesPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            patternMatchesPanel.add(patternMatchesLabel);
            labelsPanel.add(patternMatchesPanel);
            
            JPanel patternEfficiencyPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            patternEfficiencyPanel.add(patternEfficiencyLabel);
            labelsPanel.add(patternEfficiencyPanel);
            
            // Add components to panel
            add(barChart, BorderLayout.CENTER);
            add(labelsPanel, BorderLayout.SOUTH);
        }
        
        /**
         * Updates the data displayed in the panel.
         * @param patternEfficiency The pattern efficiency by type
         * @param patternsLearned The number of patterns learned
         * @param patternMatches The number of pattern matches
         */
        public void updateData(Map<String, Double> patternEfficiency, int patternsLearned, int patternMatches) {
            barChart.updateData(patternEfficiency);
            
            patternsLearnedLabel.setText("Total Patterns Learned: " + patternsLearned);
            patternMatchesLabel.setText("Total Pattern Matches: " + patternMatches);
            
            // Calculate average efficiency
            double avgEfficiency = patternEfficiency.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            
            patternEfficiencyLabel.setText("Overall Pattern Efficiency: " + 
                    String.format("%.1f%%", avgEfficiency * 100));
        }
    }
    
    /**
     * Panel for displaying performance metrics.
     */
    private static class PerformanceMetricsPanel extends JBPanel<PerformanceMetricsPanel> {
        private LineChartPanel lineChart;
        private JBLabel avgResponseTimeLabel;
        private JBLabel avgTokensPerRequestLabel;
        
        /**
         * Creates a new PerformanceMetricsPanel.
         */
        public PerformanceMetricsPanel() {
            super(new BorderLayout());
            createUI();
        }
        
        /**
         * Creates the UI.
         */
        private void createUI() {
            // Create line chart
            lineChart = new LineChartPanel();
            
            // Create labels panel
            JPanel labelsPanel = new JBPanel<>();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
            labelsPanel.setBorder(JBUI.Borders.empty(10));
            
            avgResponseTimeLabel = new JBLabel("Average Response Time: 0ms");
            avgTokensPerRequestLabel = new JBLabel("Average Tokens per Request: 0");
            
            JPanel avgResponseTimePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            avgResponseTimePanel.add(avgResponseTimeLabel);
            labelsPanel.add(avgResponseTimePanel);
            
            JPanel avgTokensPerRequestPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
            avgTokensPerRequestPanel.add(avgTokensPerRequestLabel);
            labelsPanel.add(avgTokensPerRequestPanel);
            
            // Add components to panel
            add(lineChart, BorderLayout.CENTER);
            add(labelsPanel, BorderLayout.SOUTH);
        }
        
        /**
         * Updates the data displayed in the panel.
         * @param responseTimesMs The response times in milliseconds
         * @param tokensPerRequest The tokens per request
         */
        public void updateData(List<Double> responseTimesMs, List<Double> tokensPerRequest) {
            lineChart.updateData(responseTimesMs, tokensPerRequest);
            
            // Calculate averages
            double avgResponseTime = responseTimesMs.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            
            double avgTokensPerReq = tokensPerRequest.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            
            avgResponseTimeLabel.setText("Average Response Time: " + 
                    String.format("%.1fms", avgResponseTime));
            avgTokensPerRequestLabel.setText("Average Tokens per Request: " + 
                    String.format("%.1f", avgTokensPerReq));
        }
    }
    
    /**
     * Panel for displaying a pie chart.
     */
    private static class PieChartPanel extends JBPanel<PieChartPanel> {
        private Map<String, Integer> data = new HashMap<>();
        private Map<String, Color> colors = new HashMap<>();
        private static final Color[] DEFAULT_COLORS = {
                new JBColor(new Color(66, 133, 244), new Color(66, 133, 244)),    // Blue
                new JBColor(new Color(219, 68, 55), new Color(219, 68, 55)),      // Red
                new JBColor(new Color(244, 180, 0), new Color(244, 180, 0)),      // Yellow
                new JBColor(new Color(15, 157, 88), new Color(15, 157, 88)),      // Green
                new JBColor(new Color(171, 71, 188), new Color(171, 71, 188)),    // Purple
                new JBColor(new Color(255, 112, 67), new Color(255, 112, 67)),    // Orange
        };
        
        /**
         * Creates a new PieChartPanel.
         */
        public PieChartPanel() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(400, 300));
        }
        
        /**
         * Updates the data displayed in the panel.
         * @param data The data to display
         */
        public void updateData(Map<String, Integer> data) {
            this.data = new HashMap<>(data);
            
            // Assign colors
            int colorIndex = 0;
            for (String key : data.keySet()) {
                if (!colors.containsKey(key)) {
                    colors.put(key, DEFAULT_COLORS[colorIndex % DEFAULT_COLORS.length]);
                    colorIndex++;
                }
            }
            
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Calculate total
            int total = data.values().stream().mapToInt(Integer::intValue).sum();
            
            if (total > 0) {
                // Draw pie chart
                int diameter = Math.min(width, height) - 40;
                int x = (width - diameter) / 2;
                int y = (height - diameter) / 2;
                
                double startAngle = 0;
                
                for (Map.Entry<String, Integer> entry : data.entrySet()) {
                    double angle = 360.0 * entry.getValue() / total;
                    
                    g2d.setColor(colors.getOrDefault(entry.getKey(), JBColor.GRAY));
                    g2d.fill(new Arc2D.Double(x, y, diameter, diameter, startAngle, angle, Arc2D.PIE));
                    
                    startAngle += angle;
                }
                
                // Draw legend
                int legendX = 10;
                int legendY = height - 10 - data.size() * 20;
                
                g2d.setColor(UIUtil.getLabelForeground());
                g2d.setFont(UIUtil.getLabelFont());
                
                for (Map.Entry<String, Integer> entry : data.entrySet()) {
                    g2d.setColor(colors.getOrDefault(entry.getKey(), JBColor.GRAY));
                    g2d.fillRect(legendX, legendY, 15, 15);
                    
                    g2d.setColor(UIUtil.getLabelForeground());
                    String legendText = entry.getKey() + ": " + entry.getValue() + 
                            " (" + String.format("%.1f%%", 100.0 * entry.getValue() / total) + ")";
                    g2d.drawString(legendText, legendX + 20, legendY + 12);
                    
                    legendY += 20;
                }
            } else {
                // Draw no data message
                g2d.setColor(UIUtil.getLabelForeground());
                g2d.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 14f));
                
                String message = "No data available";
                FontMetrics fm = g2d.getFontMetrics();
                int messageWidth = fm.stringWidth(message);
                
                g2d.drawString(message, (width - messageWidth) / 2, height / 2);
            }
            
            g2d.dispose();
        }
    }
    
    /**
     * Panel for displaying a bar chart.
     */
    private static class BarChartPanel extends JBPanel<BarChartPanel> {
        private Map<String, Double> data = new HashMap<>();
        private Map<String, Color> colors = new HashMap<>();
        private static final Color[] DEFAULT_COLORS = {
                new JBColor(new Color(66, 133, 244), new Color(66, 133, 244)),    // Blue
                new JBColor(new Color(219, 68, 55), new Color(219, 68, 55)),      // Red
                new JBColor(new Color(244, 180, 0), new Color(244, 180, 0)),      // Yellow
                new JBColor(new Color(15, 157, 88), new Color(15, 157, 88)),      // Green
                new JBColor(new Color(171, 71, 188), new Color(171, 71, 188)),    // Purple
                new JBColor(new Color(255, 112, 67), new Color(255, 112, 67)),    // Orange
        };
        
        /**
         * Creates a new BarChartPanel.
         */
        public BarChartPanel() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(400, 300));
        }
        
        /**
         * Updates the data displayed in the panel.
         * @param data The data to display
         */
        public void updateData(Map<String, Double> data) {
            this.data = new HashMap<>(data);
            
            // Assign colors
            int colorIndex = 0;
            for (String key : data.keySet()) {
                if (!colors.containsKey(key)) {
                    colors.put(key, DEFAULT_COLORS[colorIndex % DEFAULT_COLORS.length]);
                    colorIndex++;
                }
            }
            
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            if (!data.isEmpty()) {
                // Draw axes
                g2d.setColor(UIUtil.getLabelForeground());
                g2d.drawLine(50, height - 50, width - 20, height - 50); // X-axis
                g2d.drawLine(50, height - 50, 50, 20); // Y-axis
                
                // Draw Y-axis labels
                g2d.setFont(UIUtil.getLabelFont());
                for (int i = 0; i <= 10; i++) {
                    int y = height - 50 - (height - 70) * i / 10;
                    g2d.drawLine(47, y, 50, y); // Tick mark
                    g2d.drawString(String.format("%.1f", i / 10.0), 10, y + 5);
                }
                
                // Draw bars
                int barCount = data.size();
                int barWidth = (width - 70) / (barCount * 2);
                int x = 50 + barWidth;
                
                for (Map.Entry<String, Double> entry : data.entrySet()) {
                    g2d.setColor(colors.getOrDefault(entry.getKey(), JBColor.GRAY));
                    
                    int barHeight = (int) ((height - 70) * Math.min(1.0, entry.getValue()));
                    g2d.fill(new Rectangle2D.Double(x, height - 50 - barHeight, barWidth, barHeight));
                    
                    // Draw label
                    g2d.setColor(UIUtil.getLabelForeground());
                    String label = entry.getKey();
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(label);
                    
                    if (labelWidth > barWidth * 2) {
                        label = label.substring(0, Math.min(label.length(), 10)) + "...";
                        labelWidth = fm.stringWidth(label);
                    }
                    
                    g2d.drawString(label, x + (barWidth - labelWidth) / 2, height - 30);
                    
                    // Draw value
                    String value = String.format("%.1f%%", entry.getValue() * 100);
                    int valueWidth = fm.stringWidth(value);
                    g2d.drawString(value, x + (barWidth - valueWidth) / 2, height - 50 - barHeight - 5);
                    
                    x += barWidth * 2;
                }
            } else {
                // Draw no data message
                g2d.setColor(UIUtil.getLabelForeground());
                g2d.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 14f));
                
                String message = "No data available";
                FontMetrics fm = g2d.getFontMetrics();
                int messageWidth = fm.stringWidth(message);
                
                g2d.drawString(message, (width - messageWidth) / 2, height / 2);
            }
            
            g2d.dispose();
        }
    }
    
    /**
     * Panel for displaying a line chart.
     */
    private static class LineChartPanel extends JBPanel<LineChartPanel> {
        private List<Double> dataSet1 = new ArrayList<>();
        private List<Double> dataSet2 = new ArrayList<>();
        private Color color1 = new JBColor(new Color(66, 133, 244), new Color(66, 133, 244)); // Blue
        private Color color2 = new JBColor(new Color(15, 157, 88), new Color(15, 157, 88));   // Green
        
        /**
         * Creates a new LineChartPanel.
         */
        public LineChartPanel() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(400, 300));
        }
        
        /**
         * Updates the data displayed in the panel.
         * @param dataSet1 The first data set
         * @param dataSet2 The second data set
         */
        public void updateData(List<Double> dataSet1, List<Double> dataSet2) {
            this.dataSet1 = new ArrayList<>(dataSet1);
            this.dataSet2 = new ArrayList<>(dataSet2);
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            if (!dataSet1.isEmpty() || !dataSet2.isEmpty()) {
                // Draw axes
                g2d.setColor(UIUtil.getLabelForeground());
                g2d.drawLine(50, height - 50, width - 20, height - 50); // X-axis
                g2d.drawLine(50, height - 50, 50, 20); // Y-axis
                
                // Draw dataset 1
                if (!dataSet1.isEmpty()) {
                    double maxValue1 = dataSet1.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
                    
                    g2d.setColor(color1);
                    g2d.setStroke(new BasicStroke(2f));
                    
                    int x1 = 50;
                    int y1 = height - 50 - (int) ((height - 70) * dataSet1.get(0) / maxValue1);
                    
                    for (int i = 1; i < dataSet1.size(); i++) {
                        int x2 = 50 + (width - 70) * i / (dataSet1.size() - 1);
                        int y2 = height - 50 - (int) ((height - 70) * dataSet1.get(i) / maxValue1);
                        
                        g2d.drawLine(x1, y1, x2, y2);
                        
                        // Draw points
                        g2d.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
                        
                        x1 = x2;
                        y1 = y2;
                    }
                    
                    // Draw last point
                    g2d.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
                    
                    // Draw Y-axis labels for dataset 1
                    g2d.setFont(UIUtil.getLabelFont());
                    for (int i = 0; i <= 5; i++) {
                        int y = height - 50 - (height - 70) * i / 5;
                        g2d.drawLine(47, y, 50, y); // Tick mark
                        g2d.drawString(String.format("%.0f", maxValue1 * i / 5), 10, y + 5);
                    }
                    
                    // Draw legend
                    g2d.fillRect(width - 150, 20, 15, 15);
                    g2d.setColor(UIUtil.getLabelForeground());
                    g2d.drawString("Response Time (ms)", width - 130, 33);
                }
                
                // Draw dataset 2
                if (!dataSet2.isEmpty()) {
                    double maxValue2 = dataSet2.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
                    
                    g2d.setColor(color2);
                    g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                    
                    int x1 = 50;
                    int y1 = height - 50 - (int) ((height - 70) * dataSet2.get(0) / maxValue2);
                    
                    for (int i = 1; i < dataSet2.size(); i++) {
                        int x2 = 50 + (width - 70) * i / (dataSet2.size() - 1);
                        int y2 = height - 50 - (int) ((height - 70) * dataSet2.get(i) / maxValue2);
                        
                        g2d.drawLine(x1, y1, x2, y2);
                        
                        // Draw points
                        g2d.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
                        
                        x1 = x2;
                        y1 = y2;
                    }
                    
                    // Draw last point
                    g2d.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
                    
                    // Draw legend
                    g2d.fillRect(width - 150, 40, 15, 15);
                    g2d.setColor(UIUtil.getLabelForeground());
                    g2d.drawString("Tokens per Request", width - 130, 53);
                }
                
                // Draw X-axis labels
                g2d.setColor(UIUtil.getLabelForeground());
                int pointCount = Math.max(dataSet1.size(), dataSet2.size());
                for (int i = 0; i < pointCount; i++) {
                    int x = 50 + (width - 70) * i / (pointCount - 1);
                    g2d.drawLine(x, height - 50, x, height - 47); // Tick mark
                    g2d.drawString(String.valueOf(i + 1), x - 3, height - 35);
                }
            } else {
                // Draw no data message
                g2d.setColor(UIUtil.getLabelForeground());
                g2d.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 14f));
                
                String message = "No data available";
                FontMetrics fm = g2d.getFontMetrics();
                int messageWidth = fm.stringWidth(message);
                
                g2d.drawString(message, (width - messageWidth) / 2, height / 2);
            }
            
            g2d.dispose();
        }
    }
}