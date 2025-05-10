import type { Express, Request, Response } from "express";
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

export async function registerRoutes(app: Express): Promise<Server> {
  // Create HTTP server
  const httpServer = createServer(app);

  // Health check endpoint
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
  
  // Generate generic code
  app.post("/api/ai/generate-generic-code", async (req, res) => {
    try {
      const { prompt, language, context, complexity } = req.body;
      if (!prompt) {
        return res.status(400).json({ message: "Prompt is required" });
      }

      const result = await generateCode(prompt, { language, context, complexity });
      res.json(result);
    } catch (error) {
      console.error("Error generating generic code:", error);
      res.status(500).json({ 
        message: "Failed to generate generic code",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Add features
  app.post("/api/ai/add-features", async (req, res) => {
    try {
      const { files, newFeatureDescription } = req.body;
      if (!files || !newFeatureDescription) {
        return res.status(400).json({ message: "Missing required fields" });
      }

      const result = await addModFeatures(files, newFeatureDescription);
      res.json(result);
    } catch (error) {
      console.error("Error adding features:", error);
      res.status(500).json({ message: "Failed to add features" });
    }
  });
  
  // Generate mod ideas
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

      // Generate ideas
      const ideas = await generateModIdeas(validationResult.data);
      res.json(ideas);
    } catch (error) {
      console.error("Error generating mod ideas:", error);
      res.status(500).json({ 
        message: "Failed to generate mod ideas",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });
  
  // Expand a mod idea
  app.post("/api/ai/expand-idea", async (req, res) => {
    try {
      const { title, description } = req.body;
      if (!title || !description) {
        return res.status(400).json({ message: "Missing required fields: title and description" });
      }

      // Expand the idea
      const expandedIdea = await expandModIdea(title, description);
      res.json(expandedIdea);
    } catch (error) {
      console.error("Error expanding mod idea:", error);
      res.status(500).json({ 
        message: "Failed to expand mod idea",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Continuous Development Endpoints

  // Start continuous development
  app.post("/api/mods/:id/continuous-development/start", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Get frequency from the mod's compileFrequency setting
      let frequency = 5 * 60 * 1000; // Default: 5 minutes
      
      switch (mod.compileFrequency) {
        case "Every 5 Minutes":
          frequency = 5 * 60 * 1000;
          break;
        case "Every 15 Minutes":
          frequency = 15 * 60 * 1000;
          break;
        case "Manual Only":
          return res.status(400).json({ 
            message: "Cannot start continuous development for mods with 'Manual Only' compile frequency" 
          });
        case "After Every Change":
          frequency = 2 * 60 * 1000; // Check every 2 minutes for changes
          break;
      }

      // Start continuous development
      continuousService.startContinuousDevelopment(modId, frequency);

      res.json({ 
        message: "Continuous development started",
        modId,
        frequency,
        compileFrequency: mod.compileFrequency
      });
    } catch (error) {
      console.error("Error starting continuous development:", error);
      res.status(500).json({ message: "Failed to start continuous development" });
    }
  });

  // Stop continuous development
  app.post("/api/mods/:id/continuous-development/stop", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Stop continuous development
      continuousService.stopContinuousDevelopment(modId);

      res.json({ 
        message: "Continuous development stopped",
        modId
      });
    } catch (error) {
      console.error("Error stopping continuous development:", error);
      res.status(500).json({ message: "Failed to stop continuous development" });
    }
  });

  // Get continuous development status
  app.get("/api/mods/:id/continuous-development/status", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Get continuous development status
      const status = continuousService.getStatistics(modId);

      res.json({ 
        modId,
        status
      });
    } catch (error) {
      console.error("Error getting continuous development status:", error);
      res.status(500).json({ message: "Failed to get continuous development status" });
    }
  });
  
  // Get features and their progress for a mod
  app.get("/api/mods/:id/features/progress", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // In a real implementation, this would fetch from a database
      // For now, we'll simulate this with a static data object
      res.json({
        modId,
        features: [
          {
            id: "feature-1",
            name: "Basic mod structure and configuration",
            status: "completed",
            progress: 100,
            notes: "Initial setup complete with all required files"
          },
          {
            id: "feature-2",
            name: "Custom weapons implementation",
            status: "in_progress",
            progress: 65,
            estimatedCompletion: "~20 minutes",
            notes: "Currently implementing attack animations"
          },
          {
            id: "feature-3",
            name: "Stamina system",
            status: "planned",
            progress: 0,
            estimatedCompletion: "~45 minutes",
            notes: "Will start after weapons implementation"
          },
          {
            id: "feature-4",
            name: "Particle effects for combat actions",
            status: "planned",
            progress: 0,
            estimatedCompletion: "~30 minutes"
          }
        ],
        currentFeature: "feature-2",
        aiSuggestions: [
          "Consider adding special weapon effects for critical hits",
          "Combat balancing: adjust damage values based on weapon weight",
          "Add sound effects for each weapon type"
        ]
      });
    } catch (error) {
      console.error("Error getting feature progress:", error);
      res.status(500).json({ message: "Failed to get feature progress" });
    }
  });
  
  // Get analytics data for a mod
  app.get("/api/mods/:id/analytics/:timeRange?", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const timeRange = req.params.timeRange || "week";
      
      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // In a real implementation, this would calculate analytics from real data
      // For now, we'll simulate this with a static data object
      res.json({
        modId: mod.id,
        buildStats: {
          totalBuilds: 37,
          successfulBuilds: 29,
          failedBuilds: 8,
          buildsByDay: [
            { date: "Mon", count: 5, successful: 3, failed: 2 },
            { date: "Tue", count: 7, successful: 5, failed: 2 },
            { date: "Wed", count: 6, successful: 5, failed: 1 },
            { date: "Thu", count: 8, successful: 7, failed: 1 },
            { date: "Fri", count: 6, successful: 5, failed: 1 },
            { date: "Sat", count: 3, successful: 2, failed: 1 },
            { date: "Sun", count: 2, successful: 2, failed: 0 }
          ]
        },
        errorStats: {
          totalErrors: 24,
          errorsFixed: 22,
          errorCategories: [
            { name: "Syntax", count: 9 },
            { name: "Type", count: 7 },
            { name: "Logical", count: 4 },
            { name: "Dependency", count: 3 },
            { name: "Other", count: 1 }
          ],
          errorsByDay: [
            { date: "Mon", count: 5, fixed: 4 },
            { date: "Tue", count: 6, fixed: 5 },
            { date: "Wed", count: 4, fixed: 4 },
            { date: "Thu", count: 3, fixed: 3 },
            { date: "Fri", count: 4, fixed: 4 },
            { date: "Sat", count: 2, fixed: 2 },
            { date: "Sun", count: 0, fixed: 0 }
          ]
        },
        featureStats: {
          totalFeatures: 12,
          completedFeatures: 8,
          featureCategories: [
            { name: "Weapons", count: 5 },
            { name: "Combat System", count: 3 },
            { name: "UI", count: 2 },
            { name: "Effects", count: 2 }
          ],
          featureProgress: [
            { date: "Week 1", completed: 2, total: 12 },
            { date: "Week 2", completed: 5, total: 12 },
            { date: "Week 3", completed: 8, total: 12 }
          ]
        },
        performanceStats: {
          averageBuildTime: 42,
          averageFixTime: 18,
          buildTimes: [
            { buildNumber: 1, time: 65 },
            { buildNumber: 2, time: 58 },
            { buildNumber: 3, time: 51 },
            { buildNumber: 4, time: 48 },
            { buildNumber: 5, time: 45 },
            { buildNumber: 6, time: 43 },
            { buildNumber: 7, time: 40 }
          ]
        }
      });
    } catch (error) {
      console.error("Error getting analytics data:", error);
      res.status(500).json({ message: "Failed to get analytics data" });
    }
  });

  // Mount the metrics API routes
  app.use('/api/metrics', apiMetricsRouter);
  
  // Mount the web explorer API routes
  app.use('/api/web-explorer', webExplorerRouter);
  
  // Mount the JAR analyzer API routes
  app.use('/api/jar-analyzer', jarAnalyzerRouter);

  return httpServer;
}

// Asynchronous code generation process
async function generateModCodeAsync(mod: any, build: any) {
  try {
    // Generate code
    const result = await generateModCode(
      mod.name,
      mod.description,
      mod.modLoader,
      mod.minecraftVersion,
      mod.idea
    );

    // Update build logs - use null coalescing to handle undefined case
    const initialUpdate = await storage.updateBuild(build.id, {
      logs: build.logs + result.logs
    });
    
    if (!initialUpdate) {
      throw new Error(`Failed to update build ${build.id}`);
    }
    
    let currentLogs = initialUpdate.logs || '';

    // Store files in storage
    for (const file of result.files) {
      await storage.createModFile({
        modId: mod.id,
        path: file.path,
        content: file.content
      });
    }

    // Compile the mod - add to logs safely
    currentLogs += "\nStarting compilation process...\n";
    const preCompileUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
    
    if (preCompileUpdate) {
      currentLogs = preCompileUpdate.logs;
    }
    
    const compileResult = await compileMod(mod.id);

    // Update build with compilation results
    const status = compileResult.success ? BuildStatus.Success : BuildStatus.Failed;
    currentLogs += compileResult.logs;
    
    const postCompileUpdate = await storage.updateBuild(build.id, {
      status,
      logs: currentLogs,
      errorCount: compileResult.errors.length,
      warningCount: compileResult.warnings.length,
      downloadUrl: compileResult.downloadUrl
    });
    
    if (postCompileUpdate) {
      currentLogs = postCompileUpdate.logs;
    }

    // If compilation failed and auto-fix is enabled, try to fix errors
    if (!compileResult.success && compileResult.errors.length > 0 && 
        (mod.autoFixLevel === "Balanced" || mod.autoFixLevel === "Aggressive")) {
      
      // Add fix logs
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
      const fixLogsUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (fixLogsUpdate) {
        currentLogs = fixLogsUpdate.logs;
      }

      // Update mod files with fixes
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

      // Try compilation again
      currentLogs += "\nRetrying compilation after fixes...\n";
      const preRetryUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preRetryUpdate) {
        currentLogs = preRetryUpdate.logs;
      }

      const retryResult = await compileMod(mod.id);
      
      // Update build with new compilation results
      const newStatus = retryResult.success ? BuildStatus.Success : BuildStatus.Failed;
      currentLogs += retryResult.logs;
      
      await storage.updateBuild(build.id, {
        status: newStatus,
        logs: currentLogs,
        errorCount: retryResult.errors.length,
        warningCount: retryResult.warnings.length,
        downloadUrl: retryResult.downloadUrl
      });
    }
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

// Asynchronous compilation process
async function compileModAsync(mod: any, build: any) {
  let currentLogs = build.logs || '';
  
  try {
    // Compile the mod
    const compileResult = await compileMod(mod.id);

    // Update build with compilation results
    const status = compileResult.success ? BuildStatus.Success : BuildStatus.Failed;
    currentLogs += compileResult.logs;
    
    const compileUpdate = await storage.updateBuild(build.id, {
      status,
      logs: currentLogs,
      errorCount: compileResult.errors.length,
      warningCount: compileResult.warnings.length,
      downloadUrl: compileResult.downloadUrl
    });
    
    if (compileUpdate) {
      currentLogs = compileUpdate.logs;
    }

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
      const fixLogsUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (fixLogsUpdate) {
        currentLogs = fixLogsUpdate.logs;
      }

      // Update mod files with fixes
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

      // Try compilation again
      currentLogs += "\nRetrying compilation after fixes...\n";
      const preRetryUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preRetryUpdate) {
        currentLogs = preRetryUpdate.logs;
      }

      const retryResult = await compileMod(mod.id);
      
      // Update build with new compilation results
      const newStatus = retryResult.success ? BuildStatus.Success : BuildStatus.Failed;
      currentLogs += retryResult.logs;
      
      await storage.updateBuild(build.id, {
        status: newStatus,
        logs: currentLogs,
        errorCount: retryResult.errors.length,
        warningCount: retryResult.warnings.length,
        downloadUrl: retryResult.downloadUrl
      });
    }
  } catch (error) {
    console.error("Error in compileModAsync:", error);
    // Update build with error
    await storage.updateBuild(build.id, {
      status: BuildStatus.Failed,
      logs: build.logs + `\nError during compilation: ${error instanceof Error ? error.message : String(error)}\n`,
      errorCount: 1,
      warningCount: 0
    });
  }
}
