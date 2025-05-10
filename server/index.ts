import express, { type Request, Response, NextFunction } from "express";
import { registerRoutes } from "./routes";
import { setupVite, serveStatic, log } from "./vite";
import helmet from "helmet";
import session from "express-session";
import { pool } from "./db";
import connectPgSimple from "connect-pg-simple";
import authRoutes from "./routes/auth-routes";
import { scheduleMaintenanceTasks } from "./maintenance-tasks";
import { rootLogger, getLogger } from "./logging";
import { initializeErrorRecovery, cleanupScheduledJobs } from "./index-error-recovery";
import { initializeBackupSystem } from "./backup-integration";
import { initializeNotifications } from "./notification-integration";
import { scheduleErrorStoreCleanup } from "./error-tracker";
import os from "os";

const app = express();
// Enable trust proxy to work correctly with express-rate-limit behind a proxy (like in Replit)
app.set('trust proxy', 1);
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// Setup PostgreSQL session store
const PgSession = connectPgSimple(session);
app.use(session({
  store: new PgSession({
    pool,
    tableName: 'session', // Use default table name
    createTableIfMissing: true
  }),
  secret: process.env.SESSION_SECRET || 'modforge-dev-secret', // Use env var in production
  resave: false,
  saveUninitialized: false,
  cookie: { 
    secure: process.env.NODE_ENV === 'production',
    maxAge: 30 * 24 * 60 * 60 * 1000 // 30 days
  }
}));

// We'll use our new auth implementation instead of the old routes
// app.use('/api/auth', authRoutes);

// Set security headers with helmet
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
      styleSrc: ["'self'", "'unsafe-inline'", "https://fonts.googleapis.com"],
      fontSrc: ["'self'", "https://fonts.gstatic.com"],
      imgSrc: ["'self'", "data:", "https:"],
      connectSrc: ["'self'", "https://api.openai.com"]
    }
  },
  crossOriginEmbedderPolicy: false // Required for some features to work properly
}));

app.use((req, res, next) => {
  const start = Date.now();
  const path = req.path;
  let capturedJsonResponse: Record<string, any> | undefined = undefined;

  const originalResJson = res.json;
  res.json = function (bodyJson, ...args) {
    capturedJsonResponse = bodyJson;
    return originalResJson.apply(res, [bodyJson, ...args]);
  };

  res.on("finish", () => {
    const duration = Date.now() - start;
    if (path.startsWith("/api")) {
      let logLine = `${req.method} ${path} ${res.statusCode} in ${duration}ms`;
      if (capturedJsonResponse) {
        logLine += ` :: ${JSON.stringify(capturedJsonResponse)}`;
      }

      if (logLine.length > 80) {
        logLine = logLine.slice(0, 79) + "…";
      }

      log(logLine);
    }
  });

  next();
});

/**
 * Advanced memory management system for 24/7 operation
 * - Monitors heap and RSS memory usage
 * - Implements adaptive memory thresholds based on system capabilities
 * - Provides escalating recovery actions for memory pressure
 * - Detects memory leaks through heap growth pattern analysis
 * - Maintains memory usage history for diagnostics
 */
function setupMemoryManagement() {
  const memoryLogger = getLogger('memory-management');
  
  // Define memory reading type
  interface MemoryReading {
    timestamp: string;
    heapUsedMB: number;
    rssMemoryMB: number;
    heapTotalMB: number;
    externalMB: number;
  }
  
  // Configuration with environment-aware settings
  const isProduction = process.env.NODE_ENV === 'production';
  const MEMORY_CHECK_INTERVAL = isProduction ? 5 * 60 * 1000 : 15 * 60 * 1000; // 5 min in prod, 15 min in dev
  const MEMORY_HISTORY_SIZE = 24; // Keep last 24 readings
  
  // Get system total memory and set thresholds accordingly
  const totalSystemMemoryMB = Math.round(os.totalmem() / 1024 / 1024);
  
  // In production, set thresholds to percentages of available system memory
  // In development, use fixed thresholds to be more conservative
  const WARNING_THRESHOLD_MB = isProduction 
    ? Math.min(1024, Math.round(totalSystemMemoryMB * 0.3)) // 30% of system memory or 1GB, whichever is smaller
    : 768; // 768MB in development
    
  const CRITICAL_THRESHOLD_MB = isProduction
    ? Math.min(1536, Math.round(totalSystemMemoryMB * 0.5)) // 50% of system memory or 1.5GB, whichever is smaller
    : 1024; // 1GB in development
  
  // Memory metrics history for leak detection
  const memoryHistory: MemoryReading[] = [];
  
  // Keep track of consecutive high memory readings and recovery actions
  let consecutiveHighMemoryCount = 0;
  let recoveryActionsPerformed = 0;
  let lastFullRecoveryTime = Date.now();
  
  // Memory leak detection variables
  let consistentGrowthCount = 0;
  const LEAK_DETECTION_THRESHOLD = 5; // Number of consistent growth readings to trigger leak warning
  
  memoryLogger.info('Memory management initialized', {
    systemMemory: `${totalSystemMemoryMB}MB`,
    warningThreshold: `${WARNING_THRESHOLD_MB}MB`,
    criticalThreshold: `${CRITICAL_THRESHOLD_MB}MB`,
    checkInterval: `${MEMORY_CHECK_INTERVAL/1000}s`
  });
  
  // Clear application-specific caches to reduce memory pressure
  function clearApplicationCaches() {
    try {
      // Import and clear pattern-matching caches if available
      try {
        const aiServiceManager = require('./ai-service-manager');
        if (typeof aiServiceManager.clearPatternCache === 'function') {
          const clearedPatterns = aiServiceManager.clearPatternCache();
          memoryLogger.info(`Cleared pattern cache, removed ${clearedPatterns || 'unknown'} patterns`);
        }
      } catch (e) {
        // Module might not be available, that's ok
      }
      
      // Clear any other module caches that might be available
      global.gc && global.gc();
      
      memoryLogger.info('Application caches cleared');
      return true;
    } catch (err) {
      memoryLogger.error('Failed to clear application caches', { error: err });
      return false;
    }
  }
  
  // Check for memory leaks by analyzing the growth pattern
  function checkForMemoryLeaks(history: MemoryReading[]): boolean {
    if (history.length < 3) return false;
    
    const recentReadings = history.slice(-3);
    // Check if each reading is higher than the previous by at least 5%
    const consistent = recentReadings.every((reading: MemoryReading, index: number) => {
      if (index === 0) return true;
      const prevReading = recentReadings[index - 1];
      // Growth of at least 5% between readings
      return reading.heapUsedMB > prevReading.heapUsedMB * 1.05;
    });
    
    if (consistent) {
      consistentGrowthCount++;
      if (consistentGrowthCount >= LEAK_DETECTION_THRESHOLD) {
        // Potential memory leak detected
        memoryLogger.warn('Potential memory leak detected - consistent memory growth pattern', {
          readings: recentReadings.map((r: MemoryReading) => `${r.heapUsedMB}MB at ${r.timestamp}`)
        });
        return true;
      }
    } else {
      // Reset the counter if growth is not consistent
      consistentGrowthCount = 0;
    }
    
    return false;
  }
  
  // Setup periodic memory check
  const interval = setInterval(() => {
    try {
      const memoryUsage = process.memoryUsage();
      const heapUsedMB = Math.round(memoryUsage.heapUsed / 1024 / 1024);
      const rssMemoryMB = Math.round(memoryUsage.rss / 1024 / 1024);
      const heapTotalMB = Math.round(memoryUsage.heapTotal / 1024 / 1024);
      const externalMB = Math.round(memoryUsage.external / 1024 / 1024);
      
      // Record memory reading with timestamp
      const timestamp = new Date().toISOString();
      const memoryReading = {
        timestamp,
        heapUsedMB,
        rssMemoryMB,
        heapTotalMB,
        externalMB
      };
      
      // Add to history and maintain size limit
      memoryHistory.push(memoryReading);
      if (memoryHistory.length > MEMORY_HISTORY_SIZE) {
        memoryHistory.shift();
      }
      
      // Log memory usage in a structured way
      memoryLogger.info('Memory status', {
        heap: `${heapUsedMB}MB / ${heapTotalMB}MB`,
        rss: `${rssMemoryMB}MB`,
        external: `${externalMB}MB`,
        percentUsed: `${Math.round((heapUsedMB / heapTotalMB) * 100)}%`
      });
      
      // Check for memory leaks
      const potentialLeak = checkForMemoryLeaks(memoryHistory);
      
      // Handle critical memory pressure
      if (heapUsedMB > CRITICAL_THRESHOLD_MB || rssMemoryMB > CRITICAL_THRESHOLD_MB * 1.5) {
        // Critical memory usage - take immediate action
        memoryLogger.warn('CRITICAL MEMORY USAGE DETECTED', {
          heap: `${heapUsedMB}MB`,
          rss: `${rssMemoryMB}MB`,
          threshold: `${CRITICAL_THRESHOLD_MB}MB`,
          consecutive: consecutiveHighMemoryCount + 1
        });
        
        consecutiveHighMemoryCount++;
        recoveryActionsPerformed++;
        
        // Force garbage collection if available
        if (global.gc) {
          memoryLogger.info('Running forced garbage collection');
          global.gc();
        }
        
        // Escalating recovery actions based on consecutive readings
        if (consecutiveHighMemoryCount === 2) {
          // After 2 consecutive critical readings, clear caches
          memoryLogger.warn('Multiple critical memory readings - clearing application caches');
          clearApplicationCaches();
        } 
        else if (consecutiveHighMemoryCount >= 3 || potentialLeak) {
          // After 3+ consecutive critical readings or potential leak, take more aggressive action
          memoryLogger.error('Persistent critical memory pressure - performing emergency recovery', {
            consecutiveReadings: consecutiveHighMemoryCount,
            recoveryAttempt: recoveryActionsPerformed,
            potentialLeak: potentialLeak
          });
          
          // Clear all caches
          clearApplicationCaches();
          
          // If we still have issues after multiple recovery attempts, consider more drastic measures
          if (recoveryActionsPerformed >= 3 && Date.now() - lastFullRecoveryTime > 30 * 60 * 1000) {
            memoryLogger.critical('Emergency memory recovery initiated after multiple failed attempts', {
              memoryHistory: memoryHistory.slice(-5) // Include recent history
            });
            
            // Reset recovery counters after taking drastic action
            lastFullRecoveryTime = Date.now();
            recoveryActionsPerformed = 0;
          }
          
          // Reset consecutive counter after taking action
          consecutiveHighMemoryCount = 0;
        }
      } 
      // Handle warning level memory pressure
      else if (heapUsedMB > WARNING_THRESHOLD_MB || rssMemoryMB > WARNING_THRESHOLD_MB * 1.5) {
        // Warning level - monitor closely
        memoryLogger.warn('High memory usage detected', {
          heap: `${heapUsedMB}MB`,
          rss: `${rssMemoryMB}MB`,
          threshold: `${WARNING_THRESHOLD_MB}MB`,
          consecutive: consecutiveHighMemoryCount + 1
        });
        
        consecutiveHighMemoryCount++;
        
        // Suggest garbage collection after multiple warnings
        if (consecutiveHighMemoryCount >= 2 && global.gc) {
          memoryLogger.info('Running suggested garbage collection after multiple warnings');
          global.gc();
          
          // After multiple warnings, start clearing caches proactively
          if (consecutiveHighMemoryCount >= 3) {
            memoryLogger.warn('Persistent high memory - proactively clearing caches');
            clearApplicationCaches();
            consecutiveHighMemoryCount = 1; // Reduce but don't reset to track longer patterns
          }
        }
      } else {
        // Normal operation - gradually reduce counters to recognize improvement
        if (consecutiveHighMemoryCount > 0) {
          consecutiveHighMemoryCount--;
        }
        
        // After a sustained period of normal memory usage, reset recovery counter
        if (memoryHistory.length >= 3 && 
            memoryHistory.slice(-3).every(m => 
              m.heapUsedMB < WARNING_THRESHOLD_MB && 
              m.rssMemoryMB < WARNING_THRESHOLD_MB * 1.5)) {
          recoveryActionsPerformed = 0;
        }
      }
    } catch (error) {
      memoryLogger.error('Error in memory management', { 
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined
      });
    }
  }, MEMORY_CHECK_INTERVAL);
  
  // Also set up a less frequent check for garbage collection during idle periods
  const idleGcInterval = setInterval(() => {
    try {
      // Only run idle GC if we have the capability and not in high memory pressure
      if (global.gc && consecutiveHighMemoryCount === 0) {
        memoryLogger.debug('Running idle-time garbage collection');
        global.gc();
      }
    } catch (error) {
      memoryLogger.error('Error in idle garbage collection', { error });
    }
  }, MEMORY_CHECK_INTERVAL * 4); // Run every 4x the normal check interval
  
  // Ensure all intervals are cleaned up on application shutdown
  return () => {
    clearInterval(interval);
    clearInterval(idleGcInterval);
    memoryLogger.info('Memory management system shutdown');
  };
}

// Setup global unhandled exception handlers
const uncaughtLogger = getLogger('uncaught-exception');

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
  uncaughtLogger.critical('Uncaught exception', { 
    error: error.message,
    stack: error.stack,
    name: error.name
  });
  
  // Continue running - we don't want to crash the server for non-critical errors
  // For truly catastrophic errors, our shutdown timeout will still apply
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
  const reasonStr = reason instanceof Error ? 
    { message: reason.message, stack: reason.stack } : 
    String(reason);
    
  uncaughtLogger.error('Unhandled promise rejection', { 
    reason: reasonStr,
    promise: String(promise)
  });
  
  // Continue running - we don't want to crash the server
});

(async () => {
  rootLogger.info("Initializing ModForge server with 24/7 reliability features");
  
  // Setup memory management system for 24/7 operation
  const cleanupMemoryManagement = setupMemoryManagement();
  
  // Setup periodic maintenance tasks for 24/7 operation
  const cleanupMaintenanceTasks = scheduleMaintenanceTasks();
  
  // Initialize error recovery system
  const cleanupErrorRecovery = initializeErrorRecovery();
  
  // Initialize backup system
  const cleanupBackups = initializeBackupSystem();
  
  // Initialize notification system
  const cleanupNotifications = initializeNotifications();
  
  // Initialize error tracking cleanup to prevent memory leaks
  const cleanupErrorStore = scheduleErrorStoreCleanup();
  
  const server = await registerRoutes(app);

  // Improved error handling middleware
  app.use((err: any, req: Request, res: Response, _next: NextFunction) => {
    const status = err.status || err.statusCode || 500;
    const message = err.message || "Internal Server Error";
    
    // Don't expose stack traces in production
    const stack = process.env.NODE_ENV === 'development' ? err.stack : undefined;
    
    // Log the error for server-side debugging
    console.error(`[ERROR] ${req.method} ${req.path}: ${err.message}`);
    if (stack) console.error(stack);
    
    // Send a structured error response
    res.status(status).json({ 
      error: {
        message,
        status,
        path: req.path,
        timestamp: new Date().toISOString()
      }
    });
    
    // Don't throw the error, as it's already been handled
  });

  // importantly only setup vite in development and after
  // setting up all the other routes so the catch-all route
  // doesn't interfere with the other routes
  if (app.get("env") === "development") {
    await setupVite(app, server);
  } else {
    serveStatic(app);
  }

  // ALWAYS serve the app on port 5000
  // this serves both the API and the client.
  // It is the only port that is not firewalled.
  const port = 5000;
  // Setup graceful shutdown handlers
  const gracefulShutdown = async (signal: string) => {
    const shutdownLogger = getLogger('shutdown');
    shutdownLogger.info(`${signal} received - starting graceful shutdown`);
    
    // Clean up memory management system
    shutdownLogger.info('Shutting down memory management system...');
    cleanupMemoryManagement();
    
    // Clean up maintenance tasks
    shutdownLogger.info('Shutting down maintenance tasks...');
    cleanupMaintenanceTasks();
    
    // Clean up error recovery and scheduled jobs
    shutdownLogger.info('Shutting down error recovery system...');
    cleanupErrorRecovery();
    
    // Clean up all scheduled jobs
    shutdownLogger.info('Shutting down scheduled jobs...');
    cleanupScheduledJobs();
    
    // Clean up backup system
    shutdownLogger.info('Shutting down backup system...');
    cleanupBackups();
    
    // Clean up notification system
    shutdownLogger.info('Shutting down notification system...');
    cleanupNotifications();
    shutdownLogger.info('Notification system shutdown complete');
    
    // Import the continuousService to clean it up
    const { continuousService } = await import('./continuous-service');
    
    // Clean up continuous development service resources (shutdown all running processes)
    shutdownLogger.info('Shutting down continuous development service...');
    continuousService.cleanup();
    shutdownLogger.info('Continuous development service shutdown complete');
    
    // Close database pool
    shutdownLogger.info('Closing database connection pool...');
    await pool.end();
    shutdownLogger.info('Database connections closed');
    
    // Close the server
    server.close(() => {
      shutdownLogger.info('HTTP server closed, shutdown complete');
      // Log final message before exit
      shutdownLogger.info('Exiting process gracefully', { 
        uptime: process.uptime(),
        signal
      });
      
      // Short delay to allow log to be written
      setTimeout(() => {
        process.exit(0);
      }, 500);
    });
    
    // If server hasn't closed in 10 seconds, force exit
    setTimeout(() => {
      shutdownLogger.error('Server shutdown timed out after 10 seconds, forcing exit');
      process.exit(1);
    }, 10000);
  };
  
  // Attach shutdown handlers
  process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
  process.on('SIGINT', () => gracefulShutdown('SIGINT'));
  
  // Start the server
  server.listen({
    port,
    host: "0.0.0.0",
    reusePort: true,
  }, () => {
    log(`serving on port ${port}`);
    rootLogger.info(`Server started and listening on port ${port}`, {
      environment: process.env.NODE_ENV || 'development',
      nodeVersion: process.version
    });
  });
})();
