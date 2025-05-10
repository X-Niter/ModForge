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
    
    // Get pattern counts from database with error handling
    let codePatterns = [], errorPatterns = [], ideaPatterns = [];
    let expansionPatterns = [], featurePatterns = [], docPatterns = [];
    
    try {
      codePatterns = await db.select({
        id: schema.codePatterns.id,
        useCount: schema.codePatterns.useCount,
        successRate: schema.codePatterns.successRate
      }).from(schema.codePatterns);
    } catch (error) {
      console.log('Code patterns table not available:', error.message);
    }
    
    try {
      errorPatterns = await db.select({
        id: schema.errorPatterns.id,
        successCount: schema.errorPatterns.successCount,
        failureCount: schema.errorPatterns.failureCount
      }).from(schema.errorPatterns);
    } catch (error) {
      console.log('Error patterns table not available:', error.message);
    }
    
    try {
      ideaPatterns = await db.select({
        id: schema.ideaPatterns.id,
        useCount: schema.ideaPatterns.useCount,
        successRate: schema.ideaPatterns.successRate
      }).from(schema.ideaPatterns);
    } catch (error) {
      console.log('Idea patterns table not available:', error.message);
    }
    
    try {
      expansionPatterns = await db.select({
        id: schema.ideaExpansionPatterns.id,
        useCount: schema.ideaExpansionPatterns.useCount,
        successRate: schema.ideaExpansionPatterns.successRate
      }).from(schema.ideaExpansionPatterns);
    } catch (error) {
      console.log('Expansion patterns table not available:', error.message);
    }
    
    try {
      featurePatterns = await db.select({
        id: schema.featurePatterns.id,
        useCount: schema.featurePatterns.useCount,
        successRate: schema.featurePatterns.successRate
      }).from(schema.featurePatterns);
    } catch (error) {
      console.log('Feature patterns table not available:', error.message);
    }
    
    try {
      docPatterns = await db.select({
        id: schema.documentationPatterns.id,
        useCount: schema.documentationPatterns.useCount,
        successRate: schema.documentationPatterns.successRate
      }).from(schema.documentationPatterns);
    } catch (error) {
      console.log('Documentation patterns table not available:', error.message);
    }
    
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
        try {
          const patterns = await db.select({
            id: schema.codePatterns.id,
            patternType: schema.codePatterns.patternType,
            modLoader: schema.codePatterns.modLoader,
            minecraftVersion: schema.codePatterns.minecraftVersion,
            useCount: schema.codePatterns.useCount,
            successRate: schema.codePatterns.successRate,
            createdAt: schema.codePatterns.createdAt
          }).from(schema.codePatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying code patterns:', error.message);
          res.json([]);
        }
        break;
        
      case 'error':
        try {
          const patterns = await db.select({
            id: schema.errorPatterns.id,
            errorType: schema.errorPatterns.errorType,
            modLoader: schema.errorPatterns.modLoader,
            successCount: schema.errorPatterns.successCount,
            failureCount: schema.errorPatterns.failureCount,
            createdAt: schema.errorPatterns.createdAt
          }).from(schema.errorPatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying error patterns:', error.message);
          res.json([]);
        }
        break;
        
      case 'idea':
        try {
          const patterns = await db.select({
            id: schema.ideaPatterns.id,
            category: schema.ideaPatterns.category,
            useCount: schema.ideaPatterns.useCount,
            successRate: schema.ideaPatterns.successRate,
            createdAt: schema.ideaPatterns.createdAt
          }).from(schema.ideaPatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying idea patterns:', error.message);
          res.json([]);
        }
        break;
        
      case 'expansion':
        try {
          const patterns = await db.select({
            id: schema.ideaExpansionPatterns.id,
            originalIdeaTitle: schema.ideaExpansionPatterns.originalIdeaTitle,
            useCount: schema.ideaExpansionPatterns.useCount,
            successRate: schema.ideaExpansionPatterns.successRate,
            createdAt: schema.ideaExpansionPatterns.createdAt
          }).from(schema.ideaExpansionPatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying expansion patterns:', error.message);
          res.json([]);
        }
        break;
        
      case 'feature':
        try {
          const patterns = await db.select({
            id: schema.featurePatterns.id,
            featureType: schema.featurePatterns.featureType,
            modLoader: schema.featurePatterns.modLoader,
            useCount: schema.featurePatterns.useCount,
            successRate: schema.featurePatterns.successRate,
            createdAt: schema.featurePatterns.createdAt
          }).from(schema.featurePatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying feature patterns:', error.message);
          res.json([]);
        }
        break;
        
      case 'documentation':
        try {
          const patterns = await db.select({
            id: schema.documentationPatterns.id,
            codeType: schema.documentationPatterns.codeType,
            language: schema.documentationPatterns.language,
            style: schema.documentationPatterns.style,
            useCount: schema.documentationPatterns.useCount,
            successRate: schema.documentationPatterns.successRate,
            createdAt: schema.documentationPatterns.createdAt
          }).from(schema.documentationPatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying documentation patterns:', error.message);
          res.json([]);
        }
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