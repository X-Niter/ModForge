import { db } from "./db";
import { eq, and, desc } from 'drizzle-orm';
import { pgTable, serial, text, timestamp, integer, jsonb } from 'drizzle-orm/pg-core';

/**
 * This module implements the pattern learning capabilities for idea generation.
 * It allows the system to learn from previous idea generations, reducing the need
 * for OpenAI API calls over time.
 */

// Schema for idea generation patterns
export const ideaPatterns = pgTable('idea_patterns', {
  id: serial('id').primaryKey(),
  theme: text('theme'), // Can be null if general
  complexity: text('complexity').notNull(), // Simple, medium, complex
  modLoader: text('mod_loader'), // Can be null if any
  minecraftVersion: text('minecraft_version'), // Can be null if any
  prompt: text('prompt').notNull(), // The original prompt
  response: jsonb('response').notNull(), // The generated ideas
  useCount: integer('use_count').default(0).notNull(),
  successRate: integer('success_rate').default(100).notNull(),
  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull(),
});

// Interface for pattern matching results
interface IdeaPatternMatch {
  pattern: typeof ideaPatterns.$inferSelect;
  similarityScore: number;
}

/**
 * Store a successful idea generation pattern
 */
export async function storeIdeaGenerationPattern(
  prompt: string,
  theme: string | null,
  complexity: string,
  modLoader: string | null,
  minecraftVersion: string | null,
  response: any
): Promise<void> {
  try {
    await db.insert(ideaPatterns).values({
      prompt,
      theme,
      complexity,
      modLoader,
      minecraftVersion,
      response
    });
    console.log(`Stored new idea generation pattern for complexity: ${complexity}`);
  } catch (error) {
    console.error('Failed to store idea generation pattern:', error);
  }
}

/**
 * Find similar idea generation patterns
 * Returns patterns sorted by similarity (most similar first)
 */
export async function findSimilarIdeaPatterns(
  prompt: string,
  theme: string | null,
  complexity: string,
  modLoader: string | null,
  minecraftVersion: string | null,
  limit: number = 5
): Promise<IdeaPatternMatch[]> {
  try {
    // Build our query conditions
    const conditions = [];
    
    // First, look for exact complexity match
    conditions.push(eq(ideaPatterns.complexity, complexity));
    
    // Add other conditions if they are specified
    if (theme) {
      conditions.push(eq(ideaPatterns.theme, theme));
    }
    
    if (modLoader) {
      conditions.push(eq(ideaPatterns.modLoader, modLoader));
    }
    
    if (minecraftVersion) {
      conditions.push(eq(ideaPatterns.minecraftVersion, minecraftVersion));
    }
    
    // Get matching patterns
    const patterns = await db.select()
      .from(ideaPatterns)
      .where(and(...conditions))
      .orderBy(desc(ideaPatterns.useCount))
      .limit(20); // Get more than we need for filtering
    
    // Computer word similarity for the prompt
    const matches: IdeaPatternMatch[] = patterns.map(pattern => {
      const promptWords = new Set(prompt.toLowerCase().split(/\s+/));
      const patternWords = new Set(pattern.prompt.toLowerCase().split(/\s+/));
      
      // Count words that appear in both sets
      let commonWords = 0;
      for (const word of promptWords) {
        if (patternWords.has(word)) {
          commonWords++;
        }
      }
      
      // Calculate Jaccard similarity: intersection size / union size
      const similarity = commonWords / (promptWords.size + patternWords.size - commonWords);
      
      return {
        pattern,
        similarityScore: similarity
      };
    });
    
    // Sort by similarity and return top matches
    return matches
      .sort((a, b) => b.similarityScore - a.similarityScore)
      .slice(0, limit);
  } catch (error) {
    console.error('Error finding similar idea patterns:', error);
    return [];
  }
}

/**
 * Record the usage and success of a pattern
 */
export async function recordIdeaPatternResult(
  patternId: number,
  successful: boolean
): Promise<void> {
  try {
    const pattern = await db.select()
      .from(ideaPatterns)
      .where(eq(ideaPatterns.id, patternId))
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
    
    await db.update(ideaPatterns)
      .set({
        useCount: totalUses,
        successRate: newSuccessRate,
        updatedAt: new Date()
      })
      .where(eq(ideaPatterns.id, patternId));
    
  } catch (error) {
    console.error('Error recording idea pattern result:', error);
  }
}

/**
 * Try to generate ideas using existing patterns before falling back to OpenAI
 */
export async function tryGenerateIdeasFromPatterns(
  prompt: string,
  theme: string | null,
  complexity: string,
  modLoader: string | null,
  minecraftVersion: string | null,
  similarityThreshold: number = 0.7
): Promise<{ ideas: any | null; patternId: number | null; confidence: number }> {
  try {
    const similarPatterns = await findSimilarIdeaPatterns(
      prompt, 
      theme,
      complexity,
      modLoader,
      minecraftVersion
    );
    
    // If we have a highly similar pattern, use it
    const bestMatch = similarPatterns[0];
    if (bestMatch && bestMatch.similarityScore >= similarityThreshold) {
      return {
        ideas: bestMatch.pattern.response,
        patternId: bestMatch.pattern.id,
        confidence: bestMatch.similarityScore
      };
    }
    
    return { ideas: null, patternId: null, confidence: 0 };
  } catch (error) {
    console.error('Error trying to generate ideas from patterns:', error);
    return { ideas: null, patternId: null, confidence: 0 };
  }
}