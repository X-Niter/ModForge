import { generateModCode, fixCompilationErrors, addModFeatures, generateCode, fixCode, enhanceCode, summarizeCode, explainError, generateDocumentation } from './ai-service';
import { tryGenerateFromPatterns, tryFixFromPatterns, recordPatternResult } from './pattern-learning';
import { tryGenerateIdeasFromPatterns, recordIdeaPatternResult } from './idea-pattern-learning';
import { tryExpandIdeaFromPatterns, recordExpansionPatternResult } from './idea-expansion-pattern-learning';
import { tryAddFeaturesFromPatterns, recordFeaturePatternResult } from './feature-pattern-learning';
import { tryGenerateDocsFromPatterns, recordDocumentationPatternResult } from './documentation-pattern-learning';

/**
 * The AI Service Manager acts as a centralized orchestration layer that:
 * 1. First attempts to serve requests using the pattern learning systems
 * 2. Only falls back to OpenAI when necessary
 * 3. Tracks and records usage patterns
 * 4. Provides analytics on API usage and cost savings
 */

// Track usage metrics for analytics
interface UsageMetrics {
  totalRequests: number;
  patternMatches: number;
  apiCalls: number;
  estimatedTokensSaved: number;
  estimatedCostSaved: number;
}

// Initialize metrics
const usageMetrics: UsageMetrics = {
  totalRequests: 0,
  patternMatches: 0,
  apiCalls: 0,
  estimatedTokensSaved: 0,
  estimatedCostSaved: 0,
};

// Constants for cost estimation
const AVG_TOKENS_PER_REQUEST = 2000;
const COST_PER_1K_TOKENS = 0.03; // Approximate cost for GPT-4o

/**
 * Update metrics when a pattern match is successful
 */
function recordPatternMatchSuccess(tokensEstimate: number = AVG_TOKENS_PER_REQUEST) {
  usageMetrics.totalRequests++;
  usageMetrics.patternMatches++;
  usageMetrics.estimatedTokensSaved += tokensEstimate;
  usageMetrics.estimatedCostSaved += (tokensEstimate / 1000) * COST_PER_1K_TOKENS;
}

/**
 * Update metrics when falling back to API
 */
function recordApiFallback(tokensEstimate: number = AVG_TOKENS_PER_REQUEST) {
  usageMetrics.totalRequests++;
  usageMetrics.apiCalls++;
}

/**
 * Get current usage metrics and cost savings
 * @returns {UsageMetrics} Current usage metrics with additional calculated fields
 */
export function getUsageMetrics(): UsageMetrics & {
  efficiencyRate: number;
  patternMatchPercentage: number;
  systemStartTime: string;
} {
  // Calculate additional metrics
  const totalRequests = usageMetrics.totalRequests || 1; // Avoid division by zero
  const patternMatchPercentage = (usageMetrics.patternMatches / totalRequests) * 100;
  const efficiencyRate = usageMetrics.estimatedTokensSaved / (usageMetrics.apiCalls * AVG_TOKENS_PER_REQUEST || 1);
  
  // Get system uptime information
  const systemStartTime = new Date(Date.now() - process.uptime() * 1000).toISOString();
  
  return { 
    ...usageMetrics,
    patternMatchPercentage: Math.round(patternMatchPercentage * 100) / 100, // Round to 2 decimal places
    efficiencyRate: Math.round(efficiencyRate * 100) / 100,
    systemStartTime
  };
}

// Enhanced versions of AI functions that use pattern learning first

/**
 * Generate documentation with pattern learning
 */
export async function smartGenerateDocumentation(
  code: string, 
  language: string = 'java', 
  style: string = 'standard'
): Promise<{ text: string }> {
  try {
    // First try to find a pattern match
    const patternResult = await tryGenerateDocsFromPatterns(code, language, style);
    
    if (patternResult.documentation && patternResult.patternId) {
      console.log(`Using documentation pattern #${patternResult.patternId} with confidence ${patternResult.confidence.toFixed(2)}`);
      recordPatternMatchSuccess();
      
      // Record successful pattern usage
      recordDocumentationPatternResult(patternResult.patternId, true);
      
      return { text: patternResult.documentation };
    }
    
    // Fall back to OpenAI
    console.log('No suitable documentation pattern found, using OpenAI API');
    recordApiFallback();
    
    // Convert style string to valid type
    const validStyle: "javadoc" | "markdown" | "inline" = 
      (style === "javadoc" || style === "markdown" || style === "inline") 
        ? style 
        : "javadoc";
    
    const result = await generateDocumentation(code, language, validStyle);
    
    // Store the successful generation for future use
    if (result.text) {
      await storeDocumentationPattern(code, result.text, language, validStyle);
    }
    
    return result;
  } catch (error) {
    console.error('Error in smartGenerateDocumentation:', error);
    throw error;
  }
}

/**
 * Generate code with pattern learning
 */
export async function smartGenerateCode(
  prompt: string,
  language: string = 'javascript',
  context: string = '',
  complexity: string = 'medium'
): Promise<{ code: string; explanation: string; suggestedFileName?: string }> {
  try {
    // First try to find a pattern match
    const patternResult = await tryGenerateFromPatterns(prompt, language, complexity);
    
    if (patternResult.code && patternResult.patternId) {
      console.log(`Using code generation pattern #${patternResult.patternId} with confidence ${patternResult.confidence.toFixed(2)}`);
      recordPatternMatchSuccess();
      
      // Record successful pattern usage
      recordPatternResult(patternResult.patternId, true);
      
      return { 
        code: patternResult.code, 
        explanation: "Generated using our pattern learning system" 
      };
    }
    
    // Fall back to OpenAI
    console.log('No suitable code pattern found, using OpenAI API');
    recordApiFallback();
    
    return await generateCode(prompt, {
      language,
      context,
      complexity
    });
  } catch (error) {
    console.error('Error in smartGenerateCode:', error);
    throw error;
  }
}

/**
 * Fix code with pattern learning
 */
export async function smartFixCode(
  code: string,
  errors: string[],
  language: string = 'javascript'
): Promise<{ code: string; explanation: string }> {
  try {
    // Extract the first error message for pattern matching
    const firstErrorMessage = errors.length > 0 ? errors[0] : '';
    
    if (firstErrorMessage) {
      // Try to find a matching error pattern
      const patternResult = await tryFixFromPatterns(firstErrorMessage, code, language);
      
      if (patternResult.fixedCode) {
        console.log(`Using error fix pattern with confidence ${patternResult.confidence.toFixed(2)}`);
        recordPatternMatchSuccess();
        
        return { 
          code: patternResult.fixedCode, 
          explanation: "Fixed using our pattern learning system" 
        };
      }
    }
    
    // Fall back to OpenAI
    console.log('No suitable error fix pattern found, using OpenAI API');
    recordApiFallback();
    
    return await fixCode(code, errors, language);
  } catch (error) {
    console.error('Error in smartFixCode:', error);
    throw error;
  }
}

/**
 * Add features with pattern learning
 */
export async function smartAddFeatures(
  files: Array<{ path: string; content: string }>,
  newFeatureDescription: string
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
  logs: string;
}> {
  let logs = `[${new Date().toISOString()}] Starting feature addition process...\n`;
  
  try {
    // First try to find a pattern match
    const patternResult = await tryAddFeaturesFromPatterns(files, newFeatureDescription);
    
    if (patternResult.updatedFiles && patternResult.patternId) {
      logs += `[${new Date().toISOString()}] Using feature pattern #${patternResult.patternId} with confidence ${patternResult.confidence.toFixed(2)}\n`;
      recordPatternMatchSuccess(4000); // Features often require more tokens
      
      // Record successful pattern usage
      recordFeaturePatternResult(patternResult.patternId, true);
      
      logs += `[${new Date().toISOString()}] Feature addition complete using pattern learning\n`;
      
      return { 
        files: patternResult.updatedFiles, 
        explanation: patternResult.explanation || "Features added using our pattern learning system",
        logs
      };
    }
    
    // Fall back to OpenAI
    logs += `[${new Date().toISOString()}] No suitable feature pattern found, using OpenAI API\n`;
    recordApiFallback(4000);
    
    const result = await addModFeatures(files, newFeatureDescription);
    
    // Store the successful generation for future use
    if (result.files && result.files.length > 0) {
      await storeFeaturePattern(
        files,
        result.files,
        newFeatureDescription,
        result.explanation
      );
    }
    
    return result;
  } catch (error) {
    logs += `[${new Date().toISOString()}] Error in feature addition: ${error instanceof Error ? error.message : String(error)}\n`;
    console.error('Error in smartAddFeatures:', error);
    
    return {
      files,
      explanation: "Failed to add features",
      logs
    };
  }
}

// Import the missing functions to prevent TypeScript errors
import { storeDocumentationPattern } from './documentation-pattern-learning';
import { storeFeaturePattern } from './feature-pattern-learning';