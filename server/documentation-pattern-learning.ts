import { db } from "./db";
import { eq, and, desc } from 'drizzle-orm';
import { pgTable, serial, text, timestamp, integer, jsonb } from 'drizzle-orm/pg-core';

/**
 * This module implements the pattern learning for documentation generation.
 * It allows the system to learn from previous documentation generations
 * to reduce the need for OpenAI API calls.
 */

// Schema for documentation patterns
export const documentationPatterns = pgTable('documentation_patterns', {
  id: serial('id').primaryKey(),
  language: text('language').default('java').notNull(),
  codeFingerprint: text('code_fingerprint').notNull(), // A hash/fingerprint of the code for quick matching
  codeLength: integer('code_length').notNull(), // Length of the original code
  codeType: text('code_type').notNull(), // "class", "method", "module", etc.
  style: text('style').default('standard').notNull(), // Documentation style
  inputCode: text('input_code').notNull(), // The original code
  outputDocs: text('output_docs').notNull(), // The generated documentation
  useCount: integer('use_count').default(0).notNull(),
  successRate: integer('success_rate').default(100).notNull(),
  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull(),
});

// Interface for pattern matching results
interface DocumentationPatternMatch {
  pattern: typeof documentationPatterns.$inferSelect;
  similarityScore: number;
}

/**
 * Generate a simple fingerprint of code for matching
 * This creates a basic structural signature of the code
 */
function generateCodeFingerprint(code: string): string {
  // Remove comments
  const codeWithoutComments = code.replace(/\/\/.*|\/\*[\s\S]*?\*\//g, '');
  
  // Remove whitespace
  const strippedCode = codeWithoutComments.replace(/\s+/g, '');
  
  // Count occurrences of various syntax elements to create a signature
  const classCount = (code.match(/class\s+([a-zA-Z0-9_]+)/g) || []).length;
  const methodCount = (code.match(/(?:public|private|protected|static|\s) +[\w\<\>\[\]]+\s+(\w+) *\([^\)]*\) *(\{?|[^;])/g) || []).length;
  const importCount = (code.match(/import\s+[\w.]+;/g) || []).length;
  const loopCount = (code.match(/for|while|do\s*\{/g) || []).length;
  const ifCount = (code.match(/if\s*\(/g) || []).length;
  
  // Create a simple hash by combining these counts and the code length
  return `c${classCount}m${methodCount}i${importCount}l${loopCount}if${ifCount}len${strippedCode.length}`;
}

/**
 * Determine the code type (class, method, module)
 */
function determineCodeType(code: string): string {
  if (/class\s+([a-zA-Z0-9_]+)/.test(code)) {
    return 'class';
  } else if (/(?:public|private|protected|static|\s) +[\w\<\>\[\]]+\s+(\w+) *\([^\)]*\) *(\{?|[^;])/.test(code)) {
    return 'method';
  } else if (/module\s+([a-zA-Z0-9_]+)/.test(code)) {
    return 'module';
  } else if (/interface\s+([a-zA-Z0-9_]+)/.test(code)) {
    return 'interface';
  } else {
    return 'code-snippet';
  }
}

/**
 * Store a successful documentation generation pattern
 */
export async function storeDocumentationPattern(
  code: string,
  documentation: string,
  language: string = 'java',
  style: string = 'standard'
): Promise<void> {
  try {
    const codeType = determineCodeType(code);
    const codeFingerprint = generateCodeFingerprint(code);
    
    await db.insert(documentationPatterns).values({
      language,
      codeFingerprint,
      codeLength: code.length,
      codeType,
      style,
      inputCode: code,
      outputDocs: documentation
    });
    
    console.log(`Stored new documentation pattern for ${codeType} in ${language}`);
  } catch (error) {
    console.error('Failed to store documentation pattern:', error);
  }
}

/**
 * Find similar documentation patterns
 */
export async function findSimilarDocumentationPatterns(
  code: string,
  language: string = 'java',
  style: string = 'standard',
  limit: number = 5
): Promise<DocumentationPatternMatch[]> {
  try {
    const codeType = determineCodeType(code);
    const codeFingerprint = generateCodeFingerprint(code);
    
    // Find patterns with the same language, code type and similar length
    const codeLength = code.length;
    const patterns = await db.select()
      .from(documentationPatterns)
      .where(
        and(
          eq(documentationPatterns.language, language),
          eq(documentationPatterns.codeType, codeType),
          eq(documentationPatterns.style, style)
        )
      )
      .orderBy(desc(documentationPatterns.successRate))
      .limit(20); // Get more than we need for filtering
    
    // Calculate similarity based on fingerprint and size
    const matches: DocumentationPatternMatch[] = patterns.map(pattern => {
      // Similarity of fingerprints (structural similarity)
      const fingerprintSimilarity = pattern.codeFingerprint === codeFingerprint ? 1.0 : 0.0;
      
      // Similarity of code length
      const sizeDiff = Math.abs(pattern.codeLength - codeLength);
      const maxSize = Math.max(pattern.codeLength, codeLength);
      const sizeSimilarity = 1.0 - (sizeDiff / maxSize);
      
      // Overall similarity score (weighted)
      const overallSimilarity = (fingerprintSimilarity * 0.7) + (sizeSimilarity * 0.3);
      
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
    console.error('Error finding similar documentation patterns:', error);
    return [];
  }
}

/**
 * Record the success or failure of a documentation pattern
 */
export async function recordDocumentationPatternResult(
  patternId: number,
  successful: boolean
): Promise<void> {
  try {
    const pattern = await db.select()
      .from(documentationPatterns)
      .where(eq(documentationPatterns.id, patternId))
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
    
    await db.update(documentationPatterns)
      .set({
        useCount: totalUses,
        successRate: newSuccessRate,
        updatedAt: new Date()
      })
      .where(eq(documentationPatterns.id, patternId));
    
  } catch (error) {
    console.error('Error recording documentation pattern result:', error);
  }
}

/**
 * Try to generate documentation using existing patterns before falling back to OpenAI
 */
export async function tryGenerateDocsFromPatterns(
  code: string,
  language: string = 'java',
  style: string = 'standard',
  similarityThreshold: number = 0.9
): Promise<{ 
  documentation: string | null; 
  patternId: number | null; 
  confidence: number 
}> {
  try {
    const similarPatterns = await findSimilarDocumentationPatterns(code, language, style);
    
    // If we have a very similar pattern (we need high confidence for documentation), use it
    const bestMatch = similarPatterns[0];
    if (bestMatch && bestMatch.similarityScore >= similarityThreshold) {
      return {
        documentation: bestMatch.pattern.outputDocs,
        patternId: bestMatch.pattern.id,
        confidence: bestMatch.similarityScore
      };
    }
    
    return { documentation: null, patternId: null, confidence: 0 };
  } catch (error) {
    console.error('Error trying to generate documentation from patterns:', error);
    return { documentation: null, patternId: null, confidence: 0 };
  }
}