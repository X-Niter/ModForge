package com.modforge.intellij.plugin.designers.advancement.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Model class for advancement criteria
 * Represents a single criterion for an advancement
 */
public class AdvancementCriterion {
    private String id;
    private String trigger;
    private String conditions;
    
    /**
     * Default constructor for serialization
     */
    public AdvancementCriterion() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new advancement criterion
     * 
     * @param id The criterion ID
     * @param trigger The trigger type
     */
    public AdvancementCriterion(@NotNull String id, @NotNull String trigger) {
        this.id = id;
        this.trigger = trigger;
    }
    
    /**
     * Create a new advancement criterion with conditions
     * 
     * @param id The criterion ID
     * @param trigger The trigger type
     * @param conditions JSON string of conditions
     */
    public AdvancementCriterion(@NotNull String id, @NotNull String trigger, @Nullable String conditions) {
        this.id = id;
        this.trigger = trigger;
        this.conditions = conditions;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTrigger() {
        return trigger;
    }
    
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }
    
    public String getConditions() {
        return conditions;
    }
    
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdvancementCriterion that = (AdvancementCriterion) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Criterion{" +
                "id='" + id + '\'' +
                ", trigger='" + trigger + '\'' +
                '}';
    }
}