import { db } from "./db";
import { eq, and, desc } from 'drizzle-orm';
import { storage } from './storage';
import { codePatterns, errorPatterns } from '@shared/schema';

/**
 * This module implements the pattern learning capabilities of the system.
 * It allows the system to learn from successful code generations and error fixes,
 * gradually reducing the need for OpenAI API calls.
 */

// Interface definitions moved to shared/schema.ts

// Interface for pattern matching results
interface PatternMatch {
  pattern: typeof codePatterns.$inferSelect;
  similarityScore: number;
}

/**
 * Store a successful code generation pattern
 */
export async function storeCodeGenerationPattern(
  prompt: string,
  modLoader: string,
  minecraftVersion: string,
  code: string,
  metadata: any = {}
): Promise<void> {
  try {
    await db.insert(codePatterns).values({
      patternType: 'generation',
      prompt,
      modLoader,
      minecraftVersion,
      outputCode: code,
      metadata
    });
    console.log(`Stored new code generation pattern for ${modLoader} ${minecraftVersion}`);
  } catch (error) {
    console.error('Failed to store code generation pattern:', error);
  }
}

/**
 * Store a successful error fix pattern
 */
export async function storeErrorFixPattern(
  errorPattern: string,
  errorType: string,
  fixStrategy: string,
  modLoader: string
): Promise<void> {
  try {
    // Check if a similar pattern exists
    const existingPatterns = await db.select()
      .from(errorPatterns)
      .where(
        and(
          eq(errorPatterns.errorType, errorType),
          eq(errorPatterns.modLoader, modLoader)
        )
      );
    
    const exactMatch = existingPatterns.find(p => p.errorPattern === errorPattern);
    
    if (exactMatch) {
      // Update success count
      await db.update(errorPatterns)
        .set({ 
          successCount: exactMatch.successCount + 1,
          updatedAt: new Date()
        })
        .where(eq(errorPatterns.id, exactMatch.id));
    } else {
      // Store new pattern
      await db.insert(errorPatterns).values({
        errorType,
        errorPattern,
        fixStrategy,
        modLoader,
        successCount: 1
      });
    }
    
    console.log(`Stored error fix pattern for ${errorType} in ${modLoader}`);
  } catch (error) {
    console.error('Failed to store error fix pattern:', error);
  }
}

/**
 * Find similar code generation patterns
 * Returns patterns sorted by similarity (most similar first)
 */
export async function findSimilarGenerationPatterns(
  prompt: string,
  modLoader: string,
  minecraftVersion: string,
  limit: number = 5
): Promise<PatternMatch[]> {
  try {
    // For now, we'll use a simple approach - get patterns for the same mod loader and version
    const patterns = await db.select()
      .from(codePatterns)
      .where(
        and(
          eq(codePatterns.patternType, 'generation'),
          eq(codePatterns.modLoader, modLoader),
          eq(codePatterns.minecraftVersion, minecraftVersion)
        )
      )
      .orderBy(desc(codePatterns.useCount))
      .limit(20); // Get more than we need for filtering
    
    // Compute similarity (this is a simple implementation - would be replaced with embedding similarity)
    const matches: PatternMatch[] = patterns.map(pattern => {
      // Simple word overlap calculation - in a real implementation, use embeddings
      const promptWords = new Set(prompt.toLowerCase().split(/\s+/));
      const patternWords = new Set(pattern.prompt.toLowerCase().split(/\s+/));
      const intersection = new Set([...promptWords].filter(x => patternWords.has(x)));
      const similarity = intersection.size / Math.max(promptWords.size, patternWords.size);
      
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
    console.error('Error finding similar generation patterns:', error);
    return [];
  }
}

/**
 * Find fix strategies for similar errors
 */
export async function findErrorFixStrategies(
  errorMessage: string,
  modLoader: string
): Promise<string | null> {
  try {
    const patterns = await db.select()
      .from(errorPatterns)
      .where(eq(errorPatterns.modLoader, modLoader))
      .orderBy(desc(errorPatterns.successCount));
    
    // Find the most similar error pattern
    // This is a simplified approach - would be enhanced with better pattern matching
    for (const pattern of patterns) {
      if (errorMessage.includes(pattern.errorPattern)) {
        return pattern.fixStrategy;
      }
    }
    
    return null;
  } catch (error) {
    console.error('Error finding error fix strategies:', error);
    return null;
  }
}

/**
 * Record the success or failure of a pattern
 */
export async function recordPatternResult(
  patternId: number,
  successful: boolean
): Promise<void> {
  try {
    const pattern = await db.select()
      .from(codePatterns)
      .where(eq(codePatterns.id, patternId))
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
    
    await db.update(codePatterns)
      .set({
        useCount: totalUses,
        successRate: newSuccessRate,
        updatedAt: new Date()
      })
      .where(eq(codePatterns.id, patternId));
    
  } catch (error) {
    console.error('Error recording pattern result:', error);
  }
}

/**
 * Try to generate code using existing patterns before falling back to OpenAI
 */
export async function tryGenerateFromPatterns(
  prompt: string,
  modLoader: string,
  minecraftVersion: string,
  similarityThreshold: number = 0.7
): Promise<{ code: string | null; patternId: number | null; confidence: number }> {
  try {
    const similarPatterns = await findSimilarGenerationPatterns(prompt, modLoader, minecraftVersion);
    
    // If we have a highly similar pattern, use it
    const bestMatch = similarPatterns[0];
    if (bestMatch && bestMatch.similarityScore >= similarityThreshold) {
      return {
        code: bestMatch.pattern.outputCode,
        patternId: bestMatch.pattern.id,
        confidence: bestMatch.similarityScore
      };
    }
    
    return { code: null, patternId: null, confidence: 0 };
  } catch (error) {
    console.error('Error trying to generate from patterns:', error);
    return { code: null, patternId: null, confidence: 0 };
  }
}

/**
 * Try to fix errors using existing patterns before falling back to OpenAI
 */
export async function tryFixFromPatterns(
  errorMessage: string,
  code: string,
  modLoader: string
): Promise<{ fixedCode: string | null; confidence: number }> {
  try {
    const fixStrategy = await findErrorFixStrategies(errorMessage, modLoader);
    
    if (fixStrategy) {
      // This is a simplified approach - in reality, you would need
      // a more sophisticated way to apply the fix strategy to the code
      // For example, using regex patterns or a simple rule-based system
      
      // For demonstration, we'll just return that we found a strategy
      return {
        fixedCode: null, // In a real implementation, would apply the strategy to the code
        confidence: 0.8  // A placeholder confidence value
      };
    }
    
    return { fixedCode: null, confidence: 0 };
  } catch (error) {
    console.error('Error trying to fix from patterns:', error);
    return { fixedCode: null, confidence: 0 };
  }
}