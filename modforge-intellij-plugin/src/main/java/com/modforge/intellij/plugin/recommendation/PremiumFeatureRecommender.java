package com.modforge.intellij.plugin.recommendation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.models.UserProfile;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import com.modforge.intellij.plugin.services.UserSubscriptionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Engine for recommending premium features based on user behavior and context.
 * Analyzes usage patterns to provide timely, relevant upgrade suggestions.
 */
public class PremiumFeatureRecommender {
    private static final Logger LOG = Logger.getInstance(PremiumFeatureRecommender.class);
    
    private final Project project;
    private final UserSubscriptionService subscriptionService;
    private final ExecutorService executorService;
    
    // Usage tracking
    private final Map<String, FeatureUsageData> featureUsage = new ConcurrentHashMap<>();
    private final List<RecommendationEvent> recentEvents = Collections.synchronizedList(new ArrayList<>());
    private final Map<RecommendationContext, Integer> contextHits = new ConcurrentHashMap<>();
    
    // State
    private int sessionEventCount = 0;
    private long sessionStartTime = System.currentTimeMillis();
    private long lastRecommendationTime = 0;
    private final int cooldownPeriodMinutes = 60; // Default cooldown between recommendations
    
    // Configuration
    private double conversionThreshold = 0.4; // Minimum similarity threshold for recommendations
    private int maxRecommendationsPerSession = 3;
    private int recommendationCount = 0;
    
    // Learning coefficients
    private double recencyWeight = 0.7;
    private double frequencyWeight = 0.3;
    private double similarityBoost = 1.5;
    
    // Recommendation templates
    private static final Map<String, PremiumFeature> PREMIUM_FEATURES = new HashMap<>();
    
    static {
        // Initialize premium features with descriptions and contexts
        PREMIUM_FEATURES.put("advanced_error_resolution", new PremiumFeature(
                "Advanced Error Resolution",
                "Unleash the full power of our ML-based error resolution system with access to enhanced pattern matching and priority processing.",
                "Resolve compilation errors 3x faster with our premium error resolution engine.",
                Set.of(RecommendationContext.ERROR_RESOLUTION, RecommendationContext.BUILD_FAILURE),
                "error_resolution_demo.gif"
        ));
        
        PREMIUM_FEATURES.put("unlimited_jar_analysis", new PremiumFeature(
                "Unlimited JAR Analysis",
                "Analyze unlimited mod JARs to extract patterns, APIs, and examples for your own projects.",
                "Upgrade to analyze unlimited mod JARs and supercharge your learning database.",
                Set.of(RecommendationContext.JAR_ANALYSIS, RecommendationContext.KNOWLEDGE_BASE),
                "jar_analysis_comparison.png"
        ));
        
        PREMIUM_FEATURES.put("remote_testing", new PremiumFeature(
                "Remote Build & Test",
                "Test your mods automatically in the cloud across multiple Minecraft versions and mod loaders.",
                "Save hours of testing time with our cloud-based testing infrastructure.",
                Set.of(RecommendationContext.BUILD_SUCCESS, RecommendationContext.TESTING),
                "remote_testing_dashboard.png"
        ));
        
        PREMIUM_FEATURES.put("enhanced_code_generation", new PremiumFeature(
                "Enhanced Code Generation",
                "Generate complex mod components with advanced AI models and priority processing.",
                "Premium members generate code 50% faster with our enhanced AI models.",
                Set.of(RecommendationContext.CODE_GENERATION, RecommendationContext.FEATURE_ADDITION),
                "code_generation_comparison.gif"
        ));
        
        PREMIUM_FEATURES.put("automatic_documentation", new PremiumFeature(
                "Automatic Documentation",
                "Automatically generate comprehensive documentation for your mod, including wiki pages and README files.",
                "Let AI handle your documentation so you can focus on coding.",
                Set.of(RecommendationContext.DOCUMENTATION, RecommendationContext.PROJECT_COMPLETION),
                "auto_documentation_demo.png"
        ));
        
        PREMIUM_FEATURES.put("pattern_sharing", new PremiumFeature(
                "Community Pattern Sharing",
                "Access and contribute to our community pattern database with thousands of solutions to common problems.",
                "Join our premium community and leverage the collective knowledge of thousands of modders.",
                Set.of(RecommendationContext.PATTERN_LEARNING, RecommendationContext.COMMUNITY),
                "pattern_sharing_network.png"
        ));
    }
    
    /**
     * Creates a new PremiumFeatureRecommender.
     * @param project The project
     */
    public PremiumFeatureRecommender(@NotNull Project project) {
        this.project = project;
        this.subscriptionService = UserSubscriptionService.getInstance();
        this.executorService = AppExecutorUtil.getAppExecutorService();
        
        // Schedule regular analysis of usage patterns
        AppExecutorUtil.getAppScheduledExecutorService().scheduleAtFixedRate(
                this::analyzeUsagePatterns,
                30,
                30,
                TimeUnit.MINUTES
        );
    }
    
    /**
     * Records feature usage to improve recommendation quality.
     * @param featureId The feature ID
     * @param context The context in which the feature was used
     */
    public void recordFeatureUsage(@NotNull String featureId, @NotNull RecommendationContext context) {
        if (subscriptionService.isPremium()) {
            return; // No need to track for premium users
        }
        
        // Update feature usage data
        FeatureUsageData usageData = featureUsage.computeIfAbsent(featureId, k -> new FeatureUsageData(featureId));
        usageData.recordUsage(context);
        
        // Add to recent events
        recentEvents.add(new RecommendationEvent(featureId, context, System.currentTimeMillis()));
        
        // Update context hits
        contextHits.merge(context, 1, Integer::sum);
        
        // Update session stats
        sessionEventCount++;
        
        // Trim events list if it gets too large
        if (recentEvents.size() > 100) {
            synchronized (recentEvents) {
                if (recentEvents.size() > 100) {
                    recentEvents.subList(0, recentEvents.size() - 100).clear();
                }
            }
        }
        
        LOG.debug("Recorded feature usage: " + featureId + " in context " + context);
    }
    
    /**
     * Gets a recommendation for the current context.
     * @param currentContext The current context
     * @return A recommended premium feature, or null if no recommendation is available
     */
    @Nullable
    public PremiumFeature getRecommendation(@NotNull RecommendationContext currentContext) {
        if (subscriptionService.isPremium()) {
            return null; // No recommendations for premium users
        }
        
        // Check if we should show a recommendation
        if (!shouldShowRecommendation(currentContext)) {
            return null;
        }
        
        // Get the best recommendation for this context
        PremiumFeature recommendation = findBestRecommendation(currentContext);
        
        if (recommendation != null) {
            // Update state
            lastRecommendationTime = System.currentTimeMillis();
            recommendationCount++;
            
            // Record the recommendation event
            recentEvents.add(new RecommendationEvent(
                    recommendation.getId(),
                    currentContext,
                    System.currentTimeMillis(),
                    true
            ));
            
            // Record in subscription service for analytics
            subscriptionService.recordPremiumPromoImpression(
                    UserSubscriptionService.MarketingStrategy.FEATURE_BLOCKED
            );
            
            LOG.info("Recommending premium feature: " + recommendation.getTitle());
        }
        
        return recommendation;
    }
    
    /**
     * Determines whether a recommendation should be shown in the current context.
     * @param context The current context
     * @return Whether a recommendation should be shown
     */
    private boolean shouldShowRecommendation(@NotNull RecommendationContext context) {
        // Check if we've shown too many recommendations this session
        if (recommendationCount >= maxRecommendationsPerSession) {
            LOG.debug("Not showing recommendation: maximum per session reached");
            return false;
        }
        
        // Check cooldown period
        long timeSinceLastRecommendation = System.currentTimeMillis() - lastRecommendationTime;
        if (timeSinceLastRecommendation < cooldownPeriodMinutes * 60 * 1000) {
            LOG.debug("Not showing recommendation: in cooldown period");
            return false;
        }
        
        // Check if we've just recently shown a recommendation in this context
        boolean recentlyShownInContext = recentEvents.stream()
                .filter(e -> e.isRecommendation && e.context == context)
                .anyMatch(e -> System.currentTimeMillis() - e.timestamp < 24 * 60 * 60 * 1000);
                
        if (recentlyShownInContext) {
            LOG.debug("Not showing recommendation: recently shown in this context");
            return false;
        }
        
        // Let the subscription service decide based on its own logic
        boolean subscriptionServiceApproval = subscriptionService.considerShowingPremiumPromotion(
                UserSubscriptionService.MarketingStrategy.FEATURE_BLOCKED
        );
        
        if (!subscriptionServiceApproval) {
            LOG.debug("Not showing recommendation: subscription service declined");
            return false;
        }
        
        return true;
    }
    
    /**
     * Finds the best recommendation for the current context.
     * @param currentContext The current context
     * @return The best recommendation, or null if no recommendation is available
     */
    @Nullable
    private PremiumFeature findBestRecommendation(@NotNull RecommendationContext currentContext) {
        List<ScoredFeature> scoredFeatures = new ArrayList<>();
        
        // Score all premium features for this context
        for (PremiumFeature feature : PREMIUM_FEATURES.values()) {
            double score = calculateRecommendationScore(feature, currentContext);
            if (score > 0) {
                scoredFeatures.add(new ScoredFeature(feature, score));
            }
        }
        
        // Sort by score
        Collections.sort(scoredFeatures);
        
        // Return the highest-scoring feature
        if (!scoredFeatures.isEmpty() && scoredFeatures.get(0).score >= conversionThreshold) {
            return scoredFeatures.get(0).feature;
        }
        
        return null;
    }
    
    /**
     * Calculates a recommendation score for a feature in the current context.
     * @param feature The feature
     * @param currentContext The current context
     * @return The recommendation score
     */
    private double calculateRecommendationScore(@NotNull PremiumFeature feature, @NotNull RecommendationContext currentContext) {
        // Base score is 0
        double score = 0.0;
        
        // If the feature is directly relevant to the current context, boost the score
        if (feature.getRelevantContexts().contains(currentContext)) {
            score += 0.5;
        }
        
        // Add score based on recent usage
        double recencyScore = calculateRecencyScore(feature);
        score += recencyWeight * recencyScore;
        
        // Add score based on usage frequency
        double frequencyScore = calculateFrequencyScore(feature);
        score += frequencyWeight * frequencyScore;
        
        // Add score based on user profile similarity
        UserProfile userProfile = subscriptionService.getUserProfile();
        if (userProfile != null) {
            double similarityScore = calculateSimilarityScore(feature, userProfile);
            score += similarityBoost * similarityScore;
        }
        
        LOG.debug("Feature " + feature.getTitle() + " scored " + score + " for context " + currentContext);
        return score;
    }
    
    /**
     * Calculates a recency score for a feature based on recent usage.
     * @param feature The feature
     * @return The recency score
     */
    private double calculateRecencyScore(@NotNull PremiumFeature feature) {
        String featureId = feature.getId();
        FeatureUsageData usageData = featureUsage.get(featureId);
        if (usageData == null || usageData.getLastUsed() == 0) {
            return 0.0;
        }
        
        // Calculate how recently the feature was used (0-1, higher is more recent)
        long timeSinceLastUse = System.currentTimeMillis() - usageData.getLastUsed();
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        return Math.max(0.0, 1.0 - ((double) timeSinceLastUse / sessionDuration));
    }
    
    /**
     * Calculates a frequency score for a feature based on usage frequency.
     * @param feature The feature
     * @return The frequency score
     */
    private double calculateFrequencyScore(@NotNull PremiumFeature feature) {
        String featureId = feature.getId();
        FeatureUsageData usageData = featureUsage.get(featureId);
        if (usageData == null || usageData.getUsageCount() == 0) {
            return 0.0;
        }
        
        // Calculate how frequently the feature is used (0-1, higher is more frequent)
        if (sessionEventCount == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) usageData.getUsageCount() / sessionEventCount);
    }
    
    /**
     * Calculates a similarity score for a feature based on user profile similarity.
     * @param feature The feature
     * @param userProfile The user profile
     * @return The similarity score
     */
    private double calculateSimilarityScore(@NotNull PremiumFeature feature, @NotNull UserProfile userProfile) {
        // This is a simplified implementation. In a real system, you would use more sophisticated
        // similarity metrics based on the user's profile, behaviors, and preferences.
        double score = 0.0;
        
        // Check if this feature is related to the user's favorite features
        for (String favoriteFeature : userProfile.getAnalytics().getFavoriteFeaturesUsed()) {
            if (feature.getId().contains(favoriteFeature) || favoriteFeature.contains(feature.getId())) {
                score += 0.3;
                break;
            }
        }
        
        // Check if this feature is related to the user's preferred mod loader
        String preferredModLoader = userProfile.getAnalytics().getPreferredModLoader();
        if (preferredModLoader != null && !preferredModLoader.isEmpty()) {
            // If we had mod loader specific features, we would adjust score here
        }
        
        return score;
    }
    
    /**
     * Analyzes usage patterns to improve recommendation quality.
     */
    private void analyzeUsagePatterns() {
        if (subscriptionService.isPremium()) {
            return; // No need to analyze for premium users
        }
        
        LOG.info("Analyzing usage patterns for premium feature recommendations");
        
        try {
            // Identify the most active contexts
            Map<RecommendationContext, Integer> sortedContexts = new LinkedHashMap<>();
            contextHits.entrySet().stream()
                    .sorted(Map.Entry.<RecommendationContext, Integer>comparingByValue().reversed())
                    .forEachOrdered(e -> sortedContexts.put(e.getKey(), e.getValue()));
            
            // Identify usage patterns
            List<String> mostUsedFeatures = new ArrayList<>();
            featureUsage.values().stream()
                    .sorted(Comparator.comparingInt(FeatureUsageData::getUsageCount).reversed())
                    .limit(5)
                    .forEach(data -> mostUsedFeatures.add(data.getFeatureId()));
            
            // Adjust recommendation parameters based on patterns
            if (!sortedContexts.isEmpty()) {
                // Adjust cooldown period based on user activity
                int totalHits = sortedContexts.values().stream().mapToInt(Integer::intValue).sum();
                if (totalHits > 50) {
                    // More active users get more space between recommendations
                    maxRecommendationsPerSession = 2;
                } else if (totalHits < 10) {
                    // Less active users get more recommendations
                    maxRecommendationsPerSession = 4;
                }
                
                // Adjust weights based on user behavior patterns
                if (recentEvents.size() > 20) {
                    // If user has many recent events, focus more on recency
                    recencyWeight = 0.8;
                    frequencyWeight = 0.2;
                } else {
                    // Otherwise, balanced approach
                    recencyWeight = 0.5;
                    frequencyWeight = 0.5;
                }
            }
            
            LOG.info("Updated recommendation parameters based on usage patterns");
        } catch (Exception e) {
            LOG.error("Error analyzing usage patterns", e);
        }
    }
    
    /**
     * Records a user interaction with a recommendation.
     * @param feature The recommended feature
     * @param action The action taken (e.g., "clicked", "dismissed")
     */
    public void recordRecommendationInteraction(@NotNull PremiumFeature feature, @NotNull String action) {
        LOG.info("User " + action + " recommendation for feature: " + feature.getTitle());
        
        // Record in subscription service for analytics
        subscriptionService.recordPremiumPromoInteraction(
                UserSubscriptionService.MarketingStrategy.FEATURE_BLOCKED,
                action
        );
        
        // If the user clicked to learn more, note this as a positive signal
        if ("clicked".equals(action)) {
            // This feature is interesting to the user - increase its weighting
            FeatureUsageData usageData = featureUsage.computeIfAbsent(
                    feature.getId(), k -> new FeatureUsageData(feature.getId())
            );
            usageData.recordInteraction(true);
        } else if ("dismissed".equals(action)) {
            // This feature is less interesting - decrease its weighting
            FeatureUsageData usageData = featureUsage.get(feature.getId());
            if (usageData != null) {
                usageData.recordInteraction(false);
            }
        }
    }
    
    /**
     * Gets a premium feature by ID.
     * @param featureId The feature ID
     * @return The premium feature, or null if not found
     */
    @Nullable
    public PremiumFeature getFeatureById(@NotNull String featureId) {
        return PREMIUM_FEATURES.get(featureId);
    }
    
    /**
     * Gets all available premium features.
     * @return All premium features
     */
    @NotNull
    public Collection<PremiumFeature> getAllFeatures() {
        return PREMIUM_FEATURES.values();
    }
    
    /**
     * Contexts in which premium features can be recommended.
     */
    public enum RecommendationContext {
        ERROR_RESOLUTION,
        BUILD_FAILURE,
        BUILD_SUCCESS,
        CODE_GENERATION,
        JAR_ANALYSIS,
        KNOWLEDGE_BASE,
        TESTING,
        FEATURE_ADDITION,
        DOCUMENTATION,
        PROJECT_COMPLETION,
        PATTERN_LEARNING,
        COMMUNITY,
        IDE_START,
        GENERAL
    }
    
    /**
     * Class for tracking feature usage data.
     */
    private static class FeatureUsageData {
        private final String featureId;
        private int usageCount = 0;
        private final Map<RecommendationContext, Integer> contextCounts = new HashMap<>();
        private long lastUsed = 0;
        private int positiveInteractions = 0;
        private int negativeInteractions = 0;
        
        public FeatureUsageData(String featureId) {
            this.featureId = featureId;
        }
        
        public void recordUsage(RecommendationContext context) {
            usageCount++;
            contextCounts.merge(context, 1, Integer::sum);
            lastUsed = System.currentTimeMillis();
        }
        
        public void recordInteraction(boolean positive) {
            if (positive) {
                positiveInteractions++;
            } else {
                negativeInteractions++;
            }
        }
        
        public String getFeatureId() {
            return featureId;
        }
        
        public int getUsageCount() {
            return usageCount;
        }
        
        public Map<RecommendationContext, Integer> getContextCounts() {
            return contextCounts;
        }
        
        public long getLastUsed() {
            return lastUsed;
        }
        
        public double getInteractionScore() {
            int total = positiveInteractions + negativeInteractions;
            if (total == 0) {
                return 0.5; // Neutral score
            }
            return (double) positiveInteractions / total;
        }
    }
    
    /**
     * Class for tracking recommendation events.
     */
    private static class RecommendationEvent {
        private final String featureId;
        private final RecommendationContext context;
        private final long timestamp;
        private final boolean isRecommendation;
        
        public RecommendationEvent(String featureId, RecommendationContext context, long timestamp) {
            this(featureId, context, timestamp, false);
        }
        
        public RecommendationEvent(String featureId, RecommendationContext context, long timestamp, boolean isRecommendation) {
            this.featureId = featureId;
            this.context = context;
            this.timestamp = timestamp;
            this.isRecommendation = isRecommendation;
        }
    }
    
    /**
     * Class for scoring features for recommendation.
     */
    private static class ScoredFeature implements Comparable<ScoredFeature> {
        private final PremiumFeature feature;
        private final double score;
        
        public ScoredFeature(PremiumFeature feature, double score) {
            this.feature = feature;
            this.score = score;
        }
        
        @Override
        public int compareTo(ScoredFeature other) {
            return Double.compare(other.score, this.score);
        }
    }
}