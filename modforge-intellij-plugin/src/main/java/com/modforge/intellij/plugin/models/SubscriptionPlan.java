package com.modforge.intellij.plugin.models;

/**
 * Enum representing the different subscription plans.
 */
public enum SubscriptionPlan {
    FREE("Free"),
    PREMIUM("Premium"),
    ENTERPRISE("Enterprise");
    
    private final String displayName;
    
    SubscriptionPlan(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the API call limit for this plan.
     * @return The API call limit
     */
    public int getApiCallLimit() {
        switch (this) {
            case FREE:
                return 100;
            case PREMIUM:
                return Integer.MAX_VALUE;
            case ENTERPRISE:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }
    
    /**
     * Gets the pattern sync limit for this plan.
     * @return The pattern sync limit
     */
    public int getPatternSyncLimit() {
        switch (this) {
            case FREE:
                return 10;
            case PREMIUM:
                return 100;
            case ENTERPRISE:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }
    
    /**
     * Gets the JAR analysis limit for this plan.
     * @return The JAR analysis limit
     */
    public int getJarAnalysisLimit() {
        switch (this) {
            case FREE:
                return 2;
            case PREMIUM:
                return 20;
            case ENTERPRISE:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }
    
    /**
     * Gets whether this plan has access to remote testing.
     * @return Whether this plan has access to remote testing
     */
    public boolean hasRemoteTesting() {
        return this == PREMIUM || this == ENTERPRISE;
    }
    
    /**
     * Gets whether this plan has access to priority support.
     * @return Whether this plan has access to priority support
     */
    public boolean hasPrioritySupport() {
        return this == PREMIUM || this == ENTERPRISE;
    }
    
    /**
     * Gets whether this plan has access to enhanced pattern learning.
     * @return Whether this plan has access to enhanced pattern learning
     */
    public boolean hasEnhancedPatternLearning() {
        return this == PREMIUM || this == ENTERPRISE;
    }
    
    /**
     * Gets whether this plan has access to the team collaboration features.
     * @return Whether this plan has access to the team collaboration features
     */
    public boolean hasTeamCollaboration() {
        return this == ENTERPRISE;
    }
    
    /**
     * Gets whether this plan has access to the white-label deployment features.
     * @return Whether this plan has access to the white-label deployment features
     */
    public boolean hasWhiteLabelDeployment() {
        return this == ENTERPRISE;
    }
    
    /**
     * Gets whether this plan has access to the custom API integration features.
     * @return Whether this plan has access to the custom API integration features
     */
    public boolean hasCustomApiIntegration() {
        return this == ENTERPRISE;
    }
}