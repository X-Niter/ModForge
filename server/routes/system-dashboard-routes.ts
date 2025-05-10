/**
 * System Dashboard Routes for ModForge
 * 
 * These routes provide a comprehensive overview of the entire system status,
 * bringing together health checks, error tracking, metrics, and more.
 */

import express from 'express';
import { getLogger } from '../logging';
import { checkSystemHealth } from '../health-check';
import { getErrorStatistics, getTrackedErrors } from '../error-tracker';
import { ErrorSeverity } from '../error-types';
import { getScheduledJobsStatus } from '../scheduled-jobs';
import { getUsageMetrics } from '../ai-service-manager';
import { continuousService } from '../continuous-service';
import os from 'os';
import { exec } from 'child_process';
import { promisify } from 'util';

const router = express.Router();
const logger = getLogger('system-dashboard');
const execAsync = promisify(exec);

// Calculate uptime in a more human-readable format
function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / (3600 * 24));
  const hours = Math.floor((seconds % (3600 * 24)) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = Math.floor(seconds % 60);
  
  return `${days}d ${hours}h ${minutes}m ${remainingSeconds}s`;
}

// Dashboard data endpoint - combines multiple data sources
router.get('/', async (req, res) => {
  try {
    // Get system health data
    const health = await checkSystemHealth();
    
    // Get error statistics
    const errorStats = getErrorStatistics();
    
    // Get scheduled job status
    const scheduledJobs = getScheduledJobsStatus();
    
    // Get usage metrics
    const usageMetrics = getUsageMetrics();
    
    // Get continuous development status
    const continuousHealth = continuousService.getHealthStatus();
    
    // Get system stats
    const systemInfo = {
      platform: process.platform,
      architecture: process.arch,
      nodeVersion: process.version,
      cpus: os.cpus().length,
      totalMemory: Math.round(os.totalmem() / (1024 * 1024 * 1024) * 100) / 100, // GB
      freeMemory: Math.round(os.freemem() / (1024 * 1024 * 1024) * 100) / 100, // GB
      loadAverage: os.loadavg(),
      uptime: {
        os: formatUptime(os.uptime()),
        process: formatUptime(process.uptime())
      }
    };
    
    // Get most recent critical errors
    const recentCriticalErrors = getTrackedErrors({
      severity: ErrorSeverity.CRITICAL,
      resolved: false,
      since: new Date(Date.now() - 24 * 60 * 60 * 1000) // Last 24 hours
    }).slice(0, 5); // Limit to 5
    
    // Combine all data into a comprehensive system overview
    const dashboard = {
      status: health.status,
      timestamp: new Date().toISOString(),
      systemHealth: {
        status: health.status,
        database: health.database.status,
        fileSystem: health.fileSystem.status,
        memory: health.memory.status
      },
      errorTracking: {
        total: errorStats.total,
        unresolved: errorStats.unresolved,
        critical: errorStats.bySeverity.critical,
        high: errorStats.bySeverity.high,
        recentCritical: recentCriticalErrors.map(error => ({
          id: error.id,
          message: error.message,
          lastSeen: error.lastSeen,
          count: error.count,
          category: error.category
        }))
      },
      scheduledJobs: {
        total: scheduledJobs.length,
        healthy: scheduledJobs.filter(job => job.status === 'healthy').length,
        failing: scheduledJobs.filter(job => job.status === 'failing').length,
        disabled: scheduledJobs.filter(job => job.status === 'disabled').length
      },
      aiMetrics: {
        totalRequests: usageMetrics.totalRequests,
        patternMatches: usageMetrics.patternMatches,
        apiCalls: usageMetrics.apiCalls,
        costSaved: usageMetrics.estimatedCostSaved,
        effectivenessRatio: usageMetrics.totalRequests > 0 
          ? (usageMetrics.patternMatches / usageMetrics.totalRequests * 100).toFixed(1) + '%'
          : '0%'
      },
      continuousDevelopment: {
        runningMods: continuousHealth.runningMods.length,
        stoppedMods: 0, // We don't have this metric directly, calculate as needed
        circuitBreakers: continuousHealth.trippedCircuitBreakers.length
      },
      systemInfo,
      processInfo: {
        memory: process.memoryUsage(),
        memoryUsedMB: Math.round(process.memoryUsage().heapUsed / (1024 * 1024) * 100) / 100,
        memoryTotalMB: Math.round(process.memoryUsage().heapTotal / (1024 * 1024) * 100) / 100,
        rssMB: Math.round(process.memoryUsage().rss / (1024 * 1024) * 100) / 100
      }
    };
    
    res.json({
      success: true,
      dashboard
    });
  } catch (error) {
    logger.error('Error generating system dashboard', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to generate system dashboard',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Get disk space usage
router.get('/disk-usage', async (req, res) => {
  try {
    // This is Unix-specific, would need adaptation for Windows
    const { stdout } = await execAsync('df -h / | tail -n 1');
    const parts = stdout.trim().split(/\s+/);
    
    const diskUsage = {
      filesystem: parts[0],
      size: parts[1],
      used: parts[2],
      available: parts[3],
      percentUsed: parts[4],
      mountPoint: parts[5]
    };
    
    res.json({
      success: true,
      data: diskUsage,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Error getting disk usage', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to get disk usage',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Get system load and resource usage over time 
router.get('/load-history', async (req, res) => {
  try {
    // In a full implementation, this would query a time-series database
    // For now, we'll just return the current values
    const loadAvg = os.loadavg();
    const cpuCount = os.cpus().length;
    const memoryUsage = process.memoryUsage();
    const systemMemory = {
      total: os.totalmem(),
      free: os.freemem(),
      percentUsed: ((os.totalmem() - os.freemem()) / os.totalmem() * 100).toFixed(1)
    };
    
    res.json({
      success: true,
      data: {
        currentLoad: {
          loadAvg1min: loadAvg[0],
          loadAvg5min: loadAvg[1],
          loadAvg15min: loadAvg[2],
          cpuCount,
          // Load per CPU core provides a better metric of system stress
          loadPerCpu1min: (loadAvg[0] / cpuCount).toFixed(2),
          loadPerCpu5min: (loadAvg[1] / cpuCount).toFixed(2),
          loadPerCpu15min: (loadAvg[2] / cpuCount).toFixed(2)
        },
        memory: {
          system: systemMemory,
          process: {
            rss: memoryUsage.rss,
            heapTotal: memoryUsage.heapTotal,
            heapUsed: memoryUsage.heapUsed,
            external: memoryUsage.external,
            percentHeapUsed: (memoryUsage.heapUsed / memoryUsage.heapTotal * 100).toFixed(1)
          }
        }
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Error getting load history', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to get load history',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

export default router;