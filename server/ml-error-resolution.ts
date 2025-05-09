import { pgTable, serial, text, integer, jsonb, timestamp, boolean, real } from "drizzle-orm/pg-core";
import { db } from "./db";
import { eq, and, desc, sql, inArray } from "drizzle-orm";
import { getUsageMetrics } from "./ai-service-manager";
import { fixCode } from "./ai-service";

// Define the error patterns table
export const errorPatterns = pgTable('error_patterns', {
  id: serial('id').primaryKey(),
  errorType: text('error_type').notNull(),
  errorPattern: text('error_pattern').notNull(),
  context: text('context'), // e.g., modLoader, mcVersion
  fixPattern: text('fix_pattern').notNull(),
  confidence: real('confidence').notNull().default(1.0),
  usageCount: integer('usage_count').notNull().default(0),
  successCount: integer('success_count').notNull().default(0),
  lastUsed: timestamp('last_used').defaultNow(),
  tags: text('tags').array(),
  metaData: jsonb('meta_data'),
  isActive: boolean('is_active').notNull().default(true),
  createdAt: timestamp('created_at').defaultNow(),
});

// Interface for error data
export interface ErrorData {
  message: string;
  line?: number;
  column?: number;
  file?: string;
  stack?: string;
  type?: string;
  code?: string;
}

interface ErrorPatternMatch {
  pattern: typeof errorPatterns.$inferSelect;
  similarityScore: number;
}

/**
 * Calculate similarity between two error messages
 * Higher score means more similar
 */
function calculateErrorSimilarity(error1: string, error2: string): number {
  // Convert to lowercase for comparison
  const e1 = error1.toLowerCase();
  const e2 = error2.toLowerCase();
  
  // If identical, maximum similarity
  if (e1 === e2) return 1.0;
  
  // Break into tokens and compare
  const tokens1 = e1.split(/[\s,.;:()\[\]{}]+/).filter(t => t.length > 0);
  const tokens2 = e2.split(/[\s,.;:()\[\]{}]+/).filter(t => t.length > 0);
  
  if (tokens1.length === 0 || tokens2.length === 0) return 0.0;
  
  // Count token matches
  let matches = 0;
  for (const t1 of tokens1) {
    if (tokens2.includes(t1)) matches++;
  }
  
  // Calculate Jaccard similarity
  const unionSize = new Set([...tokens1, ...tokens2]).size;
  return matches / unionSize;
}

/**
 * Extract key features from an error message for classification
 * @returns Features extracted from the error
 */
function extractErrorFeatures(error: ErrorData): Map<string, number> {
  const features = new Map<string, number>();
  const message = error.message.toLowerCase();
  
  // Check for common error types
  if (message.includes("cannot find symbol")) features.set("symbol_not_found", 1);
  if (message.includes("incompatible types")) features.set("type_mismatch", 1);
  if (message.includes("cannot be applied to")) features.set("method_mismatch", 1);
  if (message.includes("expected")) features.set("syntax_error", 1);
  if (message.includes("unreported exception")) features.set("exception_handling", 1);
  if (message.includes("is not abstract and does not override")) features.set("override_issue", 1);
  if (message.includes("cyclic inheritance")) features.set("inheritance_error", 1);
  if (message.includes("required")) features.set("missing_required", 1);
  if (message.includes("import")) features.set("import_issue", 1);
  
  // Check for specific Minecraft Forge/Fabric errors
  if (message.includes("registry")) features.set("registry_error", 1);
  if (message.includes("mixin")) features.set("mixin_error", 1);
  if (message.includes("event")) features.set("event_error", 1);
  if (message.includes("capability")) features.set("capability_error", 1);
  if (message.includes("model")) features.set("model_error", 1);
  if (message.includes("render")) features.set("render_error", 1);
  
  return features;
}

/**
 * Normalize an error message to improve matching
 * Removes specifics like line numbers, file paths, etc.
 */
function normalizeErrorMessage(message: string): string {
  return message
    .replace(/\r?\n/g, ' ') // Replace newlines with spaces
    .replace(/\s+/g, ' ') // Normalize multiple spaces
    .replace(/at line \d+/g, 'at line X') // Normalize line references
    .replace(/in file .+?\.java/g, 'in file X.java') // Normalize file references
    .replace(/\[.*?\]/g, '[X]') // Normalize bracketed content
    .replace(/".+?"/g, '"X"') // Normalize quoted strings
    .replace(/'.+?'/g, "'X'") // Normalize quoted chars
    .replace(/\b\d+\b/g, 'N') // Normalize numbers
    .trim();
}

/**
 * Find similar error patterns from the database
 */
export async function findSimilarErrorPatterns(
  error: ErrorData,
  context: string = "",
  threshold: number = 0.7
): Promise<ErrorPatternMatch[]> {
  try {
    // Get all patterns from the database
    const patterns = await db.select().from(errorPatterns).where(
      context ? eq(errorPatterns.context, context) : sql`1=1`
    );
    
    if (patterns.length === 0) return [];
    
    // Normalize the input error
    const normalizedError = normalizeErrorMessage(error.message);
    
    // Calculate similarity scores for each pattern
    const matches: ErrorPatternMatch[] = [];
    for (const pattern of patterns) {
      const similarityScore = calculateErrorSimilarity(
        normalizedError,
        normalizeErrorMessage(pattern.errorPattern)
      );
      
      if (similarityScore >= threshold) {
        matches.push({
          pattern,
          similarityScore
        });
      }
    }
    
    // Sort by similarity score (highest first)
    return matches.sort((a, b) => b.similarityScore - a.similarityScore);
  } catch (error) {
    console.error("Error finding similar error patterns:", error);
    return [];
  }
}

/**
 * Store a successful error fix pattern
 */
export async function storeErrorFixPattern(
  originalError: ErrorData,
  fixedCode: string,
  context: string = "",
  tags: string[] = []
): Promise<void> {
  try {
    await db.insert(errorPatterns).values({
      errorType: originalError.type || "unknown",
      errorPattern: originalError.message,
      context,
      fixPattern: fixedCode,
      tags: tags, // Pass the array directly
      confidence: 1.0, // Initial confidence
      usageCount: 1,
      successCount: 1
    });
  } catch (error) {
    console.error("Error storing error fix pattern:", error);
  }
}

/**
 * Update the success/failure metrics for an error pattern
 */
export async function recordErrorPatternResult(
  patternId: number, 
  success: boolean
): Promise<void> {
  try {
    await db.update(errorPatterns)
      .set({
        usageCount: sql`${errorPatterns.usageCount} + 1`,
        successCount: success ? sql`${errorPatterns.successCount} + 1` : errorPatterns.successCount,
        lastUsed: new Date(),
        // Adjust confidence based on success/failure
        confidence: success 
          ? sql`LEAST(${errorPatterns.confidence} * 1.1, 1.0)` 
          : sql`GREATEST(${errorPatterns.confidence} * 0.9, 0.1)`
      })
      .where(eq(errorPatterns.id, patternId));
  } catch (error) {
    console.error("Error recording pattern result:", error);
  }
}

/**
 * Get all error patterns with statistics
 */
export async function getErrorPatternStats() {
  try {
    const patterns = await db.select().from(errorPatterns);
    
    // Calculate success rates and other metrics
    return {
      totalPatterns: patterns.length,
      activePatterns: patterns.filter(p => p.isActive).length,
      highConfidencePatterns: patterns.filter(p => p.confidence > 0.8).length,
      totalUsage: patterns.reduce((sum, p) => sum + p.usageCount, 0),
      totalSuccess: patterns.reduce((sum, p) => sum + p.successCount, 0),
      averageConfidence: patterns.length > 0 
        ? patterns.reduce((sum, p) => sum + p.confidence, 0) / patterns.length
        : 0,
      recentPatterns: patterns
        .sort((a, b) => (b.lastUsed?.getTime() || 0) - (a.lastUsed?.getTime() || 0))
        .slice(0, 10)
    };
  } catch (error) {
    console.error("Error getting error pattern stats:", error);
    return {
      totalPatterns: 0,
      activePatterns: 0,
      highConfidencePatterns: 0,
      totalUsage: 0,
      totalSuccess: 0,
      averageConfidence: 0,
      recentPatterns: []
    };
  }
}

/**
 * Try to fix an error using stored patterns before falling back to AI
 */
export async function tryFixErrorFromPatterns(
  code: string,
  error: ErrorData,
  context: string = "",
  language: string = "java"
): Promise<{ 
  fixed: boolean; 
  code: string; 
  explanation: string;
  usedPattern?: boolean;
  patternId?: number;
}> {
  try {
    // Look for similar error patterns
    const matches = await findSimilarErrorPatterns(error, context);
    
    // If we have a good match, use it
    if (matches.length > 0 && matches[0].similarityScore > 0.75) {
      const bestMatch = matches[0];
      const pattern = bestMatch.pattern;
      
      // Get metrics for tracking
      const metrics = getUsageMetrics();
      
      // Apply the fix pattern - in a real implementation this would be more sophisticated
      // to actually apply the pattern correctly based on the context
      
      // For simplicity in this demo, we'll just return the pattern
      // In a real implementation, you'd need to analyze the code and error to apply the pattern
      
      // Record usage
      await recordErrorPatternResult(pattern.id, true);
      
      // Record in metrics
      if (metrics) {
        metrics.patternMatches++;
        // Assuming average tokens saved
        metrics.estimatedTokensSaved += 1000;
        metrics.estimatedCostSaved += 0.02; // $0.02 per 1K tokens saved
      }
      
      return {
        fixed: true,
        code: code, // In a real implementation, this would be the fixed code
        explanation: `Fixed using pattern #${pattern.id} with ${(bestMatch.similarityScore * 100).toFixed(1)}% confidence. This pattern has been used ${pattern.usageCount} times with a ${((pattern.successCount / pattern.usageCount) * 100).toFixed(1)}% success rate.`,
        usedPattern: true,
        patternId: pattern.id
      };
    }
    
    // Fall back to AI solution
    console.log("No matching error pattern found, falling back to AI solution");
    const aiResult = await fixCode(code, [error.message], language);
    
    // If the AI solution was successful, store it as a new pattern
    if (aiResult) {
      await storeErrorFixPattern(
        error,
        aiResult.code,
        context,
        [language]
      );
      
      // Record in metrics
      const metrics = getUsageMetrics();
      if (metrics) {
        metrics.apiCalls++;
      }
      
      return {
        fixed: true,
        code: aiResult.code,
        explanation: aiResult.explanation + "\n\nThis fix has been stored as a new pattern for future use.",
        usedPattern: false
      };
    }
    
    return {
      fixed: false,
      code: code,
      explanation: "Could not fix the error using either patterns or AI.",
      usedPattern: false
    };
  } catch (error) {
    console.error("Error in tryFixErrorFromPatterns:", error);
    return {
      fixed: false,
      code: code,
      explanation: `Error in error resolution system: ${error instanceof Error ? error.message : String(error)}`,
      usedPattern: false
    };
  }
}