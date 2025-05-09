package com.modforge.intellij.plugin.models;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User profile model containing information about the user.
 * This is synced with the ModForge backend and contains subscription information.
 */
public class UserProfile {
    @SerializedName("id")
    private int id;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("created_at")
    private Date createdAt;
    
    @SerializedName("subscription")
    private Subscription subscription;
    
    @SerializedName("analytics")
    private Analytics analytics;
    
    @SerializedName("preferences")
    private Preferences preferences;
    
    /**
     * Default constructor for Gson deserialization.
     */
    public UserProfile() {
        // Default constructor
    }
    
    /**
     * Checks if the user has a premium subscription.
     * @return Whether the user has a premium subscription
     */
    public boolean isPremium() {
        return subscription != null && subscription.isActive();
    }
    
    /**
     * Gets the user ID.
     * @return The user ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Gets the username.
     * @return The username
     */
    @NotNull
    public String getUsername() {
        return username != null ? username : "";
    }
    
    /**
     * Gets the email.
     * @return The email
     */
    @NotNull
    public String getEmail() {
        return email != null ? email : "";
    }
    
    /**
     * Gets the creation date.
     * @return The creation date
     */
    @Nullable
    public Date getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Gets the subscription information.
     * @return The subscription information
     */
    @Nullable
    public Subscription getSubscription() {
        return subscription;
    }
    
    /**
     * Gets the analytics information.
     * @return The analytics information
     */
    @NotNull
    public Analytics getAnalytics() {
        if (analytics == null) {
            analytics = new Analytics();
        }
        return analytics;
    }
    
    /**
     * Gets the user preferences.
     * @return The user preferences
     */
    @NotNull
    public Preferences getPreferences() {
        if (preferences == null) {
            preferences = new Preferences();
        }
        return preferences;
    }
    
    /**
     * Subscription information.
     */
    public static class Subscription {
        @SerializedName("plan")
        private String plan;
        
        @SerializedName("status")
        private String status;
        
        @SerializedName("expires_at")
        private Date expiresAt;
        
        @SerializedName("canceled")
        private boolean canceled;
        
        /**
         * Checks if the subscription is active.
         * @return Whether the subscription is active
         */
        public boolean isActive() {
            return "active".equals(status) && !canceled && (expiresAt == null || expiresAt.after(new Date()));
        }
        
        /**
         * Gets the subscription plan.
         * @return The subscription plan
         */
        @Nullable
        public String getPlan() {
            return plan;
        }
        
        /**
         * Gets the subscription status.
         * @return The subscription status
         */
        @Nullable
        public String getStatus() {
            return status;
        }
        
        /**
         * Gets the expiration date.
         * @return The expiration date
         */
        @Nullable
        public Date getExpiresAt() {
            return expiresAt;
        }
        
        /**
         * Checks if the subscription is canceled.
         * @return Whether the subscription is canceled
         */
        public boolean isCanceled() {
            return canceled;
        }
    }
    
    /**
     * Analytics information about the user's behavior.
     */
    public static class Analytics {
        @SerializedName("total_projects")
        private int totalProjects;
        
        @SerializedName("total_mods")
        private int totalMods;
        
        @SerializedName("total_jar_uploads")
        private int totalJarUploads;
        
        @SerializedName("total_code_generations")
        private int totalCodeGenerations;
        
        @SerializedName("total_error_resolutions")
        private int totalErrorResolutions;
        
        @SerializedName("favorite_features_used")
        private List<String> favoriteFeaturesUsed = new ArrayList<>();
        
        @SerializedName("preferred_mod_loader")
        private String preferredModLoader;
        
        @SerializedName("top_minecraft_versions")
        private List<String> topMinecraftVersions = new ArrayList<>();
        
        @SerializedName("last_active")
        private Date lastActive;
        
        /**
         * Gets the total number of projects.
         * @return The total number of projects
         */
        public int getTotalProjects() {
            return totalProjects;
        }
        
        /**
         * Gets the total number of mods.
         * @return The total number of mods
         */
        public int getTotalMods() {
            return totalMods;
        }
        
        /**
         * Gets the total number of JAR uploads.
         * @return The total number of JAR uploads
         */
        public int getTotalJarUploads() {
            return totalJarUploads;
        }
        
        /**
         * Gets the total number of code generations.
         * @return The total number of code generations
         */
        public int getTotalCodeGenerations() {
            return totalCodeGenerations;
        }
        
        /**
         * Gets the total number of error resolutions.
         * @return The total number of error resolutions
         */
        public int getTotalErrorResolutions() {
            return totalErrorResolutions;
        }
        
        /**
         * Gets the favorite features used.
         * @return The favorite features used
         */
        @NotNull
        public List<String> getFavoriteFeaturesUsed() {
            return favoriteFeaturesUsed != null ? favoriteFeaturesUsed : new ArrayList<>();
        }
        
        /**
         * Gets the preferred mod loader.
         * @return The preferred mod loader
         */
        @Nullable
        public String getPreferredModLoader() {
            return preferredModLoader;
        }
        
        /**
         * Gets the top Minecraft versions.
         * @return The top Minecraft versions
         */
        @NotNull
        public List<String> getTopMinecraftVersions() {
            return topMinecraftVersions != null ? topMinecraftVersions : new ArrayList<>();
        }
        
        /**
         * Gets the last active date.
         * @return The last active date
         */
        @Nullable
        public Date getLastActive() {
            return lastActive;
        }
    }
    
    /**
     * User preferences.
     */
    public static class Preferences {
        @SerializedName("theme")
        private String theme = "system";
        
        @SerializedName("code_style")
        private String codeStyle = "default";
        
        @SerializedName("auto_sync")
        private boolean autoSync = true;
        
        @SerializedName("notifications_enabled")
        private boolean notificationsEnabled = true;
        
        @SerializedName("marketing_emails")
        private boolean marketingEmails = false;
        
        /**
         * Gets the theme preference.
         * @return The theme preference
         */
        @NotNull
        public String getTheme() {
            return theme != null ? theme : "system";
        }
        
        /**
         * Gets the code style preference.
         * @return The code style preference
         */
        @NotNull
        public String getCodeStyle() {
            return codeStyle != null ? codeStyle : "default";
        }
        
        /**
         * Checks if auto sync is enabled.
         * @return Whether auto sync is enabled
         */
        public boolean isAutoSync() {
            return autoSync;
        }
        
        /**
         * Checks if notifications are enabled.
         * @return Whether notifications are enabled
         */
        public boolean isNotificationsEnabled() {
            return notificationsEnabled;
        }
        
        /**
         * Checks if marketing emails are enabled.
         * @return Whether marketing emails are enabled
         */
        public boolean isMarketingEmails() {
            return marketingEmails;
        }
    }
}