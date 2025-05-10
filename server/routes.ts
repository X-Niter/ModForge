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
import { pushModToGitHub } from "./github";
import { checkDatabaseHealth } from "./db-health";
import { continuousService } from "./continuous-service";
import { generateModIdeas, expandModIdea, ideaGenerationRequestSchema } from "./idea-generator-service";
import { z } from "zod";
import { insertModSchema } from "@shared/schema";
import { BuildStatus } from "@/types";
import apiMetricsRouter from "./routes/api-metrics";
import webExplorerRouter from "./routes/web-explorer-routes";
import jarAnalyzerRouter from "./routes/jar-analyzer-routes";
import axios from "axios";
import rateLimit from "express-rate-limit";

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
        content: file.content
      });
    }
    
    // Compile the mod
    await compileModAsync(mod, build);
  } catch (error) {
    console.error("Error in generateModCodeAsync:", error);
    // Update build with error
    await storage.updateBuild(build.id, {
      status: BuildStatus.Failed,
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
    const status = compileResult.success ? BuildStatus.Success : BuildStatus.Failed;
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
            content: file.content
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
      const finalStatus = recompileResult.success ? BuildStatus.Success : BuildStatus.Failed;
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
      status: BuildStatus.Failed,
      logs: currentLogs + `\nError during compilation: ${error instanceof Error ? error.message : String(error)}\n`,
      errorCount: 1
    });
  }
}

export async function registerRoutes(app: Express): Promise<Server> {
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
        status: BuildStatus.InProgress,
        errorCount: 0,
        warningCount: 0,
        logs: `Starting build #1 for ${mod.name}...\n`,
        downloadUrl: null
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
        status: BuildStatus.InProgress,
        errorCount: 0,
        warningCount: 0,
        logs: `Starting build #${buildNumber} for ${mod.name}...\n`,
        downloadUrl: null
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
      
      const { token } = req.body;
      if (!token) {
        return res.status(400).json({ message: "GitHub token is required" });
      }

      // Push to GitHub
      const result = await pushModToGitHub(modId, token);
      
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

      const result = await addModFeatures(files, features, modLoader, mcVersion);
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

  // Start/stop continuous development
  app.post("/api/mods/:id/continuous-development", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const { action, frequency } = req.body;
      if (action !== "start" && action !== "stop") {
        return res.status(400).json({ message: "Action must be 'start' or 'stop'" });
      }

      // Check if mod exists
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      if (action === "start") {
        const frequencyMs = frequency ? parseInt(frequency, 10) * 1000 : undefined;
        continuousService.startContinuousDevelopment(modId, frequencyMs);
        res.json({ message: "Continuous development started" });
      } else {
        continuousService.stopContinuousDevelopment(modId);
        res.json({ message: "Continuous development stopped" });
      }
    } catch (error) {
      console.error("Error managing continuous development:", error);
      res.status(500).json({ message: "Failed to manage continuous development" });
    }
  });

  // Get continuous development status
  app.get("/api/mods/:id/continuous-development", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      // Check if mod exists
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      const isRunning = continuousService.isRunning(modId);
      const stats = continuousService.getStatistics(modId);

      res.json({
        isRunning,
        statistics: stats
      });
    } catch (error) {
      console.error("Error getting continuous development status:", error);
      res.status(500).json({ message: "Failed to get continuous development status" });
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
        content
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
  app.use("/api/web-explorer", webExplorerRouter);
  app.use("/api/jar-analyzer", jarAnalyzerRouter);

  return httpServer;
}
