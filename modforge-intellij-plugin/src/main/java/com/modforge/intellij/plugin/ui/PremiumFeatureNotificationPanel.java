package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.recommendation.PremiumFeature;
import com.modforge.intellij.plugin.recommendation.PremiumFeatureRecommender;
import com.modforge.intellij.plugin.services.UserSubscriptionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel for displaying premium feature notifications.
 * Shows contextual recommendations based on user activity.
 */
public class PremiumFeatureNotificationPanel extends JBPanel<PremiumFeatureNotificationPanel> {
    private static final Logger LOG = Logger.getInstance(PremiumFeatureNotificationPanel.class);
    
    private final Project project;
    private final PremiumFeature feature;
    private final PremiumFeatureRecommender recommender;
    private final UserSubscriptionService subscriptionService;
    
    // UI Components
    private JBLabel titleLabel;
    private JBLabel pitchLabel;
    private JButton learnMoreButton;
    private JButton dismissButton;
    private JPanel mainPanel;
    
    /**
     * Creates a new premium feature notification panel.
     * @param project The project
     * @param feature The premium feature to display
     * @param recommender The premium feature recommender
     */
    public PremiumFeatureNotificationPanel(
            @NotNull Project project,
            @NotNull PremiumFeature feature,
            @NotNull PremiumFeatureRecommender recommender
    ) {
        super(new BorderLayout());
        this.project = project;
        this.feature = feature;
        this.recommender = recommender;
        this.subscriptionService = UserSubscriptionService.getInstance();
        
        setBackground(UIUtil.getPanelBackground());
        setBorder(JBUI.Borders.empty(10));
        
        createUI();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(UIUtil.getPanelBackground());
        mainPanel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(UIUtil.getBorderColor()),
                JBUI.Borders.empty(10)
        ));
        
        // Add premium badge
        JBLabel premiumBadge = new JBLabel("PREMIUM");
        premiumBadge.setForeground(Color.BLACK);
        premiumBadge.setBackground(new Color(255, 215, 0)); // Gold
        premiumBadge.setOpaque(true);
        premiumBadge.setBorder(JBUI.Borders.empty(2, 5));
        premiumBadge.setFont(premiumBadge.getFont().deriveFont(Font.BOLD, 10f));
        
        // Add title
        titleLabel = new JBLabel(feature.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        // Create header panel with title and badge
        JPanel headerPanel = new JBPanel<>(new BorderLayout(10, 0));
        headerPanel.setBackground(UIUtil.getPanelBackground());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(premiumBadge, BorderLayout.EAST);
        
        // Add pitch
        pitchLabel = new JBLabel("<html>" + feature.getShortPitch() + "</html>");
        pitchLabel.setBorder(JBUI.Borders.empty(5, 0, 10, 0));
        
        // Add buttons
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UIUtil.getPanelBackground());
        
        learnMoreButton = new JButton("Learn More");
        learnMoreButton.addActionListener(e -> onLearnMoreClicked());
        
        dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(e -> onDismissClicked());
        
        buttonPanel.add(learnMoreButton);
        buttonPanel.add(dismissButton);
        
        // Assemble the panel
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBackground(UIUtil.getPanelBackground());
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(pitchLabel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add close button in corner
        JLabel closeLabel = new JLabel("×");
        closeLabel.setFont(closeLabel.getFont().deriveFont(Font.BOLD, 16f));
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onDismissClicked();
            }
        });
        mainPanel.add(closeLabel, BorderLayout.EAST);
        
        // Add to this panel
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Shows a full details dialog for the premium feature.
     */
    private void showDetailsDialog() {
        PremiumFeatureDetailsDialog dialog = new PremiumFeatureDetailsDialog(project, feature);
        dialog.show();
        
        if (dialog.isOK()) {
            // User clicked "Upgrade Now"
            recommender.recordRecommendationInteraction(feature, "upgrade_clicked");
            subscriptionService.openSubscriptionPage();
        }
    }
    
    /**
     * Called when the Learn More button is clicked.
     */
    private void onLearnMoreClicked() {
        LOG.info("Learn More clicked for premium feature: " + feature.getTitle());
        recommender.recordRecommendationInteraction(feature, "clicked");
        showDetailsDialog();
    }
    
    /**
     * Called when the Dismiss button is clicked.
     */
    private void onDismissClicked() {
        LOG.info("Dismiss clicked for premium feature: " + feature.getTitle());
        recommender.recordRecommendationInteraction(feature, "dismissed");
        
        // Remove this panel from its parent
        Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.revalidate();
            parent.repaint();
        }
    }
    
    /**
     * Dialog for displaying premium feature details.
     */
    private static class PremiumFeatureDetailsDialog extends DialogWrapper {
        private final PremiumFeature feature;
        
        public PremiumFeatureDetailsDialog(Project project, PremiumFeature feature) {
            super(project, false);
            this.feature = feature;
            setTitle("Premium Feature: " + feature.getTitle());
            setOKButtonText("Upgrade Now");
            setCancelButtonText("Maybe Later");
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JBPanel<>(new BorderLayout());
            panel.setPreferredSize(new Dimension(500, 400));
            
            // Create HTML content
            JEditorPane editorPane = new JEditorPane("text/html", feature.getDetailsHtml());
            editorPane.setEditable(false);
            editorPane.setBackground(UIUtil.getPanelBackground());
            editorPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (Exception ex) {
                                LOG.error("Error opening URL", ex);
                            }
                        }
                    }
                }
            });
            
            JBScrollPane scrollPane = new JBScrollPane(editorPane);
            scrollPane.setBorder(JBUI.Borders.empty());
            
            panel.add(scrollPane, BorderLayout.CENTER);
            
            return panel;
        }
    }
}