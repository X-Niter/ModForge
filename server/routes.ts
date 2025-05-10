import type { Express, Request, Response, NextFunction } from "express";
import { createServer, type Server } from "http";
import { storage } from "./storage";
import { compileMod } from "./compiler";
import { 
  generateModCode, 
  fixCompilationErrors, 
  addModFeatures,
  generateCode,
  fixCode,
  enhanceCode,
  summarizeCode,
  explainError,
  generateDocumentation,
  type CodeGenerationResponse,
  type CompletionResponse
} from "./ai-service";
import { getUsageMetrics } from "./ai-service-manager";
import { pushModToGitHub } from "./github";
import { checkDatabaseHealth } from "./db-health";
import { checkSystemHealth, scheduleHealthChecks } from "./health-check";
import { continuousService } from "./continuous-service";
import { generateModIdeas, expandModIdea, ideaGenerationRequestSchema } from "./idea-generator-service";
import { setupAuth } from "./auth"; // This function returns a requireAuth middleware
import { errorHandlerMiddleware } from "./error-handler";
import { errorTrackerMiddleware } from "./error-tracker";
import { z } from "zod";
import { insertModSchema } from "@shared/schema";
import { BuildStatus } from "@shared/schemas/core/builds";
import apiMetricsRouter from "./routes/api-metrics";
import webExplorerRouter from "./routes/web-explorer-routes";
import jarAnalyzerRouter from "./routes/jar-analyzer-routes";
import patternLearningRouter from "./routes/pattern-learning-metrics";
import githubRoutes from "./routes/github-routes";
import errorMonitoringRouter from "./routes/error-monitoring";
import healthCheckRoutes from "./routes/health-check-routes";
import errorTrackingRoutes from "./routes/error-tracking-routes";
import systemDashboardRoutes from "./routes/system-dashboard-routes";
import backupRoutes from "./routes/backup-routes";
import notificationRoutes from "./routes/notification-routes";
import axios from "axios";
import rateLimit from "express-rate-limit";
import path from "path";

// Function declarations
async function generateModCodeAsync(mod: any, build: any): Promise<void> {
  try {
    // Generate the initial mod code
    const codeResult = await generateModCode(
      mod.name,
      mod.description,
      mod.modLoader,
      mod.minecraftVersion,
      mod.idea
    );
    
    // Update the build with the generated code
    await storage.updateBuild(build.id, {
      logs: build.logs + "\nGenerating initial mod files...\n"
    });
    
    // Create mod files from the generated code
    for (const file of codeResult.files) {
      await storage.createModFile({
        modId: mod.id,
        path: file.path,
        content: file.content,
        contentType: "text/plain", // Default content type
        metadata: {} // Default empty metadata
      });
    }
    
    // Compile the mod
    await compileModAsync(mod, build);
  } catch (error) {
    console.error("Error in generateModCodeAsync:", error);
    // Update build with error
    await storage.updateBuild(build.id, {
      status: "failed", // Using string literal instead of enum
      logs: build.logs + `\nError during mod generation: ${error instanceof Error ? error.message : String(error)}\n`,
      errorCount: 1,
      warningCount: 0
    });
  }
}

async function compileModAsync(mod: any, build: any): Promise<void> {
  let currentLogs = build.logs || '';
  
  try {
    // Compile the mod
    const compileResult = await compileMod(mod.id);
    
    // Update build with compilation results
    const status = compileResult.success ? "succeeded" : "failed"; // Using string literals
    currentLogs += compileResult.logs;
    
    await storage.updateBuild(build.id, {
      status,
      logs: currentLogs,
      errorCount: compileResult.errors.length,
      warningCount: compileResult.warnings.length,
      downloadUrl: compileResult.downloadUrl
    });
    
    // If compilation failed and auto-fix is enabled, try to fix errors
    if (!compileResult.success && compileResult.errors.length > 0 && 
        (mod.autoFixLevel === "Balanced" || mod.autoFixLevel === "Aggressive")) {
      
      // Add to logs safely
      currentLogs += "\nAttempting to fix compilation errors...\n";
      const preFixUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preFixUpdate) {
        currentLogs = preFixUpdate.logs;
      }
      
      // Get all mod files
      const modFiles = await storage.getModFilesByModId(mod.id);
      const files = modFiles.map(file => ({
        path: file.path,
        content: file.content
      }));
      
      // Fix errors
      const fixResult = await fixCompilationErrors(files, compileResult.errors);
      
      // Update logs
      currentLogs += fixResult.logs;
      const postFixUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (postFixUpdate) {
        currentLogs = postFixUpdate.logs;
      }
      
      // Update mod files with fixed versions
      for (const file of fixResult.files) {
        const existingFile = modFiles.find(f => f.path === file.path);
        
        if (existingFile) {
          await storage.updateModFile(existingFile.id, {
            content: file.content
          });
        } else {
          await storage.createModFile({
            modId: mod.id,
            path: file.path,
            content: file.content,
            contentType: "text/plain", // Default content type
            metadata: {} // Default empty metadata
          });
        }
      }
      
      // Try to compile again
      currentLogs += "\nAttempting to compile with fixed code...\n";
      const recompileUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (recompileUpdate) {
        currentLogs = recompileUpdate.logs;
      }
      
      const recompileResult = await compileMod(mod.id);
      
      // Update build with recompilation results
      const finalStatus = recompileResult.success ? "succeeded" : "failed"; // Using string literals
      currentLogs += recompileResult.logs;
      
      await storage.updateBuild(build.id, {
        status: finalStatus,
        logs: currentLogs,
        errorCount: recompileResult.errors.length,
        warningCount: recompileResult.warnings.length,
        downloadUrl: recompileResult.downloadUrl
      });
    }
  } catch (error) {
    console.error("Error in compileModAsync:", error);
    await storage.updateBuild(build.id, {
      status: "failed", // Using string literal
      logs: currentLogs + `\nError during compilation: ${error instanceof Error ? error.message : String(error)}\n`,
      errorCount: 1
    });
  }
}

export async function registerRoutes(app: Express): Promise<Server> {
  // Setup authentication system
  const requireAuth = setupAuth(app);
  
  // Protected routes that require authentication
  app.use([
    '/api/mods',
    '/api/github', 
    '/api/settings',
    '/api/metrics/private'
  ], requireAuth);
  
  // Create HTTP server
  const httpServer = createServer(app);
  
  // Configure rate limiters
  const standardLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    limit: 100, // Limit each IP to 100 requests per windowMs
    standardHeaders: 'draft-7',
    legacyHeaders: false,
    message: {
      status: 429,
      message: 'Too many requests, please try again later.'
    }
  });
  
  // More strict rate limiting for AI endpoints
  const aiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    limit: 30, // Limit each IP to 30 requests per windowMs
    standardHeaders: 'draft-7',
    legacyHeaders: false,
    message: {
      status: 429,
      message: 'Too many AI requests, please try again later.'
    }
  });
  
  // Request validation middleware
  const validateRequestBody = (schema: z.ZodType<any>) => {
    return (req: Request, res: Response, next: NextFunction) => {
      try {
        const validationResult = schema.safeParse(req.body);
        if (!validationResult.success) {
          return res.status(400).json({
            message: "Invalid request data",
            errors: validationResult.error.errors
          });
        }
        
        // Replace the body with the validated and sanitized data
        req.body = validationResult.data;
        next();
      } catch (error) {
        return res.status(400).json({
          message: "Request validation failed",
          error: error instanceof Error ? error.message : String(error)
        });
      }
    };
  };

  // Health check endpoint - always needs to be accessible
  app.get("/api/health", async (req, res) => {
    try {
      const dbHealth = await checkDatabaseHealth();
      
      // Check for OpenAI API key
      const aiStatus = {
        status: process.env.OPENAI_API_KEY ? 'available' : 'unavailable',
        message: process.env.OPENAI_API_KEY 
          ? 'OpenAI API key is configured' 
          : 'OpenAI API key is missing'
      };
      
      // Get server information
      const serverInfo = {
        version: process.env.npm_package_version || '1.0.0',
        env: process.env.NODE_ENV || 'development',
        uptime: process.uptime(),
        memory: process.memoryUsage(),
        timestamp: new Date().toISOString()
      };
      
      if (dbHealth.status === "healthy") {
        return res.status(200).json({
          status: "healthy",
          message: "Service is healthy",
          database: dbHealth,
          ai: aiStatus,
          server: serverInfo
        });
      } else {
        // Service is degraded or unhealthy
        const statusCode = dbHealth.status === "error" ? 503 : 500;
        return res.status(statusCode).json({
          status: "unhealthy",
          message: "Service is experiencing issues",
          database: dbHealth,
          ai: aiStatus,
          server: serverInfo
        });
      }
    } catch (error) {
      console.error("Health check failed:", error);
      return res.status(500).json({
        status: "error",
        message: "Health check failed",
        error: error instanceof Error ? error.message : String(error),
        timestamp: new Date().toISOString()
      });
    }
  });

  // Detailed health metrics for monitoring systems and admins  
  app.get("/api/health/detailed", requireAuth, async (req, res) => {
    try {
      // Get database health
      const dbHealth = await checkDatabaseHealth();
      
      // Get continuous development service status
      const continuousHealth = continuousService.getHealthStatus();
      
      // Get usage metrics
      const usageMetrics = getUsageMetrics();
      
      // Get circuit breaker statuses for all mods with circuit breakers
      const circuitBreakerKeys = Object.keys(process.env)
        .filter(key => key.startsWith('circuit_breaker_') && !key.includes('_time'));
      
      const circuitBreakerStats = circuitBreakerKeys.map(key => {
        const modId = parseInt(key.replace('circuit_breaker_', ''), 10);
        const count = parseInt(process.env[key] || '0', 10);
        const tripped = count >= 5;
        const tripTimeKey = `${key}_time`;
        const tripTime = process.env[tripTimeKey] ? new Date(parseInt(process.env[tripTimeKey] || '0', 10)) : null;
        
        return {
          modId,
          errorCount: count,
          tripped,
          tripTime: tripTime ? tripTime.toISOString() : null
        };
      });
      
      // Get overall system metrics
      const healthStatus = continuousService.getHealthStatus();
      const totalRunningMods = healthStatus.runningMods.length;
      const totalTrippedCircuitBreakers = circuitBreakerStats.filter(s => s.tripped).length;
      
      // Format uptime for human readability
      const formatUptime = (seconds: number): string => {
        const days = Math.floor(seconds / (3600 * 24));
        const hours = Math.floor((seconds % (3600 * 24)) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const remainingSeconds = Math.floor(seconds % 60);
        
        return `${days}d ${hours}h ${minutes}m ${remainingSeconds}s`;
      };
      
      // Check for system problems that might need self-healing
      const systemIssues: Array<{component: string, issue: string, severity: string}> = [];
      const selfHealingActions: Array<{component: string, action: string, timestamp: string}> = [];
      
      // Check for database issues
      if (dbHealth.status !== 'healthy') {
        systemIssues.push({
          component: 'database',
          issue: dbHealth.message,
          severity: 'high'
        });
      }
      
      // Check for excessive circuit breakers
      if (totalTrippedCircuitBreakers > 3) {
        systemIssues.push({
          component: 'continuous-development',
          issue: `${totalTrippedCircuitBreakers} circuit breakers tripped`,
          severity: 'medium'
        });
        
        // Attempt to self-heal by resetting circuit breakers that have been tripped too long
        const now = Date.now();
        const TWO_HOURS = 2 * 60 * 60 * 1000;
        
        // Find long-tripped circuit breakers
        const expiredCircuitBreakers = circuitBreakerStats.filter(cb => {
          if (!cb.tripped || !cb.tripTime) return false;
          const tripTime = new Date(cb.tripTime).getTime();
          return now - tripTime > TWO_HOURS;
        });
        
        // Reset expired circuit breakers
        if (expiredCircuitBreakers.length > 0) {
          expiredCircuitBreakers.forEach(cb => {
            const circuitBreakerKey = `circuit_breaker_${cb.modId}`;
            process.env[circuitBreakerKey] = '0';
            delete process.env[`${circuitBreakerKey}_time`];
            
            selfHealingActions.push({
              component: 'circuit-breaker',
              action: `Reset circuit breaker for mod ${cb.modId}`,
              timestamp: new Date().toISOString()
            });
          });
        }
      }
      
      // Return comprehensive metrics with self-healing information
      res.json({
        success: true,
        status: dbHealth.status === 'healthy' ? 'healthy' : 'unhealthy',
        database: dbHealth,
        continuousDevelopment: continuousHealth,
        usageMetrics,
        systemHealth: {
          issues: systemIssues,
          selfHealing: {
            enabled: true,
            actionsPerformed: selfHealingActions,
            lastChecked: new Date().toISOString()
          }
        },
        circuitBreakers: {
          total: circuitBreakerStats.length,
          tripped: totalTrippedCircuitBreakers,
          details: circuitBreakerStats
        },
        memory: {
          usage: process.memoryUsage(),
          heapUsedMB: Math.round(process.memoryUsage().heapUsed / 1024 / 1024 * 100) / 100,
          heapTotalMB: Math.round(process.memoryUsage().heapTotal / 1024 / 1024 * 100) / 100,
          rssMemoryMB: Math.round(process.memoryUsage().rss / 1024 / 1024 * 100) / 100
        },
        uptime: {
          seconds: process.uptime(),
          formatted: formatUptime(process.uptime())
        },
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      console.error("Detailed health check error:", error);
      res.status(500).json({
        status: "error",
        message: "Error generating detailed health report",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Apply standard rate limiter to other API endpoints
  app.use("/api", standardLimiter);
  
  // Apply stricter rate limit to AI-specific endpoints
  app.use("/api/ai", aiLimiter);
  
  // Get all mods for a user
  app.get("/api/mods", async (req, res) => {
    try {
      // For this demo, we'll use user ID 1
      const userId = 1;
      const mods = await storage.getModsByUserId(userId);
      res.json({ mods });
    } catch (error) {
      console.error("Error fetching mods:", error);
      res.status(500).json({ message: "Failed to fetch mods" });
    }
  });

  // Get a specific mod
  app.get("/api/mods/:id", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      res.json({ mod });
    } catch (error) {
      console.error("Error fetching mod:", error);
      res.status(500).json({ message: "Failed to fetch mod" });
    }
  });

  // Create a new mod
  app.post("/api/mods", async (req, res) => {
    try {
      // Validate request body
      const validationResult = insertModSchema.safeParse(req.body);
      if (!validationResult.success) {
        return res.status(400).json({ 
          message: "Invalid mod data",
          errors: validationResult.error.errors 
        });
      }

      // For this demo, we'll use user ID 1
      const userId = 1;
      const modData = validationResult.data;

      // Create the mod in storage
      const mod = await storage.createMod({
        ...modData,
        userId
      });

      // Create a new build
      const build = await storage.createBuild({
        modId: mod.id,
        buildNumber: 1,
        version: "0.1.0", // Default initial version
        status: "in_progress",
        errors: [], // Required field
        errorCount: 0,
        warningCount: 0,
        logs: `Starting build #1 for ${mod.name}...\n`,
        downloadUrl: null,
        metadata: {} // Required field
      });

      // Start generating code asynchronously
      generateModCodeAsync(mod, build);

      res.status(201).json({ mod, build });
    } catch (error) {
      console.error("Error creating mod:", error);
      res.status(500).json({ message: "Failed to create mod" });
    }
  });

  // Update an existing mod
  app.patch("/api/mods/:id", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Validate request body (partial schema)
      const updatedMod = await storage.updateMod(modId, req.body);
      res.json({ mod: updatedMod });
    } catch (error) {
      console.error("Error updating mod:", error);
      res.status(500).json({ message: "Failed to update mod" });
    }
  });

  // Delete a mod
  app.delete("/api/mods/:id", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const success = await storage.deleteMod(modId);
      if (!success) {
        return res.status(404).json({ message: "Mod not found" });
      }

      res.status(204).send();
    } catch (error) {
      console.error("Error deleting mod:", error);
      res.status(500).json({ message: "Failed to delete mod" });
    }
  });

  // Get all builds for a mod
  app.get("/api/mods/:id/builds", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const builds = await storage.getBuildsByModId(modId);
      res.json({ builds });
    } catch (error) {
      console.error("Error fetching builds:", error);
      res.status(500).json({ message: "Failed to fetch builds" });
    }
  });

  // Get a specific build
  app.get("/api/mods/:modId/builds/:buildId", async (req, res) => {
    try {
      const buildId = parseInt(req.params.buildId, 10);
      if (isNaN(buildId)) {
        return res.status(400).json({ message: "Invalid build ID" });
      }

      const build = await storage.getBuild(buildId);
      if (!build) {
        return res.status(404).json({ message: "Build not found" });
      }

      res.json({ build });
    } catch (error) {
      console.error("Error fetching build:", error);
      res.status(500).json({ message: "Failed to fetch build" });
    }
  });

  // Manually trigger compilation
  app.post("/api/mods/:id/compile", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Get the latest build number for this mod
      const builds = await storage.getBuildsByModId(modId);
      const buildNumber = builds.length > 0 ? builds[0].buildNumber + 1 : 1;

      // Create a new build
      const build = await storage.createBuild({
        modId: mod.id,
        buildNumber,
        version: `0.${buildNumber}.0`, // Incrementing version number
        status: "in_progress",
        errors: [], // Required field
        errorCount: 0,
        warningCount: 0,
        logs: `Starting build #${buildNumber} for ${mod.name}...\n`,
        downloadUrl: null,
        metadata: {} // Required field
      });

      // Start compilation asynchronously
      compileModAsync(mod, build);

      res.status(201).json({ build });
    } catch (error) {
      console.error("Error starting compilation:", error);
      res.status(500).json({ message: "Failed to start compilation" });
    }
  });

  // Verify GitHub token
  app.post("/api/github/verify-token", async (req, res) => {
    try {
      const { token } = req.body;
      if (!token) {
        return res.status(400).json({ message: "GitHub token is required" });
      }
      
      try {
        // Test the token by making a request to the GitHub API
        const response = await axios.get('https://api.github.com/user', {
          headers: {
            'Authorization': `token ${token}`,
            'Accept': 'application/vnd.github.v3+json'
          }
        });
        
        // If we get here, the token is valid
        res.json({ 
          valid: true, 
          username: response.data.login 
        });
      } catch (githubError) {
        // Token is invalid or doesn't have the right permissions
        res.status(200).json({ 
          valid: false,
          message: "Invalid GitHub token or insufficient permissions"
        });
      }
    } catch (error) {
      console.error("Error verifying GitHub token:", error);
      res.status(500).json({ message: "Failed to verify GitHub token" });
    }
  });
  
  // Push to GitHub
  app.post("/api/mods/:id/push-to-github", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }
      
      // Get token from request body if provided (legacy support)
      const { token } = req.body;

      // Push to GitHub with both session authentication and token (if provided)
      const result = await pushModToGitHub(modId, req, token);
      
      if (!result.success) {
        return res.status(400).json({ 
          success: false,
          error: result.error || "Failed to push to GitHub"
        });
      }

      res.json({ 
        success: true,
        repoUrl: result.repoUrl,
        owner: result.owner
      });
    } catch (error) {
      console.error("Error pushing to GitHub:", error);
      res.status(500).json({ 
        success: false,
        message: "Failed to push to GitHub",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Download mod JAR
  app.get("/api/mods/:id/download", (req, res) => {
    // In a real implementation, this would serve the compiled JAR file
    // For now, we'll just return a message
    res.status(404).json({ message: "Download functionality not implemented" });
  });

  // AI Service Endpoints
  
  // Generate mod code
  app.post("/api/ai/generate-mod-code", async (req, res) => {
    try {
      const { modName, modDescription, modLoader, mcVersion, idea } = req.body;
      if (!modName || !modLoader || !idea) {
        return res.status(400).json({ message: "Missing required fields" });
      }

      const result = await generateModCode(
        modName,
        modDescription,
        modLoader,
        mcVersion,
        idea
      );

      res.json(result);
    } catch (error) {
      console.error("Error generating code:", error);
      res.status(500).json({ message: "Failed to generate code" });
    }
  });

  // Fix compilation errors
  app.post("/api/ai/fix-errors", async (req, res) => {
    try {
      const { files, errors } = req.body;
      if (!files || !errors) {
        return res.status(400).json({ message: "Missing required fields" });
      }

      const result = await fixCompilationErrors(files, errors);
      res.json(result);
    } catch (error) {
      console.error("Error fixing errors:", error);
      res.status(500).json({ message: "Failed to fix errors" });
    }
  });

  // Generic code generation
  app.post("/api/ai/generate-code", async (req, res) => {
    try {
      const { prompt, language, context, complexity } = req.body;
      if (!prompt) {
        return res.status(400).json({ message: "Prompt is required" });
      }

      const result = await generateCode(prompt, { language, context, complexity });
      res.json(result);
    } catch (error) {
      console.error("Error generating code:", error);
      res.status(500).json({ 
        message: "Failed to generate code",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Fix code with errors
  app.post("/api/ai/fix-code", async (req, res) => {
    try {
      const { code, errors, language } = req.body;
      if (!code || !errors) {
        return res.status(400).json({ message: "Code and errors are required" });
      }

      const result = await fixCode(code, errors, language);
      res.json(result);
    } catch (error) {
      console.error("Error fixing code:", error);
      res.status(500).json({ 
        message: "Failed to fix code",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Enhance code
  app.post("/api/ai/enhance-code", async (req, res) => {
    try {
      const { code, instructions, language } = req.body;
      if (!code || !instructions) {
        return res.status(400).json({ message: "Code and instructions are required" });
      }

      const result = await enhanceCode(code, instructions, language);
      res.json(result);
    } catch (error) {
      console.error("Error enhancing code:", error);
      res.status(500).json({ 
        message: "Failed to enhance code",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Summarize code
  app.post("/api/ai/summarize-code", async (req, res) => {
    try {
      const { code, language } = req.body;
      if (!code) {
        return res.status(400).json({ message: "Code is required" });
      }

      const result = await summarizeCode(code, language);
      res.json(result);
    } catch (error) {
      console.error("Error summarizing code:", error);
      res.status(500).json({ 
        message: "Failed to summarize code",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Explain error
  app.post("/api/ai/explain-error", async (req, res) => {
    try {
      const { errorMessage, code, language } = req.body;
      if (!errorMessage) {
        return res.status(400).json({ message: "Error message is required" });
      }

      const result = await explainError(errorMessage, code, language);
      res.json(result);
    } catch (error) {
      console.error("Error explaining error:", error);
      res.status(500).json({ 
        message: "Failed to explain error",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Generate documentation
  app.post("/api/ai/generate-documentation", async (req, res) => {
    try {
      const { code, language, style } = req.body;
      if (!code) {
        return res.status(400).json({ message: "Code is required" });
      }

      const result = await generateDocumentation(code, language, style);
      res.json(result);
    } catch (error) {
      console.error("Error generating documentation:", error);
      res.status(500).json({ 
        message: "Failed to generate documentation",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });
  
  // Removed duplicate endpoint for generate-generic-code
  // The functionality is covered by the /api/ai/generate-code endpoint

  // Add features
  app.post("/api/ai/add-features", async (req, res) => {
    try {
      const { files, features, modLoader, mcVersion } = req.body;
      if (!files || !features) {
        return res.status(400).json({ message: "Files and features are required" });
      }

      // Only passing the required arguments (files and features)
      const result = await addModFeatures(files, features);
      res.json(result);
    } catch (error) {
      console.error("Error adding features:", error);
      res.status(500).json({ 
        message: "Failed to add features",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Generate ideas
  app.post("/api/ai/generate-ideas", async (req, res) => {
    try {
      // Validate request body
      const validationResult = ideaGenerationRequestSchema.safeParse(req.body);
      if (!validationResult.success) {
        return res.status(400).json({ 
          message: "Invalid request data",
          errors: validationResult.error.errors 
        });
      }

      const ideaRequest = validationResult.data;
      const ideas = await generateModIdeas(ideaRequest);
      res.json(ideas);
    } catch (error) {
      console.error("Error generating ideas:", error);
      res.status(500).json({ 
        message: "Failed to generate ideas",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Expand idea
  app.post("/api/ai/expand-idea", async (req, res) => {
    try {
      const { title, description } = req.body;
      if (!title || !description) {
        return res.status(400).json({ message: "Title and description are required" });
      }

      const expandedIdea = await expandModIdea(title, description);
      res.json(expandedIdea);
    } catch (error) {
      console.error("Error expanding idea:", error);
      res.status(500).json({ 
        message: "Failed to expand idea",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });
  
  // New frontend-friendly routes for the idea generator page
  
  // Generate ideas
  app.post("/api/idea-generator/generate", async (req, res) => {
    try {
      // Create a compatible request object expected by the service
      const ideaRequest = {
        theme: req.body.theme || "",
        modLoader: req.body.modLoader || "forge",
        complexity: req.body.complexity || "Moderate",
        minecraftVersion: req.body.minecraftVersion || "1.20.4",
        keywords: req.body.keywords || [],
        count: req.body.count || 3
      };
      
      // Validate request body
      const validationResult = ideaGenerationRequestSchema.safeParse(ideaRequest);
      if (!validationResult.success) {
        return res.status(400).json({ 
          message: "Invalid request data",
          errors: validationResult.error.errors 
        });
      }

      const ideas = await generateModIdeas(validationResult.data);
      res.json(ideas);
    } catch (error) {
      console.error("Error generating ideas:", error);
      res.status(500).json({ 
        message: "Failed to generate ideas",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Expand idea
  app.post("/api/idea-generator/expand", async (req, res) => {
    try {
      const { title, description } = req.body;
      if (!title || !description) {
        return res.status(400).json({ message: "Title and description are required" });
      }

      const expandedIdea = await expandModIdea(title, description);
      res.json(expandedIdea);
    } catch (error) {
      console.error("Error expanding idea:", error);
      res.status(500).json({ 
        message: "Failed to expand idea",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Reset circuit breaker for a mod
  app.post("/api/mods/:id/circuit-breaker/reset", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ success: false, message: "Invalid mod ID" });
      }

      // Check if mod exists
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ success: false, message: "Mod not found" });
      }

      const result = continuousService.resetCircuitBreaker(modId);
      
      if (result.success) {
        res.json({ 
          success: true, 
          message: result.message || "Circuit breaker reset successfully"
        });
      } else {
        // Return a 409 Conflict if already reset
        res.status(409).json({ 
          success: false, 
          message: result.message || "Circuit breaker is not tripped" 
        });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Error resetting circuit breaker:", errorMessage);
      res.status(500).json({ 
        success: false, 
        message: "Failed to reset circuit breaker",
        error: errorMessage 
      });
    }
  });

  // Start/stop continuous development
  app.post("/api/mods/:id/continuous-development", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ success: false, message: "Invalid mod ID" });
      }

      const { action, frequency } = req.body;
      if (action !== "start" && action !== "stop") {
        return res.status(400).json({ success: false, message: "Action must be 'start' or 'stop'" });
      }

      // Check if mod exists
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ success: false, message: "Mod not found" });
      }

      if (action === "start") {
        const frequencyMs = frequency ? parseInt(frequency, 10) * 1000 : undefined;
        const result = continuousService.startContinuousDevelopment(modId, frequencyMs);
        
        if (result.success) {
          res.json({ 
            success: true, 
            message: "Continuous development started"
          });
        } else {
          // If there was a circuit breaker or other issue, return as a 409 Conflict
          res.status(409).json({ 
            success: false, 
            message: result.message || "Could not start continuous development" 
          });
        }
      } else {
        const result = continuousService.stopContinuousDevelopment(modId);
        
        if (result.success) {
          res.json({ 
            success: true, 
            message: "Continuous development stopped"
          });
        } else {
          // Return conflict if it wasn't running
          res.status(409).json({ 
            success: false, 
            message: result.message || "Continuous development was not running" 
          });
        }
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Error managing continuous development:", errorMessage);
      res.status(500).json({ 
        success: false, 
        message: "Failed to manage continuous development",
        error: errorMessage 
      });
    }
  });

  // Get continuous development status
  app.get("/api/mods/:id/continuous-development", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ success: false, message: "Invalid mod ID" });
      }

      // Check if mod exists
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ success: false, message: "Mod not found" });
      }

      // Get comprehensive statistics including circuit breaker status
      const stats = continuousService.getStatistics(modId);
      
      // Get the most recent builds to include in the status response
      const recentBuilds = await storage.getBuildsByModId(modId);
      const lastFiveBuilds = recentBuilds.slice(0, 5).map(build => ({
        id: build.id,
        buildNumber: build.buildNumber,
        status: build.status,
        errorCount: build.errorCount,
        warningCount: build.warningCount,
        timestamp: build.updatedAt || build.createdAt
      }));
      
      // Get any in-progress builds
      const inProgressBuilds = recentBuilds.filter(build => 
        build.status === "in_progress" || 
        build.status === "queued"
      );
      
      res.json({
        success: true,
        isRunning: stats.isRunning,
        statistics: stats,
        recentBuilds: lastFiveBuilds,
        activeBuilds: inProgressBuilds.length,
        circuitBreakerStatus: stats.circuitBreakerStatus
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Error getting continuous development status:", errorMessage);
      res.status(500).json({ 
        success: false, 
        message: "Failed to get continuous development status",
        error: errorMessage
      });
    }
  });

  // Get mod files
  app.get("/api/mods/:id/files", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const files = await storage.getModFilesByModId(modId);
      res.json({ files });
    } catch (error) {
      console.error("Error fetching mod files:", error);
      res.status(500).json({ message: "Failed to fetch mod files" });
    }
  });

  // Get a specific mod file
  app.get("/api/mods/:modId/files/:fileId", async (req, res) => {
    try {
      const fileId = parseInt(req.params.fileId, 10);
      if (isNaN(fileId)) {
        return res.status(400).json({ message: "Invalid file ID" });
      }

      const file = await storage.getModFile(fileId);
      if (!file) {
        return res.status(404).json({ message: "File not found" });
      }

      res.json({ file });
    } catch (error) {
      console.error("Error fetching mod file:", error);
      res.status(500).json({ message: "Failed to fetch mod file" });
    }
  });

  // Create a mod file
  app.post("/api/mods/:id/files", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const { path, content } = req.body;
      if (!path || content === undefined) {
        return res.status(400).json({ message: "Path and content are required" });
      }

      // Check if mod exists
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      const file = await storage.createModFile({
        modId,
        path,
        content,
        contentType: "text/plain", // Default content type
        metadata: {} // Default empty metadata
      });

      res.status(201).json({ file });
    } catch (error) {
      console.error("Error creating mod file:", error);
      res.status(500).json({ message: "Failed to create mod file" });
    }
  });

  // Update a mod file
  app.patch("/api/mods/:modId/files/:fileId", async (req, res) => {
    try {
      const fileId = parseInt(req.params.fileId, 10);
      if (isNaN(fileId)) {
        return res.status(400).json({ message: "Invalid file ID" });
      }

      const { content } = req.body;
      if (content === undefined) {
        return res.status(400).json({ message: "Content is required" });
      }

      // Check if file exists
      const file = await storage.getModFile(fileId);
      if (!file) {
        return res.status(404).json({ message: "File not found" });
      }

      const updatedFile = await storage.updateModFile(fileId, { content });
      res.json({ file: updatedFile });
    } catch (error) {
      console.error("Error updating mod file:", error);
      res.status(500).json({ message: "Failed to update mod file" });
    }
  });

  // Delete a mod file
  app.delete("/api/mods/:modId/files/:fileId", async (req, res) => {
    try {
      const fileId = parseInt(req.params.fileId, 10);
      if (isNaN(fileId)) {
        return res.status(400).json({ message: "Invalid file ID" });
      }

      const success = await storage.deleteModFile(fileId);
      if (!success) {
        return res.status(404).json({ message: "File not found" });
      }

      res.status(204).send();
    } catch (error) {
      console.error("Error deleting mod file:", error);
      res.status(500).json({ message: "Failed to delete mod file" });
    }
  });

  // Use feature routers
  app.use("/api/metrics", apiMetricsRouter);
  
  // Admin control for all continuous services
  app.post("/api/system/continuous-service/control", requireAuth, async (req, res) => {
    try {
      // Check if user is admin
      if (!req.user?.isAdmin) {
        return res.status(403).json({
          success: false, 
          message: "Administrative privileges required for this operation"
        });
      }
      
      const { action, reason } = req.body;
      
      if (action === "shutdown") {
        const result = continuousService.shutdownAll(reason || "Administrative shutdown");
        res.json({
          success: true,
          message: `All continuous development processes shutdown. ${result.summary.shutdownCount} processes stopped.`,
          details: result.summary
        });
      } else {
        return res.status(400).json({
          success: false,
          message: "Invalid action. Supported actions: 'shutdown'"
        });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Error controlling continuous service:", errorMessage);
      res.status(500).json({
        success: false,
        message: "Failed to control continuous service",
        error: errorMessage
      });
    }
  });
  
  // API metrics for continuous service (admin only)
  app.get("/api/system/continuous-service/metrics", requireAuth, async (req, res) => {
    // Check if user is admin
    if (!req.user?.isAdmin) {
      return res.status(403).json({
        success: false, 
        message: "Administrative privileges required for this operation"
      });
    }
    
    try {
      // Get circuit breaker stats
      const circuitBreakerKeys = Object.keys(process.env)
        .filter(key => key.startsWith('circuit_breaker_') && !key.includes('_time'));
      
      const circuitBreakerStats = circuitBreakerKeys.map(key => {
        const modId = parseInt(key.replace('circuit_breaker_', ''), 10);
        const count = parseInt(process.env[key] || '0', 10);
        const timeKey = `${key}_time`;
        const tripTime = process.env[timeKey] ? new Date(parseInt(process.env[timeKey], 10)) : null;
        
        return {
          modId,
          failureCount: count,
          tripped: count >= 5,
          tripTime: tripTime ? tripTime.toISOString() : null
        };
      });
      
      // Get overall system metrics from the health status
      const healthStatus = continuousService.getHealthStatus();
      const totalRunningMods = healthStatus.runningMods.length;
      const totalTrippedCircuitBreakers = circuitBreakerStats.filter(s => s.tripped).length;
      
      // Return comprehensive metrics
      res.json({
        success: true,
        metrics: {
          totalRunningMods,
          totalTrippedCircuitBreakers,
          circuitBreakers: circuitBreakerStats,
          timestamp: new Date().toISOString()
        }
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Error getting continuous service metrics:", errorMessage);
      res.status(500).json({
        success: false,
        message: "Failed to get continuous service metrics",
        error: errorMessage
      });
    }
  });
  
  // Continuous service health endpoint (admin only)
  app.get("/api/system/continuous-service/health", requireAuth, async (req, res) => {
    // Check if user is admin
    if (!req.user?.isAdmin) {
      return res.status(403).json({
        success: false, 
        message: "Administrative privileges required for this operation"
      });
    }
    try {
      const health = continuousService.getHealthStatus();
      
      // Get additional system information
      const systemInfo = {
        memory: process.memoryUsage(),
        uptime: process.uptime(),
        env: process.env.NODE_ENV,
        timestamp: new Date().toISOString()
      };
      
      res.json({
        success: true,
        health,
        system: systemInfo
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error("Error getting continuous service health:", errorMessage);
      res.status(500).json({
        success: false,
        message: "Failed to get continuous service health",
        error: errorMessage
      });
    }
  });
  
  app.use("/api/web-explorer", webExplorerRouter);
  app.use("/api/jar-analyzer", jarAnalyzerRouter);
  app.use("/api/pattern-learning", patternLearningRouter);
  app.use("/api/github", githubRoutes);
  app.use("/api/error-monitoring", requireAuth, errorMonitoringRouter);
  app.use("/api/health-check", healthCheckRoutes);
  app.use("/api/error-tracking", requireAuth, errorTrackingRoutes);
  app.use("/api/system-dashboard", requireAuth, systemDashboardRoutes);
  app.use("/api/backups", requireAuth, backupRoutes);
  app.use("/api/notifications", requireAuth, notificationRoutes);
  
  // Test endpoint for logging system
  app.get("/api/logging/test", requireAuth, (req, res) => {
    // Import the logging system
    import('./logging').then(({ getLogger }) => {
      const logger = getLogger('api-test', req.user?.id?.toString());
      
      // Generate test logs at different levels
      logger.debug('This is a debug message');
      logger.info('This is an info message');
      logger.warn('This is a warning message');
      logger.error('This is an error message', { source: 'test endpoint', userId: req.user?.id });
      
      res.json({
        success: true,
        message: 'Test logs generated successfully',
        timestamp: new Date().toISOString()
      });
    }).catch(error => {
      res.status(500).json({
        success: false,
        message: 'Failed to load logging system',
        error: error instanceof Error ? error.message : String(error)
      });
    });
  });
  
  // Test endpoint for error tracking system
  app.post("/api/error-tracking/test", requireAuth, (req, res) => {
    // Only allow admin users to run the test
    if (!req.user?.isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Only administrators can run this test'
      });
    }
    
    // Import and run the error tracking test
    import('./test-error-tracking').then(async ({ testErrorTracking }) => {
      try {
        await testErrorTracking();
        
        res.json({
          success: true,
          message: 'Error tracking system test completed successfully',
          timestamp: new Date().toISOString()
        });
      } catch (error) {
        res.status(500).json({
          success: false,
          message: 'Error tracking system test failed',
          error: error instanceof Error ? error.message : String(error),
          timestamp: new Date().toISOString()
        });
      }
    }).catch(error => {
      res.status(500).json({
        success: false,
        message: 'Failed to load error tracking test module',
        error: error instanceof Error ? error.message : String(error),
        timestamp: new Date().toISOString()
      });
    });
  });
  
  // Maintenance endpoint for manual system maintenance
  app.post("/api/maintenance/run", requireAuth, async (req, res) => {
    try {
      // Only allow admin users to trigger maintenance
      if (!req.user?.isAdmin) {
        return res.status(403).json({
          success: false,
          message: "Only administrators can trigger maintenance tasks"
        });
      }
      
      // Import maintenance tasks
      const { cleanupTempFiles, cleanupCompiledModules } = await import('./maintenance-tasks');
      
      // Run maintenance tasks
      const tempCleanupResult = await cleanupTempFiles(
        path.join(__dirname, '../.temp'), 
        req.body.maxAgeHours || 24
      );
      
      const moduleCleanupResult = await cleanupCompiledModules();
      
      // Return results
      res.json({
        success: true,
        message: "Maintenance tasks completed",
        results: {
          tempFiles: tempCleanupResult,
          compiledModules: moduleCleanupResult
        },
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        message: "Failed to run maintenance tasks",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });
  
  // Add error tracker middleware before the main error handler
  app.use(errorTrackerMiddleware());
  
  // Register global error handler as the last middleware
  app.use(errorHandlerMiddleware);

  // Schedule periodic health checks (every 5 minutes)
  const cleanupHealthChecks = scheduleHealthChecks(5 * 60 * 1000);
  
  // Add cleanup function to any existing array of cleanup functions
  const originalClose = httpServer.close.bind(httpServer);
  httpServer.close = (callback?: (err?: Error) => void) => {
    // Cleanup health checks when server is closed
    cleanupHealthChecks();
    return originalClose(callback);
  };

  return httpServer;
}
