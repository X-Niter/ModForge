package com.modforge.intellij.plugin.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.models.SubscriptionPlan;
import com.modforge.intellij.plugin.models.UserProfile;
import com.modforge.intellij.plugin.utils.ApiRequestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing user subscription and premium features.
 * Handles authentication, subscription status, and feature access.
 */
@Service
public final class UserSubscriptionService {
    private static final Logger LOG = Logger.getInstance(UserSubscriptionService.class);
    
    // API endpoints
    private static final String AUTH_ENDPOINT = "/auth";
    private static final String SUBSCRIPTION_ENDPOINT = "/subscription";
    private static final String USER_PROFILE_ENDPOINT = "/user/profile";
    private static final String ENGAGEMENT_ENDPOINT = "/engagement";
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Gson gson;
    
    // User state
    private boolean authenticated = false;
    private UserProfile userProfile = null;
    private SubscriptionPlan currentPlan = SubscriptionPlan.FREE;
    private String authToken = null;
    
    // Usage tracking
    private int apiCallsThisMonth = 0;
    private int freeApiCallsLimit = 100;
    private int premiumPromoImpressions = 0;
    private int premiumPromoInteractions = 0;
    
    // Smart marketing state
    private MarketingStrategy currentStrategy = MarketingStrategy.NONE;
    private double conversionRate = 0.0;
    private long lastPromotionTimestamp = 0;
    private int minimumPromotionIntervalMinutes = 60; // Default: at most once per hour
    
    public UserSubscriptionService() {
        this.executorService = AppExecutorUtil.getAppExecutorService();
        this.scheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Schedule periodic checks of subscription status
        scheduledExecutorService.scheduleAtFixedRate(
                this::refreshSubscriptionStatus,
                1, 
                60, 
                TimeUnit.MINUTES
        );
    }
    
    /**
     * Gets the instance of this service.
     * @return The service instance
     */
    public static UserSubscriptionService getInstance() {
        return ApplicationManager.getApplication().getService(UserSubscriptionService.class);
    }
    
    /**
     * Attempts to authenticate the user with the given credentials.
     * @param username The username
     * @param password The password
     * @return CompletableFuture that resolves to true if authentication was successful, false otherwise
     */
    public CompletableFuture<Boolean> authenticate(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Authenticating user: " + username);
                
                String apiUrl = ModForgeSettingsService.getInstance().getApiUrl();
                String json = gson.toJson(new AuthRequest(username, password));
                
                String response = ApiRequestUtil.post(apiUrl + AUTH_ENDPOINT, json);
                if (response != null) {
                    AuthResponse authResponse = gson.fromJson(response, AuthResponse.class);
                    if (authResponse != null && authResponse.getToken() != null) {
                        this.authToken = authResponse.getToken();
                        this.authenticated = true;
                        
                        // Fetch user profile and subscription status
                        fetchUserProfile();
                        fetchSubscriptionStatus();
                        
                        return true;
                    }
                }
                
                this.authenticated = false;
                this.authToken = null;
                return false;
            } catch (Exception e) {
                LOG.error("Error authenticating user", e);
                this.authenticated = false;
                this.authToken = null;
                return false;
            }
        }, executorService);
    }
    
    /**
     * Refreshes the user's subscription status.
     */
    public void refreshSubscriptionStatus() {
        if (!authenticated || authToken == null) {
            return;
        }
        
        try {
            fetchUserProfile();
            fetchSubscriptionStatus();
        } catch (Exception e) {
            LOG.error("Error refreshing subscription status", e);
        }
    }
    
    /**
     * Fetches the user's profile information.
     */
    private void fetchUserProfile() {
        if (!authenticated || authToken == null) {
            return;
        }
        
        try {
            LOG.info("Fetching user profile");
            
            String apiUrl = ModForgeSettingsService.getInstance().getApiUrl();
            String url = apiUrl + USER_PROFILE_ENDPOINT;
            
            // Add authorization header
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .build();
            
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    userProfile = gson.fromJson(responseBody, UserProfile.class);
                    LOG.info("Fetched user profile for: " + userProfile.getUsername());
                }
            }
        } catch (Exception e) {
            LOG.error("Error fetching user profile", e);
        }
    }
    
    /**
     * Fetches the user's subscription status.
     */
    private void fetchSubscriptionStatus() {
        if (!authenticated || authToken == null) {
            return;
        }
        
        try {
            LOG.info("Fetching subscription status");
            
            String apiUrl = ModForgeSettingsService.getInstance().getApiUrl();
            String url = apiUrl + SUBSCRIPTION_ENDPOINT;
            
            // Add authorization header
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .build();
            
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    SubscriptionResponse subscriptionResponse = gson.fromJson(responseBody, SubscriptionResponse.class);
                    
                    if (subscriptionResponse != null) {
                        this.currentPlan = subscriptionResponse.getPlan();
                        this.apiCallsThisMonth = subscriptionResponse.getApiCallsThisMonth();
                        this.freeApiCallsLimit = subscriptionResponse.getFreeApiCallsLimit();
                        
                        LOG.info("Fetched subscription status: " + currentPlan);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error fetching subscription status", e);
        }
    }
    
    /**
     * Logs the user out.
     */
    public void logout() {
        this.authenticated = false;
        this.authToken = null;
        this.userProfile = null;
        this.currentPlan = SubscriptionPlan.FREE;
    }
    
    /**
     * Gets whether the user is authenticated.
     * @return Whether the user is authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Gets the current subscription plan.
     * @return The current subscription plan
     */
    public SubscriptionPlan getCurrentPlan() {
        return currentPlan;
    }
    
    /**
     * Gets the user profile.
     * @return The user profile, or null if not authenticated
     */
    @Nullable
    public UserProfile getUserProfile() {
        return userProfile;
    }
    
    /**
     * Gets whether the user has a premium subscription.
     * @return Whether the user has a premium subscription
     */
    public boolean isPremium() {
        return currentPlan == SubscriptionPlan.PREMIUM || 
               currentPlan == SubscriptionPlan.ENTERPRISE;
    }
    
    /**
     * Gets whether the user has an enterprise subscription.
     * @return Whether the user has an enterprise subscription
     */
    public boolean isEnterprise() {
        return currentPlan == SubscriptionPlan.ENTERPRISE;
    }
    
    /**
     * Gets whether the user has API calls remaining.
     * @return Whether the user has API calls remaining
     */
    public boolean hasApiCallsRemaining() {
        return isPremium() || apiCallsThisMonth < freeApiCallsLimit;
    }
    
    /**
     * Records an API call.
     */
    public void recordApiCall() {
        apiCallsThisMonth++;
        
        // If this puts the user over the free limit, consider showing a premium promotion
        if (!isPremium() && apiCallsThisMonth >= freeApiCallsLimit) {
            considerShowingPremiumPromotion(MarketingStrategy.USAGE_LIMIT);
        }
    }
    
    /**
     * Records when a user encounters a premium-only feature.
     * @param featureName The name of the feature
     */
    public void recordPremiumFeatureEncounter(String featureName) {
        if (!isPremium()) {
            considerShowingPremiumPromotion(MarketingStrategy.FEATURE_BLOCKED);
            
            // Record the encounter for analytics
            executorService.submit(() -> {
                try {
                    String apiUrl = ModForgeSettingsService.getInstance().getApiUrl();
                    String json = gson.toJson(new EngagementEvent(
                            "premium_feature_encounter", 
                            featureName
                    ));
                    
                    ApiRequestUtil.post(apiUrl + ENGAGEMENT_ENDPOINT, json);
                } catch (Exception e) {
                    LOG.error("Error recording premium feature encounter", e);
                }
            });
        }
    }
    
    /**
     * Records a premium promotion impression.
     * @param strategy The marketing strategy used
     */
    public void recordPremiumPromoImpression(MarketingStrategy strategy) {
        premiumPromoImpressions++;
        
        // Record the impression for analytics
        executorService.submit(() -> {
            try {
                String apiUrl = ModForgeSettingsService.getInstance().getApiUrl();
                String json = gson.toJson(new EngagementEvent(
                        "premium_promo_impression", 
                        strategy.name()
                ));
                
                ApiRequestUtil.post(apiUrl + ENGAGEMENT_ENDPOINT, json);
            } catch (Exception e) {
                LOG.error("Error recording premium promo impression", e);
            }
        });
    }
    
    /**
     * Records a premium promotion interaction.
     * @param strategy The marketing strategy used
     * @param action The action taken (e.g., "clicked", "dismissed")
     */
    public void recordPremiumPromoInteraction(MarketingStrategy strategy, String action) {
        premiumPromoInteractions++;
        
        // Update conversion rate
        if (premiumPromoImpressions > 0) {
            conversionRate = (double) premiumPromoInteractions / premiumPromoImpressions;
        }
        
        // Record the interaction for analytics
        executorService.submit(() -> {
            try {
                String apiUrl = ModForgeSettingsService.getInstance().getApiUrl();
                String json = gson.toJson(new EngagementEvent(
                        "premium_promo_interaction", 
                        strategy.name() + ":" + action
                ));
                
                ApiRequestUtil.post(apiUrl + ENGAGEMENT_ENDPOINT, json);
            } catch (Exception e) {
                LOG.error("Error recording premium promo interaction", e);
            }
        });
    }
    
    /**
     * Considers showing a premium promotion based on the given trigger.
     * Uses smart marketing logic to decide whether to show a promotion.
     * @param trigger The trigger for showing a promotion
     * @return Whether a promotion should be shown
     */
    public boolean considerShowingPremiumPromotion(MarketingStrategy trigger) {
        if (isPremium()) {
            return false; // Already premium
        }
        
        // Check if we've shown a promotion recently
        long now = System.currentTimeMillis();
        if (now - lastPromotionTimestamp < minimumPromotionIntervalMinutes * 60 * 1000) {
            return false; // Too soon
        }
        
        // Decide whether to show promotion based on the trigger and user behavior
        boolean shouldShow = false;
        
        switch (trigger) {
            case USAGE_LIMIT:
                // Almost always show when hitting usage limit
                shouldShow = true;
                break;
                
            case FEATURE_BLOCKED:
                // Show with moderate frequency for blocked features
                shouldShow = Math.random() < 0.7;
                break;
                
            case TIME_BASED:
                // Show occasionally based on time
                shouldShow = Math.random() < 0.3;
                break;
                
            case SUCCESS_MOMENT:
                // Show during moments of success, with moderate frequency
                shouldShow = Math.random() < 0.5;
                break;
        }
        
        if (shouldShow) {
            // Update state
            currentStrategy = trigger;
            lastPromotionTimestamp = now;
        }
        
        return shouldShow;
    }
    
    /**
     * Gets the current marketing strategy.
     * @return The current marketing strategy
     */
    public MarketingStrategy getCurrentMarketingStrategy() {
        return currentStrategy;
    }
    
    /**
     * Gets the premium promotional message based on the current strategy.
     * @return The promotional message
     */
    public String getPremiumPromotionalMessage() {
        switch (currentStrategy) {
            case USAGE_LIMIT:
                return "You've reached the free API usage limit for this month. " +
                        "Upgrade to Premium for unlimited API calls and exclusive features!";
                
            case FEATURE_BLOCKED:
                return "This feature is available exclusively to Premium subscribers. " +
                        "Unlock the full power of ModForge AI by upgrading today!";
                
            case TIME_BASED:
                return "Supercharge your Minecraft mod development with ModForge AI Premium. " +
                        "Unlock advanced features and save valuable development time!";
                
            case SUCCESS_MOMENT:
                return "Great job! Upgrade to Premium to get even more powerful AI-driven " +
                        "mod creation tools and unlimited generations!";
                
            default:
                return "Upgrade to ModForge AI Premium for the ultimate mod development experience!";
        }
    }
    
    /**
     * Gets the benefits of the premium plan.
     * @return The benefits of the premium plan
     */
    public String[] getPremiumBenefits() {
        return new String[]{
                "✓ Unlimited API calls",
                "✓ Advanced pattern learning",
                "✓ Priority processing",
                "✓ Exclusive premium features",
                "✓ Remote build and test",
                "✓ Enhanced error resolution"
        };
    }
    
    /**
     * Opens the subscription page in the browser.
     */
    public void openSubscriptionPage() {
        String subscriptionUrl = ModForgeSettingsService.getInstance().getApiUrl()
                .replace("/api", "/subscription");
        
        // Add user token if available
        if (authToken != null) {
            subscriptionUrl += "?token=" + authToken;
        }
        
        // Open the URL in the browser
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(subscriptionUrl));
            
            // Record the interaction
            recordPremiumPromoInteraction(currentStrategy, "clicked_subscribe");
        } catch (Exception e) {
            LOG.error("Error opening subscription page", e);
        }
    }
    
    /**
     * Authentication request.
     */
    private static class AuthRequest {
        private final String username;
        private final String password;
        
        public AuthRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
    }
    
    /**
     * Authentication response.
     */
    private static class AuthResponse {
        private String token;
        private String message;
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    /**
     * Subscription response.
     */
    private static class SubscriptionResponse {
        private SubscriptionPlan plan;
        private int apiCallsThisMonth;
        private int freeApiCallsLimit;
        
        public SubscriptionPlan getPlan() {
            return plan;
        }
        
        public void setPlan(SubscriptionPlan plan) {
            this.plan = plan;
        }
        
        public int getApiCallsThisMonth() {
            return apiCallsThisMonth;
        }
        
        public void setApiCallsThisMonth(int apiCallsThisMonth) {
            this.apiCallsThisMonth = apiCallsThisMonth;
        }
        
        public int getFreeApiCallsLimit() {
            return freeApiCallsLimit;
        }
        
        public void setFreeApiCallsLimit(int freeApiCallsLimit) {
            this.freeApiCallsLimit = freeApiCallsLimit;
        }
    }
    
    /**
     * Engagement event.
     */
    private static class EngagementEvent {
        private final String type;
        private final String data;
        private final long timestamp;
        
        public EngagementEvent(String type, String data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getType() {
            return type;
        }
        
        public String getData() {
            return data;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Marketing strategies.
     */
    public enum MarketingStrategy {
        NONE,           // No marketing strategy
        USAGE_LIMIT,    // User has reached usage limit
        FEATURE_BLOCKED,// User tried to access premium feature
        TIME_BASED,     // Periodic promotion
        SUCCESS_MOMENT  // Show during moment of success
    }
}