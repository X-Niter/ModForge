package com.modforge.intellij.plugin.templates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a template for a Minecraft mod.
 */
public class ModTemplate {
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final ModTemplateType type;
    private final Map<String, String> files;
    private final Map<String, String> variables;
    
    /**
     * Create a new mod template.
     *
     * @param id          The template ID
     * @param name        The template name
     * @param description The template description
     * @param category    The template category
     * @param type        The template type
     * @param files       The template files, with file paths as keys and content as values
     * @param variables   The template variables, with variable names as keys and default values as values
     */
    public ModTemplate(
            @NotNull String id,
            @NotNull String name,
            @NotNull String description,
            @NotNull String category,
            @NotNull ModTemplateType type,
            @NotNull Map<String, String> files,
            @NotNull Map<String, String> variables
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.type = type;
        this.files = Collections.unmodifiableMap(files);
        this.variables = Collections.unmodifiableMap(variables);
    }
    
    /**
     * Get the template ID.
     *
     * @return The template ID
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Get the template name.
     *
     * @return The template name
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * Get the template description.
     *
     * @return The template description
     */
    @NotNull
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the template category.
     *
     * @return The template category
     */
    @NotNull
    public String getCategory() {
        return category;
    }
    
    /**
     * Get the template type.
     *
     * @return The template type
     */
    @NotNull
    public ModTemplateType getType() {
        return type;
    }
    
    /**
     * Get the template files.
     *
     * @return The template files
     */
    @NotNull
    public Map<String, String> getFiles() {
        return files;
    }
    
    /**
     * Get the template variables.
     *
     * @return The template variables
     */
    @NotNull
    public Map<String, String> getVariables() {
        return variables;
    }
    
    /**
     * Get a file from the template.
     *
     * @param path The file path
     * @return The file content, or null if the file doesn't exist
     */
    @Nullable
    public String getFile(@NotNull String path) {
        return files.get(path);
    }
    
    /**
     * Get a variable from the template.
     *
     * @param name The variable name
     * @return The variable value, or null if the variable doesn't exist
     */
    @Nullable
    public String getVariable(@NotNull String name) {
        return variables.get(name);
    }
    
    /**
     * Builder for creating ModTemplate instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String category;
        private ModTemplateType type;
        private Map<String, String> files;
        private Map<String, String> variables;
        
        /**
         * Set the template ID.
         *
         * @param id The template ID
         * @return This builder
         */
        public Builder id(@NotNull String id) {
            this.id = id;
            return this;
        }
        
        /**
         * Set the template name.
         *
         * @param name The template name
         * @return This builder
         */
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Set the template description.
         *
         * @param description The template description
         * @return This builder
         */
        public Builder description(@NotNull String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Set the template category.
         *
         * @param category The template category
         * @return This builder
         */
        public Builder category(@NotNull String category) {
            this.category = category;
            return this;
        }
        
        /**
         * Set the template type.
         *
         * @param type The template type
         * @return This builder
         */
        public Builder type(@NotNull ModTemplateType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Set the template files.
         *
         * @param files The template files
         * @return This builder
         */
        public Builder files(@NotNull Map<String, String> files) {
            this.files = files;
            return this;
        }
        
        /**
         * Set the template variables.
         *
         * @param variables The template variables
         * @return This builder
         */
        public Builder variables(@NotNull Map<String, String> variables) {
            this.variables = variables;
            return this;
        }
        
        /**
         * Build the template.
         *
         * @return The template
         * @throws IllegalStateException If any required fields are missing
         */
        public ModTemplate build() {
            if (id == null) {
                throw new IllegalStateException("Template ID is required");
            }
            
            if (name == null) {
                throw new IllegalStateException("Template name is required");
            }
            
            if (description == null) {
                throw new IllegalStateException("Template description is required");
            }
            
            if (category == null) {
                throw new IllegalStateException("Template category is required");
            }
            
            if (type == null) {
                throw new IllegalStateException("Template type is required");
            }
            
            if (files == null) {
                throw new IllegalStateException("Template files are required");
            }
            
            if (variables == null) {
                throw new IllegalStateException("Template variables are required");
            }
            
            return new ModTemplate(id, name, description, category, type, files, variables);
        }
    }
}