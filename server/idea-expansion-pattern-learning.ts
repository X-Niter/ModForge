import { db } from "./db";
import { eq, and, desc } from 'drizzle-orm';
import { pgTable, serial, text, timestamp, integer, jsonb } from 'drizzle-orm/pg-core';

/**
 * This module implements the pattern learning capabilities for idea expansion.
 * It allows the system to learn from previous idea expansions, reducing the need
 * for OpenAI API calls over time.
 */

// Schema for idea expansion patterns
export const ideaExpansionPatterns = pgTable('idea_expansion_patterns', {
  id: serial('id').primaryKey(),
  ideaTitle: text('idea_title').notNull(),
  ideaDescription: text('idea_description').notNull(),
  keyTerms: text('key_terms').array(), // Array of key terms extracted for matching
  response: jsonb('response').notNull(), // The expanded idea
  useCount: integer('use_count').default(0).notNull(),
  successRate: integer('success_rate').default(100).notNull(),
  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull(),
});

// Interface for pattern matching results
interface ExpansionPatternMatch {
  pattern: typeof ideaExpansionPatterns.$inferSelect;
  similarityScore: number;
}

/**
 * Extract key terms from an idea title and description for pattern matching
 */
function extractKeyTerms(title: string, description: string): string[] {
  // Combine title and description
  const text = `${title} ${description}`.toLowerCase();
  
  // Split into words
  const words = text.split(/\s+/);
  
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
 * Store a successful idea expansion pattern
 */
export async function storeIdeaExpansionPattern(
  ideaTitle: string,
  ideaDescription: string,
  response: any
): Promise<void> {
  try {
    const keyTerms = extractKeyTerms(ideaTitle, ideaDescription);
    
    await db.insert(ideaExpansionPatterns).values({
      ideaTitle,
      ideaDescription,
      keyTerms,
      response
    });
    console.log(`Stored new idea expansion pattern for: ${ideaTitle}`);
  } catch (error) {
    console.error('Failed to store idea expansion pattern:', error);
  }
}

/**
 * Find similar idea expansion patterns
 * Returns patterns sorted by similarity (most similar first)
 */
export async function findSimilarExpansionPatterns(
  ideaTitle: string,
  ideaDescription: string,
  limit: number = 5
): Promise<ExpansionPatternMatch[]> {
  try {
    // Extract key terms for the current request
    const requestKeyTerms = extractKeyTerms(ideaTitle, ideaDescription);
    
    // Get all patterns
    const patterns = await db.select()
      .from(ideaExpansionPatterns)
      .orderBy(desc(ideaExpansionPatterns.useCount))
      .limit(20); // Get more than we need for filtering
    
    // Compute term similarity for each pattern
    const matches: ExpansionPatternMatch[] = patterns.map(pattern => {
      const patternKeyTerms = pattern.keyTerms || [];
      
      // Count matching terms
      let matchingTerms = 0;
      for (const term of requestKeyTerms) {
        if (patternKeyTerms.includes(term)) {
          matchingTerms++;
        }
      }
      
      // Calculate similarity using matching terms / total terms
      const similarity = matchingTerms / Math.max(1, Math.max(requestKeyTerms.length, patternKeyTerms.length));
      
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
    console.error('Error finding similar expansion patterns:', error);
    return [];
  }
}

/**
 * Record the success or failure of an expansion pattern
 */
export async function recordExpansionPatternResult(
  patternId: number,
  successful: boolean
): Promise<void> {
  try {
    const pattern = await db.select()
      .from(ideaExpansionPatterns)
      .where(eq(ideaExpansionPatterns.id, patternId))
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
    
    await db.update(ideaExpansionPatterns)
      .set({
        useCount: totalUses,
        successRate: newSuccessRate,
        updatedAt: new Date()
      })
      .where(eq(ideaExpansionPatterns.id, patternId));
    
  } catch (error) {
    console.error('Error recording idea expansion pattern result:', error);
  }
}

/**
 * Try to find an expanded idea using existing patterns before falling back to OpenAI
 */
export async function tryExpandIdeaFromPatterns(
  ideaTitle: string,
  ideaDescription: string,
  similarityThreshold: number = 0.7
): Promise<{ expandedIdea: any | null; patternId: number | null; confidence: number }> {
  try {
    const similarPatterns = await findSimilarExpansionPatterns(ideaTitle, ideaDescription);
    
    // If we have a highly similar pattern, use it
    const bestMatch = similarPatterns[0];
    if (bestMatch && bestMatch.similarityScore >= similarityThreshold) {
      return {
        expandedIdea: bestMatch.pattern.response,
        patternId: bestMatch.pattern.id,
        confidence: bestMatch.similarityScore
      };
    }
    
    return { expandedIdea: null, patternId: null, confidence: 0 };
  } catch (error) {
    console.error('Error trying to expand idea from patterns:', error);
    return { expandedIdea: null, patternId: null, confidence: 0 };
  }
}