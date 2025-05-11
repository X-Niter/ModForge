package com.modforge.intellij.plugin.designers.advancement.models;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Model class for Minecraft advancements
 * Represents a complete advancement tree or individual advancement
 */
public class AdvancementModel {
    // Identification
    private String id;
    private String name;
    private String description;
    
    // Display properties
    private AdvancementFrameType frameType = AdvancementFrameType.TASK;
    private String iconItem;
    private String background;
    private boolean showToast = true;
    private boolean announceToChat = true;
    private boolean hidden = false;
    
    // Criteria
    private final List<AdvancementCriterion> criteria = new ArrayList<>();
    
    // Requirements
    private final List<List<String>> requirements = new ArrayList<>();
    
    // Parent-child relationships
    private String parentId;
    
    @Transient
    private AdvancementModel parent;
    
    @Transient
    private final List<AdvancementModel> children = new ArrayList<>();
    
    /**
     * Default constructor for serialization
     */
    public AdvancementModel() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new advancement model with the given ID
     * 
     * @param id The advancement ID
     */
    public AdvancementModel(@NotNull String id) {
        this.id = id;
    }
    
    /**
     * Create a new advancement model with the given ID and display name
     * 
     * @param id The advancement ID
     * @param name The display name
     */
    public AdvancementModel(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }
    
    /**
     * Add a child advancement to this advancement
     * 
     * @param child The child advancement
     */
    public void addChild(@NotNull AdvancementModel child) {
        if (!children.contains(child)) {
            children.add(child);
            child.setParent(this);
        }
    }
    
    /**
     * Remove a child advancement from this advancement
     * 
     * @param child The child advancement to remove
     * @return True if the child was removed, false if it wasn't a child
     */
    public boolean removeChild(@NotNull AdvancementModel child) {
        boolean removed = children.remove(child);
        if (removed) {
            child.setParent(null);
        }
        return removed;
    }
    
    /**
     * Add a criterion to this advancement
     * 
     * @param criterion The criterion to add
     */
    public void addCriterion(@NotNull AdvancementCriterion criterion) {
        if (!criteria.contains(criterion)) {
            criteria.add(criterion);
        }
    }
    
    /**
     * Remove a criterion from this advancement
     * 
     * @param criterion The criterion to remove
     * @return True if the criterion was removed, false if it wasn't present
     */
    public boolean removeCriterion(@NotNull AdvancementCriterion criterion) {
        return criteria.remove(criterion);
    }
    
    /**
     * Generate the requirements for this advancement based on criteria
     */
    public void generateRequirements() {
        requirements.clear();
        
        // For simple advancements, just require all criteria
        List<String> allCriteria = new ArrayList<>();
        for (AdvancementCriterion criterion : criteria) {
            allCriteria.add(criterion.getId());
        }
        
        if (!allCriteria.isEmpty()) {
            requirements.add(allCriteria);
        }
    }
    
    /**
     * Convert this advancement model to JSON
     * 
     * @return JSON string representation
     */
    public String toJson() {
        // This is a simplified version, a real implementation would use a JSON library
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Display
        json.append("  \"display\": {\n");
        json.append("    \"icon\": {\n");
        json.append("      \"item\": \"").append(iconItem).append("\"\n");
        json.append("    },\n");
        json.append("    \"title\": {\"text\": \"").append(name).append("\"},\n");
        json.append("    \"description\": {\"text\": \"").append(description).append("\"},\n");
        json.append("    \"frame\": \"").append(frameType.name().toLowerCase()).append("\",\n");
        
        if (background != null && !background.isEmpty()) {
            json.append("    \"background\": \"").append(background).append("\",\n");
        }
        
        json.append("    \"show_toast\": ").append(showToast).append(",\n");
        json.append("    \"announce_to_chat\": ").append(announceToChat).append(",\n");
        json.append("    \"hidden\": ").append(hidden).append("\n");
        json.append("  },\n");
        
        // Parent
        if (parentId != null && !parentId.isEmpty()) {
            json.append("  \"parent\": \"").append(parentId).append("\",\n");
        }
        
        // Criteria
        json.append("  \"criteria\": {\n");
        for (int i = 0; i < criteria.size(); i++) {
            AdvancementCriterion criterion = criteria.get(i);
            json.append("    \"").append(criterion.getId()).append("\": {\n");
            json.append("      \"trigger\": \"").append(criterion.getTrigger()).append("\"");
            
            // Add conditions if present
            if (criterion.getConditions() != null && !criterion.getConditions().isEmpty()) {
                json.append(",\n      \"conditions\": ").append(criterion.getConditions());
            }
            
            json.append("\n    }");
            if (i < criteria.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  }");
        
        // Requirements
        if (!requirements.isEmpty()) {
            json.append(",\n  \"requirements\": [\n");
            for (int i = 0; i < requirements.size(); i++) {
                List<String> reqGroup = requirements.get(i);
                json.append("    [");
                for (int j = 0; j < reqGroup.size(); j++) {
                    json.append("\"").append(reqGroup.get(j)).append("\"");
                    if (j < reqGroup.size() - 1) {
                        json.append(", ");
                    }
                }
                json.append("]");
                if (i < requirements.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public AdvancementFrameType getFrameType() {
        return frameType;
    }
    
    public void setFrameType(AdvancementFrameType frameType) {
        this.frameType = frameType;
    }
    
    public String getIconItem() {
        return iconItem;
    }
    
    public void setIconItem(String iconItem) {
        this.iconItem = iconItem;
    }
    
    public String getBackground() {
        return background;
    }
    
    public void setBackground(String background) {
        this.background = background;
    }
    
    public boolean isShowToast() {
        return showToast;
    }
    
    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }
    
    public boolean isAnnounceToChat() {
        return announceToChat;
    }
    
    public void setAnnounceToChat(boolean announceToChat) {
        this.announceToChat = announceToChat;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public List<AdvancementCriterion> getCriteria() {
        return new ArrayList<>(criteria);
    }
    
    public List<List<String>> getRequirements() {
        return new ArrayList<>(requirements);
    }
    
    public void setRequirements(List<List<String>> requirements) {
        this.requirements.clear();
        this.requirements.addAll(requirements);
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    
    public AdvancementModel getParent() {
        return parent;
    }
    
    public void setParent(AdvancementModel parent) {
        this.parent = parent;
        this.parentId = parent != null ? parent.getId() : null;
    }
    
    public List<AdvancementModel> getChildren() {
        return new ArrayList<>(children);
    }
    
    /**
     * Enum for different advancement frame types
     */
    public enum AdvancementFrameType {
        TASK,
        CHALLENGE,
        GOAL
    }
}