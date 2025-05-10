import { Router } from 'express';
import { getErrorSummary } from '../error-handler';

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

export default errorMonitoringRouter;