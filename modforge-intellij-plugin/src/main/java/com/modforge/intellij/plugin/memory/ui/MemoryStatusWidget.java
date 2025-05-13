package com.modforge.intellij.plugin.memory.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import com.modforge.intellij.plugin.memory.MemoryOptimizer;
import com.modforge.intellij.plugin.memory.MemoryUtils;
import com.modforge.intellij.plugin.memory.MemoryUtils.MemoryPressureLevel;
import com.modforge.intellij.plugin.memory.settings.MemoryManagementSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Status bar widget for displaying memory status
 */
public class MemoryStatusWidget implements StatusBarWidget, StatusBarWidget.IconPresentation, StatusBarWidget.Multiframe {
    public static final String ID = "com.modforge.intellij.plugin.memory.ui.MemoryStatusWidget";
    
    private final Project project;
    private StatusBar statusBar;
    private final Timer updateTimer;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private MemoryPressureLevel lastPressureLevel = MemoryPressureLevel.NORMAL;
    private final AnimatedIcon memoryOptimizationIcon = new AnimatedIcon(
            AnimatedIcon.QUESTION_DELAY, 
            UIUtil.getQuestionIcon(), 
            UIUtil.getLabelDisabledForeground()
    );
    
    private static final Color NORMAL_COLOR = new JBColor(new Color(45, 183, 93), new Color(45, 183, 93));
    private static final Color WARNING_COLOR = new JBColor(new Color(255, 170, 0), new Color(255, 170, 0));
    private static final Color CRITICAL_COLOR = new JBColor(new Color(255, 102, 0), new Color(255, 102, 0));
    private static final Color EMERGENCY_COLOR = new JBColor(new Color(232, 17, 35), new Color(232, 17, 35));
    
    /**
     * Constructor
     * 
     * @param project The project
     */
    public MemoryStatusWidget(@NotNull Project project) {
        this.project = project;
        
        // Update timer every 5 seconds
        updateTimer = new Timer(5000, e -> {
            if (active.get() && statusBar != null) {
                updateUI();
            }
        });
        updateTimer.setInitialDelay(0);
        updateTimer.start();
    }
    
    @NotNull
    @Override
    public String ID() {
        return ID;
    }
    
    /**
     * Update the UI
     */
    private void updateUI() {
        MemoryPressureLevel currentLevel = MemoryUtils.getMemoryPressureLevel();
        
        // Update status bar if pressure level has changed
        if (currentLevel != lastPressureLevel) {
            lastPressureLevel = currentLevel;
            if (statusBar != null) {
                statusBar.updateWidget(ID());
            }
        }
    }
    
    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
        updateUI();
    }
    
    @Override
    public void dispose() {
        active.set(false);
        updateTimer.stop();
        statusBar = null;
    }
    
    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }
    
    @Override
    public Icon getIcon() {
        MemoryManager memoryManager = MemoryManager.getInstance();
        
        if (memoryManager != null && memoryManager.isOptimizing()) {
            return memoryOptimizationIcon;
        }
        
        double usagePercentage = MemoryUtils.getMemoryUsagePercentage();
        MemoryPressureLevel pressureLevel = MemoryUtils.getMemoryPressureLevel();
        String usageText = String.format("%.1f%%", usagePercentage);
        Color color;
        
        switch (pressureLevel) {
            case EMERGENCY:
                color = EMERGENCY_COLOR;
                break;
            case CRITICAL:
                color = CRITICAL_COLOR;
                break;
            case WARNING:
                color = WARNING_COLOR;
                break;
            default:
                color = NORMAL_COLOR;
                break;
        }
        
        return new MemoryStatusIcon(usageText, color);
    }
    
    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return this::handleClick;
    }
    
    @Override
    public StatusBarWidget copy() {
        return new MemoryStatusWidget(project);
    }
    
    /**
     * Handle a click on the widget
     * 
     * @param mouseEvent The mouse event
     */
    private void handleClick(MouseEvent mouseEvent) {
        DefaultActionGroup group = new DefaultActionGroup();
        
        // Add memory optimization actions
        group.add(new AnAction("Optimize Memory (Minimal)") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.MINIMAL);
            }
        });
        
        group.add(new AnAction("Optimize Memory (Conservative)") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.CONSERVATIVE);
            }
        });
        
        group.add(new AnAction("Optimize Memory (Normal)") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.NORMAL);
            }
        });
        
        group.add(new AnAction("Optimize Memory (Aggressive)") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MemoryUtils.optimizeMemory(project, MemoryOptimizer.OptimizationLevel.AGGRESSIVE);
            }
        });
        
        group.addSeparator();
        
        // Request GC action
        group.add(new AnAction("Request Garbage Collection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MemoryUtils.requestGarbageCollection();
            }
        });
        
        group.addSeparator();
        
        // Show detailed memory info
        group.add(new AnAction("Show Memory Details") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MemoryDetailsDialog.show(project);
            }
        });
        
        // Memory settings
        group.add(new AnAction("Memory Management Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Get the ShowSettingsAction instance
                AnAction showSettingsAction = ActionManager.getInstance().getAction("ShowSettings");
                
                // Create an action event with the memory settings ID
                AnActionEvent settingsEvent = AnActionEvent.createFromAnAction(
                    showSettingsAction,
                    mouseEvent,
                    ActionPlaces.UNKNOWN,
                    com.modforge.intellij.plugin.utils.CompatibilityUtil.getCompatibleDataContext(mouseEvent)
                );
                
                // Add the specific settings page ID to navigate directly to memory settings
                settingsEvent.getPresentation().putClientProperty(
                    "settings.preferedFocusedOption", 
                    "com.modforge.intellij.plugin.memory.settings.MemoryManagementConfigurable"
                );
                
                // Execute the action with our modified event
                showSettingsAction.actionPerformed(settingsEvent);
            }
        });
        
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "Memory Management",
                group,
                mouseEvent.getComponent().getGraphicsConfiguration().createCompatibleImage(1, 1).getGraphics(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
        );
        
        Disposer.register(this, popup);
        popup.show(new RelativePoint(mouseEvent));
    }
    
    /**
     * Status bar widget factory for the memory status widget
     */
    public static class Factory implements StatusBarWidgetFactory {
        @NotNull
        @Override
        public String getId() {
            return ID;
        }
        
        @NotNull
        @Override
        public String getDisplayName() {
            return "Memory Status";
        }
        
        @Override
        public boolean isAvailable(@NotNull Project project) {
            return MemoryManagementSettings.getInstance().isShowMemoryWidget();
        }
        
        @NotNull
        @Override
        public StatusBarWidget createWidget(@NotNull Project project) {
            return new MemoryStatusWidget(project);
        }
        
        @Override
        public void disposeWidget(@NotNull StatusBarWidget widget) {
            widget.dispose();
        }
        
        @Override
        public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
            return true;
        }
    }
    
    /**
     * Icon for the memory status widget
     */
    private static class MemoryStatusIcon implements Icon {
        private final String text;
        private final Color color;
        private final int width;
        private final int height;
        
        /**
         * Constructor
         * 
         * @param text The text to display
         * @param color The color of the icon
         */
        public MemoryStatusIcon(String text, Color color) {
            this.text = text;
            this.color = color;
            
            // Calculate dimensions
            FontMetrics metrics = new JLabel().getFontMetrics(UIUtil.getLabelFont());
            this.width = metrics.stringWidth(text) + 10;
            this.height = metrics.getHeight();
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            
            try {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw background
                g2d.setColor(color);
                g2d.fillRoundRect(x, y, width, height, 5, 5);
                
                // Draw text
                g2d.setColor(Color.WHITE);
                g2d.setFont(UIUtil.getLabelFont());
                FontMetrics metrics = g2d.getFontMetrics();
                int textX = x + (width - metrics.stringWidth(text)) / 2;
                int textY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent();
                g2d.drawString(text, textX, textY);
            } finally {
                g2d.dispose();
            }
        }
        
        @Override
        public int getIconWidth() {
            return width;
        }
        
        @Override
        public int getIconHeight() {
            return height;
        }
    }
}