import { db } from "./db";
import { eq, and, desc } from 'drizzle-orm';
import { pgTable, serial, text, timestamp, integer, jsonb } from 'drizzle-orm/pg-core';

/**
 * This module implements pattern learning for adding new features to mods.
 * It learns from successful feature additions to reduce API usage over time.
 */

// Schema for feature addition patterns
export const featurePatterns = pgTable('feature_patterns', {
  id: serial('id').primaryKey(),
  modLoader: text('mod_loader').notNull(),
  featureType: text('feature_type').notNull(), // e.g. "item", "block", "entity", "biome"
  featureDescription: text('feature_description').notNull(),
  keyTerms: text('key_terms').array(), // Array of key terms extracted for matching
  inputFiles: jsonb('input_files').notNull(), // File structure of the mod before feature addition
  outputFiles: jsonb('output_files').notNull(), // File structure after feature addition
  explanation: text('explanation').notNull(),
  useCount: integer('use_count').default(0).notNull(),
  successRate: integer('success_rate').default(100).notNull(),
  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull(),
});

// Interface for pattern matching results
interface FeaturePatternMatch {
  pattern: typeof featurePatterns.$inferSelect;
  similarityScore: number;
}

/**
 * Determine the feature type based on the description
 */
function determineFeatureType(description: string): string {
  const lowerDesc = description.toLowerCase();
  
  // Check for different feature types
  if (lowerDesc.includes('block') || lowerDesc.includes('blocks')) {
    return 'block';
  } else if (lowerDesc.includes('item') || lowerDesc.includes('items')) {
    return 'item';
  } else if (lowerDesc.includes('entity') || lowerDesc.includes('mob') || lowerDesc.includes('creature')) {
    return 'entity';
  } else if (lowerDesc.includes('biome') || lowerDesc.includes('dimension')) {
    return 'biome';
  } else if (lowerDesc.includes('enchantment') || lowerDesc.includes('spell')) {
    return 'enchantment';
  } else if (lowerDesc.includes('potion') || lowerDesc.includes('effect')) {
    return 'effect';
  } else if (lowerDesc.includes('structure') || lowerDesc.includes('building')) {
    return 'structure';
  } else if (lowerDesc.includes('craft') || lowerDesc.includes('recipe')) {
    return 'recipe';
  } else {
    return 'general';
  }
}

/**
 * Extract key terms from a feature description
 */
function extractFeatureKeyTerms(description: string): string[] {
  // Split into words
  const words = description.toLowerCase().split(/\s+/);
  
  // Filter out common words, duplicates, and short words
  const commonWords = new Set(['a', 'an', 'the', 'in', 'on', 'at', 'to', 'for', 'with', 'by', 'and', 'or', 'but', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'should', 'could', 'of', 'from', 'as', 'this', 'that', 'these', 'those', 'it', 'they', 'them', 'their', 'who', 'what', 'when', 'where', 'why', 'how']);
  
  const keyTerms = new Set<string>();
  for (const word of words) {
    // Only include words longer than 3 characters that aren't common words
    if (word.length > 3 && !commonWords.has(word)) {
      keyTerms.add(word);
    }
  }
  
  return Array.from(keyTerms);
}

/**
 * Identify the mod loader from file structures
 */
function identifyModLoader(files: Array<{ path: string; content: string }>): string {
  const allPaths = files.map(f => f.path).join(' ');
  
  if (allPaths.includes('fabric') || allPaths.includes('net.fabricmc')) {
    return 'Fabric';
  } else if (allPaths.includes('forge') || allPaths.includes('net.minecraftforge')) {
    return 'Forge';
  } else if (allPaths.includes('bukkit') || allPaths.includes('spigot')) {
    return 'Bukkit/Spigot';
  } else if (allPaths.includes('paper')) {
    return 'Paper';
  } else {
    return 'Unknown';
  }
}

/**
 * Store a successful feature addition pattern
 */
export async function storeFeaturePattern(
  files: Array<{ path: string; content: string }>,
  updatedFiles: Array<{ path: string; content: string }>,
  featureDescription: string,
  explanation: string
): Promise<void> {
  try {
    const modLoader = identifyModLoader(files);
    const featureType = determineFeatureType(featureDescription);
    const keyTerms = extractFeatureKeyTerms(featureDescription);
    
    await db.insert(featurePatterns).values({
      modLoader,
      featureType,
      featureDescription,
      keyTerms,
      inputFiles: files,
      outputFiles: updatedFiles,
      explanation
    });
    
    console.log(`Stored new feature pattern for type: ${featureType} in ${modLoader}`);
  } catch (error) {
    console.error('Failed to store feature pattern:', error);
  }
}

/**
 * Find similar feature patterns
 */
export async function findSimilarFeaturePatterns(
  files: Array<{ path: string; content: string }>,
  featureDescription: string,
  limit: number = 5
): Promise<FeaturePatternMatch[]> {
  try {
    const modLoader = identifyModLoader(files);
    const featureType = determineFeatureType(featureDescription);
    const requestKeyTerms = extractFeatureKeyTerms(featureDescription);
    
    // Find patterns with the same mod loader and feature type
    const patterns = await db.select()
      .from(featurePatterns)
      .where(
        and(
          eq(featurePatterns.modLoader, modLoader),
          eq(featurePatterns.featureType, featureType)
        )
      )
      .orderBy(desc(featurePatterns.successRate))
      .limit(20); // Get more than we need for filtering
    
    // Calculate similarity scores
    const matches: FeaturePatternMatch[] = patterns.map(pattern => {
      const patternKeyTerms = pattern.keyTerms || [];
      
      // Count matching terms
      let matchingTerms = 0;
      for (const term of requestKeyTerms) {
        if (patternKeyTerms.includes(term)) {
          matchingTerms++;
        }
      }
      
      // Calculate similarity score
      const termSimilarity = matchingTerms / Math.max(1, Math.max(requestKeyTerms.length, patternKeyTerms.length));
      
      // Also evaluate file structure similarity
      const patternFilePaths = (pattern.inputFiles as Array<{ path: string }>).map(f => f.path);
      const requestFilePaths = files.map(f => f.path);
      
      let matchingFiles = 0;
      for (const path of requestFilePaths) {
        if (patternFilePaths.some(p => p.includes(path) || path.includes(p))) {
          matchingFiles++;
        }
      }
      
      const fileSimilarity = matchingFiles / Math.max(1, Math.max(patternFilePaths.length, requestFilePaths.length));
      
      // Overall similarity is a weighted combination
      const overallSimilarity = (termSimilarity * 0.6) + (fileSimilarity * 0.4);
      
      return {
        pattern,
        similarityScore: overallSimilarity
      };
    });
    
    // Sort by similarity and return top matches
    return matches
      .sort((a, b) => b.similarityScore - a.similarityScore)
      .slice(0, limit);
  } catch (error) {
    console.error('Error finding similar feature patterns:', error);
    return [];
  }
}

/**
 * Record the success or failure of a feature pattern
 */
export async function recordFeaturePatternResult(
  patternId: number,
  successful: boolean
): Promise<void> {
  try {
    const pattern = await db.select()
      .from(featurePatterns)
      .where(eq(featurePatterns.id, patternId))
      .then(rows => rows[0]);
    
    if (!pattern) {
      console.error(`Pattern with ID ${patternId} not found`);
      return;
    }
    
    // Update success rate
    const totalUses = pattern.useCount + 1;
    const currentSuccesses = Math.floor((pattern.successRate / 100) * pattern.useCount);
    const newSuccesses = successful ? currentSuccesses + 1 : currentSuccesses;
    const newSuccessRate = Math.round((newSuccesses / totalUses) * 100);
    
    await db.update(featurePatterns)
      .set({
        useCount: totalUses,
        successRate: newSuccessRate,
        updatedAt: new Date()
      })
      .where(eq(featurePatterns.id, patternId));
    
  } catch (error) {
    console.error('Error recording feature pattern result:', error);
  }
}

/**
 * Try to add features using existing patterns before falling back to OpenAI
 */
export async function tryAddFeaturesFromPatterns(
  files: Array<{ path: string; content: string }>,
  featureDescription: string,
  similarityThreshold: number = 0.75
): Promise<{ 
  updatedFiles: Array<{ path: string; content: string }> | null; 
  explanation: string | null;
  patternId: number | null; 
  confidence: number 
}> {
  try {
    const similarPatterns = await findSimilarFeaturePatterns(files, featureDescription);
    
    // If we have a highly similar pattern, use it
    const bestMatch = similarPatterns[0];
    if (bestMatch && bestMatch.similarityScore >= similarityThreshold) {
      return {
        updatedFiles: bestMatch.pattern.outputFiles as Array<{ path: string; content: string }>,
        explanation: bestMatch.pattern.explanation,
        patternId: bestMatch.pattern.id,
        confidence: bestMatch.similarityScore
      };
    }
    
    return { updatedFiles: null, explanation: null, patternId: null, confidence: 0 };
  } catch (error) {
    console.error('Error trying to add features from patterns:', error);
    return { updatedFiles: null, explanation: null, patternId: null, confidence: 0 };
  }
}