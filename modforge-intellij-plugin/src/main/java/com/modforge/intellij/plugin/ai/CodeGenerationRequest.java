package com.modforge.intellij.plugin.ai;

import com.modforge.intellij.plugin.model.ModLoaderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a request for code generation.
 * This class contains all the information needed to generate code for a Minecraft mod.
 */
public class CodeGenerationRequest {
    private final String prompt;
    private final ModLoaderType modLoaderType;
    private final String minecraftVersion;
    private final String language;
    private final String codeContext;
    private final boolean usePatternMatching;
    
    /**
     * Creates a new code generation request.
     *
     * @param builder The builder to use
     */
    private CodeGenerationRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.modLoaderType = builder.modLoaderType;
        this.minecraftVersion = builder.minecraftVersion;
        this.language = builder.language;
        this.codeContext = builder.codeContext;
        this.usePatternMatching = builder.usePatternMatching;
    }
    
    /**
     * Gets the prompt for code generation.
     *
     * @return The prompt
     */
    @NotNull
    public String getPrompt() {
        return prompt;
    }
    
    /**
     * Gets the mod loader type.
     *
     * @return The mod loader type
     */
    @NotNull
    public ModLoaderType getModLoaderType() {
        return modLoaderType;
    }
    
    /**
     * Gets the Minecraft version.
     *
     * @return The Minecraft version
     */
    @NotNull
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    /**
     * Gets the programming language.
     *
     * @return The programming language
     */
    @NotNull
    public String getLanguage() {
        return language;
    }
    
    /**
     * Gets the code context.
     *
     * @return The code context, or an empty string if none
     */
    @NotNull
    public String getCodeContext() {
        return codeContext != null ? codeContext : "";
    }
    
    /**
     * Checks if pattern matching should be used.
     *
     * @return True if pattern matching should be used, false otherwise
     */
    public boolean isUsePatternMatching() {
        return usePatternMatching;
    }
    
    /**
     * Creates a new builder for code generation requests.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for code generation requests.
     */
    public static class Builder {
        private String prompt;
        private ModLoaderType modLoaderType = ModLoaderType.UNKNOWN;
        private String minecraftVersion = "";
        private String language = "Java";
        private String codeContext = "";
        private boolean usePatternMatching = true;
        
        /**
         * Sets the prompt for code generation.
         *
         * @param prompt The prompt
         * @return This builder
         */
        public Builder prompt(@NotNull String prompt) {
            this.prompt = prompt;
            return this;
        }
        
        /**
         * Sets the mod loader type.
         *
         * @param modLoaderType The mod loader type
         * @return This builder
         */
        public Builder modLoaderType(@NotNull ModLoaderType modLoaderType) {
            this.modLoaderType = modLoaderType;
            return this;
        }
        
        /**
         * Sets the Minecraft version.
         *
         * @param minecraftVersion The Minecraft version
         * @return This builder
         */
        public Builder minecraftVersion(@Nullable String minecraftVersion) {
            this.minecraftVersion = minecraftVersion != null ? minecraftVersion : "";
            return this;
        }
        
        /**
         * Sets the programming language.
         *
         * @param language The programming language
         * @return This builder
         */
        public Builder language(@Nullable String language) {
            this.language = language != null ? language : "Java";
            return this;
        }
        
        /**
         * Sets the code context.
         *
         * @param codeContext The code context
         * @return This builder
         */
        public Builder codeContext(@Nullable String codeContext) {
            this.codeContext = codeContext;
            return this;
        }
        
        /**
         * Sets whether pattern matching should be used.
         *
         * @param usePatternMatching True if pattern matching should be used, false otherwise
         * @return This builder
         */
        public Builder usePatternMatching(boolean usePatternMatching) {
            this.usePatternMatching = usePatternMatching;
            return this;
        }
        
        /**
         * Builds a new code generation request.
         *
         * @return The new code generation request
         */
        public CodeGenerationRequest build() {
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalStateException("Prompt is required");
            }
            return new CodeGenerationRequest(this);
        }
    }
}