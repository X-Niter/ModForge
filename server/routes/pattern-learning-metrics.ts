import express from 'express';
import { db } from '../db';
import * as schema from '@shared/schema';
import { getUsageMetrics } from '../ai-service-manager';

const patternLearningRouter = express.Router();

/**
 * Get overall pattern learning metrics
 */
patternLearningRouter.get('/metrics', async (req, res) => {
  try {
    // Get the usage metrics tracked by the AI service manager
    const usageMetrics = getUsageMetrics();
    
    // Response structure that will be populated with actual data when available
    const response = {
      overall: {
        totalPatterns: 0,
        totalUses: usageMetrics.patternMatches,
        apiCalls: usageMetrics.apiCalls,
        successRate: 0,
        estimatedTokensSaved: usageMetrics.estimatedTokensSaved,
        estimatedCostSaved: usageMetrics.estimatedCostSaved,
      },
      byType: {
        code: { patterns: 0, uses: 0, successRate: 0 },
        error: { patterns: 0, uses: 0, successRate: 0 },
        idea: { patterns: 0, uses: 0, successRate: 0 },
        expansion: { patterns: 0, uses: 0, successRate: 0 },
        feature: { patterns: 0, uses: 0, successRate: 0 },
        documentation: { patterns: 0, uses: 0, successRate: 0 },
      }
    };
    
    // Try to get code pattern metrics
    try {
      const count = await db.select({ count: db.fn.count() }).from(schema.codePatterns);
      if (count && count[0]) {
        response.byType.code.patterns = Number(count[0].count) || 0;
        response.overall.totalPatterns += response.byType.code.patterns;
        
        // If we have patterns, get usage and success metrics
        if (response.byType.code.patterns > 0) {
          const usageStats = await db.select({
            totalUses: db.fn.sum(schema.codePatterns.useCount),
            avgSuccess: db.fn.avg(schema.codePatterns.successRate)
          }).from(schema.codePatterns);
          
          if (usageStats && usageStats[0]) {
            response.byType.code.uses = Number(usageStats[0].totalUses) || 0;
            response.byType.code.successRate = Math.round(Number(usageStats[0].avgSuccess) || 0);
          }
        }
      }
    } catch (error) {
      console.log('Code patterns table not available:', error instanceof Error ? error.message : 'Unknown error');
    }
    
    // Try to get error pattern metrics
    try {
      const count = await db.select({ count: db.fn.count() }).from(schema.errorPatterns);
      if (count && count[0]) {
        response.byType.error.patterns = Number(count[0].count) || 0;
        response.overall.totalPatterns += response.byType.error.patterns;
        
        // If we have patterns, get usage and success metrics
        if (response.byType.error.patterns > 0) {
          const stats = await db.select({
            successCount: db.fn.sum(schema.errorPatterns.successCount),
            failCount: db.fn.sum(schema.errorPatterns.failureCount)
          }).from(schema.errorPatterns);
          
          if (stats && stats[0]) {
            const successCount = Number(stats[0].successCount) || 0;
            const failCount = Number(stats[0].failCount) || 0;
            
            response.byType.error.uses = successCount + failCount;
            response.byType.error.successRate = response.byType.error.uses > 0 
              ? Math.round((successCount / response.byType.error.uses) * 100)
              : 0;
          }
        }
      }
    } catch (error) {
      console.log('Error patterns table not available:', error instanceof Error ? error.message : 'Unknown error');
    }
    
    // Calculate weighted overall success rate based on available pattern types
    let totalSuccessScore = 0;
    let totalWeightedUses = 0;
    
    Object.values(response.byType).forEach(typeStats => {
      if (typeStats.uses > 0) {
        totalSuccessScore += typeStats.uses * typeStats.successRate;
        totalWeightedUses += typeStats.uses;
      }
    });
    
    response.overall.successRate = totalWeightedUses > 0
      ? Math.round(totalSuccessScore / totalWeightedUses)
      : 0;
    
    res.json(response);
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
          const patterns = await db.select().from(schema.codePatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying code patterns:', error instanceof Error ? error.message : 'Unknown error');
          res.json([]);
        }
        break;
        
      case 'error':
        try {
          const patterns = await db.select().from(schema.errorPatterns).limit(limit);
          res.json(patterns);
        } catch (error) {
          console.error('Error querying error patterns:', error instanceof Error ? error.message : 'Unknown error');
          res.json([]);
        }
        break;
        
      default:
        res.status(400).json({ error: 'Invalid pattern type or not yet implemented' });
        break;
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