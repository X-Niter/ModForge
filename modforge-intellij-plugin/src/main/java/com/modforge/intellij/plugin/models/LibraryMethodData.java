package com.modforge.intellij.plugin.models;

/**
 * Represents data about a method in a library.
 */
public class LibraryMethodData {
    private String name;
    private String returnType;
    private String[] parameters;
    private String[] exceptions;
    private boolean isStatic;
    private boolean isAbstract;
    private boolean isFinal;
    private String visibility; // "public", "private", "protected", "package"
    private String docComment;
    private String code;
    
    /**
     * Pattern scoring for relevance in different contexts
     * These scores are updated through usage analytics
     */
    private double codeGenerationRelevance = 0.0;
    private double errorResolutionRelevance = 0.0;
    private double documentationRelevance = 0.0;
    private int usageCount = 0;
    
    public LibraryMethodData() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getReturnType() {
        return returnType;
    }
    
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
    
    public String[] getParameters() {
        return parameters;
    }
    
    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }
    
    public String[] getExceptions() {
        return exceptions;
    }
    
    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }
    
    public boolean isStatic() {
        return isStatic;
    }
    
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }
    
    public boolean isAbstract() {
        return isAbstract;
    }
    
    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
    
    public String getDocComment() {
        return docComment;
    }
    
    public void setDocComment(String docComment) {
        this.docComment = docComment;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public double getCodeGenerationRelevance() {
        return codeGenerationRelevance;
    }
    
    public void setCodeGenerationRelevance(double codeGenerationRelevance) {
        this.codeGenerationRelevance = codeGenerationRelevance;
    }
    
    public double getErrorResolutionRelevance() {
        return errorResolutionRelevance;
    }
    
    public void setErrorResolutionRelevance(double errorResolutionRelevance) {
        this.errorResolutionRelevance = errorResolutionRelevance;
    }
    
    public double getDocumentationRelevance() {
        return documentationRelevance;
    }
    
    public void setDocumentationRelevance(double documentationRelevance) {
        this.documentationRelevance = documentationRelevance;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    /**
     * Increments the usage count.
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }
    
    /**
     * Updates the relevance scores based on usage context.
     * @param context The usage context (code generation, error resolution, documentation)
     * @param success Whether the usage was successful
     */
    public void updateRelevanceScores(String context, boolean success) {
        // Update the usage count
        incrementUsageCount();
        
        // Weight factors for relevance score updates
        double learningRate = 0.1;
        double successBonus = success ? 0.2 : -0.1;
        
        // Update the relevance score for the given context
        switch (context) {
            case "code_generation":
                codeGenerationRelevance += learningRate * (1.0 + successBonus - codeGenerationRelevance);
                break;
            case "error_resolution":
                errorResolutionRelevance += learningRate * (1.0 + successBonus - errorResolutionRelevance);
                break;
            case "documentation":
                documentationRelevance += learningRate * (1.0 + successBonus - documentationRelevance);
                break;
        }
    }
    
    /**
     * Gets the method signature.
     * @return The method signature
     */
    public String getSignature() {
        StringBuilder signature = new StringBuilder();
        signature.append(returnType).append(" ").append(name).append("(");
        
        if (parameters != null && parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    signature.append(", ");
                }
                signature.append(parameters[i]);
            }
        }
        
        signature.append(")");
        
        if (exceptions != null && exceptions.length > 0) {
            signature.append(" throws ");
            for (int i = 0; i < exceptions.length; i++) {
                if (i > 0) {
                    signature.append(", ");
                }
                signature.append(exceptions[i]);
            }
        }
        
        return signature.toString();
    }
}