/**
 * Health check routes for the ModForge application
 * 
 * These routes provide system health information for monitoring and diagnostics.
 */

import express from 'express';
import { checkSystemHealth } from '../health-check';
import { getLogger } from '../logging';

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
      res.json(health);
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

export default router;