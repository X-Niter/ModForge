import { Router } from 'express';
import { getUsageMetrics } from '../ai-service-manager';
import { db } from '../db';
import * as schema from '@shared/schema';

/**
 * Router for pattern learning metrics
 * This provides endpoints to monitor the effectiveness of the pattern learning system
 */
const patternLearningRouter = Router();

// Calculate the average success rate for a set of patterns
function calculateAverageSuccessRate(patterns: any[]): number {
  if (patterns.length === 0) return 0;
  
  const totalSuccessRate = patterns.reduce(
    (sum: number, pattern: any) => sum + Number(pattern.successRate || 0), 
    0
  );
  
  return Math.round(totalSuccessRate / patterns.length);
}

/**
 * Get overall pattern learning metrics
 */
patternLearningRouter.get('/metrics', async (req, res) => {
  try {
    // Get the usage metrics tracked by the AI service manager
    const usageMetrics = getUsageMetrics();
    
    // Get pattern counts from database
    const codePatterns = await db.select().from(schema.codePatterns);
    const errorPatterns = await db.select().from(schema.errorPatterns);
    const ideaPatterns = await db.select().from(schema.ideaPatterns);
    const expansionPatterns = await db.select().from(schema.ideaExpansionPatterns);
    const featurePatterns = await db.select().from(schema.featurePatterns);
    const docPatterns = await db.select().from(schema.documentationPatterns);
    
    // Calculate total patterns
    const totalPatterns = 
      codePatterns.length + 
      errorPatterns.length + 
      ideaPatterns.length + 
      expansionPatterns.length + 
      featurePatterns.length + 
      docPatterns.length;
    
    // Calculate usage statistics
    const totalCodeUses = codePatterns.reduce((sum, p) => sum + Number(p.useCount || 0), 0);
    const totalIdeaUses = ideaPatterns.reduce((sum, p) => sum + Number(p.useCount || 0), 0);
    const totalDocUses = docPatterns.reduce((sum, p) => sum + Number(p.useCount || 0), 0);
    
    // Calculate success rates
    const codeSuccessRate = calculateAverageSuccessRate(codePatterns);
    const ideaSuccessRate = calculateAverageSuccessRate(ideaPatterns);
    const docSuccessRate = calculateAverageSuccessRate(docPatterns);
    
    // Error patterns have a different success metric
    const errorSuccessCount = errorPatterns.reduce(
      (sum, p) => sum + Number(p.successCount || 0), 
      0
    );
    const errorFailureCount = errorPatterns.reduce(
      (sum, p) => sum + Number(p.failureCount || 0), 
      0
    );
    const errorSuccessRate = errorSuccessCount + errorFailureCount > 0 
      ? Math.round((errorSuccessCount / (errorSuccessCount + errorFailureCount)) * 100)
      : 0;
      
    // Feature patterns use standard success rate  
    const featureSuccessRate = calculateAverageSuccessRate(featurePatterns);
    const featureUseCount = featurePatterns.reduce(
      (sum, p) => sum + Number(p.useCount || 0), 
      0
    );
      
    // Calculate the overall success rate 
    const successRates = [
      codeSuccessRate,
      errorSuccessRate,
      ideaSuccessRate,
      docSuccessRate,
      featureSuccessRate,
    ].filter(rate => rate > 0);
    
    const overallSuccessRate = successRates.length > 0
      ? Math.round(successRates.reduce((sum, rate) => sum + rate, 0) / successRates.length)
      : 0;
    
    res.json({
      overall: {
        totalPatterns,
        totalUses: usageMetrics.patternMatches,
        apiCalls: usageMetrics.apiCalls,
        successRate: overallSuccessRate,
        estimatedTokensSaved: usageMetrics.estimatedTokensSaved,
        estimatedCostSaved: usageMetrics.estimatedCostSaved,
      },
      byType: {
        code: {
          patterns: codePatterns.length,
          uses: totalCodeUses,
          successRate: codeSuccessRate,
        },
        error: {
          patterns: errorPatterns.length,
          uses: errorSuccessCount + errorFailureCount,
          successRate: errorSuccessRate,
        },
        idea: {
          patterns: ideaPatterns.length,
          uses: totalIdeaUses,
          successRate: ideaSuccessRate,
        },
        feature: {
          patterns: featurePatterns.length,
          uses: featureUseCount,
          successRate: featureSuccessRate,
        },
        documentation: {
          patterns: docPatterns.length,
          uses: totalDocUses,
          successRate: docSuccessRate,
        },
      }
    });
  } catch (error) {
    console.error('Error getting pattern learning metrics:', error);
    res.status(500).json({ 
      error: 'Failed to get pattern learning metrics',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

/**
 * Get detailed pattern information (limited to 100 patterns per type)
 */
patternLearningRouter.get('/patterns/:type', async (req, res) => {
  try {
    const type = req.params.type;
    const limit = 100;
    
    switch (type) {
      case 'code':
        const codePatterns = await db.select().from(schema.codePatterns).limit(limit);
        res.json(codePatterns);
        break;
        
      case 'error':
        const errorPatterns = await db.select().from(schema.errorPatterns).limit(limit);
        res.json(errorPatterns);
        break;
        
      case 'idea':
        const ideaPatterns = await db.select().from(schema.ideaPatterns).limit(limit);
        res.json(ideaPatterns);
        break;
        
      case 'expansion':
        const expansionPatterns = await db.select().from(schema.ideaExpansionPatterns).limit(limit);
        res.json(expansionPatterns);
        break;
        
      case 'feature':
        const featurePatterns = await db.select().from(schema.featurePatterns).limit(limit);
        res.json(featurePatterns);
        break;
        
      case 'documentation':
        const docPatterns = await db.select().from(schema.documentationPatterns).limit(limit);
        res.json(docPatterns);
        break;
        
      default:
        res.status(400).json({ error: 'Invalid pattern type' });
    }
  } catch (error) {
    console.error(`Error getting ${req.params.type} patterns:`, error);
    res.status(500).json({ 
      error: `Failed to get ${req.params.type} patterns`,
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

export default patternLearningRouter;