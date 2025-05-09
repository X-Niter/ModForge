package com.modforge.intellij.plugin.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a user profile.
 */
public class UserProfile {
    private String id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Date joinDate;
    private Date lastLoginDate;
    private SubscriptionPlan subscriptionPlan;
    private Date subscriptionEndDate;
    private int totalProjects;
    private int totalPatterns;
    private boolean isAdmin;
    private List<String> roles;
    private UserPreferences preferences;
    private UserAnalytics analytics;
    
    /**
     * Represents user preferences.
     */
    public static class UserPreferences {
        private boolean darkMode = true;
        private boolean syncEnabled = true;
        private boolean allowUsageTracking = true;
        private boolean allowMarketingEmails = false;
        private String defaultModLoader;
        private String defaultMinecraftVersion;
        private int patternMatchingThreshold = 70;
        
        public boolean isDarkMode() {
            return darkMode;
        }
        
        public void setDarkMode(boolean darkMode) {
            this.darkMode = darkMode;
        }
        
        public boolean isSyncEnabled() {
            return syncEnabled;
        }
        
        public void setSyncEnabled(boolean syncEnabled) {
            this.syncEnabled = syncEnabled;
        }
        
        public boolean isAllowUsageTracking() {
            return allowUsageTracking;
        }
        
        public void setAllowUsageTracking(boolean allowUsageTracking) {
            this.allowUsageTracking = allowUsageTracking;
        }
        
        public boolean isAllowMarketingEmails() {
            return allowMarketingEmails;
        }
        
        public void setAllowMarketingEmails(boolean allowMarketingEmails) {
            this.allowMarketingEmails = allowMarketingEmails;
        }
        
        public String getDefaultModLoader() {
            return defaultModLoader;
        }
        
        public void setDefaultModLoader(String defaultModLoader) {
            this.defaultModLoader = defaultModLoader;
        }
        
        public String getDefaultMinecraftVersion() {
            return defaultMinecraftVersion;
        }
        
        public void setDefaultMinecraftVersion(String defaultMinecraftVersion) {
            this.defaultMinecraftVersion = defaultMinecraftVersion;
        }
        
        public int getPatternMatchingThreshold() {
            return patternMatchingThreshold;
        }
        
        public void setPatternMatchingThreshold(int patternMatchingThreshold) {
            this.patternMatchingThreshold = patternMatchingThreshold;
        }
    }
    
    /**
     * Represents user analytics for marketing and personalization.
     */
    public static class UserAnalytics {
        private int totalLogins = 0;
        private int totalApiCalls = 0;
        private int codeGenerations = 0;
        private int errorResolutions = 0;
        private int jarAnalyses = 0;
        private long totalTimeSpentMs = 0;
        private List<String> favoriteFeaturesUsed = new ArrayList<>();
        private List<InteractionEvent> recentInteractions = new ArrayList<>();
        private double upgradePromptConversionRate = 0.0;
        private int upgradePromptsShown = 0;
        private int upgradePromptsClicked = 0;
        private String mostEffectivePromptType = "";
        private String preferredModLoader = "";
        private double patternMatchSuccessRate = 0.0;
        
        /**
         * Represents an interaction event.
         */
        public static class InteractionEvent {
            private String type;
            private String data;
            private long timestamp;
            
            public InteractionEvent() {
                this.timestamp = System.currentTimeMillis();
            }
            
            public InteractionEvent(String type, String data) {
                this();
                this.type = type;
                this.data = data;
            }
            
            public String getType() {
                return type;
            }
            
            public void setType(String type) {
                this.type = type;
            }
            
            public String getData() {
                return data;
            }
            
            public void setData(String data) {
                this.data = data;
            }
            
            public long getTimestamp() {
                return timestamp;
            }
            
            public void setTimestamp(long timestamp) {
                this.timestamp = timestamp;
            }
        }
        
        public int getTotalLogins() {
            return totalLogins;
        }
        
        public void setTotalLogins(int totalLogins) {
            this.totalLogins = totalLogins;
        }
        
        public void incrementLogins() {
            this.totalLogins++;
        }
        
        public int getTotalApiCalls() {
            return totalApiCalls;
        }
        
        public void setTotalApiCalls(int totalApiCalls) {
            this.totalApiCalls = totalApiCalls;
        }
        
        public void incrementApiCalls() {
            this.totalApiCalls++;
        }
        
        public int getCodeGenerations() {
            return codeGenerations;
        }
        
        public void setCodeGenerations(int codeGenerations) {
            this.codeGenerations = codeGenerations;
        }
        
        public void incrementCodeGenerations() {
            this.codeGenerations++;
        }
        
        public int getErrorResolutions() {
            return errorResolutions;
        }
        
        public void setErrorResolutions(int errorResolutions) {
            this.errorResolutions = errorResolutions;
        }
        
        public void incrementErrorResolutions() {
            this.errorResolutions++;
        }
        
        public int getJarAnalyses() {
            return jarAnalyses;
        }
        
        public void setJarAnalyses(int jarAnalyses) {
            this.jarAnalyses = jarAnalyses;
        }
        
        public void incrementJarAnalyses() {
            this.jarAnalyses++;
        }
        
        public long getTotalTimeSpentMs() {
            return totalTimeSpentMs;
        }
        
        public void setTotalTimeSpentMs(long totalTimeSpentMs) {
            this.totalTimeSpentMs = totalTimeSpentMs;
        }
        
        public void addTimeSpent(long timeSpentMs) {
            this.totalTimeSpentMs += timeSpentMs;
        }
        
        public List<String> getFavoriteFeaturesUsed() {
            return favoriteFeaturesUsed;
        }
        
        public void setFavoriteFeaturesUsed(List<String> favoriteFeaturesUsed) {
            this.favoriteFeaturesUsed = favoriteFeaturesUsed;
        }
        
        public void addFeatureUsed(String feature) {
            if (!this.favoriteFeaturesUsed.contains(feature)) {
                this.favoriteFeaturesUsed.add(feature);
            }
        }
        
        public List<InteractionEvent> getRecentInteractions() {
            return recentInteractions;
        }
        
        public void setRecentInteractions(List<InteractionEvent> recentInteractions) {
            this.recentInteractions = recentInteractions;
        }
        
        public void addInteraction(String type, String data) {
            this.recentInteractions.add(new InteractionEvent(type, data));
            // Keep only the 100 most recent interactions
            if (this.recentInteractions.size() > 100) {
                this.recentInteractions.remove(0);
            }
        }
        
        public double getUpgradePromptConversionRate() {
            return upgradePromptConversionRate;
        }
        
        public void setUpgradePromptConversionRate(double upgradePromptConversionRate) {
            this.upgradePromptConversionRate = upgradePromptConversionRate;
        }
        
        public void recalculateConversionRate() {
            if (upgradePromptsShown > 0) {
                this.upgradePromptConversionRate = (double) upgradePromptsClicked / upgradePromptsShown;
            }
        }
        
        public int getUpgradePromptsShown() {
            return upgradePromptsShown;
        }
        
        public void setUpgradePromptsShown(int upgradePromptsShown) {
            this.upgradePromptsShown = upgradePromptsShown;
        }
        
        public void incrementUpgradePromptsShown() {
            this.upgradePromptsShown++;
            recalculateConversionRate();
        }
        
        public int getUpgradePromptsClicked() {
            return upgradePromptsClicked;
        }
        
        public void setUpgradePromptsClicked(int upgradePromptsClicked) {
            this.upgradePromptsClicked = upgradePromptsClicked;
        }
        
        public void incrementUpgradePromptsClicked() {
            this.upgradePromptsClicked++;
            recalculateConversionRate();
        }
        
        public String getMostEffectivePromptType() {
            return mostEffectivePromptType;
        }
        
        public void setMostEffectivePromptType(String mostEffectivePromptType) {
            this.mostEffectivePromptType = mostEffectivePromptType;
        }
        
        public String getPreferredModLoader() {
            return preferredModLoader;
        }
        
        public void setPreferredModLoader(String preferredModLoader) {
            this.preferredModLoader = preferredModLoader;
        }
        
        public double getPatternMatchSuccessRate() {
            return patternMatchSuccessRate;
        }
        
        public void setPatternMatchSuccessRate(double patternMatchSuccessRate) {
            this.patternMatchSuccessRate = patternMatchSuccessRate;
        }
    }
    
    public UserProfile() {
        this.id = UUID.randomUUID().toString();
        this.joinDate = new Date();
        this.lastLoginDate = new Date();
        this.subscriptionPlan = SubscriptionPlan.FREE;
        this.roles = new ArrayList<>();
        this.preferences = new UserPreferences();
        this.analytics = new UserAnalytics();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName != null ? displayName : username;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public Date getJoinDate() {
        return joinDate;
    }
    
    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }
    
    public Date getLastLoginDate() {
        return lastLoginDate;
    }
    
    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    
    public void updateLastLoginDate() {
        this.lastLoginDate = new Date();
    }
    
    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }
    
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
    
    public Date getSubscriptionEndDate() {
        return subscriptionEndDate;
    }
    
    public void setSubscriptionEndDate(Date subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }
    
    public boolean isSubscriptionActive() {
        return subscriptionPlan != SubscriptionPlan.FREE || 
               (subscriptionEndDate != null && subscriptionEndDate.after(new Date()));
    }
    
    public int getTotalProjects() {
        return totalProjects;
    }
    
    public void setTotalProjects(int totalProjects) {
        this.totalProjects = totalProjects;
    }
    
    public int getTotalPatterns() {
        return totalPatterns;
    }
    
    public void setTotalPatterns(int totalPatterns) {
        this.totalPatterns = totalPatterns;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    
    public void addRole(String role) {
        if (!this.roles.contains(role)) {
            this.roles.add(role);
        }
    }
    
    public boolean hasRole(String role) {
        return this.roles.contains(role);
    }
    
    public UserPreferences getPreferences() {
        return preferences;
    }
    
    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }
    
    public UserAnalytics getAnalytics() {
        return analytics;
    }
    
    public void setAnalytics(UserAnalytics analytics) {
        this.analytics = analytics;
    }
}