/**
 * Health check routes for the ModForge application
 * 
 * These routes provide system health information for monitoring and diagnostics.
 */

import express from 'express';
import { checkSystemHealth } from '../health-check';
import { getLogger } from '../logging';
import { getScheduledJobsStatus, resetJob } from '../scheduled-jobs';

const router = express.Router();
const logger = getLogger('health-api');

// Basic health check endpoint - returns minimal information
router.get('/live', (req, res) => {
  res.json({
    status: 'up',
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});

// Ready check endpoint - confirms if the system is ready to serve requests
router.get('/ready', async (req, res) => {
  try {
    const health = await checkSystemHealth();
    
    // If system is unhealthy, return 503 Service Unavailable
    if (health.status === 'unhealthy') {
      return res.status(503).json({
        status: health.status,
        timestamp: health.timestamp,
        message: 'System is not ready to serve requests'
      });
    }
    
    // If system is degraded, still return success but include warning
    const response = {
      status: health.status,
      timestamp: health.timestamp,
      message: health.status === 'degraded' 
        ? 'System is operational but experiencing issues' 
        : 'System is ready to serve requests',
      uptime: health.uptime
    };
    
    res.json(response);
  } catch (error) {
    logger.error('Health check failed', { error });
    
    res.status(500).json({
      status: 'error',
      timestamp: new Date().toISOString(),
      message: 'Failed to check system health'
    });
  }
});

// Detailed health check endpoint - requires authentication
router.get('/status', async (req, res) => {
  try {
    // Check if user is admin - only admins can see detailed health information
    if (req.user?.isAdmin) {
      const health = await checkSystemHealth();
      const scheduledJobs = getScheduledJobsStatus();
      
      // Enhance health report with scheduled jobs information
      const enhancedHealth = {
        ...health,
        scheduledJobs: {
          total: scheduledJobs.length,
          healthy: scheduledJobs.filter(job => job.status === 'healthy').length,
          failing: scheduledJobs.filter(job => job.status === 'failing').length,
          disabled: scheduledJobs.filter(job => job.status === 'disabled').length,
          details: scheduledJobs
        }
      };
      
      res.json(enhancedHealth);
    } else {
      // For non-admin users, provide minimal information
      const health = await checkSystemHealth();
      
      res.json({
        status: health.status,
        timestamp: health.timestamp,
        uptime: health.uptime,
        message: health.status === 'healthy' 
          ? 'All systems operational' 
          : 'System experiencing issues, administrators have been notified'
      });
    }
  } catch (error) {
    logger.error('Detailed health check failed', { error });
    
    res.status(500).json({
      status: 'error',
      timestamp: new Date().toISOString(),
      message: 'Failed to check system health'
    });
  }
});

// Reset a failing scheduled job
router.post('/jobs/:jobId/reset', async (req, res) => {
  // Only admins can reset jobs
  if (!req.user?.isAdmin) {
    return res.status(403).json({
      success: false,
      message: 'Administrator privileges required'
    });
  }
  
  const jobId = req.params.jobId;
  
  try {
    const success = resetJob(jobId);
    
    if (success) {
      logger.info(`Admin ${req.user.username} reset scheduled job ${jobId}`);
      res.json({
        success: true,
        message: `Job reset successfully`,
        timestamp: new Date().toISOString()
      });
    } else {
      res.status(404).json({
        success: false,
        message: `Job with ID ${jobId} not found`,
        timestamp: new Date().toISOString()
      });
    }
  } catch (error) {
    logger.error('Failed to reset job', { jobId, error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to reset job',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

export default router;