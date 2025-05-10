import { Router } from 'express';
import { getErrorSummary } from '../error-handler';
import { getUsageMetrics } from '../ai-service-manager';

const errorMonitoringRouter = Router();

/**
 * Get an overview of system errors
 * Provides aggregated error statistics and recent error entries
 */
errorMonitoringRouter.get('/summary', async (req, res) => {
  try {
    const summary = getErrorSummary();
    res.json({
      success: true,
      ...summary
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve error summary',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

/**
 * Get detailed error logs
 * Filters can be applied via query parameters:
 * - category: Filter by error category
 * - severity: Filter by error severity
 * - from: Start date (ISO string)
 * - to: End date (ISO string)
 * - limit: Maximum number of results to return (default: 50)
 */
errorMonitoringRouter.get('/logs', async (req, res) => {
  try {
    // In a real implementation, would fetch from a database or log storage
    // For now, returns a placeholder empty array
    res.json({
      success: true,
      errors: [],
      total: 0,
      page: 1,
      limit: parseInt(req.query.limit as string) || 50
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve error logs',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

/**
 * Get system health based on error metrics
 * Returns an assessment of system health based on error rates and severities
 */
errorMonitoringRouter.get('/health', async (req, res) => {
  try {
    const summary = getErrorSummary();
    
    // In a real implementation, would analyze error trends and determine health status
    // For now, returns a placeholder status
    res.json({
      success: true,
      status: 'healthy',
      message: 'All systems operating normally',
      errorRate: 0,
      criticalErrors: 0,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to assess system health',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

/**
 * Get pattern learning effectiveness metrics
 * Returns statistics on pattern usage, API cost savings, and system efficiency
 */
errorMonitoringRouter.get('/pattern-learning', async (req, res) => {
  try {
    const metrics = getUsageMetrics();
    
    // Calculate additional meaningful metrics
    const patternMatchRate = metrics.totalRequests > 0 
      ? (metrics.patternMatches / metrics.totalRequests) * 100 
      : 0;
    
    // Provide a comprehensive overview of pattern learning effectiveness
    res.json({
      success: true,
      metrics: {
        ...metrics,
        patternMatchRate: patternMatchRate.toFixed(2) + '%',
        averageCostSavingsPerRequest: metrics.totalRequests > 0 
          ? (metrics.estimatedCostSaved / metrics.totalRequests).toFixed(5) 
          : '0.00000',
        efficiencyScore: patternMatchRate > 80 ? 'Excellent' :
                         patternMatchRate > 60 ? 'Good' :
                         patternMatchRate > 40 ? 'Average' :
                         patternMatchRate > 20 ? 'Fair' : 'Poor'
      },
      recommendations: [
        patternMatchRate < 30 ? 'Consider adding more diverse patterns to improve match rate' : null,
        metrics.totalRequests > 1000 && patternMatchRate < 50 ? 'High volume with low match rate - review pattern quality' : null,
        metrics.estimatedCostSaved > 50 ? 'Significant cost savings achieved - continue current approach' : null
      ].filter(Boolean),
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve pattern learning metrics',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

export default errorMonitoringRouter;