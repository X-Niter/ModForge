import express from 'express';
import { pool } from '../db';
import { getUsageMetrics } from '../ai-service-manager';

const patternLearningRouter = express.Router();

/**
 * Get overall pattern learning metrics
 */
patternLearningRouter.get('/metrics', async (req, res) => {
  try {
    // Get the usage metrics tracked by the AI service manager
    const metrics = getUsageMetrics();
    
    // Initialize response with default values
    const response = {
      overall: {
        totalPatterns: 0,
        totalUses: metrics.patternMatches,
        apiCalls: metrics.apiCalls,
        successRate: 0,
        estimatedTokensSaved: metrics.estimatedTokensSaved,
        estimatedCostSaved: metrics.estimatedCostSaved,
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

    try {
      // Connect to the database directly
      const client = await pool.connect();
      
      // Check if code_patterns table exists
      let tableExists = await client.query(`
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' AND table_name = 'code_patterns'
        );
      `);
      
      if (tableExists.rows[0].exists) {
        // Get code pattern metrics
        const patterns = await client.query(`
          SELECT COUNT(*) as count FROM code_patterns;
        `);
        
        response.byType.code.patterns = parseInt(patterns.rows[0].count, 10);
        response.overall.totalPatterns += response.byType.code.patterns;
        
        if (response.byType.code.patterns > 0) {
          const stats = await client.query(`
            SELECT 
              SUM(use_count) as total_uses,
              AVG(success_rate) as avg_success
            FROM code_patterns;
          `);
          
          response.byType.code.uses = parseInt(stats.rows[0].total_uses || '0', 10);
          response.byType.code.successRate = Math.round(parseFloat(stats.rows[0].avg_success || '0'));
        }
      }
      
      // Check if error_patterns table exists
      tableExists = await client.query(`
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' AND table_name = 'error_patterns'
        );
      `);
      
      if (tableExists.rows[0].exists) {
        // Get error pattern metrics
        const patterns = await client.query(`
          SELECT COUNT(*) as count FROM error_patterns;
        `);
        
        response.byType.error.patterns = parseInt(patterns.rows[0].count, 10);
        response.overall.totalPatterns += response.byType.error.patterns;
        
        if (response.byType.error.patterns > 0) {
          const stats = await client.query(`
            SELECT 
              SUM(success_count) as success_count,
              SUM(failure_count) as failure_count
            FROM error_patterns;
          `);
          
          const successCount = parseInt(stats.rows[0].success_count || '0', 10);
          const failCount = parseInt(stats.rows[0].failure_count || '0', 10);
          
          response.byType.error.uses = successCount + failCount;
          response.byType.error.successRate = response.byType.error.uses > 0 
            ? Math.round((successCount / response.byType.error.uses) * 100)
            : 0;
        }
      }
      
      // Release client back to pool
      client.release();
      
      // Calculate overall success rate
      let totalSuccessScore = 0;
      let totalUsedPatterns = 0;
      
      Object.values(response.byType).forEach(typeStats => {
        if (typeStats.uses > 0) {
          totalSuccessScore += typeStats.uses * typeStats.successRate;
          totalUsedPatterns += typeStats.uses;
        }
      });
      
      response.overall.successRate = totalUsedPatterns > 0
        ? Math.round(totalSuccessScore / totalUsedPatterns)
        : 0;
      
      res.json(response);
    } catch (dbError) {
      console.error('Database error in pattern metrics:', dbError);
      // Still return the response with default values if DB fails
      res.json(response);
    }
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
    
    // Get a client from the pool
    const client = await pool.connect();
    
    try {
      switch (type) {
        case 'code':
          // Check if table exists
          const codeTableCheck = await client.query(`
            SELECT EXISTS (
              SELECT FROM information_schema.tables 
              WHERE table_schema = 'public' AND table_name = 'code_patterns'
            );
          `);
          
          if (codeTableCheck.rows[0].exists) {
            const result = await client.query(`
              SELECT * FROM code_patterns ORDER BY created_at DESC LIMIT ${limit};
            `);
            res.json(result.rows || []);
          } else {
            res.json([]);
          }
          break;
          
        case 'error':
          // Check if table exists
          const errorTableCheck = await client.query(`
            SELECT EXISTS (
              SELECT FROM information_schema.tables 
              WHERE table_schema = 'public' AND table_name = 'error_patterns'
            );
          `);
          
          if (errorTableCheck.rows[0].exists) {
            const result = await client.query(`
              SELECT * FROM error_patterns ORDER BY created_at DESC LIMIT ${limit};
            `);
            res.json(result.rows || []);
          } else {
            res.json([]);
          }
          break;
          
        default:
          res.status(400).json({ error: 'Invalid pattern type or not yet implemented' });
          break;
      }
    } finally {
      // Always release the client back to the pool
      client.release();
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