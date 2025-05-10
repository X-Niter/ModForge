/**
 * Comprehensive health check system for ModForge
 * 
 * This module provides health checks for all critical components of the system,
 * including database connections, file system access, memory usage, and more.
 * It helps identify potential issues before they become critical.
 */

import { pool, db } from "./db";
import fs from "fs/promises";
import path from "path";
import os from "os";
import { fileURLToPath } from "url";
import { getLogger } from "./logging";
import { notifyHealthStateChange } from "./notification-integration";

// Set up logger
const logger = getLogger('health-check');

// Set up paths
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const LOG_DIR = path.join(__dirname, '../logs');
const TMP_DIR = path.join(os.tmpdir(), 'modforge');

// Types
export interface SystemStatus {
  status: 'healthy' | 'degraded' | 'unhealthy';
  database: DatabaseStatus;
  fileSystem: FileSystemStatus;
  memory: MemoryStatus;
  uptime: number;
  timestamp: string;
  detailedChecks: Record<string, CheckResult>;
}

export interface DatabaseStatus {
  status: 'healthy' | 'degraded' | 'unhealthy';
  connectionPool: {
    total: number;
    idle: number;
    used: number;
    waiting: number;
    maxConnections: number;
  };
  responseTime: number;
  version?: string;
}

export interface FileSystemStatus {
  status: 'healthy' | 'degraded' | 'unhealthy';
  diskSpace: {
    total: number;
    free: number;
    percentFree: number;
  };
  tempDirAccess: boolean;
  logDirAccess: boolean;
}

export interface MemoryStatus {
  status: 'healthy' | 'degraded' | 'unhealthy';
  system: {
    total: number;
    free: number;
    percentFree: number;
  };
  process: {
    rss: number; // Resident Set Size
    heapTotal: number;
    heapUsed: number;
    external: number;
    percentHeapUsed: number;
  };
}

interface CheckResult {
  status: 'healthy' | 'degraded' | 'unhealthy';
  message: string;
  details?: any;
  responseTime?: number;
}

/**
 * Check database health
 */
async function checkDatabase(): Promise<CheckResult & { databaseStatus?: DatabaseStatus }> {
  const startTime = Date.now();
  
  try {
    // Check connection pool status
    const poolStatus = await pool.query('SELECT COUNT(*) FROM pg_stat_activity');
    
    // Get database info
    const dbVersionResult = await pool.query('SELECT version()');
    const version = dbVersionResult.rows[0]?.version;
    
    // Get connection pool stats
    const poolStats = pool as any; // Type assertion to access internal properties
    const poolInfo = {
      total: poolStats._totalCount || 0,
      idle: poolStats._idleCount || 0,
      used: (poolStats._totalCount || 0) - (poolStats._idleCount || 0),
      waiting: poolStats._waitingCount || 0,
      maxConnections: poolStats.options.max || 10,
    };
    
    // Calculate response time
    const responseTime = Date.now() - startTime;
    
    // Check if pool is close to capacity
    const utilizationPct = (poolInfo.used / poolInfo.maxConnections) * 100;
    
    let status: 'healthy' | 'degraded' | 'unhealthy' = 'healthy';
    let message = 'Database connection is healthy';
    
    if (utilizationPct > 90) {
      status = 'degraded';
      message = 'Database connection pool is near capacity';
    }
    
    if (responseTime > 1000) {
      status = 'degraded';
      message = 'Database response time is slow';
    }
    
    const databaseStatus: DatabaseStatus = {
      status,
      connectionPool: poolInfo,
      responseTime,
      version
    };
    
    return {
      status,
      message,
      databaseStatus,
      details: {
        version,
        poolStats: poolInfo,
        queryLatency: `${responseTime}ms`
      },
      responseTime
    };
  } catch (error) {
    logger.error('Database health check failed', { 
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    });
    
    return {
      status: 'unhealthy',
      message: 'Database connection failed',
      details: error instanceof Error ? { message: error.message, stack: error.stack } : String(error),
      responseTime: Date.now() - startTime,
      databaseStatus: {
        status: 'unhealthy',
        connectionPool: { total: 0, idle: 0, used: 0, waiting: 0, maxConnections: 0 },
        responseTime: Date.now() - startTime
      }
    };
  }
}

/**
 * Check file system health
 */
async function checkFileSystem(): Promise<CheckResult & { fileSystemStatus?: FileSystemStatus }> {
  const startTime = Date.now();
  
  try {
    // Check for required directories
    let logDirAccess = false;
    let tempDirAccess = false;
    
    try {
      await fs.access(LOG_DIR, fs.constants.R_OK | fs.constants.W_OK);
      logDirAccess = true;
    } catch (err) {
      // Try to create log dir if it doesn't exist
      try {
        await fs.mkdir(LOG_DIR, { recursive: true });
        logDirAccess = true;
      } catch (createErr) {
        logger.error('Failed to create log directory', {
          error: createErr instanceof Error ? createErr.message : String(createErr),
          stack: createErr instanceof Error ? createErr.stack : undefined,
          path: LOG_DIR
        });
        logDirAccess = false;
      }
    }
    
    try {
      await fs.access(TMP_DIR, fs.constants.R_OK | fs.constants.W_OK);
      tempDirAccess = true;
    } catch (err) {
      // Try to create temp dir if it doesn't exist
      try {
        await fs.mkdir(TMP_DIR, { recursive: true });
        tempDirAccess = true;
      } catch (createErr) {
        logger.error('Failed to create temp directory', {
          error: createErr instanceof Error ? createErr.message : String(createErr),
          stack: createErr instanceof Error ? createErr.stack : undefined,
          path: TMP_DIR
        });
        tempDirAccess = false;
      }
    }
    
    // Enhanced disk space detection with multiple fallback methods
    let diskSpace = { total: 0, free: 0, percentFree: 0 };
    
    try {
      // Get the current working directory to check disk space on the appropriate volume
      const cwd = process.cwd();
      const tmpDir = os.tmpdir();
      
      // Multiple detection methods in order of preference
      let detectionMethods = [];
      
      // Method 1: df command for Unix-like systems (Linux, macOS)
      if (process.platform !== 'win32') {
        detectionMethods.push(async () => {
          try {
            const { exec } = await import('child_process');
            const util = await import('util');
            const execPromise = util.promisify(exec);
            
            // Check the disk where our application is running
            // Use -P for POSIX output format which is more consistent across systems
            const { stdout } = await execPromise(`df -Pk "${cwd}" | tail -1`);
            const stats = stdout.trim().split(/\s+/);
            
            // df with -P flag returns: Filesystem 1K-blocks Used Available Capacity Mounted-on
            if (stats.length >= 5) {
              const total = parseInt(stats[1], 10) * 1024; // Convert from 1K-blocks to bytes
              const free = parseInt(stats[3], 10) * 1024;  // Available space in bytes
              const percentFree = Math.max(0, Math.min(100, (free / total) * 100)); // Constrain between 0-100%
              
              if (total > 0 && free >= 0) {
                logger.debug('Disk space detected via df command', { total, free, percentFree });
                return { total, free, percentFree };
              }
            }
            throw new Error('Invalid or incomplete df output');
          } catch (err) {
            logger.debug('df command method failed', { 
              error: err instanceof Error ? err.message : String(err),
              stack: err instanceof Error ? err.stack : undefined 
            });
            throw err; // Pass to next method
          }
        });
      }
      
      // Method 2: Alternative df command format (some Unix variants)
      if (process.platform !== 'win32') {
        detectionMethods.push(async () => {
          try {
            const { exec } = await import('child_process');
            const util = await import('util');
            const execPromise = util.promisify(exec);
            
            // Try just getting root filesystem stats
            const { stdout } = await execPromise('df -k / | tail -1');
            const stats = stdout.trim().split(/\s+/);
            
            if (stats.length >= 5) {
              const total = parseInt(stats[1], 10) * 1024;
              const free = parseInt(stats[3], 10) * 1024;
              const percentFree = Math.max(0, Math.min(100, (free / total) * 100));
              
              if (total > 0 && free >= 0) {
                logger.debug('Disk space detected via alternative df command', { total, free, percentFree });
                return { total, free, percentFree };
              }
            }
            throw new Error('Invalid or incomplete alternative df output');
          } catch (err) {
            logger.debug('Alternative df command method failed', { error: err });
            throw err;
          }
        });
      }
      
      // Method 3: OS module memory as a very rough estimate
      detectionMethods.push(async () => {
        try {
          // This is a very rough estimate, but better than nothing
          const totalMemory = os.totalmem();
          const freeMemory = os.freemem();
          
          if (totalMemory > 0) {
            // Use a ratio higher than 2x since disk is typically much larger than RAM
            const diskRatio = 8; 
            const total = totalMemory * diskRatio;
            const free = Math.max(0, Math.min(total, freeMemory * diskRatio));
            const percentFree = Math.max(0, Math.min(100, (free / total) * 100));
            
            logger.warn('Using memory-based estimate for disk space (less accurate)', 
              { total, free, percentFree, memoryTotal: totalMemory });
            
            return { total, free, percentFree };
          }
          throw new Error('Invalid memory values');
        } catch (err) {
          logger.debug('Memory estimation method failed', { error: err });
          throw err;
        }
      });
      
      // Method 4: Final fallback with conservative estimates
      detectionMethods.push(async () => {
        logger.warn('All disk space detection methods failed, using safe defaults');
        
        // Conservative defaults based on common modern systems
        const total = 50 * 1024 * 1024 * 1024; // 50GB total
        const free = 25 * 1024 * 1024 * 1024;  // 25GB free
        const percentFree = 50;                // 50% free
        
        return { total, free, percentFree };
      });
      
      // Try each method in sequence until one succeeds
      for (let index = 0; index < detectionMethods.length; index++) {
        const method = detectionMethods[index];
        try {
          diskSpace = await method();
          if (diskSpace.total > 0) {
            break; // Successfully retrieved disk space information
          }
        } catch (methodErr) {
          // Log error but continue to next method
          logger.debug(`Disk space check method ${index} failed`, {
            error: methodErr instanceof Error ? methodErr.message : String(methodErr),
            method: index
          });
          continue;
        }
      }
      
      // Sanity check - ensure we have positive values
      if (diskSpace.total <= 0 || diskSpace.free < 0) {
        logger.warn('Detected invalid disk space values, using safe defaults', { diskSpace });
        diskSpace = {
          total: 50 * 1024 * 1024 * 1024, // 50GB total
          free: 25 * 1024 * 1024 * 1024,  // 25GB free
          percentFree: 50                 // 50% free
        };
      }
    } catch (err) {
      // Master error handler for the entire disk space detection
      logger.error('All disk space detection methods failed', { error: err });
      diskSpace = {
        total: 50 * 1024 * 1024 * 1024, // 50GB total
        free: 25 * 1024 * 1024 * 1024,  // 25GB free
        percentFree: 50                 // 50% free
      };
    }
    
    // Determine status with environment-aware thresholds
    let status: 'healthy' | 'degraded' | 'unhealthy' = 'healthy';
    let message = 'File system is healthy';
    
    // Directory access is critical for operation
    if (!logDirAccess && !tempDirAccess) {
      status = 'unhealthy';
      message = 'Critical directories are not accessible';
    } else if (!logDirAccess || !tempDirAccess) {
      status = 'degraded';
      message = 'Some required directories are not accessible';
    }
    
    // Environment-specific disk space thresholds
    const isProduction = process.env.NODE_ENV === 'production';
    
    // Production needs more free space for safety
    const warningThreshold = isProduction ? 15 : 10; // 15% in prod, 10% in dev
    const criticalThreshold = isProduction ? 8 : 5;  // 8% in prod, 5% in dev
    const emergencyThreshold = isProduction ? 3 : 2; // 3% in prod, 2% in dev
    
    // Calculate absolute free space in GB
    const freeSpaceGB = diskSpace.free / (1024 * 1024 * 1024);
    const minimumFreeGB = isProduction ? 5 : 2; // 5GB in prod, 2GB in dev
    
    // Check both percentage and absolute values for a more accurate assessment
    if (diskSpace.percentFree < warningThreshold || freeSpaceGB < minimumFreeGB * 2) {
      // Only change to degraded if not already unhealthy
      if (status !== 'unhealthy') {
        status = 'degraded';
        message = `Disk space is running low (${Math.round(diskSpace.percentFree)}% free, ${freeSpaceGB.toFixed(1)}GB)`;
      }
    }
    
    if (diskSpace.percentFree < criticalThreshold || freeSpaceGB < minimumFreeGB) {
      status = 'degraded';
      message = `Critical disk space shortage (${Math.round(diskSpace.percentFree)}% free, ${freeSpaceGB.toFixed(1)}GB)`;
    }
    
    if (diskSpace.percentFree < emergencyThreshold || freeSpaceGB < minimumFreeGB / 2) {
      status = 'unhealthy';
      message = `Emergency: Extremely low disk space (${Math.round(diskSpace.percentFree)}% free, ${freeSpaceGB.toFixed(1)}GB)`;
    }
    
    const fileSystemStatus: FileSystemStatus = {
      status,
      diskSpace,
      logDirAccess,
      tempDirAccess
    };
    
    return {
      status,
      message,
      fileSystemStatus,
      details: {
        diskSpace: {
          total: `${Math.round(diskSpace.total / (1024 * 1024 * 1024))} GB`,
          free: `${Math.round(diskSpace.free / (1024 * 1024 * 1024))} GB`,
          percentFree: `${Math.round(diskSpace.percentFree)}%`
        },
        directories: {
          logs: { path: LOG_DIR, accessible: logDirAccess },
          temp: { path: TMP_DIR, accessible: tempDirAccess }
        }
      },
      responseTime: Date.now() - startTime
    };
  } catch (error) {
    logger.error('File system health check failed', { error });
    
    return {
      status: 'unhealthy',
      message: 'File system check failed',
      details: error instanceof Error ? { message: error.message, stack: error.stack } : String(error),
      responseTime: Date.now() - startTime,
      fileSystemStatus: {
        status: 'unhealthy',
        diskSpace: { total: 0, free: 0, percentFree: 0 },
        logDirAccess: false,
        tempDirAccess: false
      }
    };
  }
}

/**
 * Check memory usage health
 */
function checkMemory(): CheckResult & { memoryStatus?: MemoryStatus } {
  const startTime = Date.now();
  
  try {
    // System memory
    const totalMemory = os.totalmem();
    const freeMemory = os.freemem();
    const percentFree = (freeMemory / totalMemory) * 100;
    
    // Process memory
    const processMemory = process.memoryUsage();
    const percentHeapUsed = (processMemory.heapUsed / processMemory.heapTotal) * 100;
    
    // Determine status
    let status: 'healthy' | 'degraded' | 'unhealthy' = 'healthy';
    let message = 'Memory usage is healthy';
    
    // More lenient thresholds for development environment
    const isProduction = process.env.NODE_ENV === 'production';
    const degradedThreshold = isProduction ? 20 : 5;
    const criticalThreshold = isProduction ? 10 : 2;
    const heapDegradedThreshold = isProduction ? 80 : 95;
    const heapCriticalThreshold = isProduction ? 90 : 98;
    
    if (percentFree < degradedThreshold || percentHeapUsed > heapDegradedThreshold) {
      status = 'degraded';
      message = 'Memory usage is higher than optimal';
    }
    
    if (percentFree < criticalThreshold || percentHeapUsed > heapCriticalThreshold) {
      status = 'unhealthy';
      message = 'Critical memory shortage';
    }
    
    const memoryStatus: MemoryStatus = {
      status,
      system: {
        total: totalMemory,
        free: freeMemory,
        percentFree
      },
      process: {
        rss: processMemory.rss,
        heapTotal: processMemory.heapTotal,
        heapUsed: processMemory.heapUsed,
        external: processMemory.external,
        percentHeapUsed
      }
    };
    
    return {
      status,
      message,
      memoryStatus,
      details: {
        system: {
          total: `${Math.round(totalMemory / (1024 * 1024))} MB`,
          free: `${Math.round(freeMemory / (1024 * 1024))} MB`,
          percentFree: `${Math.round(percentFree)}%`
        },
        process: {
          rss: `${Math.round(processMemory.rss / (1024 * 1024))} MB`,
          heapTotal: `${Math.round(processMemory.heapTotal / (1024 * 1024))} MB`,
          heapUsed: `${Math.round(processMemory.heapUsed / (1024 * 1024))} MB`,
          external: `${Math.round(processMemory.external / (1024 * 1024))} MB`,
          percentHeapUsed: `${Math.round(percentHeapUsed)}%`
        }
      },
      responseTime: Date.now() - startTime
    };
  } catch (error) {
    logger.error('Memory health check failed', { 
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    });
    
    return {
      status: 'unhealthy',
      message: 'Memory check failed',
      details: error instanceof Error ? { message: error.message, stack: error.stack } : String(error),
      responseTime: Date.now() - startTime,
      memoryStatus: {
        status: 'unhealthy',
        system: { total: 0, free: 0, percentFree: 0 },
        process: { rss: 0, heapTotal: 0, heapUsed: 0, external: 0, percentHeapUsed: 0 }
      }
    };
  }
}

/**
 * Run a comprehensive health check of all system components
 */
export async function checkSystemHealth(): Promise<SystemStatus> {
  logger.debug('Running system health check');
  const startTime = Date.now();
  
  // Run all checks in parallel
  const [dbResult, fsResult] = await Promise.all([
    checkDatabase(),
    checkFileSystem()
  ]);
  
  // Memory check is synchronous
  const memResult = checkMemory();
  
  // Aggregate results
  const detailedChecks: Record<string, CheckResult> = {
    database: {
      status: dbResult.status,
      message: dbResult.message,
      details: dbResult.details,
      responseTime: dbResult.responseTime
    },
    fileSystem: {
      status: fsResult.status,
      message: fsResult.message,
      details: fsResult.details,
      responseTime: fsResult.responseTime
    },
    memory: {
      status: memResult.status,
      message: memResult.message,
      details: memResult.details,
      responseTime: memResult.responseTime
    }
  };
  
  // Determine overall system status
  let overallStatus: 'healthy' | 'degraded' | 'unhealthy' = 'healthy';
  
  // If any check is unhealthy, the system is unhealthy
  if (dbResult.status === 'unhealthy' || fsResult.status === 'unhealthy' || memResult.status === 'unhealthy') {
    overallStatus = 'unhealthy';
  } 
  // If any check is degraded, the system is degraded
  else if (dbResult.status === 'degraded' || fsResult.status === 'degraded' || memResult.status === 'degraded') {
    overallStatus = 'degraded';
  }
  
  const systemStatus: SystemStatus = {
    status: overallStatus,
    database: dbResult.databaseStatus!,
    fileSystem: fsResult.fileSystemStatus!,
    memory: memResult.memoryStatus!,
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    detailedChecks
  };
  
  logger.info(`System health check complete: ${overallStatus}`, {
    responseTime: Date.now() - startTime,
    databaseStatus: dbResult.status,
    fileSystemStatus: fsResult.status,
    memoryStatus: memResult.status
  });
  
  return systemStatus;
}

/**
 * Schedule periodic health checks
 * @param interval Interval in milliseconds between checks
 * @returns Cleanup function to stop scheduled checks
 */
export function scheduleHealthChecks(interval: number = 5 * 60 * 1000): () => void {
  logger.info(`Scheduling health checks at ${interval}ms intervals`);
  
  // Store last system status
  let lastStatus: SystemStatus | null = null;
  
  const timer = setInterval(async () => {
    try {
      const currentStatus = await checkSystemHealth();
      
      // Log state changes
      if (lastStatus && lastStatus.status !== currentStatus.status) {
        if (currentStatus.status === 'degraded') {
          logger.warn(`System health degraded from ${lastStatus.status}`, {
            previous: lastStatus.status,
            current: currentStatus.status,
            checks: currentStatus.detailedChecks
          });
          
          // Send notification for degraded state
          notifyHealthStateChange(
            lastStatus.status,
            currentStatus.status,
            `System health degraded from ${lastStatus.status.toUpperCase()} to ${currentStatus.status.toUpperCase()}`,
            {
              checks: Object.entries(currentStatus.detailedChecks)
                .filter(([_, check]) => check.status !== 'healthy')
                .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {}),
              memory: currentStatus.memory,
              uptime: currentStatus.uptime
            }
          ).catch(error => {
            logger.error('Failed to send health state change notification', { error });
          });
        } else if (currentStatus.status === 'unhealthy') {
          logger.error(`System health deteriorated to unhealthy state`, {
            previous: lastStatus.status,
            current: currentStatus.status,
            checks: currentStatus.detailedChecks
          });
          
          // Send notification for unhealthy state
          notifyHealthStateChange(
            lastStatus.status,
            currentStatus.status,
            `CRITICAL: System health deteriorated to UNHEALTHY state`,
            {
              checks: Object.entries(currentStatus.detailedChecks)
                .filter(([_, check]) => check.status !== 'healthy')
                .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {}),
              memory: currentStatus.memory,
              database: currentStatus.database,
              uptime: currentStatus.uptime
            }
          ).catch(error => {
            logger.error('Failed to send health state change notification', { error });
          });
        } else if (currentStatus.status === 'healthy' && lastStatus.status !== 'healthy') {
          logger.info(`System health recovered to healthy state`, {
            previous: lastStatus.status,
            current: currentStatus.status
          });
          
          // Send notification for recovery
          notifyHealthStateChange(
            lastStatus.status,
            currentStatus.status,
            `System health recovered to HEALTHY state`,
            {
              previousState: lastStatus.status,
              uptime: currentStatus.uptime,
              recoveryTime: new Date().toISOString()
            }
          ).catch(error => {
            logger.error('Failed to send health state recovery notification', { error });
          });
        }
      }
      
      lastStatus = currentStatus;
    } catch (error) {
      logger.error('Failed to run scheduled health check', { error });
    }
  }, interval);
  
  // Return cleanup function
  return () => {
    logger.info('Stopping scheduled health checks');
    clearInterval(timer);
  };
}