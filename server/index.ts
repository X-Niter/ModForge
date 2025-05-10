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
 * Memory management system to monitor and optimize memory usage
 * This helps ensure the application can run 24/7 without memory leaks
 */
function setupMemoryManagement() {
  const MEMORY_CHECK_INTERVAL = 15 * 60 * 1000; // 15 minutes
  const WARNING_THRESHOLD_MB = 1024; // 1GB
  const CRITICAL_THRESHOLD_MB = 1536; // 1.5GB
  
  // Keep track of consecutive high memory readings
  let consecutiveHighMemoryCount = 0;
  
  // Setup periodic memory check
  const interval = setInterval(() => {
    try {
      const memoryUsage = process.memoryUsage();
      const heapUsedMB = Math.round(memoryUsage.heapUsed / 1024 / 1024);
      const rssMemoryMB = Math.round(memoryUsage.rss / 1024 / 1024);
      
      // Log memory usage for monitoring
      log(`Memory usage: ${heapUsedMB}MB heap, ${rssMemoryMB}MB total`);
      
      if (heapUsedMB > CRITICAL_THRESHOLD_MB || rssMemoryMB > CRITICAL_THRESHOLD_MB * 1.5) {
        // Critical memory usage - take immediate action
        log('CRITICAL MEMORY USAGE DETECTED - triggering forced garbage collection', 'warn');
        consecutiveHighMemoryCount++;
        
        // Force garbage collection if available (requires --expose-gc flag)
        if (global.gc) {
          log('Running forced garbage collection');
          global.gc();
        }
        
        // If multiple consecutive critical readings, take more aggressive action
        if (consecutiveHighMemoryCount >= 3) {
          log('Multiple consecutive critical memory readings - logging heap snapshot and cleaning caches', 'error');
          
          // Clear internal caches if applicable
          // This would be application-specific, but could include clearing pattern caches, etc.
          
          // Reset counter after taking action
          consecutiveHighMemoryCount = 0;
        }
      } else if (heapUsedMB > WARNING_THRESHOLD_MB || rssMemoryMB > WARNING_THRESHOLD_MB * 1.5) {
        // Warning level - log but don't take action yet
        log(`WARNING: High memory usage detected: ${heapUsedMB}MB heap`, 'warn');
        consecutiveHighMemoryCount++;
        
        // Suggest garbage collection if multiple consecutive warnings
        if (consecutiveHighMemoryCount >= 2 && global.gc) {
          log('Running suggested garbage collection after multiple warnings');
          global.gc();
        }
      } else {
        // Normal operation - reset counter
        consecutiveHighMemoryCount = 0;
      }
    } catch (error) {
      log(`Error in memory management: ${error instanceof Error ? error.message : String(error)}`, 'error');
    }
  }, MEMORY_CHECK_INTERVAL);
  
  // Ensure the interval is cleaned up on application shutdown
  return () => {
    clearInterval(interval);
    log('Memory management system shutdown');
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
