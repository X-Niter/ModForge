import express from 'express';
import { pool } from '../db';
import { getUsageMetrics } from '../ai-service-manager';

const apiMetricsRouter = express.Router();

/**
 * Get usage metrics for the API
 */
apiMetricsRouter.get('/usage', (req, res) => {
  try {
    // Get metrics from AI service manager
    const metrics = getUsageMetrics();
    
    // Calculate rates as percentages
    const total = metrics.totalRequests || 1; // Avoid division by zero
    const patternMatchRate = `${Math.round((metrics.patternMatches / total) * 100)}%`;
    const apiCallRate = `${Math.round((metrics.apiCalls / total) * 100)}%`;
    
    // Format cost for display
    const estimatedCostSaved = Number(metrics.estimatedCostSaved.toFixed(2));
    
    res.json({
      totalRequests: metrics.totalRequests,
      patternMatches: metrics.patternMatches,
      apiCalls: metrics.apiCalls,
      estimatedTokensSaved: metrics.estimatedTokensSaved,
      estimatedCostSaved,
      patternMatchRate,
      apiCallRate
    });
  } catch (error) {
    console.error('Error getting API usage metrics:', error);
    res.status(500).json({ error: 'Failed to get API usage metrics' });
  }
});

/**
 * Get database health status
 */
apiMetricsRouter.get('/db-health', async (req, res) => {
  try {
    // Test database connection
    const client = await pool.connect();
    const result = await client.query('SELECT NOW() as time');
    client.release();
    
    res.json({
      status: 'healthy',
      timestamp: result.rows[0].time,
      connectionPool: {
        total: pool.totalCount,
        idle: pool.idleCount,
        waiting: pool.waitingCount
      }
    });
  } catch (error) {
    console.error('Database health check failed:', error);
    res.status(500).json({
      status: 'unhealthy',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

export default apiMetricsRouter;