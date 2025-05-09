package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.recommendation.PremiumFeature;
import com.modforge.intellij.plugin.recommendation.PremiumFeatureInjector;
import com.modforge.intellij.plugin.recommendation.PremiumFeatureRecommender;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing premium feature recommendations.
 * This is the main entry point for the recommendation system.
 */
@Service
public final class RecommendationService {
    private static final Logger LOG = Logger.getInstance(RecommendationService.class);
    
    private final Project project;
    private final PremiumFeatureRecommender recommender;
    private final PremiumFeatureInjector injector;
    private final UserSubscriptionService subscriptionService;
    
    /**
     * Creates a new recommendation service.
     * @param project The project
     */
    public RecommendationService(@NotNull Project project) {
        this.project = project;
        this.recommender = new PremiumFeatureRecommender(project);
        this.injector = new PremiumFeatureInjector(project, recommender);
        this.subscriptionService = UserSubscriptionService.getInstance();
        
        LOG.info("Initialized RecommendationService for project: " + project.getName());
    }
    
    /**
     * Gets the instance of this service.
     * @param project The project
     * @return The service instance
     */
    public static RecommendationService getInstance(@NotNull Project project) {
        return project.getService(RecommendationService.class);
    }
    
    /**
     * Records feature usage in the specified context.
     * This helps the recommendation engine learn user preferences and behavior.
     * @param featureId The feature ID
     * @param context The context
     */
    public void recordFeatureUsage(@NotNull String featureId, @NotNull PremiumFeatureRecommender.RecommendationContext context) {
        if (subscriptionService.isPremium()) {
            return; // No need to track for premium users
        }
        
        LOG.debug("Recording feature usage: " + featureId + " in context " + context);
        recommender.recordFeatureUsage(featureId, context);
    }
    
    /**
     * Considers showing a recommendation in the current context.
     * @param context The current context
     * @param component The component to attach to, or null
     * @return Whether a recommendation was shown
     */
    public boolean considerShowingRecommendation(
            @NotNull PremiumFeatureRecommender.RecommendationContext context,
            @NotNull JComponent component
    ) {
        if (subscriptionService.isPremium()) {
            return false; // No recommendations for premium users
        }
        
        LOG.debug("Considering showing recommendation in context: " + context);
        return injector.showRecommendationIfAppropriate(context, component, null);
    }
    
    /**
     * Considers showing a recommendation in the current context at the specified point.
     * @param context The current context
     * @param component The component to attach to
     * @param point The point to show at
     * @return Whether a recommendation was shown
     */
    public boolean considerShowingRecommendation(
            @NotNull PremiumFeatureRecommender.RecommendationContext context,
            @NotNull JComponent component,
            @NotNull Point point
    ) {
        if (subscriptionService.isPremium()) {
            return false; // No recommendations for premium users
        }
        
        LOG.debug("Considering showing recommendation in context: " + context + " at point " + point);
        return injector.showRecommendationIfAppropriate(context, component, point);
    }
    
    /**
     * Considers showing a recommendation in the current context in the tool window.
     * @param context The current context
     * @return Whether a recommendation was shown
     */
    public boolean considerShowingRecommendationInToolWindow(@NotNull PremiumFeatureRecommender.RecommendationContext context) {
        if (subscriptionService.isPremium()) {
            return false; // No recommendations for premium users
        }
        
        LOG.debug("Considering showing recommendation in tool window for context: " + context);
        return injector.showRecommendationIfAppropriate(context, null, null);
    }
    
    /**
     * Shows a specific premium feature recommendation to the user.
     * @param featureId The feature ID to recommend
     * @param component The component to attach to
     * @return Whether the recommendation was shown
     */
    public boolean showSpecificRecommendation(@NotNull String featureId, @NotNull JComponent component) {
        if (subscriptionService.isPremium()) {
            return false; // No recommendations for premium users
        }
        
        PremiumFeature feature = recommender.getFeatureById(featureId);
        if (feature == null) {
            LOG.warn("Premium feature not found: " + featureId);
            return false;
        }
        
        LOG.info("Showing specific recommendation: " + featureId);
        injector.showAsBalloon(feature, component, null);
        return true;
    }
    
    /**
     * Shows a specific premium feature recommendation as a dialog.
     * @param featureId The feature ID to recommend
     * @return Whether the recommendation was shown
     */
    public boolean showSpecificRecommendationAsDialog(@NotNull String featureId) {
        if (subscriptionService.isPremium()) {
            return false; // No recommendations for premium users
        }
        
        PremiumFeature feature = recommender.getFeatureById(featureId);
        if (feature == null) {
            LOG.warn("Premium feature not found: " + featureId);
            return false;
        }
        
        LOG.info("Showing specific recommendation as dialog: " + featureId);
        injector.showAsDialog(feature);
        return true;
    }
    
    /**
     * Startup activity that shows an initial recommendation when the project opens.
     */
    public static class RecommendationStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            // Don't run in unit tests
            if (project.isDefault()) {
                return;
            }
            
            // Delay the initial recommendation to avoid disrupting the IDE startup
            UIUtil.invokeLaterIfNeeded(() -> {
                try {
                    // Wait a bit for the IDE to finish startup
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    
                    // Show a recommendation in the IDE start context
                    RecommendationService service = getInstance(project);
                    service.considerShowingRecommendationInToolWindow(
                            PremiumFeatureRecommender.RecommendationContext.IDE_START
                    );
                } catch (Exception e) {
                    LOG.error("Error showing startup recommendation", e);
                }
            });
        }
    }
}