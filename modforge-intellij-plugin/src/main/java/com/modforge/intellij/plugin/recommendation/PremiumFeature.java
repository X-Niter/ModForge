package com.modforge.intellij.plugin.recommendation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a premium feature that can be recommended to users.
 * Contains information about the feature, its benefits, and relevant contexts.
 */
public class PremiumFeature {
    private final String id;
    private final String title;
    private final String description;
    private final String shortPitch;
    private final Set<PremiumFeatureRecommender.RecommendationContext> relevantContexts;
    private final String imageAsset;
    
    /**
     * Creates a new premium feature.
     * @param title The feature title
     * @param description The feature description
     * @param shortPitch A short pitch for the feature
     * @param relevantContexts Contexts in which this feature is relevant
     * @param imageAsset The image asset for the feature
     */
    public PremiumFeature(
            @NotNull String title,
            @NotNull String description,
            @NotNull String shortPitch,
            @NotNull Set<PremiumFeatureRecommender.RecommendationContext> relevantContexts,
            @Nullable String imageAsset
    ) {
        // Generate ID from title (convert to lowercase and replace spaces with underscores)
        this.id = title.toLowerCase().replace(' ', '_')
                .replaceAll("[^a-z0-9_]", "");
        
        this.title = title;
        this.description = description;
        this.shortPitch = shortPitch;
        this.relevantContexts = relevantContexts;
        this.imageAsset = imageAsset;
    }
    
    /**
     * Gets the feature ID.
     * @return The feature ID
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Gets the feature title.
     * @return The feature title
     */
    @NotNull
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the feature description.
     * @return The feature description
     */
    @NotNull
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the short pitch for the feature.
     * @return The short pitch
     */
    @NotNull
    public String getShortPitch() {
        return shortPitch;
    }
    
    /**
     * Gets the contexts in which this feature is relevant.
     * @return The relevant contexts
     */
    @NotNull
    public Set<PremiumFeatureRecommender.RecommendationContext> getRelevantContexts() {
        return relevantContexts;
    }
    
    /**
     * Gets the image asset for the feature.
     * @return The image asset
     */
    @Nullable
    public String getImageAsset() {
        return imageAsset;
    }
    
    /**
     * Gets the feature details as HTML.
     * @return The feature details as HTML
     */
    @NotNull
    public String getDetailsHtml() {
        StringBuilder html = new StringBuilder();
        
        html.append("<html><body>");
        html.append("<h2>").append(title).append("</h2>");
        
        if (imageAsset != null && !imageAsset.isEmpty()) {
            html.append("<img src='/icons/").append(imageAsset).append("' width='300' /><br/>");
        }
        
        html.append("<p><strong>").append(shortPitch).append("</strong></p>");
        html.append("<p>").append(description).append("</p>");
        
        html.append("<h3>Key Benefits:</h3>");
        html.append("<ul>");
        
        // Add benefits based on the feature ID
        if (id.contains("error_resolution")) {
            html.append("<li>Fix compilation errors 3x faster with AI-powered solutions</li>");
            html.append("<li>Access our exclusive pattern database with thousands of error solutions</li>");
            html.append("<li>Priority processing ensures you get fixes instantly</li>");
        } else if (id.contains("jar_analysis")) {
            html.append("<li>Analyze unlimited JARs vs. only 2 on the free plan</li>");
            html.append("<li>Extract patterns and APIs from any Minecraft mod</li>");
            html.append("<li>Automatically discover best practices from top mods</li>");
        } else if (id.contains("remote_testing")) {
            html.append("<li>Test your mods across multiple Minecraft versions automatically</li>");
            html.append("<li>Get detailed compatibility reports for different mod loaders</li>");
            html.append("<li>Save hours of manual testing and debugging time</li>");
        } else if (id.contains("code_generation")) {
            html.append("<li>Generate complex mod components with a single prompt</li>");
            html.append("<li>Access advanced AI models for better quality code</li>");
            html.append("<li>Priority processing ensures faster generations</li>");
        } else if (id.contains("documentation")) {
            html.append("<li>Generate comprehensive documentation automatically</li>");
            html.append("<li>Create wiki pages and README files with a single click</li>");
            html.append("<li>Keep documentation in sync with your code</li>");
        } else if (id.contains("pattern_sharing")) {
            html.append("<li>Access pattern database with thousands of community solutions</li>");
            html.append("<li>Share your patterns with other premium members</li>");
            html.append("<li>Earn reputation by contributing useful patterns</li>");
        } else {
            html.append("<li>Unlock the full power of ModForge AI</li>");
            html.append("<li>Get priority processing for all AI operations</li>");
            html.append("<li>Access exclusive premium-only features</li>");
        }
        
        html.append("</ul>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Gets a premium badge HTML indicating this is a premium feature.
     * @return The premium badge HTML
     */
    @NotNull
    public String getPremiumBadgeHtml() {
        return "<span style='background-color: #FFD700; color: #000; padding: 2px 5px; border-radius: 3px; font-size: 10px; font-weight: bold;'>PREMIUM</span>";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PremiumFeature that = (PremiumFeature) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return title;
    }
}