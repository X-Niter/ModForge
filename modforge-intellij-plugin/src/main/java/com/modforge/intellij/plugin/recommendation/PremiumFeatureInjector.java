package com.modforge.intellij.plugin.recommendation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.services.UserSubscriptionService;
import com.modforge.intellij.plugin.ui.PremiumFeatureNotificationPanel;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Injects premium feature recommendations into the IDE UI.
 * Handles different display methods based on context.
 */
public class PremiumFeatureInjector {
    private static final Logger LOG = Logger.getInstance(PremiumFeatureInjector.class);
    
    private final Project project;
    private final PremiumFeatureRecommender recommender;
    private final UserSubscriptionService subscriptionService;
    
    /**
     * Creates a new premium feature injector.
     * @param project The project
     * @param recommender The premium feature recommender
     */
    public PremiumFeatureInjector(@NotNull Project project, @NotNull PremiumFeatureRecommender recommender) {
        this.project = project;
        this.recommender = recommender;
        this.subscriptionService = UserSubscriptionService.getInstance();
    }
    
    /**
     * Shows a recommendation in the current context if appropriate.
     * @param context The current context
     * @param component The component to attach to, or null
     * @param point The point to show at, or null
     * @return Whether a recommendation was shown
     */
    public boolean showRecommendationIfAppropriate(
            @NotNull PremiumFeatureRecommender.RecommendationContext context,
            @Nullable JComponent component,
            @Nullable Point point
    ) {
        // Don't show recommendations for premium users
        if (subscriptionService.isPremium()) {
            return false;
        }
        
        // Get a recommendation for this context
        PremiumFeature feature = recommender.getRecommendation(context);
        if (feature == null) {
            return false;
        }
        
        // Record the feature usage in this context for future recommendations
        recommender.recordFeatureUsage(feature.getId(), context);
        
        // Choose the best way to display the recommendation based on context
        if (component != null) {
            // Show as a balloon popup if we have a component
            showAsBalloon(feature, component, point);
        } else {
            // Otherwise, show in the tool window
            showInToolWindow(feature);
        }
        
        return true;
    }
    
    /**
     * Shows a recommendation as a balloon popup.
     * @param feature The premium feature to recommend
     * @param component The component to attach to
     * @param point The point to show at, or null to show at the component's center
     */
    public void showAsBalloon(@NotNull PremiumFeature feature, @NotNull JComponent component, @Nullable Point point) {
        LOG.info("Showing premium feature recommendation as balloon: " + feature.getTitle());
        
        PremiumFeatureNotificationPanel panel = new PremiumFeatureNotificationPanel(project, feature, recommender);
        
        Balloon balloon = JBPopupFactory.getInstance()
                .createBalloonBuilder(panel)
                .setFillColor(UIUtil.getPanelBackground())
                .setBorderColor(UIUtil.getBorderColor())
                .setShowCallout(true)
                .setShadow(true)
                .setHideOnClickOutside(true)
                .setHideOnAction(false)
                .setHideOnKeyOutside(false)
                .setAnimationCycle(200)
                .setCloseButtonEnabled(true)
                .createBalloon();
        
        if (point != null) {
            balloon.show(new RelativePoint(component, point), Balloon.Position.below);
        } else {
            balloon.show(new RelativePoint(component, 
                    new Point(component.getWidth() / 2, component.getHeight() / 2)), 
                    Balloon.Position.below);
        }
    }
    
    /**
     * Shows a recommendation in the ModForge AI tool window.
     * @param feature The premium feature to recommend
     */
    public void showInToolWindow(@NotNull PremiumFeature feature) {
        LOG.info("Showing premium feature recommendation in tool window: " + feature.getTitle());
        
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ModForge AI");
        
        if (toolWindow == null) {
            LOG.warn("ModForge AI tool window not found");
            return;
        }
        
        PremiumFeatureNotificationPanel panel = new PremiumFeatureNotificationPanel(project, feature, recommender);
        panel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Create a content for the notification using compatibility method
        ContentFactory contentFactory = CompatibilityUtil.getContentFactory();
        Content content = contentFactory.createContent(panel, "Premium Feature", false);
        content.setCloseable(true);
        
        // Add the content to the tool window
        toolWindow.getContentManager().addContent(content);
        toolWindow.getContentManager().setSelectedContent(content);
        
        // Show the tool window if it's not visible
        if (!toolWindow.isVisible()) {
            toolWindow.show(null);
        }
    }
    
    /**
     * Shows a recommendation in a dialog.
     * @param feature The premium feature to recommend
     */
    public void showAsDialog(@NotNull PremiumFeature feature) {
        LOG.info("Showing premium feature recommendation as dialog: " + feature.getTitle());
        
        PremiumFeatureDialog dialog = new PremiumFeatureDialog(project, feature);
        dialog.show();
        
        if (dialog.isOK()) {
            // User clicked "Upgrade Now"
            recommender.recordRecommendationInteraction(feature, "upgrade_clicked");
            subscriptionService.openSubscriptionPage();
        } else {
            // User clicked "Maybe Later"
            recommender.recordRecommendationInteraction(feature, "dismissed");
        }
    }
    
    /**
     * Dialog for displaying a premium feature recommendation.
     */
    private static class PremiumFeatureDialog extends com.intellij.openapi.ui.DialogWrapper {
        private final PremiumFeature feature;
        
        public PremiumFeatureDialog(Project project, PremiumFeature feature) {
            super(project);
            this.feature = feature;
            setTitle("Premium Feature: " + feature.getTitle());
            setOKButtonText("Upgrade Now");
            setCancelButtonText("Maybe Later");
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(400, 200));
            
            JLabel titleLabel = new JLabel(feature.getTitle());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
            panel.add(titleLabel, BorderLayout.NORTH);
            
            JEditorPane descriptionPane = new JEditorPane();
            descriptionPane.setContentType("text/html");
            descriptionPane.setText("<html><body style='margin: 10px'>" +
                    "<p>" + feature.getShortPitch() + "</p>" +
                    "<p>" + feature.getDescription() + "</p>" +
                    "</body></html>");
            descriptionPane.setEditable(false);
            descriptionPane.setBackground(panel.getBackground());
            
            panel.add(new JScrollPane(descriptionPane), BorderLayout.CENTER);
            
            return panel;
        }
    }
}