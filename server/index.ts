import express, { type Request, Response, NextFunction } from "express";
import { registerRoutes } from "./routes";
import { setupVite, serveStatic, log } from "./vite";
import helmet from "helmet";
import session from "express-session";
import { pool } from "./db";
import connectPgSimple from "connect-pg-simple";
import authRoutes from "./routes/auth-routes";

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

(async () => {
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
    log(`${signal} received - starting graceful shutdown`);
    
    // Import the continuousService to clean it up
    const { continuousService } = await import('./continuous-service');
    
    // Clean up resources
    continuousService.cleanup();
    
    // Close database pool
    await pool.end();
    
    // Close the server
    server.close(() => {
      log('HTTP server closed');
      process.exit(0);
    });
    
    // If server hasn't closed in 10 seconds, force exit
    setTimeout(() => {
      console.error('Server shutdown timed out, forcing exit');
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
  });
})();
