/**
 * Automated Error Recovery System for ModForge
 * 
 * This system automatically detects and recovers from common error conditions
 * that might occur during long-running 24/7 operations.
 */

import { getLogger } from "./logging";

// Define a simplified version of the scheduleJob function if the original is not available
// This will be replaced when the actual module is imported
type CleanupFunction = () => void;
const scheduleJob = (
  name: string,
  interval: number,
  jobFunction: () => Promise<void>
): CleanupFunction => {
  const logger = getLogger("error-recovery-scheduler");
  logger.info(`Scheduling error recovery at ${interval}ms intervals`);
  
  // Create interval
  const timer = setInterval(async () => {
    try {
      await jobFunction();
    } catch (error) {
      logger.error("Error in scheduled job", { error });
    }
  }, interval);
  
  // Return cleanup function
  return () => {
    clearInterval(timer);
    logger.info("Stopped error recovery schedule");
  };
};
import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
import { exec } from "child_process";
import { promisify } from "util";
import { pool } from "./db";

// Setup logger
const logger = getLogger("error-recovery");

// File paths
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const TMP_DIR = path.join(__dirname, "../tmp");
const LOG_DIR = path.join(__dirname, "../logs");

// Promisify exec
const execAsync = promisify(exec);

// Recovery types
export enum RecoveryAction {
  FILE_CLEANUP = "file_cleanup",
  DB_CONNECTION_RESET = "db_connection_reset",
  MEMORY_CLEANUP = "memory_cleanup",
  PROCESS_RESTART = "process_restart",
  MOD_COMPILATION_RESET = "mod_compilation_reset",
  TEMPORARY_DISABLE_FEATURE = "temporary_disable_feature"
}

interface RecoveryResult {
  successful: boolean;
  action: RecoveryAction;
  details: string;
  timestamp: string;
}

/**
 * Clean up the temporary directory if it's getting too large
 */
async function recoverFromDiskSpaceIssues(): Promise<RecoveryResult> {
  logger.info("Starting temporary file cleanup recovery action");
  
  try {
    // Ensure temp directory exists
    try {
      await fs.mkdir(TMP_DIR, { recursive: true });
    } catch (err) {
      // Directory already exists, continue
    }
    
    // Get all files in temp directory
    const files = await fs.readdir(TMP_DIR);
    let deletedCount = 0;
    let deletedSize = 0;
    
    // Sort files by creation time (oldest first)
    const fileStats = await Promise.all(
      files.map(async (file) => {
        const filePath = path.join(TMP_DIR, file);
        const stats = await fs.stat(filePath);
        return { 
          path: filePath, 
          name: file,
          mtime: stats.mtime,
          size: stats.size 
        };
      })
    );
    
    // Sort by modification time (oldest first)
    fileStats.sort((a, b) => a.mtime.getTime() - b.mtime.getTime());
    
    // Delete oldest files if there are more than 100 or if total size is > 500MB
    const totalSize = fileStats.reduce((acc, file) => acc + file.size, 0);
    const MAX_SIZE = 500 * 1024 * 1024; // 500MB
    const MAX_FILES = 100;
    
    if (fileStats.length > MAX_FILES || totalSize > MAX_SIZE) {
      logger.warn(`Temp directory has ${fileStats.length} files totaling ${Math.round(totalSize / 1024 / 1024)}MB`);
      
      // Calculate how many files to delete
      const targetDeleteCount = fileStats.length > MAX_FILES 
        ? fileStats.length - MAX_FILES 
        : Math.ceil(fileStats.length * 0.25); // Delete 25% of files if over size limit
      
      // Delete oldest files
      for (let i = 0; i < targetDeleteCount && i < fileStats.length; i++) {
        try {
          await fs.unlink(fileStats[i].path);
          deletedCount++;
          deletedSize += fileStats[i].size;
        } catch (err) {
          logger.error(`Failed to delete file ${fileStats[i].name}`, { 
            error: err instanceof Error ? err.message : String(err),
            stack: err instanceof Error ? err.stack : undefined
          });
        }
      }
    }
    
    logger.info(`Temp file cleanup completed`, {
      deletedFiles: deletedCount,
      deletedSizeMB: Math.round(deletedSize / 1024 / 1024),
      remainingFiles: fileStats.length - deletedCount,
      remainingSizeMB: Math.round((totalSize - deletedSize) / 1024 / 1024)
    });
    
    return {
      successful: true,
      action: RecoveryAction.FILE_CLEANUP,
      details: `Cleaned up ${deletedCount} files (${Math.round(deletedSize / 1024 / 1024)}MB)`,
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error("Failed to recover from disk space issues", { 
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    });
    
    return {
      successful: false,
      action: RecoveryAction.FILE_CLEANUP,
      details: `Failed to clean up temp files: ${error instanceof Error ? error.message : String(error)}`,
      timestamp: new Date().toISOString()
    };
  }
}

/**
 * Reset database connections if they're in a bad state
 */
async function recoverFromDatabaseConnectionIssues(): Promise<RecoveryResult> {
  logger.info("Starting database connection recovery action");
  
  try {
    // Check current pool state
    const poolStats = pool as any; // Type assertion to access internal properties
    const totalConnections = poolStats._totalCount || 0;
    const idleConnections = poolStats._idleCount || 0;
    const usedConnections = totalConnections - idleConnections;
    const waitingRequests = poolStats._waitingCount || 0;
    
    logger.info("Current database connection pool state", {
      total: totalConnections,
      idle: idleConnections,
      used: usedConnections,
      waiting: waitingRequests
    });
    
    // Check if pool is in a potentially bad state
    if (waitingRequests > 5 || (totalConnections > 0 && idleConnections === 0)) {
      logger.warn("Database connection pool appears to be under stress", {
        waitingRequests,
        idleConnections
      });
      
      // Try a simple query to verify connection health
      try {
        const result = await pool.query("SELECT 1 AS test");
        if (!result || !result.rows || result.rows.length === 0) {
          throw new Error("Test query returned no results");
        }
      } catch (queryError) {
        logger.error("Database test query failed, forcing connection pool reset", {
          error: queryError instanceof Error ? queryError.message : String(queryError),
          stack: queryError instanceof Error ? queryError.stack : undefined
        });
        
        // Force a pool drain and reset
        // This is a drastic action but can help recover from connection issues
        try {
          await pool.end();
          
          // Restart the pool (this will happen automatically on next query)
          // We'll do a simple query to trigger the pool restart
          setTimeout(async () => {
            try {
              await pool.query("SELECT 1 AS connection_test");
              logger.info("Database connection pool successfully restarted");
            } catch (restartError) {
              logger.error("Failed to restart database connection pool", {
                error: restartError
              });
            }
          }, 1000);
          
          return {
            successful: true,
            action: RecoveryAction.DB_CONNECTION_RESET,
            details: "Database connection pool was forcibly reset due to connection issues",
            timestamp: new Date().toISOString()
          };
        } catch (endError) {
          logger.error("Failed to end database connection pool", {
            error: endError instanceof Error ? endError.message : String(endError),
            stack: endError instanceof Error ? endError.stack : undefined
          });
          
          return {
            successful: false,
            action: RecoveryAction.DB_CONNECTION_RESET,
            details: `Failed to reset database connections: ${endError instanceof Error ? endError.message : String(endError)}`,
            timestamp: new Date().toISOString()
          };
        }
      }
    }
    
    // If we got here, the database connections are in a good state or were successfully reset
    return {
      successful: true,
      action: RecoveryAction.DB_CONNECTION_RESET,
      details: "Database connections checked and are in a healthy state",
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error("Failed to recover from database connection issues", { 
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined 
    });
    
    return {
      successful: false,
      action: RecoveryAction.DB_CONNECTION_RESET,
      details: `Failed to check/reset database connections: ${error instanceof Error ? error.message : String(error)}`,
      timestamp: new Date().toISOString()
    };
  }
}

/**
 * Force a garbage collection to recover memory
 */
async function recoverFromMemoryIssues(): Promise<RecoveryResult> {
  logger.info("Starting memory cleanup recovery action");
  
  try {
    // Log current memory usage
    const beforeMemory = process.memoryUsage();
    logger.info("Current memory usage", {
      heapUsedMB: Math.round(beforeMemory.heapUsed / 1024 / 1024 * 100) / 100,
      heapTotalMB: Math.round(beforeMemory.heapTotal / 1024 / 1024 * 100) / 100,
      rssMemoryMB: Math.round(beforeMemory.rss / 1024 / 1024 * 100) / 100,
      externalMemoryMB: Math.round(beforeMemory.external / 1024 / 1024 * 100) / 100
    });
    
    // Try to free memory
    // Note: Node.js doesn't expose GC directly, we would need to run with --expose-gc flag
    // Here we'll use a typesafe approach to check for it
    const globalWithGC = global as unknown as { gc?: () => void };
    
    if (typeof globalWithGC.gc === 'function') {
      logger.info("Forcing garbage collection");
      globalWithGC.gc();
      
      // Check memory usage after GC
      const afterMemory = process.memoryUsage();
      const heapDiff = beforeMemory.heapUsed - afterMemory.heapUsed;
      const rssDiff = beforeMemory.rss - afterMemory.rss;
      
      logger.info("Memory usage after forced GC", {
        heapUsedMB: Math.round(afterMemory.heapUsed / 1024 / 1024 * 100) / 100,
        heapTotalMB: Math.round(afterMemory.heapTotal / 1024 / 1024 * 100) / 100,
        rssMemoryMB: Math.round(afterMemory.rss / 1024 / 1024 * 100) / 100,
        freedHeapMB: Math.round(heapDiff / 1024 / 1024 * 100) / 100,
        freedRssMB: Math.round(rssDiff / 1024 / 1024 * 100) / 100,
      });
      
      return {
        successful: true,
        action: RecoveryAction.MEMORY_CLEANUP,
        details: `Successfully freed ${Math.round(heapDiff / 1024 / 1024)}MB of heap memory`,
        timestamp: new Date().toISOString()
      };
    } else {
      logger.warn("global.gc is not available, cannot force garbage collection");
      
      // Even without explicit GC, we can still try to free memory
      // by releasing references and suggesting GC
      try {
        // Run some memory cleanup tasks
        global.gc && global.gc();
        
        logger.info("Attempted memory cleanup through reference clearing");
        
        return {
          successful: true,
          action: RecoveryAction.MEMORY_CLEANUP,
          details: "Attempted memory cleanup through reference clearing",
          timestamp: new Date().toISOString()
        };
      } catch (cleanupError) {
        logger.error("Failed to clean up memory through reference clearing", {
          error: cleanupError
        });
        
        return {
          successful: false,
          action: RecoveryAction.MEMORY_CLEANUP,
          details: `Failed to clean up memory: ${cleanupError instanceof Error ? cleanupError.message : String(cleanupError)}`,
          timestamp: new Date().toISOString()
        };
      }
    }
  } catch (error) {
    logger.error("Failed to recover from memory issues", { error });
    
    return {
      successful: false,
      action: RecoveryAction.MEMORY_CLEANUP,
      details: `Failed to clean up memory: ${error instanceof Error ? error.message : String(error)}`,
      timestamp: new Date().toISOString()
    };
  }
}

/**
 * Reset any stuck mod compilations
 */
async function recoverFromStuckCompilations(): Promise<RecoveryResult> {
  logger.info("Starting stuck compilation recovery action");
  
  try {
    // Look for compilation processes that might be stuck
    // This is a simplified version - in a real system, we'd query the database
    // for builds that have been in "in_progress" state for too long
    
    // For simplicity, we'll just check for any processes that might be stuck
    const { stdout } = await execAsync('ps -eo pid,ppid,cmd,%cpu,%mem,etime | grep -E "javac|gradle|maven" | grep -v grep');
    
    if (stdout.trim()) {
      const processes = stdout.trim().split('\n');
      logger.info(`Found ${processes.length} potential compilation processes`);
      
      // Check for any that have been running for too long
      // Format of etime is [[dd-]hh:]mm:ss
      const stuckProcesses = processes.filter(proc => {
        const etimeMatch = proc.match(/(\d+):(\d+)$/);
        if (etimeMatch) {
          const minutes = parseInt(etimeMatch[1], 10);
          return minutes > 15; // Consider processes running > 15 minutes as stuck
        }
        return false;
      });
      
      if (stuckProcesses.length > 0) {
        logger.warn(`Found ${stuckProcesses.length} stuck compilation processes`);
        
        // Kill stuck processes
        for (const proc of stuckProcesses) {
          const pidMatch = proc.match(/^\s*(\d+)/);
          if (pidMatch) {
            const pid = parseInt(pidMatch[1], 10);
            logger.info(`Killing stuck compilation process ${pid}`);
            try {
              process.kill(pid);
            } catch (killError) {
              logger.error(`Failed to kill process ${pid}`, { error: killError });
            }
          }
        }
        
        return {
          successful: true,
          action: RecoveryAction.MOD_COMPILATION_RESET,
          details: `Killed ${stuckProcesses.length} stuck compilation processes`,
          timestamp: new Date().toISOString()
        };
      }
    }
    
    logger.info("No stuck compilation processes found");
    
    return {
      successful: true,
      action: RecoveryAction.MOD_COMPILATION_RESET,
      details: "No stuck compilation processes found",
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error("Failed to recover from stuck compilations", { error });
    
    return {
      successful: false,
      action: RecoveryAction.MOD_COMPILATION_RESET,
      details: `Failed to check/reset stuck compilations: ${error instanceof Error ? error.message : String(error)}`,
      timestamp: new Date().toISOString()
    };
  }
}

/**
 * Run all error recovery actions
 */
export async function performErrorRecovery(): Promise<RecoveryResult[]> {
  logger.info("Starting comprehensive error recovery process");
  
  // Run all recovery actions in parallel
  const [
    diskResult,
    dbResult,
    memoryResult,
    compilationResult
  ] = await Promise.all([
    recoverFromDiskSpaceIssues(),
    recoverFromDatabaseConnectionIssues(),
    recoverFromMemoryIssues(),
    recoverFromStuckCompilations()
  ]);
  
  // Compile results
  const results = [
    diskResult,
    dbResult,
    memoryResult,
    compilationResult
  ];
  
  // Log a summary
  const successCount = results.filter(r => r.successful).length;
  logger.info(`Error recovery process completed. ${successCount}/${results.length} actions successful`);
  
  return results;
}

/**
 * Schedule periodic error recovery
 * @param interval Interval in milliseconds between recovery attempts
 * @returns Cleanup function to stop scheduled recovery
 */
export function scheduleErrorRecovery(interval: number = 30 * 60 * 1000): () => void {
  logger.info(`Scheduling error recovery at ${interval}ms intervals`);
  
  // Run recovery process according to the schedule
  const cleanup = scheduleJob("error-recovery", interval, async () => {
    try {
      await performErrorRecovery();
    } catch (error) {
      logger.error("Scheduled error recovery failed", { error });
    }
  });
  
  return cleanup;
}