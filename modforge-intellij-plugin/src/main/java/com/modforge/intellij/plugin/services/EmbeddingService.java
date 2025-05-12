package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for managing text embeddings for semantic similarity searches.
 * This allows for more efficient pattern matching without relying on exact string matching.
 */
@Service(Service.Level.PROJECT)
public final class EmbeddingService {
    private static final Logger LOG = Logger.getInstance(EmbeddingService.class);
    
    // Dimension of embeddings
    private static final int EMBEDDING_DIMENSION = 1536;
    
    // Path to store embeddings
    private static final String EMBEDDINGS_DIR = "embeddings";
    
    // In-memory cache for embeddings
    private final Map<String, float[]> embeddingsCache = new ConcurrentHashMap<>();
    
    // Project reference
    private final Project project;
    
    // OpenAI service for generating embeddings
    private final AIServiceManager aiServiceManager;
    
    // Pattern caching service for efficiency
    private final PatternCachingService patternCachingService;
    
    // Flags
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isGeneratingEmbeddings = new AtomicBoolean(false);
    
    /**
     * Creates a new EmbeddingService.
     * @param project The project
     */
    public EmbeddingService(@NotNull Project project) {
        this.project = project;
        this.aiServiceManager = AIServiceManager.getInstance();
        this.patternCachingService = PatternCachingService.getInstance(project);
        
        // Initialize the embeddings directory
        initializeEmbeddingsDirectory();
    }
    
    /**
     * Gets an instance of the EmbeddingService.
     * @param project The project
     * @return The EmbeddingService instance
     */
    public static EmbeddingService getInstance(@NotNull Project project) {
        return project.getService(EmbeddingService.class);
    }
    
    /**
     * Initialize the embeddings directory.
     */
    private void initializeEmbeddingsDirectory() {
        try {
            File embeddingsDir = getEmbeddingsDirectory();
            if (!embeddingsDir.exists()) {
                if (embeddingsDir.mkdirs()) {
                    LOG.info("Created embeddings directory: " + embeddingsDir.getAbsolutePath());
                } else {
                    LOG.error("Failed to create embeddings directory: " + embeddingsDir.getAbsolutePath());
                }
            }
            
            isInitialized.set(true);
        } catch (Exception e) {
            LOG.error("Error initializing embeddings directory", e);
        }
    }
    
    /**
     * Gets the embeddings directory.
     * @return The embeddings directory
     */
    @NotNull
    private File getEmbeddingsDirectory() {
        String basePath = CompatibilityUtil.getProjectBasePath(project);
        if (basePath == null) {
            LOG.error("Could not determine project base path");
            // Fallback to temporary directory
            return new File(System.getProperty("java.io.tmpdir"), "modforge_embeddings_" + project.getName());
        }
        
        Path projectPath = Paths.get(basePath);
        Path embeddingsPath = projectPath.resolve(".idea").resolve(EMBEDDINGS_DIR);
        return embeddingsPath.toFile();
    }
    
    /**
     * Generates an embedding for the given text.
     * This will call the OpenAI API if needed.
     * @param text The text to generate an embedding for
     * @param forceRefresh Whether to force refresh the embedding
     * @return The embedding
     */
    @Nullable
    public float[] getEmbedding(@NotNull String text, boolean forceRefresh) {
        if (!isInitialized.get()) {
            LOG.warn("EmbeddingService not initialized");
            return null;
        }
        
        // Normalize text
        final String normalizedText = normalizeText(text);
        
        // Generate a unique identifier for this text
        String embeddingId = generateEmbeddingId(normalizedText);
        
        // Check cache first
        if (!forceRefresh && embeddingsCache.containsKey(embeddingId)) {
            return embeddingsCache.get(embeddingId);
        }
        
        // Check if we have a stored embedding
        float[] storedEmbedding = loadEmbedding(embeddingId);
        if (!forceRefresh && storedEmbedding != null) {
            embeddingsCache.put(embeddingId, storedEmbedding);
            return storedEmbedding;
        }
        
        // Generate new embedding
        try {
            if (isGeneratingEmbeddings.get()) {
                LOG.warn("Already generating embeddings, skipping: " + embeddingId);
                return null;
            }
            
            isGeneratingEmbeddings.set(true);
            
            // Call OpenAI API to generate embedding
            float[] embedding = generateEmbeddingFromOpenAI(normalizedText);
            
            if (embedding != null) {
                // Cache and save the embedding
                embeddingsCache.put(embeddingId, embedding);
                saveEmbedding(embeddingId, embedding);
                return embedding;
            }
        } catch (Exception e) {
            LOG.error("Error generating embedding", e);
        } finally {
            isGeneratingEmbeddings.set(false);
        }
        
        return null;
    }
    
    /**
     * Computes the similarity between two embeddings.
     * Uses cosine similarity for comparison.
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The similarity score (0-1)
     */
    public double computeSimilarity(@NotNull float[] embedding1, @NotNull float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            LOG.error("Embeddings have different dimensions: " + embedding1.length + " vs " + embedding2.length);
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        // Avoid division by zero
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        // Compute cosine similarity
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Finds the most similar text from a set of candidates.
     * @param query The query text
     * @param candidates The candidate texts
     * @param similarityThreshold The minimum similarity threshold (0-1)
     * @return The most similar text and its similarity score, or null if none found
     */
    @Nullable
    public Map.Entry<String, Double> findMostSimilarText(
            @NotNull String query,
            @NotNull List<String> candidates,
            double similarityThreshold
    ) {
        float[] queryEmbedding = getEmbedding(query, false);
        if (queryEmbedding == null || candidates.isEmpty()) {
            return null;
        }
        
        String mostSimilarText = null;
        double highestSimilarity = 0.0;
        
        for (String candidate : candidates) {
            float[] candidateEmbedding = getEmbedding(candidate, false);
            if (candidateEmbedding != null) {
                double similarity = computeSimilarity(queryEmbedding, candidateEmbedding);
                if (similarity > highestSimilarity && similarity >= similarityThreshold) {
                    highestSimilarity = similarity;
                    mostSimilarText = candidate;
                }
            }
        }
        
        if (mostSimilarText != null) {
            return Map.entry(mostSimilarText, highestSimilarity);
        }
        
        return null;
    }
    
    /**
     * Normalizes text for consistent embeddings.
     * @param text The text to normalize
     * @return The normalized text
     */
    @NotNull
    private String normalizeText(@NotNull String text) {
        // Basic normalization: trim, lowercase, and remove excess whitespace
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }
    
    /**
     * Generates a unique identifier for the given text.
     * @param text The text
     * @return A unique identifier
     */
    @NotNull
    private String generateEmbeddingId(@NotNull String text) {
        return Integer.toHexString(text.hashCode());
    }
    
    /**
     * Loads an embedding from storage.
     * @param embeddingId The embedding ID
     * @return The embedding, or null if not found
     */
    @Nullable
    private float[] loadEmbedding(@NotNull String embeddingId) {
        File embeddingFile = new File(getEmbeddingsDirectory(), embeddingId + ".emb");
        if (!embeddingFile.exists()) {
            return null;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(embeddingFile))) {
            return (float[]) ois.readObject();
        } catch (Exception e) {
            LOG.error("Error loading embedding: " + embeddingId, e);
            return null;
        }
    }
    
    /**
     * Saves an embedding to storage.
     * @param embeddingId The embedding ID
     * @param embedding The embedding
     */
    private void saveEmbedding(@NotNull String embeddingId, @NotNull float[] embedding) {
        File embeddingFile = new File(getEmbeddingsDirectory(), embeddingId + ".emb");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(embeddingFile))) {
            oos.writeObject(embedding);
        } catch (Exception e) {
            LOG.error("Error saving embedding: " + embeddingId, e);
        }
    }
    
    /**
     * Generates an embedding from OpenAI.
     * @param text The text to generate an embedding for
     * @return The embedding
     */
    @Nullable
    private float[] generateEmbeddingFromOpenAI(@NotNull String text) {
        try {
            // Check if we have a cached result in the pattern cache
            String cacheKey = "embedding:" + text.hashCode();
            String cachedResult = patternCachingService.getCachedResult(cacheKey);
            
            if (cachedResult != null) {
                // Parse the cached embedding
                return parseEmbeddingString(cachedResult);
            }
            
            // Call OpenAI API to generate embedding - simplified for this implementation
            // In a real implementation, this would call the actual OpenAI API
            
            // Mock implementation for demonstration purposes
            float[] embedding = new float[EMBEDDING_DIMENSION];
            Random random = new Random(text.hashCode());
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] = (random.nextFloat() * 2) - 1; // Values between -1 and 1
            }
            
            // Normalize the embedding
            double sum = 0.0;
            for (float v : embedding) {
                sum += v * v;
            }
            double magnitude = Math.sqrt(sum);
            
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= magnitude;
            }
            
            // Cache the result
            patternCachingService.cacheResult(cacheKey, convertEmbeddingToString(embedding));
            
            return embedding;
        } catch (Exception e) {
            LOG.error("Error generating embedding from OpenAI", e);
            return null;
        }
    }
    
    /**
     * Converts an embedding to a string for caching.
     * @param embedding The embedding
     * @return The string representation
     */
    @NotNull
    private String convertEmbeddingToString(@NotNull float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        return sb.toString();
    }
    
    /**
     * Parses an embedding string.
     * @param embeddingString The embedding string
     * @return The embedding
     */
    @Nullable
    private float[] parseEmbeddingString(@NotNull String embeddingString) {
        try {
            String[] parts = embeddingString.split(",");
            float[] embedding = new float[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                embedding[i] = Float.parseFloat(parts[i]);
            }
            
            return embedding;
        } catch (Exception e) {
            LOG.error("Error parsing embedding string", e);
            return null;
        }
    }
}