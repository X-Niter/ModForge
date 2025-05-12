package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.modforge.intellij.plugin.models.UserProfile;
import com.modforge.intellij.plugin.utils.ApiRequestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing user subscriptions and premium features.
 * Handles authentication, subscription validation, and marketing analytics.
 */
@Service
@State(
        name = "ModForgeUserSubscription",
        storages = {@Storage("modforge-subscription.xml")}
)
public final class UserSubscriptionService implements PersistentStateComponent<UserSubscriptionService.State> {
    private static final Logger LOG = Logger.getInstance(UserSubscriptionService.class);
    
    // Constants
    private static final String BASE_URL = "https://modforge.ai";
    private static final String SUBSCRIPTION_URL = BASE_URL + "/pricing";
    private static final String API_URL = BASE_URL + "/api";
    private static final int SUBSCRIPTION_CHECK_INTERVAL_HOURS = 24;
    private static final int MARKETING_COOLDOWN_DAYS = 7;
    private static final double DEFAULT_CONVERSION_RATE = 0.05; // 5% of impressions convert
    
    // State
    private final State myState = new State();
    
    // Runtime data
    @Transient private UserProfile userProfile;
    @Transient private final AtomicInteger impressionCount = new AtomicInteger(0);
    @Transient private final AtomicInteger interactionCount = new AtomicInteger(0);
    @Transient private final Map<MarketingStrategy, Long> lastImpressionTime = new HashMap<>();
    @Transient private final Map<MarketingStrategy, Integer> strategyImpressions = new HashMap<>();
    @Transient private final Map<MarketingStrategy, Integer> strategyInteractions = new HashMap<>();
    @Transient private final Random random = new Random();
    @Transient private ScheduledFuture<?> checkTask;
    
    /**
     * Initializes the service.
     */
    public UserSubscriptionService() {
        // Schedule subscription checks
        scheduleSubscriptionCheck();
        
        // Initialize user profile if we have an auth token
        if (hasAuthToken()) {
            fetchUserProfile();
        }
    }
    
    /**
     * Gets the instance of this service.
     * @return The service instance
     */
    public static UserSubscriptionService getInstance() {
        return ApplicationManager.getApplication().getService(UserSubscriptionService.class);
    }
    
    /**
     * Gets the persistent state.
     * @return The state
     */
    @Override
    public State getState() {
        return myState;
    }
    
    /**
     * Loads the persistent state.
     * @param state The state
     */
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
    
    /**
     * Checks if the user has an auth token.
     * @return Whether the user has an auth token
     */
    public boolean hasAuthToken() {
        return myState.authToken != null && !myState.authToken.isEmpty();
    }
    
    /**
     * Sets the auth token.
     * @param token The auth token
     */
    public void setAuthToken(String token) {
        myState.authToken = token;
        
        // Fetch user profile after token change
        if (hasAuthToken()) {
            fetchUserProfile();
        } else {
            userProfile = null;
            myState.isPremium = false;
        }
    }
    
    /**
     * Gets the auth token.
     * @return The auth token
     */
    @Nullable
    public String getAuthToken() {
        return myState.authToken;
    }
    
    /**
     * Checks if the user has a premium subscription.
     * @return Whether the user has a premium subscription
     */
    public boolean isPremium() {
        return myState.isPremium;
    }
    
    /**
     * Gets the user profile.
     * @return The user profile
     */
    @Nullable
    public UserProfile getUserProfile() {
        return userProfile;
    }
    
    /**
     * Fetches the user profile from the API.
     */
    public void fetchUserProfile() {
        if (!hasAuthToken()) {
            LOG.warn("Cannot fetch user profile without auth token");
            return;
        }
        
        // Fetch in background
        AppExecutorUtil.getAppExecutorService().submit(() -> {
            try {
                String response = ApiRequestUtil.get(
                        API_URL + "/users/profile",
                        myState.authToken
                );
                
                if (response != null) {
                    Gson gson = new Gson();
                    userProfile = gson.fromJson(response, UserProfile.class);
                    
                    // Update premium status
                    myState.isPremium = userProfile.isPremium();
                    
                    LOG.info("Fetched user profile: " + userProfile.getUsername() + 
                            " (Premium: " + myState.isPremium + ")");
                }
            } catch (Exception e) {
                LOG.error("Error fetching user profile", e);
            }
        });
    }
    
    /**
     * Validates the subscription with the API.
     */
    public void validateSubscription() {
        if (!hasAuthToken()) {
            LOG.warn("Cannot validate subscription without auth token");
            return;
        }
        
        // Validate in background
        AppExecutorUtil.getAppExecutorService().submit(() -> {
            try {
                String response = ApiRequestUtil.get(
                        API_URL + "/subscriptions/validate",
                        myState.authToken
                );
                
                if (response != null) {
                    Gson gson = new Gson();
                    Map<String, Object> result = gson.fromJson(response, Map.class);
                    
                    boolean wasValid = myState.isPremium;
                    myState.isPremium = (boolean) result.getOrDefault("isValid", false);
                    
                    // If subscription status changed, log it
                    if (wasValid != myState.isPremium) {
                        LOG.info("Subscription status changed: " + (myState.isPremium ? "Active" : "Inactive"));
                        
                        // If subscription became inactive, show a notification
                        if (!myState.isPremium) {
                            // In a real implementation, we would show a notification to the user
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error validating subscription", e);
            }
        });
    }
    
    /**
     * Schedules regular subscription checks.
     */
    private void scheduleSubscriptionCheck() {
        // Cancel existing task if any
        if (checkTask != null && !checkTask.isDone()) {
            checkTask.cancel(false);
        }
        
        // Schedule new task
        checkTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleAtFixedRate(
                this::validateSubscription,
                1,
                SUBSCRIPTION_CHECK_INTERVAL_HOURS,
                TimeUnit.HOURS
        );
    }
    
    /**
     * Opens the subscription page in the browser.
     */
    public void openSubscriptionPage() {
        try {
            String url = SUBSCRIPTION_URL;
            
            // Add auth token if available
            if (hasAuthToken()) {
                url += "?token=" + myState.authToken;
            }
            
            // Open in browser
            BrowserUtil.browse(url);
            
            LOG.info("Opened subscription page: " + url);
        } catch (Exception e) {
            LOG.error("Error opening subscription page", e);
        }
    }
    
    /**
     * Records a premium promotion impression.
     * @param strategy The marketing strategy used
     */
    public void recordPremiumPromoImpression(MarketingStrategy strategy) {
        impressionCount.incrementAndGet();
        strategyImpressions.merge(strategy, 1, Integer::sum);
        lastImpressionTime.put(strategy, System.currentTimeMillis());
        
        LOG.debug("Recorded premium promotion impression: " + strategy);
    }
    
    /**
     * Records a premium promotion interaction.
     * @param strategy The marketing strategy used
     * @param action The action taken
     */
    public void recordPremiumPromoInteraction(MarketingStrategy strategy, String action) {
        interactionCount.incrementAndGet();
        strategyInteractions.merge(strategy, 1, Integer::sum);
        
        LOG.debug("Recorded premium promotion interaction: " + strategy + " - " + action);
    }
    
    /**
     * Considers whether to show a premium promotion.
     * @param strategy The marketing strategy to consider
     * @return Whether to show the promotion
     */
    public boolean considerShowingPremiumPromotion(MarketingStrategy strategy) {
        // Don't show promotions to premium users
        if (isPremium()) {
            return false;
        }
        
        // Check if this strategy is in cooldown
        Long lastImpression = lastImpressionTime.get(strategy);
        if (lastImpression != null) {
            long daysSinceLastImpression = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - lastImpression
            );
            
            if (daysSinceLastImpression < MARKETING_COOLDOWN_DAYS) {
                return false;
            }
        }
        
        // Get the conversion rate for this strategy
        double conversionRate = getStrategyConversionRate(strategy);
        
        // Higher conversion rate = more likely to show
        return random.nextDouble() < conversionRate;
    }
    
    /**
     * Gets the conversion rate for a marketing strategy.
     * @param strategy The marketing strategy
     * @return The conversion rate
     */
    private double getStrategyConversionRate(MarketingStrategy strategy) {
        Integer impressions = strategyImpressions.get(strategy);
        Integer interactions = strategyInteractions.get(strategy);
        
        if (impressions == null || impressions == 0 || interactions == null) {
            return DEFAULT_CONVERSION_RATE;
        }
        
        return Math.min(0.9, Math.max(0.01, (double) interactions / impressions));
    }
    
    /**
     * Marketing strategies for premium promotions.
     */
    public enum MarketingStrategy {
        STARTUP_NOTIFICATION,
        FEATURE_BLOCKED,
        ERROR_RESOLUTION,
        CODE_GENERATION,
        JAR_ANALYSIS,
        SETTINGS_PAGE
    }
    
    /**
     * Persistent state for this service.
     */
    public static class State {
        public String authToken;
        public boolean isPremium = false;
        public long lastSubscriptionCheck = 0;
    }
}